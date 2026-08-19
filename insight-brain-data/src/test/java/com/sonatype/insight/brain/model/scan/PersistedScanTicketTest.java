/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.scan;

import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistedScanTicketTest
{
  @Test
  public void testInitialization() {
    Date now = new Date();

    assertThat(new PersistedScanTicket().getCreateTime()).isAfterOrEqualTo(now).isCloseTo(now, 5000);
  }
}
