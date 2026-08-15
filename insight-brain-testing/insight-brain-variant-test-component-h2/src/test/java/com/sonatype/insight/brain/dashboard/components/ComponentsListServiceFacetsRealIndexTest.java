/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * REAL-INDEX (real Lucene + real H2 DB, no mocked search primitives) service-layer tests for the
 * Nexus One Components list owner facets (CLM-44713, tracked by CLM-45220).
 * <p>
 * Replaces the deleted mocked {@code ComponentsListFacetsBuilderTest}, which stubbed
 * {@code session.termsAggregation(...)}/{@code countDistinctGroupedBy(...)} and so could not catch a
 * real docValues wiring defect in the {@code parentOrganizationId} facet field. This class indexes a
 * real org hierarchy through the production report-ingestion + indexing pipeline (real
 * {@code NON_VULNERABLE_COMPONENT} documents, built from real {@code bom.json} components attached to
 * real {@link PolicyEvaluation} scans, not stubbed session buckets) and exercises the real
 * {@link ComponentsListService#listComponents}.
 * <p>
 * There is no {@code nexusOne.search.readPath.components} kill switch: unlike Applications/Violations,
 * {@code SearchReadPathSurface} has no {@code COMPONENTS} entry, and {@link ComponentsListService}
 * calls the owner-removed-base {@code ComponentsListFacetsBuilder#buildFacets} overload unconditionally — the
 * session-based hierarchical owner-facet path
 * is the only path for Components, so no property needs to be set here.
 * <p>
 * The Components list is DISTINCT BY {@code componentHash} (a component can span many docs/stages/apps),
 * so unlike the Applications owner facets (plain doc-count), the Components owner facets are
 * {@code countDistinct(componentHash)} facets (see {@code ComponentsListFacetsBuilder#countOrganizations}
 * / {@code #countApplications}). The real estate below therefore gives one component TWO stages under
 * {@code appChild} (two {@code NON_VULNERABLE_COMPONENT} docs, one distinct hash) and a second component
 * shared between the sibling and extra apps (two more docs across two different apps, still one distinct
 * hash) — proving the org/app facets report distinct component counts, not raw doc counts, and that a
 * shared component crossing sibling subtrees is not double-counted at their common ancestor.
 */
@ComponentH2Test
public class ComponentsListServiceFacetsRealIndexTest
    extends AbstractComponentH2Test
{
  /** Lives only under appChild, in two different stages (2 docs, 1 distinct hash). */
  private static final int INDEX_CHILD_MULTISTAGE = 0;

  /** Lives only under appChild, in a single stage. */
  private static final int INDEX_CHILD_SINGLE = 1;

  /** Shared: indexed under BOTH appSibling and appExtra (2 docs, 1 distinct hash, crosses subtrees). */
  private static final int INDEX_SHARED = 2;

  /** Lives only under appExtra. */
  private static final int INDEX_EXTRA_ONLY = 3;

  @Inject
  private ComponentsListService componentsListService;

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

  private PolicyEvaluation appChildBuildEvaluation;

  private PolicyEvaluation appChildStageReleaseEvaluation;

  @BeforeEach
  public void setUpIndex() throws Exception {
    // Mirror LuceneSearchIndexClientAggregateTest / ApplicationsListServiceFacetsRealIndexTest: swap
    // in a mocked shutdown handler and reset the lazily-created indexing executors so repeated full
    // re-indexes within this reused-context module behave deterministically across test classes.
    applyBeanFieldOverride(AbstractSearchIndexClient.class, "shutdownHandler", mockShutdownHandler);
    applyBeanFieldOverride(DocumentBuilderHelper.class, "shutdownHandler", mockShutdownHandler);
    resetTenantExecutor(lookup(AbstractSearchIndexClient.class), "indexingExecutors");
    resetTenantExecutor(lookup(DocumentBuilderHelper.class), "evalExecutors");
    resetTenantExecutor(lookup(DocumentBuilderHelper.class), "componentExecutors");

    buildOwnerHierarchyRealEstate();
    luceneSearchIndexClient.populateIndex();
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
   * Real estate: grandparent org A -> parent org B -> child org C (appChild lives under C), a
   * sibling org D under A (appSibling), and an unrelated org E under A (appExtra) — same owner shape
   * as {@code ApplicationsListServiceFacetsRealIndexTest}, so the hierarchical ancestor-closure
   * behavior is proven the same way.
   * <p>
   * appChild carries a component indexed in TWO stages (build + stage-release): two uncollapsed
   * {@code NON_VULNERABLE_COMPONENT} docs behind one distinct hash, plus a second, single-stage
   * component (2 distinct hashes total for appChild, from 3 uncollapsed docs). appSibling and appExtra
   * each carry the SAME shared component hash (crossing their subtrees, both siblings of appChild under
   * orgGrandparent), and appExtra additionally carries its own unique component. That makes the
   * grandparent-level distinct total (4: multistage + single + shared + extra-only) strictly less than
   * the naive sum of the three apps' own distinct counts (2 + 1 + 2 = 5), because the shared hash must
   * not be double-counted once it reaches the common ancestor.
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

    appChildBuildEvaluation = indexComponentScan(appChild, "childBuildScan", Stage.ID_BUILD,
        List.of(INDEX_CHILD_MULTISTAGE, INDEX_CHILD_SINGLE));
    // Same appChild, different stage, same multistage component index (same hash) -> a second
    // uncollapsed doc behind the same distinct hash.
    appChildStageReleaseEvaluation = indexComponentScan(appChild, "childStageReleaseScan",
        Stage.ID_STAGE_RELEASE, List.of(INDEX_CHILD_MULTISTAGE));

    indexComponentScan(appSibling, "siblingScan", Stage.ID_BUILD, List.of(INDEX_SHARED));

    indexComponentScan(appExtra, "extraScan", Stage.ID_BUILD, List.of(INDEX_SHARED, INDEX_EXTRA_ONLY));
  }

  /**
   * Writes a minimal real scan report (bom.json/security.json/licenses.json/dependencies.json) with
   * NO security vulnerabilities, zips it, and attaches it to a real {@link PolicyEvaluation} scan so
   * the production report ingestion + indexing pipeline is what produces the
   * {@code NON_VULNERABLE_COMPONENT} documents — not a hand-built {@code Document}. Mirrors
   * {@code VulnerabilitiesListServiceFacetsRealIndexTest#indexVulnerabilityScan} at a much smaller
   * scale, but with an empty {@code security.json} so every bom.json component lands as
   * {@code NON_VULNERABLE_COMPONENT} (the Components-tab item type) rather than
   * {@code SECURITY_VULNERABILITY}.
   */
  private PolicyEvaluation indexComponentScan(
      final Application app,
      final String scanId,
      final String stageId,
      final List<Integer> componentIndices) throws Exception
  {
    File reportDir = tempDir.newFolder("report-" + scanId);
    Files.writeString(reportDir.toPath().resolve("dependencies.json"), "{}");
    Files.writeString(reportDir.toPath().resolve("licenses.json"), "{\"aaData\":[]}");
    Files.writeString(reportDir.toPath().resolve("security.json"), "{\"aaData\":[]}");

    StringBuilder components = new StringBuilder("{\"aaData\":[");
    for (int i = 0; i < componentIndices.size(); i++) {
      if (i > 0) {
        components.append(',');
      }
      components.append(component(componentIndices.get(i)));
    }
    components.append("]}");
    Files.writeString(reportDir.toPath().resolve("bom.json"), components.toString());

    File reportZip = new File(tempDir.getRoot(), scanId + ".zip");
    Zipper.zip(reportDir, reportZip);

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageId, scanId);
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(), reportZip, insightWork);
    return evaluation;
  }

  /**
   * Report ingestion reads the nullable keys rather than defaulting them, so a component carries the
   * same key set as the checked-in report fixtures even where the values are null (see
   * {@code VulnerabilitiesListEstateRankingTest#component} /
   * {@code VulnerabilitiesListServiceFacetsRealIndexTest#component}).
   */
  private static String component(final int index) {
    String artifactId = "artifact" + index;
    return """
        {%s,"filenames":["%s.jar"],"pathnames":["some/dir/%s.jar"],\
        "displayName":{"parts":[{"value":"%s"}]},"matchState":"exact","scanError":false,"proprietary":false,\
        "relativePopularity":null,"createTime":1364313072251,"lastModifiedTime":1481029585000,\
        "lastModifiedEntryTime":null,"website":null,"identificationSource":"Sonatype",\
        "componentCategories":[{"componentCategoryId":113,"path":"Other"}],"hygieneRating":null}\
        """.formatted(identifier(index), artifactId, artifactId, artifactId);
  }

  /** The hash the report fixture gives the component at {@code componentIndex}. */
  private static String hashOf(final int componentIndex) {
    return String.format(Locale.ROOT, "hash%016d", componentIndex);
  }

  /** Coordinates and hash, stable per component index so the same index means the same hash. */
  private static String identifier(final int componentIndex) {
    return """
        "componentIdentifier":{"format":"maven","coordinates":\
        {"artifactId":"artifact%d","classifier":"","extension":"jar","groupId":"test","version":"1.0"}},\
        "hash":"%s"\
        """.formatted(componentIndex, hashOf(componentIndex));
  }

  @Test
  public void testListComponents_OwnerFacets_DistinctCountsHierarchicalWithRootExcluded() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();

    ComponentsListResponseDTO response = componentsListService.listComponents(request);

    // Estate-distinct total: 4 distinct component hashes (multistage, single, shared, extra-only),
    // even though 5 uncollapsed NON_VULNERABLE_COMPONENT documents were indexed.
    assertThat(response.total).isEqualTo(4);
    assertThat(response.facets).isNotNull();
    assertThat(response.facets.applications).isNotNull();
    assertThat(response.facets.organizations).isNotNull();

    // App facet is a DISTINCT componentHash count, not a raw document count: appChild has 3
    // uncollapsed docs (multistage x2 + single x1) behind only 2 distinct hashes.
    assertThat(response.facets.applications.get(appChild.getId()))
        .describedAs("appChild: distinct(multistage, single) despite 3 uncollapsed docs across 2 stages")
        .isEqualTo(2L);
    assertThat(response.facets.applications.get(appSibling.getId()))
        .describedAs("appSibling: only the shared component")
        .isEqualTo(1L);
    assertThat(response.facets.applications.get(appExtra.getId()))
        .describedAs("appExtra: shared + its own unique component")
        .isEqualTo(2L);
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());

    // Org facet: hierarchical ancestor-closure subtree counts, also distinct-componentHash.
    assertThat(response.facets.organizations.get(orgChild.getId()))
        .describedAs("child org subtree: appChild's 2 distinct components")
        .isEqualTo(2L);
    assertThat(response.facets.organizations.get(orgParent.getId()))
        .describedAs("parent org subtree: appChild only")
        .isEqualTo(2L);
    assertThat(response.facets.organizations.get(orgSibling.getId()))
        .describedAs("sibling org subtree: appSibling's shared component")
        .isEqualTo(1L);
    assertThat(response.facets.organizations.get(orgExtra.getId()))
        .describedAs("extra org subtree: appExtra's shared + unique components")
        .isEqualTo(2L);
    assertThat(response.facets.organizations.get(orgGrandparent.getId()))
        .describedAs("grandparent org subtree: DISTINCT union across appChild/appSibling/appExtra "
            + "(4), NOT the naive sum of each app's own distinct count (2+1+2=5) — the shared hash "
            + "crossing appSibling/appExtra must not be double-counted at their common ancestor")
        .isEqualTo(4L);

    // ROOT is in every doc's ancestor closure and must never appear as its own facet bucket.
    assertThat(response.facets.organizations).doesNotContainKey(Organization.ROOT_ORGANIZATION_ID);

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

  /**
   * Rows are asserted as empty here because of a separate defect, tracked as CLM-45441:
   * {@code ComponentsListDistinctPageFetcher.fetch} hardcodes {@code allComponents=false} on its
   * {@code searchIndexClient.searchIndex(...)} calls. On the real Lucene
   * backend that makes {@code AbstractSearchIndexClient#createInitialQuery} append {@code
   * -itemType:NON_VULNERABLE_COMPONENT} to the query, so an all-NON_VULNERABLE_COMPONENT estate (like
   * this one — no component here has a recorded CVE) yields zero raw hits and therefore an EMPTY
   * {@code response.components} / {@code hasNextPage=false}, even though {@code response.total} and
   * every owner facet below are correctly non-zero (those are computed via {@code countDistinct} /
   * {@code IndexReadSession.countDistinctGroupedBy}, a separate code path that this does not affect).
   * That contradicts {@link ComponentsListService}'s own class Javadoc ("Rows are distinct
   * componentHash values folded from NON_VULNERABLE_COMPONENT and SECURITY_VULNERABILITY docs") and
   * looks like a real paging defect — but it is orthogonal to the owner-facets contract this test
   * class covers, so only the facets/total are asserted here, not the page rows.
   */
  @Test
  public void testListComponents_OwnerFacets_OffPageValuesAppearWithSmallPageSize() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.pageSize = 1;

    ComponentsListResponseDTO response = componentsListService.listComponents(request);

    assertThat(response.total).isEqualTo(4);

    // The owner facets aggregate over the whole owner-removed base (via a distinct-count index
    // session read, not the raw searchIndex page walk), so off-page orgs/apps still surface in the
    // sidebar rails even with a page size of 1.
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
  }

  @Test
  public void testListComponents_SelectingOneOrganization_DoesNotCollapseSiblingOrgOrAppFacets() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    // Mid-level org: narrows RESULTS to its subtree (appChild) via ancestor-match on
    // PARENT_ORGANIZATION_ID, proving hierarchical filtering as well as facet no-collapse.
    request.organizationIds = Set.of(orgParent.getId());

    ComponentsListResponseDTO response = componentsListService.listComponents(request);

    // The RESULTS narrow to the selected org's subtree (appChild's 2 distinct components)...
    assertThat(response.total).isEqualTo(2);

    // ...but the owner FACETS still show the sibling/extra orgs and their apps (CLM-44713: org/app
    // facets aggregate over an owner-removed base so selecting one org does not collapse the rails).
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
    // Counts themselves are also unnarrowed by the selection (still the full estate-wide subtree counts).
    assertThat(response.facets.organizations.get(orgGrandparent.getId())).isEqualTo(4L);
    assertThat(response.facets.applications.get(appExtra.getId())).isEqualTo(2L);
  }

  @Test
  public void testListComponents_SelectingOneApplication_DoesNotCollapseOtherAppOrOrgFacets() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.applicationIds = Set.of(appSibling.getId());

    ComponentsListResponseDTO response = componentsListService.listComponents(request);

    assertThat(response.total).isEqualTo(1);

    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
    assertThat(response.facets.applications.get(appChild.getId())).isEqualTo(2L);
    assertThat(response.facets.organizations.get(orgChild.getId())).isEqualTo(2L);
  }

  /**
   * A violation-scoped filter (stage / threat level) that no component satisfies empties the owner rails
   * as well as the results.
   * <p>
   * Stage and threat level are resolved into a set of component hashes, and the "nothing matched" signal
   * therefore has to travel on the component-hash dimension: the owner-removed facet base drops the owner
   * clauses, so a signal carried on the organization field would vanish there and the org/app rails would
   * report estate-wide counts beside an empty results page.
   */
  @Test
  public void testListComponents_ViolationScopedFilterMatchingNothing_EmptiesResultsAndOwnerFacets() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    // Every scan in this estate is in the build or stage-release stage, so no component can satisfy a
    // filter on the operate stage.
    request.stageIds = Set.of(Stage.ID_OPERATE);

    ComponentsListResponseDTO response = componentsListService.listComponents(request);

    assertThat(response.total).describedAs("no component satisfies the stage filter").isZero();
    assertThat(response.components).isEmpty();

    assertThat(response.facets).isNotNull();
    assertThat(response.facets.organizations)
        .describedAs("organization rail must be empty beside an empty results page, not estate-wide")
        .isNullOrEmpty();
    assertThat(response.facets.applications)
        .describedAs("application rail must be empty beside an empty results page, not estate-wide")
        .isNullOrEmpty();
  }

  /**
   * The owner rails drop the organization and application clauses as a unit, but every OTHER active filter
   * still narrows them — otherwise the rails would offer, and count, the whole estate no matter what else
   * the user had selected.
   * <p>
   * Only the extra-only component matches this search, and it lives under appExtra, so only appExtra and
   * its ancestor organizations may appear on the rails.
   */
  @Test
  public void testListComponents_NonOwnerFilter_StillNarrowsTheOwnerRails() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.search = "artifact" + INDEX_EXTRA_ONLY;

    ComponentsListResponseDTO response = componentsListService.listComponents(request);

    assertThat(response.total).describedAs("only the extra-only component matches the search").isEqualTo(1);

    assertThat(response.facets.applications)
        .describedAs("the search clause survives owner removal, so only appExtra remains on the rail")
        .containsOnlyKeys(appExtra.getId());
    assertThat(response.facets.organizations)
        .describedAs("and only appExtra's ancestor organizations remain")
        .containsOnlyKeys(orgGrandparent.getId(), orgExtra.getId());
  }

  // --- Stage facet no-collapse -------------------------------------------------------------------

  /**
   * Puts one violation in each of appChild's two stages, on a DIFFERENT component in each, so a stage
   * selection resolves to one component hash and the stages rail can be told apart from the estate:
   * build holds a threat-8 violation on the single-stage component, stage-release a threat-3 violation on
   * the multistage component. Each violation names a component that its own scan actually reported.
   * <p>
   * Seeded inside the test that needs it, and the index is re-populated here, because the shared fixture
   * carries no violations and the sibling tests' counts are stated against that.
   */
  private void seedStageVarietyViolations() {
    Policy buildPolicy = tempEntity.newPolicy(orgChild.getId(), "Security - child build");
    tempEntity.newPolicyViolation(appChildBuildEvaluation, buildPolicy, 8, PolicyThreatCategory.SECURITY,
        "test", "artifact" + INDEX_CHILD_SINGLE, "1.0", hashOf(INDEX_CHILD_SINGLE));

    Policy stageReleasePolicy = tempEntity.newPolicy(orgChild.getId(), "Quality - child stage release");
    tempEntity.newPolicyViolation(appChildStageReleaseEvaluation, stageReleasePolicy, 3,
        PolicyThreatCategory.QUALITY,
        "test", "artifact" + INDEX_CHILD_MULTISTAGE, "1.0", hashOf(INDEX_CHILD_MULTISTAGE));

    // The index is populated once in setUp, so pick up the violations added by this test.
    luceneSearchIndexClient.populateIndex();
  }

  /**
   * The stages rail counts against a base with the stage filter removed, so selecting one stage leaves the
   * other stages on the rail and the user can switch or widen the selection.
   * <p>
   * Stage on this rail is violation-scoped — it is resolved into component hashes rather than applied as a
   * clause — so removing it means resolving the hashes again without it. A base that kept the resolved
   * hashes would show only the selected stage.
   */
  @Test
  public void testListComponents_SelectingOneStage_DoesNotCollapseTheOtherStageFacets() {
    seedStageVarietyViolations();

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.stageIds = Set.of(Stage.ID_BUILD);

    ComponentsListResponseDTO response = componentsListService.listComponents(request);

    // Results narrow to the one component with a build-stage violation...
    assertThat(response.total).isEqualTo(1);

    // ...while the stages rail still carries both stages at their unnarrowed counts.
    assertThat(response.facets.stages)
        .describedAs("selecting build must leave stage-release selectable on the rail")
        .containsEntry(Stage.ID_BUILD, 1L)
        .containsEntry(Stage.ID_STAGE_RELEASE, 1L);
  }

  /**
   * Removing the stage filter from the stages-rail base must not drop the OTHER violation-scoped filters
   * with it. A threat filter that only the build-stage violation satisfies leaves stage-release off the
   * rail, so selecting a stage cannot widen the rail into threat levels the user excluded.
   */
  @Test
  public void testListComponents_SelectingOneStage_KeepsTheThreatFilterOnTheStagesFacet() {
    seedStageVarietyViolations();

    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.stageIds = Set.of(Stage.ID_BUILD);
    request.policyThreatLevelRanges = List.of(new PolicyThreatLevelFilter(8, 10));

    ComponentsListResponseDTO response = componentsListService.listComponents(request);

    assertThat(response.total).isEqualTo(1);

    // Only the build-stage violation is threat 8; the stage-release one is threat 3 and its component
    // must not reappear on the rail just because the stage filter was removed from that base.
    assertThat(response.facets.stages)
        .describedAs("the threat filter survives stage removal")
        .containsOnlyKeys(Stage.ID_BUILD);
  }
}
