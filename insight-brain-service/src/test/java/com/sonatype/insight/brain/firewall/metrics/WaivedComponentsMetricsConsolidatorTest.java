/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.firewall.metrics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.util.DateUtil;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.WAIVED_COMPONENTS;
import static com.sonatype.insight.brain.utils.DateConverter.toLocalDate;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class WaivedComponentsMetricsConsolidatorTest
    extends AbstractComponentTest
{
  @Inject
  private FirewallMetricsDAO firewallMetricsDAOTest;

  @Inject
  private WaivedComponentMetricsConsolidator consolidator;

  private final List<Date> testLastUpdateDates = new ArrayList<>();

  private Repository repository1;

  private Repository repository2;

  @Before
  public void init() {
    initTestDates();
  }

  @Test
  public void consolidateTest_keepAllRecords() {
    initTestData();
    List<FirewallMetrics> allMetrics = firewallMetricsDAOTest.getAll();
    // Check initial data length
    assertThat(allMetrics).hasSize(5);
    // Check a record that is at least 1 year old
    LocalDate overAYearOldDateStr = LocalDate.of(2019, 12, 30);
    assertThat(findMetricByDate(overAYearOldDateStr,
        allMetrics).getMetricsDate()).isEqualTo(overAYearOldDateStr);

    consolidator.consolidate();

    allMetrics = firewallMetricsDAOTest.getAll();
    // Check the data length after consolidation
    assertThat(allMetrics).hasSize(5);
  }

  @Test
  public void consolidateTest_insertRecord_mostRecentRecordNotFound() {
    createRepositories();

    Date now = new Date();
    Date oneYearAgo = DateUtils.addHours(DateUtils.addYears(now, -1), 2);
    Date moreThanOneYearAgo = DateUtils.addDays(oneYearAgo, -2);

    Policy policy1 = tempEntity.newPolicy(repository1);
    Policy policy2 = tempEntity.newPolicy(repository2);

    tempEntity.newWaiver("hash1", policy1.getId(), repository1.getId(), emptyList(), "now-waived-1", now);
    tempEntity.newWaiver("hash2", policy2.getId(), repository2.getId(), emptyList(), "now-waived-2", now);
    tempEntity.newWaiver("hash3", policy2.getId(), repository2.getId(), emptyList(), "1-year-ago-waived", oneYearAgo);
    tempEntity.newWaiver("hash4", policy1.getId(), repository1.getId(), emptyList(), "+1-year-ago-waived",
        moreThanOneYearAgo);

    consolidator.consolidate();

    List<FirewallMetrics> allMetrics = new ArrayList<>(firewallMetricsDAOTest.getAll());

    assertThat(allMetrics).hasSize(2);
    allMetrics.sort(Comparator.comparing(FirewallMetrics::getMetricsDate));

    assertThat(allMetrics)
        .extracting(FirewallMetrics::getMetricsName)
        .containsOnly(WAIVED_COMPONENTS);

    assertThat(allMetrics)
        .extracting(FirewallMetrics::getMetricsDate)
        .containsExactly(toLocalDate(oneYearAgo), toLocalDate(now));

    assertThat(allMetrics)
        .extracting(FirewallMetrics::getMetricsValue)
        .containsExactly(1, 2);
  }

  @Test
  public void consolidateTest_insertRecord_mostRecentRecordFound() {
    createRepositories();

    Date fiveDaysAgo = DateUtils.addDays(new Date(), -5);
    Date threeDaysAgo = DateUtils.addDays(new Date(), -3);
    Date tenDaysAgo = DateUtils.addDays(new Date(), -10);

    Policy policy1 = tempEntity.newPolicy(repository1);
    Policy policy2 = tempEntity.newPolicy(repository2);

    tempEntity.newWaiver("hash3", policy2.getId(), repository2.getId(), emptyList(), "1-year-ago-waived", fiveDaysAgo);
    tempEntity.newWaiver("hash4", policy1.getId(), repository1.getId(), emptyList(), "+1-year-ago-waived",
        threeDaysAgo);

    LocalDate today = LocalDate.now();
    FirewallMetrics metric = new FirewallMetrics(today, WAIVED_COMPONENTS, 2);
    metric.setMetricsLastUpdatedAt(tenDaysAgo);
    firewallMetricsDAOTest.insert(metric);

    consolidator.consolidate();

    List<FirewallMetrics> allMetrics = new ArrayList<>(firewallMetricsDAOTest.getAll());

    assertThat(allMetrics).hasSize(3);

    allMetrics.sort(Comparator.comparing(FirewallMetrics::getMetricsDate));

    assertThat(allMetrics)
        .extracting(FirewallMetrics::getMetricsName)
        .containsOnly(WAIVED_COMPONENTS);
  }

  private FirewallMetrics findMetricByDate(
      LocalDate needle,
      List<FirewallMetrics> haystack)
  {
    List<FirewallMetrics> found = haystack.stream()
        .filter(fm -> fm.getMetricsDate().isEqual(needle))
        .collect(Collectors.toList());
    return !found.isEmpty() ? found.get(0) : null;
  }

  private void initTestDates() {
    Date today = DateUtil.now();
    Date lastYearFromToday = DateUtils.addMonths(today, -12);

    Date date2019 = new GregorianCalendar(2019, Calendar.DECEMBER, 30, 18,
        23, 12).getTime();
    Date date2015 = new GregorianCalendar(2015, Calendar.DECEMBER, 30, 18,
        23, 12).getTime();
    Date date2017 = new GregorianCalendar(2017, Calendar.DECEMBER, 30, 18,
        23, 12).getTime();
    testLastUpdateDates.add(date2019);
    testLastUpdateDates.add(today);
    testLastUpdateDates.add(lastYearFromToday);
    testLastUpdateDates.add(date2015);
    testLastUpdateDates.add(date2017);
  }

  private void initTestData() {
    testLastUpdateDates.forEach(date -> tempEntity.newFirewallMetrics(
        WAIVED_COMPONENTS,
        testLastUpdateDates.indexOf(date) + 1,
        date,
        toLocalDate(date)));
  }

  private void createRepositories() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    tempEntity.newHostedRepository(repositoryManager, "hostedRepo", ComponentIdentifier.FORMAT_NPM, true);
    repository1 = tempEntity.newRepository();
    repository2 = tempEntity.newRepository();
  }
}
