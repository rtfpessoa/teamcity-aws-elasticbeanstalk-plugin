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

import jetbrains.buildServer.parameters.ReferencesResolverUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.runner.elasticbeanstalk.ElasticBeanstalkConstants.*;
import static jetbrains.buildServer.runner.elasticbeanstalk.ElasticBeanstalkUtil.isDeploymentWaitEnabled;

final class ParametersValidator {
  /**
   * Must be used for parameters validation during the build
   * Returns map from parameter name to invalidity reason
   */
  @NotNull
  static Map<String, String> validateRuntime(@NotNull Map<String, String> runnerParams, @NotNull Map<String, String> configParams, @NotNull File checkoutDir) {
    final Map<String, String> invalids = new HashMap<String, String>(validate(runnerParams, true));

    if (isDeploymentWaitEnabled(runnerParams)) {
      final String waitIntervalSec = configParams.get(WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM);
      if (StringUtil.isNotEmpty(waitIntervalSec)) {
        validatePositiveInteger(invalids, waitIntervalSec, WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM, WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM, true);
      }
    }

    return Collections.unmodifiableMap(invalids);
  }

  /**
   * Returns map from parameter name to invalidity reason
   */
  @NotNull
  static Map<String, String> validateSettings(@NotNull Map<String, String> params) {
    return validate(params, false);
  }

  private static Map<String, String> validate(@NotNull Map<String, String> runnerParams, boolean runtime) {
    final Map<String, String> invalids = new HashMap<String, String>();

    invalids.putAll(AWSCommonParams.validate(runnerParams, !runtime));

    final String s3BucketName = runnerParams.get(S3_BUCKET_NAME_PARAM);
    if (StringUtil.isEmptyOrSpaces(s3BucketName)) {
      invalids.put(S3_BUCKET_NAME_PARAM, S3_BUCKET_NAME_LABEL + " mustn't be empty");
    } else if (s3BucketName.contains("/")) {
      invalids.put(S3_BUCKET_NAME_PARAM, S3_BUCKET_NAME_LABEL + " mustn't contain / characters. For addressing folders use " + S3_OBJECT_KEY_LABEL + " parameter");
    }

    final String s3ObjectKey = runnerParams.get(S3_OBJECT_KEY_PARAM);
    if (StringUtil.isEmptyOrSpaces(s3ObjectKey)) {
      invalids.put(S3_OBJECT_KEY_PARAM, S3_OBJECT_KEY_LABEL + " mustn't be empty");
    } else {
      validateS3Key(invalids, s3ObjectKey, S3_OBJECT_KEY_PARAM, S3_OBJECT_KEY_LABEL, runtime);
    }

    if (StringUtil.isEmptyOrSpaces(runnerParams.get(ENV_NAME_PARAM))) {
      invalids.put(ENV_NAME_PARAM, ENV_NAME_LABEL + " mustn't be empty");
    }

    if (StringUtil.isEmptyOrSpaces(runnerParams.get(APP_NAME_PARAM))) {
      invalids.put(APP_NAME_PARAM, APP_NAME_LABEL + " mustn't be empty");
    }

    if (StringUtil.isEmptyOrSpaces(runnerParams.get(APP_VERSION_PARAM))) {
      invalids.put(APP_VERSION_PARAM, APP_VERSION_LABEL + " mustn't be empty");
    }

    if (isDeploymentWaitEnabled(runnerParams)) {
      final String waitTimeoutSec = runnerParams.get(WAIT_TIMEOUT_SEC_PARAM);
      if (StringUtil.isEmptyOrSpaces(waitTimeoutSec)) {
        invalids.put(WAIT_TIMEOUT_SEC_PARAM, WAIT_TIMEOUT_SEC_LABEL + " mustn't be empty");
      } else {
        validatePositiveInteger(invalids, waitTimeoutSec, WAIT_TIMEOUT_SEC_PARAM, WAIT_TIMEOUT_SEC_LABEL, runtime);
      }
    }

    return invalids;
  }

  private static void validatePositiveInteger(@NotNull Map<String, String> invalids, @NotNull String param, @NotNull String key, @NotNull String name, boolean runtime) {
    if (!isReference(param, runtime)) {
      try {
        final int i = Integer.parseInt(param);
        if (i <= 0) {
          invalids.put(key, name + " must be a positive integer value");
        }
      } catch (NumberFormatException e) {
        invalids.put(key, name + " must be a positive integer value");
      }
    }
  }

  private static void validateS3Key(@NotNull Map<String, String> invalids, @NotNull String param, @NotNull String key, @NotNull String name, boolean runtime) {
    if (!isReference(param, runtime)) {
      if (!param.matches("[a-zA-Z_0-9!\\-\\.*'()/,:-]*")) {
        invalids.put(key, name + " must contain only safe characters");
      }
    }
  }

  private static boolean isReference(@NotNull String param, boolean runtime) {
    return ReferencesResolverUtil.containsReference(param) && !runtime;
  }
}
