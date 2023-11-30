/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationCountHistory;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.*;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ApplicationCountHistoryServiceTest
    extends AbstractComponentTest
{
  @Mock
  private DateTimeService dateTimeService;

  @Spy
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private PlexusCipher plexusCipher;

  @Inject
  private ApplicationCountHistoryService applicationCountHistoryService;

  private static final String ROOT_TOKEN = "root-token";

  private static final String ENC = "CMMDwoV";

  private Organization givenOrganization;

  @Override
  public void configure(Binder binder) {
    binder.bind(DateTimeService.class).toInstance(dateTimeService);
    binder.bind(PolicyViolationDAO.class).toInstance(policyViolationDAO);
    super.configure(binder);
  }

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
        new ApplicationCountHistory(firstRecording, 10, 0, 0, 0, 0)
    );

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);
    tempEntity.createApplications(34, givenOrganization);
    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording).hasSize(3);
    assertApplicationHistoryCountEqual(
        rowsAfterSecondRecording.get(2),
        new ApplicationCountHistory(secondRecording, 44, 0, 0, 0, 0)
    );
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
        new ApplicationCountHistory(firstRecording, 7, 2, 0, 0, 0)
    );

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
        new ApplicationCountHistory(secondRecording, 7, 5, 0, 0, 0)
    );
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
        new ApplicationCountHistory(firstRecording, 3, 0, 2, 0, 0)
    );

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
        new ApplicationCountHistory(secondRecording, 3, 0, 3, 0, 0)
    );
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
        new ApplicationCountHistory(firstRecording, 1, 0, 0, 3, 0)
    );

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
        new ApplicationCountHistory(secondRecording, 1, 0, 0, 5, 0)
    );
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
    final PolicyViolation waivedPolicyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluation, policy);
    waivedPolicyViolation1.setOpenTime(new Date(1L));
    waivedPolicyViolation1.setWaiveTime(new Date(12L));
    final PolicyViolation waivedPolicyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation, policy);
    waivedPolicyViolation2.setOpenTime(new Date(4L));
    waivedPolicyViolation2.setFixTime(new Date(7L));
    final PolicyViolation fixedPolicyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedPolicyViolation1.setOpenTime(new Date(5L));
    fixedPolicyViolation1.setFixTime(new Date(10L));

    when(policyViolationDAO.getWaivedFixed())
        .thenReturn(Arrays.asList(waivedPolicyViolation1, waivedPolicyViolation2, fixedPolicyViolation1));

    applicationCountHistoryService.recordApplicationCount();

    final long firstExpectedMeanTimeToRemediate = Math.round((float) (11L + 3L + 5L) / 3);
    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording).hasSize(2);
    assertThat(rowsAfterFirstRecording.get(1).getMeanTimeToRemediateMs())
        .isEqualTo(firstExpectedMeanTimeToRemediate);

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);

    // App has 1 new fixed violations
    final PolicyViolation fixedPolicyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedPolicyViolation2.setOpenTime(new Date(6L));
    fixedPolicyViolation2.setFixTime(new Date(50L));

    when(policyViolationDAO.getWaivedFixed())
        .thenReturn(Arrays.asList(waivedPolicyViolation1, waivedPolicyViolation2, fixedPolicyViolation1,
            fixedPolicyViolation2));

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
    final PolicyViolation waivedEarlierThanFixedPolicyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluation, policy);
    waivedEarlierThanFixedPolicyViolation1.setOpenTime(new Date(2L));
    waivedEarlierThanFixedPolicyViolation1.setWaiveTime(new Date(10L));
    waivedEarlierThanFixedPolicyViolation1.setFixTime(new Date(20L));
    final PolicyViolation fixedEarlierThanWaivedPolicyViolation2 =
        tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedEarlierThanWaivedPolicyViolation2.setOpenTime(new Date(4L));
    fixedEarlierThanWaivedPolicyViolation2.setWaiveTime(new Date(8L));
    fixedEarlierThanWaivedPolicyViolation2.setFixTime(new Date(5L));
    final PolicyViolation fixedPolicyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy);
    fixedPolicyViolation.setOpenTime(new Date(5L));
    fixedPolicyViolation.setFixTime(new Date(10L));

    when(policyViolationDAO.getWaivedFixed())
        .thenReturn(Arrays.asList(waivedEarlierThanFixedPolicyViolation1, fixedEarlierThanWaivedPolicyViolation2,
            fixedPolicyViolation));

    applicationCountHistoryService.recordApplicationCount();

    final long firstExpectedMeanTimeToRemediate = Math.round((float) (8L + 1L + 5L) / 3);
    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording).hasSize(2);
    assertThat(rowsAfterFirstRecording.get(1).getMeanTimeToRemediateMs())
        .isEqualTo(firstExpectedMeanTimeToRemediate);

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);

    // App has 1 new fixed violations
    final PolicyViolation waivedPolicyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy);
    waivedPolicyViolation.setOpenTime(new Date(23L));
    waivedPolicyViolation.setWaiveTime(new Date(60L));

    when(policyViolationDAO.getWaivedFixed())
        .thenReturn(Arrays.asList(waivedEarlierThanFixedPolicyViolation1, fixedEarlierThanWaivedPolicyViolation2,
            fixedPolicyViolation, waivedPolicyViolation));

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
    when(policyViolationDAO.getWaivedFixed())
        .thenReturn(null);

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording).hasSize(2);
    assertThat(rowsAfterFirstRecording.get(1).getMeanTimeToRemediateMs())
        .isZero();

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);

    when(policyViolationDAO.getWaivedFixed())
        .thenReturn(Collections.emptyList());

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording).hasSize(3);
    assertThat(rowsAfterSecondRecording.get(2).getMeanTimeToRemediateMs())
        .isZero();
  }

  // ignore id equality
  private void assertApplicationHistoryCountEqual(
      final ApplicationCountHistory actual,
      final ApplicationCountHistory expected
  )
  {
    assertThat(actual.getApplicationCount()).isEqualTo(expected.getApplicationCount());
    assertThat(actual.getScmFeedbackEnabledCount()).isEqualTo(expected.getScmFeedbackEnabledCount());
    assertThat(actual.getPolicyActionFailuresByAppCount()).isEqualTo(expected.getPolicyActionFailuresByAppCount());
    assertThat(actual.getWaiversCount()).isEqualTo(expected.getWaiversCount());
    assertThat(actual.getUpdatedDate()).isEqualTo(expected.getUpdatedDate());
    // Assert mean time to remediate values separately
  }

  private void enableScmFeedBackForApp(final Application application) {
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
        true
    );
  }
}
