/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.*;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FirewallMetricsDAOTest
    extends AbstractDbDAOTest
{
  private final FirewallMetricsDAO dao = new FirewallMetricsDAO();

  @Test
  public void testCRUD() {
    Date testDate = new GregorianCalendar(2023, Calendar.OCTOBER, 1).getTime();
    FirewallMetrics firewallMetrics = new FirewallMetrics(testDate, FirewallMetricsName.COMPONENTS_QUARANTINED,
        1);

    // create
    assertThat(firewallMetrics.getId()).isNull();
    dao.insert(firewallMetrics);
    assertThat(firewallMetrics.getId()).isNotNull();

    // read
    firewallMetrics = dao.getById(firewallMetrics.getId());

    assertThat(firewallMetrics.getMetricsDate()).isEqualTo(testDate);
    assertThat(firewallMetrics.getMetricsName()).isEqualTo(FirewallMetricsName.COMPONENTS_QUARANTINED);
    assertThat(firewallMetrics.getMetricsValue()).isEqualTo(1);

    // update
    firewallMetrics.setMetricsValue(99);
    dao.update(firewallMetrics);

    firewallMetrics = dao.getById(firewallMetrics.getId());

    assertThat(firewallMetrics).isNotNull();
    assertThat(firewallMetrics.getMetricsName()).isEqualTo(FirewallMetricsName.COMPONENTS_QUARANTINED);
    assertThat(firewallMetrics.getMetricsValue()).isEqualTo(99);

    // delete
    String id = firewallMetrics.getId();
    dao.delete(firewallMetrics);

    assertThat(dao.getById(id)).isNull();
  }
}
