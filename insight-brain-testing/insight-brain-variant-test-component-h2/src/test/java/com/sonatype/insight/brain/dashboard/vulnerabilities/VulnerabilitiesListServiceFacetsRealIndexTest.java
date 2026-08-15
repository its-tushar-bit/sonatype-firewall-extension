/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
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
 * Nexus One Vulnerabilities My Scan Data list hierarchical owner facets (CLM-44713, tracked by
 * CLM-45220).
 * <p>
 * Mirrors {@code ApplicationsListServiceFacetsRealIndexTest}: a real org hierarchy is indexed through
 * the production report-ingestion + indexing pipeline (real {@code SECURITY_VULNERABILITY} documents,
 * not stubbed session buckets) and the real {@link VulnerabilitiesListService#listVulnerabilities}
 * is exercised. Unlike the Applications owner facets (plain doc-count), the Vulnerabilities owner
 * facets are DISTINCT-{@code vulnerabilityId} counts (see
 * {@link VulnerabilitiesListScopeFacetsBuilder}), so the real estate here deliberately indexes one
 * vulnerability across multiple components in the same application — proving the facet reports the
 * distinct vulnerability count and not the raw uncollapsed document count.
 */
@ComponentH2Test
public class VulnerabilitiesListServiceFacetsRealIndexTest
    extends AbstractComponentH2Test
{
  private static final String CVE_CHILD_A = "CVE-2024-1001";

  private static final String CVE_CHILD_B = "CVE-2024-1002";

  private static final String CVE_SIBLING = "CVE-2024-2001";

  private static final String CVE_EXTRA = "CVE-2024-3001";

  @Inject
  private VulnerabilitiesListService vulnerabilitiesListService;

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
   * appChild carries TWO distinct vulnerabilities: {@link #CVE_CHILD_A}, which hits two different
   * components (two uncollapsed {@code SECURITY_VULNERABILITY} documents), and {@link #CVE_CHILD_B},
   * which hits one. That is three documents behind two distinct vulnerability ids — the shape that
   * would fool a doc-count facet but must not fool the real distinct-count aggregation.
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

    indexVulnerabilityScan(
        appChild,
        "childScan",
        List.of(0, 1),
        List.of(
            new VulnHit(0, CVE_CHILD_A, 9.8),
            new VulnHit(1, CVE_CHILD_A, 9.8),
            new VulnHit(0, CVE_CHILD_B, 5.0)));
    indexVulnerabilityScan(
        appSibling,
        "siblingScan",
        List.of(2),
        List.of(new VulnHit(2, CVE_SIBLING, 7.5)));
    indexVulnerabilityScan(
        appExtra,
        "extraScan",
        List.of(3),
        List.of(new VulnHit(3, CVE_EXTRA, 2.0)));
  }

  private record VulnHit(int componentIndex, String vulnerabilityId, double score)
  {
  }

  /**
   * Writes a minimal real scan report (bom.json/security.json/licenses.json/dependencies.json),
   * zips it, and attaches it to a real {@link PolicyEvaluation} scan so the production report
   * ingestion + indexing pipeline is what produces the {@code SECURITY_VULNERABILITY} documents —
   * not a hand-built {@code Document}. Mirrors {@code VulnerabilitiesListEstateRankingTest}'s fixture
   * shape at a much smaller scale.
   */
  private void indexVulnerabilityScan(
      final Application app,
      final String scanId,
      final List<Integer> componentIndices,
      final List<VulnHit> hits) throws Exception
  {
    File reportDir = tempDir.newFolder("report-" + scanId);
    Files.writeString(reportDir.toPath().resolve("dependencies.json"), "{}");
    Files.writeString(reportDir.toPath().resolve("licenses.json"), "{\"aaData\":[]}");

    StringBuilder components = new StringBuilder("{\"aaData\":[");
    for (int i = 0; i < componentIndices.size(); i++) {
      if (i > 0) {
        components.append(',');
      }
      components.append(component(componentIndices.get(i)));
    }
    components.append("]}");

    StringBuilder vulnerabilities = new StringBuilder("{\"aaData\":[");
    for (int i = 0; i < hits.size(); i++) {
      if (i > 0) {
        vulnerabilities.append(',');
      }
      VulnHit hit = hits.get(i);
      vulnerabilities.append(vulnerability(hit.componentIndex(), hit.vulnerabilityId(), hit.score()));
    }
    vulnerabilities.append("]}");

    Files.writeString(reportDir.toPath().resolve("bom.json"), components.toString());
    Files.writeString(reportDir.toPath().resolve("security.json"), vulnerabilities.toString());

    File reportZip = new File(tempDir.getRoot(), scanId + ".zip");
    Zipper.zip(reportDir, reportZip);

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(), reportZip, insightWork);
  }

  /**
   * Report ingestion reads the nullable keys rather than defaulting them, so a component carries the
   * same key set as the checked-in report fixtures even where the values are null (see
   * {@code VulnerabilitiesListEstateRankingTest#component}).
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

  /** One vulnerability hit affecting the component at {@code componentIndex}, keyed by the same hash. */
  private static String vulnerability(
      final int componentIndex,
      final String vulnerabilityId,
      final double score)
  {
    return """
        {%s,"reference":"%s","source":"cve","score":%s,"cwe":"200","status":"Open",\
        "matchState":"exact","proprietary":false}\
        """.formatted(identifier(componentIndex), vulnerabilityId, score);
  }

  /** Coordinates and hash, repeated verbatim by the component and by every vulnerability on it. */
  private static String identifier(final int componentIndex) {
    return """
        "componentIdentifier":{"format":"maven","coordinates":\
        {"artifactId":"artifact%d","classifier":"","extension":"jar","groupId":"test","version":"1.0"}},\
        "hash":"%s"\
        """.formatted(componentIndex, String.format(Locale.ROOT, "hash%016d", componentIndex));
  }

  @Test
  public void testListVulnerabilities_OwnerFacets_DistinctCountsHierarchicalWithRootExcluded() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();

    VulnerabilitiesListResponseDTO response = vulnerabilitiesListService.listVulnerabilities(request);

    // Estate-distinct total: 4 vulnerability ids, even though appChild alone carries 3 uncollapsed
    // SECURITY_VULNERABILITY documents (CVE_CHILD_A x2 components + CVE_CHILD_B x1).
    assertThat(response.total).isEqualTo(4);
    assertThat(response.facets).isNotNull();
    assertThat(response.facets.applications).isNotNull();
    assertThat(response.facets.organizations).isNotNull();

    // App facet is a DISTINCT vulnerabilityId count, not a raw document count: appChild has 3
    // uncollapsed documents behind only 2 distinct vulnerabilities.
    assertThat(response.facets.applications.get(appChild.getId()))
        .describedAs("appChild: distinct(CVE_CHILD_A, CVE_CHILD_B) despite 3 uncollapsed docs")
        .isEqualTo(2L);
    assertThat(response.facets.applications.get(appSibling.getId())).isEqualTo(1L);
    assertThat(response.facets.applications.get(appExtra.getId())).isEqualTo(1L);
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());

    // Org facet: hierarchical ancestor-closure subtree counts, also distinct-vulnerabilityId.
    assertThat(response.facets.organizations.get(orgChild.getId()))
        .describedAs("child org subtree: appChild's 2 distinct vulnerabilities")
        .isEqualTo(2L);
    assertThat(response.facets.organizations.get(orgParent.getId()))
        .describedAs("parent org subtree: appChild only")
        .isEqualTo(2L);
    assertThat(response.facets.organizations.get(orgGrandparent.getId()))
        .describedAs("grandparent org subtree: appChild(2) + appSibling(1) + appExtra(1)")
        .isEqualTo(4L);
    assertThat(response.facets.organizations.get(orgSibling.getId())).isEqualTo(1L);
    assertThat(response.facets.organizations.get(orgExtra.getId())).isEqualTo(1L);

    // ROOT is in every doc's ancestor closure and must never appear as its own facet bucket.
    assertThat(response.facets.organizations).doesNotContainKey(Organization.ROOT_ORGANIZATION_ID);
  }

  /**
   * Proves the ancestor buckets are a distinct UNION rather than a sum of their children.
   * <p>
   * The base fixture gives every application its own CVEs, so a grandparent bucket of 4 is consistent with
   * both a union and a naive sum, and a regression to per-child addition would still pass. Adding a second
   * application under the sibling organization that shares one of the child organization's CVEs separates
   * them: the union stays 4 while the sum of the children becomes 5.
   */
  @Test
  public void testListVulnerabilities_AncestorBuckets_AreADistinctUnionNotASumOfChildren() throws Exception {
    Application appSharing = tempEntity.newApplication(orgSibling.getId());
    indexVulnerabilityScan(appSharing, "sharedScan", List.of(4), List.of(new VulnHit(4, CVE_CHILD_A, 7.5)));
    // The index is populated once in setUp, so pick up the scan added by this test.
    luceneSearchIndexClient.populateIndex();

    VulnerabilitiesListResponseDTO response =
        vulnerabilitiesListService.listVulnerabilities(new VulnerabilitiesListRequestDTO());

    // The sibling subtree now holds its own CVE plus the one it shares with the child organization.
    assertThat(response.facets.organizations.get(orgSibling.getId()))
        .describedAs("sibling subtree: CVE_SIBLING + the CVE shared with appChild")
        .isEqualTo(2L);

    // Grandparent spans {CVE_CHILD_A, CVE_CHILD_B, CVE_SIBLING, CVE_EXTRA}. Adding its children's counts
    // would give 2 + 2 + 1 = 5, so this assertion fails if the closure is summed instead of deduplicated.
    assertThat(response.facets.organizations.get(orgGrandparent.getId()))
        .describedAs("grandparent subtree: distinct union of 4 CVEs, not the 5 its children sum to")
        .isEqualTo(4L);
  }

  @Test
  public void testListVulnerabilities_OwnerFacets_OffPageValuesAppearWithSmallPageSize() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.pageSize = 1;

    VulnerabilitiesListResponseDTO response = vulnerabilitiesListService.listVulnerabilities(request);

    // Only one vulnerability row on the page...
    assertThat(response.vulnerabilities).hasSize(1);
    assertThat(response.hasNextPage).isTrue();
    assertThat(response.total).isEqualTo(4);

    // ...but the owner facets aggregate over the whole owner-removed base, so off-page orgs/apps
    // still surface in the sidebar rails.
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
  }

  @Test
  public void testListVulnerabilities_SelectingOneOrganization_DoesNotCollapseSiblingOrgOrAppFacets() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    // Mid-level org: narrows RESULTS to its subtree (appChild) via ancestor-match on
    // PARENT_ORGANIZATION_ID, proving hierarchical filtering as well as facet no-collapse.
    request.organizationIds = Set.of(orgParent.getId());

    VulnerabilitiesListResponseDTO response = vulnerabilitiesListService.listVulnerabilities(request);

    // The RESULTS narrow to the selected org's subtree (appChild's 2 distinct vulnerabilities)...
    assertThat(response.total).isEqualTo(2);

    // ...but the owner FACETS still show the sibling/extra orgs and their apps (CLM-44713: org/app
    // facets aggregate over an owner-removed base so selecting one org does not collapse the rails).
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
  }

  @Test
  public void testListVulnerabilities_SelectingOneApplication_DoesNotCollapseOtherAppOrOrgFacets() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.applicationIds = Set.of(appSibling.getId());

    VulnerabilitiesListResponseDTO response = vulnerabilitiesListService.listVulnerabilities(request);

    assertThat(response.total).isEqualTo(1);

    assertThat(response.facets.applications).containsOnlyKeys(
        appChild.getId(), appSibling.getId(), appExtra.getId());
    assertThat(response.facets.organizations).containsOnlyKeys(
        orgGrandparent.getId(), orgParent.getId(), orgChild.getId(), orgSibling.getId(), orgExtra.getId());
  }

  /**
   * Organization and application are one owner dimension, unioned rather than intersected, end to end over a
   * real index. Selecting the sibling organization together with an application in a different subtree
   * returns both sides; intersecting them would return nothing, since that application is not under that
   * organization.
   * <p>
   * Matches Classic resolution, where {@code ApplicationService#getAppsByIds} adds explicitly selected
   * applications to the applications of the selected organization subtrees.
   */
  @Test
  public void testListVulnerabilities_OrganizationPlusApplicationInAnotherSubtree_ReturnsTheUnion() {
    VulnerabilitiesListRequestDTO orgOnly = new VulnerabilitiesListRequestDTO();
    orgOnly.organizationIds = Set.of(orgSibling.getId());
    assertThat(vulnerabilitiesListService.listVulnerabilities(orgOnly).total).isEqualTo(1);

    VulnerabilitiesListRequestDTO appOnly = new VulnerabilitiesListRequestDTO();
    appOnly.applicationIds = Set.of(appChild.getId());
    assertThat(vulnerabilitiesListService.listVulnerabilities(appOnly).total).isEqualTo(2);

    // appChild lives under orgChild, not orgSibling, so an intersection would be empty.
    VulnerabilitiesListRequestDTO both = new VulnerabilitiesListRequestDTO();
    both.organizationIds = Set.of(orgSibling.getId());
    both.applicationIds = Set.of(appChild.getId());

    assertThat(vulnerabilitiesListService.listVulnerabilities(both).total)
        .describedAs("sibling org (1 vulnerability) unioned with appChild (2), not intersected (0)")
        .isEqualTo(3);
  }

  @Test
  public void testListVulnerabilities_RankingAndSeverityStillWorkAlongsideOwnerFacets() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.pageSize = 10;

    VulnerabilitiesListResponseDTO response = vulnerabilitiesListService.listVulnerabilities(request);

    // Default order is worst-first (-cvssScore): CVE_CHILD_A(9.8) > CVE_SIBLING(7.5) >
    // CVE_CHILD_B(5.0) > CVE_EXTRA(2.0). The RankedGroupsResult path (main's ranking primitive) must
    // still produce this ordering and a hydrated row alongside the new owner facets.
    assertThat(response.vulnerabilities).hasSize(4);
    assertThat(response.vulnerabilities.get(0).vulnerabilityId).isEqualToIgnoringCase(CVE_CHILD_A);
    assertThat(response.vulnerabilities.get(0).cvssScore).isEqualTo(9.8f);
    // appChild only, and exactly once despite CVE_CHILD_A hitting two components in that app.
    assertThat(response.vulnerabilities.get(0).applicationCount).isEqualTo(1);

    assertThat(response.vulnerabilities.get(1).vulnerabilityId).isEqualToIgnoringCase(CVE_SIBLING);
    assertThat(response.vulnerabilities.get(2).vulnerabilityId).isEqualToIgnoringCase(CVE_CHILD_B);
    assertThat(response.vulnerabilities.get(3).vulnerabilityId).isEqualToIgnoringCase(CVE_EXTRA);

    // Severity bands (from the same ranking read) are unaffected by the owner-facet changes:
    // CVE_CHILD_A=9.8 critical, CVE_SIBLING=7.5 high, CVE_CHILD_B=5.0 medium, CVE_EXTRA=2.0 low.
    assertThat(response.facets.severities.get("critical")).isEqualTo(1L);
    assertThat(response.facets.severities.get("high")).isEqualTo(1L);
    assertThat(response.facets.severities.get("medium")).isEqualTo(1L);
    assertThat(response.facets.severities.get("low")).isEqualTo(1L);
  }

  /**
   * The owner rails drop the organization and application clauses as a unit, but every OTHER active filter
   * still narrows them — otherwise the rails would offer, and count, the whole estate no matter what else
   * the user had selected.
   * <p>
   * A critical-only severity filter matches {@link #CVE_CHILD_A} alone, which lives under appChild, so only
   * appChild and its ancestor organizations may appear on the rails.
   */
  @Test
  public void testListVulnerabilities_NonOwnerFilter_StillNarrowsTheOwnerRails() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.severities = Set.of("critical");

    VulnerabilitiesListResponseDTO response = vulnerabilitiesListService.listVulnerabilities(request);

    // CVE_CHILD_A (9.8) is the only critical vulnerability in the estate.
    assertThat(response.total).isEqualTo(1);

    assertThat(response.facets.applications)
        .describedAs("the severity filter survives owner removal, so only appChild remains on the rail")
        .containsOnlyKeys(appChild.getId());
    assertThat(response.facets.organizations)
        .describedAs("and only appChild's ancestor organizations remain")
        .containsOnlyKeys(orgGrandparent.getId(), orgParent.getId(), orgChild.getId());
  }
}
