/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.google.common.eventbus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class AccessibleDispatcherTest
{
  private Map<Integer, List<Object>> subscriberEvents;

  @BeforeEach
  public void before() {
    subscriberEvents = new HashMap<>();
  }

  @Test
  public void testDispatch_NullEvent() {
    TenantAwareDispatcher accessibleDispatcher = new TenantAwareDispatcher();

    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> accessibleDispatcher.dispatch(null, null));
  }

  @Test
  public void testDispatch() throws Exception {
    Object event1 = new Object();
    Object event2 = new Object();
    Subscriber subscriber1 = createSubscriber(0);
    Subscriber subscriber2 = createSubscriber(1);
    List<Subscriber> subscribers = Arrays.asList(subscriber1, subscriber2);
    TenantAwareDispatcher accessibleDispatcher = new TenantAwareDispatcher();

    accessibleDispatcher.dispatch(event1, subscribers.iterator());
    accessibleDispatcher.dispatch(event2, subscribers.iterator());

    assertThat(subscriberEvents.get(0)).containsExactly(event1, event2);
    assertThat(subscriberEvents.get(1)).containsExactly(event1, event2);
  }

  private Subscriber createSubscriber(int number) throws Exception {
    EventBus eventBus = new EventBus("default");
    Method method = AccessibleDispatcherTest.class.getMethod("handler" + number, Object.class);
    return Subscriber.create(eventBus, this, method);
  }

  @SuppressWarnings("unused")
  public void handler0(Object event) {
    subscriberEvents.computeIfAbsent(0, key -> new ArrayList<>()).add(event);
  }

  @SuppressWarnings("unused")
  public void handler1(Object event) {
    subscriberEvents.computeIfAbsent(1, key -> new ArrayList<>()).add(event);
  }
}
