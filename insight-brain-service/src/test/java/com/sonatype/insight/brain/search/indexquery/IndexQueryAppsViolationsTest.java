/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LowerCaseKeywordAnalyzer;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * End-to-end coverage of the PR-B Applications + Violations completeness surface (new filters, facets,
 * row fields, and hrefs) against a real in-memory Lucene index.
 */
public class IndexQueryAppsViolationsTest
{
  private SearchIndexClient searchIndexClient;

  private Directory directory;

  private IndexReader reader;

  private IndexSearcher searcher;

  private ConversionHelper conversionHelper;

  private IndexQueryResource resource;

  @Before
  public void setUp() throws Exception {
    SystemConfigurationPropertyFeatureTestSupport.install();
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    directory = new ByteBuffersDirectory();
    PerFieldAnalyzerWrapper analyzer =
        new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), new HashMap<>());
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
      writer.addDocument(appDoc("acme-prod", "Acme Prod", "Acme", "acme-id",
          List.of("Web", "PCI"), 1_700_000_000_000L, List.of("build:critical:3", "build:low:2", "release:severe:1")));
      writer.addDocument(appDoc("acme-dev", "Acme Dev", "Acme", "acme-id",
          List.of("Web"), 1_600_000_000_000L, List.of("develop:moderate:4")));
      writer.addDocument(appDoc("widget-co", "Widget Inventory", "Widget Co", "widget-id",
          List.of("Internal"), null, List.of()));
      // Open (Active), manually-waived, auto-waived violations across apps/categories/stages.
      writer.addDocument(violationDoc("pv-open", "Acme Prod", "Acme", "Security Policy", "security",
          "Active", "build", 8, "log4j-core", "2.14.1", List.of("Web", "PCI")));
      writer.addDocument(violationDoc("pv-manual", "Acme Prod", "Acme", "License Policy", "license",
          "Waived", "release", 5, "guava", "30.0", List.of("Web", "PCI")));
      writer.addDocument(violationDoc("pv-auto", "Widget Inventory", "Widget Co", "Quality Policy", "quality",
          "AutoWaived", "build", 3, "commons-io", "2.6", List.of("Internal")));
      writer.commit();
    }
    reader = DirectoryReader.open(directory);
    searcher = new IndexSearcher(reader);
    conversionHelper = new ConversionHelper(new LuceneComponents(mock(InsightWork.class)));

    searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("org-1"));
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(searchIndexClient.getLastIndexTime()).thenReturn(1000L);
    when(searchIndexClient.backendId()).thenReturn("lucene");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenAnswer(inv -> runRealSearch(inv.getArgument(0)));
    when(searchIndexClient.count(any())).thenAnswer(inv -> countReal(inv.getArgument(0)));

    IqLocalSearchService iq = new IqLocalSearchService(searchIndexClient);
    IndexQueryService service = new IndexQueryService(iq, searchIndexClient, null);
    resource = new IndexQueryResource(service, searchIndexClient);
  }

  @After
  public void tearDown() throws Exception {
    if (reader != null) {
      reader.close();
    }
    if (directory != null) {
      directory.close();
    }
    SystemConfigurationPropertyFeatureTestSupport.uninstall();
  }

  // ---- Applications ------------------------------------------------------------------------

  @Test
  public void applicationRow_surfacesStageSeverityPillsLastEvalAndCategories() {
    IndexQueryRow row = firstRow("APPLICATION", Map.of("applications", List.of("Acme Prod")));
    assertThat(row.getId()).isEqualTo("acme-prod");
    assertThat(row.getFields().get("lastEvaluationTimeEpochMs")).isEqualTo(1_700_000_000_000L);
    assertThat(row.getFields().get("applicationCategories")).asList().containsExactlyInAnyOrder("Web", "PCI");

    @SuppressWarnings("unchecked")
    Map<String, Map<String, Integer>> breakdown =
        (Map<String, Map<String, Integer>>) row.getFields().get("stageSeverityBreakdown");
    assertThat(breakdown).containsKeys("build", "release");
    assertThat(breakdown.get("build")).containsEntry("critical", 3)
        .containsEntry("low", 2)
        .containsEntry("severe", 0)
        .containsEntry("moderate", 0);
    assertThat(breakdown.get("release")).containsEntry("severe", 1);

    @SuppressWarnings("unchecked")
    Map<String, Integer> totalRisk = (Map<String, Integer>) row.getFields().get("totalRisk");
    assertThat(totalRisk).containsEntry("critical", 3).containsEntry("severe", 1).containsEntry("low", 2);
  }

  @Test
  public void applicationRow_hrefIsStableManagementLink_notClassicBundlePath() {
    IndexQueryRow row = firstRow("APPLICATION", Map.of("applications", List.of("Acme Prod")));
    // href is built from the stable internal applicationId, not the Classic bundle path or /preview.
    assertThat(row.getHref()).isEqualTo("/ui/links/application/acme-prod-appid/management");
    assertThat(row.getHref()).doesNotContain("assets/index.html").doesNotContain("/preview/");
  }

  @Test
  public void applicationCategoriesFilter_narrowsToTaggedApps() {
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "APPLICATION", Map.of("applicationCategories", List.of("Internal")), 1, 25, null, null, false));
    assertThat(resp.rows()).extracting(IndexQueryRow::getId).containsExactly("widget-co");
  }

  @Test
  public void applicationLastEvaluationSort_isAllowlistedAndAccepted() {
    // Field sort is active: lastEvaluationTime is allowlisted and runs a real numeric sort, so the
    // request succeeds rather than 400-ing, matching the name/stage sorts.
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "APPLICATION", Map.of(), 1, 25, "lastEvaluationTime", null, false));
    assertThat(resp.entityType()).isEqualTo("APPLICATION");
    assertThat(resp.rows()).isNotEmpty();
  }

  @Test
  public void applicationFacets_bucketOrgAppByName_andExposeDenormalizedViolationFacets() {
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "APPLICATION", Map.of(), 1, 25, null, null, true));
    // stages/policyTypes/violationStates bucket by the denormalized applicationViolationStage/
    // PolicyType/State keyword sets already written on each application doc, so they are served
    // from the existing index with no reindex.
    assertThat(resp.facets()).containsOnlyKeys(
        "organizations", "applications", "applicationCategories", "stages", "policyTypes", "violationStates");
    // The stage buckets are the raw indexed stage ids, so a bucket value round-trips as a filter.
    assertThat(resp.facets().get("stages")).extracting(
        IndexQueryResponse.IndexQueryFacetBucket::value).contains("build", "release", "develop");
    // Org/app facets bucket by the display name, so the emitted value round-trips back through the
    // organizations/applications filter (which matches organizationName/applicationName).
    assertThat(resp.facets().get("organizations")).anySatisfy(b -> {
      assertThat(b.value()).isEqualTo("Acme");
      assertThat(b.displayName()).isNull();
    });
    assertThat(resp.facets().get("applications")).extracting(
        IndexQueryResponse.IndexQueryFacetBucket::value).contains("Acme Prod", "Acme Dev", "Widget Inventory");
    assertThat(resp.facets().get("applicationCategories")).extracting(
        IndexQueryResponse.IndexQueryFacetBucket::value).contains("Web");
  }

  @Test
  public void applicationOrgFacetValue_roundTripsBackThroughOrganizationsFilter() {
    IndexQueryResponse facetResp = resource.query(new IndexQueryRequest(
        "APPLICATION", Map.of(), 1, 25, null, null, true));
    String orgBucketValue = facetResp.facets().get("organizations").get(0).value();
    // Feeding the facet bucket value straight back as a filter value must return matching rows.
    IndexQueryResponse filtered = resource.query(new IndexQueryRequest(
        "APPLICATION", Map.of("organizations", List.of(orgBucketValue)), 1, 25, null, null, false));
    assertThat(filtered.rows()).isNotEmpty();
  }

  @Test
  public void applicationStagesFilter_compilesAgainstDenormalizedField_andNarrowsToActiveStage() {
    // The APPLICATION stages filter compiles against the denormalized applicationViolationStage set
    // (one entry per stage with an active violation), so filtering by build keeps only apps that
    // actually carry a build-stage violation.
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "APPLICATION", Map.of("stages", List.of("build")), 1, 25, null, null, false));
    assertThat(resp.rows()).extracting(IndexQueryRow::getId).containsExactly("acme-prod");
  }

  // ---- Violations --------------------------------------------------------------------------

  @Test
  public void violationRow_surfacesStateWaiverTypeComponentVersionStageAndCategories() {
    IndexQueryRow open = violationRowById("pv-open");
    assertThat(open.getFields()).containsEntry("state", "OPEN");
    assertThat(open.getFields().get("waiverType")).isNull();
    assertThat(open.getFields()).containsEntry("componentVersion", "2.14.1");
    assertThat(open.getFields()).containsEntry("stage", "Build");
    assertThat(open.getFields().get("applicationCategories")).asList().contains("Web", "PCI");

    IndexQueryRow manual = violationRowById("pv-manual");
    assertThat(manual.getFields()).containsEntry("state", "WAIVED").containsEntry("waiverType", "MANUAL");

    IndexQueryRow auto = violationRowById("pv-auto");
    assertThat(auto.getFields()).containsEntry("state", "WAIVED").containsEntry("waiverType", "AUTO");
  }

  @Test
  public void violationRow_hrefIsStablePolicyViolationLink_notPreviewPath() {
    IndexQueryRow row = violationRowById("pv-open");
    assertThat(row.getHref()).isEqualTo("/ui/links/policyViolation/pv-open");
    assertThat(row.getHref()).doesNotContain("/preview/");
  }

  @Test
  public void violationStatesFilter_open_excludesWaived() {
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of("states", List.of("OPEN")), 1, 25, null, null, false));
    assertThat(resp.rows()).extracting(IndexQueryRow::getId).containsExactly("pv-open");
  }

  @Test
  public void violationStatesFilter_waived_includesManualAndAuto() {
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of("states", List.of("WAIVED")), 1, 25, null, null, false));
    assertThat(resp.rows()).extracting(IndexQueryRow::getId).containsExactlyInAnyOrder("pv-manual", "pv-auto");
  }

  @Test
  public void violationWaiverTypeFilter_autoAndManualAreDistinct() {
    IndexQueryResponse auto = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of("waiverType", "AUTO"), 1, 25, null, null, false));
    assertThat(auto.rows()).extracting(IndexQueryRow::getId).containsExactly("pv-auto");

    IndexQueryResponse manual = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of("waiverType", "MANUAL"), 1, 25, null, null, false));
    assertThat(manual.rows()).extracting(IndexQueryRow::getId).containsExactly("pv-manual");
  }

  @Test
  public void violationStagesFilter_narrowsByStage() {
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of("stages", List.of("release")), 1, 25, null, null, false));
    assertThat(resp.rows()).extracting(IndexQueryRow::getId).containsExactly("pv-manual");
  }

  @Test
  public void violationApplicationCategoriesFilter_narrowsByCategory() {
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of("applicationCategories", List.of("Internal")), 1, 25, null, null, false));
    assertThat(resp.rows()).extracting(IndexQueryRow::getId).containsExactly("pv-auto");
  }

  @Test
  public void violationThreatSort_isAllowlistedAndAccepted() {
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of(), 1, 25, "threat", null, false));
    assertThat(resp.entityType()).isEqualTo("VIOLATION");
    assertThat(resp.rows()).isNotEmpty();
  }

  @Test
  public void violationFacets_includeStatesWaiverTypesStagesCategories() {
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of(), 1, 25, null, null, true));
    assertThat(resp.facets()).containsKeys(
        "organizations", "applications", "applicationCategories", "stages", "policyTypes", "states", "waiverType");

    Map<String, Long> states = countsByValue(resp, "states");
    assertThat(states).containsEntry("OPEN", 1L).containsEntry("WAIVED", 2L);

    Map<String, Long> waiverTypes = countsByValue(resp, "waiverType");
    assertThat(waiverTypes).containsEntry("AUTO", 1L).containsEntry("MANUAL", 1L);

    // Org/app facets bucket by name so the value round-trips back through the name-matching filter.
    assertThat(countsByValue(resp, "organizations")).containsKeys("Acme", "Widget Co");
    assertThat(countsByValue(resp, "applications")).containsKeys("Acme Prod", "Widget Inventory");
  }

  @Test
  public void violationOrgFacetValue_roundTripsBackThroughOrganizationsFilter() {
    IndexQueryResponse facetResp = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of(), 1, 25, null, null, true));
    String appBucketValue = facetResp.facets()
        .get("applications")
        .stream()
        .map(IndexQueryResponse.IndexQueryFacetBucket::value)
        .filter("Acme Prod"::equals)
        .findFirst()
        .orElseThrow();
    IndexQueryResponse filtered = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of("applications", List.of(appBucketValue)), 1, 25, null, null, false));
    assertThat(filtered.rows()).extracting(IndexQueryRow::getId).containsExactlyInAnyOrder("pv-open", "pv-manual");
  }

  @Test
  public void fixedStateFacet_isWholeCorpus_notSelfRestrictedByOwnStateFilter() {
    // User has narrowed to OPEN, yet the WAIVED bucket must still show the true whole-corpus count
    // (2) rather than self-restricting to 0 — otherwise the user cannot see there is anything to
    // switch back to. Likewise OPEN keeps its own count.
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of("states", List.of("OPEN")), 1, 25, null, null, true));
    Map<String, Long> states = countsByValue(resp, "states");
    assertThat(states).containsEntry("OPEN", 1L).containsEntry("WAIVED", 2L);
  }

  @Test
  public void fixedWaiverTypeFacet_isWholeCorpus_notSelfRestrictedByOwnWaiverTypeFilter() {
    // Selecting waiverType=AUTO must not zero out the MANUAL bucket count.
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(
        "VIOLATION", Map.of("waiverType", "AUTO"), 1, 25, null, null, true));
    Map<String, Long> waiverTypes = countsByValue(resp, "waiverType");
    assertThat(waiverTypes).containsEntry("AUTO", 1L).containsEntry("MANUAL", 1L);
  }

  // ---- helpers -----------------------------------------------------------------------------

  private static Map<String, Long> countsByValue(final IndexQueryResponse resp, final String facetKey) {
    Map<String, Long> out = new HashMap<>();
    resp.facets().get(facetKey).forEach(b -> out.put(b.value(), b.count()));
    return out;
  }

  private IndexQueryRow firstRow(final String entityType, final Map<String, Object> filters) {
    IndexQueryResponse resp = resource.query(new IndexQueryRequest(entityType, filters, 1, 25, null, null, false));
    assertThat(resp.rows()).isNotEmpty();
    return resp.rows().get(0);
  }

  private IndexQueryRow violationRowById(final String id) {
    IndexQueryResponse resp = resource.query(new IndexQueryRequest("VIOLATION", Map.of(), 1, 25, null, null, false));
    return resp.rows().stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow();
  }

  private long countReal(final String query) throws Exception {
    return searcher.count(conversionHelper.stringToQuery(query));
  }

  private GlobalSearchResult runRealSearch(final GlobalSearchRequest request) throws Exception {
    List<String> after = request.searchAfter();
    TopDocs all = searcher.search(request.baseQuery(), Math.max(1, reader.maxDoc()));
    int startDocExclusive = (after != null && !after.isEmpty()) ? Integer.parseInt(after.get(0)) : -1;
    List<ScoreDoc> ordered = new ArrayList<>();
    for (ScoreDoc sd : all.scoreDocs) {
      if (sd.doc > startDocExclusive) {
        ordered.add(sd);
      }
    }
    List<SearchResultItemDTO> rows = new ArrayList<>();
    int returnCount = Math.min(ordered.size(), request.pageSize());
    List<String> nextSearchAfter = List.of();
    for (int i = 0; i < returnCount; i++) {
      ScoreDoc hit = ordered.get(i);
      Document doc = searcher.storedFields().document(hit.doc);
      rows.add(new SearchResultItemDTO(doc));
      if (i == returnCount - 1 && ordered.size() > request.pageSize()) {
        nextSearchAfter = List.of(String.valueOf(hit.doc));
      }
    }
    return new GlobalSearchResult(rows, all.totalHits.value, nextSearchAfter);
  }

  private static Document appDoc(
      final String publicId,
      final String name,
      final String orgName,
      final String orgId,
      final List<String> categories,
      final Long lastEvalEpochMs,
      final List<String> stageSeverityTokens)
  {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_PUBLIC_ID.label, publicId, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_ID.label, publicId + "-appid", Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_NAME.label, name, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_ID.label, orgId, Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    for (String category : categories) {
      doc.add(new TextField(FieldIdentifier.APPLICATION_CATEGORY_NAME.label, category, Store.YES));
    }
    if (lastEvalEpochMs != null) {
      doc.add(new StoredField(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label, lastEvalEpochMs));
    }
    Set<String> activeStages = new LinkedHashSet<>();
    for (String token : stageSeverityTokens) {
      doc.add(new StringField(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label, token, Store.YES));
      activeStages.add(token.substring(0, token.indexOf(':')));
    }
    // Denormalized multi-valued keyword set backing the APPLICATION stages filter (one entry per
    // stage carrying an active violation), mirroring DocumentBuilderHelper's violations rollup.
    for (String stage : activeStages) {
      doc.add(new StringField(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label, stage, Store.YES));
    }
    return doc;
  }

  private static Document violationDoc(
      final String violationId,
      final String appName,
      final String orgName,
      final String policyName,
      final String threatCategory,
      final String waiverStatus,
      final String stageId,
      final int threatLevel,
      final String componentName,
      final String componentVersion,
      final List<String> categories)
  {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_VIOLATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_VIOLATION_ID.label, violationId, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_NAME.label, appName, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_ID.label, appName.toLowerCase().replace(' ', '-'), Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_ID.label, orgName.toLowerCase().replace(' ', '-'), Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_VIOLATION_POLICY_NAME.label, policyName, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label, threatCategory, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label, waiverStatus, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_EVALUATION_STAGE.label, stageId, Store.YES));
    doc.add(new StoredField(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label, threatLevel));
    doc.add(new TextField(FieldIdentifier.COMPONENT_NAME.label, componentName, Store.YES));
    doc.add(new TextField(FieldIdentifier.COMPONENT_FORMAT.label, "maven", Store.YES));
    doc.add(new TextField("componentCoordinateVersion", componentVersion, Store.YES));
    for (String category : categories) {
      doc.add(new TextField(FieldIdentifier.APPLICATION_CATEGORY_NAME.label, category, Store.YES));
    }
    return doc;
  }

  // ---- policyThreatLevel RANGE + facets regression (Lucene pointsConfig) -------------------

  /**
   * Regression: with a {@code policyThreatLevel} RANGE filter active AND facets requested, the
   * range clause lands in the whole-corpus facet base query. The facet counts are computed via the
   * real Lucene {@code newQueryParser()} points-config path, so if
   * {@code applicationMaxPolicyThreatLevel} were missing from {@code pointsConfigsByFieldName} the
   * {@code [7 TO 10]} clause would parse as a lexical term over an IntPoint-only field, matching
   * nothing and zeroing every facet count. Non-zero counts prove the points config is registered.
   */
  @Test
  public void applicationPolicyThreatLevelRange_withFacets_countsAreNotZeroed() throws Exception {
    try (Directory threatDir = new ByteBuffersDirectory()) {
      try (IndexWriter writer =
          new IndexWriter(threatDir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer())))
      {
        // Two Acme apps at/above the range, one below; one Widget app above.
        writer.addDocument(appThreatDoc("acme-hi", "Acme Hi", "Acme", 9));
        writer.addDocument(appThreatDoc("acme-mid", "Acme Mid", "Acme", 7));
        writer.addDocument(appThreatDoc("acme-lo", "Acme Lo", "Acme", 3));
        writer.addDocument(appThreatDoc("widget-hi", "Widget Hi", "Widget Co", 8));
        writer.commit();
      }
      try (IndexReader threatReader = DirectoryReader.open(threatDir)) {
        IndexSearcher threatSearcher = new IndexSearcher(threatReader);
        ConversionHelper realConversion = new ConversionHelper(new LuceneComponents(mock(InsightWork.class)));

        SearchIndexClient client = mock(SearchIndexClient.class);
        when(client.isSearchPreviewEnabled()).thenReturn(true);
        when(client.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("org-1"));
        when(client.buildAllowedContextIdsFilter(any())).thenReturn(null);
        when(client.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(client.buildPermittedQuery(any())).thenCallRealMethod();
        when(client.getLastIndexTime()).thenReturn(1000L);
        when(client.backendId()).thenReturn("lucene");
        when(client.searchGlobal(any(GlobalSearchRequest.class)))
            .thenAnswer(inv -> runRealSearchOn(threatSearcher, threatReader, inv.getArgument(0)));
        when(client.count(any()))
            .thenAnswer(inv -> (long) threatSearcher.count(realConversion.stringToQuery(inv.getArgument(0))));

        IqLocalSearchService iq = new IqLocalSearchService(client);
        IndexQueryResource threatResource = new IndexQueryResource(new IndexQueryService(iq, client, null), client);

        // policyThreatLevel=[7,10] narrows result rows to acme-hi(9), acme-mid(7), widget-hi(8).
        IndexQueryResponse resp = threatResource.query(new IndexQueryRequest(
            "APPLICATION", Map.of("policyThreatLevel", List.of(7, 10)), 1, 25, null, null, true));
        assertThat(resp.rows()).extracting(IndexQueryRow::getId)
            .containsExactlyInAnyOrder("acme-hi", "acme-mid", "widget-hi");

        // The whole-corpus org facet is counted against a base that carries the RANGE clause. Before
        // the points-config fix these counts collapse to 0; after it, Acme=2 and Widget Co=1.
        Map<String, Long> orgCounts = countsByValue(resp, "organizations");
        assertThat(orgCounts).containsEntry("Acme", 2L).containsEntry("Widget Co", 1L);
      }
    }
  }

  private static GlobalSearchResult runRealSearchOn(
      final IndexSearcher altSearcher,
      final IndexReader altReader,
      final GlobalSearchRequest request) throws Exception
  {
    List<String> after = request.searchAfter();
    TopDocs all = altSearcher.search(request.baseQuery(), Math.max(1, altReader.maxDoc()));
    int startDocExclusive = (after != null && !after.isEmpty()) ? Integer.parseInt(after.get(0)) : -1;
    List<ScoreDoc> ordered = new ArrayList<>();
    for (ScoreDoc sd : all.scoreDocs) {
      if (sd.doc > startDocExclusive) {
        ordered.add(sd);
      }
    }
    List<SearchResultItemDTO> rows = new ArrayList<>();
    int returnCount = Math.min(ordered.size(), request.pageSize());
    List<String> nextSearchAfter = List.of();
    for (int i = 0; i < returnCount; i++) {
      ScoreDoc hit = ordered.get(i);
      rows.add(new SearchResultItemDTO(altSearcher.storedFields().document(hit.doc)));
      if (i == returnCount - 1 && ordered.size() > request.pageSize()) {
        nextSearchAfter = List.of(String.valueOf(hit.doc));
      }
    }
    return new GlobalSearchResult(rows, all.totalHits.value, nextSearchAfter);
  }

  private static Document appThreatDoc(
      final String publicId,
      final String name,
      final String orgName,
      final int maxThreatLevel)
  {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_PUBLIC_ID.label, publicId, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_ID.label, publicId + "-appid", Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_NAME.label, name, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new IntPoint(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label, maxThreatLevel));
    doc.add(new StoredField(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label, maxThreatLevel));
    return doc;
  }
}
