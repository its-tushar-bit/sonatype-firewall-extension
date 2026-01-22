/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.firewall.metrics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.DateConverter;

import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.util.DateUtil;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.DateConverter.toLocalDate;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentsAutoReleasedMetricsConsolidatorTest
    extends AbstractComponentTest
{
  @Inject
  private FirewallMetricsDAO firewallMetricsDAOTest;

  @Inject
  private ComponentsAutoReleasedMetricsConsolidator consolidator;

  @Inject
  private RepositoryDAO repositoryDAOTest;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAOTest;

  private final List<Date> testLastUpdateDates = new ArrayList<>();

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
    // Check a record that is  at least 1 year old
    LocalDate overAYearOldDateStr = LocalDate.of(2019, 12, 30);
    assertThat(findMetricByDate(overAYearOldDateStr,
        allMetrics).getMetricsDate()
    ).isEqualTo(overAYearOldDateStr);

    consolidator.consolidate();

    allMetrics = firewallMetricsDAOTest.getAll();
    // Check the data length after consolidation
    assertThat(allMetrics).hasSize(5);
  }

  @Test
  public void consolidateTest_insertRecord() {
    LocalDate today = LocalDate.now();
    LocalDate past300 = today.plusDays( -300);
    LocalDate past301 = today.plusDays( -301);
    String past300Str = formatAsISOString(past300);
    String past301Str = formatAsISOString(past301);

    LocalDate autoReleasedComponentDateFound = today.plusDays( -30);
    initRepositoryComponentData(DateConverter.toDate(autoReleasedComponentDateFound));
    testLastUpdateDates.set(1, DateConverter.toDate(past300));
    // Most recent date
    testLastUpdateDates.set(2, DateConverter.toDate(past301));

    initTestData();

    consolidator.consolidate();

    List<FirewallMetrics> allMetrics = firewallMetricsDAOTest.getAll();
    // Check a record that is at least less than 1 year old
    assertThat(formatAsISOString(
        findMetricByDate(past300, allMetrics)
        .getMetricsDate())).isEqualTo(past300Str);
    assertThat(formatAsISOString(findMetricByDate(past301, allMetrics)
        .getMetricsDate())).isEqualTo(past301Str);
    // New auto-released component metric found
    FirewallMetrics autoReleasedComponentFoundDb =
        findMetricByDate(autoReleasedComponentDateFound, allMetrics);
    assertThat(formatAsISOString(autoReleasedComponentFoundDb.getMetricsDate()))
        .isEqualTo(formatAsISOString(autoReleasedComponentDateFound));
    // New auto-released component metric  has a count of 3
    assertThat(autoReleasedComponentFoundDb.getMetricsValue()).isEqualTo(3);
  }

  @Test
  public void consolidateTest_updateRecord() {
    LocalDate today = LocalDate.now();
    LocalDate past300 = today.plusDays( -300);
    LocalDate past301 = today.plusDays( -301);
    LocalDate past302 = today.plusDays( -302); //302 is the oldest date in this test
    String past300Str = formatAsISOString(past300);
    String past301Str = formatAsISOString(past301);
    String past302Str = formatAsISOString(past302);

    initRepositoryComponentData(DateConverter.toDate(past300));
    initRepositoryComponentData(DateConverter.toDate(past301));

    // Initial firewall metrics dates
    testLastUpdateDates.set(1, DateConverter.toDate(past301));
    // Most recent date
    testLastUpdateDates.set(2, DateConverter.toDate(past302));
    initTestData();

    // Check initial value of  past301
    List<FirewallMetrics> allMetrics = firewallMetricsDAOTest.getAll();
    FirewallMetrics fmInitDb301 = findMetricByDate(past301, allMetrics);
    assertThat(formatAsISOString(fmInitDb301.getMetricsDate())).isEqualTo(past301Str);
    assertThat(fmInitDb301.getMetricsValue()).isEqualTo(2);

    consolidator.consolidate();

    allMetrics = firewallMetricsDAOTest.getAll();

    FirewallMetrics fmDb300 = findMetricByDate(past300, allMetrics);
    assertThat(formatAsISOString(fmDb300.getMetricsDate()))
        .isEqualTo(past300Str);
    assertThat(fmDb300.getMetricsValue()).isEqualTo(3);

    FirewallMetrics fmDb301 = findMetricByDate(past301, allMetrics);
    assertThat(formatAsISOString(fmDb301.getMetricsDate()))
        .isEqualTo(past301Str);
    // This was the record updated. Initially 3 now 5
    assertThat(fmDb301.getMetricsValue()).isEqualTo(5);

    FirewallMetrics fmDb302 = findMetricByDate(past302, allMetrics);
    assertThat(formatAsISOString(fmDb302.getMetricsDate()))
        .isEqualTo(past302Str);
    assertThat(fmDb302.getMetricsValue()).isEqualTo(3);
  }

  @Test
  public void consolidateTest_updateRecordWithTodayDateCase() {
    LocalDate today = LocalDate.now();
    LocalDate past300 = today.plusDays( -300);
    String past300Str = formatAsISOString(past300);
    String todayStr = formatAsISOString(today);

    initRepositoryComponentData(DateConverter.toDate(past300));
    initRepositoryComponentData(DateConverter.toDate(today));

    testLastUpdateDates.set(1, DateConverter.toDate(past300));
    // Most recent date
    testLastUpdateDates.set(2, DateConverter.toDate(today));
    initTestData();

    // Check before consolidation
    List<FirewallMetrics> allMetrics = firewallMetricsDAOTest.getAll();
    FirewallMetrics fmInitDb300 = findMetricByDate(past300, allMetrics);
    assertThat(formatAsISOString(fmInitDb300.getMetricsDate()))
        .isEqualTo(past300Str);
    assertThat(fmInitDb300.getMetricsValue()).isEqualTo(2);

    FirewallMetrics todayInitDb = findMetricByDate(today, allMetrics);
    assertThat(formatAsISOString(todayInitDb.getMetricsDate()))
        .isEqualTo(formatAsISOString(today));
    assertThat(todayInitDb.getMetricsValue()).isEqualTo(3);

    consolidator.consolidate();

    allMetrics = firewallMetricsDAOTest.getAll();

    // Check after consolidation
    FirewallMetrics fmDb300 = findMetricByDate(past300, allMetrics);
    assertThat(formatAsISOString(fmDb300.getMetricsDate()))
        .isEqualTo(past300Str);
    assertThat(fmDb300.getMetricsValue()).isEqualTo(2);

    FirewallMetrics fmDbToday = findMetricByDate(today, allMetrics);
    assertThat(formatAsISOString(
        fmDbToday.getMetricsDate()
    )).isEqualTo(todayStr);
    // This was the record updated. Initially 3 now 6
    assertThat(fmDbToday.getMetricsValue()).isEqualTo(6);
  }

  private FirewallMetrics findMetricByDate(
      LocalDate needle, List<FirewallMetrics> haystack)
  {
    List<FirewallMetrics> found = haystack.stream()
        .filter( fm -> fm.getMetricsDate().isEqual(needle))
        .collect(Collectors.toList());
    return !found.isEmpty() ? found.get(0) : null;
  }

  private void initTestDates() {
    Date today =  DateUtil.now();
    Date lastYearFromToday = DateUtils.addMonths(today, -12);

    Date date2019 = new GregorianCalendar(2019, Calendar.DECEMBER, 30, 18,
        23, 12 ).getTime();
    Date date2015 = new GregorianCalendar(2015, Calendar.DECEMBER, 30, 18,
        23, 12 ).getTime();
    Date date2017 = new GregorianCalendar(2017, Calendar.DECEMBER, 30, 18,
        23, 12 ).getTime();
    testLastUpdateDates.add(date2019);
    testLastUpdateDates.add(today);
    testLastUpdateDates.add(lastYearFromToday);
    testLastUpdateDates.add(date2015);
    testLastUpdateDates.add(date2017);
  }

  private void initTestData() {
    testLastUpdateDates.forEach( date -> tempEntity.newFirewallMetrics(
        FirewallMetricsName.COMPONENTS_AUTO_RELEASED,
        testLastUpdateDates.indexOf(date) + 1 ,
        date,
        toLocalDate(date)
    ));
  }

  private GregorianCalendar newDateWithTime(Date initDate,
                                            int hour, int minutes, int seconds)
  {
    GregorianCalendar gc = new GregorianCalendar();
    gc.setTime(initDate);
    gc.set(GregorianCalendar.HOUR, hour);
    gc.set(GregorianCalendar.MINUTE, minutes);
    gc.set(GregorianCalendar.SECOND, seconds);
    return gc;
  }

  private void initRepositoryComponentData(Date dateInThePast) {
    List<Date> hoursForDateInThePast = Arrays.asList(
        newDateWithTime(dateInThePast, 4,23,12).getTime(),
        newDateWithTime(dateInThePast, 10,1,22).getTime(),
        newDateWithTime(dateInThePast, 11,2,14).getTime()
    );

    hoursForDateInThePast.forEach( dateTime -> {
      Repository newRepo = tempEntity.newRepository();
      RepositoryComponent newRepositoryComponent = tempEntity
            .newRepositoryComponent(
            newRepo.getId(), newRepo.getId() + "/" + dateTime.toString(),
            null, dateTime, true);
      repositoryComponentDAOTest.update(newRepositoryComponent);
      repositoryDAOTest.update(newRepo);
    });
  }

  private String formatAsISOString(LocalDate date) {
    return date.toString();
  }
}
