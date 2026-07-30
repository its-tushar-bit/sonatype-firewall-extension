/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
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
 * Tests for the SLO violation feed queries on {@link PolicyViolationDAO}: an all-states cursor-paged query and
 * matching count for a single application at a single stage, with an optional {@code updatedSince} delta filter.
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

    assertThat(dao.countByOwnerIdAndStage(application.getId(), ReleaseStageType.ID, null)).isEqualTo(3L);

    List<PolicyViolation> page =
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, null, null, 100);
    assertThat(page).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(targetIds);
  }

  @Test
  public void paginationSplitsResults() {
    seedThreeReleaseViolationsInDifferentStates();

    List<PolicyViolation> allViolations =
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, null, null, 100);
    Set<String> allIds = allViolations.stream().map(PolicyViolation::getId).collect(toSet());
    assertThat(allIds).hasSize(3);

    List<PolicyViolation> page1 =
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, null, null, 2);
    PolicyViolation cursor = page1.get(1);
    List<PolicyViolation> page2 = dao.getByOwnerIdAndStageAfterCursor(
        application.getId(), ReleaseStageType.ID,
        SloFeedSortKey.of(cursor), cursor.getId(), 2);

    assertThat(page1).hasSize(2);
    assertThat(page2).hasSize(1);

    Set<String> page1Ids = page1.stream().map(PolicyViolation::getId).collect(toSet());
    Set<String> page2Ids = page2.stream().map(PolicyViolation::getId).collect(toSet());

    assertThat(page1Ids).doesNotContainAnyElementsOf(page2Ids);
    assertThat(Sets.union(page1Ids, page2Ids)).isEqualTo(allIds);
  }

  @Test
  public void cursorPaginationDoesNotSkipWhenEarlierRowUpdateTimeMovesForward() {
    Policy policy = tempEntity.newPolicy(application);

    Date base = new Date();
    List<PolicyViolation> seeded = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
          application.getId(), ReleaseStageType.ID, "slo-cursor-shift-" + i, DateUtils.addMinutes(base, i));
      seeded.add(tempEntity.newPolicyViolation(eval, policy));
    }

    List<PolicyViolation> firstPage =
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, null, null, 2);
    assertThat(firstPage).hasSize(2);

    // Freeze the continuation cursor at the last row the caller saw before any concurrent mutation.
    PolicyViolation cursorRow = firstPage.get(1);
    Date cursorUpdateTime = SloFeedSortKey.of(cursorRow);

    // Simulate a concurrent update that moves an already-seen (earlier) row to the end of the sort order.
    PolicyViolation moved = firstPage.get(0);
    moved.setFixTime(DateUtils.addHours(base, 24));
    dao.update(moved);

    List<PolicyViolation> secondPage = dao.getByOwnerIdAndStageAfterCursor(
        application.getId(), ReleaseStageType.ID, cursorUpdateTime, cursorRow.getId(), 10);

    Set<String> collected = Sets.union(
        firstPage.stream().map(PolicyViolation::getId).collect(toSet()),
        secondPage.stream().map(PolicyViolation::getId).collect(toSet()));

    assertThat(collected).containsExactlyInAnyOrderElementsOf(
        seeded.stream().map(PolicyViolation::getId).collect(toSet()));
  }

  /**
   * P0 regression: the <em>cursor</em> row itself (not merely an earlier row) moves forward between page 1 and page 2.
   * The frozen composite cursor must still return the rows that were between the cursor row and the next page rather
   * than silently skipping them. The cursor row may be re-delivered (a duplicate), which callers dedupe by id.
   */
  @Test
  public void cursorPaginationDoesNotSkipWhenTheCursorRowItselfMovesForward() {
    Policy policy = tempEntity.newPolicy(application);

    Date base = new Date();
    List<PolicyViolation> seeded = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
          application.getId(), ReleaseStageType.ID, "slo-cursor-self-shift-" + i, DateUtils.addMinutes(base, i));
      seeded.add(tempEntity.newPolicyViolation(eval, policy));
    }

    List<PolicyViolation> firstPage =
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, null, null, 2);
    assertThat(firstPage).hasSize(2);

    // Freeze the cursor at the last row of page 1 (this is the row we will then move forward).
    PolicyViolation cursorRow = firstPage.get(1);
    Date cursorUpdateTime = SloFeedSortKey.of(cursorRow);

    // The cursor row is waived/fixed after delivery, jumping it to the end of the sort order.
    cursorRow.setFixTime(DateUtils.addHours(base, 48));
    dao.update(cursorRow);

    List<PolicyViolation> secondPage = dao.getByOwnerIdAndStageAfterCursor(
        application.getId(), ReleaseStageType.ID, cursorUpdateTime, cursorRow.getId(), 10);

    Set<String> secondPageIds = secondPage.stream().map(PolicyViolation::getId).collect(toSet());
    Set<String> unseen = seeded.stream()
        .skip(2)
        .map(PolicyViolation::getId)
        .collect(toSet());

    // The previously-unseen rows must not be skipped even though the cursor row moved past them.
    assertThat(secondPageIds).containsAll(unseen);

    // No unseen row is lost across the whole walk (dedupe by id).
    Set<String> collected = Sets.union(
        firstPage.stream().map(PolicyViolation::getId).collect(toSet()), secondPageIds);
    assertThat(collected).containsExactlyInAnyOrderElementsOf(
        seeded.stream().map(PolicyViolation::getId).collect(toSet()));
  }

  /**
   * The composite {@code (updatedSince, afterViolationId)} cursor exists so that when a page boundary falls inside a
   * group of rows sharing one sort key, the {@code policy_violation_id} tiebreaker keeps the walk moving without
   * skipping or duplicating. Every other paging test seeds distinct update times, so only this test exercises the
   * {@code updateTime = updatedSince AND id > afterViolationId} branch of the keyset predicate.
   */
  @Test
  public void cursorPaginationTiebreaksByIdWhenSortKeysAreIdentical() {
    Policy policy = tempEntity.newPolicy(application);

    // One evaluation → all violations share its open_time → identical GREATEST sort key, forcing the id tiebreaker.
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-tie");
    Set<String> seededIds = new LinkedHashSet<>();
    for (int i = 0; i < 5; i++) {
      seededIds.add(tempEntity.newPolicyViolation(evaluation, policy).getId());
    }

    List<PolicyViolation> all =
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, null, null, 100);
    assertThat(all).hasSize(5);
    assertThat(all.stream().map(v -> SloFeedSortKey.of(v).getTime()).collect(toSet()))
        .as("all rows share one sort key so the walk exercises the id tiebreaker")
        .hasSize(1);

    // Walk in pages of 2 so page boundaries land inside the tie group; terminate on the trailing empty page.
    List<String> collected = new ArrayList<>();
    Date updatedSince = null;
    String afterViolationId = null;
    for (int page = 0; page < 20; page++) {
      List<PolicyViolation> slice = dao.getByOwnerIdAndStageAfterCursor(
          application.getId(), ReleaseStageType.ID, updatedSince, afterViolationId, 2);
      if (slice.isEmpty()) {
        break;
      }
      slice.forEach(v -> collected.add(v.getId()));
      PolicyViolation last = slice.get(slice.size() - 1);
      updatedSince = SloFeedSortKey.of(last);
      afterViolationId = last.getId();
    }

    assertThat(collected).as("no duplicates across the tie-group walk").doesNotHaveDuplicates();
    assertThat(new LinkedHashSet<>(collected)).isEqualTo(seededIds);
  }

  /**
   * A cursor id that belongs to a different application is only an opaque tiebreaker string here; the query stays
   * scoped to {@code (applicationId, stageTypeId)}, so it can only ever page the target application's rows — never
   * another application's. This is the isolation guarantee that lets the service skip re-validating the cursor row.
   */
  @Test
  public void foreignCursorIdStaysScopedToTheApplication() {
    Set<String> targetIds = seedThreeReleaseViolationsInDifferentStates();
    seedOtherApplicationReleaseViolation();

    List<PolicyViolation> page = dao.getByOwnerIdAndStageAfterCursor(
        application.getId(), ReleaseStageType.ID, new Date(0L), "some-foreign-application-violation-id", 100);

    assertThat(page).extracting(PolicyViolation::getId).containsExactlyInAnyOrderElementsOf(targetIds);
  }

  @Test
  public void updatedSinceFiltersByTimeColumns() {
    Policy policy = tempEntity.newPolicy(application);

    Date now = new Date();
    Date oldTime = DateUtils.addDays(now, -2);
    Date cutoff = DateUtils.addDays(now, -1);

    PolicyEvaluation olderEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-old", oldTime);
    tempEntity.newPolicyViolation(olderEvaluation, policy);

    PolicyEvaluation newerEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-new", now);
    tempEntity.newPolicyViolation(newerEvaluation, policy);

    assertThat(dao.countByOwnerIdAndStage(application.getId(), ReleaseStageType.ID, null)).isEqualTo(2L);
    assertThat(dao.countByOwnerIdAndStage(application.getId(), ReleaseStageType.ID, cutoff)).isEqualTo(1L);
    assertThat(
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, cutoff, null, 10))
            .hasSize(1);
  }

  @Test
  public void updatedSinceIsDrivenByFixTime() {
    Policy policy = tempEntity.newPolicy(application);

    Date now = new Date();
    Date oldTime = DateUtils.addDays(now, -2);
    Date cutoff = DateUtils.addDays(now, -1);

    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-fixed", oldTime);
    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(evaluation, policy);
    fixedViolation.setFixTime(now);
    dao.update(fixedViolation);

    assertThat(fixedViolation.getOpenTime()).isBefore(cutoff);

    assertThat(dao.countByOwnerIdAndStage(application.getId(), ReleaseStageType.ID, cutoff)).isEqualTo(1L);
    assertThat(
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, cutoff, null, 10))
            .extracting(PolicyViolation::getId)
            .containsExactly(fixedViolation.getId());
  }

  @Test
  public void updatedSinceIsDrivenByWaiveTime() {
    Policy policy = tempEntity.newPolicy(application);

    Date now = new Date();
    Date oldTime = DateUtils.addDays(now, -2);
    Date cutoff = DateUtils.addDays(now, -1);

    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-waived", oldTime);
    PolicyViolation waivedViolation = tempEntity.newPolicyViolation(evaluation, policy);
    waivedViolation.setWaiveTime(now);
    dao.update(waivedViolation);

    assertThat(waivedViolation.getOpenTime()).isBefore(cutoff);

    assertThat(dao.countByOwnerIdAndStage(application.getId(), ReleaseStageType.ID, cutoff)).isEqualTo(1L);
    assertThat(
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, cutoff, null, 10))
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

    Date boundary = newerViolation.getOpenTime();

    assertThat(dao.countByOwnerIdAndStage(application.getId(), ReleaseStageType.ID, boundary)).isEqualTo(1L);
    assertThat(dao.getByOwnerIdAndStageAfterCursor(
        application.getId(), ReleaseStageType.ID, boundary, null, 10))
            .extracting(PolicyViolation::getId)
            .containsExactly(newerViolation.getId());

    Date afterNewest = new Date(boundary.getTime() + 1000L);
    assertThat(dao.countByOwnerIdAndStage(application.getId(), ReleaseStageType.ID, afterNewest)).isEqualTo(0L);
  }

  @Test
  public void legacyViolationIsReturnedAndDrivesDelta() {
    Policy policy = tempEntity.newPolicy(application);

    Date now = new Date();
    Date openedAt = DateUtils.addDays(now, -5);
    Date beforeLegacyMark = DateUtils.addDays(now, -3);
    Date legacyMarkedAt = DateUtils.addDays(now, -2);
    Date afterLegacyMark = DateUtils.addDays(now, -1);

    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan-legacy", openedAt);
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(evaluation, policy);
    legacyViolation.setLegacyViolationTime(legacyMarkedAt);
    dao.update(legacyViolation);

    assertThat(
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, null, null, 10))
            .extracting(PolicyViolation::getId)
            .contains(legacyViolation.getId());

    assertThat(dao.countByOwnerIdAndStage(application.getId(), ReleaseStageType.ID, beforeLegacyMark))
        .isEqualTo(1L);
    assertThat(dao.countByOwnerIdAndStage(application.getId(), ReleaseStageType.ID, afterLegacyMark))
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
        dao.getByOwnerIdAndStageAfterCursor(application.getId(), ReleaseStageType.ID, null, null, 2);
    PolicyViolation cursor = page1.get(1);
    List<PolicyViolation> page2 = dao.getByOwnerIdAndStageAfterCursor(
        application.getId(), ReleaseStageType.ID,
        SloFeedSortKey.of(cursor), cursor.getId(), 2);

    Set<String> page1Ids = page1.stream().map(PolicyViolation::getId).collect(toSet());
    Set<String> page2Ids = page2.stream().map(PolicyViolation::getId).collect(toSet());

    assertThat(page1Ids).doesNotContainAnyElementsOf(page2Ids);
    assertThat(Sets.union(page1Ids, page2Ids)).isEqualTo(allIds);
  }

  private Set<String> seedThreeReleaseViolationsInDifferentStates() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "slo-scan");

    PolicyViolation openViolation = tempEntity.newPolicyViolation(evaluation, policy);

    PolicyViolation fixedViolation = tempEntity.newPolicyViolation(evaluation, policy);
    fixedViolation.setFixTime(new Date());
    dao.update(fixedViolation);

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
