/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiUsageIncrementDto;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationCountHistory;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

public class ApplicationCountHistoryServiceTest
    extends AbstractComponentTest
{
  private static final long ONE_DAY_IN_MS = 86_400_000L;

  private static final long ONE_WEEK_IN_MS = 604_800_000L;

  @Mock
  private DateTimeService dateTimeService;

  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private CIEvaluationStatService ciEvaluationStatService;

  @Inject
  private PlexusCipher plexusCipher;

  @Inject
  private ApplicationCountHistoryService applicationCountHistoryService;

  private static final String ROOT_TOKEN = "root-token";

  private static final String ENC = "CMMDwoV";

  private Organization givenOrganization;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Before
  public void setup() throws PlexusCipherException {
    givenOrganization = tempEntity.newOrganization();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt(ROOT_TOKEN, ENC),
        SourceControlProvider.GITHUB);
  }

  @Test
  public void testRecordApplicationCount_shouldCorrectlyRecordNumberOfApplications() {
    // === Given ===
    final Date firstRecording = Date.from(Instant.now().plus(Duration.ofDays(1)));
    final Date secondRecording = Date.from(Instant.now().plus(Duration.ofDays(2)));

    // === Then Initial State ===
    final List<ApplicationCountHistory> initialRows = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(initialRows).hasSize(1);
    assertThat(initialRows.get(0).getId()).isEqualTo("initialization");

    // === Then First Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(firstRecording);
    tempEntity.createApplications(10, givenOrganization);
    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording).hasSize(2);
    assertApplicationHistoryCountEqual(
        rowsAfterFirstRecording.get(1),
        new ApplicationCountHistory(firstRecording, 10, 0, 0, 0, 0));

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);
    tempEntity.createApplications(34, givenOrganization);
    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording).hasSize(3);
    assertApplicationHistoryCountEqual(
        rowsAfterSecondRecording.get(2),
        new ApplicationCountHistory(secondRecording, 44, 0, 0, 0, 0));
  }

  @Test
  public void testRecordApplicationCount_shouldCorrectlyRecordNumberAppsWithScmFeedbackEnabled() {
    // === Given ===
    final Date firstRecording = Date.from(Instant.now().plus(Duration.ofDays(1)));
    final Date secondRecording = Date.from(Instant.now().plus(Duration.ofDays(2)));

    // === Then First Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(firstRecording);
    final List<Application> applications = tempEntity.createApplications(7, givenOrganization);
    enableScmFeedBackForApp(applications.get(0));
    enableScmFeedBackForApp(applications.get(2));

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording).hasSize(2);
    assertApplicationHistoryCountEqual(
        rowsAfterFirstRecording.get(1),
        new ApplicationCountHistory(firstRecording, 7, 2, 0, 0, 0));

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);
    enableScmFeedBackForApp(applications.get(3));
    enableScmFeedBackForApp(applications.get(4));
    enableScmFeedBackForApp(applications.get(5));

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording).hasSize(3);
    assertApplicationHistoryCountEqual(
        rowsAfterSecondRecording.get(2),
        new ApplicationCountHistory(secondRecording, 7, 5, 0, 0, 0));
  }

  @Test
  public void testRecordApplicationCount_shouldConsiderDefaultScmFeedbackEnabledWhenTheValueIsNullAllTheWay() {
    // === Given ===
    final Date firstRecording = Date.from(Instant.now().plus(Duration.ofDays(1)));
    final Date secondRecording = Date.from(Instant.now().plus(Duration.ofDays(2)));

    // === Then First Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(firstRecording);
    final List<Application> applications = tempEntity.createApplications(1, givenOrganization);

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording)
        .last()
        .extracting(ach -> ach.getScmFeedbackEnabledCount())
        .isEqualTo(0);

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);
    setScmFeedBackForApp(applications.get(0), null);

    // The scenario that commitStatusEnabled was not set anywhere in the hierarchy for App1
    assertThat(sourceControlDAO.getByOwnerId(ROOT_ORGANIZATION_ID).getCommitStatusEnabled()).isNull();
    assertThat(sourceControlDAO.getByOwnerId(givenOrganization.getId())).isNull();
    assertThat(sourceControlDAO.getByOwnerId(applications.get(0).getId()).getCommitStatusEnabled()).isNull();

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording)
        .last()
        .extracting(ach -> ach.getScmFeedbackEnabledCount())
        .isEqualTo(1);
  }

  @Test
  public void testRecordApplicationCount_shouldCorrectlyRecordPolicyActionFailuresByAppCount() {
    // === Given ===
    final Date firstRecording = Date.from(Instant.now().plus(Duration.ofDays(1)));
    final Date secondRecording = Date.from(Instant.now().plus(Duration.ofDays(2)));

    final Application application1 = tempEntity.newApplication(givenOrganization.getId());
    final Application application2 = tempEntity.newApplication(givenOrganization.getId());
    final Application application3 = tempEntity.newApplication(givenOrganization.getId());
    final Policy policy1 = tempEntity.newPolicy(application1);
    final Policy policy2 = tempEntity.newPolicy(application2);
    final Policy policy3 = tempEntity.newPolicy(application3);
    final PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    final PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    final PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(application3.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));

    // === Then First Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(firstRecording);
    // App has 5 fail
    IntStream.of(0, 5)
        .forEach(i -> tempEntity.newPolicyViolation(policyEvaluation1, policy1, 10, PolicyThreatCategory.OTHER,
            "test-group-id", "test-artifact-id", "v1", "test-hash", FailActionType.ID));
    // App2 has 1 warn
    tempEntity.newPolicyViolation(policyEvaluation2, policy2, 6, PolicyThreatCategory.QUALITY, "test-group-id",
        "test-artifact-id", "v1", "test-hash", WarnActionType.ID);
    // App 3 has 1 fail
    tempEntity.newPolicyViolation(policyEvaluation3, policy3, 8, PolicyThreatCategory.QUALITY, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording).hasSize(2);
    assertApplicationHistoryCountEqual(
        rowsAfterFirstRecording.get(1),
        new ApplicationCountHistory(firstRecording, 3, 0, 2, 0, 0));

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);
    // App2 has new fail
    tempEntity.newPolicyViolation(policyEvaluation2, policy2, 8, PolicyThreatCategory.QUALITY, "test-group-id",
        "test-artifact-id", "v1", "test-hash", FailActionType.ID);

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording).hasSize(3);
    assertApplicationHistoryCountEqual(
        rowsAfterSecondRecording.get(2),
        new ApplicationCountHistory(secondRecording, 3, 0, 3, 0, 0));
  }

  @Test
  public void testRecordApplicationCount_shouldCorrectlyRecordWaiversCount() {
    // === Given ===
    final Date firstRecording = Date.from(Instant.now().plus(Duration.ofDays(1)));
    final Date secondRecording = Date.from(Instant.now().plus(Duration.ofDays(2)));

    final Application application = tempEntity.newApplication(givenOrganization.getId());
    final Policy policy = tempEntity.newPolicy(application);
    final PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));
    final PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());

    // === Then First Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(firstRecording);
    // App has 3 active waivers
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, policyWaiver);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, policyWaiver);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, policyWaiver);
    // App has 1 unwaived
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording).hasSize(2);
    assertApplicationHistoryCountEqual(
        rowsAfterFirstRecording.get(1),
        new ApplicationCountHistory(firstRecording, 1, 0, 0, 3, 0));

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);

    // App2 has 2 new waived
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, policyWaiver);
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy, policyWaiver);

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording).hasSize(3);
    assertApplicationHistoryCountEqual(
        rowsAfterSecondRecording.get(2),
        new ApplicationCountHistory(secondRecording, 1, 0, 0, 5, 0));
  }

  @Test
  public void testRecordApplicationCount_shouldCorrectlyRecordMeanTimeToRemediateMillis() {
    // === Given ===
    final Date firstRecording = Date.from(Instant.now().plus(Duration.ofDays(1)));
    final Date secondRecording = Date.from(Instant.now().plus(Duration.ofDays(2)));

    final Application application = tempEntity.newApplication(givenOrganization.getId());
    final Policy policy = tempEntity.newPolicy(application);
    final PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));

    // === Then First Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(firstRecording);

    // App has 3 waived or fixed violations
    tempEntity.createWaivedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        11L);
    tempEntity.createWaivedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        3L);
    tempEntity.createFixedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        5L);

    applicationCountHistoryService.recordApplicationCount();

    final long firstExpectedMeanTimeToRemediate = Math.round((float) (11L + 3L + 5L) / 3);
    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording).hasSize(2);
    assertThat(rowsAfterFirstRecording.get(1).getMeanTimeToRemediateMs())
        .isEqualTo(firstExpectedMeanTimeToRemediate);

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);

    // App has 1 new fixed violations
    tempEntity.createFixedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        44L);

    applicationCountHistoryService.recordApplicationCount();

    final long secondExpectedMeanTimeToRemediate = Math.round((float) (11L + 3L + 5L + 44L) / 4);
    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording).hasSize(3);
    assertThat(rowsAfterSecondRecording.get(2).getMeanTimeToRemediateMs())
        .isEqualTo(secondExpectedMeanTimeToRemediate);
  }

  @Test
  public void testRecordApplicationCount_shouldUseEarlierOfWaiveOrFixed_WhenCalculatingMeanTimeToRemediateMillis() {
    // === Given ===
    final Date firstRecording = Date.from(Instant.now().plus(Duration.ofDays(1)));
    final Date secondRecording = Date.from(Instant.now().plus(Duration.ofDays(2)));

    final Application application = tempEntity.newApplication(givenOrganization.getId());
    final Policy policy = tempEntity.newPolicy(application);
    final PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "scan-1", new Date(System.currentTimeMillis()));

    // === Then First Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(firstRecording);

    // App has 3 waived or fixed violations
    tempEntity.createWaivedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        8L);

    tempEntity.createWaivedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        1L);

    tempEntity.createFixedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        5L);

    applicationCountHistoryService.recordApplicationCount();

    final long firstExpectedMeanTimeToRemediate = Math.round((float) (8L + 1L + 5L) / 3);
    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording).hasSize(2);
    assertThat(rowsAfterFirstRecording.get(1).getMeanTimeToRemediateMs())
        .isEqualTo(firstExpectedMeanTimeToRemediate);

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);

    // App has 1 new fixed violations
    tempEntity.createWaivedPolicyViolation(
        policyEvaluation,
        policy,
        new Date(),
        37L);

    applicationCountHistoryService.recordApplicationCount();

    final long secondExpectedMeanTimeToRemediate = Math.round((float) (8L + 1L + 5L + 37L) / 4);
    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording).hasSize(3);
    assertThat(rowsAfterSecondRecording.get(2).getMeanTimeToRemediateMs())
        .isEqualTo(secondExpectedMeanTimeToRemediate);
  }

  @Test
  public void testRecordApplicationCount_returnZeroIfViolationListIsNullEmpty_WhenCalcMeanTimeToRemediateMillis() {
    // === Given ===
    final Date firstRecording = Date.from(Instant.now().plus(Duration.ofDays(1)));
    final Date secondRecording = Date.from(Instant.now().plus(Duration.ofDays(2)));

    // === Then First Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(firstRecording);

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording).hasSize(2);
    assertThat(rowsAfterFirstRecording.get(1).getMeanTimeToRemediateMs())
        .isZero();

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording).hasSize(3);
    assertThat(rowsAfterSecondRecording.get(2).getMeanTimeToRemediateMs())
        .isZero();
  }

  @Test
  public void testGetUsageOverTime_shouldReturnCorrectValuesGivenIncrementSizeAndNumber() {
    // === Given ===
    // Clock frozen at 2023-11-03T15:51:56.287Z
    final long nowMs = 1699027090422L;
    final Clock fixedClock = Clock.fixed(Instant.ofEpochMilli(nowMs), ZoneId.systemDefault());

    final Date fiveWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-35));

    final Date fourWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-28));
    tempEntity.newApplicationCountHistoryEntry(fourWeeksAgo, 42, 20, 33, 21, 3);

    final Date threeWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-21));
    tempEntity.newApplicationCountHistoryEntry(threeWeeksAgo, 62, 63, 42, 29, 55);

    final Date twoWeeksAgo = getDateFromOffset(fixedClock, Duration.ofDays(-14));
    tempEntity.newApplicationCountHistoryEntry(twoWeeksAgo, 111, 65, 21, 15, 74);

    final Date oneWeekAgo = getDateFromOffset(fixedClock, Duration.ofDays(-7));
    tempEntity.newApplicationCountHistoryEntry(oneWeekAgo, 134, 61, 100, 51, 38);

    final Date current = new Date(nowMs);
    tempEntity.newApplicationCountHistoryEntry(current, 429, 84, 135, 76, 93);

    // === When ===
    when(dateTimeService.getCurrentTimeMs()).thenReturn(nowMs);

    mockApplicationWithCiCdCalls(
        Pair.of(fiveWeeksAgo, 0),
        Pair.of(fourWeeksAgo, 5),
        Pair.of(threeWeeksAgo, 55),
        Pair.of(twoWeeksAgo, 100),
        Pair.of(getDateFromOffset(fixedClock, -13), 34),
        Pair.of(getDateFromOffset(fixedClock, -12), 45),
        Pair.of(getDateFromOffset(fixedClock, -11), 23),
        Pair.of(getDateFromOffset(fixedClock, -10), 88),
        Pair.of(getDateFromOffset(fixedClock, -9), 122),
        Pair.of(getDateFromOffset(fixedClock, -8), 111),
        Pair.of(oneWeekAgo, 101),
        Pair.of(getDateFromOffset(fixedClock, -6), 78),
        Pair.of(getDateFromOffset(fixedClock, -5), 34),
        Pair.of(getDateFromOffset(fixedClock, -4), 22),
        Pair.of(getDateFromOffset(fixedClock, -3), 200),
        Pair.of(getDateFromOffset(fixedClock, -2), 144),
        Pair.of(getDateFromOffset(fixedClock, -1), 102),
        Pair.of(current, 103));

    // === Then ===
    final List<ApiUsageIncrementDto> fiveWeeksByWeeklyIncrements =
        applicationCountHistoryService.getUsageOverTime(ONE_WEEK_IN_MS, 6);

    assertThat(fiveWeeksByWeeklyIncrements).isEqualTo(
        Lists.newArrayList(
            new ApiUsageIncrementDto(fiveWeeksAgo.getTime(), 0, 0, 0, 0, 0, 0),
            new ApiUsageIncrementDto(fourWeeksAgo.getTime(), 42, 20, 33, 21, 3, 5),
            new ApiUsageIncrementDto(threeWeeksAgo.getTime(), 62, 63, 42, 29, 55, 55),
            new ApiUsageIncrementDto(twoWeeksAgo.getTime(), 111, 65, 21, 15, 74, 100),
            new ApiUsageIncrementDto(oneWeekAgo.getTime(), 134, 61, 100, 51, 38, 101),
            new ApiUsageIncrementDto(nowMs, 429, 84, 135, 76, 93, 103)));

    // === Then -- With Different Increment and Size ===
    final List<ApiUsageIncrementDto> twoWeeksByDailyIncrements =
        applicationCountHistoryService.getUsageOverTime(ONE_DAY_IN_MS, 14);

    assertThat(twoWeeksByDailyIncrements).isEqualTo(
        Lists.newArrayList(
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -13).getTime(),
                111,
                65,
                21,
                15,
                74,
                34),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -12).getTime(),
                111,
                65,
                21,
                15,
                74,
                45),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -11).getTime(),
                111,
                65,
                21,
                15,
                74,
                23),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -10).getTime(),
                111,
                65,
                21,
                15,
                74,
                88),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -9).getTime(),
                111,
                65,
                21,
                15,
                74,
                122),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -8).getTime(),
                111,
                65,
                21,
                15,
                74,
                111),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -7).getTime(),
                134,
                61,
                100,
                51,
                38,
                101),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -6).getTime(),
                134,
                61,
                100,
                51,
                38,
                78),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -5).getTime(),
                134,
                61,
                100,
                51,
                38,
                34),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -4).getTime(),
                134,
                61,
                100,
                51,
                38,
                22),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -3).getTime(),
                134,
                61,
                100,
                51,
                38,
                200),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -2).getTime(),
                134,
                61,
                100,
                51,
                38,
                144),
            new ApiUsageIncrementDto(
                getDateFromOffset(fixedClock, -1).getTime(),
                134,
                61,
                100,
                51,
                38,
                102),
            new ApiUsageIncrementDto(nowMs,
                429,
                84,
                135,
                76,
                93,
                103)));
  }

  private Date getDateFromOffset(final Clock baseClock, final int numberOfDays) {
    return getDateFromOffset(baseClock, Duration.ofDays(numberOfDays));
  }

  private Date getDateFromOffset(final Clock baseClock, final Duration offset) {
    return Date.from(Clock.offset(baseClock, offset).instant());
  }

  // ignore id equality
  private void assertApplicationHistoryCountEqual(
      final ApplicationCountHistory actual,
      final ApplicationCountHistory expected)
  {
    assertThat(actual.getApplicationCount()).isEqualTo(expected.getApplicationCount());
    assertThat(actual.getScmFeedbackEnabledCount()).isEqualTo(expected.getScmFeedbackEnabledCount());
    assertThat(actual.getPolicyActionFailuresByAppCount()).isEqualTo(expected.getPolicyActionFailuresByAppCount());
    assertThat(actual.getWaiversCount()).isEqualTo(expected.getWaiversCount());
    assertThat(actual.getUpdatedDate()).isEqualTo(expected.getUpdatedDate());
    // Assert mean time to remediate values separately
  }

  private void enableScmFeedBackForApp(final Application application) {
    setScmFeedBackForApp(application, true);
  }

  private void setScmFeedBackForApp(final Application application, final Boolean commitStatusEnabled) {
    final String anyRepoUrl = "https://example.com/organization/" + UUID.randomUUID();
    final String appId = application.getId();

    tempEntity.newSourceControl(
        appId,
        anyRepoUrl,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        true,
        true,
        "/target/*",
        true,
        commitStatusEnabled,
        false,
        false);
  }

  @SafeVarargs
  private final void mockApplicationWithCiCdCalls(Pair<Date, Integer>... timeToCounts) {
    Arrays.stream(timeToCounts).forEach(timeToCount -> {
      final Date upperBound = timeToCount.getLeft();
      when(ciEvaluationStatService.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluationsNoAuth(upperBound))
          .thenReturn(timeToCount.getRight());
    });
  }
}
