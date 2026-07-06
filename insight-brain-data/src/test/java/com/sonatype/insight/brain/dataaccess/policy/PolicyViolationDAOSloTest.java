/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the SLO violation feed queries on {@link PolicyViolationDAO}: an all-states paged query and matching count
 * for a single application at a single stage, with an optional {@code updatedSince} delta filter.
 */
public class PolicyViolationDAOSloTest
    extends AbstractDbDAOTest
{
  private PolicyViolationDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createPolicyViolationDAO();
  }

  @Test
  public void returnsAllStatesForAppAndStage_paged() {
    Set<String> targetIds = seedThreeReleaseViolationsInDifferentStates();

    // Negative controls: rows that must be excluded by the application_id / stage_type_id filters.
    seedOtherApplicationReleaseViolation();
    seedTargetApplicationBuildStageViolation();

    assertThat(dao.countByApplicationIdAndStage(application.getId(), ReleaseStageType.ID, null)).isEqualTo(3L);

    List<PolicyViolation> page =
        dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, null, 0, 100);
    assertThat(page).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(targetIds);
  }

  @Test
  public void paginationSplitsResults() {
    seedThreeReleaseViolationsInDifferentStates();

    List<PolicyViolation> allViolations =
        dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, null, 0, 100);
    Set<String> allIds = allViolations.stream().map(PolicyViolation::getId).collect(toSet());
    assertThat(allIds).hasSize(3);

    List<PolicyViolation> page1 =
        dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, null, 0, 2);
    List<PolicyViolation> page2 =
        dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, null, 2, 2);

    assertThat(page1).hasSize(2);
    assertThat(page2).hasSize(1);

    Set<String> page1Ids = page1.stream().map(PolicyViolation::getId).collect(toSet());
    Set<String> page2Ids = page2.stream().map(PolicyViolation::getId).collect(toSet());

    // Pages must be disjoint (no duplicates across pages) and together cover the full result set exactly.
    assertThat(page1Ids).doesNotContainAnyElementsOf(page2Ids);
    assertThat(page1Ids).hasSize(2);
    assertThat(page2Ids).hasSize(1);
    assertThat(Sets.union(page1Ids, page2Ids)).isEqualTo(allIds);
  }

  @Test
  public void updatedSinceFiltersByTimeColumns() {
    Policy policy = tempEntity.newPolicy(application);

    Date now = new Date();
    Date oldTime = DateUtils.addDays(now, -2);
    Date cutoff = DateUtils.addDays(now, -1);

    // The violation's open_time is derived from the policy evaluation time, so we drive open_time via the evaluation.
    PolicyEvaluation olderEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-old", oldTime);
    tempEntity.newPolicyViolation(olderEvaluation, policy);

    PolicyEvaluation newerEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-new", now);
    tempEntity.newPolicyViolation(newerEvaluation, policy);

    assertThat(dao.countByApplicationIdAndStage(application.getId(), ReleaseStageType.ID, null)).isEqualTo(2L);
    assertThat(dao.countByApplicationIdAndStage(application.getId(), ReleaseStageType.ID, cutoff)).isEqualTo(1L);
    assertThat(dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, cutoff, 0, 10))
        .hasSize(1);
  }

  @Test
  public void updatedSinceIsDrivenByFixTime() {
    Policy policy = tempEntity.newPolicy(application);

    Date now = new Date();
    Date oldTime = DateUtils.addDays(now, -2);
    Date cutoff = DateUtils.addDays(now, -1);

    // Opened before the cutoff (so the open_time OR-branch cannot match) but fixed at `now`. Only the
    // FIX_TIME.ge(...) branch of updatedSinceCondition can bring this violation into the delta; if that branch were
    // dropped this test would fail.
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-fixed", oldTime);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(evaluation, policy);
    fixedViolation.setFixTime(now);
    dao.update(fixedViolation);

    // Guard: open_time really is before the cutoff, so nothing but fix_time can satisfy the delta.
    assertThat(fixedViolation.getOpenTime()).isBefore(cutoff);

    assertThat(dao.countByApplicationIdAndStage(application.getId(), ReleaseStageType.ID, cutoff)).isEqualTo(1L);
    assertThat(dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, cutoff, 0, 10))
        .extracting(PolicyViolation::getId)
        .containsExactly(fixedViolation.getId());
  }

  @Test
  public void updatedSinceIsDrivenByWaiveTime() {
    Policy policy = tempEntity.newPolicy(application);

    Date now = new Date();
    Date oldTime = DateUtils.addDays(now, -2);
    Date cutoff = DateUtils.addDays(now, -1);

    // Opened before the cutoff (so the open_time OR-branch cannot match) but waived at `now`. Only the
    // WAIVE_TIME.ge(...) branch of updatedSinceCondition can bring this violation into the delta; if that branch were
    // dropped this test would fail.
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-waived", oldTime);
    PolicyViolation waivedViolation = tempEntity.newPolicyViolation(evaluation, policy);
    waivedViolation.setWaiveTime(now);
    dao.update(waivedViolation);

    // Guard: open_time really is before the cutoff, so nothing but waive_time can satisfy the delta.
    assertThat(waivedViolation.getOpenTime()).isBefore(cutoff);

    assertThat(dao.countByApplicationIdAndStage(application.getId(), ReleaseStageType.ID, cutoff)).isEqualTo(1L);
    assertThat(dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, cutoff, 0, 10))
        .extracting(PolicyViolation::getId)
        .containsExactly(waivedViolation.getId());
  }

  @Test
  public void updatedSinceIsInclusiveAtTheBoundary() {
    Policy policy = tempEntity.newPolicy(application);

    Date now = new Date();
    Date oldTime = DateUtils.addDays(now, -2);

    PolicyEvaluation olderEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-old", oldTime);
    tempEntity.newPolicyViolation(olderEvaluation, policy);

    PolicyEvaluation newerEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-new", now);
    PolicyViolation newerViolation = tempEntity.newPolicyViolation(newerEvaluation, policy);

    // Use the persisted open_time as the exact watermark to avoid any precision drift between `now` and storage.
    Date boundary = newerViolation.getOpenTime();

    // Inclusive (>=): a violation whose time exactly equals updatedSince is returned; the strictly-older one is not.
    assertThat(dao.countByApplicationIdAndStage(application.getId(), ReleaseStageType.ID, boundary)).isEqualTo(1L);
    assertThat(dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, boundary, 0, 10))
        .extracting(PolicyViolation::getId)
        .containsExactly(newerViolation.getId());

    // A cutoff strictly after the newest update time excludes everything.
    Date afterNewest = new Date(boundary.getTime() + 1000L);
    assertThat(dao.countByApplicationIdAndStage(application.getId(), ReleaseStageType.ID, afterNewest)).isEqualTo(0L);
  }

  @Test
  public void legacyViolationIsReturnedAndDrivesDelta() {
    Policy policy = tempEntity.newPolicy(application);

    Date now = new Date();
    Date openedAt = DateUtils.addDays(now, -5);
    Date beforeLegacyMark = DateUtils.addDays(now, -3);
    Date legacyMarkedAt = DateUtils.addDays(now, -2);
    Date afterLegacyMark = DateUtils.addDays(now, -1);

    // A violation opened long ago that was later retroactively marked legacy. It is STILL an SLO violation and must
    // be returned; the legacy mark participates only in change-detection (delta), not SLO suppression.
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-legacy", openedAt);
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(evaluation, policy);
    legacyViolation.setLegacyViolationTime(legacyMarkedAt);
    dao.update(legacyViolation);

    // (a) Returned with no delta filter.
    assertThat(dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, null, 0, 10))
        .extracting(PolicyViolation::getId)
        .contains(legacyViolation.getId());

    // (b) Included when the cutoff precedes the legacy-mark time (the label flipped after the cutoff), even though
    // open_time is older than the cutoff...
    assertThat(dao.countByApplicationIdAndStage(application.getId(), ReleaseStageType.ID, beforeLegacyMark))
        .isEqualTo(1L);
    // ...and excluded when the cutoff is after every one of its time columns.
    assertThat(dao.countByApplicationIdAndStage(application.getId(), ReleaseStageType.ID, afterLegacyMark))
        .isEqualTo(0L);
  }

  @Test
  public void paginationStaysDisjointWithLegacyRowPresent() {
    Set<String> targetIds = seedThreeReleaseViolationsInDifferentStates();

    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-legacy-page");
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(evaluation, policy);
    legacyViolation.setLegacyViolationTime(new Date());
    dao.update(legacyViolation);

    Set<String> allIds = Sets.union(targetIds, Set.of(legacyViolation.getId()));

    List<PolicyViolation> page1 =
        dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, null, 0, 2);
    List<PolicyViolation> page2 =
        dao.getByApplicationIdAndStagePaged(application.getId(), ReleaseStageType.ID, null, 2, 2);

    Set<String> page1Ids = page1.stream().map(PolicyViolation::getId).collect(toSet());
    Set<String> page2Ids = page2.stream().map(PolicyViolation::getId).collect(toSet());

    // Pages must remain disjoint and together cover the full result set (including the legacy row) exactly once.
    assertThat(page1Ids).doesNotContainAnyElementsOf(page2Ids);
    assertThat(page1Ids).hasSize(2);
    assertThat(page2Ids).hasSize(2);
    assertThat(Sets.union(page1Ids, page2Ids)).isEqualTo(allIds);
  }

  private Set<String> seedThreeReleaseViolationsInDifferentStates() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan");

    // Open (unresolved).
    PolicyViolation openViolation = tempEntity.newPolicyViolation(evaluation, policy);

    // Fixed.
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(evaluation, policy);
    fixedViolation.setFixTime(new Date());
    dao.update(fixedViolation);

    // Waived.
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(evaluation, policy,
        tempEntity.newWaiver(policy.getId(), application.getId()));

    return Set.of(openViolation.getId(), fixedViolation.getId(), waivedViolation.getId());
  }

  private void seedOtherApplicationReleaseViolation() {
    Application otherApplication = tempEntity.newApplication(organization.getId());
    Policy otherPolicy = tempEntity.newPolicy(otherApplication);
    PolicyEvaluation otherEvaluation =
        tempEntity.newPolicyEvaluation(otherApplication.getId(), ReleaseStageType.ID, "slo-scan-other-app");
    tempEntity.newPolicyViolation(otherEvaluation, otherPolicy);
  }

  private void seedTargetApplicationBuildStageViolation() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation buildEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "slo-scan-build-stage");
    tempEntity.newPolicyViolation(buildEvaluation, policy);
  }
}
