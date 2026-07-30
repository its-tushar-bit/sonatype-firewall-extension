/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Date;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyRepositoryComponentTest
{
  @Test
  public void testIsQuarantined() {
    final ProxyRepositoryComponent component = new ProxyRepositoryComponent();
    assertThat(component.isQuarantined()).isFalse();

    final Date now = new Date();
    component.setQuarantineTime(now);
    assertThat(component.isQuarantined()).as("Only 'QuarantineTime' == quarantined.").isTrue();

    component.setUnquarantineTimeForManualRelease(now);
    assertThat(component.isQuarantined()).as("Both 'Un/QuarantineTime' != quarantined.").isFalse();
  }
}
