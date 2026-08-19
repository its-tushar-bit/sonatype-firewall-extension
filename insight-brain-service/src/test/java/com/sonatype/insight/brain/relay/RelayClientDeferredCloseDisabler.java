/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Test-only: disables {@link RelayClient}'s 15-minute deferred-close on every RelayClient bean as it is created, so
 * its non-daemon "RelayHttpClientCloser" thread does not sleep for minutes holding the RelayClient (and therefore the
 * whole application context) reachable in reused test forks (CLM-40425). Lives in the {@code relay} package so it can
 * set the package-private field without reflection or any production change.
 */
public class RelayClientDeferredCloseDisabler
    implements BeanPostProcessor
{
  @Override
  public Object postProcessAfterInitialization(final Object bean, final String beanName) {
    if (bean instanceof RelayClient) {
      ((RelayClient) bean).waitToCloseOldClients = false;
    }
    return bean;
  }
}
