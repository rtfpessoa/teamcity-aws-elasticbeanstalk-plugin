/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.runner.elasticbeanstalk;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import jetbrains.buildServer.util.amazon.AWSClients;
import jetbrains.buildServer.util.amazon.AWSException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AWSClient {

  @NotNull
  private AWSElasticBeanstalkClient myElasticBeanstalkClient;
  @Nullable
  private String myDescription;
  @NotNull
  private Listener myListener = new Listener();

  public AWSClient(@NotNull AWSClients clients) {
    myElasticBeanstalkClient = clients.createElasticBeanstalkClient();
  }

  @NotNull
  public AWSClient withDescription(@NotNull String description) {
    myDescription = description;
    return this;
  }

  @NotNull
  public AWSClient withListener(@NotNull Listener listener) {
    myListener = listener;
    return this;
  }

  /**
   * Uploads application revision archive to S3 bucket named s3BucketName with the provided key and bundle type.
   * <p>
   * For performing this operation target AWSClient must have corresponding S3 permissions.
   *
   * @param s3BucketName valid S3 bucket name
   * @param s3ObjectKey  valid S3 object key
   */
  public void createApplicationVersion(@NotNull String applicationName, @NotNull String versionLabel,
                                       @NotNull String s3BucketName, @NotNull String s3ObjectKey) {
    try {
      myListener.createVersionStarted(applicationName, versionLabel, s3BucketName, s3ObjectKey);
      S3Location location = new S3Location().withS3Bucket(s3BucketName).withS3Key(s3ObjectKey);
      CreateApplicationVersionRequest request = new CreateApplicationVersionRequest(applicationName, versionLabel)
        .withSourceBundle(location);
      myElasticBeanstalkClient.createApplicationVersion(request);
      myListener.createVersionFinished(applicationName, versionLabel, s3BucketName, s3ObjectKey);
    } catch (Throwable t) {
      processFailure(t);
    }
  }

  /**
   * Creates deployment of the application revision from the specified location for specified application (must be pre-configured) to
   * deploymentGroupName (must be pre-configured) EC2 instances group with
   * deploymentConfigName or default configuration name and waits for deployment finish.
   * <p>
   * For performing this operation target AWSClient must have corresponding ElasticBeanstalk permissions.
   *
   * @param environmentName ElasticBeanstalk environment name
   * @param versionLabel    ElasticBeanstalk version label
   * @param waitTimeoutSec  seconds to wait for the created deployment finish or fail
   * @param waitIntervalSec seconds between polling ElasticBeanstalk for the created deployment status
   */
  public void updateEnvironmentAndWait(@NotNull String environmentName, @NotNull String versionLabel,
                                       int waitTimeoutSec, int waitIntervalSec) {
    doUpdateAndWait(environmentName, versionLabel, true, waitTimeoutSec, waitIntervalSec);
  }

  /**
   * The same as {@link #updateEnvironmentAndWait} but without waiting
   */
  public void updateEnvironment(@NotNull String environmentName, @NotNull String versionLabel) {
    doUpdateAndWait(environmentName, versionLabel, false, null, null);
  }

  @SuppressWarnings("ConstantConditions")
  private void doUpdateAndWait(@NotNull String environmentName, @NotNull String versionLabel,
                               boolean wait, @Nullable Integer waitTimeoutSec, @Nullable Integer waitIntervalSec) {
    try {
      UpdateEnvironmentRequest request = new UpdateEnvironmentRequest()
        .withEnvironmentName(environmentName)
        .withVersionLabel(versionLabel);
      UpdateEnvironmentResult result = myElasticBeanstalkClient.updateEnvironment(request);

      String environmentId = result.getEnvironmentId();

      myListener.deploymentStarted(environmentId, environmentName, versionLabel);

      if (wait) {
        waitForDeployment(environmentId, versionLabel, waitTimeoutSec, waitIntervalSec);
      }
    } catch (Throwable t) {
      processFailure(t);
    }
  }

  private void waitForDeployment(@NotNull String environmentId, String versionLabel, int waitTimeoutSec, int waitIntervalSec) {
    myListener.deploymentWaitStarted(environmentId);

    EnvironmentDescription environment = getEnvironment(environmentId);
    String status = getHumanReadableStatus(environment.getStatus());
    List<EventDescription> events = getErrorEvents(environmentId, versionLabel);
    boolean hasError = events.size() > 0;

    long startTime = System.currentTimeMillis();

    while (status.equals("updating") && !hasError) {
      myListener.deploymentInProgress(environmentId);

      if (System.currentTimeMillis() - startTime > waitTimeoutSec * 1000) {
        myListener.deploymentFailed(environmentId, environment.getApplicationName(), versionLabel, true, null);
        return;
      }

      try {
        Thread.sleep(waitIntervalSec * 1000);
      } catch (InterruptedException e) {
        processFailure(e);
        return;
      }

      environment = getEnvironment(environmentId);
      status = getHumanReadableStatus(environment.getStatus());
      events = getErrorEvents(environmentId, versionLabel);
      hasError = events.size() > 0;
    }

    if (isSuccess(environment, versionLabel)) {
      myListener.deploymentSucceeded(environmentId, environment.getApplicationName(), versionLabel);
    } else {
      Listener.ErrorInfo errorEvent = events.size() > 0 ? getErrorInfo(events.get(0)) : null;
      myListener.deploymentFailed(environmentId, environment.getApplicationName(), versionLabel, false, errorEvent);
    }
  }

  public EnvironmentDescription getEnvironment(@NotNull String environmentId) {
    return myElasticBeanstalkClient.describeEnvironments(new DescribeEnvironmentsRequest().withEnvironmentIds(environmentId))
      .getEnvironments().get(0);
  }

  private List<EventDescription> getErrorEvents(@NotNull String environmentId, String versionLabel) {
    return myElasticBeanstalkClient.describeEvents(new DescribeEventsRequest()
      .withEnvironmentId(environmentId)
      .withMaxRecords(10)
      .withVersionLabel(versionLabel)
      .withSeverity(EventSeverity.ERROR))
      .getEvents();
  }

  private boolean isSuccess(@NotNull EnvironmentDescription environment, @NotNull String versionLabel) {
    return environment.getVersionLabel().equals(versionLabel);
  }

  private void processFailure(@NotNull Throwable t) {
    myListener.exception(new AWSException(t));
  }

  @NotNull
  private String getHumanReadableStatus(@NotNull String status) {
    if (EnvironmentStatus.Launching.toString().equals(status)) return "launching";
    if (EnvironmentStatus.Updating.toString().equals(status)) return "updating";
    if (EnvironmentStatus.Ready.toString().equals(status)) return "ready";
    if (EnvironmentStatus.Terminating.toString().equals(status)) return "terminating";
    if (EnvironmentStatus.Terminated.toString().equals(status)) return "terminated";
    return ElasticBeanstalkConstants.STATUS_IS_UNKNOWN;
  }

  @NotNull
  private Listener.ErrorInfo getErrorInfo(@NotNull EventDescription event) {
    final Listener.ErrorInfo errorInfo = new Listener.ErrorInfo();
    errorInfo.message = removeTrailingDot(event.getMessage());
    errorInfo.severity = event.getSeverity();
    return errorInfo;
  }

  @Contract("null -> null")
  @Nullable
  private String removeTrailingDot(@Nullable String msg) {
    return (msg != null && msg.endsWith(".")) ? msg.substring(0, msg.length() - 1) : msg;
  }

  public static class Listener {
    void createVersionStarted(@NotNull String applicationName, @NotNull String versionLabel,
                              @NotNull String s3BucketName, @NotNull String s3ObjectKey) {
    }

    void createVersionFinished(@NotNull String applicationName, @NotNull String versionLabel,
                               @NotNull String s3BucketName, @NotNull String s3ObjectKey) {
    }

    void deploymentStarted(@NotNull String environmentId, @NotNull String applicationName, @NotNull String versionLabel) {
    }

    void deploymentWaitStarted(@NotNull String environmentId) {
    }

    void deploymentInProgress(@NotNull String environmentId) {
    }

    void deploymentFailed(@NotNull String environmentId, @NotNull String applicationName, @NotNull String versionLabel,
                          @NotNull Boolean hasTimeout, @Nullable ErrorInfo errorInfo) {
    }

    void deploymentSucceeded(@NotNull String environmentId, @NotNull String applicationName, @NotNull String versionLabel) {
    }

    void exception(@NotNull AWSException exception) {
    }

    public static class ErrorInfo {
      @Nullable
      String severity;
      @Nullable
      String message;
    }
  }
}
