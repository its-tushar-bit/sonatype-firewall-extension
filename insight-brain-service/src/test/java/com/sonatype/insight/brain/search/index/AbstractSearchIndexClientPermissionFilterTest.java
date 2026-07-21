/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import static com.sonatype.insight.brain.search.index.AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP;
import static com.sonatype.insight.brain.search.index.AbstractSearchIndexClient.capTotalHitsForGlobalSearch;
import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Asserts the new {@code TermInSetQuery} permission envelope matches an independently-modeled
 * ancestor-closure oracle on a 200-org fixture. The new envelope's correctness rests on the
 * invariant that every doc's denormalized {@code allowedContextIds} field contains its full
 * ancestor-org closure (plus its owning app id when applicable); given a user's
 * permitted-context-id set, intersecting that closure is equivalent to expanding each permitted
 * org id into its subtree and matching the doc's primary org id.
 *
 * <p>
 * This test does <b>not</b> invoke the legacy
 * {@code appendAllowedApplicationsAndOrganizationsToQuery} production code; that path is
 * exercised by the existing advanced-search integration tests. The oracle here is a hand-built
 * model of the ancestor-closure semantics we expect the new envelope to preserve, so the test
 * stays in-memory (no Spring / DI / database).
 */
public class AbstractSearchIndexClientPermissionFilterTest
{
  private static final int NUM_ORGS = 200;

  private static final int APPS_PER_LEAF_ORG = 1;

  private static final long FIXTURE_SEED = 0xC0FFEEL;

  private Directory directory;

  private DirectoryReader reader;

  /** Top-level orgs (parent = ROOT_ORGANIZATION_ID). */
  private final List<String> topLevelOrgIds = new ArrayList<>();

  /** All org ids in insertion order. */
  private final List<String> allOrgIds = new ArrayList<>();

  /** For each org id, the closure of its ancestor org ids including itself + ROOT. */
  private final Map<String, List<String>> ancestorClosureByOrgId = new HashMap<>();

  /** For each org id, its direct children. Allows oracle to walk-down. */
  private final Map<String, List<String>> directChildrenByOrgId = new HashMap<>();

  /** Index of {docId -> primary org id} for the oracle. */
  private final Map<Integer, String> orgIdByDocId = new HashMap<>();

  /** Index of {docId -> owning app id} for the oracle. {@code null} when no app. */
  private final Map<Integer, String> appIdByDocId = new HashMap<>();

