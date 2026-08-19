/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.ExecutorService;

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

import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * REAL-INDEX (real Lucene + real H2 DB, no mocked search primitives) service-layer tests for the
 * Nexus One Applications list owner facets (CLM-44713, tracked by CLM-45220).
 * <p>
 * The mocked {@code ApplicationsListSessionFacetsServiceTest} stubs {@code session.termsAggregation(...)}
 * to return a canned bucket list, so it cannot catch a real docValues wiring defect in the
 * {@code parentOrganizationId} facet field. These tests instead index a real org hierarchy through the
 * production indexing pipeline ({@link LuceneSearchIndexClient#populateIndex()}) and call the real
 * {@link ApplicationsListService#listApplications} — proving the hierarchical subtree counts, ROOT
 * exclusion, off-page discovery, and no-collapse owner-facet behavior against an actual Lucene index.
 */
@ComponentH2Test
public class ApplicationsListServiceFacetsRealIndexTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApplicationsListService applicationsListService;

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
  public void setUpSessionReadPathAndIndex() {
    // Real-index tests exercise the CLM-44713 session read path (IndexReadSession.termsAggregation),
    // not the legacy per-org count() path. See ApplicationsListSessionFacetsServiceTest for the
    // mocked equivalent this test class is meant to catch what that one cannot.
    System.setProperty("nexusOne.search.readPath.applications", "new");

    // Mirror LuceneSearchIndexClientAggregateTest: swap in a mocked shutdown handler and reset the
    // lazily-created indexing executors so repeated full re-indexes within this reused-context module
    // behave deterministically across test classes.
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
    System.clearProperty("nexusOne.search.readPath.applications");
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
   * Real estate: grandparent org A → parent org B → child org C (appChild lives under C), a
   * sibling org D under A (appSibling), and an unrelated org E under A (appExtra) — enough depth and
   * breadth to prove hierarchical subtree counting, ROOT exclusion, off-page discovery, and no-collapse
   * against a real ancestor closure ({@code parentOrganizationId}) built by the real indexing pipeline.
   */
  private void buildOwnerHierarchyRealEstate() {
    orgGrandparent = tempEntity.newOrganization();
    orgParent = tempEntity.newOrganization(orgGrandparent);
    orgChild = tempEntity.newOrganization(orgParent);
    orgSibling = tempEntity.newOrganization(orgGrandparent);
    orgExtra = tempEntity.newOrganization(orgGrandparent);

    appChild = tempEntity.newApplication(orgChild.getId());
    appSibling = tempEntity.newApplication(orgSibling.getId());
    appExtra = tempEntity.newApplication(orgExtra.getId());
  }

  @Test
  public void testListApplications_OrganizationFacet_HierarchicalSubtreeCountsWithRootExcluded() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();

    ApplicationsListResponseDTO response = applicationsListService.listApplications(request);

    assertThat(response.total).isEqualTo(3);
    assertThat(response.facets).isNotNull();
    assertThat(response.facets.organizations).isNotNull();

    // Hierarchical subtree counts: each ancestor's bucket counts its whole subtree, computed from a
    // REAL parentOrganizationId docValues aggregation over the real index, not a mocked bucket list.
    assertThat(response.facets.organizations.get(orgGrandparent.getId()))
        .describedAs("grandparent org subtree: appChild + appSibling + appExtra")
        .isEqualTo(3L);
    assertThat(response.facets.organizations.get(orgParent.getId()))
        .describedAs("parent org subtree: appChild only")
        .isEqualTo(1L);
    assertThat(response.facets.organizations.get(orgChild.getId()))
        .describedAs("child org subtree: appChild only")
        .isEqualTo(1L);
    assertThat(response.facets.organizations.get(orgSibling.getId()))
        .describedAs("sibling org subtree: appSibling only")
        .isEqualTo(1L);
    assertThat(response.facets.organizations.get(orgExtra.getId()))
        .describedAs("extra org subtree: appExtra only")
        .isEqualTo(1L);

    // ROOT is in every doc's ancestor closure and must never appear as its own facet bucket.
    assertThat(response.facets.organizations).doesNotContainKey(Organization.ROOT_ORGANIZATION_ID);

    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
  }

  @Test
  public void testListApplications_OwnerFacets_OffPageValuesAppearWithSmallPageSize() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.pageSize = 1;

    ApplicationsListResponseDTO response = applicationsListService.listApplications(request);

    // Only one application on the page...
    assertThat(response.applications).hasSize(1);
    assertThat(response.hasNextPage).isTrue();
    assertThat(response.total).isEqualTo(3);

    // ...but the owner facets aggregate over the full owner-removed base, so off-page orgs/apps
    // still surface in the sidebar rails (a single-pass real aggregation, not per-page counting).
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
  }

  @Test
  public void testListApplications_SelectingOneOrganization_DoesNotCollapseSiblingOrgOrOtherAppFacets() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.organizationIds = Set.of(orgChild.getId());

    ApplicationsListResponseDTO response = applicationsListService.listApplications(request);

    // The page of RESULTS narrows to the selected org's subtree...
    assertThat(response.applications).hasSize(1);
    assertThat(response.applications.get(0).id).isEqualTo(appChild.getId());

    // ...but the owner FACETS still show the sibling/extra orgs and their apps (CLM-44713: org/app
    // facets aggregate over an owner-removed base so selecting one org does not collapse the rails).
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
  }

  @Test
  public void testListApplications_SelectingOneApplication_DoesNotCollapseOtherAppOrOrgFacets() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.applicationIds = Set.of(appSibling.getId());

    ApplicationsListResponseDTO response = applicationsListService.listApplications(request);

    assertThat(response.applications).hasSize(1);
    assertThat(response.applications.get(0).id).isEqualTo(appSibling.getId());

    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
  }

  // --- Fixed-vocabulary facet no-collapse (CLM-44713) ------------------------------------------

  /**
   * Seeds an open/SECURITY/BUILD violation on {@code appChild} and {@code appSibling}, and a
   * WAIVED/QUALITY/RELEASE violation on {@code appExtra} only — so filtering to any ONE of those
   * three dimensions (state/policy-type/stage) narrows the application results to {@code appExtra}
   * alone, letting each dimension's facet be asserted to still show the OTHER (open/appChild+appSibling)
   * value at its full, unnarrowed count.
   */
  private void seedFixedFacetVarietyRealEstate() throws Exception {
    seedOpenSecurityBuildViolation(appChild, orgChild, "child", "hashPvtOpenChild001");
    seedOpenSecurityBuildViolation(appSibling, orgSibling, "sibling", "hashPvtOpenSibl001");

    PolicyEvaluation extraReleaseEvaluation =
        tempEntity.newPolicyEvaluation(appExtra.getId(), Stage.ID_RELEASE, "extraReleaseScan");
    ReportTestUtils.createReportFile(
        extraReleaseEvaluation.getOwnerId(),
        extraReleaseEvaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", tempDir),
        insightWork);
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

  private void seedOpenSecurityBuildViolation(
      final Application app,
      final Organization org,
      final String tag,
      final String hash) throws Exception
  {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, tag + "OpenScan");
    ReportTestUtils.createReportFile(
        evaluation.getOwnerId(),
        evaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/policyViolationReport", tempDir),
        insightWork);
    Policy policy = tempEntity.newPolicy(org.getId(), "Security - " + tag);
    tempEntity.newPolicyViolation(evaluation, policy, 8, PolicyThreatCategory.SECURITY,
        "org.apache.logging", "log4j-core", "2.14.0", hash);
  }

  @Test
  public void testListApplications_SelectingViolationStateFilter_DoesNotCollapseOtherViolationStateFacets() throws Exception {
    seedFixedFacetVarietyRealEstate();

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyViolationStates = new PolicyViolationStateFilter(PolicyViolationState.WAIVED);

    ApplicationsListResponseDTO response = applicationsListService.listApplications(request);

    // Results narrow to just appExtra (the only app with a WAIVED violation)...
    assertThat(response.applications).extracting(card -> card.id).containsExactly(appExtra.getId());

    // ...but the violationStates facet still shows OPEN at its full, unnarrowed count (appChild +
    // appSibling), not collapsed to zero under the WAIVED-narrowed application ids.
    assertThat(response.facets.violationStates).containsEntry(PolicyViolationState.OPEN.name(), 2L);
    assertThat(response.facets.violationStates).containsEntry(PolicyViolationState.WAIVED.name(), 1L);
  }

  @Test
  public void testListApplications_SelectingPolicyTypeFilter_DoesNotCollapseOtherPolicyTypeFacets() throws Exception {
    seedFixedFacetVarietyRealEstate();

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyThreatCategories = new PolicyThreatCategoryFilter(PolicyThreatCategory.QUALITY);

    ApplicationsListResponseDTO response = applicationsListService.listApplications(request);

    assertThat(response.applications).extracting(card -> card.id).containsExactly(appExtra.getId());

    // policyTypes facet still shows SECURITY at its full, unnarrowed count.
    assertThat(response.facets.policyTypes).containsEntry(PolicyThreatCategory.SECURITY.getName(), 2L);
    assertThat(response.facets.policyTypes).containsEntry(PolicyThreatCategory.QUALITY.getName(), 1L);
  }

  @Test
  public void testListApplications_SelectingStageFilter_DoesNotCollapseOtherStageFacets() throws Exception {
    seedFixedFacetVarietyRealEstate();

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.stageIds = Set.of(Stage.ID_RELEASE);

    ApplicationsListResponseDTO response = applicationsListService.listApplications(request);

    assertThat(response.applications).extracting(card -> card.id).containsExactly(appExtra.getId());

    // stages facet still shows BUILD at its full, unnarrowed count.
    assertThat(response.facets.stages).containsEntry(Stage.ID_BUILD, 2L);
    assertThat(response.facets.stages).containsEntry(Stage.ID_RELEASE, 1L);
  }

  /**
   * A violation-scoped filter (here stage; threat level, policy type and violation state behave the same
   * way) narrows the owner rails to the owners that actually have a matching violation. Only the owner
   * dimension is dropped from the owner facet base — the violation scope stays, so a release-stage page
   * shows organization and application counts for release-stage violations rather than for the whole
   * estate.
   * <p>
   * Asserted separately from the stages-facet no-collapse test above because the two rules pull in
   * opposite directions: the stage rail must NOT narrow under a stage selection, while the owner rails
   * MUST. Keeping them apart makes a failure name the rule that broke.
   */
  @Test
  public void testListApplications_SelectingStageFilter_NarrowsOwnerFacetsToOwnersOfMatchingViolations() throws Exception {
    seedFixedFacetVarietyRealEstate();

    ApplicationsListRequestDTO stageOnly = new ApplicationsListRequestDTO();
    stageOnly.stageIds = Set.of(Stage.ID_RELEASE);

    ApplicationsListResponseDTO response = applicationsListService.listApplications(stageOnly);

    // appExtra is the only application with a release-stage violation, so it is the only owner in the
    // rails; appChild/appSibling (build-stage only) and their organizations are absent.
    assertThat(response.facets.applications).containsOnlyKeys(appExtra.getId());
    assertThat(response.facets.organizations).containsOnlyKeys(orgGrandparent.getId(), orgExtra.getId());
    assertThat(response.facets.organizations).containsEntry(orgExtra.getId(), 1L);
    // orgGrandparent is orgExtra's ancestor, so its subtree bucket counts the same single application.
    assertThat(response.facets.organizations).containsEntry(orgGrandparent.getId(), 1L);

    // With an organization ALSO selected, the rails are unchanged: the owner selection is dropped from
    // the owner facet base while the stage filter is kept. If the owner selection leaked in the rails
    // would be empty (orgChild has no release-stage violation); if the stage filter were dropped they
    // would list all five organizations and all three applications.
    ApplicationsListRequestDTO stageAndOtherOrg = new ApplicationsListRequestDTO();
    stageAndOtherOrg.stageIds = Set.of(Stage.ID_RELEASE);
    stageAndOtherOrg.organizationIds = Set.of(orgChild.getId());

    ApplicationsListResponseDTO filtered = applicationsListService.listApplications(stageAndOtherOrg);

    // The page of RESULTS is empty — no release-stage violation under orgChild...
    assertThat(filtered.applications).isEmpty();
    // ...while the rails still offer the owners the user could switch to.
    assertThat(filtered.facets.applications).containsOnlyKeys(appExtra.getId());
    assertThat(filtered.facets.organizations).containsOnlyKeys(orgGrandparent.getId(), orgExtra.getId());
  }
}
