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

public interface ElasticBeanstalkConstants {
  String RUNNER_TYPE = "aws.elasticBeanstalk";
  String RUNNER_DISPLAY_NAME = "AWS Elastic Beanstalk";
  String RUNNER_DESCR = "Create, and update application version using AWS ElasticBeanstalk";

  String DEPLOYMENT_ID_BUILD_CONFIG_PARAM = "elasticbeanstalk.deployment.id";
  String S3_OBJECT_VERSION_CONFIG_PARAM = "elasticbeanstalk.revision.s3.version";

  String EDIT_PARAMS_HTML = "editElasticBeanstalkParams.html";
  String VIEW_PARAMS_HTML = "viewElasticBeanstalkParams.html";
  String EDIT_PARAMS_JSP = "editElasticBeanstalkParams.jsp";
  String VIEW_PARAMS_JSP = "viewElasticBeanstalkParams.jsp";

  String TIMEOUT_BUILD_PROBLEM_TYPE = "ELASTICBEANSTALK_TIMEOUT";
  String FAILURE_BUILD_PROBLEM_TYPE = "ELASTICBEANSTALK_FAILURE";

  String S3_BUCKET_NAME_PARAM = "elasticbeanstalk_s3_bucket_name";
  String S3_BUCKET_NAME_LABEL = "S3 bucket";

  String S3_OBJECT_KEY_PARAM = "elasticbeanstalk_s3_object_key";
  String S3_OBJECT_KEY_LABEL = "S3 object key";

  String ENV_NAME_PARAM = "elasticbeanstalk_environment_name";
  String ENV_NAME_LABEL = "Environment Name";

  String APP_NAME_PARAM = "elasticbeanstalk_appname_label";
  String APP_NAME_LABEL = "Application Name";

  String APP_VERSION_PARAM = "elasticbeanstalk_version_label";
  String APP_VERSION_LABEL = "Application Version";

  String APP_VERSION_SKIP_DUPE_PARAM = "elasticbeanstalk_version_skip_dupe_label";
  String APP_VERSION_SKIP_DUPE_LABEL = "Skip Upload If Application Version Already Exists?";

  String WAIT_FLAG_PARAM = "elasticbeanstalk_wait";
  String WAIT_FLAG_LABEL = "Wait for deployment finish";

  String WAIT_TIMEOUT_SEC_PARAM = "elasticbeanstalk_wait_timeout_sec";
  String WAIT_TIMEOUT_SEC_LABEL = "Timeout (seconds)";

  String WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM = "elasticbeanstalk.wait.poll.interval.sec";
  int WAIT_POLL_INTERVAL_SEC_DEFAULT = 20;

  String STATUS_IS_UNKNOWN = "status is unknown";
}