  @Before
  public void buildFixture() throws IOException {
    directory = new ByteBuffersDirectory();
    Random rng = new Random(FIXTURE_SEED);

    for (int i = 0; i < 25; i++) {
      String topId = "top-" + i;
      topLevelOrgIds.add(topId);
      registerOrg(topId, Organization.ROOT_ORGANIZATION_ID);
    }
    while (allOrgIds.size() < NUM_ORGS) {
      String parent = allOrgIds.get(rng.nextInt(allOrgIds.size()));
      String child = "org-" + allOrgIds.size();
      registerOrg(child, parent);
    }

    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new KeywordAnalyzer()))) {
      int docId = 0;
      for (String orgId : allOrgIds) {
        addDoc(writer, docId++, orgId, /* appId */ null);
        if (isLeaf(orgId)) {
          for (int a = 0; a < APPS_PER_LEAF_ORG; a++) {
            addDoc(writer, docId++, orgId, "app-" + orgId + "-" + a);
          }
        }
      }
      writer.commit();
    }

    reader = DirectoryReader.open(directory);
  }

  @After
  public void tearDown() throws IOException {
    if (reader != null) {
      reader.close();
    }
    if (directory != null) {
      directory.close();
    }
  }

  @Test
  public void permissionFilter_globalContext_returnsAllDocs() {
    Set<String> permitted = Set.of(MembershipMapping.GLOBAL_CONTEXT_ID);
    Query filter = newClient().buildAllowedContextIdsFilterForTest(permitted);

    // A null filter means the caller has global access and the base query is returned unchanged.
    assertThat(filter).isNull();

    assertThat(capTotalHitsForGlobalSearch(50_000L)).isEqualTo(GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP);
    assertThat(capTotalHitsForGlobalSearch(123L)).isEqualTo(123L);
  }

  @Test
  public void permissionFilter_rootOrgPermission_returnsAllDocs() {
    Set<String> permitted = Set.of(Organization.ROOT_ORGANIZATION_ID);
    Query filter = newClient().buildAllowedContextIdsFilterForTest(permitted);

    assertThat(filter).isNull();
  }

  @Test
  public void permissionFilter_emptyPermissions_returnsZeroDocs() throws IOException {
    Set<String> permitted = Collections.emptySet();
    Query filter = newClient().buildAllowedContextIdsFilterForTest(permitted);

    assertThat(filter).isInstanceOf(MatchNoDocsQuery.class);
    assertThat(executeAndCollectOrgIds(filter)).isEmpty();
  }

  @Test
  public void permissionFilter_nullPermissions_returnsZeroDocs() {
    Query filter = newClient().buildAllowedContextIdsFilterForTest(null);

    assertThat(filter).isInstanceOf(MatchNoDocsQuery.class);
  }

  @Test
  public void permissionFilter_singleTopLevelOrg_matchesAncestorClosureOracle() throws IOException {
    String topOrg = topLevelOrgIds.get(0);
    Set<String> permitted = Set.of(topOrg);

    assertNewEnvelopeMatchesOracle(permitted);
  }

  @Test
  public void permissionFilter_singleDeepOrg_matchesAncestorClosureOracle() throws IOException {
    // Pick the last org registered — likely deep in the tree.
    String deepOrg = allOrgIds.get(allOrgIds.size() - 1);
    Set<String> permitted = Set.of(deepOrg);

    assertNewEnvelopeMatchesOracle(permitted);
  }

  @Test
  public void permissionFilter_singleApplicationPermission_matchesAncestorClosureOracle() throws IOException {
    String anyAppId = appIdByDocId.values().stream().filter(v -> v != null).findFirst().orElseThrow();
    Set<String> permitted = Set.of(anyAppId);

    assertNewEnvelopeMatchesOracle(permitted);
  }

  @Test
  public void permissionFilter_mixedOrgAndAppPermissions_matchesAncestorClosureOracle() throws IOException {
    Random rng = new Random(0x42L);
    Set<String> permitted = new LinkedHashSet<>();
    for (int i = 0; i < 5; i++) {
      permitted.add(allOrgIds.get(rng.nextInt(allOrgIds.size())));
    }
    List<String> apps = appIdByDocId.values().stream().filter(v -> v != null).distinct().toList();
    for (int i = 0; i < 3; i++) {
      permitted.add(apps.get(rng.nextInt(apps.size())));
    }

    assertNewEnvelopeMatchesOracle(permitted);
  }

  @Test
  public void permissionFilter_unrelatedPermission_returnsNothingButQueryStillRuns() throws IOException {
    Set<String> permitted = Set.of("ghost-org-not-in-index");
    Query filter = newClient().buildAllowedContextIdsFilterForTest(permitted);

    assertThat(filter).isInstanceOf(TermInSetQuery.class);
    assertThat(executeAndCollectOrgIds(filter)).isEmpty();
  }

  @Test
  public void permissionFilter_baseQueryWrapping_intersectsWithBaseQuery() throws IOException {
    String topOrg = topLevelOrgIds.get(0);
    Set<String> permitted = Set.of(topOrg);
    TestSearchIndexClient client = newClient();
    Query filter = client.buildAllowedContextIdsFilterForTest(permitted);
    Query baseQuery = new MatchAllDocsQuery();
    Query wrapped = client.wrapWithPermissionFilterForTest(baseQuery, filter);

    Set<String> wrappedHits = executeAndCollectOrgIds(wrapped);
    Set<String> filterOnlyHits = executeAndCollectOrgIds(filter);

    // baseQuery = MatchAll, so the wrapped query is equivalent to the filter alone.
    assertThat(wrappedHits).isEqualTo(filterOnlyHits).isNotEmpty();
  }

  @Test
  public void wrapWithPermissionFilter_nullFilter_returnsBaseQueryUnchanged() {
    Query base = new MatchAllDocsQuery();
    assertThat(newClient().wrapWithPermissionFilterForTest(base, null)).isSameAs(base);
  }

  @Test
  public void buildPermittedQuery_globalUserNullBase_substitutesMatchAllDocsNeverNull() {
    // Global access => null filter; a null base query would leave wrapWithPermissionFilter
    // returning null. buildPermittedQuery must substitute MatchAllDocsQuery instead, mirroring
    // the SearchIndexClient interface default's never-null contract.
    TestSearchIndexClient client = newClient();
    client.stubReadContextIds(Set.of(MembershipMapping.GLOBAL_CONTEXT_ID));

    Query permitted = client.buildPermittedQuery(null);

    assertThat(permitted).isInstanceOf(MatchAllDocsQuery.class);
  }

  @Test
  public void buildPermittedQuery_globalUserWithBase_returnsBaseUnchanged() {
    TestSearchIndexClient client = newClient();
    client.stubReadContextIds(Set.of(MembershipMapping.GLOBAL_CONTEXT_ID));
    Query base = new MatchAllDocsQuery();

    assertThat(client.buildPermittedQuery(base)).isSameAs(base);
  }

  /**
   * Guard for the fail-open concern: a user with an empty read-context set must get zero results,
   * NOT the MatchAllDocsQuery substitution. The empty set flows through buildAllowedContextIdsFilter
   * as a MatchNoDocsQuery (not null), so wrapWithPermissionFilter never yields null and the MatchAll
   * substitution is never reached. Locks that the empty-set path stays fail-closed.
   */
  @Test
  public void buildPermittedQuery_emptyReadContexts_returnsZeroDocs() throws IOException {
    TestSearchIndexClient client = newClient();
    client.stubReadContextIds(Collections.emptySet());

    Query permitted = client.buildPermittedQuery(new MatchAllDocsQuery());

    assertThat(permitted).isNotInstanceOf(MatchAllDocsQuery.class);
    assertThat(executeAndCollectOrgIds(permitted)).isEmpty();
  }

  @Test
  public void permissionFilter_isCaseSensitive_matchesRawContextIdOnly() throws IOException {
    // Context-id matching is case-sensitive (keyword field, no normalizer): a lowercased variant
    // must NOT match, else a broken filter is a silent security miss. Assert both directions.
    String mixedCaseId = "MixedCaseOrg-AbC123";
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new KeywordAnalyzer()))) {
        Document doc = new Document();
        doc.add(new StringField(FieldIdentifier.ALLOWED_CONTEXT_IDS.label, mixedCaseId, Field.Store.NO));
        doc.add(new StringField(FieldIdentifier.ORGANIZATION_ID.label, mixedCaseId, Field.Store.YES));
        writer.addDocument(doc);
      }
      try (DirectoryReader reader = DirectoryReader.open(dir)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        Query exact = newClient().buildAllowedContextIdsFilterForTest(Set.of(mixedCaseId));
        Query lowered = newClient()
            .buildAllowedContextIdsFilterForTest(Set.of(mixedCaseId.toLowerCase(java.util.Locale.ROOT)));
        assertThat(searcher.count(exact)).as("exact-case id matches").isEqualTo(1);
        assertThat(searcher.count(lowered)).as("lowercased id does NOT match (case-sensitive)").isEqualTo(0);
      }
    }
  }

  private void assertNewEnvelopeMatchesOracle(Set<String> permittedContextIds) throws IOException {
    Set<String> oracleOrgIds = ancestorClosureOracle(permittedContextIds);

    Query filter = newClient().buildAllowedContextIdsFilterForTest(permittedContextIds);
    Set<String> newPathOrgIds = executeAndCollectOrgIds(filter);

    assertThat(newPathOrgIds)
        .withFailMessage(
            "New TermsQuery envelope diverges from oracle. Permitted: %s Only in oracle: %s Only in new: %s",
            permittedContextIds, diff(oracleOrgIds, newPathOrgIds), diff(newPathOrgIds, oracleOrgIds))
        .isEqualTo(oracleOrgIds);
  }

  /**
   * Ancestor-closure oracle: expand each permitted org id to its full descendant subtree, union
   * with the permitted app ids. A doc matches if its primary org id is in the expanded set OR
   * its app id is in the permitted-app set. This is the ancestor-closure semantics the new
   * envelope must preserve; it is <b>not</b> a call into the legacy production code.
   */
  private Set<String> ancestorClosureOracle(Set<String> permittedContextIds) {
    if (permittedContextIds.contains(MembershipMapping.GLOBAL_CONTEXT_ID) ||
        permittedContextIds.contains(Organization.ROOT_ORGANIZATION_ID))
    {
      return new TreeSet<>(orgIdByDocId.values());
    }
    Set<String> expandedOrgs = new HashSet<>();
    Set<String> permittedApps = new HashSet<>();
    for (String contextId : permittedContextIds) {
      if (ancestorClosureByOrgId.containsKey(contextId)) {
        expandSubtree(contextId, expandedOrgs);
      }
      else {
        permittedApps.add(contextId);
      }
    }

    Set<String> matchedOrgIds = new TreeSet<>();
    for (Map.Entry<Integer, String> e : orgIdByDocId.entrySet()) {
      Integer docId = e.getKey();
      String orgId = e.getValue();
      String appId = appIdByDocId.get(docId);
      if (expandedOrgs.contains(orgId) || (appId != null && permittedApps.contains(appId))) {
        matchedOrgIds.add(orgId);
      }
    }
    return matchedOrgIds;
  }

  private void expandSubtree(String rootId, Set<String> out) {
    out.add(rootId);
    for (String child : directChildrenByOrgId.getOrDefault(rootId, Collections.emptyList())) {
      expandSubtree(child, out);
    }
  }

  private Set<String> executeAndCollectOrgIds(Query query) throws IOException {
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs topDocs = searcher.search(query, Math.max(1, reader.maxDoc()));
    Set<String> orgIds = new TreeSet<>();
    for (ScoreDoc sd : topDocs.scoreDocs) {
      Document doc = searcher.storedFields().document(sd.doc);
      String orgId = doc.get("primary_org_id");
      if (orgId != null) {
        orgIds.add(orgId);
      }
    }
    return orgIds;
  }

  private static Set<String> diff(Set<String> a, Set<String> b) {
    Set<String> result = new TreeSet<>(a);
    result.removeAll(b);
    return result;
  }

  private void registerOrg(String orgId, String parentOrgId) {
    allOrgIds.add(orgId);
    directChildrenByOrgId.computeIfAbsent(parentOrgId, k -> new ArrayList<>()).add(orgId);
    List<String> closure = new ArrayList<>();
    closure.add(orgId);
    // Root sentinel is intentionally omitted from the indexed closure so DocumentBuilderHelper
    // does not emit a wasted term per doc; callers with READ on root bypass permission
    // filtering via the null-filter path in buildAllowedContextIdsLuceneFilter.
    if (!Organization.ROOT_ORGANIZATION_ID.equals(parentOrgId)) {
      closure.addAll(ancestorClosureByOrgId.get(parentOrgId));
    }
    ancestorClosureByOrgId.put(orgId, closure);
  }

  private boolean isLeaf(String orgId) {
    return !directChildrenByOrgId.containsKey(orgId);
  }

  private void addDoc(IndexWriter writer, int docId, String primaryOrgId, String appId) throws IOException {
    Document doc = new Document();
    doc.add(new StoredField("doc_id", docId));
    doc.add(new StringField("primary_org_id", primaryOrgId, Field.Store.YES));
    if (appId != null) {
      doc.add(new StringField("primary_app_id", appId, Field.Store.YES));
    }
    List<String> closure = new ArrayList<>(ancestorClosureByOrgId.get(primaryOrgId));
    if (appId != null) {
      closure.add(appId);
    }
    for (String id : closure) {
      doc.add(new StringField(FieldIdentifier.ALLOWED_CONTEXT_IDS.label, id, Field.Store.NO));
    }
    writer.addDocument(doc);
    orgIdByDocId.put(docId, primaryOrgId);
    appIdByDocId.put(docId, appId);
  }

  private static TestSearchIndexClient newClient() {
    return new TestSearchIndexClient();
  }

  /**
   * Minimal AbstractSearchIndexClient subclass that surfaces the protected new-path helpers for
   * testing. Pure pass-through: no DAOs touched, no DI configured. Extending here (rather than a
   * stateless helper class) is what forces the 22-arg superclass ctor below.
   */
  private static final class TestSearchIndexClient
      extends AbstractSearchIndexClient
  {
    private Set<String> stubbedReadContextIds;

    void stubReadContextIds(Set<String> ids) {
      this.stubbedReadContextIds = ids;
    }

    @Override
    public Set<String> getCurrentUserContextIdsWithReadPermission() {
      return stubbedReadContextIds;
    }

    private TestSearchIndexClient() {
      // AbstractSearchIndexClient ctor params (order):
      // applicationDAO, labelDAO, organizationDAO, ownerDAO, policyDAO, policyWaiverDAO,
      // autoPolicyWaiverDAO, searchIndexChangeDAO, tagDAO, thirdPartySbomMetadataDAO,
      // documentBuilderHelper, productLicense, telemetrySender, luceneComponents,
      // advancedSearchTelemetryMetrics, configuration, permissionService, authorizationChecker,
      // currentUser, conversionHelper, shutdownHandler, readableContextAuthzCache.
      // None are exercised by buildAllowedContextIdsLuceneFilter / wrapWithPermissionFilter.
      super(null, null, null, null, null, null, null, null, null, null, null, null, null, null,
          null, null, null, null, null, null, null, null);
    }

    Query buildAllowedContextIdsFilterForTest(Set<String> permittedContextIds) {
      return buildAllowedContextIdsLuceneFilter(permittedContextIds);
    }

    Query wrapWithPermissionFilterForTest(Query baseQuery, Query permissionFilter) {
      return wrapWithPermissionFilter(baseQuery, permissionFilter);
    }

    @Override
    protected void updateMaxQueryClauseCount() {
    }

    @Override
    protected void updateIndex(SearchIndexChange change, IndexingContext indexingContext) {
    }

    @Override
    public SearchResultDTO searchIndex(
        String q,
        int pageSize,
        int page,
        boolean allComponents,
        boolean isSbomManagerMode,
        List<String> searchAfter)
    {
      return null;
    }

    @Override
    public void populateIndex() {
    }

    @Override
    public void updateIndex(List<SearchIndexChange> changes, Consumer<SearchIndexChange> cb) {
    }

    @Override
    public void updateIndex() {
    }

    @Override
    public Long getLastIndexTime() {
      return null;
    }

    @Override
    public long getIndexSize() {
      return 0;
    }

    @Override
    protected boolean isChangeSpecificError(Exception e) {
      return false;
    }

    @Override
    protected boolean isSystemicError(Exception e) {
      return false;
    }

    @Override
    public long countDistinct(String metricQuery, List<String> compositeKeyFields) {
      return 0;
    }

    @Override
    public long count(String metricQuery) {
      return 0;
    }

    @Override
    public MetricAggregationResult aggregateCountByField(
        String metricQuery,
        String bucketField,
        Map<String, int[]> ranges)
    {
      return null;
    }
  }
}
