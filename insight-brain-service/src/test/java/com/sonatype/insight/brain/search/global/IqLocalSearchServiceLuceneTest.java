/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalSearchResponse;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.SearchInputs;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.indexquery.IndexQueryFilterCompiler;
import com.sonatype.insight.brain.search.indexquery.IndexQueryType;
import com.sonatype.insight.brain.search.lucene.DocumentBuilder;
import com.sonatype.insight.brain.search.lucene.LowerCaseKeywordAnalyzer;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.sonatype.insight.brain.search.global.fieldmap.FieldMap;
import com.sonatype.insight.brain.search.lucene.LuceneIndexingContext;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.NumericUtils;
import org.mockito.Mockito;

/**
 * Lucene-backed unit test for the IQ-local query path: stands up a real in-memory Lucene index,
 * indexes a tiny fixture (handful of orgs/apps + several components and a vulnerability),
 * executes the query that {@link IqLocalSearchService} would compose, and verifies hit-set
 * correctness for each tab type. Permission lookup is mocked through the
 * {@link SearchIndexClient} interface — this test exercises query construction and matching
 * against real Lucene, but does not stand up the full DB-backed {@code LuceneSearchIndexClient}
 * (whose constructor takes ~18 collaborators) and needs no external infrastructure, so it runs as
 * a plain unit test rather than a DB integration test.
 */
