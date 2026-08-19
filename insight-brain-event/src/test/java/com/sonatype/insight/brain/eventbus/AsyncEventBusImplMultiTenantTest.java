/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.eventbus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import com.google.common.eventbus.Subscribe;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenantNameFromTest;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenantAndInvalidate;
import static org.assertj.core.api.Assertions.assertThat;

public class AsyncEventBusImplMultiTenantTest
    extends MultiTenantTestSupport
{
  private AsyncEventBusImpl asyncEventBus;

  private Map<String, List<String>> eventsByTenantSlug;

  private CountDownLatch countDownLatch;

  @BeforeEach
  public void before() {
    asyncEventBus = new AsyncEventBusImpl(500);
    asyncEventBus.register(this);
    eventsByTenantSlug = new ConcurrentHashMap<>();
  }

  @AfterEach
  public void after() {
    asyncEventBus.unregister(this);
    asyncEventBus = null;
    eventsByTenantSlug = null;
    countDownLatch = null;
  }

  @Test
  public void testPost_TenantsOnlyProcessOwnEvents() throws Exception {
    int eventsPerTenant = 2000;
    String tenant1Name = createTenantNameFromTest(currentMethodName());
    eventsByTenantSlug.put(tenant1Name, new CopyOnWriteArrayList<>());
    String tenant2Name = createTenantNameFromTest(currentMethodName());
    eventsByTenantSlug.put(tenant2Name, new CopyOnWriteArrayList<>());
    countDownLatch = new CountDownLatch(eventsPerTenant * 2);

    new Thread(() -> {
      for (int i = 0; i < eventsPerTenant; i++) {
        int finalI = i;
        ThreadContext.bind(securityManager);
        testAsTenantAndInvalidate(tenant1Name, t -> asyncEventBus.post("t1e" + finalI));
      }
    }).start();
    new Thread(() -> {
      for (int i = 0; i < eventsPerTenant; i++) {
        int finalI = i;
        ThreadContext.bind(securityManager);
        testAsTenantAndInvalidate(tenant2Name, t -> asyncEventBus.post("t2e" + finalI));
      }
    }).start();

    assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(eventsByTenantSlug.get(tenant1Name)).allMatch(s -> s.startsWith("t1"));
    assertThat(eventsByTenantSlug.get(tenant2Name)).allMatch(s -> s.startsWith("t2"));
  }

  @SuppressWarnings("unused")
  @Subscribe
  public void handler(String event) throws Exception {
    Tenant tenant = TenantThreadLocal.getTenant();
    eventsByTenantSlug.get(tenant.tenantSlug).add(event);
    countDownLatch.countDown();
  }
}
