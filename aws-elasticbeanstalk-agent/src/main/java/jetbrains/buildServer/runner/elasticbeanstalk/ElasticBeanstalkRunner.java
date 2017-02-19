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

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.messages.ErrorData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.runner.elasticbeanstalk.ElasticBeanstalkConstants.*;
import static jetbrains.buildServer.util.StringUtil.nullIfEmpty;
import static jetbrains.buildServer.util.amazon.AWSCommonParams.*;

public class ElasticBeanstalkRunner implements AgentBuildRunner {
  @NotNull
  @Override
  public BuildProcess createBuildProcess(@NotNull final AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {
    return new SyncBuildProcessAdapter() {
      @NotNull
      @Override
      protected BuildFinishedStatus runImpl() throws RunBuildException {

        final Map<String, String> runnerParameters = validateParams();
        final Map<String, String> configParameters = context.getConfigParameters();

        final Mutable m = new Mutable(configParameters);
        m.problemOccurred = false;
        m.s3ObjectVersion = nullIfEmpty(configParameters.get(S3_OBJECT_VERSION_CONFIG_PARAM));

        final AWSClient awsClient = createAWSClient(runnerParameters, runningBuild).withListener(
          new LoggingDeploymentListener(runnerParameters, runningBuild.getBuildLogger(), runningBuild.getCheckoutDirectory().getAbsolutePath()));

        final String s3BucketName = runnerParameters.get(S3_BUCKET_NAME_PARAM);
        String s3ObjectKey = runnerParameters.get(S3_OBJECT_KEY_PARAM);

        final String applicationName = runnerParameters.get(APP_NAME_PARAM);
        final String environmentName = runnerParameters.get(ENV_NAME_PARAM);
        final String versionLabel = runnerParameters.get(APP_VERSION_PARAM);
        final Boolean skipDuplicateVersions = Boolean.valueOf(runnerParameters.get(APP_VERSION_SKIP_DUPE_PARAM));

        if (!m.problemOccurred && !isInterrupted()) {
          awsClient.createApplicationVersion(applicationName, versionLabel, skipDuplicateVersions, s3BucketName, s3ObjectKey);
        }

        if (!m.problemOccurred && !isInterrupted()) {
          if (ElasticBeanstalkUtil.isDeploymentWaitEnabled(runnerParameters)) {
            awsClient.updateEnvironmentAndWait(environmentName, versionLabel,
              Integer.parseInt(runnerParameters.get(WAIT_TIMEOUT_SEC_PARAM)),
              getIntegerOrDefault(configParameters.get(WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM), WAIT_POLL_INTERVAL_SEC_DEFAULT));
          } else {
            awsClient.updateEnvironment(environmentName, versionLabel);
          }
        }

        return m.problemOccurred ? BuildFinishedStatus.FINISHED_WITH_PROBLEMS : BuildFinishedStatus.FINISHED_SUCCESS;
      }

      @NotNull
      private Map<String, String> validateParams() throws RunBuildException {
        final Map<String, String> runnerParameters = context.getRunnerParameters();
        final Map<String, String> invalids = ParametersValidator.validateRuntime(runnerParameters, context.getConfigParameters(), runningBuild.getCheckoutDirectory());
        if (invalids.isEmpty()) return runnerParameters;
        throw new ElasticBeanstalkRunnerException(ElasticBeanstalkUtil.printStrings(invalids.values()), null);
      }
    };
  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new AgentBuildRunnerInfo() {
      @NotNull
      @Override
      public String getType() {
        return RUNNER_TYPE;
      }

      @Override
      public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
        return true;
      }
    };
  }

  @NotNull
  private AWSClient createAWSClient(final Map<String, String> runnerParameters, @NotNull final AgentRunningBuild runningBuild) {
    final Map<String, String> params = new HashMap<>(runnerParameters);
    params.put(TEMP_CREDENTIALS_SESSION_NAME_PARAM, runningBuild.getBuildTypeExternalId() + runningBuild.getBuildId());
    if (ElasticBeanstalkUtil.isDeploymentWaitEnabled(runnerParameters)) {
      params.put(TEMP_CREDENTIALS_DURATION_SEC_PARAM, String.valueOf(2 * Integer.parseInt(runnerParameters.get(WAIT_TIMEOUT_SEC_PARAM))));
    }

    return new AWSClient(createAWSClients(params, true));
  }

  private static class ElasticBeanstalkRunnerException extends RunBuildException {
    ElasticBeanstalkRunnerException(@NotNull String message, @Nullable Throwable cause) {
      super(message, cause, ErrorData.BUILD_RUNNER_ERROR_TYPE);
      this.setLogStacktrace(false);
    }
  }

  private class Mutable {
    Mutable(@NotNull Map<String, String> configParameters) {
      problemOccurred = false;
      s3ObjectVersion = nullIfEmpty(configParameters.get(S3_OBJECT_VERSION_CONFIG_PARAM));
    }

    boolean problemOccurred;
    String s3ObjectVersion;
  }
}
