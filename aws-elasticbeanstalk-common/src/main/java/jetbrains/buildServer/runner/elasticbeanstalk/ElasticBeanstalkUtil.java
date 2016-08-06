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

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

import static jetbrains.buildServer.runner.elasticbeanstalk.ElasticBeanstalkConstants.WAIT_FLAG_PARAM;

final class ElasticBeanstalkUtil {

  static boolean isDeploymentWaitEnabled(@NotNull Map<String, String> params) {
    return Boolean.parseBoolean(params.get(WAIT_FLAG_PARAM));
  }

  @NotNull
  private static String normalize(@NotNull String path, boolean isFromPart) {
    path = StringUtil.removeLeadingSlash(FileUtil.toSystemIndependentName(path));
    final String suffix = isFromPart && path.endsWith("/") ? "/" : StringUtil.EMPTY;
    path = FileUtil.normalizeRelativePath(path);
    return StringUtil.isEmpty(path) && isFromPart ? "**" : path + suffix;
  }

  @NotNull
  static String printStrings(@NotNull Collection<String> strings) {
    if (strings.isEmpty()) return StringUtil.EMPTY;
    final StringBuilder sb = new StringBuilder();
    for (String s : strings) sb.append(s).append("\n");
    return sb.toString();
  }

  /* Borrowed from jetbrains.buildServer.util.StringUtil.truncateStringValueWithDotsAtCenter
  * TODO: use the original util method */
  @Contract("null, _ -> null")
  public static String truncateStringValueWithDotsAtCenter(@Nullable final String str, final int maxLength) {
    if (str == null) return null;
    if (str.length() > maxLength) {
      String start = str.substring(0, maxLength / 2);
      String dots = "...";
      String end = str.substring(str.length() - maxLength + start.length() + dots.length(), str.length());
      return start + dots + end;
    } else {
      return str;
    }
  }
}
