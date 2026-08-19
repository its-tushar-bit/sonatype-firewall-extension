/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that parses IQ client/integration user agent string
 */
public class ClientUserAgentUtil
{
  // complete examples:
  // Nexus_IQ_IDEA/1.0.1-01 (Java 1.8.0_76-release; Mac OS X 10.11.6; IDEA IU-162.1447.26)
  // Sonatype_CLM_CI_Jenkins/3.13 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)
  // partial example:
  // GitLab_Nexus_IQ_CLI/1.133.0-SNAPSHOT (Java 1.8.0_322; Linux 5.10.76-linuxkit)

  private static final Pattern PATTERN_IQ_CLIENT_USER_AGENT = Pattern.compile(
      "([^/]{1,100})\\/([^\\s]{1,50})\\s" +
          "\\(([^;\\s]{1,50})\\s([^\\s;]{1,50});\\s([^\\s]{1,50})\\s([^;]{1,50})(?:;\\s(.+))?\\)");

  public static UserAgent parse(String ua) {
    if (ua == null) {
      return null;
    }

    Matcher matcher = PATTERN_IQ_CLIENT_USER_AGENT.matcher(ua);
    if (!matcher.matches()) {
      return null;
    }

    int index = 1;
    UserAgent agent = new UserAgent();
    agent.client = matcher.group(index++);
    agent.clientVersion = matcher.group(index++);
    agent.runtime = matcher.group(index++);
    agent.runtimeVersion = matcher.group(index++);
    agent.os = matcher.group(index++);
    agent.osVersion = matcher.group(index++);
    agent.other = matcher.group(index);
    return agent;
  }

  public static class UserAgent
  {
    public String client;

    public String clientVersion;

    public String runtime;

    public String runtimeVersion;

    public String os;

    public String osVersion;

    public String other;
  }
}
