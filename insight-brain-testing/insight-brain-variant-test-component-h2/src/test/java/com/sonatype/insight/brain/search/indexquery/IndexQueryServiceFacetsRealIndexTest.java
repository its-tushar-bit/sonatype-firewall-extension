/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.indexquery.IndexQueryResponse.IndexQueryFacetBucket;
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
 * REAL-INDEX (real Lucene + real H2 DB, no mocked {@code IndexReadSession}/{@code SearchIndexClient})
 * service-layer tests for {@code POST /rest/search/index-query} (CLM-44713, tracked by CLM-45220).
 * <p>
 * {@code IndexQueryEndpointTest} and {@code IndexQueryServiceTest} both stub
 * {@code IndexReadSession.termsAggregation(...)} to a canned/empty bucket list
 * ({@code when(session.termsAggregation(any(), anyString(), anyInt())).thenReturn(List.of())}), so
 * neither can catch a real docValues wiring defect in the id-keyed facet fields ({@code
 * parentOrganizationId}, {@code applicationId}, {@code policyWaiverPolicyId}) that {@link
 * IndexQueryService#computeFacets} aggregates via {@link
 * com.sonatype.insight.brain.search.session.IndexReadSession#termsAggregation}, nor the displayName
 * resolution {@link IndexQueryService#resolveDisplayNames} performs against the real DAOs. This class
 * instead indexes a real org hierarchy + policies + waivers through the production indexing pipeline
 * ({@link LuceneSearchIndexClient#populateIndex()}) and calls the real {@link IndexQueryService#query} —
 * the same real {@code IndexReadSessionFactory}/{@code SearchIndexClient} beans the Vulnerabilities and
 * Applications real-index tests in this module already exercise.
 * <p>
 * Mostly scoped to {@link IndexQueryType#WAIVER}: unlike {@code APPLICATION}/{@code VIOLATION}, WAIVER
 * ({@code POLICY_WAIVER}) documents are built directly from {@code Policy}/{@code PolicyWaiver} DB rows
 * ({@code DocumentBuilderHelper#buildPolicyWaiverDocs}) with no scan report required, so the real
 * estate here is a few DAO inserts rather than a hand-built report zip. WAIVER also carries every facet
 * shape the task calls out except {@code applicationCategories}: a hierarchical org VALUE facet
 * ({@code parentOrganizationId}), a flat app VALUE facet ({@code applicationId}), a flat policy VALUE
 * facet ({@code policyWaiverPolicyId}), and a NUMERIC facet ({@code policyWaiverThreatLevel}).
 * <p>
 * The {@code applicationCategories} facet and its {@code applicationCategoryIds} filter are declared
 * only for the APPLICATION and VIOLATION query types (see {@code IndexQueryService.FACET_FIELDS} and
 * {@code IndexQueryFilterSchema}), so the categories test below queries {@link
 * IndexQueryType#APPLICATION}. APPLICATION documents are likewise built straight from {@code
 * Application} DB rows ({@code DocumentBuilderHelper#buildApplicationDocs}) and need no scan report,
 * so the same tag-and-reindex estate serves it.
 * <p>
 * CLM-44713 slice 2/2b id-keying: the org/app/policy facet buckets now carry {@code value} = the
 * entity's opaque id and {@code displayName} = the resolved, real-cased name (the name docValues field
 * is backed by a case-FOLDED sort twin, so aggregating on it — the pre-id-keying behaviour — silently
 * lowercased every display name; id-keying sidesteps that entirely). The structured filters this test
 * uses to prove the no-collapse (own-clause-removal) behaviour are therefore the ID-keyed ones
 * ({@code organizationIds}/{@code applicationIds}/{@code policyIds}), which round-trip through the same
 * id fields the facets aggregate on (see {@code IndexQueryFilterSchema}); the legacy name-keyed filters
 * ({@code organizations}/{@code applications}/{@code policy}) remain supported as deprecated aliases but
 * do not exercise this alignment.
 */
@ComponentH2Test
public class IndexQueryServiceFacetsRealIndexTest
    extends AbstractComponentH2Test
{
  @Inject
  private IndexQueryService indexQueryService;

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

  private Policy policyHigh;

  private Policy policyLow;

  @BeforeEach
  public void setUpIndex() {
    // IqLocalSearchService.search rejects every call unless the Global Search preview flag is on
    // (the same flag IndexQueryResource gates the HTTP endpoint behind); calling the service directly
    // bypasses the resource's gate check, so it must be set here instead.
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    // Mirror LuceneSearchIndexClientAggregateTest / the other real-index tests in this module: swap in
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
  public void clearPreviewFlag() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
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
   * org D under A (appSibling), and an unrelated org E under A (appExtra) — the same owner shape as
   * the Applications/Vulnerabilities real-index tests, so the {@code parentOrganizationId}
   * ancestor-closure aggregation is proven the same way.
   * <p>
   * Two policies (threat levels 9 and 3) and four manual, application-scoped waivers: appChild carries
   * both policies (2 waivers), appSibling and appExtra each carry one. Every org/app/policy name below
   * is deliberately real mixed-case ({@code TemporaryEntity}'s "Test Org "/"Test App " prefixes, and an
   * explicit "Policy High"/"Policy Low"), so a regression that aggregates on the case-folded name
   * docValues field instead of the id field (the bug id-keying fixes) shows up as a lowercased
   * {@code displayName} rather than passing silently.
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

    policyHigh = tempEntity.newPolicy(orgGrandparent.getId(), "Policy High", 9);
    policyLow = tempEntity.newPolicy(orgGrandparent.getId(), "Policy Low", 3);

    tempEntity.newWaiver(policyHigh.getId(), appChild.getId());
    tempEntity.newWaiver(policyLow.getId(), appChild.getId());
    tempEntity.newWaiver(policyHigh.getId(), appSibling.getId());
    tempEntity.newWaiver(policyLow.getId(), appExtra.getId());
  }

  private IndexQueryResponse queryWaivers(final Map<String, Object> filters, final Integer pageSize) {
    IndexQueryRequest request =
        new IndexQueryRequest("WAIVER", filters, 1, pageSize, null, null, true);
    return indexQueryService.query(IndexQueryType.WAIVER, request);
  }

  private IndexQueryResponse queryApplications(final Map<String, Object> filters) {
    IndexQueryRequest request =
        new IndexQueryRequest("APPLICATION", filters, 1, 100, null, null, true);
    return indexQueryService.query(IndexQueryType.APPLICATION, request);
  }

  private static long countOf(final List<IndexQueryFacetBucket> buckets, final String value) {
    return buckets.stream()
        .filter(bucket -> bucket.value().equals(value))
        .mapToLong(IndexQueryFacetBucket::count)
        .findFirst()
        .orElse(0L);
  }

  @Test
  public void testQueryWaivers_OrgAppPolicyValueFacetsAndNumericThreatLevelFacet_AggregateAgainstRealIndex() {
    IndexQueryResponse response = queryWaivers(Map.of(), 100);

    assertThat(response.totalEstimate()).isEqualTo(4);
    assertThat(response.facets()).isNotNull();

    // Hierarchical org VALUE facet: real termsAggregation over the real parentOrganizationId
    // docValues field, ancestor-closure counts, ROOT excluded. The bucket value is the org id, and
    // displayName is the real-cased org name resolved via a single batched OrganizationDAO#getByIds
    // call (id-keying, CLM-44713 slice 2) rather than the case-folded name field.
    List<IndexQueryFacetBucket> organizations = response.facets().get("organizations");
    assertThat(organizations).isNotNull();
    assertThat(organizations)
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::displayName, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(orgGrandparent.getId(), orgGrandparent.getName(), 4L),
            tuple(orgParent.getId(), orgParent.getName(), 2L),
            tuple(orgChild.getId(), orgChild.getName(), 2L),
            tuple(orgSibling.getId(), orgSibling.getName(), 1L),
            tuple(orgExtra.getId(), orgExtra.getName(), 1L));
    assertThat(organizations).extracting(IndexQueryFacetBucket::value)
        .doesNotContain(Organization.ROOT_ORGANIZATION_ID);

    // Flat app VALUE facet: bucket value is the application id, displayName the real-cased app name.
    List<IndexQueryFacetBucket> applications = response.facets().get("applications");
    assertThat(applications).isNotNull();
    assertThat(applications)
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::displayName, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(appChild.getId(), appChild.getName(), 2L),
            tuple(appSibling.getId(), appSibling.getName(), 1L),
            tuple(appExtra.getId(), appExtra.getName(), 1L));
    // The casing bug id-keying fixes: aggregating on the case-folded name field would silently
    // lowercase every displayName, so assert the real mixed case survives end to end.
    assertThat(applications).extracting(IndexQueryFacetBucket::displayName)
        .allMatch(name -> name.startsWith("Test App"));

    // Flat policy VALUE facet: bucket value is the policy id, displayName the real-cased policy name.
    List<IndexQueryFacetBucket> policy = response.facets().get("policy");
    assertThat(policy).isNotNull();
    assertThat(policy)
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::displayName, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(policyHigh.getId(), "Policy High", 2L),
            tuple(policyLow.getId(), "Policy Low", 2L));

    // NUMERIC threatLevel facet, via aggregateCountByField (not termsAggregation) and not id-keyed --
    // unchanged by CLM-44713 slice 2/2b, but likewise a real, un-mocked aggregation over the real index.
    List<IndexQueryFacetBucket> threatLevel = response.facets().get("threatLevel");
    assertThat(threatLevel).isNotNull();
    assertThat(countOf(threatLevel, "9")).isEqualTo(2L);
    assertThat(countOf(threatLevel, "3")).isEqualTo(2L);
  }

  @Test
  public void testQueryWaivers_OffPageFacetValuesAppearWithSmallPageSize() {
    IndexQueryResponse response = queryWaivers(Map.of(), 1);

    // Only one waiver row on the page...
    assertThat(response.rows()).hasSize(1);
    assertThat(response.totalEstimate()).isEqualTo(4);

    // ...but every owner/policy facet bucket is a whole-corpus count, independent of the page: all
    // five orgs, all three apps and both policies still surface in the rail, keyed by id.
    assertThat(response.facets().get("organizations")).extracting(IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder(
            orgGrandparent.getId(), orgParent.getId(), orgChild.getId(),
            orgSibling.getId(), orgExtra.getId());
    assertThat(response.facets().get("applications")).extracting(IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder(appChild.getId(), appSibling.getId(), appExtra.getId());
    assertThat(response.facets().get("policy")).extracting(IndexQueryFacetBucket::value)
        .containsExactlyInAnyOrder(policyHigh.getId(), policyLow.getId());

    // The NUMERIC threatLevel facet is whole-corpus for the same reason: it is one
    // aggregateCountByField pass over the RBAC-scoped base, not a tally of the page's rows. The single
    // page row carries one threat level, yet both levels still report their full corpus counts.
    List<IndexQueryFacetBucket> threatLevel = response.facets().get("threatLevel");
    assertThat(countOf(threatLevel, "9")).isEqualTo(2L);
    assertThat(countOf(threatLevel, "3")).isEqualTo(2L);
  }

  @Test
  public void testQueryWaivers_SelectingOneOrganization_DoesNotCollapseTheOrganizationsOrApplicationsFacet() {
    // organizationIds (parentOrganizationId) and applicationIds (applicationId) are ONE OR'd "owner"
    // dimension (CLM-44713 tech-lead decision b), compiled by IndexQueryFilterCompiler into a single
    // combined chip registered under BOTH fields in clausesByField. Selecting only an org therefore
    // still lets computeFacets' own-clause removal drop the WHOLE owner chip when building either the
    // organizations or the applications facet base, so BOTH stay whole-corpus -- not just the facet
    // whose own filter was set. If this assertion fails, the owner-chip wiring is broken and the rail
    // collapses to only the selected org/app -- a real service bug, not a test issue.
    IndexQueryResponse response = queryWaivers(Map.of("organizationIds", List.of(orgChild.getId())), 100);

    // RESULTS narrow to orgChild's subtree (appChild's 2 waivers)...
    assertThat(response.totalEstimate()).isEqualTo(2);

    // ...but the organizations FACET still lists every org (sibling orgs included) with its
    // whole-corpus subtree count, and the applications FACET still lists every app.
    assertThat(response.facets().get("organizations"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(orgGrandparent.getId(), 4L),
            tuple(orgParent.getId(), 2L),
            tuple(orgChild.getId(), 2L),
            tuple(orgSibling.getId(), 1L),
            tuple(orgExtra.getId(), 1L));
    assertThat(response.facets().get("applications"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(appChild.getId(), 2L),
            tuple(appSibling.getId(), 1L),
            tuple(appExtra.getId(), 1L));
  }

  @Test
  public void testQueryWaivers_SelectingOneApplication_DoesNotCollapseTheApplicationsOrOrganizationsFacet() {
    // applicationIds (applicationId) is the other half of the owner OR-group (see the organizationIds
    // test above): selecting only an app still drops the WHOLE owner chip when building EITHER the
    // applications or the organizations facet base, so both stay whole-corpus. appSibling (not
    // appChild) is used here so this test is not merely the mirror image of the org-selection test's
    // subtree, but a distinct single-app selection under its own org (orgSibling).
    IndexQueryResponse response = queryWaivers(Map.of("applicationIds", List.of(appSibling.getId())), 100);

    // RESULTS narrow to appSibling's 1 waiver...
    assertThat(response.totalEstimate()).isEqualTo(1);

    // ...but the applications FACET still lists every app, AND the organizations FACET still lists
    // every org (including orgs unrelated to appSibling's owner chain), both with whole-corpus counts.
    assertThat(response.facets().get("applications"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(appChild.getId(), 2L),
            tuple(appSibling.getId(), 1L),
            tuple(appExtra.getId(), 1L));
    assertThat(response.facets().get("organizations"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(orgGrandparent.getId(), 4L),
            tuple(orgParent.getId(), 2L),
            tuple(orgChild.getId(), 2L),
            tuple(orgSibling.getId(), 1L),
            tuple(orgExtra.getId(), 1L));
  }

  @Test
  public void testQueryWaivers_SelectingOrganizationAndApplicationTogether_UnionsResultsAndKeepsBothFacetsFull() {
    // organizationIds=[orgChild] and applicationIds=[appExtra] together: orgChild and appExtra's owner
    // (orgExtra) are unrelated branches of the hierarchy, so this proves owner-OR (not owner-AND): the
    // combined chip is (parentOrganizationId:orgChild OR applicationId:appExtra), AND'd with the rest
    // of the (empty) filter set -- an owner-AND reading would produce an impossible query (0 results);
    // owner-OR produces the UNION of orgChild's subtree and appExtra's waivers.
    IndexQueryResponse response = queryWaivers(
        Map.of(
            "organizationIds", List.of(orgChild.getId()),
            "applicationIds", List.of(appExtra.getId())),
        100);

    // RESULTS = orgChild's subtree (appChild's 2 waivers) UNION appExtra's 1 waiver = 3, not an empty
    // AND and not just one side.
    assertThat(response.totalEstimate()).isEqualTo(3);
    assertThat(response.rows()).extracting(row -> row.getFields().get("applicationId"))
        .containsExactlyInAnyOrderElementsOf(
            List.of(appChild.getId(), appChild.getId(), appExtra.getId()));

    // ...and BOTH facets stay whole-corpus: organizations includes appExtra's owner (orgExtra) even
    // though it was reached only via the applicationIds side of the OR, and applications includes
    // appChild even though it was reached only via the organizationIds side.
    assertThat(response.facets().get("organizations"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(orgGrandparent.getId(), 4L),
            tuple(orgParent.getId(), 2L),
            tuple(orgChild.getId(), 2L),
            tuple(orgSibling.getId(), 1L),
            tuple(orgExtra.getId(), 1L));
    assertThat(response.facets().get("applications"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(appChild.getId(), 2L),
            tuple(appSibling.getId(), 1L),
            tuple(appExtra.getId(), 1L));
  }

  @Test
  public void testQueryWaivers_SelectingOnePolicy_DoesNotCollapseThePolicyFacet() {
    // policyIds is the id-keyed structured filter and compiles directly to policyWaiverPolicyId -- the
    // SAME field the policy facet aggregates on -- so own-clause removal applies here too.
    IndexQueryResponse response = queryWaivers(Map.of("policyIds", List.of(policyHigh.getId())), 100);

    // RESULTS narrow to the 2 waivers referencing policyHigh (appChild + appSibling)...
    assertThat(response.totalEstimate()).isEqualTo(2);

    // ...but the policy FACET still lists both policies with their whole-corpus counts, id-keyed.
    assertThat(response.facets().get("policy"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(policyHigh.getId(), 2L),
            tuple(policyLow.getId(), 2L));
  }

  /**
   * The {@code applicationCategories} facet and the {@code applicationCategoryIds} filter, end to end
   * against the real index: both key on the denormalized {@code applicationCategoryId} field written on
   * every document belonging to a tagged application, and the display name is resolved from the real
   * {@code TagDAO} (application categories are backed by {@code Tag}).
   * <p>
   * The two categories are deliberately real mixed-case, because the id-keyed aggregation is what keeps
   * the display name real-cased: a regression that aggregated the case-FOLDED
   * {@code applicationCategoryName} sort twin instead would silently lowercase both names here.
   * <p>
   * The categories are attached inside this test rather than in the shared fixture, so the index is
   * repopulated to pick them up.
   */
  @Test
  public void testQueryApplications_ApplicationCategoriesFacet_FiltersByIdAndDoesNotCollapse() {
    Tag categoryFinance = tempEntity.newTag(orgGrandparent.getId(), "Finance Category");
    Tag categoryPlatform = tempEntity.newTag(orgGrandparent.getId(), "Platform Category");
    tempEntity.newApplicationTag(appChild.getId(), categoryFinance.getId());
    tempEntity.newApplicationTag(appSibling.getId(), categoryPlatform.getId());
    // appExtra stays uncategorized, so it must never be reachable through a category filter.
    luceneSearchIndexClient.populateIndex();

    IndexQueryResponse unfiltered = queryApplications(Map.of());

    // All three applications are on the page; the categories facet counts the two that carry one,
    // id-keyed with the real-cased name resolved from the DB.
    assertThat(unfiltered.totalEstimate()).isEqualTo(3);
    assertThat(unfiltered.facets().get("applicationCategories"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::displayName, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(categoryFinance.getId(), "Finance Category", 1L),
            tuple(categoryPlatform.getId(), "Platform Category", 1L));

    IndexQueryResponse filtered =
        queryApplications(Map.of("applicationCategoryIds", List.of(categoryFinance.getId())));

    // RESULTS narrow to the applications carrying that category -- appChild only, not the
    // differently-categorized appSibling and not the uncategorized appExtra.
    assertThat(filtered.totalEstimate()).isEqualTo(1);
    assertThat(filtered.rows()).extracting(row -> row.getFields().get("applicationId"))
        .containsExactly(appChild.getId());

    // ...but the categories FACET does not collapse to the selection: applicationCategoryIds compiles
    // to the same applicationCategoryId field the facet aggregates on, so computeFacets' own-clause
    // removal drops it from that facet's base and the unselected category is still offered with its
    // whole-corpus count. If this fails, selecting a category becomes single-use -- no multi-select and
    // no way back but Reset.
    assertThat(filtered.facets().get("applicationCategories"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(categoryFinance.getId(), 1L),
            tuple(categoryPlatform.getId(), 1L));
  }
}
