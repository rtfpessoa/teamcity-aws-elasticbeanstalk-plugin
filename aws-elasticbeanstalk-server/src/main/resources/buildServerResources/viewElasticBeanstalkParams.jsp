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

<%@include file="paramsConstants.jspf"%>

<jsp:include page="viewAWSCommonParams.jsp"/>

<div class="parameter">
    ${bucket_name_label}: <props:displayValue name="${bucket_name_param}" emptyValue="empty"/>
</div>

<c:set var="s3_object_key" value="${propertiesBean.properties[s3_object_key_param]}"/>
<c:if test="${not empty s3_object_key}">
    <div class="parameter">
            ${s3_object_key_label}: <props:displayValue name="${s3_object_key_param}" emptyValue="empty"/>
    </div>
</c:if>

<div class="parameter">
    ${env_name_label}: <props:displayValue name="${env_name_param}" emptyValue="empty"/>
</div>

<div class="parameter">
    ${app_name_label}: <props:displayValue name="${app_name_param}" emptyValue="empty"/>
</div>

<div class="parameter">
    ${app_version_label}: <props:displayValue name="${app_version_param}" emptyValue="empty"/>
</div>

<div class="parameter">
    ${wait_flag_label}: <strong><props:displayCheckboxValue name="${wait_flag_param}"/></strong>
</div>

<c:set var="wait_flag" value="${propertiesBean.properties[wait_flag_param]}"/>
<c:if test="${empty wait_flag or ('true' eq wait_flag)}">
    <div class="parameter">
        ${wait_timeout_label}: <props:displayValue name="${wait_timeout_param}" emptyValue="empty"/>
    </div>
</c:if>
