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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import jetbrains.buildServer.util.amazon.AWSException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

class LoggingDeploymentListener extends AWSClient.Listener {
  @NotNull
  private static final Logger LOG = Logger.getInstance(Loggers.VCS_CATEGORY + ElasticBeanstalkRunner.class);

  static final String CREATE_VERSION = "Create version";
  static final String UPDATE_ENVIRONMENT = "Update environment";

  @NotNull
  private final Map<String, String> myRunnerParameters;
  @NotNull
  private final BuildProgressLogger myBuildLogger;
  @NotNull
  private final String myCheckoutDir;

  LoggingDeploymentListener(@NotNull Map<String, String> runnerParameters, @NotNull BuildProgressLogger buildLogger, @NotNull String checkoutDir) {
    myRunnerParameters = runnerParameters;
    myBuildLogger = buildLogger;
    myCheckoutDir = checkoutDir;
  }

  @Override
  void createVersionStarted(@NotNull String applicationName, @NotNull String versionLabel,
                            @NotNull String s3BucketName, @NotNull String s3ObjectKey) {
    open(CREATE_VERSION);
    log(String.format("Creating application %s version %s with bucket %s and key %s.", applicationName, versionLabel, s3BucketName, s3ObjectKey));
  }

  @Override
  void createVersionSkipped(@NotNull String applicationName, @NotNull String versionLabel) {
    log(String.format("Application %s version %s already exists, skipping upload...", applicationName, versionLabel));
    close(CREATE_VERSION);
  }

  @Override
  void createVersionFinished(@NotNull String applicationName, @NotNull String versionLabel,
                             @NotNull String s3BucketName, @NotNull String s3ObjectKey) {
    log(String.format("Created application %s version %s with bucket %s and key %s.", applicationName, versionLabel, s3BucketName, s3ObjectKey));
    close(CREATE_VERSION);
  }

  @Override
  void deploymentStarted(@NotNull String applicationName, @NotNull String environmentName, @NotNull String versionLabel) {
    open(UPDATE_ENVIRONMENT);
    log(String.format("Started deployment of application %s version %s to %s.", applicationName, versionLabel, environmentName));
  }

  @Override
  void deploymentWaitStarted(@NotNull String environmentName) {
    log("Waiting for deployment finish.");
  }

  @Override
  void deploymentInProgress(@NotNull String environmentName) {
    progress(String.format("Waiting for deployment on environment %s.", environmentName));
  }

  @Override
  void deploymentUpdate(@NotNull String message) {
    progress(message);
  }

  @Override
  void deploymentFailed(@NotNull String applicationName, @NotNull String environmentName, @NotNull String versionLabel,
                        @NotNull Boolean hasTimeout, @Nullable ErrorInfo errorInfo) {
    String msg = (!hasTimeout ? "Error, " : "Timeout exceeded, ");

    String errMessage = "";
    String errSeverity = "";
    if (errorInfo != null) {
      if (StringUtil.isNotEmpty(errorInfo.message)) {
        err("Associated error: " + errorInfo.message);
        msg += ": " + errorInfo.message;
      }
      if (StringUtil.isNotEmpty(errorInfo.severity)) {
        err("Error severity: " + errorInfo.severity);
      }
      errMessage = errorInfo.message;
      errSeverity = errorInfo.severity;
    }

    String failureType = !hasTimeout ? ElasticBeanstalkConstants.FAILURE_BUILD_PROBLEM_TYPE : ElasticBeanstalkConstants.TIMEOUT_BUILD_PROBLEM_TYPE;

    problem(getIdentity(hasTimeout.toString(), errMessage, errSeverity), failureType, msg);

    close(UPDATE_ENVIRONMENT);
  }

  @Override
  void deploymentSucceeded(@NotNull String environmentName, @NotNull String versionLabel) {
    String message = String.format("Version %s was deployed to %s successfully.", versionLabel, environmentName);
    log(message);
    statusText(message);
    close(UPDATE_ENVIRONMENT);
  }

  @Override
  void exception(@NotNull AWSException e) {
    LOG.error(e);

    final String message = e.getMessage();
    final String details = e.getDetails();

    err(message);
    if (StringUtil.isNotEmpty(details)) err(details);
    problem(getIdentity(e.getIdentity()), e.getType(), message);
    close(UPDATE_ENVIRONMENT);
  }

  private int getIdentity(String... parts) {
    return AWSCommonParams.calculateIdentity(myCheckoutDir, myRunnerParameters, CollectionsUtil.join(getIdentityFormingParameters(), Arrays.asList(parts)));
  }

  @NotNull
  private Collection<String> getIdentityFormingParameters() {
    return Arrays.asList(
      myRunnerParameters.get(ElasticBeanstalkConstants.S3_OBJECT_KEY_PARAM),
      myRunnerParameters.get(ElasticBeanstalkConstants.S3_BUCKET_NAME_PARAM),
      myRunnerParameters.get(ElasticBeanstalkConstants.ENV_NAME_PARAM),
      myRunnerParameters.get(ElasticBeanstalkConstants.APP_NAME_PARAM),
      myRunnerParameters.get(ElasticBeanstalkConstants.APP_VERSION_SKIP_DUPE_PARAM),
      myRunnerParameters.get(ElasticBeanstalkConstants.APP_VERSION_PARAM));
  }

  protected void log(@NotNull String message) {
    myBuildLogger.message(message);
  }

  protected void err(@NotNull String message) {
    myBuildLogger.error(message);
  }

  protected void open(@NotNull String block) {
    myBuildLogger.targetStarted(block);
  }

  protected void close(@NotNull String block) {
    myBuildLogger.targetFinished(block);
  }

  protected void progress(@NotNull String message) {
    myBuildLogger.message(String.format("##teamcity[progressMessage '%s']", escape(message)));
  }

  protected void problem(int identity, @NotNull String type, @NotNull String descr) {
    myBuildLogger.message(String.format("##teamcity[buildProblem identity='%d' type='%s' description='%s' tc:tags='tc:internal']", identity, type, escape(descr)));
  }

  protected void statusText(@NotNull String text) {
    myBuildLogger.message(String.format("##teamcity[buildStatus tc:tags='tc:internal' text='{build.status.text}; %s']", text));
  }

  @NotNull
  private String escape(@NotNull String s) {
    return s.
      replace("|", "||").
      replace("'", "|'").
      replace("\n", "|n").
      replace("\r", "|r").
      replace("\\uNNNN", "|0xNNNN").
      replace("[", "|[").replace("]", "|]");
  }
}
