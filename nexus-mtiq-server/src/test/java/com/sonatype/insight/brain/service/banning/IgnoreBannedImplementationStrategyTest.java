/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import com.sonatype.insight.brain.service.DefaultTenantManagedInitializer;

import com.google.inject.Binder;
import com.google.inject.Key;
import org.eclipse.sisu.wire.Wiring;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IgnoreBannedImplementationStrategyTest
{
  private IgnoreBannedImplementationStrategy underTest;

  @Mock
  private Binder binder;

  @Before
  public void before() {
    when(binder.withSource(any())).thenReturn(binder);
  }

  @Test
  public void test_WiringNotPerformedIfBanned() {
    BannedImplementationService banned = new BannedImplementationService();
    underTest = new IgnoreBannedImplementationStrategy(banned);

    Wiring wiring = underTest.wiring(binder);
    Key<DefaultTenantManagedInitializer> key = Key.get(DefaultTenantManagedInitializer.class);
    assertThat(wiring.wire(key)).isFalse();

    verify(binder, never()).bind(key);
  }

  @Test
  public void test_WiringPerformedWhenNotBanned() {
    BannedImplementationService banned = new BannedImplementationService();
    underTest = new IgnoreBannedImplementationStrategy(banned);

    Wiring wiring = underTest.wiring(binder);

    Key<IgnoreBannedImplementationStrategyTest> key = Key.get(IgnoreBannedImplementationStrategyTest.class);
    try {
      wiring.wire(key);
    }
    catch (Exception ignored) {
      // Throws an exception because binder mock is not set up correctly
    }

    // As part of creating LocatorWiring a call to binder is made proving
    verify(binder).bind(key);
  }
}
