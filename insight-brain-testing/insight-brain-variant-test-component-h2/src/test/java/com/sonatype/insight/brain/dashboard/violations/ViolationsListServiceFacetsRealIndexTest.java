/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * REAL-INDEX (real Lucene + real H2 DB, no mocked search primitives) service-layer tests for the
 * Nexus One Violations list owner facets (CLM-44713, tracked by CLM-45220).
 * <p>
 * Mirrors {@code ApplicationsListServiceFacetsRealIndexTest} / {@code
 * VulnerabilitiesListServiceFacetsRealIndexTest}: a real org hierarchy is indexed through the
 * production report-ingestion + indexing pipeline (real {@code POLICY_VIOLATION} documents, built from
 * real {@link PolicyEvaluation}/{@code PolicyViolation} rows, not stubbed session buckets) and the real
 * {@link ViolationsListService#listViolations} is exercised. This replaces the mocked {@code
 * ViolationsListFacetsBuilderTest} coverage, which stubbed {@code session.termsAggregation(...)} and so
 * could not catch a real docValues wiring defect in the {@code parentOrganizationId} facet field.
 * <p>
 * Unlike Applications (plain per-app doc count), the Violations owner facets are per-violation document
 * counts (see
 * {@link ViolationsListFacetsBuilder#countOrganizations(com.sonatype.insight.brain.search.session.IndexReadSession,
 * org.apache.lucene.search.Query)}), so the real estate here deliberately gives {@code appChild} TWO
 * distinct policy violations (vs. one each for the sibling/extra apps) to prove the facet counts
 * violations, not applications.
 */
