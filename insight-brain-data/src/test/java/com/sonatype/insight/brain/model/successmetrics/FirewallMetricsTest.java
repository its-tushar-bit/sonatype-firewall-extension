/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import java.util.*;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FirewallMetricsTest
{
  @Test
  public void testCreationMetric() {
    Date testDate = new GregorianCalendar(2023, Calendar.OCTOBER, 1).getTime();
    FirewallMetrics firewallMetrics = new FirewallMetrics(testDate, FirewallMetricsName.COMPONENTS_QUARANTINED,
        1);

    assertThat(firewallMetrics.getMetricsDate()).isEqualTo(testDate);
    assertThat(firewallMetrics.getMetricsName()).isEqualTo(FirewallMetricsName.COMPONENTS_QUARANTINED);
    assertThat(firewallMetrics.getMetricsValue()).isEqualTo(1);
  }
}
