/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.*;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
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

  @Test
  public void testMetricsValueByName() {
    Date testDate1 = new GregorianCalendar(2023, Calendar.OCTOBER, 1).getTime();
    Date testDate2 = new GregorianCalendar(2023, Calendar.JANUARY, 1).getTime();
    Date testDate3 = new GregorianCalendar(2023, Calendar.OCTOBER, 7).getTime();
    Date testDate4 = new GregorianCalendar(2023, Calendar.OCTOBER, 8).getTime();
    Date testDate5 = new GregorianCalendar(2023, Calendar.OCTOBER, 9).getTime();
    Date testDate6 = new GregorianCalendar(2023, Calendar.OCTOBER, 10).getTime();
    Date testDate7 = new GregorianCalendar(2023, Calendar.OCTOBER, 11).getTime();
    Date testDate8 = new GregorianCalendar(2023, Calendar.OCTOBER, 12).getTime();
    Date testDate9 = new GregorianCalendar(2023, Calendar.OCTOBER, 13).getTime();

    FirewallMetrics firewallMetrics1 = new FirewallMetrics(testDate1, FirewallMetricsName.COMPONENTS_QUARANTINED,
        30);
    FirewallMetrics firewallMetrics2 = new FirewallMetrics(testDate2, FirewallMetricsName.COMPONENTS_QUARANTINED,
        20);
    FirewallMetrics firewallMetrics3 = new FirewallMetrics(testDate3, FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED,
        10);
    FirewallMetrics firewallMetrics4 = new FirewallMetrics(testDate4, FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED,
        30);
    FirewallMetrics firewallMetrics5 =
        new FirewallMetrics(testDate5, FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY, 20);
    FirewallMetrics firewallMetrics6 = new FirewallMetrics(testDate6, FirewallMetricsName.WAIVED_COMPONENTS,
        10);
    FirewallMetrics firewallMetrics7 = new FirewallMetrics(testDate7, FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED,
        10);
    FirewallMetrics firewallMetrics8 = new FirewallMetrics(testDate8, FirewallMetricsName.WAIVED_COMPONENTS,
        10);
    FirewallMetrics firewallMetrics9 = new FirewallMetrics(testDate9, FirewallMetricsName.COMPONENTS_AUTO_RELEASED,
        17);

    firewallMetrics1.setMetricsLastUpdatedAt(testDate1);
    firewallMetrics2.setMetricsLastUpdatedAt(testDate2);
    firewallMetrics3.setMetricsLastUpdatedAt(testDate3);
    firewallMetrics4.setMetricsLastUpdatedAt(testDate4);
    firewallMetrics5.setMetricsLastUpdatedAt(testDate5);
    firewallMetrics6.setMetricsLastUpdatedAt(testDate6);
    firewallMetrics7.setMetricsLastUpdatedAt(testDate7);
    firewallMetrics8.setMetricsLastUpdatedAt(testDate8);
    firewallMetrics9.setMetricsLastUpdatedAt(testDate9);

    dao.insert(firewallMetrics1);
    dao.insert(firewallMetrics2);
    dao.insert(firewallMetrics3);
    dao.insert(firewallMetrics4);
    dao.insert(firewallMetrics5);
    dao.insert(firewallMetrics6);
    dao.insert(firewallMetrics7);
    dao.insert(firewallMetrics8);
    dao.insert(firewallMetrics9);

    // read
    Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> firewallMetrics = dao.getMetricsValueByName();
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO1 =
        firewallMetrics.get(FirewallMetricsName.COMPONENTS_QUARANTINED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO2 =
        firewallMetrics.get(FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO3 =
        firewallMetrics.get(FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO4 =
        firewallMetrics.get(FirewallMetricsName.WAIVED_COMPONENTS);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO5 =
        firewallMetrics.get(FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO6 =
        firewallMetrics.get(FirewallMetricsName.COMPONENTS_AUTO_RELEASED);

    assertThat(firewallMetrics.size()).isEqualTo(6);
    assertThat(apiFirewallMetricsResultDTO1.getFirewallMetricsValue()).isEqualTo(50);
    assertThat(apiFirewallMetricsResultDTO2.getFirewallMetricsValue()).isEqualTo(10);
    assertThat(apiFirewallMetricsResultDTO3.getFirewallMetricsValue()).isEqualTo(40);
    assertThat(apiFirewallMetricsResultDTO4.getFirewallMetricsValue()).isEqualTo(20);
    assertThat(apiFirewallMetricsResultDTO5.getFirewallMetricsValue()).isEqualTo(20);
    assertThat(apiFirewallMetricsResultDTO6.getFirewallMetricsValue()).isEqualTo(17);

    assertThat(apiFirewallMetricsResultDTO1.getLatestUpdatedTime()).isEqualTo(testDate1);
    assertThat(apiFirewallMetricsResultDTO2.getLatestUpdatedTime()).isEqualTo(testDate3);
    assertThat(apiFirewallMetricsResultDTO3.getLatestUpdatedTime()).isEqualTo(testDate7);
    assertThat(apiFirewallMetricsResultDTO4.getLatestUpdatedTime()).isEqualTo(testDate8);
    assertThat(apiFirewallMetricsResultDTO5.getLatestUpdatedTime()).isEqualTo(testDate5);
    assertThat(apiFirewallMetricsResultDTO6.getLatestUpdatedTime()).isEqualTo(testDate9);
  }
}