@ComponentH2Test
public class ViolationsListServiceFacetsRealIndexTest
    extends AbstractComponentH2Test
{
  @Inject
  private ViolationsListService violationsListService;

  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private Organization orgGrandparent;

  private Organization orgParent;

  private Organization orgChild;

  private Organization orgSibling;

  private Organization orgExtra;

  private Application appChild;

  private Application appSibling;

  private Application appExtra;

  @BeforeEach
  public void setUpSessionReadPathAndIndex() throws Exception {
    // Real-index tests exercise the CLM-44713 session read path (IndexReadSession.termsAggregation),
    // not the legacy per-org count() path. See ViolationsListServiceSessionTest for the mocked
    // equivalent this test class is meant to catch what that one cannot.
    System.setProperty("nexusOne.search.readPath.violations", "new");

    // Mirror LuceneSearchIndexClientAggregateTest / ApplicationsListServiceFacetsRealIndexTest: swap in
    // a mocked shutdown handler and reset the lazily-created indexing executors so repeated full
    // re-indexes within this reused-context module behave deterministically across test classes.
    applyBeanFieldOverride(AbstractSearchIndexClient.class, "shutdownHandler", mockShutdownHandler);
    applyBeanFieldOverride(DocumentBuilderHelper.class, "shutdownHandler", mockShutdownHandler);
    resetTenantExecutor(lookup(AbstractSearchIndexClient.class), "indexingExecutors");
    resetTenantExecutor(lookup(DocumentBuilderHelper.class), "evalExecutors");
    resetTenantExecutor(lookup(DocumentBuilderHelper.class), "componentExecutors");

    buildOwnerHierarchyRealEstate();
    luceneSearchIndexClient.populateIndex();
  }

  @AfterEach
  public void clearSessionReadPathFlag() {
    System.clearProperty("nexusOne.search.readPath.violations");
  }

  private static void resetTenantExecutor(final Object bean, final String fieldName) {
    @SuppressWarnings("unchecked")
    TenantReference<ExecutorService> executors =
        (TenantReference<ExecutorService>) ReflectionTestUtils.getField(bean, fieldName);
    if (executors == null) {
      return;
    }
    ExecutorService oldExecutor = executors.remove();
    if (oldExecutor != null) {
      oldExecutor.shutdownNow();
    }
  }

  /**
   * Real estate: grandparent org A -> parent org B -> child org C (appChild lives under C), a sibling
   * org D under A (appSibling), and an unrelated org E under A (appExtra) — same owner shape as
   * {@code ApplicationsListServiceFacetsRealIndexTest}, so the hierarchical ancestor-closure behavior is
   * proven the same way.
   * <p>
   * appChild carries TWO policy violations (on two different components under two different policies);
   * appSibling and appExtra each carry one. That gives 4 total {@code POLICY_VIOLATION} documents with a
   * distinguishable per-app split (2 / 1 / 1), so the facet counts can be pinned precisely and distinguished
   * from a plain application count (which would show 1 / 1 / 1 for the app facet's "presence", not volume).
   */
  private void buildOwnerHierarchyRealEstate() throws Exception {
    orgGrandparent = tempEntity.newOrganization();
    orgParent = tempEntity.newOrganization(orgGrandparent);
    orgChild = tempEntity.newOrganization(orgParent);
    orgSibling = tempEntity.newOrganization(orgGrandparent);
    orgExtra = tempEntity.newOrganization(orgGrandparent);

    appChild = tempEntity.newApplication(orgChild.getId());
    appSibling = tempEntity.newApplication(orgSibling.getId());
    appExtra = tempEntity.newApplication(orgExtra.getId());

    PolicyEvaluation childEvaluation = newAppReport(appChild.getId(), Stage.ID_BUILD, "childScan");
    Policy childPolicyA = tempEntity.newPolicy(orgChild.getId(), "Security - Child A");
    Policy childPolicyB = tempEntity.newPolicy(orgChild.getId(), "Security - Child B");
    tempEntity.newPolicyViolation(childEvaluation, childPolicyA, 8, PolicyThreatCategory.SECURITY,
        "org.apache.logging", "log4j-core", "2.14.0", "hashPvtLog4j00000000");
    tempEntity.newPolicyViolation(childEvaluation, childPolicyB, 6, PolicyThreatCategory.SECURITY,
        "org.linux", "linux-kernel", "5.10", "hashPvtLinux00000000");

    PolicyEvaluation siblingEvaluation = newAppReport(appSibling.getId(), Stage.ID_BUILD, "siblingScan");
    Policy siblingPolicy = tempEntity.newPolicy(orgSibling.getId(), "Security - Sibling");
    tempEntity.newPolicyViolation(siblingEvaluation, siblingPolicy, 7, PolicyThreatCategory.SECURITY,
        "net.busybox", "busybox", "1.33", "hashPvtBusybox000000");

    PolicyEvaluation extraEvaluation = newAppReport(appExtra.getId(), Stage.ID_BUILD, "extraScan");
    Policy extraPolicy = tempEntity.newPolicy(orgExtra.getId(), "Security - Extra");
    tempEntity.newPolicyViolation(extraEvaluation, extraPolicy, 5, PolicyThreatCategory.SECURITY,
        "org.openssl", "openssl", "3.0", "hashPvtOpenssl00000");
  }

  /**
   * Mirrors {@code LuceneSearchIndexClientAggregateTest#newAppReport}: attaches a real scan report to a
   * real {@link PolicyEvaluation} so the production report-ingestion + indexing pipeline produces the
   * {@code POLICY_VIOLATION} documents (denormalized {@code organizationId}/{@code parentOrganizationId}
   * ancestor closure included), not a hand-built {@code Document}.
   */
  private PolicyEvaluation newAppReport(
      final String appId,
      final String stageId,
      final String scanId) throws Exception
  {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(appId, stageId, scanId);
    ReportTestUtils.createReportFile(
        policyEvaluation.getOwnerId(),
        policyEvaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", tempDir),
        insightWork);
    return policyEvaluation;
  }

  @Test
  public void testListViolations_OrganizationFacet_HierarchicalSubtreeCountsWithRootExcluded() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();

    ViolationsListResponseDTO response = violationsListService.listViolations(request);

    // 2 (appChild) + 1 (appSibling) + 1 (appExtra) = 4 POLICY_VIOLATION documents estate-wide.
    assertThat(response.total).isEqualTo(4);
    assertThat(response.facets).isNotNull();
    assertThat(response.facets.organizations).isNotNull();
    assertThat(response.facets.applications).isNotNull();

    // Hierarchical subtree counts: each ancestor's bucket counts its whole subtree, computed from a
    // REAL parentOrganizationId docValues aggregation over the real index, not a mocked bucket list.
    assertThat(response.facets.organizations.get(orgGrandparent.getId()))
        .describedAs("grandparent org subtree: appChild(2) + appSibling(1) + appExtra(1)")
        .isEqualTo(4L);
    assertThat(response.facets.organizations.get(orgParent.getId()))
        .describedAs("parent org subtree: appChild(2) only")
        .isEqualTo(2L);
    assertThat(response.facets.organizations.get(orgChild.getId()))
        .describedAs("child org subtree: appChild(2) only")
        .isEqualTo(2L);
    assertThat(response.facets.organizations.get(orgSibling.getId()))
        .describedAs("sibling org subtree: appSibling(1) only")
        .isEqualTo(1L);
    assertThat(response.facets.organizations.get(orgExtra.getId()))
        .describedAs("extra org subtree: appExtra(1) only")
        .isEqualTo(1L);

    // ROOT is in every doc's ancestor closure and must never appear as its own facet bucket.
    assertThat(response.facets.organizations).doesNotContainKey(Organization.ROOT_ORGANIZATION_ID);

    // App facet: per-violation document counts (appChild has 2 violations, siblings/extra have 1 each).
    assertThat(response.facets.applications.get(appChild.getId())).isEqualTo(2L);
    assertThat(response.facets.applications.get(appSibling.getId())).isEqualTo(1L);
    assertThat(response.facets.applications.get(appExtra.getId())).isEqualTo(1L);
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());

    // Owner display names carry the real (proper-case) org/app names through to the facet rail.
    assertThat(response.facets.organizationNames)
        .containsEntry(orgGrandparent.getId(), orgGrandparent.getName())
        .containsEntry(orgParent.getId(), orgParent.getName())
        .containsEntry(orgChild.getId(), orgChild.getName())
        .containsEntry(orgSibling.getId(), orgSibling.getName())
        .containsEntry(orgExtra.getId(), orgExtra.getName());
    assertThat(response.facets.applicationNames)
        .containsEntry(appChild.getId(), appChild.getName())
        .containsEntry(appSibling.getId(), appSibling.getName())
        .containsEntry(appExtra.getId(), appExtra.getName());
  }

  @Test
  public void testListViolations_OwnerFacets_OffPageValuesAppearWithSmallPageSize() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.pageSize = 1;

    ViolationsListResponseDTO response = violationsListService.listViolations(request);

    // Only one violation row on the page...
    assertThat(response.violations).hasSize(1);
    assertThat(response.hasNextPage).isTrue();
    assertThat(response.total).isEqualTo(4);

    // ...but the owner facets aggregate over the full owner-removed base, so off-page orgs/apps still
    // surface in the sidebar rails (a single-pass real aggregation, not per-page counting).
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
  }

  @Test
  public void testListViolations_SelectingOneOrganization_DoesNotCollapseSiblingOrgOrAppFacets() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    // Mid-level org: narrows RESULTS to its subtree (appChild's 2 violations) via ancestor-match on
    // parentOrganizationId, proving hierarchical filtering as well as facet no-collapse.
    request.organizationIds = Set.of(orgParent.getId());

    ViolationsListResponseDTO response = violationsListService.listViolations(request);

    // The RESULTS narrow to the selected org's subtree...
    assertThat(response.total).isEqualTo(2);

    // ...but the owner FACETS still show the sibling/extra orgs and their apps (CLM-44713: org and app
    // are ONE owner dimension, OR-combined; both facets aggregate over an owner-removed base so
    // selecting one org does not collapse either rail).
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
    // Counts themselves are also unnarrowed by the selection (still the full estate-wide subtree counts).
    assertThat(response.facets.organizations.get(orgGrandparent.getId())).isEqualTo(4L);
    assertThat(response.facets.applications.get(appSibling.getId())).isEqualTo(1L);
  }

  @Test
  public void testListViolations_SelectingOneApplication_DoesNotCollapseOtherAppOrOrgFacets() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.applicationIds = Set.of(appSibling.getId());

    ViolationsListResponseDTO response = violationsListService.listViolations(request);

    assertThat(response.total).isEqualTo(1);

    // Same owner-group no-collapse behavior when the selection narrows by app instead of org.
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
    assertThat(response.facets.applications.get(appChild.getId())).isEqualTo(2L);
    assertThat(response.facets.organizations.get(orgChild.getId())).isEqualTo(2L);
  }

  // --- Fixed-vocabulary facet no-collapse (CLM-44713) ------------------------------------------

  /**
   * Adds ONE additional violation on {@code appExtra} that is simultaneously WAIVED, QUALITY, and
   * RELEASE-stage — distinct on all three dimensions from the 4 base violations seeded by
   * {@link #buildOwnerHierarchyRealEstate()} (all OPEN, SECURITY, BUILD). Filtering on any ONE of
   * those three dimensions narrows the RESULTS to just this row, so each dimension's facet can be
   * asserted to still show the OTHER (base) value at its full, unnarrowed count.
   */
  private void seedFixedFacetVarietyRealEstate() throws Exception {
    PolicyEvaluation extraReleaseEvaluation =
        newAppReport(appExtra.getId(), Stage.ID_RELEASE, "extraReleaseScan");
    Policy extraQualityPolicy = tempEntity.newPolicy(orgExtra.getId(), "Quality - Extra");
    PolicyWaiver waiver = tempEntity.newWaiver(extraQualityPolicy.getId(), orgExtra.getId());
    tempEntity.newWaivedPolicyViolation(
        extraReleaseEvaluation,
        extraQualityPolicy,
        4,
        PolicyThreatCategory.QUALITY,
        ComponentIdentifier.createMavenCoordinates("com.example", "waived-lib", "1.0"),
        "hashPvtWaivedQual001",
        waiver);
    luceneSearchIndexClient.populateIndex();
  }

  /**
   * The organization rail's name search narrows the rail to organizations matching the typed text, and
   * counts them against the owner-removed base — so a user who has already selected one organization and
   * then searches for another still sees that other organization's count rather than zero.
   */
  @Test
  public void testListViolations_OrganizationFacetSearch_NarrowsTheRailWithoutCollapsingIt() {
    ViolationsListRequestDTO searchOnly = new ViolationsListRequestDTO();
    searchOnly.organizationFacetSearch = orgChild.getName();

    ViolationsListResponseDTO response = violationsListService.listViolations(searchOnly);

    // The rail narrows to the searched organization, so the ancestors that would otherwise dominate the
    // hierarchical rail are absent.
    assertThat(response.facets.organizations).containsOnlyKeys(orgChild.getId());
    assertThat(response.facets.organizations).containsEntry(orgChild.getId(), 2L);
    assertThat(response.facets.organizationNames).containsEntry(orgChild.getId(), orgChild.getName());

    // With a DIFFERENT organization already selected, searching for orgChild must still report its own
    // count: the search counts against the owner-removed base, not the org-narrowed list query. Counting
    // against the narrowed query would report 0 and make the rail unusable for switching selection.
    ViolationsListRequestDTO searchWhileFiltered = new ViolationsListRequestDTO();
    searchWhileFiltered.organizationIds = Set.of(orgSibling.getId());
    searchWhileFiltered.organizationFacetSearch = orgChild.getName();

    ViolationsListResponseDTO filtered = violationsListService.listViolations(searchWhileFiltered);

    assertThat(filtered.total).isEqualTo(1);
    assertThat(filtered.facets.organizations).containsEntry(orgChild.getId(), 2L);
  }

  @Test
  public void testListViolations_SelectingStateFilter_DoesNotCollapseOtherStateFacets() throws Exception {
    seedFixedFacetVarietyRealEstate();

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.WAIVED);

    ViolationsListResponseDTO response = violationsListService.listViolations(request);

    // Results narrow to just the new WAIVED violation...
    assertThat(response.total).isEqualTo(1);

    // ...but the states facet still shows OPEN at its full, unnarrowed count (the 4 base violations),
    // not collapsed to zero under the WAIVED-narrowed query.
    assertThat(response.facets.states).containsEntry(PolicyViolationState.OPEN.name(), 4L);
    assertThat(response.facets.states).containsEntry(PolicyViolationState.WAIVED.name(), 1L);
  }

  @Test
  public void testListViolations_SelectingThreatCategoryFilter_DoesNotCollapseOtherThreatCategoryFacets() throws Exception {
    seedFixedFacetVarietyRealEstate();

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.policyThreatCategories = new PolicyThreatCategoryFilter(PolicyThreatCategory.QUALITY);

    ViolationsListResponseDTO response = violationsListService.listViolations(request);

    assertThat(response.total).isEqualTo(1);

    // threatCategories facet still shows SECURITY at its full, unnarrowed count.
    assertThat(response.facets.threatCategories)
        .containsEntry(PolicyThreatCategory.SECURITY.getName(), 4L)
        .containsEntry(PolicyThreatCategory.QUALITY.getName(), 1L);
  }

  @Test
  public void testListViolations_SelectingStageFilter_DoesNotCollapseOtherStageFacets() throws Exception {
    seedFixedFacetVarietyRealEstate();

    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.stageIds = Set.of(Stage.ID_RELEASE);

    ViolationsListResponseDTO response = violationsListService.listViolations(request);

    assertThat(response.total).isEqualTo(1);

    // stages facet still shows BUILD at its full, unnarrowed count.
    assertThat(response.facets.stages)
        .containsEntry(Stage.ID_BUILD, 4L)
        .containsEntry(Stage.ID_RELEASE, 1L);
  }

  /**
   * The owner facet base drops only the owner dimension; the waiver-type filter is kept, so the owner
   * rails narrow to the owners that actually have a violation of the selected waiver type instead of
   * counting the whole estate.
   */
  @Test
  public void testListViolations_SelectingWaiverTypeFilter_NarrowsOwnerFacetsToOwnersOfMatchingViolations() throws Exception {
    seedFixedFacetVarietyRealEstate();

    ViolationsListRequestDTO manuallyWaived = new ViolationsListRequestDTO();
    manuallyWaived.waivedWithAutoWaiver = Boolean.FALSE;

    ViolationsListResponseDTO response = violationsListService.listViolations(manuallyWaived);

    // appExtra's waived violation is the only manually-waived one, so it is the only owner in the rails;
    // appChild/appSibling (open violations only) and their organizations are absent.
    assertThat(response.total).isEqualTo(1);
    assertThat(response.facets.applications).containsOnlyKeys(appExtra.getId());
    assertThat(response.facets.organizations).containsOnlyKeys(orgGrandparent.getId(), orgExtra.getId());
    assertThat(response.facets.organizations).containsEntry(orgExtra.getId(), 1L);
    // orgGrandparent is orgExtra's ancestor, so its subtree bucket counts the same single violation.
    assertThat(response.facets.organizations).containsEntry(orgGrandparent.getId(), 1L);

    // With an organization ALSO selected, the rails are unchanged: the owner selection is dropped from
    // the owner facet base while the waiver-type filter is kept. If the owner selection leaked in the
    // rails would be empty (orgSibling has no waived violation); if the waiver-type filter were dropped
    // they would list all five organizations and all three applications.
    ViolationsListRequestDTO waivedAndOtherOrg = new ViolationsListRequestDTO();
    waivedAndOtherOrg.waivedWithAutoWaiver = Boolean.FALSE;
    waivedAndOtherOrg.organizationIds = Set.of(orgSibling.getId());

    ViolationsListResponseDTO filtered = violationsListService.listViolations(waivedAndOtherOrg);

    assertThat(filtered.total).isEqualTo(0);
    assertThat(filtered.facets.applications).containsOnlyKeys(appExtra.getId());
    assertThat(filtered.facets.organizations).containsOnlyKeys(orgGrandparent.getId(), orgExtra.getId());
  }
}
