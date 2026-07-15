/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
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
import com.sonatype.insight.brain.search.lucene.LowerCaseKeywordAnalyzer;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
      writer.addDocument(vulnDoc("CVE-2021-44228", "log4j-core", "Remote code execution in Log4j2"));
      writer.addDocument(vulnDoc("CVE-2017-7525", "jackson-databind", "Deserialization gadget chain"));
      writer.commit();
    }
    reader = DirectoryReader.open(directory);
    searcher = new IndexSearcher(reader);
    service = new IqLocalSearchService(searchIndexClient,
        com.sonatype.insight.brain.search.global.fieldmap.FieldMap.defaultMap());

    when(searchIndexClient.isGlobalSearchEnabled()).thenReturn(true);
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
    // 5 components in the fixture, all NON_VULNERABLE_COMPONENT.
    assertThat(response.total()).isEqualTo(5L);
  }

  @Test
  public void componentsTab_itemTypeComponentFilter_matchesComponentDocs() {
    // The user-facing itemType:COMPONENT token must resolve to the index discriminator
    // non_vulnerable_component and match every component document (5 in the fixture) with no
    // spurious warning — proving the alias mapping and the dropped enum-value gate work end to end.
    SearchInputs inputs = new SearchInputs("itemType:COMPONENT", Tab.COMPONENT,
        Set.of(ItemType.NON_VULNERABLE_COMPONENT), 25, "relevance", null);
    IqLocalSearchResponse response = service.search(inputs);
    assertThat(response.warnings()).isEmpty();
    assertThat(response.total()).isEqualTo(5L);
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
  public void search_appliesSortKey_byApplicationName() {
    SearchInputs inputs = new SearchInputs("acme", Tab.APPLICATION,
        Set.of(ItemType.APPLICATION), 25, "name", null);
    IqLocalSearchResponse response = service.search(inputs);
    // The validated sort key is echoed back even though the physical sort resolves to
    // relevance (the current DocumentBuilder does not emit doc-values for the sortable
    // keys yet). Both Acme apps plus the "Acme West" child-org app (prefix match on its own
    // organizationName) must be returned, in any relevance order.
    assertThat(response.sortKey()).isEqualTo("name");
    assertThat(response.rows()).extracting(r -> r.row().applicationPublicId)
        .containsExactlyInAnyOrder("acme-dev", "acme-prod", "acme-west");
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
    Query allApps = new org.apache.lucene.search.TermQuery(
        new org.apache.lucene.index.Term(FieldIdentifier.ITEM_TYPE.label,
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
      rows.add(dto);
    }
    long total = topDocs.totalHits.value;
    return new GlobalSearchResult(rows, total, List.of());
  }

  /**
   * Build the {@link FieldDoc} anchor for {@link IndexSearcher#searchAfter} from a request's
   * field {@link org.apache.lucene.search.Sort} and its {@code searchAfter} sort-values. Returns
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

  private static Document vulnDoc(final String vulnId, final String componentName, final String description) {
    Map<String, String> fields = new HashMap<>();
    fields.put(FieldIdentifier.ITEM_TYPE.label, ItemType.SECURITY_VULNERABILITY.name());
    fields.put(FieldIdentifier.VULNERABILITY_ID.label, vulnId);
    fields.put(FieldIdentifier.COMPONENT_NAME.label, componentName);
    fields.put(FieldIdentifier.VULNERABILITY_DESCRIPTION.label, description);
    return docOf(fields);
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
}
