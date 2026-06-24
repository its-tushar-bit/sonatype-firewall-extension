/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO.StageEvaluationWindow;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyEvaluationDAOTest
    extends AbstractDbDAOTest
{
  private static final String COMMIT_HASH = "abcdef1234abcdef1234abcdef1234abcdef1234";

  private SourceControlDefaultBranchCommitHistoryDAO defaultBranchCommitHistoryDAO;

  private PolicyEvaluationDAO dao;

  private SourceControlPullRequestCommentDAO pullRequestCommentDAO;

  private SourceControlEventDAO sourceControlEventDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    defaultBranchCommitHistoryDAO = daoFactory.createSourceControlDefaultBranchCommitHistoryDAO();
    dao = daoFactory.createPolicyEvaluationDAO();
    pullRequestCommentDAO = daoFactory.createSourceControlPullRequestCommentDAO();
    sourceControlEventDAO = daoFactory.createSourceControlEventDAO();
  }

  @Test
  public void testCRUD() {
    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    // Create
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(application.getId(), stageTypeId, scanId, "system", ScanTriggerType.CLI);
    assertThat(policyEvaluation.getId()).isNull();
    dao.insert(policyEvaluation);
    assertThat(policyEvaluation.getId()).isNotNull();
    assertThat(policyEvaluation.getTime().getTime()).isBetween(System.currentTimeMillis() - 10 * 1000,
        System.currentTimeMillis() + 1000);

    // Read
    policyEvaluation = dao.getById(policyEvaluation.getId());
    assertThat(policyEvaluation).isNotNull();
    assertPolicyEvaluation(application.getId(), stageTypeId, scanId, false, false, "system", policyEvaluation);

    // Update is not allowed
    PolicyEvaluation policyEvaluationToUpdate = policyEvaluation;
    assertThatThrownBy(() -> dao.update(policyEvaluationToUpdate)).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("The PolicyEvaluation table does not support update operations");

    // Delete
    dao.delete(policyEvaluation);

    policyEvaluation = dao.getById(policyEvaluation.getId());
    assertThat(policyEvaluation).isNull();
  }

  @Test
  public void testGetLastByApplicationIdAndScanId() {
    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, time2);

    PolicyEvaluation policyEvaluation = dao.getLastByApplicationIdAndScanId(application.getId(), scanId);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluation.getTime()).isEqualTo(time2);
  }

  @Test
  public void testGetLastByApplicationIdAndStageId() {
    String stageTypeId = ReleaseStageType.ID;

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time2);

    PolicyEvaluation policyEvaluation = dao.getLastByApplicationIdAndStageId(application.getId(), stageTypeId);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluation.getTime()).isEqualTo(time2);
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }

  @Test
  public void testGetLastByApplicationIdAndStageId_Reevaluation() {
    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId,
        true /* isReevaluation */, false /* forMonitoring */, time2);

    PolicyEvaluation policyEvaluation = dao.getLastByApplicationIdAndStageId(application.getId(), stageTypeId);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluation.getTime()).isEqualTo(time2);
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }

  @Test
  public void testGetLastByApplicationIdAndStageId_ReevaluationOfOldScan() {
    String stageTypeId = ReleaseStageType.ID;

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time2);
    // A re-eval of an older scan should not be returned as the last eval
    Date time3 = new Date(time2.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId1", true /* isReevaluation */,
        false /* forMonitoring */, true /* isForObsoleteScan */, time3);

    PolicyEvaluation policyEvaluation = dao.getLastByApplicationIdAndStageId(application.getId(), stageTypeId);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluation.getTime()).isEqualTo(time2);
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }

  @Test
  public void testGetLastPrimaryByApplicationIdAndStageId() {
    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, true /* isReevaluation */,
        false /* forMonitoring */, time2);

    PolicyEvaluation policyEvaluation =
        dao.getLastPrimaryByApplicationIdAndStageId(application.getId(), ReleaseStageType.ID);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getTime()).isEqualTo(time1);
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }

  @Test
  public void testGetLastPrimaryByApplicationIdsAndStageId() {
    String stageTypeId = ReleaseStageType.ID;

    Application application2 = tempEntity.newApplication("AbstractDbDAOTest-AppName2",
        "AbstractDbDAOTest_AppPublicId2", organization.getId());
    // application without any evaluation; must be absent from the result map
    Application application3 = tempEntity.newApplication("AbstractDbDAOTest-AppName3",
        "AbstractDbDAOTest_AppPublicId3", organization.getId());

    Date time1 = new Date();
    PolicyEvaluation app1Primary = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scan1", time1);
    // a later reevaluation must NOT win over the primary
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scan1", true /* isReevaluation */,
        false /* forMonitoring */, time2);
    // a primary for a wrong stage must be ignored
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-build", time2);

    // second app: two primaries, the newest wins
    Date time3 = new Date(time1.getTime() + 2000);
    tempEntity.newPolicyEvaluation(application2.getId(), stageTypeId, "scan2", time1);
    PolicyEvaluation app2Newest = tempEntity.newPolicyEvaluation(application2.getId(), stageTypeId, "scan3", time3);

    var result = dao.getLastPrimaryByApplicationIdsAndStageId(
        Sets.newHashSet(application.getId(), application2.getId(), application3.getId()), stageTypeId);

    assertThat(result).containsOnlyKeys(application.getId(), application2.getId());
    assertThat(result.get(application.getId()).getId()).isEqualTo(app1Primary.getId());
    assertThat(result.get(application.getId()).isReevaluation()).isFalse();
    assertThat(result.get(application2.getId()).getId()).isEqualTo(app2Newest.getId());
  }

  @Test
  public void testGetLastPrimaryByApplicationIdsAndStageId_emptyInput() {
    assertThat(dao.getLastPrimaryByApplicationIdsAndStageId(Collections.emptySet(), ReleaseStageType.ID)).isEmpty();
  }

  private void assertPolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      boolean reevaluation,
      boolean forMonitoring,
      String initiator,
      PolicyEvaluation actual)
  {
    assertThat(actual.getApplicationId()).isEqualTo(applicationId);
    assertThat(actual.getStageTypeId()).isEqualTo(stageTypeId);
    assertThat(actual.getScanId()).isEqualTo(scanId);
    assertThat(actual.isReevaluation()).isEqualTo(reevaluation);
    assertThat(actual.isForMonitoring()).isEqualTo(forMonitoring);
    assertThat(actual.getInitiator()).isEqualTo(initiator);
  }

  @Test
  public void testGetLastByApplicationIdsAndStageIdsGetsNewest() {
    String stageTypeId = ReleaseStageType.ID;

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time2);

    List<PolicyEvaluation> policyEvaluations =
        dao.getLastByApplicationIdsAndStageIds(Sets.newHashSet(application.getId()),
            Sets.newHashSet(ReleaseStageType.ID));
    assertThat(policyEvaluations).hasSize(1);
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluation.getTime()).isEqualTo(time2);
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }

  @Test
  public void testGetLastByApplicationIdsAndStageIdsFiltersInvalidAppIdsAndStages() {
    Application application2 = tempEntity.newApplication("AbstractDbDAOTest-AppName2",
        "AbstractDbDAOTest_AppPublicId2", organization.getId());

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan1", time1);

    // wrong stage
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan2", time2);

    // wrong appId
    Date time3 = new Date(time1.getTime() + 2000);
    tempEntity.newPolicyEvaluation(application2.getId(), ReleaseStageType.ID, "scan3", time3);

    // wrong both
    Date time4 = new Date(time1.getTime() + 3000);
    tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, "scan4", time4);

    List<PolicyEvaluation> policyEvaluations =
        dao.getLastByApplicationIdsAndStageIds(Sets.newHashSet(application.getId()),
            Sets.newHashSet(ReleaseStageType.ID));
    assertThat(policyEvaluations).hasSize(1);
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getTime()).isEqualTo(time1);
  }

  @Test
  public void testGetLastByApplicationIdsAndStageIdsDealsWithDuplicateTimes() {
    Date time1 = new Date();
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan1", time1);
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan2", time1);

    List<PolicyEvaluation> policyEvaluations =
        dao.getLastByApplicationIdsAndStageIds(Sets.newHashSet(application.getId()),
            Sets.newHashSet(ReleaseStageType.ID));
    assertThat(policyEvaluations).hasSize(1);
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation.getTime()).isEqualTo(time1);
    assertThat(policyEvaluation.getId()).isEqualTo(pe1.getId());
  }

  @Test
  public void testGetLastByApplicationIdsAndStageIdsDealsWithTwoApps() {
    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scan2", time2);

    // second app
    Application application2 = tempEntity.newApplication("AbstractDbDAOTest-AppName2",
        "AbstractDbDAOTest_AppPublicId2", organization.getId());
    tempEntity.newPolicyEvaluation(application2.getId(), ReleaseStageType.ID, "scan3", time1);
    PolicyEvaluation pe4 = tempEntity.newPolicyEvaluation(application2.getId(), ReleaseStageType.ID, "scan4", time2);

    List<PolicyEvaluation> policyEvaluations = dao.getLastByApplicationIdsAndStageIds(
        Sets.newHashSet(application.getId(), application2.getId()), Sets.newHashSet(ReleaseStageType.ID));
    assertThat(policyEvaluations).noneMatch(Objects::isNull)
        .allSatisfy(pe -> assertThat(pe.getTime()).isEqualTo(time2))
        .extracting(PolicyEvaluation::getId)
        .containsExactlyInAnyOrder(pe2.getId(), pe4.getId());
  }

  @Test
  public void testGetLastByApplicationIdsAndStageIds_ReevaluationOfOldScan() {
    String stageTypeId = ReleaseStageType.ID;

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time2);
    // A re-eval of an older scan should not be returned as the last eval
    Date time3 = new Date(time2.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId1", true /* isReevaluation */,
        false /* forMonitoring */, true /* isForObsoleteScan */, time3);

    List<PolicyEvaluation> policyEvaluations =
        dao.getLastByApplicationIdsAndStageIds(Sets.newHashSet(application.getId()),
            Sets.newHashSet(stageTypeId));
    assertThat(policyEvaluations).hasSize(1);
    assertThat(policyEvaluations.get(0).getTime()).isEqualTo(time2);
    assertThat(policyEvaluations.get(0).getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluations.get(0).isForObsoleteScan()).isFalse();
  }

  @Test
  public void testGetLastByApplicationIdsAndStageIds_ReevaluationOfMultipleOldScan() {
    String stageTypeId = ReleaseStageType.ID;

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time2);
    // A re-eval of an older scan should not be returned as the last eval
    Date time3 = new Date(time2.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId1", true /* isReevaluation */,
        false /* forMonitoring */, true /* isForObsoleteScan */, time3);

    // second app
    Application application2 = tempEntity.newApplication("AbstractDbDAOTest-AppName2",
        "AbstractDbDAOTest_AppPublicId2", organization.getId());
    Date time4 = new Date(time3.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application2.getId(), stageTypeId, "scanId5", time4);
    Date time5 = new Date(time4.getTime() + 1000);
    PolicyEvaluation pe5 = tempEntity.newPolicyEvaluation(application2.getId(), stageTypeId, "scanId6", time5);
    // A re-eval of an older scan should not be returned as the last eval
    Date time6 = new Date(time5.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application2.getId(), stageTypeId, "scanId5", true /* isReevaluation */,
        false /* forMonitoring */, true /* isForObsoleteScan */, time6);

    // third app
    Application application3 = tempEntity.newApplication("AbstractDbDAOTest-AppName3",
        "AbstractDbDAOTest_AppPublicId3", organization.getId());
    Date time7 = new Date();
    tempEntity.newPolicyEvaluation(application3.getId(), stageTypeId, "scanId7", time7);
    Date time8 = new Date(time7.getTime() + 9000);
    PolicyEvaluation pe8 = tempEntity.newPolicyEvaluation(application3.getId(), stageTypeId, "scanId7",
        true /* isReevaluation */, false /* forMonitoring */, time8);

    List<PolicyEvaluation> policyEvaluations = dao.getLastByApplicationIdsAndStageIds(
        Sets.newHashSet(application.getId(), application2.getId(), application3.getId()), Sets.newHashSet(stageTypeId));
    assertThat(policyEvaluations).noneMatch(Objects::isNull)
        .noneMatch(PolicyEvaluation::isForObsoleteScan)
        .extracting(PolicyEvaluation::getId)
        .containsExactlyInAnyOrder(pe2.getId(), pe5.getId(), pe8.getId());
  }

  @Test
  public void testGetLastByApplicationIdsAndStageIds_InOperatorOptimizationForH2() {
    testGetLastByApplicationIdsAndStageIds_InOperatorOptimization(true);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetLastByApplicationIdsAndStageIds_InOperatorOptimizationForPostgres() {
    testGetLastByApplicationIdsAndStageIds_InOperatorOptimization(false);
  }

  private void testGetLastByApplicationIdsAndStageIds_InOperatorOptimization(boolean isEmbeddedDb) {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());

    Date time1 = new Date();
    Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, "scanId1", time1);
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId2", time1);
    tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scanId3", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId4", time2);

    int inOperatorThreshold = isEmbeddedDb
        ? PolicyEvaluationDAO.H2_IN_OPERATOR_THRESHOLD
        : PolicyEvaluationDAO.POSTGRES_IN_OPERATOR_THRESHOLD;
    Set<String> appIds = new LinkedHashSet<>();
    while (appIds.size() < inOperatorThreshold) {
      appIds.add(TemporaryEntity.uuid());
    }
    appIds.add(application.getId());
    List<PolicyEvaluation> policyEvaluations = dao.getLastByApplicationIdsAndStageIds(appIds,
        Collections.singleton(BuildStageType.ID));
    assertThat(policyEvaluations).hasSize(1);
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation.getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluation.getTime()).isEqualTo(time2);
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }

  @Test
  public void testValidateForObsoleteScan_Insert() {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(application.getId(), ReleaseStageType.ID, "scanId", "system",
            ScanTriggerType.CLI);
    policyEvaluation.setForObsoleteScan(true);
    assertThatThrownBy(() -> dao.insert(policyEvaluation)).isInstanceOf(IllegalStateException.class)
        .hasMessage("Primary evaluations cannot be for obsolete scans");
  }

  @Test
  public void testGetLastByApplicationIds() {
    String stageTypeId = ReleaseStageType.ID;
    Date time1 = new Date();
    Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application2.getId(), stageTypeId, "scanId1", time1);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId3", time2);

    List<PolicyEvaluation> policyEvaluations = dao.getLastByApplicationIds(Sets.newHashSet(application.getId()));
    assertThat(policyEvaluations).hasSize(1);
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation.getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluation.getTime()).isEqualTo(time2);
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetLastByApplicationIds_InOperatorOptimizationForPostgres() {
    testGetLastByApplicationIds_InOperatorOptimization(false);
  }

  @Test
  public void testGetLastByApplicationIds_InOperatorOptimizationForH2() {
    testGetLastByApplicationIds_InOperatorOptimization(true);
  }

  private void testGetLastByApplicationIds_InOperatorOptimization(boolean isDatabaseEmbedded) {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());

    String stageTypeId = ReleaseStageType.ID;
    Date time1 = new Date();
    Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application2.getId(), stageTypeId, "scanId1", time1);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId3", time2);

    Set<String> appIds = new LinkedHashSet<>();
    int threshold = isDatabaseEmbedded
        ? PolicyEvaluationDAO.H2_IN_OPERATOR_THRESHOLD
        : PolicyEvaluationDAO.POSTGRES_IN_OPERATOR_THRESHOLD;

    while (appIds.size() < threshold) {
      appIds.add(TemporaryEntity.uuid());
    }
    appIds.add(application.getId());
    List<PolicyEvaluation> policyEvaluations = dao.getLastByApplicationIds(appIds);
    assertThat(policyEvaluations).hasSize(1);
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation.getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluation.getTime()).isEqualTo(time2);
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }

  @Test
  public void testDelete_UpdateLastPolicyEvaluation() {
    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    Date time1 = new Date();
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, time2);

    // Assert we have a last policy evaluation and that it is the second one
    PolicyEvaluation lastPolicyEvaluation = dao.getLastByApplicationIdAndStageId(application.getId(), stageTypeId);
    assertThat(lastPolicyEvaluation.getId()).isEqualTo(pe2.getId());

    // Delete the second evaluation. The last policy eval will be updated to the first policy eval.
    dao.delete(pe2);

    lastPolicyEvaluation = dao.getLastByApplicationIdAndStageId(application.getId(), stageTypeId);
    assertThat(lastPolicyEvaluation.getId()).isEqualTo(pe1.getId());
  }

  @Test
  public void testDelete_cascadeToSourceControlDefaultBranchCommitHistory() {
    // given branch commit history that references a policy evaluation
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan", "commit");
    SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory =
        tempEntity.newSourceControlDefaultBranchCommitHistory(
            application.getId(), policyEvaluation.getCommitHash(), new Date(), policyEvaluation.getId());

    // when : fetch the history
    SourceControlDefaultBranchCommitHistory fetchedDefaultBranchCommitHistory =
        defaultBranchCommitHistoryDAO.getByApplicationIdAndPolicyEvaluationId(
            application.getId(),
            defaultBranchCommitHistory.getPolicyEvaluationId());

    // then : the history exists
    assertThat(fetchedDefaultBranchCommitHistory).isNotNull();

    // when : deleting the policy evaluation
    dao.delete(policyEvaluation);

    // then : the history no longer exists
    fetchedDefaultBranchCommitHistory = defaultBranchCommitHistoryDAO.getByApplicationIdAndPolicyEvaluationId(
        application.getId(),
        defaultBranchCommitHistory.getPolicyEvaluationId());
    assertThat(fetchedDefaultBranchCommitHistory).isNull();
  }

  @Test
  public void testDelete_cascadeToSourceControlPullRequestCommentForSourcePolicyEvaluation() {
    testDelete_cascadeToSourceControlPullRequestComment(
        (sourcePolicyEvaluation, targetPolicyEvaluation) -> sourcePolicyEvaluation);
  }

  @Test
  public void testDelete_cascadeToSourceControlPullRequestCommentForTargetPolicyEvaluation() {
    testDelete_cascadeToSourceControlPullRequestComment(
        (sourcePolicyEvaluation, targetPolicyEvaluation) -> targetPolicyEvaluation);
  }

  private void testDelete_cascadeToSourceControlPullRequestComment(PolicyEvaluationChooser policyEvaluationChooser) {
    // given an overall source control comment and a line comment that reference policy evaluations
    PolicyEvaluation sourcePolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    PolicyEvaluation targetPolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    SourceControlPullRequestComment pullRequestComment = new SourceControlPullRequestComment(
        application.getId(),
        1,
        2,
        4,
        "contentHash",
        sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId());
    pullRequestCommentDAO.insert(pullRequestComment);

    final String componentHash = "componentHash1";

    SourceControlPullRequestComment lineComment = new SourceControlPullRequestComment(
        application.getId(),
        componentHash,
        "testpathname",
        2,
        3,
        4,
        sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId());
    pullRequestCommentDAO.insert(lineComment);

    // when : fetch the comments
    SourceControlPullRequestComment fetchedPullRequestComment = pullRequestCommentDAO
        .getByApplicationIdAndPullRequestIdWithoutComponent(application.getId(), pullRequestComment.getPullRequestId());

    SourceControlPullRequestComment fetchedLineComment = pullRequestCommentDAO
        .getByApplicationIdAndComponentAndPullRequestId(application.getId(), componentHash,
            lineComment.getPullRequestId());

    // then : the comments exist
    assertThat(fetchedPullRequestComment).isNotNull();
    assertThat(fetchedLineComment).isNotNull();

    // when : deleting one of the policy evaluations
    dao.delete(policyEvaluationChooser.choose(sourcePolicyEvaluation, targetPolicyEvaluation));

    // then : the comment no longer exists
    fetchedPullRequestComment = pullRequestCommentDAO.getByApplicationIdAndPullRequestIdWithoutComponent(
        application.getId(),
        pullRequestComment.getPullRequestId());
    assertThat(fetchedPullRequestComment).isNull();

    fetchedLineComment = pullRequestCommentDAO
        .getByApplicationIdAndComponentAndPullRequestId(application.getId(), componentHash,
            pullRequestComment.getPullRequestId());
    assertThat(fetchedLineComment).isNull();
  }

  @Test
  public void testDelete_cascadeToSourceControlEventForSourcePolicyEvaluation() {
    // given a source control event with policy evaluations
    PolicyEvaluation sourcePolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");

    SourceControlEvent sourceControlEvent =
        tempEntity.newSourceControlEvent(application, sourcePolicyEvaluation);

    SourceControlEvent sourceControlEventByIdBeforeDelete = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventByIdBeforeDelete).isNotNull();

    // when the policy evaluation is deleted
    dao.delete(sourcePolicyEvaluation);

    // then the source control event is deleted
    SourceControlEvent sourceControlEventByIAfterDelete = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventByIAfterDelete).isNull();
  }

  @Test
  public void testGetBetweenDatesByApplicationIdAndStageIds() {
    Date now = new Date();
    Date earlier = new Date(now.getTime() - 1000);
    Date later = new Date(now.getTime() + 1000);
    Date latest = new Date(now.getTime() + 2000);

    Application app = tempEntity.newApplicationWithParent("test");
    Application otherApp = tempEntity.newApplicationWithParent("other");
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1", earlier);

    // insert these chronologically backwards to have extra assurance that the DAO deliberately sorts them
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan4", latest);
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan3", later);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan2", now);

    // an evaluation in another stage, and one in another app. These should not be returned
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, "scan4", later);
    tempEntity.newPolicyEvaluation(otherApp.getId(), BuildStageType.ID, "scan5", later);

    List<PolicyEvaluation> results = dao.getBetweenDatesByApplicationIdAndStageIds(now, latest, app.getId(),
        Collections.singleton(BuildStageType.ID));

    assertThat(results).extracting(PolicyEvaluation::getId).containsExactly(eval2.getId(), eval3.getId());
  }

  @Test
  public void testGetOldestByApplicationId() {
    Date date2 = new Date();
    Date date1 = new Date(date2.getTime() - 1000);
    Date date3 = new Date(date2.getTime() + 1000);

    Application app = tempEntity.newApplicationWithParent("test");
    Application otherApp = tempEntity.newApplicationWithParent("other");
    tempEntity.newPolicyEvaluation(otherApp.getId(), BuildStageType.ID, "scan1", date1);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan2", date2);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan3", date3);

    PolicyEvaluation results = dao.getOldestByApplicationId(app.getId());

    assertThat(results).isNotNull();
    assertThat(results.getId()).isEqualTo(eval2.getId());
  }

  @Test
  public void testGetPrimaryNonMonitoringByApplicationIdAndStageId() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();

    PolicyEvaluation evaluation1 =
        tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan1", false, false, new Date());
    PolicyEvaluation evaluation2 =
        tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan2", false, false, new Date());
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan2", true, false, new Date());
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan3", false, true, new Date());
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan4", false, false, new Date());
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "scan5", false, false, new Date());

    assertThat(dao.getPrimaryNonMonitoringByApplicationIdAndStageId(app1.getId(), Stage.ID_BUILD))
        .usingElementComparator(Comparator.comparing(PolicyEvaluation::getId))
        .containsExactlyInAnyOrder(evaluation1, evaluation2);
  }

  @Test
  public void testGetPrimaryNonMonitoringByApplicationIdAndStageId_ComplianceStage_IsNotIncluded() {
    Application app1 = tempEntity.newApplicationWithParent();

    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_COMPLIANCE, "scan1", false, false, new Date());
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_COMPLIANCE, "scan2", false, false, new Date());

    assertThat(dao.getPrimaryNonMonitoringByApplicationIdAndStageId(app1.getId(), Stage.ID_COMPLIANCE)).isEmpty();
  }

  @Test
  public void testGetPrimaryForMonitoringByApplicationId() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();

    PolicyEvaluation evaluation1 =
        tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, true, new Date());
    PolicyEvaluation evaluation2 =
        tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_OPERATE, "scan2", false, true, new Date());
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_OPERATE, "scan2", true, true, new Date());
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_OPERATE, "scan3", false, false, new Date());
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan4", false, true, new Date());

    assertThat(dao.getPrimaryForMonitoringByApplicationId(app1.getId()))
        .usingElementComparator(Comparator.comparing(PolicyEvaluation::getId))
        .containsExactlyInAnyOrder(evaluation1, evaluation2);
  }

  @Test
  public void testGetPrimaryForMonitoringByApplicationId_ComplianceStage_IsNotIncluded() {
    Application app1 = tempEntity.newApplicationWithParent();

    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_COMPLIANCE, "scan1", false, true, new Date());
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_COMPLIANCE, "scan2", false, true, new Date());

    assertThat(dao.getPrimaryForMonitoringByApplicationId(app1.getId())).isEmpty();
  }

  @Test
  public void testGetLastByCommitHash() {
    final Date date = new Date();
    final Application app1 = tempEntity.newApplicationWithParent();
    final Application app2 = tempEntity.newApplicationWithParent();

    tempEntity
        .newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            new Date(date.getTime() - 1000), COMMIT_HASH);
    PolicyEvaluation evaluation2 =
        tempEntity
            .newPolicyEvaluation(app2.getId(), Stage.ID_OPERATE, "scan2", false, false, false, date, COMMIT_HASH);

    assertThat(dao.getLastByCommitHash(COMMIT_HASH).getId())
        .isEqualTo(evaluation2.getId());
  }

  @Test
  public void testGetLastByCommitHashPerApplication() {
    // given: two applications with multiple policy evaluations each
    final Application app1 = tempEntity.newApplication("app1", organization.getId());
    final Application app2 = tempEntity.newApplication("app2", organization.getId());
    Date now = new Date();
    tempEntity
        .newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "app1_old", new Date(now.getTime() - 1000), COMMIT_HASH);
    tempEntity
        .newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "app1_new", new Date(now.getTime() - 500), COMMIT_HASH);
    tempEntity
        .newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "app2_old", new Date(now.getTime() - 750), COMMIT_HASH);
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "app2_new", now, COMMIT_HASH);

    // when: get latest policy evaluation for each application for the given commit
    List<PolicyEvaluation> policyEvaluations = dao.getLastByCommitHashPerApplication(COMMIT_HASH);

    // then: we have the latest evals for each app, identified by their scan IDs
    Set<String> scanIds = policyEvaluations.stream().map(PolicyEvaluation::getScanId).collect(Collectors.toSet());
    assertThat(scanIds).containsExactlyInAnyOrder("app1_new", "app2_new");
  }

  @Test
  public void testGetLastByCommitHash_NotFound() {
    assertThat(dao.getLastByCommitHash(COMMIT_HASH))
        .isNull();
  }

  @Test
  public void testGetLastByApplicationAndCommitHash() {
    final Date date = new Date();
    final Application app1 = tempEntity.newApplicationWithParent();

    tempEntity
        .newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            new Date(date.getTime() - 1000), COMMIT_HASH);
    final PolicyEvaluation evaluation2 =
        tempEntity
            .newPolicyEvaluation(app1.getId(), Stage.ID_OPERATE, "scan2", false, false, false, date, COMMIT_HASH);

    assertThat(dao.getLastByApplicationAndCommitHash(app1.getId(), COMMIT_HASH).getId())
        .isEqualTo(evaluation2.getId());
  }

  @Test
  public void testGetLastByApplicationAndCommitHash_NotFound() {
    final Date date = new Date();
    final Application app1 = tempEntity.newApplicationWithParent();
    final Application app2 = tempEntity.newApplicationWithParent();

    tempEntity
        .newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            new Date(date.getTime() - 1000), COMMIT_HASH.replace('a', '0'));

    tempEntity
        .newPolicyEvaluation(app2.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            new Date(date.getTime() - 1000), COMMIT_HASH);

    assertThat(dao.getLastByApplicationAndCommitHash(app1.getId(), COMMIT_HASH))
        .isNull();
  }

  @Test
  public void testGetLastByApplicationAndAbbreviatedCommitHash() {
    final Date date = new Date();
    final Application app1 = tempEntity.newApplicationWithParent();

    tempEntity
        .newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            new Date(date.getTime() - 1000), COMMIT_HASH);
    final PolicyEvaluation evaluation2 =
        tempEntity
            .newPolicyEvaluation(app1.getId(), Stage.ID_OPERATE, "scan2", false, false, false, date, COMMIT_HASH);

    assertThat(dao.getLastByApplicationAndAbbreviatedCommitHash(app1.getId(), COMMIT_HASH.substring(0, 7)).getId())
        .isEqualTo(evaluation2.getId());
  }

  @Test
  public void testGetLastByApplicationAndAbbreviatedCommitHash_NotFound() {
    final Date date = new Date();
    final Application app1 = tempEntity.newApplicationWithParent();
    final Application app2 = tempEntity.newApplicationWithParent();

    tempEntity
        .newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            new Date(date.getTime() - 1000), COMMIT_HASH.replace('a', '0'));

    tempEntity
        .newPolicyEvaluation(app2.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            new Date(date.getTime() - 1000), COMMIT_HASH);
    assertThat(dao.getLastByApplicationAndAbbreviatedCommitHash(app1.getId(), COMMIT_HASH.substring(0, 7)))
        .isNull();
  }

  @Test
  public void testHasExternalPolicyEvaluations() {
    final Application app1 = tempEntity.newApplicationWithParent();
    OffsetDateTime now = OffsetDateTime.now();
    final Date cutoffDate = Date.from(now.minusDays(7).toInstant());

    Date scanTime = Date.from(now.minusHours(2).toInstant());

    // add internally triggered policy evaluation
    tempEntity.newPolicyEvaluation(app1.getId(), StageTypes.SOURCE.getId(), "scan1", false, false, false,
        scanTime, "commitHash1", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    assertThat(dao.hasExternalPolicyEvaluations(app1.getId(), cutoffDate)).isFalse();

    // add externally triggered policy evaluation
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan2", false, false, false,
        scanTime, "commitHash2");

    assertThat(dao.hasExternalPolicyEvaluations(app1.getId(), cutoffDate)).isTrue();
  }

  @Test
  public void testGetCount() {
    String stageTypeId = ReleaseStageType.ID;

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time2);

    assertThat(dao.getCount()).isEqualTo(2);
  }

  @Test
  public void testGetLastInTimeRangeByApplicationAndStage() {
    final Date baselineDate = new Date();
    final Date minDate = new Date(baselineDate.getTime() - 2500);
    final Date maxDate = new Date(baselineDate.getTime() - 50);
    final Application app1 = tempEntity.newApplicationWithParent();
    final Application app2 = tempEntity.newApplicationWithParent();

    // the expected eval
    PolicyEvaluation expected = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(minDate.getTime() + 1000));

    // later, but different app
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(minDate.getTime() + 1500));

    // later, but different stage
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan1", false, false,
        new Date(minDate.getTime() + 1500));

    // after the time window
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false, new Date(maxDate.getTime()));

    // before the expected eval
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(minDate.getTime() + 500));

    assertThat(dao.getLastInTimeRangeByApplicationAndStage(app1.getId(), Stage.ID_RELEASE, minDate, maxDate))
        .extracting(PolicyEvaluation::getId)
        .isEqualTo(expected.getId());
  }

  @Test
  public void testGetLastInTimeRangeByApplicationAndStage_MinDate() {
    final Date baselineDate = new Date();
    final Date minDate = new Date(baselineDate.getTime() - 2500);
    final Date maxDate = new Date(baselineDate.getTime() - 50);
    final Application app1 = tempEntity.newApplicationWithParent();

    // the expected eval
    PolicyEvaluation expected = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(minDate.getTime()));

    assertThat(dao.getLastInTimeRangeByApplicationAndStage(app1.getId(), Stage.ID_RELEASE, minDate, maxDate))
        .extracting(PolicyEvaluation::getId)
        .isEqualTo(expected.getId());
  }

  @Test
  public void testGetLastInTimeRangeByApplicationAndStage_NoneMatch() {
    final Date baselineDate = new Date();
    final Date minDate = new Date(baselineDate.getTime() - 2500);
    final Date maxDate = new Date(baselineDate.getTime() - 50);
    final Application app1 = tempEntity.newApplicationWithParent();
    final Application app2 = tempEntity.newApplicationWithParent();

    // before the minDate
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(minDate.getTime() - 1));

    // later, but different app
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(minDate.getTime() + 1500));

    // later, but different stage
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan1", false, false,
        new Date(minDate.getTime() + 1500));

    // after the time window
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false, new Date(maxDate.getTime()));

    assertThat(dao.getLastInTimeRangeByApplicationAndStage(app1.getId(), Stage.ID_RELEASE, minDate, maxDate)).isNull();
  }

  @Test
  public void testGetLastInTimeRangeByApplicationAndStage_NullMaxDate() {
    final Date baselineDate = new Date();
    final Date minDate = new Date(baselineDate.getTime() - 2500);
    final Application app1 = tempEntity.newApplicationWithParent();
    final Application app2 = tempEntity.newApplicationWithParent();

    // earlier
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(minDate.getTime() + 1000));

    // later, but different app
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(minDate.getTime() + 1500));

    // later, but different stage
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan1", false, false,
        new Date(minDate.getTime() + 1500));

    // the expected eval
    PolicyEvaluation expected = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(minDate.getTime() + 1500));

    assertThat(dao.getLastInTimeRangeByApplicationAndStage(app1.getId(), Stage.ID_RELEASE, minDate, null))
        .extracting(PolicyEvaluation::getId)
        .isEqualTo(expected.getId());
  }

  @Test
  public void testGetLatestEvaluationPerWindow() {
    final Date baselineDate = new Date();
    final Date minDate = new Date(baselineDate.getTime() - 2500);
    final Date maxDate = new Date(baselineDate.getTime() - 50);
    final Application app1 = tempEntity.newApplicationWithParent();
    final Application app2 = tempEntity.newApplicationWithParent();

    // inclusive lower bound: exactly on minDate is included, but it is not the latest in the window
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false, new Date(minDate.getTime()));

    PolicyEvaluation releaseLatest = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan2", false,
        false, new Date(minDate.getTime() + 1000));

    PolicyEvaluation build = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan3", false, false,
        new Date(minDate.getTime() + 1500));

    // exclusive upper bound: exactly on maxDate is excluded
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan4", false, false, new Date(maxDate.getTime()));

    // before the window
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan5", false, false,
        new Date(minDate.getTime() - 1));

    // stage not requested
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_STAGE_RELEASE, "scan6", false, false,
        new Date(minDate.getTime() + 1000));

    // different app
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_RELEASE, "scan7", false, false,
        new Date(minDate.getTime() + 1000));

    List<StageEvaluationWindow> windows = List.of(
        new StageEvaluationWindow(Stage.ID_RELEASE, minDate, maxDate),
        new StageEvaluationWindow(Stage.ID_BUILD, minDate, maxDate));

    assertThat(dao.getLatestEvaluationPerWindow(app1.getId(), windows))
        .extracting(PolicyEvaluation::getId)
        .containsExactlyInAnyOrder(releaseLatest.getId(), build.getId());
  }

  @Test
  public void testGetLatestEvaluationPerWindow_NullMaxDate() {
    final Date baselineDate = new Date();
    final Date minDate = new Date(baselineDate.getTime() - 2500);
    final Application app1 = tempEntity.newApplicationWithParent();
    final Application app2 = tempEntity.newApplicationWithParent();

    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(minDate.getTime() + 1000));

    PolicyEvaluation later = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan2", false, false,
        new Date(minDate.getTime() + 5000));

    // before the window
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_RELEASE, "scan3", false, false,
        new Date(minDate.getTime() - 1));

    // different app
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_RELEASE, "scan4", false, false,
        new Date(minDate.getTime() + 2000));

    assertThat(dao.getLatestEvaluationPerWindow(app1.getId(),
        List.of(new StageEvaluationWindow(Stage.ID_RELEASE, minDate, null))))
            .extracting(PolicyEvaluation::getId)
            .containsExactly(later.getId());
  }

  @Test
  public void testGetLatestEvaluationPerWindow_DistinctWindowsSameStage() {
    final Date base = new Date();
    final Application app = tempEntity.newApplicationWithParent();

    PolicyEvaluation early = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(base.getTime() + 1000));
    PolicyEvaluation late = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan2", false, false,
        new Date(base.getTime() + 3000));

    // two violations on the same stage with different windows must resolve to different evaluations
    StageEvaluationWindow boundedToEarly =
        new StageEvaluationWindow(Stage.ID_RELEASE, new Date(base.getTime()), new Date(base.getTime() + 2000));
    StageEvaluationWindow openEndedFromLate =
        new StageEvaluationWindow(Stage.ID_RELEASE, new Date(base.getTime() + 2500), null);

    assertThat(dao.getLatestEvaluationPerWindow(app.getId(), List.of(boundedToEarly, openEndedFromLate)))
        .extracting(PolicyEvaluation::getId)
        .containsExactlyInAnyOrder(early.getId(), late.getId());
  }

  @Test
  public void testGetLatestEvaluationPerWindow_NoMatchContributesNothing() {
    final Date base = new Date();
    final Application app = tempEntity.newApplicationWithParent();

    PolicyEvaluation build = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scan1", false, false,
        new Date(base.getTime() + 1000));

    // the RELEASE window has no matching evaluation; the BUILD window does
    assertThat(dao.getLatestEvaluationPerWindow(app.getId(), List.of(
        new StageEvaluationWindow(Stage.ID_RELEASE, new Date(base.getTime()), null),
        new StageEvaluationWindow(Stage.ID_BUILD, new Date(base.getTime()), null))))
            .extracting(PolicyEvaluation::getId)
            .containsExactly(build.getId());
  }

  @Test
  public void testGetLatestEvaluationPerWindow_DuplicateWindowsCoalesced() {
    final Date base = new Date();
    final Application app = tempEntity.newApplicationWithParent();

    PolicyEvaluation latest = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan1", false, false,
        new Date(base.getTime() + 1000));

    StageEvaluationWindow window = new StageEvaluationWindow(Stage.ID_RELEASE, new Date(base.getTime()), null);

    assertThat(dao.getLatestEvaluationPerWindow(app.getId(), List.of(window, window)))
        .extracting(PolicyEvaluation::getId)
        .containsExactly(latest.getId());
  }

  @Test
  public void testGetLatestEvaluationPerWindow_EmptyWindows() {
    final Application app1 = tempEntity.newApplicationWithParent();

    assertThat(dao.getLatestEvaluationPerWindow(app1.getId(), List.of())).isEmpty();
  }

  @Test
  public void testGetLatestEvaluationPerWindow_NullWindows() {
    final Application app1 = tempEntity.newApplicationWithParent();

    assertThat(dao.getLatestEvaluationPerWindow(app1.getId(), null)).isEmpty();
  }

  @Test
  public void testGetLimitedAmountByApplicationId_none() {
    // when fetching evaluations
    List<PolicyEvaluation> policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), 100, null);

    // then assert that results are not null, and are empty
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).isEmpty();
  }

  @Test
  public void testGetLimitedAmountByApplicationId_single() {
    // setup
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "scan1", false, false, new Date());

    // when fetching evaluations
    List<PolicyEvaluation> policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), 100, null);

    // then assert that 1 evaluation is returned
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).hasSize(1);
  }

  @Test
  public void testGetLimitedAmountByApplicationId_multiple() {
    // setup
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "scan1", false, false, new Date());
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "scan2", false, false, new Date());

    // when fetching evaluations
    List<PolicyEvaluation> policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), 100, null);

    // then assert that 2 evaluations are returned
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).hasSize(2);
  }

  @Test
  public void testGetLimitedAmountByApplicationId_byStage() {
    // setup
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan0", false, false, new Date());
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "scan1", false, false, new Date());
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan2", false, false, new Date());
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_SOURCE, "scan3", false, false, new Date());

    // when fetching evaluations by stage build
    List<PolicyEvaluation> policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), 100, "build");

    // then assert that evaluations for build are returned only
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).hasSize(2);
    assertThat(policyEvaluations.get(0).getStageTypeId()).isEqualTo("build");
    assertThat(policyEvaluations.get(1).getStageTypeId()).isEqualTo("build");
  }

  @Test
  public void testGetLimitedAmountByApplicationId_byStageAndLimit() {
    Date yesterday = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    Date today = Date.from(Instant.now());

    // setup
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan0", false, false, yesterday);
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "scan1", false, false, today);
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan2", false, false, today);
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_SOURCE, "scan3", false, false, today);

    // when fetching evaluations by stage build
    List<PolicyEvaluation> policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), 1, "build");

    // then assert that only the most recent evaluation for stage build is returned
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).hasSize(1);
    assertThat(policyEvaluations.get(0).getStageTypeId()).isEqualTo("build");
    assertThat(policyEvaluations.get(0).getScanId()).isEqualTo("scan2");
  }

  @Test
  public void testGetLimitedAmountByApplicationId_limited() {
    // setup
    final int policyEvalCount = 5;
    for (int i = 0; i < policyEvalCount; i++) {
      tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "scan" + i, false, false, new Date());
    }

    // when fetching evaluations
    List<PolicyEvaluation> policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), 100, null);

    // then assert that 5 evaluations are returned
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).hasSize(policyEvalCount);

    // when fetching evaluations with limit lower than the number
    policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), policyEvalCount - 1, null);

    // then assert that only the specified max is retrieved
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).hasSize(policyEvalCount - 1);
  }

  @Test
  public void testGetLastByApplicationIdCommitHashAndStageId() {
    // setup
    String commitHash = "hash";

    // add a couple BUILD stage policy evaluations for the same commit hash
    Calendar now = Calendar.getInstance();
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan-build-1", now.getTime(), commitHash);
    now.add(Calendar.MINUTE, 10);
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan-build-2", now.getTime(), commitHash);

    // add one DEVELOP stage policy evaluation for the same commit hash
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_DEVELOP, "scan-develop-1", now.getTime(), commitHash);

    // when fetching last BUILD evaluation for the given app and commit hash
    PolicyEvaluation policyEvaluation =
        dao.getLastByApplicationIdCommitHashAndStageId(application.getId(), commitHash, Stage.ID_BUILD);

    // then assert that the second BUILD evaluation is returned
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation).extracting(PolicyEvaluation::getScanId).isEqualTo("scan-build-2");

    // when fetching last DEVELOP evaluation for the given app and commit hash
    policyEvaluation =
        dao.getLastByApplicationIdCommitHashAndStageId(application.getId(), commitHash, Stage.ID_DEVELOP);

    // then assert that the only DEVELOP evaluation is returned
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation).extracting(PolicyEvaluation::getScanId).isEqualTo("scan-develop-1");

    // when fetching last RELEASE evaluation for the given app and commit hash
    policyEvaluation =
        dao.getLastByApplicationIdCommitHashAndStageId(application.getId(), commitHash, Stage.ID_RELEASE);

    // then assert that no evaluation is returned
    assertThat(policyEvaluation).isNull();

    // when fetching last evaluation for null commit hash
    policyEvaluation =
        dao.getLastByApplicationIdCommitHashAndStageId(application.getId(), null, Stage.ID_BUILD);

    // then assert that no evaluation is returned
    assertThat(policyEvaluation).isNull();

    // when fetching last evaluation for non-existent app and given commit hash
    policyEvaluation =
        dao.getLastByApplicationIdCommitHashAndStageId("no-application", commitHash, Stage.ID_BUILD);

    // then assert that no evaluation is returned
    assertThat(policyEvaluation).isNull();

    // when fetching last evaluation for the given app and non-existent commit hash
    policyEvaluation =
        dao.getLastByApplicationIdCommitHashAndStageId(application.getId(), "no-hash", Stage.ID_BUILD);

    // then assert that no evaluation is returned
    assertThat(policyEvaluation).isNull();
  }

  @Test
  public void testGetLastByApplicationAndCommitHashAndTriggerType() {
    // setup
    String commitHash = "hash";

    // add a couple BUILD stage policy evaluations for the same commit hash
    Calendar now = Calendar.getInstance();
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan-build-1",
        false, false, false, now.getTime(), commitHash, ScanTriggerType.CLI);
    now.add(Calendar.MINUTE, 10);
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan-build-2",
        false, false, false, now.getTime(), commitHash, ScanTriggerType.CLI);
    now.add(Calendar.MINUTE, 10);

    // add one DEVELOP stage policy evaluation for the same commit hash
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_DEVELOP, "scan-develop-1",
        false, false, false, now.getTime(), commitHash,
        ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST);

    // when fetching last external evaluation for the given app and commit hash
    PolicyEvaluation policyEvaluation =
        dao.getLastByApplicationAndCommitHashAndTriggerType(application.getId(), commitHash, true);

    // then assert that the second BUILD evaluation is returned
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation).extracting(PolicyEvaluation::getScanId).isEqualTo("scan-build-2");

    // when fetching last internal evaluation for the given app and commit hash
    policyEvaluation =
        dao.getLastByApplicationAndCommitHashAndTriggerType(application.getId(), commitHash, false);

    // then assert that the DEVELOP evaluation is returned
    assertThat(policyEvaluation).isNotNull();
    assertThat(policyEvaluation).extracting(PolicyEvaluation::getScanId).isEqualTo("scan-develop-1");
  }

  @Test
  public void testHasCIIntegrationEvaluation() {
    final long cutOffFiveSecondsAgo = 5000;
    final Date evalTime = new Date();
    final Date cutOffWindow = new Date(evalTime.getTime() - cutOffFiveSecondsAgo);

    // App 1 - checks all the boxes
    final Application application1 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application1.getId(), Stage.ID_BUILD, "scan-build-1",
        false, false, false, evalTime, "hash-1", ScanTriggerType.CONTINUOUS_INTEGRATION);

    // App 2 check all the boxes,
    final Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application2.getId(), Stage.ID_BUILD, "scan-build-2",
        false, false, false, evalTime, "hash-2", ScanTriggerType.CLI);

    // App 3 was not a BUILD stage evaluation, so it does not check all the boxes
    final Application application3 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application3.getId(), Stage.ID_DEVELOP, "scan-build-3",
        false, false, false, evalTime, "hash-3", ScanTriggerType.CLI);

    // App 4 has no evaluations, should return false
    final Application application4 = tempEntity.newApplication(organization.getId());

    // App 5 would qualify but is outside the cut off window
    final Application application5 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application5.getId(), Stage.ID_BUILD, "scan-build-5",
        false, false, false, new Date(evalTime.getTime() - 6000), "hash-5", ScanTriggerType.CLI);

    // App 6 is a reevaluation, so it does not check all the boxes
    final Application application6 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application6.getId(), Stage.ID_BUILD, "scan-build-6",
        true, false, false, evalTime, "hash-6", ScanTriggerType.CLI);

    // App 7 is for monitoring, so it does not check all the boxes
    final Application application7 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application7.getId(), Stage.ID_BUILD, "scan-build-7",
        false, true, false, evalTime, "hash-7", ScanTriggerType.CLI);

    final boolean hasCIIntegrationEvaluationApp1 = dao.hasCIIntegrationEvaluation(application1.getId(), cutOffWindow);
    assertThat(hasCIIntegrationEvaluationApp1).isTrue();

    final boolean hasCIIntegrationEvaluationApp2 = dao.hasCIIntegrationEvaluation(application2.getId(), cutOffWindow);
    assertThat(hasCIIntegrationEvaluationApp2).isTrue();

    final boolean hasCIIntegrationEvaluationApp3 = dao.hasCIIntegrationEvaluation(application3.getId(), cutOffWindow);
    assertThat(hasCIIntegrationEvaluationApp3).isFalse();

    final boolean hasCIIntegrationEvaluationApp4 = dao.hasCIIntegrationEvaluation(application4.getId(), cutOffWindow);
    assertThat(hasCIIntegrationEvaluationApp4).isFalse();

    final boolean hasCIIntegrationEvaluationApp5 = dao.hasCIIntegrationEvaluation(application5.getId(), cutOffWindow);
    assertThat(hasCIIntegrationEvaluationApp5).isFalse();

    final boolean hasCIIntegrationEvaluationApp6 = dao.hasCIIntegrationEvaluation(application6.getId(), cutOffWindow);
    assertThat(hasCIIntegrationEvaluationApp6).isFalse();

    final boolean hasCIIntegrationEvaluationApp7 = dao.hasCIIntegrationEvaluation(application7.getId(), cutOffWindow);
    assertThat(hasCIIntegrationEvaluationApp7).isFalse();
  }

  @Test
  public void testGetBoundedCountOfApplicationsWithCiCdTriggeredEvaluations_shouldRespectUpperAndLowerBounds() {
    final Date now = new Date();
    final long oneWeekMS = 604800000L;
    final Organization organization = tempEntity.newOrganization();

    // app 1- evaluated 1 weeks ago
    final Application application1 = tempEntity.newApplication(organization.getId());
    final Date oneWeekAgo = new Date(now.getTime() - oneWeekMS);
    tempEntity.newPolicyEvaluation(
        application1.getId(),
        Stage.ID_BUILD,
        "scan-build-1",
        false,
        false,
        false,
        oneWeekAgo,
        "hash-1",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // app 2- evaluated 2 weeks ago
    final Application application2 = tempEntity.newApplication(organization.getId());
    final Date twoWeeksAgo = new Date(now.getTime() - oneWeekMS * 2);
    tempEntity.newPolicyEvaluation(
        application2.getId(),
        Stage.ID_BUILD,
        "scan-build-2",
        false,
        false,
        false,
        twoWeeksAgo,
        "hash-2",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // app 3 - evaluated 3 weeks ago
    final Application application3 = tempEntity.newApplication(organization.getId());
    final Date threeWeeksAgo = new Date(now.getTime() - oneWeekMS * 3);
    tempEntity.newPolicyEvaluation(
        application3.getId(),
        Stage.ID_BUILD,
        "scan-build-3",
        false,
        false,
        false,
        threeWeeksAgo,
        "hash-3",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // should pick up all the apps
    int results = dao.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(
        threeWeeksAgo,
        now);
    assertThat(results).isEqualTo(3);

    // should cut off app3 because the eval is too old
    results = dao.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(
        new Date(threeWeeksAgo.getTime() + 1),
        now);
    assertThat(results).isEqualTo(2);

    // should cut off app2 and 3 because the evals are too old
    results = dao.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(
        new Date(twoWeeksAgo.getTime() + 1),
        now);
    assertThat(results).isEqualTo(1);

    // should pick up all the apps
    results = dao.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(
        threeWeeksAgo,
        oneWeekAgo);
    assertThat(results).isEqualTo(3);

    // should cut off app3 because the eval is too new
    results = dao.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(
        threeWeeksAgo,
        new Date(oneWeekAgo.getTime() - 1));
    assertThat(results).isEqualTo(2);
  }

  @Test
  public void testGetBoundedCountOfApplicationsWithCiCdTriggeredEvaluations_shouldFilterOutReevals() {
    final Date now = new Date();
    final long oneWeekMS = 604800000L;
    final Organization organization = tempEntity.newOrganization();

    // app 1- will be counted
    final Application application1 = tempEntity.newApplication(organization.getId());
    final Date oneWeekAgo = new Date(now.getTime() - oneWeekMS);
    tempEntity.newPolicyEvaluation(
        application1.getId(),
        Stage.ID_BUILD,
        "scan-build-1",
        false,
        false,
        false,
        oneWeekAgo,
        "hash-1",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // app 2- will not be counted it's a reeval
    final Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(
        application2.getId(),
        Stage.ID_BUILD,
        "scan-build-2",
        true,
        false,
        false,
        oneWeekAgo,
        "hash-2",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    int results = dao.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(
        oneWeekAgo,
        now);
    assertThat(results).isEqualTo(1);
  }

  // There is a third parameter we filter on, isForObsolete scan. That is not tested here because the dao will not
  // let you create new evaluation where isReevaluation is false and isForObsolete scan is true. Since we also
  // filter on isReevaluation we can't test this interdependently
  @Test
  public void testGetBoundedCountOfApplicationsWithCiCdTriggeredEvaluations_shouldFilterOutContinuousMonitoring() {
    final Date now = new Date();
    final long oneWeekMS = 604800000L;
    final Organization organization = tempEntity.newOrganization();

    // app 1- will be counted
    final Application application1 = tempEntity.newApplication(organization.getId());
    final Date oneWeekAgo = new Date(now.getTime() - oneWeekMS);
    tempEntity.newPolicyEvaluation(
        application1.getId(),
        Stage.ID_BUILD,
        "scan-build-1",
        false,
        false,
        false,
        oneWeekAgo,
        "hash-1",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    // app 2- will not be counted it's from continuous monitoring
    final Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(
        application2.getId(),
        Stage.ID_BUILD,
        "scan-build-2",
        false,
        true,
        false,
        oneWeekAgo,
        "hash-2",
        ScanTriggerType.CONTINUOUS_INTEGRATION);

    int results = dao.getBoundedCountOfApplicationsWithCiCdTriggeredEvaluations(
        oneWeekAgo,
        now);
    assertThat(results).isEqualTo(1);
  }

  @Test
  public void testGetLastByApplicationIdAndStageIdNoMonitoringNoReeval() {
    final long currentTime = System.currentTimeMillis();
    // Is continuous monitoring and build stage - incorrect configuration
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan-build-1",
        false, true, false, new Date(currentTime - 300), "hash-1", ScanTriggerType.CLI);
    // Is reevaluation and build stage - incorrect configuration
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan-build-2",
        true, false, false, new Date(currentTime - 300), "hash-2", ScanTriggerType.CLI);
    // Is both reevaluation and continuous monitoring, build stage, 2nd latest - incorrect configuration
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan-build-3",
        true, true, false, new Date(currentTime - 100), "hash-3", ScanTriggerType.CLI);
    // Is neither reevaluation nor continuous monitoring, not build stage, but latest - incorrect configuration
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_SOURCE, "scan-build-4",
        false, false, false, new Date(currentTime), "hash-4", ScanTriggerType.CLI);
    // Is neither reevaluation nor continuous monitoring, build stage, 3rd latest - correct configuration
    final PolicyEvaluation expectedPolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scan-build-5", false,
            false, false, new Date(currentTime - 200), "hash-5", ScanTriggerType.CLI);

    final PolicyEvaluation latestPolicyEvaluation =
        dao.getLastByApplicationIdAndStageIdNoMonitoringNoReeval(application.getId(), Stage.ID_BUILD);
    assertThat(latestPolicyEvaluation.getId())
        .isEqualTo(expectedPolicyEvaluation.getId());
  }

  @Test
  public void testGetByApplicationId() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    assertThat(dao.getByApplicationId(app.getId(), 1, 2)).isEmpty();
    assertThat(dao.getByApplicationId(app.getId(), 2, 2)).isEmpty();
    assertThat(dao.getByApplicationId(app.getId(), 3, 2)).isEmpty();

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId1");
    Thread.sleep(50);

    assertThat(dao.getByApplicationId(app.getId(), 1, 2))
        .extracting(PolicyEvaluation::getId)
        .containsExactly(eval1.getId());
    assertThat(dao.getByApplicationId(app.getId(), 2, 2)).isEmpty();
    assertThat(dao.getByApplicationId(app.getId(), 3, 2)).isEmpty();

    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_STAGE_RELEASE, "scanId2");
    Thread.sleep(50);

    assertThat(dao.getByApplicationId(app.getId(), 1, 2))
        .extracting(PolicyEvaluation::getId)
        .containsExactly(eval1.getId(), eval2.getId());
    assertThat(dao.getByApplicationId(app.getId(), 2, 2)).isEmpty();
    assertThat(dao.getByApplicationId(app.getId(), 3, 2)).isEmpty();

    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scanId3");

    assertThat(dao.getByApplicationId(app.getId(), 1, 2))
        .extracting(PolicyEvaluation::getId)
        .containsExactly(eval1.getId(), eval2.getId());
    assertThat(dao.getByApplicationId(app.getId(), 2, 2))
        .extracting(PolicyEvaluation::getId)
        .containsExactly(eval3.getId());
    assertThat(dao.getByApplicationId(app.getId(), 3, 2)).isEmpty();
  }

  @Test
  public void testGetByScanIdAndApplicationId_Found() {
    Application app = tempEntity.newApplicationWithParent();
    String scanId = "test-scan-id";

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    PolicyEvaluation result = dao.getByScanIdAndApplicationId(scanId, app.getId());

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(eval.getId());
    assertThat(result.getScanId()).isEqualTo(scanId);
    assertThat(result.getApplicationId()).isEqualTo(app.getId());
  }

  @Test
  public void testGetByScanIdAndApplicationId_NotFound() {
    Application app = tempEntity.newApplicationWithParent();
    String scanId = "non-existent-scan";

    PolicyEvaluation result = dao.getByScanIdAndApplicationId(scanId, app.getId());

    assertThat(result).isNull();
  }

  @Test
  public void testGetByScanIdAndApplicationId_WrongApplication() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    String scanId = "test-scan-id";

    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, scanId);

    PolicyEvaluation result = dao.getByScanIdAndApplicationId(scanId, app2.getId());

    assertThat(result).isNull();
  }

  @FunctionalInterface
  interface PolicyEvaluationChooser
  {
    PolicyEvaluation choose(PolicyEvaluation sourcePolicyEvaluation, PolicyEvaluation targetPolicyEvaluation);
  }
}
