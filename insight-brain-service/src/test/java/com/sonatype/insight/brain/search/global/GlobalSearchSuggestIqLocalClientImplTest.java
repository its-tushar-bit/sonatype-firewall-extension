/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.global.fieldmap.FieldMap;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration-style test for the real {@link GlobalSearchSuggestIqLocalClientImpl}. Wires a real
 * {@link IqLocalSearchService} over a mocked {@link SearchIndexClient} (stubbed at the
 * {@code searchGlobal} boundary) so the adapter's type-mapping, row-dropping, and null-principal
 * behaviour is exercised end-to-end without a live Lucene index.
 *
 * <p>
 * Extends {@link GlobalSearchSuggestIqLocalClientContractTest} so the shared SPI contract holds for
 * the real implementation too.
 */
@RunWith(MockitoJUnitRunner.class)
public class GlobalSearchSuggestIqLocalClientImplTest
    extends GlobalSearchSuggestIqLocalClientContractTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  private GlobalSearchSuggestIqLocalClientImpl underTest;

  private final UserPrincipal principal = mock(UserPrincipal.class);

  @Before
  public void setUp() {
    when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    lenient().when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    lenient().when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    lenient().when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());
    underTest = new GlobalSearchSuggestIqLocalClientImpl(new IqLocalSearchService(searchIndexClient,
        FieldMap.defaultMap()));
  }

  @Override
  protected GlobalSearchSuggestIqLocalClient createService() {
    // A fresh service with the default open-permission wiring so the contract test runs standalone.
    SearchIndexClient client = mock(SearchIndexClient.class);
    lenient().when(client.isSearchPreviewEnabled()).thenReturn(true);
    lenient().when(client.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    lenient().when(client.buildAllowedContextIdsFilter(any())).thenReturn(null);
    lenient().when(client.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    lenient().when(client.buildPermittedQuery(any())).thenCallRealMethod();
    lenient().when(client.searchGlobal(any())).thenReturn(emptyResult());
    return new GlobalSearchSuggestIqLocalClientImpl(new IqLocalSearchService(client, FieldMap.defaultMap()));
  }

  @Test
  public void nullPrincipal_shortCircuits_neverQueriesIndex() {
    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.APPLICATION), 5, null);

    assertThat(rows).isEmpty();
    verify(searchIndexClient, never()).searchGlobal(any());
  }

  @Test
  public void applicationRow_mapsIdTitleAndPublicIdSubtitle_taggedLocal_noHref() {
    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.applicationId = "app-1";
    doc.applicationName = "My App";
    doc.applicationPublicId = "my-app";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(doc), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.APPLICATION), 5, principal);

    assertThat(rows).hasSize(1);
    SuggestRow row = rows.get(0);
    assertThat(row.id()).isEqualTo("app-1");
    assertThat(row.type()).isEqualTo(SuggestItemType.APPLICATION);
    assertThat(row.title()).isEqualTo("My App");
    assertThat(row.subtitle()).isEqualTo("my-app");
    assertThat(row.source()).isEqualTo(SearchSource.LOCAL);
    assertThat(row.href()).isNull();
  }

  @Test
  public void componentRow_withIndexedIdentifier_carriesCoordinateAsId() {
    // I3: a local component whose identifier is indexed exposes a pkg: coordinate as its id (mirroring
    // the catalog leg) so a pasted coordinate can promote it to BEST MATCH. The expected coordinate is
    // derived from the same converter the mapper uses, so the assertion is robust to the exact format.
    ComponentIdentifier identifier =
        new ComponentIdentifier("maven", Map.of("groupId", "org.example", "artifactId", "lib", "version", "1.0.0"));
    String expectedCoordinate = PackageUrlIdentifier.toPackageUrl(identifier);

    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.componentName = "lib";
    doc.componentHash = "abc123";
    doc.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(identifier);
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(doc), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.COMPONENT), 5, principal);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).id()).isEqualTo(expectedCoordinate);
    assertThat(rows.get(0).title()).isEqualTo("lib");
  }

  @Test
  public void componentRow_withoutIdentifier_fallsBackToHashId() {
    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.componentName = "lib";
    doc.componentHash = "abc123";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(doc), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.COMPONENT), 5, principal);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).id()).isEqualTo("abc123");
    assertThat(rows.get(0).title()).isEqualTo("lib");
  }

  @Test
  public void componentRow_unbuildableIdentifierWithNoHashOrName_isDroppedNotThrown() {
    // A component doc whose identifier cannot render a coordinate (malformed purl) and carries neither
    // a hash nor a name yields no usable id/title. The mapper must drop that single row rather than
    // build a SuggestRow that throws (which would degrade the WHOLE COMPONENT type to empty). A second,
    // valid component row in the same fan-out must still come back.
    ApiComponentIdentifierDTOV2 unbuildable = new ApiComponentIdentifierDTOV2();
    unbuildable.setFormat("unknownformat");
    unbuildable.setCoordinates(Map.of());

    SearchResultItemDTO bad = new SearchResultItemDTO();
    bad.componentIdentifier = unbuildable;
    // No componentHash, no componentName: nothing to fall back to.

    SearchResultItemDTO good = new SearchResultItemDTO();
    good.componentName = "lib";
    good.componentHash = "abc123";

    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(bad, good), 2L, List.of()));

    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.COMPONENT), 5, principal);

    // The malformed row was dropped; the valid row survived and the COMPONENT type did not degrade.
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).id()).isEqualTo("abc123");
    assertThat(rows.get(0).title()).isEqualTo("lib");
  }

  @Test
  public void pastedCoordinate_promotesLocalComponentToBestMatch() {
    // I3 end-to-end: pasting the component's coordinate promotes the LOCAL component row to BEST MATCH.
    ComponentIdentifier identifier =
        new ComponentIdentifier("maven", Map.of("groupId", "org.example", "artifactId", "lib", "version", "1.0.0"));
    String coordinate = PackageUrlIdentifier.toPackageUrl(identifier);

    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.componentName = "lib";
    doc.componentHash = "abc123";
    doc.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(identifier);
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(doc), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest(coordinate, List.of(SuggestItemType.COMPONENT), 5, principal);
    SuggestRow bestMatch = new BestMatchResolver().resolve(coordinate, rows);

    assertThat(bestMatch).isNotNull();
    assertThat(bestMatch.type()).isEqualTo(SuggestItemType.COMPONENT);
    assertThat(bestMatch.source()).isEqualTo(SearchSource.LOCAL);
    assertThat(bestMatch.id()).isEqualTo(coordinate);
  }

  @Test
  public void mixedCasePastedCoordinate_promotesLocalComponentToBestMatch() {
    // A pasted coordinate in mixed case must still promote the matching canonical lowercased-id row:
    // the coordinate-shape gate is case-insensitive, but the row id is always the fully-lowercased
    // canonical purl emitted by the IQ purl converter.
    ComponentIdentifier identifier =
        new ComponentIdentifier("maven", Map.of("groupId", "org.example", "artifactId", "lib", "version", "1.0.0"));
    String coordinate = PackageUrlIdentifier.toPackageUrl(identifier);
    String pastedMixedCase = coordinate.toUpperCase(java.util.Locale.ROOT);
    assertThat(pastedMixedCase).isNotEqualTo(coordinate);

    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.componentName = "lib";
    doc.componentHash = "abc123";
    doc.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(identifier);
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(doc), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest(pastedMixedCase, List.of(SuggestItemType.COMPONENT), 5, principal);
    SuggestRow bestMatch = new BestMatchResolver().resolve(pastedMixedCase, rows);

    assertThat(bestMatch).isNotNull();
    assertThat(bestMatch.type()).isEqualTo(SuggestItemType.COMPONENT);
    assertThat(bestMatch.source()).isEqualTo(SearchSource.LOCAL);
    // The promoted row keeps its canonical lowercased id even though the query was mixed-case.
    assertThat(bestMatch.id()).isEqualTo(coordinate);
  }

  @Test
  public void violationRow_fallsBackToIdWhenPolicyNameBlank() {
    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.policyViolationId = "violation-1";
    doc.policyViolationPolicyName = "";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(doc), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.VIOLATION), 5, principal);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).id()).isEqualTo("violation-1");
    assertThat(rows.get(0).title()).isEqualTo("violation-1");
  }

  @Test
  public void violationRow_droppedWhenIdAndNameBlank() {
    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.policyViolationId = null;
    doc.policyViolationPolicyName = "";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(doc), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.VIOLATION), 5, principal);

    assertThat(rows).isEmpty();
  }

  @Test
  public void vulnerabilityRow_mapsIdAndDescription() {
    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.vulnerabilityId = "CVE-2024-9";
    doc.vulnerabilityDescription = "boom";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(doc), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.VULNERABILITY), 5, principal);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).type()).isEqualTo(SuggestItemType.VULNERABILITY);
    assertThat(rows.get(0).id()).isEqualTo("CVE-2024-9");
    assertThat(rows.get(0).subtitle()).isEqualTo("boom");
  }

  @Test
  public void waiverRow_fallsBackToIdWhenPolicyNameBlank() {
    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.policyWaiverId = "waiver-1";
    doc.policyWaiverPolicyName = "  ";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(doc), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.WAIVER), 5, principal);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).id()).isEqualTo("waiver-1");
    assertThat(rows.get(0).title()).isEqualTo("waiver-1");
  }

  @Test
  public void waiverRow_droppedWhenIdAndNameBlank_doesNotDegradeWholeGroup() {
    // A blank-string waiver id can be indexed (setPolicyWaiverId only rejects null). With no policy
    // name to fall back to, the row has no usable id/title. It must be dropped rather than let the
    // SuggestRow compact constructor throw, which would degrade the whole WAIVER group to empty and
    // discard the valid waiver in the same fan-out.
    SearchResultItemDTO bad = new SearchResultItemDTO();
    bad.policyWaiverId = "  ";
    bad.policyWaiverPolicyName = null;
    SearchResultItemDTO good = new SearchResultItemDTO();
    good.policyWaiverId = "waiver-1";
    good.policyWaiverPolicyName = "License Policy";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(bad, good), 2L, List.of()));

    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.WAIVER), 5, principal);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).id()).isEqualTo("waiver-1");
    assertThat(rows.get(0).title()).isEqualTo("License Policy");
  }

  @Test
  public void malformedRow_missingId_isDropped() {
    SearchResultItemDTO good = new SearchResultItemDTO();
    good.applicationId = "app-1";
    good.applicationName = "App One";
    SearchResultItemDTO bad = new SearchResultItemDTO();
    // No applicationId/applicationName — must be dropped rather than fail the response.
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(good, bad), 2L, List.of()));

    List<SuggestRow> rows = underTest.suggest("alpha", List.of(SuggestItemType.APPLICATION), 5, principal);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).id()).isEqualTo("app-1");
  }

  @Test
  public void perTypeLimit_isThreadedAsPageSize_notTruncated() {
    when(searchIndexClient.searchGlobal(any())).thenReturn(emptyResult());

    underTest.suggest("alpha", List.of(SuggestItemType.APPLICATION), 7, principal);

    org.mockito.ArgumentCaptor<GlobalSearchRequest> captor =
        org.mockito.ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient).searchGlobal(captor.capture());
    assertThat(captor.getValue().pageSize()).isEqualTo(7);
  }

  @After
  public void unbindShiro() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  @Test
  public void multipleTypes_areFannedOutAndGroupedInRequestedOrder() {
    // Every per-type query gets the same DTO carrying fields for BOTH types; each type's mapper reads
    // only its own fields, so the merged result carries one row per requested type. Ordering must
    // follow the requested types order, not parallel completion order.
    SearchResultItemDTO doc = new SearchResultItemDTO();
    doc.applicationId = "app-1";
    doc.applicationName = "My App";
    doc.vulnerabilityId = "CVE-2024-9";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(doc), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest(
        "alpha",
        List.of(SuggestItemType.VULNERABILITY, SuggestItemType.APPLICATION),
        5,
        principal);

    assertThat(rows).extracting(SuggestRow::type)
        .containsExactly(SuggestItemType.VULNERABILITY, SuggestItemType.APPLICATION);
    assertThat(rows).extracting(SuggestRow::id).containsExactly("CVE-2024-9", "app-1");
  }

  @Test
  public void parallelFanOut_preservesShiroSubjectOnWorkerThreads_rbacFailClosed() {
    // Bind a real Shiro Subject on the calling thread. The permission filter (buildPermittedQuery)
    // runs inside searchGlobal on a worker thread; TenantAwareOneTimeRunnable must re-associate the
    // Subject there. If the Subject were lost, getCurrentUserContextIdsWithReadPermission would see a
    // null principal on the worker and fail-close to zero rows. We assert every worker observed the
    // scoped principal, and that scoped rows still come back.
    SecurityManager securityManager = new DefaultSecurityManager();
    ThreadContext.bind(securityManager);
    Subject scoped = new Subject.Builder(securityManager).principals(
        new org.apache.shiro.subject.SimplePrincipalCollection("scoped-user", "test")).buildSubject();
    ThreadContext.bind(scoped);

    Set<Object> principalsSeenOnWorkers = ConcurrentHashMap.newKeySet();
    when(searchIndexClient.searchGlobal(any())).thenAnswer(inv -> {
      Object principal = SecurityUtils.getSubject().getPrincipal();
      // null principal on a worker would mean the Subject was lost -> RBAC would fail-close to zero.
      principalsSeenOnWorkers.add(String.valueOf(principal));
      SearchResultItemDTO app = new SearchResultItemDTO();
      app.applicationId = "app-permitted";
      app.applicationName = "Permitted App";
      return new GlobalSearchResult(List.of(app), 1L, List.of());
    });

    List<SuggestRow> rows = underTest.suggest(
        "alpha",
        List.of(SuggestItemType.APPLICATION, SuggestItemType.VULNERABILITY),
        5,
        principal);

    // Every worker thread saw the scoped principal (never null), so the permission filter ran against
    // the real caller -> RBAC stays fail-closed under the parallel path.
    assertThat(principalsSeenOnWorkers).containsExactly("scoped-user");
    assertThat(rows).isNotEmpty();
  }

  @Test
  public void oneTypeFailing_degradesThatGroupOnly_doesNotFailWholeSuggest() {
    // Deterministic per-type failure: the VULNERABILITY leg throws, keyed on its compiled query rather
    // than call order, so completion-race between workers cannot pick the wrong leg. The vuln worker's
    // ExecutionException must degrade only that type to an empty group; APPLICATION still returns its
    // row and the whole suggest never throws.
    when(searchIndexClient.searchGlobal(any())).thenAnswer(inv -> {
      GlobalSearchRequest request = inv.getArgument(0);
      boolean isVulnLeg = request.baseQuery().toString().contains("itemType:security_vulnerability");
      if (isVulnLeg) {
        throw new RuntimeException("index boom");
      }
      SearchResultItemDTO app = new SearchResultItemDTO();
      app.applicationId = "app-1";
      app.applicationName = "My App";
      return new GlobalSearchResult(List.of(app), 1L, List.of());
    });

    List<SuggestRow> rows = underTest.suggest(
        "alpha",
        List.of(SuggestItemType.VULNERABILITY, SuggestItemType.APPLICATION),
        5,
        principal);

    // The failing VULNERABILITY type degraded to empty; the surviving APPLICATION type still returns.
    assertThat(rows).extracting(SuggestRow::id).containsExactly("app-1");
    assertThat(rows).extracting(SuggestRow::type).doesNotContain(SuggestItemType.VULNERABILITY);
  }

  @Test
  public void parallelFanOut_scopedPrincipal_excludesForbiddenRows_failClosedPreserved() {
    // D2 RBAC: prove the REAL permission filter (buildPermittedQuery -> buildAllowedContextIdsFilter ->
    // wrapWithPermissionFilter) runs on the fan-out worker thread, not a fake that we taught to filter.
    // The scoped principal has READ only on "org-allowed"; buildPermittedQuery wraps the base query with
    // a TermInSetQuery(allowedContextIds) FILTER for that context. searchGlobal here stands in for Lucene:
    // it returns the forbidden doc ONLY when that FILTER is absent from the query it received. So if a
    // regression weakens or bypasses the filter on the worker, the forbidden row leaks and this fails.
    // buildPermittedQuery (the composed entry point) is real (stubbed in setUp), so it drives the real
    // lookup -> build filter -> wrap -> substitute pipeline. The two low-level steps are interface
    // default methods that throw unless implemented, so we stand them in with the SAME Lucene shape the
    // production AbstractSearchIndexClient produces: a TermInSetQuery(allowedContextIds) restricted to
    // the caller's READ contexts, ANDed onto the base query as a FILTER clause.
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("org-allowed"));
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenAnswer(inv -> {
      Set<String> ctx = inv.getArgument(0);
      List<org.apache.lucene.util.BytesRef> terms = new java.util.ArrayList<>();
      for (String id : ctx) {
        terms.add(new org.apache.lucene.util.BytesRef(id));
      }
      return new org.apache.lucene.search.TermInSetQuery("allowedContextIds", terms);
    });
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> {
      org.apache.lucene.search.Query base = inv.getArgument(0);
      org.apache.lucene.search.Query filter = inv.getArgument(1);
      if (filter == null) {
        return base;
      }
      if (base == null) {
        return filter;
      }
      return new org.apache.lucene.search.BooleanQuery.Builder()
          .add(base, org.apache.lucene.search.BooleanClause.Occur.MUST)
          .add(filter, org.apache.lucene.search.BooleanClause.Occur.FILTER)
          .build();
    });

    when(searchIndexClient.searchGlobal(any())).thenAnswer(inv -> {
      GlobalSearchRequest request = inv.getArgument(0);
      // The real permission FILTER, when present, restricts to the allowed context. Absence of this term
      // in the query the worker built is exactly the bypass this test must catch.
      boolean permissionFilterApplied = request.baseQuery().toString().contains("allowedContextIds:(org-allowed)");

      SearchResultItemDTO permitted = new SearchResultItemDTO();
      permitted.applicationId = "app-allowed";
      permitted.applicationName = "Allowed App";

      SearchResultItemDTO forbidden = new SearchResultItemDTO();
      forbidden.applicationId = "app-forbidden";
      forbidden.applicationName = "Forbidden App";

      List<SearchResultItemDTO> docs = permissionFilterApplied
          ? List.of(permitted)
          : List.of(permitted, forbidden);
      return new GlobalSearchResult(docs, docs.size(), List.of());
    });

    List<SuggestRow> rows = underTest.suggest(
        "alpha",
        List.of(SuggestItemType.APPLICATION, SuggestItemType.VULNERABILITY),
        5,
        principal);

    // Fail-closed preserved across the parallel fan-out: only the permitted row comes back, never the
    // forbidden one.
    assertThat(rows).extracting(SuggestRow::id).contains("app-allowed");
    assertThat(rows).extracting(SuggestRow::id).doesNotContain("app-forbidden");
  }

  @Test
  public void perTypeTimeout_degradesThatTypeToEmpty_neverFailsWholeSuggest() {
    // D1 timeout-degrade: one type's query blocks past the per-type deadline. awaitType must time out,
    // cancel that future, degrade the type to an empty group, and still return the fast type's row. No
    // Thread.sleep / wall-clock reliance: the slow leg blocks on a latch we release only AFTER asserting
    // the timeout already degraded it, so the assertion never races the block.
    CountDownLatch releaseSlowLeg = new CountDownLatch(1);
    AtomicBoolean slowLegInterrupted = new AtomicBoolean(false);
    when(searchIndexClient.searchGlobal(any())).thenAnswer(inv -> {
      GlobalSearchRequest request = inv.getArgument(0);
      // VULNERABILITY maps to SECURITY_VULNERABILITY; its compiled query is the only one carrying that
      // item type, so we block exactly the vuln leg and let the application leg return immediately.
      boolean isVulnLeg = request.baseQuery().toString().contains("itemType:security_vulnerability");
      if (isVulnLeg) {
        try {
          // Block well past SUGGEST_FAN_OUT_TIMEOUT_MILLIS so awaitType times out first; cancel(true)
          // interrupts this await, which we surface so the test can prove the worker was cancelled.
          if (!releaseSlowLeg.await(30, TimeUnit.SECONDS)) {
            slowLegInterrupted.set(true);
          }
        }
        catch (InterruptedException e) {
          slowLegInterrupted.set(true);
          Thread.currentThread().interrupt();
        }
        SearchResultItemDTO vuln = new SearchResultItemDTO();
        vuln.vulnerabilityId = "CVE-2024-9";
        return new GlobalSearchResult(List.of(vuln), 1L, List.of());
      }
      SearchResultItemDTO app = new SearchResultItemDTO();
      app.applicationId = "app-1";
      app.applicationName = "My App";
      return new GlobalSearchResult(List.of(app), 1L, List.of());
    });

    List<SuggestRow> rows = underTest.suggest(
        "alpha",
        List.of(SuggestItemType.VULNERABILITY, SuggestItemType.APPLICATION),
        5,
        principal);

    // The slow vuln type degraded to empty; the fast application type still returned its row. The whole
    // suggest never threw.
    assertThat(rows).extracting(SuggestRow::id).containsExactly("app-1");
    assertThat(rows).extracting(SuggestRow::type).doesNotContain(SuggestItemType.VULNERABILITY);

    // The worker really was interrupted by cancel(true): the interrupt lands asynchronously on the
    // worker after suggest() returns, so poll for it. This would fail if cancel were dropped or weakened
    // to cancel(false), catching a regression the behavior assertions above cannot.
    await().atMost(Duration.ofSeconds(5)).untilTrue(slowLegInterrupted);

    // Now unblock the orphaned worker so it does not leak past the test.
    releaseSlowLeg.countDown();
  }

  @Test
  public void duplicateTypeInRequest_returnsThatTypesRowsExactlyOnce() {
    // The SPI does not promise a distinct type list. fanOut de-duplicates up front so a duplicated type
    // is queried once and its rows appear once in the merged output (never double-added).
    SearchResultItemDTO app = new SearchResultItemDTO();
    app.applicationId = "app-1";
    app.applicationName = "My App";
    when(searchIndexClient.searchGlobal(any()))
        .thenReturn(new GlobalSearchResult(List.of(app), 1L, List.of()));

    List<SuggestRow> rows = underTest.suggest(
        "alpha",
        List.of(SuggestItemType.APPLICATION, SuggestItemType.APPLICATION),
        5,
        principal);

    assertThat(rows).extracting(SuggestRow::id).containsExactly("app-1");
    verify(searchIndexClient, times(1)).searchGlobal(any());
  }

  private static GlobalSearchResult emptyResult() {
    return new GlobalSearchResult(List.of(), 0L, List.of());
  }
}
