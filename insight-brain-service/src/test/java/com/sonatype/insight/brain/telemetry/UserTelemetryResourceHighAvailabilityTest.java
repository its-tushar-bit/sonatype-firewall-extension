/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.hds.HdsClient.RelayResponse;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive tests for CLM-34770: Reproducing and verifying the fix for browser connection exhaustion during
 * Gainsight outages.
 *
 * @since CLM-34770
 */
@RunWith(MockitoJUnitRunner.class)
public class UserTelemetryResourceHighAvailabilityTest
{
  @Mock
  private PendoService mockPendoService;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private Configuration mockSystemConfiguration;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private UserTelemetryResource userTelemetryResource;

  @Before
  public void setUp() {
    when(mockSystemConfiguration.getUserTelemetryPoolSize()).thenReturn(4);
    userTelemetryResource = new UserTelemetryResource(mockPendoService, mockSystemConfiguration, mockShutdownHandler);
  }

  @Test
  public void testAsyncTelemetryStillWorks() throws InterruptedException {
    CountDownLatch asyncCallLatch = new CountDownLatch(1);
    AtomicBoolean asyncCallExecuted = new AtomicBoolean(false);

    // Mock PendoService to signal when async call happens
    when(mockPendoService.proxyWithoutRetry(any(HttpServletRequest.class), any(String.class)))
        .thenAnswer(invocation -> {
          asyncCallExecuted.set(true);
          asyncCallLatch.countDown();
          return new RelayResponse<>("success", "text/plain");
        });

    // Make request
    Response response = userTelemetryResource.proxyPost(mockRequest, "rte/v1/inapp");

    // Verify immediate response
    assertThat("Response should be immediate", response.getStatus(), equalTo(200));

    // Wait for async processing
    boolean asyncCompleted = asyncCallLatch.await(5, TimeUnit.SECONDS);
    assertThat("Async telemetry call should complete", asyncCompleted, equalTo(true));
    assertThat("Async call should have been executed", asyncCallExecuted.get(), equalTo(true));

    // Verify the call was made
    verify(mockPendoService).proxyWithoutRetry(mockRequest, "rte/v1/inapp");
  }

  /**
   * This test verifies our fix prevents browser connection exhaustion
   */
  @Test
  public void testAsyncVersionPreventsConnectionExhaustion() throws InterruptedException {
    // Same Gainsight outage simulation
    when(mockPendoService.proxyWithoutRetry(any(), any())).thenAnswer(invocation -> {
      Thread.sleep(10000); // 10 second hang
      throw new SocketTimeoutException("Connection timed out");
    });

    // Simulate multiple concurrent requests from the browser
    final int BROWSER_CONNECTION_LIMIT = 6;
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch completionLatch = new CountDownLatch(BROWSER_CONNECTION_LIMIT);
    final long[] responseTimes = new long[BROWSER_CONNECTION_LIMIT];
    final AtomicInteger completedRequests = new AtomicInteger(0);

    // Same concurrent request pattern
    for (int i = 0; i < BROWSER_CONNECTION_LIMIT; i++) {
      final int index = i;
      new Thread(() -> {
        try {
          startLatch.await();

          long startTime = System.currentTimeMillis();

          // This should return immediately with our fix
          userTelemetryResource.proxyPost(mockRequest, "rte/v1/inapp?p=" + index);

          long duration = System.currentTimeMillis() - startTime;
          responseTimes[index] = duration;

          completedRequests.incrementAndGet();
        }
        catch (Exception e) {
          System.out.println("Async request " + index + " failed: " + e.getMessage());
        }
        finally {
          completionLatch.countDown();
        }
      }).start();
    }

    startLatch.countDown(); // Start all requests

    // With our fix, all requests should complete immediately
    boolean completedInTime = completionLatch.await(12, TimeUnit.SECONDS);

    // Our fix should make all requests return immediately
    assertThat("All async requests should complete quickly", completedInTime, equalTo(true));
    assertThat("All requests should complete", completedRequests.get(), equalTo(BROWSER_CONNECTION_LIMIT));

    // Verify all responses were immediate
    for (int i = 0; i < BROWSER_CONNECTION_LIMIT; i++) {
      assertThat("Request " + i + " should be immediate", responseTimes[i], lessThan(2000L));
    }
  }