@RunWith(MockitoJUnitRunner.class)
public class IqLocalSearchServiceLuceneTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  private static final long DAY_MS = 24L * 60 * 60 * 1000;

  // Single clock reference for the whole test: both the seeded first-seen timestamps and the
  // first-seen window boundaries (start = NOW_MS - windowDays) derive from this one value, so no
  // wall-clock is read at query time. Do not compose window queries via firstSeenWindowChip (which
  // reads Instant.now()) in these tests or the two sides could skew on a slow run.
  private static final long NOW_MS = System.currentTimeMillis();

  private Directory directory;

  private IndexReader reader;

  private IndexSearcher searcher;

  private IqLocalSearchService service;

  @Before
  public void setUp() throws Exception {
    directory = new ByteBuffersDirectory();
    // Match production analyzer wiring: LowerCaseKeywordAnalyzer for non-description fields,
    // StandardAnalyzer for description fields. This mirrors LuceneComponents.newAnalyzerForSearch()
    // so the query tokens emitted by QueryCompiler match what the test fixture indexes.
    Map<String, Analyzer> perField = new HashMap<>();
    perField.put(FieldIdentifier.VULNERABILITY_DESCRIPTION.label, new StandardAnalyzer());
    PerFieldAnalyzerWrapper analyzerWrapper =
        new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), perField);
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzerWrapper))) {
      writer.addDocument(appDoc("acme-prod", "Acme Prod", "Acme"));
      writer.addDocument(appDoc("acme-dev", "Acme Dev", "Acme"));
      writer.addDocument(appDoc("widget-co", "Widget Inventory", "Widget Co"));
      // A child-org app: its own org is "Acme West", but its ancestor chain (parentOrganizationName)
      // includes the parent org "Acme". Exercises v2 subtree matching parity with v1 classic search.
      writer.addDocument(childOrgAppDoc("acme-west", "Acme West App", "Acme West", "Acme"));
      writer.addDocument(orgDoc("Acme"));
      writer.addDocument(orgDoc("Widget Co"));
      writer.addDocument(componentDoc("log4j-core"));
      writer.addDocument(componentDoc("log4j-api"));
      writer.addDocument(componentDoc("commons-lang"));
      writer.addDocument(componentDoc("guava"));
      writer.addDocument(componentDoc("jackson-databind"));
      // Maven components carrying real coordinate fields, so a pasted purl can be decomposed and
      // matched against componentCoordinateGroupId/ArtifactId/Version rather than a whole-string
      // free-text term (which matches no indexed field). aopalliance is the reviewer's example.
      writer.addDocument(mavenComponentDoc("aopalliance", "aopalliance", "1.0"));
      writer.addDocument(mavenComponentDoc("aopalliance", "aopalliance", "2.0"));
      writer.addDocument(mavenComponentDoc("org.springframework", "spring-core", "6.1.0"));
      writer.addDocument(vulnDoc("CVE-2021-44228", "log4j-core", "Remote code execution in Log4j2"));
      writer.addDocument(vulnDoc("CVE-2017-7525", "jackson-databind", "Deserialization gadget chain"));
      // First-seen fixtures for the local "first seen (within ...)" window. LongPoint mirrors the
      // production DocumentBuilder.setVulnerabilityFirstSeenEpochMs; CVE-NONE omits the field (a
      // non-violating vuln). MULTI carries the MIN open time (~200d) as the resolved first-seen.
      writer.addDocument(vulnDocWithFirstSeen("CVE-RECENT", "firstseen-fixture", NOW_MS - 10L * DAY_MS));
      writer.addDocument(vulnDocWithFirstSeen("CVE-OLD", "firstseen-fixture", NOW_MS - 200L * DAY_MS));
      writer.addDocument(vulnDocWithFirstSeen("CVE-MULTI", "firstseen-fixture", NOW_MS - 200L * DAY_MS));
      writer.addDocument(vulnDoc("CVE-NONE", "firstseen-fixture", "informational, no policy violation"));
      // Component max-policy-threat-level fixtures for the Components tab policyThreatLevel range
      // filter (componentMaxPolicyThreatLevel:[lo TO hi]). IntPoint mirrors production
      // DocumentBuilder.setComponentMaxPolicyThreatLevel. The IN-RANGE doc must be returned and the
      // BELOW-RANGE doc excluded; without a PointsConfig for this field the Lucene backend cannot
      // parse the IntPoint range and silently returns zero.
      writer.addDocument(componentDocWithMaxPolicyThreatLevel("threat-fixture-high", 9));
      writer.addDocument(componentDocWithMaxPolicyThreatLevel("threat-fixture-low", 3));
      writer.commit();
    }
    reader = DirectoryReader.open(directory);
    searcher = new IndexSearcher(reader);
    service = new IqLocalSearchService(searchIndexClient,
        FieldMap.defaultMap());

    when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of());
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    // Wire the mock SearchIndexClient to actually run the captured Query against our real
    // in-memory Lucene index — so the test exercises both QueryCompiler's output and
    // the index's matching behaviour, not just mock interactions.
    when(searchIndexClient.searchGlobal(any())).thenAnswer(inv -> runRealSearch(inv.getArgument(0)));
  }

  @After
  public void tearDown() throws Exception {
    if (reader != null) {
      reader.close();
    }
    if (directory != null) {
      directory.close();
    }
  }

  @Test
  public void applicationsTab_log4jQuery_matchesNothing() {
    SearchInputs inputs = new SearchInputs("log4j", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).isEmpty();
  }

  @Test
  public void applicationsTab_acmeQuery_returnsAcmeApps_inDeterministicOrder() {
    SearchInputs inputs = new SearchInputs("acme", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    // Bare "acme" prefix-matches the organizationName token of the two Acme apps and the
    // "acme west" child-org app (its own org name starts with "acme").
    assertThat(response.rows()).extracting(r -> r.row().applicationPublicId)
        .containsExactlyInAnyOrder("acme-prod", "acme-dev", "acme-west");
  }

  @Test
  public void applicationsTab_organizationNameFilter_matchesDescendantOrgApp() {
    // v2 subtree parity: organizationName now targets parentOrganizationName, so a fielded query
    // for the parent org "Acme" matches the app that lives in the child org "Acme West" (whose
    // ancestor chain includes "Acme"), not just apps directly in "Acme".
    SearchInputs inputs = new SearchInputs("organizationName:\"Acme\"", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).extracting(r -> r.row().applicationPublicId)
        .contains("acme-west");
  }

  // The ORGANIZATION tab was removed as a user-facing surface — orgs are queryable via the
  // organizationName: filter chip on other tabs, not as a top-level tab. The underlying index
  // still carries ORGANIZATION docs; direct ItemType queries against them are exercised via
  // filter-chip syntax in QueryCompilerTest.

  @Test
  public void componentsTab_log4jQuery_returnsBothLog4jComponents() {
    SearchInputs inputs = new SearchInputs("log4j", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    // log4j-core and log4j-api both share the keyword-analyzed lowercase prefix "log4j".
    assertThat(response.rows()).hasSize(2);
  }

  @Test
  public void vulnerabilitiesTab_log4jCoreQuery_matchesViaComponentName() {
    SearchInputs inputs = new SearchInputs("log4j-core", Tab.VULNERABILITY,
        Set.of(ItemType.SECURITY_VULNERABILITY), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    // LowerCaseKeywordAnalyzer treats "log4j-core" as one keyword token; only the
    // vulnerability whose componentName field stores the same keyword should match.
    assertThat(response.rows()).hasSize(1);
  }

  @Test
  public void vulnerabilitiesTab_bareTermOnlyInDescription_doesNotMatch() {
    // "remote" appears only in the TEXT vulnerabilityDescription, which is intentionally excluded
    // from the bare-term defaults (bare terms search KEYWORD fields only). A user must use the
    // fielded form (vulnerabilityDescription:remote) to search the description.
    SearchInputs inputs = new SearchInputs("remote", Tab.VULNERABILITY,
        Set.of(ItemType.SECURITY_VULNERABILITY), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).isEmpty();
  }

  @Test
  public void vulnerabilitiesTab_fieldedDescriptionQuery_matchesViaStandardAnalyzedField() {
    // The fielded form still searches the StandardAnalyzer-tokenized description.
    SearchInputs inputs = new SearchInputs("vulnerabilityDescription:remote", Tab.VULNERABILITY,
        Set.of(ItemType.SECURITY_VULNERABILITY), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).hasSize(1);
  }

  @Test
  public void vulnerabilitiesTab_firstSeenWindow30d_narrowsToRecentAndExcludesNoFirstSeen() {
    // The local window filter compiles to vulnerabilityFirstSeenEpochMs:[now-30d TO *]. On the real
    // Lucene index this must return only the vuln first-seen 10 days ago; the 200-day-old vulns and
    // the non-violating vuln with no first-seen field are excluded. This is the Lucene leg of the
    // dual-backend guard (the recurring bug is a Lucene-only zero/mismatch).
    long start = NOW_MS - 30L * DAY_MS;
    SearchInputs inputs = new SearchInputs(
        "vulnerabilityFirstSeenEpochMs:[" + start + " TO *]", Tab.VULNERABILITY,
        Set.of(ItemType.SECURITY_VULNERABILITY), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).extracting(r -> r.row().vulnerabilityId)
        .containsExactlyInAnyOrder("CVE-RECENT");
  }

  @Test
  public void vulnerabilitiesTab_firstSeenWindow1y_includesOldAndMinButNotNoFirstSeen() {
    long start = NOW_MS - 365L * DAY_MS;
    SearchInputs inputs = new SearchInputs(
        "vulnerabilityFirstSeenEpochMs:[" + start + " TO *]", Tab.VULNERABILITY,
        Set.of(ItemType.SECURITY_VULNERABILITY), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    // v_recent (10d), v_old (200d), v_multi (MIN 200d) qualify; the non-violating CVE-NONE does not.
    assertThat(response.rows()).extracting(r -> r.row().vulnerabilityId)
        .containsExactlyInAnyOrder("CVE-RECENT", "CVE-OLD", "CVE-MULTI");
  }

  @Test
  public void vulnerabilitiesTab_firstSeenStoredValue_readBackOnRow() {
    // The stored first-seen epoch-ms round-trips onto the row (SearchResultItemDTO parse) so the
    // catalog mapper can emit it; a LongPoint-only field would read back null.
    long start = NOW_MS - 30L * DAY_MS;
    SearchInputs inputs = new SearchInputs(
        "vulnerabilityFirstSeenEpochMs:[" + start + " TO *]", Tab.VULNERABILITY,
        Set.of(ItemType.SECURITY_VULNERABILITY), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).hasSize(1);
    assertThat(response.rows().get(0).row().vulnerabilityFirstSeenEpochMs).isEqualTo(NOW_MS - 10L * DAY_MS);
  }

  @Test
  public void componentsTab_naturalPurlWithoutDefaultQualifier_retrievesExactComponent() {
    // The reviewer's case: a pasted natural purl (no ?type=jar) must retrieve exactly the matching
    // aopalliance@1.0 component out of several components, so the row reaches the best-match
    // candidate list. The generic parser would treat pkg:... as an unknown field and fail open to
    // match-all; the coordinate path narrows to the exact groupId+artifactId+version.
    SearchInputs inputs = new SearchInputs("pkg:maven/aopalliance/aopalliance@1.0", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.total()).isEqualTo(1L);
    assertThat(response.rows()).hasSize(1);
    assertThat(response.rows().get(0).row().componentName).isEqualTo("aopalliance : aopalliance : 1.0");
    // A purl on a component-bearing type is the happy path — no non-component warning.
    assertThat(response.warnings())
        .doesNotContain(IqLocalSearchService.COORDINATE_ON_NON_COMPONENT_WARNING);
  }

  @Test
  public void componentsTab_purlWithDefaultQualifier_retrievesExactComponent() {
    // The exact canonical purl (with ?type=jar) retrieves the same single component — qualifiers are
    // ignored by the coordinate decomposition, so both the natural and canonical forms match.
    SearchInputs inputs = new SearchInputs("pkg:maven/aopalliance/aopalliance@1.0?type=jar", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.total()).isEqualTo(1L);
    assertThat(response.rows().get(0).row().componentName).isEqualTo("aopalliance : aopalliance : 1.0");
  }

  @Test
  public void componentsTab_purlForDifferentVersion_retrievesOnlyThatVersion() {
    SearchInputs inputs = new SearchInputs("pkg:maven/aopalliance/aopalliance@2.0", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.total()).isEqualTo(1L);
    assertThat(response.rows().get(0).row().componentName).isEqualTo("aopalliance : aopalliance : 2.0");
  }

  @Test
  public void applicationsTab_purlQuery_matchesNoApplication() {
    // A coordinate query yields match-nothing for non-component types — a purl is not an app — but
    // the empty section must be non-silent: a user-facing warning explains why and points at the
    // Components tab (the warnings set flows through to the results endpoint's SectionResult).
    SearchInputs inputs = new SearchInputs("pkg:maven/aopalliance/aopalliance@1.0", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).isEmpty();
    assertThat(response.warnings()).containsOnlyOnce(IqLocalSearchService.COORDINATE_ON_NON_COMPONENT_WARNING);
  }

  @Test
  public void componentsTab_queryThatMatchesNothing_returnsEmpty() {
    SearchInputs inputs = new SearchInputs("nonexistent-zzz-component", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).isEmpty();
    assertThat(response.total()).isEqualTo(0L);
  }

  @Test
  public void componentsTab_emptyQuery_matchesAllComponentsViaPermissionFilter() {
    SearchInputs inputs = new SearchInputs("", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    // 10 components in the fixture, all NON_VULNERABLE_COMPONENT.
    assertThat(response.total()).isEqualTo(10L);
  }

  @Test
  public void componentsTab_policyThreatLevelRange_returnsInRangeComponent() {
    // The Components tab policyThreatLevel range chip compiles to
    // componentMaxPolicyThreatLevel:[8 TO 10]. On the real Lucene index this must return the
    // component whose max policy threat level is 9. Without a PointsConfig entry for this field the
    // range fragment cannot be parsed as an IntPoint range and the Lucene backend silently returns
    // zero (the recurring dual-backend, Lucene-only defect this guards against).
    SearchInputs inputs = new SearchInputs(
        "componentMaxPolicyThreatLevel:[8 TO 10]", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).extracting(r -> r.row().componentName)
        .containsExactly("threat-fixture-high");
  }

  @Test
  public void componentsTab_policyThreatLevelRange_excludesBelowRangeComponent() {
    // Negative boundary: the below-range component (max threat 3) must NOT match [8 TO 10]; only
    // the in-range component (9) is returned. Proves the IntPoint range bound is honoured rather
    // than the query failing open to match-all.
    SearchInputs inputs = new SearchInputs(
        "componentMaxPolicyThreatLevel:[8 TO 10]", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.rows()).extracting(r -> r.row().componentName)
        .doesNotContain("threat-fixture-low");
    assertThat(response.total()).isEqualTo(1L);
  }

  @Test
  public void componentsTab_itemTypeComponentFilter_matchesComponentDocs() {
    // The user-facing itemType:COMPONENT token must resolve to the index discriminator
    // non_vulnerable_component and match every component document (10 in the fixture) with no
    // spurious warning — proving the alias mapping and the dropped enum-value gate work end to end.
    SearchInputs inputs = new SearchInputs("itemType:COMPONENT", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.warnings()).isEmpty();
    assertThat(response.total()).isEqualTo(10L);
    // Stored value is the original (unanalyzed) ItemType name; matching happens on the
    // lowercased analyzed token non_vulnerable_component.
    assertThat(response.rows())
        .allMatch(r -> ItemType.NON_VULNERABLE_COMPONENT.name().equals(r.row().itemType));
  }

  @Test
  public void unknownFieldName_emitsWarning_matchesAllOfType() {
    // AST parser recognises `name:log4j-core` as a field-scoped predicate. "name" is not in
    // the FieldMap vocabulary (the real field is "componentName"), so the compiler emits an
    // "Unknown filter" warning and translates that clause to MatchAllDocs — fail-open per the
    // frontend spec's X-Search-Warnings contract. The type filter still narrows to components.
    SearchInputs inputs = new SearchInputs("name:log4j-core", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    // At least one warning is emitted, and it mentions the unknown filter name.
    assertThat(response.warnings()).anyMatch(w -> w.contains("name"));
  }

  @Test
  public void luceneMetacharactersInQuery_doNotCrash_returnSafely() {
    SearchInputs inputs = new SearchInputs("+ - && || ! ( ) { } [ ] ^ \" ~ * ? : \\ /",
        Tab.COMPONENT, Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    // No exception is thrown. The query may match nothing or match by tokenized punctuation —
    // the contract is "doesn't crash, doesn't escape to QueryParser semantics".
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response).isNotNull();
  }

  @Test
  public void search_appliesSortKey_byApplicationName_ascendingCaseInsensitive() {
    SearchInputs inputs = new SearchInputs("acme", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "name", null);
    IqLocalSearchResponse response = service.search(inputs);
    // Field sort is on: the name sort runs a real STRING sort on the lower-cased applicationName
    // doc-values (the fixture indexes them, matching production). Ascending by display name:
    // "Acme Dev" < "Acme Prod" < "Acme West App".
    assertThat(response.sortKey()).isEqualTo("name");
    assertThat(response.rows()).extracting(r -> r.row().applicationPublicId)
        .containsExactly("acme-dev", "acme-prod", "acme-west");
  }

  @Test
  public void search_appliesNumericThreatSort_descending_notLexicographic() throws Exception {
    // Stand up a tiny violation fixture with threat levels 2 and 10. A lexicographic sort would
    // order "10" before "2"; the numeric SortedNumericDocValues sort must place 10 (higher threat)
    // first under threat-desc.
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        writer.addDocument(violationDoc("v-threat-2", 2));
        writer.addDocument(violationDoc("v-threat-10", 10));
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        IndexSearcher localSearcher = new IndexSearcher(localReader);
        Sort byThreat = IqLocalSearchService.sortFor(Tab.VIOLATION, "threat");
        Query allViolations = new TermQuery(
            new Term(FieldIdentifier.ITEM_TYPE.label,
                ItemType.POLICY_VIOLATION.name().toLowerCase()));
        TopDocs top = localSearcher.search(allViolations, 10, byThreat);
        List<String> order = new ArrayList<>();
        for (ScoreDoc sd : top.scoreDocs) {
          order.add(localSearcher.storedFields()
              .document(sd.doc)
              .get(FieldIdentifier.POLICY_VIOLATION_ID.label));
        }
        assertThat(order).containsExactly("v-threat-10", "v-threat-2");
      }
    }
  }

  @Test
  public void violationThreatSort_overMixedPolicyAndLegalViolations_ordersByThreat_withLegalLast() throws Exception {
    // Regression guard for the VIOLATION tab's DEFAULT sort against a production-shaped index.
    // Two things make this fail without the INT-width sort: the docs carry the 4-byte IntPoint that
    // DocumentBuilder writes (Lucene validates a numeric sort's byte width against the points index,
    // so an 8-byte LONG sort throws "indexed with 4 bytes per dimension ... expected 8" as soon as a
    // segment holds a value), and the result set mixes POLICY_VIOLATION with LEGAL_VIOLATION docs that
    // carry no policyViolationThreatLevel at all and must degrade to last rather than break the sort.
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        writer.addDocument(violationDoc("pv-4", 4));
        writer.addDocument(legalViolationDoc("lv-a", 5));
        writer.addDocument(violationDoc("pv-10", 10));
        writer.addDocument(violationDoc("pv-0", 0));
        writer.commit();
        // Second segment: the comparator is built per-leaf, so a multi-segment index proves the
        // width check passes on every leaf and the cross-segment merge still orders correctly.
        writer.addDocument(violationDoc("pv-9", 9));
        writer.addDocument(legalViolationDoc("lv-b", 7));
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        IndexSearcher localSearcher = new IndexSearcher(localReader);
        Sort byThreat = IqLocalSearchService.sortFor(Tab.VIOLATION, "threat");
        // Match BOTH item types the VIOLATION tab spans, so the sort sees the real mixed shape.
        Query allViolations = new BooleanQuery.Builder()
            .add(new TermQuery(new Term(
                FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_VIOLATION.name().toLowerCase())),
                BooleanClause.Occur.SHOULD)
            .add(new TermQuery(new Term(
                FieldIdentifier.ITEM_TYPE.label, ItemType.LEGAL_VIOLATION.name().toLowerCase())),
                BooleanClause.Occur.SHOULD)
            .build();

        TopDocs top = localSearcher.search(allViolations, 10, byThreat);

        List<String> order = new ArrayList<>();
        for (ScoreDoc sd : top.scoreDocs) {
          order.add(localSearcher.storedFields()
              .document(sd.doc)
              .get(FieldIdentifier.POLICY_VIOLATION_ID.label));
        }
        // Policy violations highest-threat first; the two legal violations (no threat field) trail.
        assertThat(order.subList(0, 4)).containsExactly("pv-10", "pv-9", "pv-4", "pv-0");
        assertThat(order.subList(4, 6)).containsExactlyInAnyOrder("lv-a", "lv-b");
      }
    }
  }

  @Test
  public void everyAllowlistedNumericSort_reportsAnEncodableNumericTypeNotCustom() {
    // A SortedNumericSortField reports getType() == CUSTOM (it comparator-wraps the numeric type), so
    // a cursor codec keyed off getType() rejects every numeric sort with "Unsupported SortField.Type in
    // searchAfter: CUSTOM" and page 2 of a threat-sorted list 400s. The tuple slots must be keyed off
    // getNumericType() instead. This asserts the type each sort reports; the encode/decode round-trip
    // through that type is exercised in LuceneSearchIndexClientSearchAfterEncodeTest.
    for (Tab tab : Tab.values()) {
      for (String sortKey : GlobalSearchSortAllowlist.allowedFor(tab)) {
        Sort sort = IqLocalSearchService.sortFor(tab, sortKey);
        if (sort == null) {
          continue;
        }
        for (SortField sf : sort.getSort()) {
          if (sf instanceof SortedNumericSortField numeric) {
            assertThat(numeric.getType())
                .as("SortedNumericSortField.getType() is CUSTOM, so a cursor codec must not key off it")
                .isEqualTo(SortField.Type.CUSTOM);
            assertThat(numeric.getNumericType())
                .as("encodable numeric type for '%s' on tab %s", sortKey, tab)
                .isIn(SortField.Type.LONG, SortField.Type.INT, SortField.Type.FLOAT, SortField.Type.DOUBLE);
          }
        }
      }
    }
  }

  /**
   * Drift guard for every allowlisted numeric sort: writes a single doc whose numeric field carries
   * the point field at the width {@code DocumentBuilder} uses plus the doc-values twin
   * {@code LuceneIndexingContext} adds, then runs the {@link Sort} {@link IqLocalSearchService}
   * builds for that (tab, sortKey). Lucene's numeric comparator reads the same-named points index to
   * build a competitive iterator and throws when the sort's comparator width disagrees with the
   * point field's width, so this fails for ANY numeric sort key that drifts out of width agreement
   * — not only the fields enumerated in the targeted tests. A value must be present, since an absent
   * field yields no {@code PointValues} and the mismatch would stay invisible.
   *
   * <p>
   * The point width per field is read out of a {@code DocumentBuilder}-built document
   * ({@link #pointClassesByLabel()}), so the write side is the single source of truth: no label list
   * here restates which fields are {@code IntPoint} vs {@code LongPoint}.
   */
  @Test
  public void everyAllowlistedNumericSort_executesAgainstAProductionShapedIndex() throws Exception {
    Map<String, Class<?>> pointClasses = pointClassesByLabel();
    for (Tab tab : Tab.values()) {
      for (String sortKey : GlobalSearchSortAllowlist.allowedFor(tab)) {
        FieldIdentifier f = IqLocalSearchService.sortableIndexFieldFor(tab, sortKey);
        if (f == null) {
          continue;
        }
        Sort sort = IqLocalSearchService.sortFor(tab, sortKey);
        if (sort == null || !(sort.getSort()[0] instanceof SortedNumericSortField)) {
          continue;
        }
        Class<?> pointClass = pointClasses.get(f.label);
        assertThat(pointClass)
            .as("numeric sort '%s' on tab %s sorts %s, but DocumentBuilder writes no point field for it, "
                + "so the sort's comparator width is unverifiable", sortKey, tab, f.label)
            .isNotNull();
        assertNumericSortExecutes(tab, sortKey, f, sort, pointClass);
      }
    }
  }

  /**
   * Every point field {@code DocumentBuilder} writes, mapped to its point class, by building one
   * document with every numeric setter populated. Derives "which fields are {@code IntPoint}" from the
   * write side, so a new {@code IntPoint} field that is sortable but missing from the production INT
   * allowlist fails {@link #everyAllowlistedNumericSort_executesAgainstAProductionShapedIndex} on the
   * points byte-width check rather than slipping past a stale label list.
   */
  private static Map<String, Class<?>> pointClassesByLabel() {
    Document written = new DocumentBuilder(ItemType.POLICY_VIOLATION)
        .setPolicyThreatLevel(5)
        .setPolicyViolationThreatLevel(5)
        .setComponentLicenseThreatLevel(5)
        .setPolicyWaiverThreatLevel(5)
        .setComponentMaxPolicyThreatLevel(5)
        .setApplicationMaxPolicyThreatLevel(5)
        .setApplicationViolationStateSortOrdinal(0)
        .setVulnerabilitySeverity(5.5f)
        .setPolicyWaiverCreatedAtEpochMs(5000L)
        .setPolicyWaiverExpiresAtEpochMs(5000L)
        .setApplicationLastEvaluationTimeEpochMs(5000L)
        .setVulnerabilityFirstSeenEpochMs(5000L)
        .build();
    Map<String, Class<?>> byLabel = new HashMap<>();
    for (IndexableField field : written.getFields()) {
      if (field instanceof IntPoint || field instanceof LongPoint
          || field instanceof FloatPoint)
      {
        byLabel.put(field.name(), field.getClass());
      }
    }
    // Guards the setter list above: a newly added numeric point field left uncalled here would make the
    // width check silently skip it, so pin the count of point fields this document is expected to carry.
    assertThat(byLabel)
        .as("DocumentBuilder point fields covered by this document; add the new numeric setter above "
            + "when DocumentBuilder gains an IntPoint/LongPoint/FloatPoint field")
        .hasSize(12);
    return byLabel;
  }

  private static void assertNumericSortExecutes(
      final Tab tab,
      final String sortKey,
      final FieldIdentifier f,
      final Sort sort,
      final Class<?> pointClass) throws Exception
  {
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        Document doc = new Document();
        if (IntPoint.class.equals(pointClass)) {
          doc.add(new IntPoint(f.label, 5));
          doc.add(new SortedNumericDocValuesField(f.label, 5));
        }
        else if (FloatPoint.class.equals(pointClass)) {
          doc.add(new FloatPoint(f.label, 5.5f));
          doc.add(new SortedNumericDocValuesField(
              f.label, NumericUtils.floatToSortableInt(5.5f)));
        }
        else {
          doc.add(new LongPoint(f.label, 5000L));
          doc.add(new SortedNumericDocValuesField(f.label, 5000L));
        }
        writer.addDocument(doc);
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        IndexSearcher localSearcher = new IndexSearcher(localReader);
        // Throws IllegalArgumentException when the sort width and the point width disagree.
        assertThat(localSearcher.search(new MatchAllDocsQuery(), 10, sort).scoreDocs)
            .as("sort '%s' on tab %s (field %s, written as %s) must execute against a real index",
                sortKey, tab, f.label, pointClass.getSimpleName())
            .hasSize(1);
      }
    }
  }

  @Test
  public void search_appliesNumericCreatedSort_descending_forWaiver() throws Exception {
    // WAIVER default created-desc: newest waiver first. Epoch-millis 3000 (newest) before 1000.
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        writer.addDocument(waiverDoc("w-old", 1000L));
        writer.addDocument(waiverDoc("w-new", 3000L));
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        IndexSearcher localSearcher = new IndexSearcher(localReader);
        Sort byCreated = IqLocalSearchService.sortFor(Tab.WAIVER, GlobalSearchSortAllowlist.WAIVER_CREATED);
        Query allWaivers = new TermQuery(
            new Term(FieldIdentifier.ITEM_TYPE.label,
                ItemType.POLICY_WAIVER.name().toLowerCase()));
        TopDocs top = localSearcher.search(allWaivers, 10, byCreated);
        List<String> order = new ArrayList<>();
        for (ScoreDoc sd : top.scoreDocs) {
          order.add(localSearcher.storedFields()
              .document(sd.doc)
              .get(FieldIdentifier.POLICY_WAIVER_ID.label));
        }
        assertThat(order).containsExactly("w-new", "w-old");
      }
    }
  }

  @Test
  public void search_appliesNumericMaxPolicyThreatSort_descending_forApplication() throws Exception {
    // A5: highest max-threat first. Lexicographic would order "10" before "9"; numeric places 10 first,
    // then 9, then 3. An app with no max-threat (null ordinal) sorts last (missing-value default).
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        writer.addDocument(appThreatDoc("a-9", 9));
        writer.addDocument(appThreatDoc("a-10", 10));
        writer.addDocument(appThreatDoc("a-3", 3));
        writer.addDocument(appThreatDoc("a-none", null));
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        IndexSearcher localSearcher = new IndexSearcher(localReader);
        Sort byThreat = IqLocalSearchService.sortFor(Tab.APPLICATION, "policyThreatLevel");
        Query allApps = new TermQuery(new Term(
            FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name().toLowerCase()));
        TopDocs top = localSearcher.search(allApps, 10, byThreat);
        List<String> order = new ArrayList<>();
        for (ScoreDoc sd : top.scoreDocs) {
          order.add(localSearcher.storedFields().document(sd.doc).get(FieldIdentifier.APPLICATION_PUBLIC_ID.label));
        }
        assertThat(order).containsExactly("a-10", "a-9", "a-3", "a-none");
      }
    }
  }

  @Test
  public void search_appliesNumericThreatSort_descending_forWaiver() throws Exception {
    // WAIVER threat sort: highest threat first, NUMERIC (not lexicographic). If it sorted the level
    // as a string, "10" would order before "2"; numeric order puts 10 (w-hi) first, then 2 (w-lo).
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        writer.addDocument(waiverThreatDoc("w-lo", 2));
        writer.addDocument(waiverThreatDoc("w-hi", 10));
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        IndexSearcher localSearcher = new IndexSearcher(localReader);
        Sort byThreat = IqLocalSearchService.sortFor(Tab.WAIVER, GlobalSearchSortAllowlist.WAIVER_THREAT);
        TopDocs top = localSearcher.search(allWaiversQuery(), 10, byThreat);
        List<String> order = new ArrayList<>();
        for (ScoreDoc sd : top.scoreDocs) {
          order.add(localSearcher.storedFields().document(sd.doc).get(FieldIdentifier.POLICY_WAIVER_ID.label));
        }
        assertThat(order).as("threat sorts numerically, highest first").containsExactly("w-hi", "w-lo");
      }
    }
  }

  @Test
  public void search_appliesViolationStateOrdinalSort_ascending_openFirst_forApplication() throws Exception {
    // A6: Open(0) first, then Waived(1), then Legacy(2). An app with no ordinal sorts last.
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        writer.addDocument(appStateOrdinalDoc("a-legacy", 2));
        writer.addDocument(appStateOrdinalDoc("a-open", 0));
        writer.addDocument(appStateOrdinalDoc("a-waived", 1));
        writer.addDocument(appStateOrdinalDoc("a-none", null));
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        IndexSearcher localSearcher = new IndexSearcher(localReader);
        Sort byState = IqLocalSearchService.sortFor(Tab.APPLICATION, "violationState");
        Query allApps = new TermQuery(new Term(
            FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name().toLowerCase()));
        TopDocs top = localSearcher.search(allApps, 10, byState);
        List<String> order = new ArrayList<>();
        for (ScoreDoc sd : top.scoreDocs) {
          order.add(localSearcher.storedFields().document(sd.doc).get(FieldIdentifier.APPLICATION_PUBLIC_ID.label));
        }
        assertThat(order).containsExactly("a-open", "a-waived", "a-legacy", "a-none");
      }
    }
  }

  @Test
  public void search_appliesNumericExpirationSort_ascending_neverExpiresLast_forWaiver() throws Exception {
    // WAIVER expiration sort: soonest expiry first (ASCENDING, numeric), and a never-expiring waiver
    // (no expires-at twin) sorts LAST. Epoch 1000 (w-soon) before 3000 (w-late) before never (w-never).
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        writer.addDocument(waiverExpiresDoc("w-late", 3000L));
        writer.addDocument(waiverExpiresDoc("w-never", null));
        writer.addDocument(waiverExpiresDoc("w-soon", 1000L));
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        IndexSearcher localSearcher = new IndexSearcher(localReader);
        Sort byExpiration = IqLocalSearchService.sortFor(Tab.WAIVER, GlobalSearchSortAllowlist.WAIVER_EXPIRATION);
        TopDocs top = localSearcher.search(allWaiversQuery(), 10, byExpiration);
        List<String> order = new ArrayList<>();
        for (ScoreDoc sd : top.scoreDocs) {
          order.add(localSearcher.storedFields().document(sd.doc).get(FieldIdentifier.POLICY_WAIVER_ID.label));
        }
        assertThat(order).as("expiration ascending, never-expires last")
            .containsExactly("w-soon", "w-late", "w-never");
      }
    }
  }

  private static Query allWaiversQuery() {
    return new TermQuery(new Term(
        FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_WAIVER.name().toLowerCase()));
  }

  @Test
  public void search_appliesNumericCreatedSort_ascending_oldestFirst_forWaiver() throws Exception {
    // WAIVER "oldest" sort reuses the created-at twin but ASCENDING: oldest (epoch 1000) first.
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        writer.addDocument(waiverDoc("w-old", 1000L));
        writer.addDocument(waiverDoc("w-new", 3000L));
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        IndexSearcher localSearcher = new IndexSearcher(localReader);
        Sort byOldest = IqLocalSearchService.sortFor(Tab.WAIVER, GlobalSearchSortAllowlist.WAIVER_OLDEST);
        TopDocs top = localSearcher.search(allWaiversQuery(), 10, byOldest);
        List<String> order = new ArrayList<>();
        for (ScoreDoc sd : top.scoreDocs) {
          order.add(localSearcher.storedFields().document(sd.doc).get(FieldIdentifier.POLICY_WAIVER_ID.label));
        }
        assertThat(order).as("oldest sorts created-at ascending").containsExactly("w-old", "w-new");
      }
    }
  }

  @Test
  public void luceneIndexingContext_emitsWaiverAndRequestNumericTwins_soSortsWork() throws Exception {
    // Round-trip proof that LuceneIndexingContext.addDocuments emits the threat + expiry numeric
    // sort twins for BOTH POLICY_WAIVER and POLICY_WAIVER_REQUEST docs (not just query-side fixtures).
    Directory dir = new ByteBuffersDirectory();
    try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
      LuceneIndexingContext ctx =
          new LuceneIndexingContext(null, writer, null);
      Document waiver = new DocumentBuilder(ItemType.POLICY_WAIVER)
          .setPolicyWaiverId("w1")
          .setPolicyWaiverThreatLevel(7)
          .setPolicyWaiverExpiresAtEpochMs(5000L)
          .build();
      Document request =
          new DocumentBuilder(ItemType.POLICY_WAIVER_REQUEST)
              .setPolicyWaiverId("r1")
              .setPolicyWaiverThreatLevel(3)
              .setPolicyWaiverExpiresAtEpochMs(2000L)
              .build();
      ctx.addDocuments(List.of(waiver, request));
      writer.commit();
    }
    try (IndexReader localReader = DirectoryReader.open(dir)) {
      IndexSearcher localSearcher = new IndexSearcher(localReader);
      // Expiration ascending across both item types: request (2000) before waiver (5000).
      Query bothTypes = new BooleanQuery.Builder()
          .add(new TermQuery(new Term(
              FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_WAIVER.name().toLowerCase())),
              BooleanClause.Occur.SHOULD)
          .add(new TermQuery(new Term(
              FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_WAIVER_REQUEST.name().toLowerCase())),
              BooleanClause.Occur.SHOULD)
          .build();
      Sort byExpiration = IqLocalSearchService.sortFor(Tab.WAIVER, GlobalSearchSortAllowlist.WAIVER_EXPIRATION);
      TopDocs top = localSearcher.search(bothTypes, 10, byExpiration);
      List<String> order = new ArrayList<>();
      for (ScoreDoc sd : top.scoreDocs) {
        order.add(localSearcher.storedFields().document(sd.doc).get(FieldIdentifier.POLICY_WAIVER_ID.label));
      }
      assertThat(order).as("twins emitted for both item types; expiration ascending spans both")
          .containsExactly("r1", "w1");
    }
    dir.close();
  }

  @Test
  public void searchAfter_numericFieldSortedPage2_returnsRowsPastPage1() throws Exception {
    // Cursor stability under a NUMERIC (threat-desc) field sort: page 1 (size 1) returns the
    // highest-threat doc; searchAfter its numeric sort value must return the next-lower doc, not
    // repeat page 1. Proves the SortedNumericSortField cursor anchors correctly (numeric, not string).
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        writer.addDocument(violationDoc("v-2", 2));
        writer.addDocument(violationDoc("v-7", 7));
        writer.addDocument(violationDoc("v-10", 10));
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        IndexSearcher localSearcher = new IndexSearcher(localReader);
        Sort byThreat = IqLocalSearchService.sortFor(Tab.VIOLATION, "threat");
        Query allViolations = new TermQuery(
            new Term(FieldIdentifier.ITEM_TYPE.label,
                ItemType.POLICY_VIOLATION.name().toLowerCase()));

        TopDocs page1 = localSearcher.search(allViolations, 1, byThreat);
        assertThat(localSearcher.storedFields()
            .document(page1.scoreDocs[0].doc)
            .get(FieldIdentifier.POLICY_VIOLATION_ID.label)).isEqualTo("v-10");

        // Anchor searchAfter on the highest threat value (10); next page must be v-7 then v-2. The
        // anchor is an Integer because the threat sort is an INT comparator (the field's point twin is
        // a 4-byte IntPoint), and searchAfter requires the anchor type to match the sort type.
        FieldDoc after = new FieldDoc(localReader.maxDoc() - 1, Float.NaN, new Object[]{10});
        TopDocs page2 = localSearcher.searchAfter(after, allViolations, 10, byThreat);
        List<String> page2Ids = new ArrayList<>();
        for (ScoreDoc sd : page2.scoreDocs) {
          page2Ids.add(localSearcher.storedFields()
              .document(sd.doc)
              .get(FieldIdentifier.POLICY_VIOLATION_ID.label));
        }
        assertThat(page2Ids).containsExactly("v-7", "v-2");
        assertThat(page2Ids).doesNotContain("v-10");
      }
    }
  }

  @Test
  public void mintNextCursor_roundTripsThroughSearch_withoutStaleCursorException() {
    // Scope: this test covers generation-token validation ONLY — that a cursor minted by the
    // service validates against the service's own expectedGenerationToken on the follow-up
    // search() call (no StaleCursorException). It does NOT assert paginated result ordering:
    // the sortKey here is "relevance", so sortFor resolves to null (field sort is flag-gated
    // off), and a relevance cursor's string sort-values cannot anchor an IndexSearcher
    // searchAfter. Actual searchAfter paging is exercised by
    // searchAfter_fieldSortedPage2_returnsRowsPastPage1 below (test helper) and at the backend
    // layer in A2.
    //
    // This proves mint<->validate agreement: mintNextCursor and search compute the same
    // generation-token preimage for the same (tab, sortKey, pageSize).
    Tab tab = Tab.APPLICATION;
    String sortKey = "relevance";
    int pageSize = 25;
    List<String> sortValues = List.of("acme-prod");

    GlobalSearchCursor minted = service.mintNextCursor(tab, sortKey, pageSize, sortValues);
    assertThat(minted).isNotNull();
    assertThat(minted.sortValues()).containsExactly("acme-prod");

    // The minted token must equal the token the service will expect on the next request.
    assertThat(minted.generationToken())
        .isEqualTo(service.expectedGenerationToken(tab, sortKey, pageSize));

    // Encode -> decode-with-validation round-trip against the service's expected token: the
    // opaque string a client would echo back decodes cleanly and validates (no stale throw).
    String encoded = minted.encode();
    assertThatCode(() -> GlobalSearchCursor.decode(encoded, service.expectedGenerationToken(tab, sortKey, pageSize)))
        .doesNotThrowAnyException();

    // Feed the minted cursor back into a real second search(): the generation-token check in
    // search() must accept it (no StaleCursorException) and return the expected page.
    SearchInputs followUp = new SearchInputs("acme", tab,
        Set.of(ItemType.APPLICATION), pageSize, sortKey, minted.encode());
    IqLocalSearchResponse response = service.search(followUp);
    assertThat(response).isNotNull();
    assertThat(response.sortKey()).isEqualTo(sortKey);
    assertThat(response.rows()).extracting(r -> r.row().applicationPublicId)
        .containsExactlyInAnyOrder("acme-prod", "acme-dev", "acme-west");
  }

  @Test
  public void search_propagatesServingBackendIdFromResult() {
    SearchResultItemDTO row = new SearchResultItemDTO();
    row.itemType = ItemType.APPLICATION.name();
    row.applicationPublicId = "acme-prod";
    // doReturn avoids invoking the setUp thenAnswer stub (which would run runRealSearch(null)).
    Mockito.doReturn(new GlobalSearchResult(List.of(row), 1, List.of(), true, "secondary"))
        .when(searchIndexClient)
        .searchGlobal(any());

    SearchInputs inputs = new SearchInputs("acme", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.servingBackendId()).isEqualTo("secondary");
  }

  @Test
  public void search_withCursorMintedForDifferentPageSize_throwsStaleCursorException() {
    // Negative control for the round-trip: a cursor pinned to a different pageSize must NOT
    // validate, confirming the generation token actually binds pageSize (mint<->validate is
    // exact, not vacuously true).
    Tab tab = Tab.APPLICATION;
    String sortKey = "relevance";
    GlobalSearchCursor mintedForPage10 = service.mintNextCursor(tab, sortKey, 10, List.of("acme-prod"));
    SearchInputs followUp = new SearchInputs("acme", tab,
        Set.of(ItemType.APPLICATION), 25, sortKey, mintedForPage10.encode());
    assertThatCode(() -> service.search(followUp)).isInstanceOf(StaleCursorException.class);
  }

  @Test
  public void searchAfter_fieldSortedPage2_returnsRowsPastPage1() throws Exception {
    // Exercises real searchAfter paging against the in-memory index. Bypasses service.search
    // (whose field sort is flag-gated off) by driving runRealSearch directly with a STRING field
    // Sort on APPLICATION_NAME, for which the fixture indexes doc-values. Three application docs
    // sort by name as: "Acme Dev", "Acme Prod", "Acme West App", "Widget Inventory".
    Query allApps = new TermQuery(
        new Term(FieldIdentifier.ITEM_TYPE.label,
            ItemType.APPLICATION.name().toLowerCase()));
    Sort byName = new Sort(new SortField(FieldIdentifier.APPLICATION_NAME.label, SortField.Type.STRING));

    GlobalSearchResult page1 = runRealSearch(new GlobalSearchRequest(allApps, byName, 2, List.of()));
    List<String> page1Ids = page1.rows().stream().map(r -> r.applicationPublicId).toList();
    assertThat(page1Ids).containsExactly("acme-dev", "acme-prod");

    // The cursor anchor is the sort-value of the last hit on page 1 ("Acme Prod"). searchAfter
    // must return the row(s) strictly past it, i.e. page 2 differs from page 1.
    GlobalSearchResult page2 =
        runRealSearch(new GlobalSearchRequest(allApps, byName, 2, List.of("Acme Prod")));
    List<String> page2Ids = page2.rows().stream().map(r -> r.applicationPublicId).toList();
    assertThat(page2Ids).containsExactly("acme-west", "widget-co");
    assertThat(page2Ids).doesNotContainAnyElementsOf(page1Ids);
  }

  // ---- helpers --------------------------------------------------------------------------------

  private GlobalSearchResult runRealSearch(final GlobalSearchRequest request) throws Exception {
    int numHits = Math.max(1, request.pageSize());
    FieldDoc after = searchAfterFieldDoc(request);
    TopDocs topDocs;
    if (request.sort() == null) {
      // Relevance ordering. A relevance cursor would need the prior page's (score, docId); the
      // string sort-values a GlobalSearchCursor carries cannot reconstruct that, so first-page
      // and searchAfter behave identically here. Real relevance paging lives in the A2 backend.
      topDocs = searcher.search(request.baseQuery(), numHits);
    }
    else if (after == null) {
      topDocs = searcher.search(request.baseQuery(), numHits, request.sort());
    }
    else {
      // Field-sorted paging: forward the cursor's sort-values as the searchAfter anchor so the
      // second page genuinely starts past the last hit of the first page (mirrors how the real
      // backend applies IndexSearcher.searchAfter once field sort is enabled).
      topDocs = searcher.searchAfter(after, request.baseQuery(), numHits, request.sort());
    }
    List<SearchResultItemDTO> rows = new ArrayList<>();
    for (ScoreDoc hit : topDocs.scoreDocs) {
      Document doc = searcher.storedFields().document(hit.doc);
      SearchResultItemDTO dto = new SearchResultItemDTO();
      dto.applicationName = doc.get(FieldIdentifier.APPLICATION_NAME.label);
      dto.applicationPublicId = doc.get(FieldIdentifier.APPLICATION_PUBLIC_ID.label);
      dto.organizationName = doc.get(FieldIdentifier.ORGANIZATION_NAME.label);
      dto.itemType = doc.get(FieldIdentifier.ITEM_TYPE.label);
      dto.componentName = doc.get(FieldIdentifier.COMPONENT_NAME.label);
      dto.vulnerabilityId = doc.get(FieldIdentifier.VULNERABILITY_ID.label);
      String firstSeen = doc.get(FieldIdentifier.VULNERABILITY_FIRST_SEEN_EPOCH_MS.label);
      dto.vulnerabilityFirstSeenEpochMs = firstSeen == null ? null : Long.valueOf(firstSeen);
      rows.add(dto);
    }
    long total = topDocs.totalHits.value;
    return new GlobalSearchResult(rows, total, List.of());
  }

  /**
   * Build the {@link FieldDoc} anchor for {@link IndexSearcher#searchAfter} from a request's
   * field {@link Sort} and its {@code searchAfter} sort-values. Returns
   * {@code null} for the first page (no cursor) or when the request has no field sort — the
   * relevance path cannot be anchored by string sort-values alone. Only STRING sort fields are
   * supported, which is all this fixture indexes doc-values for.
   */
  private FieldDoc searchAfterFieldDoc(final GlobalSearchRequest request) {
    if (request.sort() == null || request.searchAfter().isEmpty()) {
      return null;
    }
    SortField[] sortFields = request.sort().getSort();
    List<String> values = request.searchAfter();
    Object[] fieldValues = new Object[sortFields.length];
    for (int i = 0; i < sortFields.length; i++) {
      String raw = i < values.size() ? values.get(i) : null;
      if (sortFields[i].getType() != SortField.Type.STRING) {
        throw new IllegalArgumentException(
            "test helper only supports STRING sort searchAfter, got " + sortFields[i].getType());
      }
      // The APPLICATION_NAME doc-values are lower-cased at index time (see docOf), so anchor on the
      // lower-cased form to match how Lucene compares the stored BytesRef.
      fieldValues[i] = raw == null ? null : new BytesRef(raw.toLowerCase());
    }
    // The doc id is only a within-equal-sort-value tiebreaker. Sort values are unique in this
    // fixture, so any valid doc id works; use the last one so a tie would fall after the anchor.
    // IndexSearcher rejects doc ids >= reader.maxDoc(), so Integer.MAX_VALUE is not usable here.
    return new FieldDoc(reader.maxDoc() - 1, Float.NaN, fieldValues);
  }

  private static Document appDoc(final String publicId, final String name, final String orgName) {
    Map<String, String> fields = new HashMap<>();
    // Store ItemType in uppercase so the LowerCaseKeywordAnalyzer actually exercises
    // case-folding at index time (matching production DocumentBuilder behaviour).
    fields.put(FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name());
    fields.put(FieldIdentifier.APPLICATION_PUBLIC_ID.label, publicId);
    fields.put(FieldIdentifier.APPLICATION_NAME.label, name);
    fields.put(FieldIdentifier.ORGANIZATION_NAME.label, orgName);
    return docOf(fields);
  }

  private static Document childOrgAppDoc(
      final String publicId,
      final String name,
      final String ownOrgName,
      final String parentOrgName)
  {
    Map<String, String> fields = new HashMap<>();
    fields.put(FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name());
    fields.put(FieldIdentifier.APPLICATION_PUBLIC_ID.label, publicId);
    fields.put(FieldIdentifier.APPLICATION_NAME.label, name);
    fields.put(FieldIdentifier.ORGANIZATION_NAME.label, ownOrgName);
    // Ancestor-carrying field the org filters now target; production DocumentBuilder emits one
    // parentOrganizationName per ancestor. A single ancestor is enough to prove subtree matching.
    fields.put(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, parentOrgName);
    return docOf(fields);
  }

  private static Document orgDoc(final String orgName) {
    Map<String, String> fields = new HashMap<>();
    fields.put(FieldIdentifier.ITEM_TYPE.label, ItemType.ORGANIZATION.name());
    fields.put(FieldIdentifier.ORGANIZATION_NAME.label, orgName);
    return docOf(fields);
  }

  private static Document componentDoc(final String componentName) {
    Map<String, String> fields = new HashMap<>();
    fields.put(FieldIdentifier.ITEM_TYPE.label, ItemType.NON_VULNERABLE_COMPONENT.name());
    fields.put(FieldIdentifier.COMPONENT_NAME.label, componentName);
    return docOf(fields);
  }

  private static Document componentDocWithMaxPolicyThreatLevel(final String componentName, final int threatLevel) {
    Map<String, String> fields = new HashMap<>();
    fields.put(FieldIdentifier.ITEM_TYPE.label, ItemType.NON_VULNERABLE_COMPONENT.name());
    fields.put(FieldIdentifier.COMPONENT_NAME.label, componentName);
    Document doc = docOf(fields);
    // Mirror DocumentBuilder.setComponentMaxPolicyThreatLevel: IntPoint (range-queryable) + StoredField,
    // plus the SortedNumericDocValues sort twin LuceneIndexingContext adds, so this fixture backs both
    // the range filter and the policyThreatLevel sort.
    doc.add(new IntPoint(FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label, threatLevel));
    doc.add(new StoredField(FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label, threatLevel));
    doc.add(new SortedNumericDocValuesField(
        FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label, threatLevel));
    return doc;
  }

  /** A Maven component carrying the coordinate fields a pasted purl is decomposed against. */
  private static Document mavenComponentDoc(final String groupId, final String artifactId, final String version) {
    Map<String, String> fields = new HashMap<>();
    fields.put(FieldIdentifier.ITEM_TYPE.label, ItemType.NON_VULNERABLE_COMPONENT.name());
    fields.put(FieldIdentifier.COMPONENT_NAME.label, groupId + " : " + artifactId + " : " + version);
    fields.put(FieldIdentifier.COMPONENT_FORMAT.label, "maven");
    fields.put(FieldIdentifier.COMPONENT_COORDINATE_GROUP_ID.label, groupId);
    fields.put(FieldIdentifier.COMPONENT_COORDINATE_ARTIFACT_ID.label, artifactId);
    fields.put(FieldIdentifier.COMPONENT_COORDINATE_VERSION.label, version);
    fields.put(FieldIdentifier.COMPONENT_COORDINATE_EXTENSION.label, "jar");
    return docOf(fields);
  }

  /**
   * POLICY_VIOLATION doc mirroring production exactly: {@code DocumentBuilder} writes the threat
   * level as a 4-byte {@link IntPoint} plus a {@link StoredField}, and
   * {@code LuceneIndexingContext.addDocuments} adds the {@link SortedNumericDocValuesField} sort
   * twin. The IntPoint matters for sort coverage — Lucene's numeric comparator validates the sort's
   * byte width against the points index, so a fixture carrying only the doc-values twin cannot catch
   * a sort-width mismatch that fails on a real index.
   */
  private static Document violationDoc(final String violationId, final int threatLevel) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_VIOLATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_VIOLATION_ID.label, violationId, Store.YES));
    doc.add(new IntPoint(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label, threatLevel));
    doc.add(new StoredField(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label, threatLevel));
    doc.add(new SortedNumericDocValuesField(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label, threatLevel));
    return doc;
  }

  /**
   * LEGAL_VIOLATION doc: shares the VIOLATION tab with POLICY_VIOLATION but carries
   * {@code componentLicenseThreatLevel} instead of a policy threat level, so it has no
   * policyViolationThreatLevel field at all and must sort last under the threat sort.
   */
  private static Document legalViolationDoc(final String violationId, final int licenseThreatLevel) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.LEGAL_VIOLATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_VIOLATION_ID.label, violationId, Store.YES));
    doc.add(new IntPoint(FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label, licenseThreatLevel));
    doc.add(new StoredField(FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label, licenseThreatLevel));
    return doc;
  }

  /**
   * APPLICATION doc carrying the max-policy-threat numeric sort twin (null = no active violation),
   * plus the 4-byte {@link IntPoint} production writes alongside it so the sort's byte-width
   * agreement with the points index is actually exercised.
   */
  private static Document appThreatDoc(final String publicId, final Integer maxThreatLevel) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_PUBLIC_ID.label, publicId, Store.YES));
    if (maxThreatLevel != null) {
      doc.add(new IntPoint(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label, maxThreatLevel));
      doc.add(new StoredField(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label, maxThreatLevel));
      doc.add(new SortedNumericDocValuesField(
          FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label, maxThreatLevel));
    }
    return doc;
  }

  /**
   * APPLICATION doc carrying the violation-state-ordinal numeric sort twin (null = no violation),
   * plus the 4-byte {@link IntPoint} production writes alongside it.
   */
  private static Document appStateOrdinalDoc(final String publicId, final Integer ordinal) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_PUBLIC_ID.label, publicId, Store.YES));
    if (ordinal != null) {
      doc.add(new IntPoint(FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL.label, ordinal));
      doc.add(new StoredField(FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL.label, ordinal));
      doc.add(new SortedNumericDocValuesField(
          FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL.label, ordinal));
    }
    return doc;
  }

  /** POLICY_WAIVER doc carrying the created-at epoch-millis numeric sort doc-values twin. */
  private static Document waiverDoc(final String waiverId, final long createdAtEpochMs) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_WAIVER.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_ID.label, waiverId, Store.YES));
    doc.add(new SortedNumericDocValuesField(
        FieldIdentifier.POLICY_WAIVER_CREATED_AT_EPOCH_MS.label, createdAtEpochMs));
    return doc;
  }

  /**
   * POLICY_WAIVER doc carrying the threat-level numeric sort doc-values twin. Mirrors the twin
   * {@code LuceneIndexingContext} will emit for POLICY_WAIVER docs (owned by the waiver-request
   * indexing workstream); the test emits it directly to prove the query-side sort ordering.
   */
  private static Document waiverThreatDoc(final String waiverId, final int threatLevel) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_WAIVER.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_ID.label, waiverId, Store.YES));
    doc.add(new IntPoint(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, threatLevel));
    doc.add(new StoredField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, threatLevel));
    doc.add(new SortedNumericDocValuesField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, threatLevel));
    return doc;
  }

  /**
   * POLICY_WAIVER doc carrying the expires-at epoch-millis numeric sort doc-values twin, or no twin
   * for a never-expiring waiver (null). Mirrors the twin {@code LuceneIndexingContext} will emit for
   * POLICY_WAIVER docs (owned by the waiver-request indexing workstream).
   */
  private static Document waiverExpiresDoc(final String waiverId, final Long expiresAtEpochMs) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_WAIVER.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_ID.label, waiverId, Store.YES));
    if (expiresAtEpochMs != null) {
      doc.add(new SortedNumericDocValuesField(
          FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label, expiresAtEpochMs));
    }
    return doc;
  }

  private static Document vulnDoc(final String vulnId, final String componentName, final String description) {
    Map<String, String> fields = new HashMap<>();
    fields.put(FieldIdentifier.ITEM_TYPE.label, ItemType.SECURITY_VULNERABILITY.name());
    fields.put(FieldIdentifier.VULNERABILITY_ID.label, vulnId);
    fields.put(FieldIdentifier.COMPONENT_NAME.label, componentName);
    fields.put(FieldIdentifier.VULNERABILITY_DESCRIPTION.label, description);
    return docOf(fields);
  }

  private static Document vulnDocWithFirstSeen(
      final String vulnId,
      final String componentName,
      final long firstSeenEpochMs)
  {
    Map<String, String> fields = new HashMap<>();
    fields.put(FieldIdentifier.ITEM_TYPE.label, ItemType.SECURITY_VULNERABILITY.name());
    fields.put(FieldIdentifier.VULNERABILITY_ID.label, vulnId);
    fields.put(FieldIdentifier.COMPONENT_NAME.label, componentName);
    Document doc = docOf(fields);
    // Mirror DocumentBuilder.setVulnerabilityFirstSeenEpochMs: LongPoint (range-queryable) + StoredField.
    doc.add(new LongPoint(FieldIdentifier.VULNERABILITY_FIRST_SEEN_EPOCH_MS.label, firstSeenEpochMs));
    doc.add(new StoredField(FieldIdentifier.VULNERABILITY_FIRST_SEEN_EPOCH_MS.label, firstSeenEpochMs));
    return doc;
  }

  /** Builds an analyzed Document. Matches LuceneComponents.newAnalyzerForSearch() wiring. */
  private static Document docOf(final Map<String, String> fields) {
    Document doc = new Document();
    for (Map.Entry<String, String> e : fields.entrySet()) {
      doc.add(new TextField(e.getKey(), e.getValue(), Store.YES));
    }
    // SortedDocValuesField for fields that the sort assertions exercise.
    String appName = fields.get(FieldIdentifier.APPLICATION_NAME.label);
    if (appName != null) {
      doc.add(new SortedDocValuesField(FieldIdentifier.APPLICATION_NAME.label,
          new BytesRef(appName.toLowerCase())));
    }
    return doc;
  }

  // Shape-of-builder-output test removed: the AST-based composition path is covered by
  // QueryCompilerTest and FieldMapTest under the fieldmap package. Retaining a shape assertion
  // here would duplicate that coverage and would over-constrain the internal Query tree the
  // AST compiler produces (which now uses a richer Query set including PhraseQuery and
  // PointRangeQuery, not the narrow PrefixQuery+TermQuery subset the old builder emitted).

  /**
   * Live round-trip on a real Lucene index proving the request-status filters actually match. The
   * status is written by {@link DocumentBuilder} (lowercased keyword) and the query strings are the
   * ones {@link IndexQueryFilterCompiler} emits, parsed through the production analyzer
   * ({@code LuceneComponents.newQueryParser}). Before the fix the StringField held the uppercase
   * enum name while the query term was lowercased by {@code LowerCaseKeywordAnalyzer}, so
   * waiverStates=[requested]/[rejected], status=[REJECTED], and explicit includeAutoWaivers:false +
   * request states all returned ZERO on Lucene (OpenSearch's keyword normalizer hid this).
   */
  @Test
  public void requestStatusFilters_matchOnRealLuceneIndex_notZeroedByCaseMismatch() throws Exception {
    final LuceneComponents luceneComponents = new LuceneComponents(mock(InsightWork.class));
    try (Directory dir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(luceneComponents.newAnalyzerForSearch()))) {
        writer.addDocument(new DocumentBuilder(ItemType.POLICY_WAIVER_REQUEST)
            .setPolicyWaiverId("req-requested")
            .setPolicyWaiverRequestStatus("REQUESTED")
            .build());
        writer.addDocument(new DocumentBuilder(ItemType.POLICY_WAIVER_REQUEST)
            .setPolicyWaiverId("req-rejected")
            .setPolicyWaiverRequestStatus("REJECTED")
            .build());
        writer.addDocument(new DocumentBuilder(ItemType.POLICY_WAIVER)
            .setPolicyWaiverId("committed-waiver")
            .setPolicyWaiverAuto(false)
            .build());
        writer.commit();
      }
      try (IndexReader localReader = DirectoryReader.open(dir)) {
        final IndexSearcher localSearcher = new IndexSearcher(localReader);

        assertThat(hits(localSearcher, luceneComponents,
            IndexQueryFilterCompiler.compileWithClauses(
                IndexQueryType.WAIVER, Map.of("waiverStates", List.of("requested"))).q()))
                    .as("waiverStates=[requested] returns the REQUESTED request on Lucene")
                    .isEqualTo(1);

        assertThat(hits(localSearcher, luceneComponents,
            IndexQueryFilterCompiler.compileWithClauses(
                IndexQueryType.WAIVER, Map.of("waiverStates", List.of("rejected"))).q()))
                    .as("waiverStates=[rejected] returns the REJECTED request on Lucene")
                    .isEqualTo(1);

        assertThat(hits(localSearcher, luceneComponents,
            IndexQueryFilterCompiler.compileWithClauses(
                IndexQueryType.WAIVER, Map.of("status", List.of("REJECTED"))).q()))
                    .as("status=[REJECTED] returns the REJECTED request on Lucene")
                    .isEqualTo(1);

        final Map<String, Object> explicitFalseWithStates = new HashMap<>();
        explicitFalseWithStates.put("waiverStates", List.of("requested", "rejected"));
        explicitFalseWithStates.put("includeAutoWaivers", false);
        assertThat(hits(localSearcher, luceneComponents,
            IndexQueryFilterCompiler.compileWithClauses(IndexQueryType.WAIVER, explicitFalseWithStates).q()))
                .as("explicit includeAutoWaivers:false + request states returns both requests on Lucene")
                .isEqualTo(2);
      }
    }
  }

  private static int hits(
      final IndexSearcher searcher,
      final LuceneComponents components,
      final String queryString) throws Exception
  {
    final Query query = components.newQueryParser().apply(queryString);
    return (int) searcher.search(query, 100).totalHits.value;
  }
}
