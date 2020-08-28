/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;

import com.google.common.collect.Sets;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyEvaluationDAOTest
    extends AbstractDbDAOTest
{
  private static final String COMMIT_HASH = "abcdef1234abcdef1234abcdef1234abcdef1234";

  @Test
  public void testCRUD() throws Exception {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    // Create
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(application.getId(), stageTypeId, scanId, "system");
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
    assertThatThrownBy(() -> {
      dao.update(policyEvaluationToUpdate);
    }).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("The PolicyEvaluation table does not support update operations");

    // Delete
    dao.delete(policyEvaluation);

    policyEvaluation = dao.getById(policyEvaluation.getId());
    assertThat(policyEvaluation).isNull();
  }

  @Test
  public void testGetLastByApplicationIdAndScanId() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    assertThat(policyEvaluations).noneMatch(Objects::isNull).allSatisfy(pe -> assertThat(pe.getTime()).isEqualTo(time2))
        .extracting(PolicyEvaluation::getId).containsExactlyInAnyOrder(pe2.getId(), pe4.getId());
  }

  @Test
  public void testGetLastByApplicationIdsAndStageIds_ReevaluationOfOldScan() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    assertThat(policyEvaluations).noneMatch(Objects::isNull).noneMatch(PolicyEvaluation::isForObsoleteScan)
        .extracting(PolicyEvaluation::getId).containsExactlyInAnyOrder(pe2.getId(), pe5.getId(), pe8.getId());
  }

  @Test
  public void testGetLastByApplicationIdsAndStageIds_InOperatorOptimizationForH2() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    Date time1 = new Date();
    Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, "scanId1", time1);
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId2", time1);
    tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scanId3", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId4", time2);

    Set<String> appIds = new LinkedHashSet<>();
    while (appIds.size() < PolicyEvaluationDAO.H2_IN_OPERATOR_THRESHOLD) {
      appIds.add(tempEntity.uuid());
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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(application.getId(), ReleaseStageType.ID, "scanId", "system");
    policyEvaluation.setForObsoleteScan(true);
    assertThatThrownBy(() -> {
      dao.insert(policyEvaluation);
    }).isInstanceOf(IllegalStateException.class).hasMessage("Primary evaluations cannot be for obsolete scans");
  }

  @Test
  public void testGetLastByApplicationIds() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
  public void testGetLastByApplicationIds_InOperatorOptimizationForH2() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;
    Date time1 = new Date();
    Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application2.getId(), stageTypeId, "scanId1", time1);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId3", time2);

    Set<String> appIds = new LinkedHashSet<>();
    while (appIds.size() < PolicyEvaluationDAO.H2_IN_OPERATOR_THRESHOLD) {
      appIds.add(tempEntity.uuid());
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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    dao.delete(pe2, true /* updateLastPolicyEvaluation */);

    lastPolicyEvaluation = dao.getLastByApplicationIdAndStageId(application.getId(), stageTypeId);
    assertThat(lastPolicyEvaluation.getId()).isEqualTo(pe1.getId());
  }

  @Test
  public void testDelete_DoNotUpdateLastPolicyEvaluation() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, time2);

    // Assert we have a last policy evaluation and that it is the second one
    PolicyEvaluation lastPolicyEvaluation = dao.getLastByApplicationIdAndStageId(application.getId(), stageTypeId);
    assertThat(lastPolicyEvaluation.getId()).isEqualTo(pe2.getId());

    // Delete the second evaluation. The last policy eval will be deleted and there will not be a new last policy eval
    // because we tell the delete to not update the last policy eval.
    dao.delete(pe2, false /* updateLastPolicyEvaluation */);

    lastPolicyEvaluation = dao.getLastByApplicationIdAndStageId(application.getId(), stageTypeId);
    assertThat(lastPolicyEvaluation).isNull();
  }

  @Test
  public void testDelete_cascadeToSourceControlDefaultBranchCommitHistory() {
    // given branch commit history that references a policy evaluation
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan", "commit");
    SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory =
        tempEntity.newSourceControlDefaultBranchCommitHistory(
            application.getId(), policyEvaluation.getCommitHash(), new Date(), policyEvaluation.getId()
        );
    SourceControlDefaultBranchCommitHistoryDAO defaultBranchCommitHistoryDAO =
        new SourceControlDefaultBranchCommitHistoryDAO();

    // when : fetch the history
    SourceControlDefaultBranchCommitHistory fetchedDefaultBranchCommitHistory =
        defaultBranchCommitHistoryDAO.getByApplicationIdAndPolicyEvaluationId(
            application.getId(),
            defaultBranchCommitHistory.getPolicyEvaluationId());

    // then : the history exists
    assertThat(fetchedDefaultBranchCommitHistory).isNotNull();

    // when : deleting the policy evaluation
    PolicyEvaluationDAO policyEvaluationDao = new PolicyEvaluationDAO();
    policyEvaluationDao.delete(policyEvaluation);

    // then : the history no longer exists
    fetchedDefaultBranchCommitHistory = defaultBranchCommitHistoryDAO.getByApplicationIdAndPolicyEvaluationId(
        application.getId(),
        defaultBranchCommitHistory.getPolicyEvaluationId());
    assertThat(fetchedDefaultBranchCommitHistory).isNull();
  }

  @Test
  public void testDelete_cascadeToSourceControlPullRequestCommentForSourcePolicyEvaluation() {
    testDelete_cascadeToSourceControlPullRequestComment(
        (sourcePolicyEvaluation, targetPolicyEvaluation) -> sourcePolicyEvaluation
    );
  }

  @Test
  public void testDelete_cascadeToSourceControlPullRequestCommentForTargetPolicyEvaluation() {
    testDelete_cascadeToSourceControlPullRequestComment(
        (sourcePolicyEvaluation, targetPolicyEvaluation) -> targetPolicyEvaluation
    );
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
        targetPolicyEvaluation.getId()
    );
    SourceControlPullRequestCommentDAO pullRequestCommentDAO = new SourceControlPullRequestCommentDAO();
    pullRequestCommentDAO.insert(pullRequestComment);

    final String componentHash = "componentHash1";

    SourceControlPullRequestComment lineComment = new SourceControlPullRequestComment(
        application.getId(),
        componentHash,
        2,
        3,
        4,
        sourcePolicyEvaluation.getId(),
        targetPolicyEvaluation.getId()
    );
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
    PolicyEvaluationDAO policyEvaluationDao = new PolicyEvaluationDAO();
    policyEvaluationDao.delete(policyEvaluationChooser.choose(sourcePolicyEvaluation, targetPolicyEvaluation));

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
        tempEntity.newSourceControlEvent(application, sourcePolicyEvaluation, null);

    SourceControlEventDAO sourceControlEventDAO = new SourceControlEventDAO();
    SourceControlEvent sourceControlEventByIdBeforeDelete = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventByIdBeforeDelete).isNotNull();

    // when the policy evaluation is deleted
    PolicyEvaluationDAO policyEvaluationDao = new PolicyEvaluationDAO();
    policyEvaluationDao.delete(sourcePolicyEvaluation);

    // then the source control event is deleted
    SourceControlEvent sourceControlEventByIAfterDelete = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventByIAfterDelete).isNull();
  }

  @Test
  public void testDelete_cascadeToSourceControlEventForTargetPolicyEvaluation() {
    // given a source control event with policy evaluations
    PolicyEvaluation sourcePolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");

    PolicyEvaluation targetPolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "targetScan", "targetCommit");

    SourceControlEvent sourceControlEvent =
        tempEntity.newSourceControlEvent(application, sourcePolicyEvaluation, targetPolicyEvaluation);

    SourceControlEventDAO sourceControlEventDAO = new SourceControlEventDAO();
    SourceControlEvent sourceControlEventByIdBeforeDelete = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventByIdBeforeDelete).isNotNull();

    // when the policy evaluation is deleted
    PolicyEvaluationDAO policyEvaluationDao = new PolicyEvaluationDAO();
    policyEvaluationDao.delete(targetPolicyEvaluation);

    // then the source control event is deleted
    SourceControlEvent sourceControlEventByIAfterDelete = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventByIAfterDelete).isNull();
  }

  @Test
  public void testGetBetweenDatesByApplicationIdAndStageIds() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
  public void testGetPrimaryForMonitoringByApplicationId() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
  public void testGetLastByCommitHash() {
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();
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
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    assertThat(dao.getLastByCommitHash(COMMIT_HASH))
        .isNull();
  }

  @Test
  public void testGetLastByApplicationAndCommitHash() {
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
  public void testGetCount() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time2);

    assertThat(dao.getCount()).isEqualTo(2);
  }

  @Test
  public void testGetLastInTimeRangeByApplicationAndStage() {
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
    final PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

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
  public void testGetLimitedAmountByApplicationId_none() {
    //setup
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    //when fetching evaluations
    List<PolicyEvaluation> policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), 100);

    //then assert that results are not null, and are empty
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).isEmpty();
  }

  @Test
  public void testGetLimitedAmountByApplicationId_single() {
    //setup
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "scan1", false, false, new Date());

    //when fetching evaluations
    List<PolicyEvaluation> policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), 100);

    //then assert that 1 evaluation is returned
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).hasSize(1);
  }

  @Test
  public void testGetLimitedAmountByApplicationId_multiple() {
    //setup
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "scan1", false, false, new Date());
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "scan2", false, false, new Date());

    //when fetching evaluations
    List<PolicyEvaluation> policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), 100);

    //then assert that 2 evaluations are returned
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).hasSize(2);
  }

  @Test
  public void testGetLimitedAmountByApplicationId_limited() {
    //setup
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();
    final int policyEvalCount = 5;
    for (int i = 0; i < policyEvalCount; i++) {
      tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, "scan" + i, false, false, new Date());
    }

    //when fetching evaluations
    List<PolicyEvaluation> policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), 100);

    //then assert that 5 evaluations are returned
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).hasSize(policyEvalCount);

    //when fetching evaluations with limit lower than the number
    policyEvaluations = dao.getLimitedAmountByApplicationId(application.getId(), policyEvalCount - 1);

    //then assert that only the specified max is retrieved
    assertThat(policyEvaluations).isNotNull();
    assertThat(policyEvaluations).hasSize(policyEvalCount - 1);
  }

  @FunctionalInterface
  interface PolicyEvaluationChooser
  {
    PolicyEvaluation choose(PolicyEvaluation sourcePolicyEvaluation, PolicyEvaluation targetPolicyEvaluation);
  }
}
