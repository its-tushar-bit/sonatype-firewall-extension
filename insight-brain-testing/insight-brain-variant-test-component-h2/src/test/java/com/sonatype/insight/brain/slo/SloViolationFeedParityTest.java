/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration coverage for the SLO violation feed through the full {@link SloViolationFeedService} stack (authz,
 * feature flag, DAO paging + delta filter, enrichment):
 * <ol>
 * <li>field parity between the feed and its underlying prioritization source,</li>
 * <li>pagination across pages returning every violation exactly once, and</li>
 * <li>{@code updatedSince} delta filtering by open/fix/waive time.</li>
 * </ol>
 */
@ComponentH2Test
public class SloViolationFeedParityTest
    extends AbstractComponentH2AuthzTest
{
  private static final String SCAN_ID = "slo-parity-scan";

  // Component present in the seeded report-1 fixture (see ApiReportDataServiceTest/report-1/bom.json).
  private static final ComponentIdentifier GSON =
      ComponentIdentifier.createMavenCoordinates("com.google.code.gson", "gson", "2.8.1", "", "jar");

  private static final String RECOMMENDED_VERSION = "2.8.9";

  private static final int THREAT_LEVEL = 8;

  @Inject
  private SloViolationFeedService service;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO;

  @BeforeEach
  public void enableFeature() {
    SystemConfigurationPropertyFeature.SLO_VIOLATION_FEED.setEnabled(true);
  }

  @AfterEach
  public void disableFeature() {
    SystemConfigurationPropertyFeature.SLO_VIOLATION_FEED.setEnabled(false);
  }

  @Test
  public void reachabilityThreatAndRecommendationMatchPrioritiesSource() throws Exception {
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, SCAN_ID);
    seedReport();

    // Seed the same prioritization source the Priorities page reads: report-derived dependency type (report-1 marks
    // gson as a transitive dependency) plus a bulk development_prioritization_component_info recommendation row.
    String prioritizationId = tempEntity.newDevelopmentPrioritization(SCAN_ID).getId();
    tempEntity.newDevelopmentPrioritizationComponentInfo(prioritizationId, SCAN_ID, GSON.toSyntheticHash(),
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, RECOMMENDED_VERSION);

    PolicyViolation seeded = insertViolation(evaluation, policy, GSON, v -> {
      v.setThreatLevel(THREAT_LEVEL);
      v.setReachabilityStatus(ReachabilityStatus.REACHABLE);
    });

    grantReadPermission(app.getId());
    SloViolationFeedResults results =
        service.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, null, 50);

    SloViolation result = results.violations()
        .results()
        .stream()
        .filter(v -> v.violationId.equals(seeded.getId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Seeded gson violation missing from the feed page"));

    assertThat(result.reachabilityStatus).isEqualTo(ReachabilityStatus.REACHABLE.getName());
    assertThat(result.threatLevel).isEqualTo(THREAT_LEVEL);
    assertThat(result.recommendedRemediationVersion).isEqualTo(RECOMMENDED_VERSION);
    assertThat(result.dependencyType).isEqualTo("Transitive");

    // Direct source parity: the feed's recommended version must equal what the shared prioritization source
    // (the bulk development_prioritization_component_info table read by the Priorities feature and the enricher)
    // reports for this scan + component. The report-derived dependency type is proven same-source separately by
    // Task 7's SloViolationEnricherRemediationTest, which asserts against the real DevelopmentPrioritiesReportService.
    Map<String, DevelopmentPrioritizationComponentInfo> source =
        prioritizationComponentInfoDAO.getByScanIdAndComponentHashes(SCAN_ID, Set.of(GSON.toSyntheticHash()));
    DevelopmentPrioritizationComponentInfo sourceInfo = source.get(GSON.toSyntheticHash());
    assertThat(sourceInfo).isNotNull();
    assertThat(result.recommendedRemediationVersion).isEqualTo(sourceInfo.getRemediationVersion());
  }

  @Test
  public void paginationReturnsAllViolationsAcrossPages() {
    Policy policy = tempEntity.newPolicy(app);

    // Distinct open times (per-evaluation) so the walk exercises a real multi-page (updateTime, id) ordering.
    Date base = new Date();
    Set<String> seededIds = new LinkedHashSet<>();
    for (int i = 0; i < 7; i++) {
      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
          app.getId(), Stage.ID_RELEASE, SCAN_ID + "-" + i, DateUtils.addMinutes(base, i));
      seededIds.add(tempEntity.newPolicyViolation(evaluation, policy).getId());
    }

    grantReadPermission(app.getId());

    List<String> collected = new ArrayList<>();
    long firstPageTotal = -1;
    SloViolationFeedCursor cursor = null;
    boolean done = false;
    // Walk to completion: the loop bound is generous; termination is driven by a null next-page cursor.
    for (int page = 0; page < 10; page++) {
      Date updatedSince = cursor == null ? null : new Date(cursor.updatedSince());
      String afterViolationId = cursor == null ? null : cursor.afterViolationId();
      SloViolationFeedResults results =
          service.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, updatedSince, afterViolationId, 3);
      if (page == 0) {
        // total reflects the request: on the unfiltered first page it is the grand total.
        firstPageTotal = results.violations().total();
      }
      collected.addAll(results.violations().results().stream().map(v -> v.violationId).collect(toList()));
      cursor = results.violations().nextPageCursor();
      if (cursor == null) {
        done = true;
        break;
      }
    }

    // Static data: the walk terminates on a null cursor having returned every id exactly once.
    assertThat(done).as("walk terminated on a null next-page cursor").isTrue();
    assertThat(collected).hasSize(7);
    assertThat(new LinkedHashSet<>(collected)).as("no duplicates across pages").hasSize(7);
    assertThat(new LinkedHashSet<>(collected)).isEqualTo(seededIds);
    assertThat(firstPageTotal).isEqualTo(7L);
  }

  @Test
  public void cursorRowMovingForwardBetweenPagesDoesNotSkipUnseenRows() {
    Policy policy = tempEntity.newPolicy(app);

    // Five violations with strictly increasing update times so the sort order is deterministic.
    Date base = new Date();
    List<String> seededInOrder = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
          app.getId(), Stage.ID_RELEASE, SCAN_ID + "-move-" + i, DateUtils.addMinutes(base, i));
      seededInOrder.add(tempEntity.newPolicyViolation(evaluation, policy).getId());
    }

    grantReadPermission(app.getId());

    SloViolationFeedResults page1 =
        service.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, null, 2);
    assertThat(page1.violations().results()).hasSize(2);
    SloViolationFeedCursor cursor = page1.violations().nextPageCursor();
    assertThat(cursor).isNotNull();

    // The cursor row (last row of page 1) is fixed after delivery, jumping it to the end of the sort order.
    PolicyViolation cursorRow = policyViolationDAO.getById(cursor.afterViolationId());
    cursorRow.setFixTime(DateUtils.addHours(base, 24));
    policyViolationDAO.update(cursorRow);

    SloViolationFeedResults page2 = service.getSloViolations(
        app.getPublicId(), Stage.ID_RELEASE, new Date(cursor.updatedSince()), cursor.afterViolationId(), 10);

    Set<String> page2Ids = page2.violations().results().stream().map(v -> v.violationId).collect(toSet());
    Set<String> unseen = new LinkedHashSet<>(seededInOrder.subList(2, 5));

    // The previously-unseen rows must still be delivered even though the cursor row moved past them.
    assertThat(page2Ids).as("unseen rows not skipped when cursor row moves forward").containsAll(unseen);

    // Deduping by id across the whole walk recovers every seeded violation.
    Set<String> collected = page1.violations().results().stream().map(v -> v.violationId).collect(toSet());
    collected.addAll(page2Ids);
    assertThat(collected).containsExactlyInAnyOrderElementsOf(new LinkedHashSet<>(seededInOrder));
  }

  @Test
  public void exactMultipleOfPageSizeEndsWithTrailingEmptyPage() {
    Policy policy = tempEntity.newPolicy(app);

    // Six violations with pageSize 3: the match count is an exact multiple of the page size, so the last full page
    // still emits a non-null cursor and the walk terminates on a trailing empty page.
    Date base = new Date();
    Set<String> seededIds = new LinkedHashSet<>();
    for (int i = 0; i < 6; i++) {
      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
          app.getId(), Stage.ID_RELEASE, SCAN_ID + "-mult-" + i, DateUtils.addMinutes(base, i));
      seededIds.add(tempEntity.newPolicyViolation(evaluation, policy).getId());
    }

    grantReadPermission(app.getId());

    List<String> collected = new ArrayList<>();
    int pages = 0;
    int lastPageSize = -1;
    SloViolationFeedCursor cursor = null;
    boolean done = false;
    for (int page = 0; page < 10; page++) {
      Date updatedSince = cursor == null ? null : new Date(cursor.updatedSince());
      String afterViolationId = cursor == null ? null : cursor.afterViolationId();
      SloViolationFeedResults results =
          service.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, updatedSince, afterViolationId, 3);
      pages++;
      lastPageSize = results.violations().results().size();
      collected.addAll(results.violations().results().stream().map(v -> v.violationId).collect(toList()));
      cursor = results.violations().nextPageCursor();
      if (cursor == null) {
        done = true;
        break;
      }
    }

    // Two full pages of 3, then a trailing empty page carrying the null cursor that ends the walk.
    assertThat(pages).isEqualTo(3);
    assertThat(lastPageSize).as("trailing page is empty").isZero();
    assertThat(done).as("walk terminated on a null cursor").isTrue();
    assertThat(new LinkedHashSet<>(collected)).isEqualTo(seededIds);
  }

  @Test
  public void blankCursorReturnsFirstPageRatherThanSilentlyEmptyFeed() {
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, SCAN_ID);
    Set<String> seededIds = new LinkedHashSet<>();
    for (int i = 0; i < 3; i++) {
      seededIds.add(tempEntity.newPolicyViolation(evaluation, policy).getId());
    }

    grantReadPermission(app.getId());

    // A blank afterViolationId (e.g. "afterViolationId=" in the query string) must be treated as "first page",
    // not passed through as an empty-string cursor that silently returns zero rows.
    SloViolationFeedResults results =
        service.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, "  ", 50);

    Set<String> returnedIds =
        results.violations().results().stream().map(v -> v.violationId).collect(toSet());
    assertThat(returnedIds).isEqualTo(seededIds);
  }

  @Test
  public void updatedSinceReturnsOnlyRecentlyChangedViolations() {
    Policy policy = tempEntity.newPolicy(app);

    Date now = new Date();
    Date oldTime = DateUtils.addDays(now, -2);
    Date cutoff = DateUtils.addDays(now, -1);

    // open_time is derived from the evaluation time, so drive open_time via the evaluation used to seed a violation.
    PolicyEvaluation olderEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "slo-parity-old", oldTime);
    PolicyEvaluation newerEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "slo-parity-new", now);

    // Excluded: opened before the cutoff and never changed.
    PolicyViolation oldOpen = tempEntity.newPolicyViolation(olderEvaluation, policy);

    // Included: opened after the cutoff.
    PolicyViolation newOpen = tempEntity.newPolicyViolation(newerEvaluation, policy);

    // Included: opened before the cutoff but fixed after it.
    PolicyViolation fixedAfter = tempEntity.newPolicyViolation(olderEvaluation, policy);
    fixedAfter.setFixTime(now);
    policyViolationDAO.update(fixedAfter);

    // Included: opened before the cutoff but waived after it.
    PolicyViolation waivedAfter = tempEntity.newPolicyViolation(olderEvaluation, policy);
    waivedAfter.setWaiveTime(now);
    policyViolationDAO.update(waivedAfter);

    grantReadPermission(app.getId());
    SloViolationFeedResults results =
        service.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, cutoff, null, 50);

    Set<String> returnedIds =
        results.violations().results().stream().map(v -> v.violationId).collect(toSet());

    assertThat(returnedIds).containsExactlyInAnyOrder(newOpen.getId(), fixedAfter.getId(), waivedAfter.getId());
    assertThat(returnedIds).doesNotContain(oldOpen.getId());
    assertThat(results.violations().total()).isEqualTo(3L);
  }

  private void seedReport() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ApiReportDataServiceTest/report-1", app.getId(), SCAN_ID);
  }

  private PolicyViolation insertViolation(
      final PolicyEvaluation evaluation,
      final Policy policy,
      final ComponentIdentifier componentIdentifier,
      final java.util.function.Consumer<PolicyViolation> customizer)
  {
    // policy_violation.hash is varchar(20); bound the low digits of nanoTime so the unique value always fits.
    PolicyViolation violation = new PolicyViolation(evaluation, policy,
        "hash-" + (System.nanoTime() % 100_000_000_000_000L),
        componentIdentifier, Collections.singletonList(new ConstraintFact()), "component.jar");
    violation.setId("pv-" + System.nanoTime());
    customizer.accept(violation);
    policyViolationDAO.insert(violation);
    return policyViolationDAO.getById(violation.getId());
  }
}
