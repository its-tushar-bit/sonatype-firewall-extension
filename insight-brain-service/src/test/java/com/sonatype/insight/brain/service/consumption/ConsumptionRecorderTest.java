/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.consumption.ConsumptionEventDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ConsumptionRecorderTest
{
  private static final int QUEUE_CAPACITY_PER_TENANT = 10_000;

  private final CopyOnWriteArrayList<ConsumptionEvent> writtenEvents = new CopyOnWriteArrayList<>();

  @Mock
  private ConsumptionEventDAO mockDao;

  @Mock
  private SystemConfigurationPropertyDAO mockSystemConfigDao;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private ConsumptionRecorder recorder;

  private Object originalSystemConfigDao;

  @Before
  public void setUp() throws Exception {
    injectMockSystemConfigurationPropertyDAO();
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(true);

    writtenEvents.clear();
    lenient().doAnswer(invocation -> {
      writtenEvents.add(invocation.getArgument(0));
      return null;
    }).when(mockDao).recordEvent(any(ConsumptionEvent.class));

    recorder = new ConsumptionRecorder(mockDao, mockShutdownHandler);
  }

  @After
  public void tearDown() throws Exception {
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(false);
    ConsumptionContext.clear();
    restoreSystemConfigurationPropertyDAO();
  }

  @Test
  public void record_writesEventViaDao() {
    ConsumptionEvent event = createTestEvent();

    recorder.record(event);

    await().atMost(Duration.ofSeconds(2))
        .untilAsserted(() -> assertThat(writtenEvents).contains(event));
  }

  @Test
  public void record_multipleEvents_allPersisted() {
    ConsumptionEvent event1 = createTestEvent("org-1");
    ConsumptionEvent event2 = createTestEvent("org-2");

    recorder.record(event1);
    recorder.record(event2);

    await().atMost(Duration.ofSeconds(2))
        .untilAsserted(() -> assertThat(writtenEvents).contains(event1, event2));
  }

  @Test
  public void record_skipsWhenFeatureFlagDisabled() {
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(false);

    ConsumptionEvent event = createTestEvent();
    recorder.record(event);

    verify(mockDao, never()).recordEvent(any(ConsumptionEvent.class));
    assertThat(recorder.getQueueDepth()).isZero();
  }

  @Test
  public void record_dropsWhenQueueFull() throws Exception {
    long baselineDropped = recorder.getDroppedEventCount();

    CountDownLatch blockWorker = new CountDownLatch(1);
    lenient().doAnswer(invocation -> {
      blockWorker.await(5, TimeUnit.SECONDS);
      writtenEvents.add(invocation.getArgument(0));
      return null;
    }).when(mockDao).recordEvent(any(ConsumptionEvent.class));

    for (int i = 0; i < QUEUE_CAPACITY_PER_TENANT + 1; i++) {
      recorder.record(createTestEvent("org-" + i));
    }

    await().atMost(Duration.ofSeconds(2))
        .untilAsserted(() -> assertThat(recorder.getQueueDepth()).isEqualTo(QUEUE_CAPACITY_PER_TENANT));

    recorder.record(createTestEvent("overflow-org"));

    assertThat(recorder.getDroppedEventCount()).isEqualTo(baselineDropped + 1);

    blockWorker.countDown();
  }

  @Test
  public void record_swallowsDaoFailure() {
    AtomicInteger insertCalls = new AtomicInteger(0);
    lenient().doAnswer(invocation -> {
      insertCalls.incrementAndGet();
      throw new RuntimeException("Database connection failed");
    }).when(mockDao).recordEvent(any(ConsumptionEvent.class));

    recorder.record(createTestEvent());

    await().atMost(Duration.ofSeconds(2))
        .untilAsserted(() -> assertThat(insertCalls.get()).isGreaterThanOrEqualTo(1));
    recorder.record(createTestEvent("another-org"));
    await().atMost(Duration.ofSeconds(2))
        .untilAsserted(() -> assertThat(insertCalls.get()).isGreaterThanOrEqualTo(2));
  }

  @Test
  public void droppedEventCount_isolatedPerTenant() {
    Tenant tenantA = testAsNewTenant("recorder-isolation-a", t -> {
      recorder.getExecutors().get().shutdown();
      recorder.record(createTestEvent("org-a"));
      assertThat(recorder.getDroppedEventCount()).isEqualTo(1L);
    });

    testAsNewTenant("recorder-isolation-b", t -> {
      assertThat(recorder.getDroppedEventCount()).isEqualTo(0L);
      recorder.getExecutors().get().shutdown();
      recorder.record(createTestEvent("org-b"));
      assertThat(recorder.getDroppedEventCount()).isEqualTo(1L);
    });

    testAsTenant(tenantA, t -> assertThat(recorder.getDroppedEventCount()).isEqualTo(1L));
  }

  private final AtomicReference<SystemConfigurationProperty> featureFlagState = new AtomicReference<>(null);

  private void injectMockSystemConfigurationPropertyDAO() throws Exception {
    Field field = SystemConfigurationPropertyFeature.class.getDeclaredField("systemConfigurationPropertyDAO");
    field.setAccessible(true);
    originalSystemConfigDao = field.get(null);
    field.set(null, mockSystemConfigDao);

    TransactionContext mockTx = mock(TransactionContext.class);
    lenient().when(mockSystemConfigDao.createTransactionContext()).thenReturn(mockTx);
    lenient().when(mockTx.dsl()).thenReturn(DSL.using(SQLDialect.POSTGRES));

    lenient().when(mockSystemConfigDao.getByName(any(), any()))
        .thenAnswer(invocation -> featureFlagState.get());

    lenient().doAnswer(invocation -> {
      String value = invocation.getArgument(2);
      if (value == null) {
        featureFlagState.set(null);
      }
      else {
        featureFlagState.set(new SystemConfigurationProperty(invocation.getArgument(1), value));
      }
      return null;
    }).when(mockSystemConfigDao).set(any(), any(), any());
  }

  private void restoreSystemConfigurationPropertyDAO() throws Exception {
    Field field = SystemConfigurationPropertyFeature.class.getDeclaredField("systemConfigurationPropertyDAO");
    field.setAccessible(true);
    field.set(null, originalSystemConfigDao);
  }

  private ConsumptionEvent createTestEvent() {
    return createTestEvent("test-org-id");
  }

  private ConsumptionEvent createTestEvent(String orgId) {
    ConsumptionEvent event = new ConsumptionEvent();
    event.setOrgId(orgId);
    event.setEventTimestamp(Instant.now());
    event.setActivityType(ActivityType.COMPONENT_DETAILS);
    event.setSource("TEST");
    event.setTier("STANDARD");
    event.setBillingMonth(LocalDate.now());
    event.setComponentCount(1);
    return event;
  }
}
