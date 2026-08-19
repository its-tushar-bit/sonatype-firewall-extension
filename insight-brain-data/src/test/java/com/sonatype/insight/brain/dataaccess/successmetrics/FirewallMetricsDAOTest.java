/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_AUTO_RELEASED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_QUARANTINED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.WAIVED_COMPONENTS;
import static com.sonatype.insight.brain.utils.DateConverter.toLocalDate;
import static com.sonatype.insight.brain.utils.DateConverter.toDate;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallMetricsDAOTest
    extends AbstractDbDAOTest
{
  private FirewallMetricsDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createFirewallMetricsDAO();
  }

  private final List<Date> testLastUpdateDates = new ArrayList<>();

  @BeforeEach
  public void init() {
    initTestDates();
  }

  @Test
  public void testCRUD() {
    LocalDate testDate = LocalDate.of(2023, Month.OCTOBER, 1);
    FirewallMetrics firewallMetrics = new FirewallMetrics(testDate, COMPONENTS_QUARANTINED, 1);

    // create
    assertThat(firewallMetrics.getId()).isNull();
    dao.insert(firewallMetrics);
    assertThat(firewallMetrics.getId()).isNotNull();

    // read
    firewallMetrics = dao.getById(firewallMetrics.getId());

    assertThat(firewallMetrics.getMetricsDate()).isEqualTo(testDate);
    assertThat(firewallMetrics.getMetricsName()).isEqualTo(COMPONENTS_QUARANTINED);
    assertThat(firewallMetrics.getMetricsValue()).isEqualTo(1);

    // update
    firewallMetrics.setMetricsValue(99);
    dao.update(firewallMetrics);

    firewallMetrics = dao.getById(firewallMetrics.getId());

    assertThat(firewallMetrics).isNotNull();
    assertThat(firewallMetrics.getMetricsName()).isEqualTo(COMPONENTS_QUARANTINED);
    assertThat(firewallMetrics.getMetricsValue()).isEqualTo(99);

    // delete
    String id = firewallMetrics.getId();
    dao.delete(firewallMetrics);

    assertThat(dao.getById(id)).isNull();
  }

  @Test
  public void getMostRecentLastUpdatedAtDateByName() {
    initTestData();
    Date mostRecentDate = dao.getMostRecentLastUpdatedAtDateByName(
        FirewallMetricsName.COMPONENTS_QUARANTINED);
    assertThat(mostRecentDate)
        .hasYear(2022)
        .hasMonth(2)
        .hasDayOfMonth(2)
        .hasHourOfDay(18)
        .hasMinute(4)
        .hasSecond(13);
  }

  private void initTestDates() {
    Date date2019 = new GregorianCalendar(2019, Calendar.JANUARY, 1, 8, 3, 12).getTime();
    Date date2022 = new GregorianCalendar(2022, Calendar.FEBRUARY, 2, 18,
        4, 13).getTime();
    Date date2015 = new GregorianCalendar(2015, Calendar.MARCH, 3, 9,
        15, 14).getTime();
    Date date2017 = new GregorianCalendar(2017, Calendar.APRIL, 4, 22,
        23, 15).getTime();
    testLastUpdateDates.add(date2019);
    testLastUpdateDates.add(date2022);
    testLastUpdateDates.add(date2015);
    testLastUpdateDates.add(date2017);
  }

  private void initTestData() {
    testLastUpdateDates.forEach(date -> tempEntity.newFirewallMetrics(
        FirewallMetricsName.COMPONENTS_QUARANTINED,
        testLastUpdateDates.indexOf(date) + 1,
        date,
        toLocalDate(date)));
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

    // Create some dates that are less than or equal to a year old
    LocalDate testDate10 = LocalDate.now().minusDays(5);
    LocalDate testDate11 = LocalDate.now().minusWeeks(3);
    LocalDate testDate12 = LocalDate.now().minusMonths(10);
    LocalDate testDate13 = LocalDate.now().minusMonths(12);

    FirewallMetrics firewallMetrics1 = new FirewallMetrics(toLocalDate(testDate1), COMPONENTS_QUARANTINED, 30);
    FirewallMetrics firewallMetrics2 = new FirewallMetrics(toLocalDate(testDate2), COMPONENTS_QUARANTINED, 20);
    FirewallMetrics firewallMetrics3 = new FirewallMetrics(toLocalDate(testDate3), SUPPLY_CHAIN_ATTACKS_BLOCKED, 10);
    FirewallMetrics firewallMetrics4 = new FirewallMetrics(toLocalDate(testDate4), NAMESPACE_ATTACKS_BLOCKED, 30);
    FirewallMetrics firewallMetrics5 =
        new FirewallMetrics(toLocalDate(testDate5), SAFE_VERSIONS_SELECTED_AUTOMATICALLY, 20);
    FirewallMetrics firewallMetrics6 = new FirewallMetrics(toLocalDate(testDate6), WAIVED_COMPONENTS, 10);
    FirewallMetrics firewallMetrics7 = new FirewallMetrics(toLocalDate(testDate7), NAMESPACE_ATTACKS_BLOCKED, 10);
    FirewallMetrics firewallMetrics8 = new FirewallMetrics(toLocalDate(testDate8), WAIVED_COMPONENTS, 10);
    FirewallMetrics firewallMetrics9 = new FirewallMetrics(toLocalDate(testDate9), COMPONENTS_AUTO_RELEASED, 17);
    FirewallMetrics firewallMetrics10 = new FirewallMetrics(testDate10, COMPONENTS_QUARANTINED, 15);
    FirewallMetrics firewallMetrics11 = new FirewallMetrics(testDate11, SAFE_VERSIONS_SELECTED_AUTOMATICALLY, 25);
    FirewallMetrics firewallMetrics12 = new FirewallMetrics(testDate12, WAIVED_COMPONENTS, 35);
    FirewallMetrics firewallMetrics13 = new FirewallMetrics(testDate13, COMPONENTS_AUTO_RELEASED, 45);

    firewallMetrics1.setMetricsLastUpdatedAt(testDate1);
    firewallMetrics2.setMetricsLastUpdatedAt(testDate2);
    firewallMetrics3.setMetricsLastUpdatedAt(testDate3);
    firewallMetrics4.setMetricsLastUpdatedAt(testDate4);
    firewallMetrics5.setMetricsLastUpdatedAt(testDate5);
    firewallMetrics6.setMetricsLastUpdatedAt(testDate6);
    firewallMetrics7.setMetricsLastUpdatedAt(testDate7);
    firewallMetrics8.setMetricsLastUpdatedAt(testDate8);
    firewallMetrics9.setMetricsLastUpdatedAt(testDate9);
    firewallMetrics10.setMetricsLastUpdatedAt(toDate(testDate10));
    firewallMetrics11.setMetricsLastUpdatedAt(toDate(testDate11));
    firewallMetrics12.setMetricsLastUpdatedAt(toDate(testDate12));
    firewallMetrics13.setMetricsLastUpdatedAt(toDate(testDate13));

    dao.insert(firewallMetrics1);
    dao.insert(firewallMetrics2);
    dao.insert(firewallMetrics3);
    dao.insert(firewallMetrics4);
    dao.insert(firewallMetrics5);
    dao.insert(firewallMetrics6);
    dao.insert(firewallMetrics7);
    dao.insert(firewallMetrics8);
    dao.insert(firewallMetrics9);
    dao.insert(firewallMetrics10);
    dao.insert(firewallMetrics11);
    dao.insert(firewallMetrics12);
    dao.insert(firewallMetrics13);

    // read
    Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> firewallMetrics = dao.getMetricsValueByName();
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO1 = firewallMetrics.get(COMPONENTS_QUARANTINED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO2 = firewallMetrics.get(SUPPLY_CHAIN_ATTACKS_BLOCKED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO3 = firewallMetrics.get(NAMESPACE_ATTACKS_BLOCKED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO4 = firewallMetrics.get(WAIVED_COMPONENTS);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO5 =
        firewallMetrics.get(SAFE_VERSIONS_SELECTED_AUTOMATICALLY);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO6 = firewallMetrics.get(COMPONENTS_AUTO_RELEASED);

    assertThat(firewallMetrics.size()).isEqualTo(6);
    assertThat(apiFirewallMetricsResultDTO1.getFirewallMetricsValue()).isEqualTo(15);
    assertThat(apiFirewallMetricsResultDTO2.getFirewallMetricsValue()).isEqualTo(10);
    assertThat(apiFirewallMetricsResultDTO3.getFirewallMetricsValue()).isEqualTo(40);
    assertThat(apiFirewallMetricsResultDTO4.getFirewallMetricsValue()).isEqualTo(35);
    assertThat(apiFirewallMetricsResultDTO5.getFirewallMetricsValue()).isEqualTo(25);
    assertThat(apiFirewallMetricsResultDTO6.getFirewallMetricsValue()).isEqualTo(45);

    assertThat(apiFirewallMetricsResultDTO1.getLatestUpdatedTime()).isEqualTo(toDate(testDate10));
    assertThat(apiFirewallMetricsResultDTO2.getLatestUpdatedTime()).isEqualTo(testDate3);
    assertThat(apiFirewallMetricsResultDTO3.getLatestUpdatedTime()).isEqualTo(testDate7);
    assertThat(apiFirewallMetricsResultDTO4.getLatestUpdatedTime()).isEqualTo(toDate(testDate12));
    assertThat(apiFirewallMetricsResultDTO5.getLatestUpdatedTime()).isEqualTo(toDate(testDate11));
    assertThat(apiFirewallMetricsResultDTO6.getLatestUpdatedTime()).isEqualTo(toDate(testDate13));
  }

  @Test
  public void testInsertUpdateFirewallMetrics_MetricsUpdate() {
    LocalDate testDate = LocalDate.of(2023, Month.OCTOBER, 1);
    FirewallMetrics firewallMetrics1 = new FirewallMetrics(testDate, COMPONENTS_QUARANTINED, 1);
    FirewallMetrics firewallMetrics2 = new FirewallMetrics(testDate, COMPONENTS_AUTO_RELEASED, 2);
    FirewallMetrics firewallMetrics3 = new FirewallMetrics(testDate, SUPPLY_CHAIN_ATTACKS_BLOCKED, 3);

    dao.insert(firewallMetrics1);
    dao.insert(firewallMetrics2);
    dao.insert(firewallMetrics3);

    FirewallMetrics newFirewallMetrics = new FirewallMetrics(testDate, COMPONENTS_QUARANTINED, 5);

    FirewallMetrics resFirewallMetrics = dao.insertUpdateFirewallMetrics(newFirewallMetrics);
    assertThat(resFirewallMetrics.getMetricsValue()).isEqualTo(6);
    assertThat(resFirewallMetrics.getMetricsName()).isEqualTo(COMPONENTS_QUARANTINED);
  }

  @Test
  public void testInsertUpdateFirewallMetrics_MetricsInsert() {
    LocalDate testDate = LocalDate.of(2023, Month.OCTOBER, 1);
    FirewallMetrics firewallMetrics1 = new FirewallMetrics(testDate, COMPONENTS_QUARANTINED, 1);
    FirewallMetrics firewallMetrics2 = new FirewallMetrics(testDate, COMPONENTS_AUTO_RELEASED, 2);
    FirewallMetrics firewallMetrics3 = new FirewallMetrics(testDate, SUPPLY_CHAIN_ATTACKS_BLOCKED, 3);

    dao.insert(firewallMetrics2);
    dao.insert(firewallMetrics3);

    String firewallMetricsId = dao.insertUpdateFirewallMetrics(firewallMetrics1).getId();
    FirewallMetrics res = dao.getById(firewallMetricsId);
    assertThat(res).isNotNull();
    assertThat(res.getMetricsName()).isEqualTo(firewallMetrics1.getMetricsName());
    assertThat(res.getMetricsValue()).isEqualTo(firewallMetrics1.getMetricsValue());
  }

  @Test
  public void testInsertUpdateFirewallMetrics_Multithreading() {
    LocalDate date = LocalDate.now();

    IntStream.range(1, 100).parallel().forEach(i -> {
      FirewallMetrics metric = new FirewallMetrics(date, COMPONENTS_QUARANTINED, i);
      dao.insertUpdateFirewallMetrics(metric);
    });

    List<FirewallMetrics> result = dao.getAll();

    assertThat(result)
        .extracting(FirewallMetrics::getMetricsName)
        .containsOnly(FirewallMetricsName.COMPONENTS_QUARANTINED);

    assertThat(dao.getAll().get(0).getMetricsValue()).isEqualTo(4950);
  }

  @Test
  public void testGetEarliestMetricDateByName() {
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    LocalDate threeMonthsAgo = today.minusMonths(3);

    dao.insert(new FirewallMetrics(today, COMPONENTS_QUARANTINED, 1));
    dao.insert(new FirewallMetrics(yesterday, COMPONENTS_QUARANTINED, 4));
    dao.insert(new FirewallMetrics(threeMonthsAgo, NAMESPACE_ATTACKS_BLOCKED, 900));

    assertThat(dao.getEarliestMetricDateByName(COMPONENTS_QUARANTINED)).isEqualTo(yesterday);
    assertThat(dao.getEarliestMetricDateByName(NAMESPACE_ATTACKS_BLOCKED)).isEqualTo(threeMonthsAgo);
    assertThat(dao.getEarliestMetricDateByName(COMPONENTS_AUTO_RELEASED)).isNull();
    assertThat(dao.getEarliestMetricDateByName(SAFE_VERSIONS_SELECTED_AUTOMATICALLY)).isNull();
    assertThat(dao.getEarliestMetricDateByName(SUPPLY_CHAIN_ATTACKS_BLOCKED)).isNull();
    assertThat(dao.getEarliestMetricDateByName(WAIVED_COMPONENTS)).isNull();
  }
}
