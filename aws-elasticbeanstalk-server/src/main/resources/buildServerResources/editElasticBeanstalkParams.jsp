<%--
  ~ Copyright 2000-2016 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<style type="text/css">
    .runnerFormTable span.facultativeNote {
        display: none;
    }

    .runnerFormTable span.facultativeAsterix {
        display: none;
    }
</style>


<%@include file="paramsConstants.jspf"%>

<jsp:include page="editAWSCommonParams.jsp"/>

<l:settingsGroup title="Version Location">
    <tr>
        <th><label for="${bucket_name_param}">${bucket_name_label}: <l:star/></label></th>
        <td><props:textProperty name="${bucket_name_param}" className="longField" maxlength="256"/><a href="http://console.aws.amazon.com/s3" target="_blank">Open S3 Console</a>
            <span class="smallNote">Existing S3 bucket name</span><span class="error" id="error_${bucket_name_param}"></span>
        </td>
    </tr>
    <tr>
        <th><label for="${s3_object_key_param}">${s3_object_key_label}: <l:star/></label></th>
        <td><props:textProperty name="${s3_object_key_param}" className="longField" maxlength="256"/>
            <span id="${s3_object_key_param}_mandatory_note" class="smallNote facultativeNote">Unique path inside the bucket</span>
            <span class="error" id="error_${s3_object_key_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

<tr class="groupingTitle">
    <td colspan="2">ElasticBeanstalk Application</td>
</tr>
<tr>
    <th><label for="${env_name_param}">${env_name_label}: <l:star/></label></th>
    <td><props:textProperty name="${env_name_param}" className="longField" maxlength="256"/><a href="http://console.aws.amazon.com/elasticbeanstalk" target="_blank">Open ElasticBeanstalk Console</a>
        <span class="smallNote">Pre-configured ElasticBeanstalk environment name</span><span class="error" id="error_${env_name_param}"></span>
    </td>
</tr>
<tr>
    <th><label for="${app_name_param}">${app_name_label}: <l:star/></label></th>
    <td><props:textProperty name="${app_name_param}" className="longField" maxlength="256"/><a href="http://console.aws.amazon.com/elasticbeanstalk" target="_blank">Open ElasticBeanstalk Console</a>
        <span class="smallNote">Pre-configured ElasticBeanstalk application name</span><span class="error" id="error_${app_name_param}"></span>
    </td>
</tr>
<tr>
    <th><label for="${app_version_param}">${app_version_label}: <l:star/></label></th>
    <td><props:textProperty name="${app_version_param}" className="longField" maxlength="256"/>
        <span class="smallNote">Version Label to Publish</span><span class="error" id="error_${app_version_param}"></span>
    </td>
</tr>
<tr>
    <th><label for="${app_version_skip_dupe_param}">${app_version_skip_dupe_label}: </label></th>
    <td><props:checkboxProperty name="${app_version_skip_dupe_param}" uncheckedValue="false" /></td>
</tr>
<tr>
    <th><label for="${wait_flag_param}">${wait_flag_label}: </label></th>
    <td><props:checkboxProperty name="${wait_flag_param}" uncheckedValue="false" onclick="elasticBeanstalkWaitFlag()"/></td>
</tr>
<tr id="${wait_timeout_param}_row">
    <th><label for="${wait_timeout_param}">${wait_timeout_label}: <l:star/></label></th>
    <td><props:textProperty name="${wait_timeout_param}" maxlength="256"/>
        <span class="smallNote">Build will fail if the timeout is exceeded</span><span class="error" id="error_${wait_timeout_param}"></span>
    </td>
</tr>

<script type="application/javascript">
    window.elasticBeanstalkWaitFlag = function () {
        if ($j('#${wait_flag_param}').is(':checked')) {
            BS.Util.show('${wait_timeout_param}_row');
        } else {
            BS.Util.hide('${wait_timeout_param}_row');
        }
    };
    elasticBeanstalkWaitFlag();
</script>
