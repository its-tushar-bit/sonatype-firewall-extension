/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.operational.check.AdminHealthCheckEndpoint.HealthCheckResponse;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ThreadDeadlockAdminHealthCheckEndpointTest
    extends AbstractComponentTest
{
  @Inject
  private ThreadDeadlockAdminHealthCheckEndpoint threadDeadlockAdminHealthCheckEndpoint;

  @Test
  public void testGetName() {
    assertThat(threadDeadlockAdminHealthCheckEndpoint.getName()).isEqualTo("ThreadDeadlock");
  }

  @Test
  public void testGetPath() {
    assertThat(threadDeadlockAdminHealthCheckEndpoint.getPath()).isEqualTo("/healthcheck/threadDeadlock");
  }

  @Test
  public void testIsHealthy_Deadlock() {
    testIsHealthy(new long[]{1}, new HealthCheckResponse(false,
        "Thread deadlock detected:\nThread with name name1 and ID 1.\nstack1\nstack2\n"));
  }

  @Test
  public void testIsHealthy_NoDeadlock() {
    testIsHealthy(null, new HealthCheckResponse(true));
    testIsHealthy(new long[]{}, new HealthCheckResponse(true));
  }

  private void testIsHealthy(long[] deadlockedThreads, HealthCheckResponse expectedHealthCheckResponse) {
    try (MockedStatic<ManagementFactory> mockManagementFactory = mockStatic(ManagementFactory.class)) {
      ThreadMXBean mockThreadMXBean = mock(ThreadMXBean.class);
      when(mockThreadMXBean.findDeadlockedThreads()).thenReturn(deadlockedThreads);
      lenient().when(mockThreadMXBean.getThreadInfo(any())).thenAnswer(invocation -> {
        long[] input = invocation.getArgument(0);
        ThreadInfo[] result = new ThreadInfo[input.length];
        for (int i = 0; i < result.length; i++) {
          ThreadInfo mockThreadInfo = mock(ThreadInfo.class);
          lenient().when(mockThreadInfo.getThreadName()).thenReturn("name" + input[i]);
          lenient().when(mockThreadInfo.getThreadId()).thenReturn(input[i]);
          StackTraceElement mockStackTraceElement1 = mock(StackTraceElement.class);
          lenient().when(mockStackTraceElement1.toString()).thenReturn("stack1");
          StackTraceElement mockStackTraceElement2 = mock(StackTraceElement.class);
          lenient().when(mockStackTraceElement2.toString()).thenReturn("stack2");
          lenient().when(mockThreadInfo.getStackTrace()).thenReturn(
              new StackTraceElement[]{mockStackTraceElement1, mockStackTraceElement2});
          result[i] = mockThreadInfo;
        }
        return result;
      });
      mockManagementFactory.when(ManagementFactory::getThreadMXBean).thenReturn(mockThreadMXBean);

      assertThat(threadDeadlockAdminHealthCheckEndpoint.getHealthCheckResponse()).usingRecursiveComparison()
          .isEqualTo(expectedHealthCheckResponse);
    }
  }
}
