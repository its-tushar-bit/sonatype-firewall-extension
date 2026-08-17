/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.integration.OrganizationSummaryService;
import com.sonatype.insight.brain.model.Organization;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LowerCaseKeywordAnalyzer;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.IndexTermsBucket;
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
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  private IndexReadSessionFactory sessionFactory;

  private IndexReadSession session;

  private IndexQueryResource resource;

  private OrganizationSummaryService organizationSummaryService;

  @BeforeEach
  public void setUp() throws Exception {
    // The organizations facet resolves bucket names through an @AuthzFilter-woven call, so a Shiro
    // SecurityManager must be reachable from this thread. A null principal makes the filter pass every
    // organization through, leaving the read gate to the mocked OrganizationSummaryService below.
    ThreadContext.bind(mock(SecurityManager.class));
    ThreadContext.bind(mock(Subject.class));

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

    // Mock session factory for VALUE facet termsAggregation
    sessionFactory = mock(IndexReadSessionFactory.class);
    session = mock(IndexReadSession.class);
    when(sessionFactory.open()).thenReturn(session);
    // Bounded-vocabulary facets only. The org/app/category facets aggregate on opaque ids
    // (parentOrganizationId / applicationId / applicationCategoryId) and resolve display names from the
    // DAOs, which a stub cannot model faithfully; their bucket values are asserted against a real index
    // in IndexQueryServiceFacetsRealIndexTest instead. Stubbing them here would only assert the stub.
    when(session.termsAggregation(any(), anyString(), anyInt())).thenAnswer(inv -> {
      String field = inv.getArgument(1);
      java.util.List<IndexTermsBucket> buckets = new java.util.ArrayList<>();
      switch (field) {
        case "applicationViolationStage":
          buckets.add(new IndexTermsBucket("build", 3L));
          buckets.add(new IndexTermsBucket("release", 1L));
          buckets.add(new IndexTermsBucket("develop", 1L));
          break;
        case "applicationViolationPolicyType":
          buckets.add(new IndexTermsBucket("security", 2L));
          buckets.add(new IndexTermsBucket("license", 1L));
          buckets.add(new IndexTermsBucket("quality", 1L));
          break;
        case "applicationViolationState":
          buckets.add(new IndexTermsBucket("open", 2L));
          buckets.add(new IndexTermsBucket("waived", 1L));
          buckets.add(new IndexTermsBucket("AutoWaived", 1L));
          break;
        case "policyEvaluationStage":
          buckets.add(new IndexTermsBucket("build", 2L));
          buckets.add(new IndexTermsBucket("release", 1L));
          break;
        case "policyViolationThreatCategory":
          buckets.add(new IndexTermsBucket("security", 2L));
          buckets.add(new IndexTermsBucket("license", 1L));
          buckets.add(new IndexTermsBucket("quality", 1L));
          break;
        default:
          // Including the id-aggregated facets: no buckets, so the facet key is still present and empty.
          break;
      }
      return buckets;
    });

    IqLocalSearchService iq = new IqLocalSearchService(searchIndexClient);

    OrganizationDAO organizationDAO = mock(OrganizationDAO.class);
    Organization rootOrg = mock(Organization.class);
    when(rootOrg.getName()).thenReturn("Root Org");
    when(organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID)).thenReturn(rootOrg);

    organizationSummaryService = mock(OrganizationSummaryService.class);
    lenient().when(organizationSummaryService.getOrganizationsForRead(anySet())).thenAnswer(inv -> {
      Set<String> ids = inv.getArgument(0);
      return ids.stream().map(id -> {
        Organization o = new Organization();
        o.setId(id);
        return o;
      }).toList();
    });

    IndexQueryService service =
        new IndexQueryService(organizationDAO, mock(ApplicationDAO.class), mock(TagDAO.class), mock(PolicyDAO.class),
            iq, searchIndexClient, sessionFactory, conversionHelper, organizationSummaryService, null);
    resource = new IndexQueryResource(service, searchIndexClient);
  }

  @AfterEach
  public void tearDown() throws Exception {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
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
    // Org/app/category facet bucket VALUES (id-keyed, with a resolved displayName) and the
    // organizationIds/applicationIds/applicationCategoryIds round-trip are covered by the real-index
    // service-layer tests (see IndexQueryServiceFacetsRealIndexTest, CLM-45220), not this mock-based
    // endpoint test -- the mocked IndexReadSession#termsAggregation here does not model real
    // aggregation, so asserting specific bucket values against it only tests the stub.
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

    // Org/app/category facet bucket VALUES (id-keyed) and the organizationIds/applicationIds/
    // applicationCategoryIds round-trip are covered by the real-index service-layer tests (see
    // IndexQueryServiceFacetsRealIndexTest, CLM-45220), not this mock-based endpoint test.
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
   * Regression: a {@code policyThreatLevel} RANGE filter compiles against
   * {@code applicationMaxPolicyThreatLevel}, a real Lucene {@code newQueryParser()} points-config
   * path -- if that field were missing from {@code pointsConfigsByFieldName}, the {@code [7 TO 10]}
   * clause would parse as a lexical term over an IntPoint-only field, matching nothing and dropping
   * every row instead of narrowing to the in-range ones. The row assertion below exercises the real
   * (non-mocked) Lucene searcher, so it fails if the points config regresses.
   * <p>
   * This test previously also asserted the whole-corpus org facet count stayed non-zero under the
   * same RANGE base query; that assertion is removed (CLM-44713 slice 2b) because
   * {@link IndexReadSession#termsAggregation} is mocked here and ignores the query it is passed, so
   * it could never actually detect a broken points config -- facet-count correctness under a RANGE
   * filter is covered by the real-index service-layer tests instead (see
   * {@code IndexQueryServiceFacetsRealIndexTest}, CLM-45220).
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

        OrganizationDAO organizationDAO = mock(OrganizationDAO.class);
        Organization rootOrg = mock(Organization.class);
        when(rootOrg.getName()).thenReturn("Root Org");
        when(organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID)).thenReturn(rootOrg);

        IndexQueryResource threatResource =
            new IndexQueryResource(
                new IndexQueryService(organizationDAO, mock(ApplicationDAO.class), mock(TagDAO.class),
                    mock(PolicyDAO.class), iq, client, sessionFactory, conversionHelper, organizationSummaryService,
                    null),
                client);

        // policyThreatLevel=[7,10] narrows result rows to acme-hi(9), acme-mid(7), widget-hi(8).
        IndexQueryResponse resp = threatResource.query(new IndexQueryRequest(
            "APPLICATION", Map.of("policyThreatLevel", List.of(7, 10)), 1, 25, null, null, true));
        assertThat(resp.rows()).extracting(IndexQueryRow::getId)
            .containsExactlyInAnyOrder("acme-hi", "acme-mid", "widget-hi");
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
