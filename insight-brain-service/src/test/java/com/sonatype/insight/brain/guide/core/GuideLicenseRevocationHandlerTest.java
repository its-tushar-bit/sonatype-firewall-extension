/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GuideLicenseRevocationHandlerTest
{
  @Mock
  private CLMLicenseManager clmLicenseManager;

  @Mock
  private ProductLicense productLicense;

  private GuideLicenseRevocationHandler handler;

  @Before
  public void setUp() {
    handler = new GuideLicenseRevocationHandler(clmLicenseManager, productLicense);
  }

  @Test
  public void singlePaymentRequired_triggersExactlyOneLoadLicense() {
    handler.onPaymentRequired("rest/search/components/detail");

    verify(clmLicenseManager, times(1)).loadLicense();
  }

  @Test
  public void concurrentPaymentRequireds_collapseToSingleRefresh() throws Exception {
    int threads = 20;
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch loadInFlight = new CountDownLatch(1);
    CountDownLatch loadAllowedToFinish = new CountDownLatch(1);

    // Make loadLicense() block until we explicitly release it, so all 20 threads contend on
    // the single-flight CAS while the first one is "in flight".
    doAnswer(invocation -> {
      loadInFlight.countDown();
      loadAllowedToFinish.await();
      return null;
    }).when(clmLicenseManager).loadLicense();

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      for (int i = 0; i < threads; i++) {
        pool.submit(() -> {
          ready.countDown();
          start.await();
          handler.onPaymentRequired("rest/search/components/detail");
          return null;
        });
      }

      ready.await();
      start.countDown();
      loadInFlight.await();
      loadAllowedToFinish.countDown();
    }
    finally {
      pool.shutdown();
      pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    verify(clmLicenseManager, times(1)).loadLicense();
  }

  @Test
  public void secondCallWithinDebounceWindow_skipsRefresh() {
    handler.onPaymentRequired("rest/search/components/detail");
    verify(clmLicenseManager, times(1)).loadLicense();

    // Second call immediately after — well within 60s — must NOT trigger another refresh.
    handler.onPaymentRequired("rest/search/components/detail");
    verify(clmLicenseManager, times(1)).loadLicense();
  }

  @Test
  public void callAfterDebounceWindow_triggersFreshRefresh() {
    handler.onPaymentRequired("rest/search/components/detail");
    // Pretend 90 seconds have passed.
    handler.setLastRefreshCompletedAtMillisForTesting(System.currentTimeMillis() - 90_000L);

    handler.onPaymentRequired("rest/search/components/detail");

    verify(clmLicenseManager, times(2)).loadLicense();
  }

  @Test
  public void failedRefreshDoesNotUpdateDebounceTimestamp() {
    doThrow(new RuntimeException("HDS unreachable"))
        .doNothing()
        .when(clmLicenseManager)
        .loadLicense();

    assertThatThrownBy(() -> handler.onPaymentRequired("rest/search/components/detail"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("HDS unreachable");

    // Even though zero ms passed, the previous attempt failed, so debounce must NOT skip:
    // the next call retries the refresh.
    handler.onPaymentRequired("rest/search/components/detail");

    verify(clmLicenseManager, times(2)).loadLicense();
  }

  @Test
  public void successfulRefresh_emitsInfoLogWithFeatureDiff() {
    when(productLicense.getFingerprint()).thenReturn("fp-test");
    // Configure productLicense to flip GUIDE_SEARCH=true → false across the loadLicense call.
    AtomicBoolean refreshed = new AtomicBoolean(false);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenAnswer(inv -> !refreshed.get());
    doAnswer(inv -> {
      refreshed.set(true);
      return null;
    }).when(clmLicenseManager).loadLicense();

    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GuideLicenseRevocationHandler.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      handler.onPaymentRequired("rest/search/components/detail");
    }
    finally {
      logger.detachAppender(appender);
    }

    ILoggingEvent event = appender.list.stream()
        .filter(e -> e.getLevel() == Level.INFO)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected an INFO log line"));
    String formatted = event.getFormattedMessage();
    assertThat(formatted)
        .contains("rest/search/components/detail")
        .contains("removedFeatures=")
        .contains("GUIDE_SEARCH")
        .contains("licenseFingerprint=fp-test");
  }
}
