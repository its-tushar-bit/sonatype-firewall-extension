/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

public class PolicyMonitorUnitTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyMonitor policyMonitor;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Override
  public void configure(Binder binder) {
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
    super.configure(binder);
  }

  @Test
  public void testPolicyMonitor() {
    verify(mockShutdownHandler).add(policyMonitor.getApplicationMonitorForkJoinPool());
  }
}
