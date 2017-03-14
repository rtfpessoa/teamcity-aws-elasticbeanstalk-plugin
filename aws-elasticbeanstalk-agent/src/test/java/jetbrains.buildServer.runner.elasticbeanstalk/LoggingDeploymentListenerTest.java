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

import jetbrains.buildServer.agent.NullBuildProgressLogger;
import jetbrains.buildServer.util.amazon.AWSException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

public class LoggingDeploymentListenerTest extends LoggingTestCase {

  private static final String FAKE_ENV_NAME = "ENV-NAME-FOO";
  private static final String FAKE_APP_NAME = "APP-NAME-BAR";
  private static final String FAKE_APP_VERSION = "1.0.0-alpha1";

  @BeforeMethod(alwaysRun = true)
  public void mySetUp() throws Exception {
    super.mySetUp();
  }

  @AfterMethod(alwaysRun = true)
  public void myTearDown() throws Exception {
    super.myTearDown();
  }

  @Test
  public void common_events() throws Exception {
    final LoggingDeploymentListener listener = create();

    final String bucketName = "bucketName";
    final String key = "path/key.zip";

    listener.createVersionStarted(FAKE_APP_NAME, FAKE_APP_VERSION, bucketName, key);

    listener.createVersionFinished(FAKE_APP_NAME, FAKE_APP_VERSION, bucketName, key);

    listener.deploymentStarted(FAKE_APP_NAME, FAKE_ENV_NAME, FAKE_APP_VERSION);

    listener.deploymentWaitStarted(FAKE_ENV_NAME);

    listener.deploymentInProgress(FAKE_ENV_NAME);

    listener.deploymentSucceeded(FAKE_ENV_NAME, FAKE_APP_VERSION);

    assertLog(
      "OPEN " + LoggingDeploymentListener.CREATE_VERSION,
      String.format("LOG Creating application %s version %s with bucket %s and key %s.", FAKE_APP_NAME, FAKE_APP_VERSION, bucketName, key),
      String.format("LOG Created application %s version %s with bucket %s and key %s.", FAKE_APP_NAME, FAKE_APP_VERSION, bucketName, key),
      "CLOSE " + LoggingDeploymentListener.CREATE_VERSION,
      "OPEN " + LoggingDeploymentListener.UPDATE_ENVIRONMENT,
      String.format("LOG Started deployment of application %s version %s to %s.", FAKE_APP_NAME, FAKE_APP_VERSION, FAKE_ENV_NAME),
      "LOG Waiting for deployment finish.",
      String.format("PROGRESS Waiting for deployment on environment %s.", FAKE_ENV_NAME),
      String.format("LOG Version %s was deployed to %s successfully.", FAKE_APP_VERSION, FAKE_ENV_NAME),
      String.format("STATUS_TEXT Version %s was deployed to %s successfully.", FAKE_APP_VERSION, FAKE_ENV_NAME),
      "CLOSE " + LoggingDeploymentListener.UPDATE_ENVIRONMENT);
  }

  @Test
  public void deployment_progress() throws Exception {
    create().deploymentInProgress(FAKE_ENV_NAME);
    assertLog(String.format("PROGRESS Waiting for deployment on environment %s.", FAKE_ENV_NAME));
  }

  @Test
  public void deployment_succeeded() throws Exception {
    create().deploymentSucceeded(FAKE_ENV_NAME, FAKE_APP_VERSION);
    assertLog(
      String.format("LOG Version %s was deployed to %s successfully.", FAKE_APP_VERSION, FAKE_ENV_NAME),
      String.format("STATUS_TEXT Version %s was deployed to %s successfully.", FAKE_APP_VERSION, FAKE_ENV_NAME),
      "CLOSE " + LoggingDeploymentListener.UPDATE_ENVIRONMENT);
  }

  @Test
  public void deployment_failed_timeout() throws Exception {
    create().deploymentFailed(FAKE_APP_NAME, FAKE_ENV_NAME, FAKE_APP_VERSION, true, null);
    assertLog(
      "PROBLEM identity: 3569038 type: ELASTICBEANSTALK_TIMEOUT descr: Timeout exceeded, ",
      "CLOSE " + LoggingDeploymentListener.UPDATE_ENVIRONMENT);
  }

  @Test
  public void deployment_failed() throws Exception {
    create().deploymentFailed(FAKE_APP_NAME, FAKE_ENV_NAME, FAKE_APP_VERSION, false, createError("abc", "Some error message"));
    assertLog(
      "ERR Associated error: Some error message",
      "ERR Error severity: abc",
      "PROBLEM identity: 79914740 type: ELASTICBEANSTALK_FAILURE descr: Error, : Some error message",
      "CLOSE " + LoggingDeploymentListener.UPDATE_ENVIRONMENT);
  }

  @Test
  public void deployment_exception_type() throws Exception {
    create().exception(new AWSException("Some exception message", null, AWSException.EXCEPTION_BUILD_PROBLEM_TYPE, null));
    assertLog(
      "ERR Some exception message",
      "PROBLEM identity: 2086901196 type: ELASTICBEANSTALK_EXCEPTION descr: Some exception message",
      "CLOSE " + LoggingDeploymentListener.UPDATE_ENVIRONMENT);
  }

  @Test
  public void deployment_exception_description_type() throws Exception {
    create().exception(new AWSException("Some exception message", null, AWSException.CLIENT_PROBLEM_TYPE, "Some exception details"));
    assertLog(
      "ERR Some exception message",
      "ERR Some exception details",
      "PROBLEM identity: 2086901196 type: ELASTICBEANSTALK_CLIENT descr: Some exception message",
      "CLOSE " + LoggingDeploymentListener.UPDATE_ENVIRONMENT);
  }

  @Override
  protected void performAfterTestVerification() {
    // override parent behaviour
  }

  @NotNull
  private AWSClient.Listener.ErrorInfo createError(@Nullable String severity, @Nullable String message) {
    final AWSClient.Listener.ErrorInfo errorInfo = new AWSClient.Listener.ErrorInfo();
    errorInfo.severity = severity;
    errorInfo.message = message;
    return errorInfo;
  }

  @NotNull
  private LoggingDeploymentListener create() {
    return new LoggingDeploymentListener(Collections.<String, String>emptyMap(),
      new NullBuildProgressLogger(),
      "fake_checkout_dir") {
      @Override
      protected void log(@NotNull String message) {
        logMessage("LOG " + message);
      }

      @Override
      protected void err(@NotNull String message) {
        logMessage("ERR " + message);
      }

      @Override
      protected void open(@NotNull String block) {
        logMessage("OPEN " + block);
      }

      @Override
      protected void close(@NotNull String block) {
        logMessage("CLOSE " + block);
      }

      @Override
      protected void problem(int identity, @NotNull String type, @NotNull String descr) {
        logMessage("PROBLEM identity: " + identity + " type: " + type + " descr: " + descr);
      }

      @Override
      protected void progress(@NotNull String message) {
        logMessage("PROGRESS " + message);
      }

      @Override
      protected void statusText(@NotNull String text) {
        logMessage("STATUS_TEXT " + text);
      }
    };
  }
}
