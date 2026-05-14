/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class FirewallQuarantineHdsClientTest
{
  @Test
  public void testValidatePoolSize_throwsOnZero() {
    assertThatThrownBy(() -> FirewallQuarantineHdsClient.validatePoolSize(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nexus.firewall.hds.quarantine.pool.size must be between 1 and 50")
        .hasMessageContaining("got: 0");
  }

  @Test
  public void testValidatePoolSize_throwsOnNegative() {
    assertThatThrownBy(() -> FirewallQuarantineHdsClient.validatePoolSize(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("got: -1");
  }

  @Test
  public void testValidatePoolSize_throwsAboveMax() {
    assertThatThrownBy(() -> FirewallQuarantineHdsClient.validatePoolSize(51))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("got: 51");
  }

  @Test
  public void testValidatePoolSize_acceptsMinimum() {
    assertThat(FirewallQuarantineHdsClient.validatePoolSize(1)).isEqualTo(1);
  }

  @Test
  public void testValidatePoolSize_acceptsDefault() {
    assertThat(FirewallQuarantineHdsClient.validatePoolSize(20)).isEqualTo(20);
  }

  @Test
  public void testValidatePoolSize_acceptsMaximum() {
    assertThat(FirewallQuarantineHdsClient.validatePoolSize(50)).isEqualTo(50);
  }
}
