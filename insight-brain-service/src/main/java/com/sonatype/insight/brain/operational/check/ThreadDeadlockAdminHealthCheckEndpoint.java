/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;

@Named
@Singleton
public class ThreadDeadlockAdminHealthCheckEndpoint
    implements AdminHealthCheckEndpoint
{
  @Override
  public String getName() {
    return "ThreadDeadlock";
  }

  @Override
  public String getPath() {
    return "/healthcheck/threadDeadlock";
  }

  @Override
  public HealthCheckResponse getHealthCheckResponse() {
    HealthCheckResponse healthCheckResponse = new HealthCheckResponse();
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
    if (ArrayUtils.isNotEmpty(deadlockedThreads)) {
      healthCheckResponse.setHealthy(false);
      healthCheckResponse.setContent(getContent(threadMXBean, deadlockedThreads));
    }
    else {
      healthCheckResponse.setHealthy(true);
    }
    return healthCheckResponse;
  }

  private String getContent(ThreadMXBean threadMXBean, long[] deadlockedThreads) {
    ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreads);
    StringBuilder content = new StringBuilder();
    content.append("Thread deadlock detected:\n");
    for (ThreadInfo threadInfo : threadInfos) {
      content.append("Thread with name ")
          .append(threadInfo.getThreadName())
          .append(" and ID ")
          .append(threadInfo.getThreadId())
          .append(".\n");
      for (StackTraceElement stackTraceElement : threadInfo.getStackTrace()) {
        content.append(stackTraceElement.toString()).append("\n");
      }
    }
    return content.toString();
  }
}