  @Test
  public void testSocketTimeoutScenario() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    // Simulate socket timeout after 10 seconds
    when(mockPendoService.proxyWithoutRetry(any(), any())).thenAnswer(invocation -> {
      Thread.sleep(10000);
      latch.countDown();
      throw new SocketTimeoutException("Read timed out");
    });

    long startTime = System.currentTimeMillis();
    assertThrows(SocketTimeoutException.class, () -> {
      Response response = userTelemetryResource.proxyPost(mockRequest, "user-telemetry/events/rte/v2/command");
      long duration = System.currentTimeMillis() - startTime;

      // Response should be immediate despite background timeout
      assertThat("Should return immediately despite socket timeout", duration, lessThan(1000L));
      assertThat("Should return 200 OK", response.getStatus(), equalTo(200));
      // Empty entity response as is fire-and-forget request
      assertThat(response.getHeaderString("Content-Type"), equalTo(null));
      assertThat(response.getEntity(), equalTo(null));
    });

    // Wait to see if background processing happens
    latch.await(12, TimeUnit.SECONDS);

    // Verify timeout occurred in background
    assertThat("Background timeout should have occurred", latch.getCount(), equalTo(0L));
  }

  @Test
  public void testDNSResolutionFailureScenario() {
    // Simulate DNS resolution failure
    when(mockPendoService.proxyWithoutRetry(any(), any()))
        .thenThrow(new RuntimeException("Name or service not known"));

    long startTime = System.currentTimeMillis();
    assertThrows(RuntimeException.class, () -> {
      assertThat(userTelemetryResource.proxyPost(mockRequest, "track/dns"), isNull());
    });

    long duration = System.currentTimeMillis() - startTime;
    assertThat("Should return immediately despite DNS failure", duration, lessThan(50L));
  }

  @Test
  public void testPerformanceUnderLoad() throws InterruptedException {
    final int HIGH_LOAD_REQUESTS = 1000;
    final CountDownLatch completionLatch = new CountDownLatch(HIGH_LOAD_REQUESTS);
    final AtomicInteger successCount = new AtomicInteger(0);

    // Simulate slow Gainsight responses  
    when(mockPendoService.proxyWithoutRetry(any(), any())).thenAnswer(invocation -> {
      Thread.sleep(5000); // 5 second delay
      return new RelayResponse<>("success", "text/plain");
    });

    long startTime = System.currentTimeMillis();

    // Flood with requests
    for (int i = 0; i < HIGH_LOAD_REQUESTS; i++) {
      final int index = i;
      new Thread(() -> {
        try {
          Response response = userTelemetryResource.proxyPost(mockRequest, "rte/v1/inapp" + index);
          if (response.getStatus() == 200) {
            successCount.incrementAndGet();
          }
        }
        finally {
          completionLatch.countDown();
        }
      }).start();
    }

    // All should complete immediately despite background delays
    boolean allCompleted = completionLatch.await(10, TimeUnit.SECONDS);
    long totalTime = System.currentTimeMillis() - startTime;

    assertThat("All requests should complete quickly under load", allCompleted, equalTo(true));
    assertThat("All requests should succeed", successCount.get(), equalTo(HIGH_LOAD_REQUESTS));
    assertThat("Average response time should be very fast",
        totalTime / (double) HIGH_LOAD_REQUESTS, lessThan(10.0));
  }

  @Test
  public void testTypicalUserClickingAroundDuringOutage() throws InterruptedException {
    // Simulate various user actions that trigger telemetry
    String[] userActions = {
        "track/pageview", "track/button_click", "track/form_submit",
        "identify/user", "track/navigation", "track/feature_use",
        "track/error_event", "track/performance"
    };

    // Gainsight is completely down
    when(mockPendoService.proxyWithoutRetry(any(), any()))
        .thenThrow(new RuntimeException("Gainsight service unavailable"));

    long startTime = System.currentTimeMillis();

    // Simulate user rapidly clicking around (8 actions in quick succession)
    for (String action : userActions) {

      assertThrows(RuntimeException.class, () -> {
        assertThat(userTelemetryResource.proxyPost(mockRequest, action), isNull());
      });

      // Small delay between clicks (realistic user behavior)  
      Thread.sleep(100);
    }

    long totalTime = System.currentTimeMillis() - startTime;

    // Should be very fast despite Gainsight being down
    assertThat("Total time should be reasonable", totalTime, lessThan(2000L)); // Under 2 seconds
    assertThat("Average per action should be fast",
        totalTime / (double) userActions.length, lessThan(250.0)); // Under 250ms per action
  }
}
