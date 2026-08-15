/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.global.FilterValidationException;
import com.sonatype.insight.brain.search.global.FilterValidationExceptionMapper;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import jakarta.ws.rs.core.Response;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.PolicyWaiverExpiryStatuses;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LowerCaseKeywordAnalyzer;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.session.IndexReadSession;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class IndexQueryEndpointTest
{
  /** A year either side of the captured "now" keeps the active/expired classification stable regardless of run time. */
  private static final Duration EXPIRY_OFFSET = Duration.ofDays(365);

  /** Well past "now" (active); set in {@link #setUp} against the same clock the expiry filter reads at request time. */
  private long futureExpiryMs;

  /**
   * Well before "now" (expired); set in {@link #setUp} against the same clock the expiry filter reads at request time.
   */
  private long pastExpiryMs;

  private SearchIndexClient searchIndexClient;

  private Directory directory;

  private IndexReader reader;

  private IndexSearcher searcher;

  private IndexReadSessionFactory sessionFactory;

  private IndexReadSession session;

  private ConversionHelper conversionHelper;

  private OrganizationSummaryService organizationSummaryService;

  private IndexQueryResource resource;

  @Before
  public void setUp() throws Exception {
    // The organizations facet resolves bucket names through an @AuthzFilter-woven call, so a Shiro
    // SecurityManager must be reachable from this thread. A null principal makes the filter pass every
    // organization through, leaving the read gate to the mocked OrganizationSummaryService.
    ThreadContext.bind(mock(SecurityManager.class));
    ThreadContext.bind(mock(Subject.class));

    SystemConfigurationPropertyFeatureTestSupport.install();
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    final Instant now = Instant.now();
    futureExpiryMs = now.plus(EXPIRY_OFFSET).toEpochMilli();
    pastExpiryMs = now.minus(EXPIRY_OFFSET).toEpochMilli();

    directory = new ByteBuffersDirectory();
    Map<String, Analyzer> perField = new HashMap<>();
    // allowedContextIds is a case-sensitive keyword in production (no lowercase normalizer) so the
    // TermInSetQuery permission clause matches the raw context id byte-for-byte. Index it the same
    // way here (plain KeywordAnalyzer), otherwise the default lowercasing would break the match.
    perField.put(FieldIdentifier.ALLOWED_CONTEXT_IDS.label, new KeywordAnalyzer());
    // Ana expiryStatus vocabulary is case-sensitive Active/Expired/Never (OpenSearch keyword).
    perField.put(FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label, new KeywordAnalyzer());
    PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), perField);
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
      writer.addDocument(appDoc("acme-prod", "Acme Prod", "Acme"));
      writer.addDocument(appDoc("acme-dev", "Acme Dev", "Acme"));
      writer.addDocument(appDoc("widget-co", "Widget Inventory", "Widget Co"));
      writer.addDocument(policyViolationDoc("pv-1", "Acme Prod", "Acme"));
      writer.addDocument(policyViolationDoc("pv-2", "Widget Inventory", "Widget Co"));
      // w-acme-1: future expiry (active). w-widget-1: past expiry (expired). Others: no expiry (active).
      writer.addDocument(manualWaiverDoc("w-acme-1", "Security High", "Acme", 8, "alice", futureExpiryMs));
      writer.addDocument(manualWaiverDoc("w-acme-2", "License Copyleft", "Acme", 4, "bob", null));
      writer.addDocument(manualWaiverDoc("w-widget-1", "Security High", "Widget Co", 9, "carol", pastExpiryMs));
      // App-scoped manual waiver owned by the Acme Prod application (carries applicationName/Id).
      writer.addDocument(appScopedWaiverDoc("w-acme-app-1", "Security High", "Acme", "Acme Prod", "acme-prod", 6));
      writer.addDocument(autoWaiverDoc("w-auto-1", "Acme", 10));
      // POLICY rows for the waiverCount aggregation: pol-sec-high (has waivers), pol-orphan (none).
      writer.addDocument(policyDoc("pol-sec-high", "Security High", "Acme", 8));
      writer.addDocument(policyDoc("pol-orphan", "Unused Policy", "Acme", 3));
      writer.commit();
    }
    reader = DirectoryReader.open(directory);
    searcher = new IndexSearcher(reader);

    searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    // Default: global read access (no permission narrowing). The RBAC test overrides these three
    // stubs to drive the real production permission clause instead.
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("org-1"));
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(searchIndexClient.getLastIndexTime()).thenReturn(1000L);
    when(searchIndexClient.backendId()).thenReturn("lucene");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenAnswer(inv -> runRealSearch(inv.getArgument(0)));

    sessionFactory = mock(IndexReadSessionFactory.class);
    session = mock(IndexReadSession.class);
    conversionHelper = mock(ConversionHelper.class);
    when(sessionFactory.open()).thenReturn(session);
    when(conversionHelper.stringToQuery(anyString())).thenReturn(new org.apache.lucene.search.MatchAllDocsQuery());
    when(session.termsAggregation(any(), anyString(), anyInt())).thenReturn(List.of());

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

    IqLocalSearchService iq = new IqLocalSearchService(searchIndexClient);
    IndexQueryService service =
        new IndexQueryService(organizationDAO, mock(ApplicationDAO.class), mock(TagDAO.class), mock(PolicyDAO.class),
            iq, searchIndexClient, sessionFactory, conversionHelper, organizationSummaryService, null);
    resource = new IndexQueryResource(service, searchIndexClient);
  }

  @After
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

  @Test
  public void applicationQuery_acmeQuery_returnsAcmeApps() {
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of("query", "acme"), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.entityType()).isEqualTo("APPLICATION");
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("acme-prod", "acme-dev");
    assertThat(response.rows()).allSatisfy(r -> assertThat(r.getSource()).isEqualTo("local"));
  }

  @Test
  public void applicationQuery_organizationFilter_narrowsResults() {
    IndexQueryRequest req = new IndexQueryRequest(
        "APPLICATION", Map.of("organizations", List.of("Widget Co")), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId).containsExactly("widget-co");
  }

  /**
   * Full-path E2E of the Applications aggregate filters (A1/A2/A3/A4) against a self-contained index of
   * aggregate-bearing app docs: TERMS OR-within a filter, TERMS AND-across distinct filters, and the
   * max-threat RANGE. Runs through the real resource -> service -> compiler -> Lucene search path.
   */
  @Test
  public void applicationQuery_aggregateFilters_termsAndRange() throws Exception {
    try (Directory aggDir = new ByteBuffersDirectory()) {
      try (IndexWriter writer = new IndexWriter(aggDir, new IndexWriterConfig(new LowerCaseKeywordAnalyzer()))) {
        // app-a: BUILD, security, open, threat 9. app-b: RELEASE, license, waived, threat 4.
        // app-c: BUILD, quality, legacy, threat 7.
        writer.addDocument(appAggDoc("app-a", "App A", "Acme",
            List.of("build"), List.of("security"), List.of("open"), 9));
        writer.addDocument(appAggDoc("app-b", "App B", "Acme",
            List.of("release"), List.of("license"), List.of("waived"), 4));
        writer.addDocument(appAggDoc("app-c", "App C", "Acme",
            List.of("build"), List.of("quality"), List.of("legacy"), 7));
        writer.commit();
      }
      try (IndexReader aggReader = DirectoryReader.open(aggDir)) {
        IndexQueryResource aggResource = resourceOver(aggReader);

        // A1 TERMS OR-within: stages=[build,release] -> app-a, app-b, app-c (build OR release).
        assertThat(idsOf(aggResource, Map.of("stages", List.of("build", "release"))))
            .containsExactlyInAnyOrder("app-a", "app-b", "app-c");
        // A1 single: stages=[build] -> app-a, app-c.
        assertThat(idsOf(aggResource, Map.of("stages", List.of("build"))))
            .containsExactlyInAnyOrder("app-a", "app-c");
        // A2 policyTypes=[security] -> app-a.
        assertThat(idsOf(aggResource, Map.of("policyTypes", List.of("security")))).containsExactly("app-a");
        // A3 violationStates=[waived,legacy] -> app-b, app-c.
        assertThat(idsOf(aggResource, Map.of("violationStates", List.of("waived", "legacy"))))
            .containsExactlyInAnyOrder("app-b", "app-c");
        // A4 RANGE policyThreatLevel=[7,10] -> app-a(9), app-c(7).
        assertThat(idsOf(aggResource, Map.of("policyThreatLevel", List.of(7, 10))))
            .containsExactlyInAnyOrder("app-a", "app-c");
        // AND-across distinct filters: stages=[build] AND policyTypes=[quality] -> app-c only.
        assertThat(idsOf(aggResource, Map.of("stages", List.of("build"), "policyTypes", List.of("quality"))))
            .containsExactly("app-c");
      }
    }
  }

  private List<String> idsOf(final IndexQueryResource res, final Map<String, Object> filters) {
    IndexQueryResponse response = res.query(new IndexQueryRequest("APPLICATION", filters, 1, 25, null, null, false));
    return response.rows().stream().map(IndexQueryRow::getId).toList();
  }

  /** Rebuilds the resource stack over an alternate reader so a test can drive its own fixture index. */
  private IndexQueryResource resourceOver(final IndexReader altReader) {
    IndexSearcher altSearcher = new IndexSearcher(altReader);
    SearchIndexClient altClient = mock(SearchIndexClient.class);
    when(altClient.isSearchPreviewEnabled()).thenReturn(true);
    when(altClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("org-1"));
    when(altClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(altClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(altClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(altClient.getLastIndexTime()).thenReturn(1000L);
    when(altClient.backendId()).thenReturn("lucene");
    when(altClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenAnswer(inv -> runRealSearchOn(altSearcher, altReader, inv.getArgument(0)));
    IqLocalSearchService iq = new IqLocalSearchService(altClient);
    OrganizationDAO organizationDAO = mock(OrganizationDAO.class);
    Organization rootOrg = mock(Organization.class);
    when(rootOrg.getName()).thenReturn("Root Org");
    when(organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID)).thenReturn(rootOrg);
    return new IndexQueryResource(
        new IndexQueryService(organizationDAO, mock(ApplicationDAO.class), mock(TagDAO.class), mock(PolicyDAO.class),
            iq, altClient, sessionFactory, conversionHelper, organizationSummaryService, null),
        altClient);
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

  private static Document appAggDoc(
      final String publicId,
      final String name,
      final String orgName,
      final List<String> stages,
      final List<String> policyTypes,
      final List<String> states,
      final int maxThreatLevel)
  {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_PUBLIC_ID.label, publicId, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_ID.label, publicId, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_NAME.label, name, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    for (String stage : stages) {
      doc.add(new StringField(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label, stage, Store.YES));
    }
    for (String type : policyTypes) {
      doc.add(new StringField(FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label, type, Store.YES));
    }
    for (String state : states) {
      doc.add(new StringField(FieldIdentifier.APPLICATION_VIOLATION_STATE.label, state, Store.YES));
    }
    doc.add(new IntPoint(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label, maxThreatLevel));
    doc.add(new StoredField(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label, maxThreatLevel));
    return doc;
  }

  // Org facet bucket VALUES (the whole-corpus counts, id-keyed) are covered against a real index in
  // IndexQueryServiceFacetsRealIndexTest, not here: the mocked IndexReadSession#termsAggregation in this
  // endpoint test does not model real aggregation, so asserting bucket values would only test the stub.

  @Test
  public void applicationQuery_defaultView_neverAddsAutoWaiverRestriction() {
    // Only WAIVER carries an AUTO_WAIVER_TOGGLE entry, so a non-WAIVER default view must never emit the
    // manual-only policyWaiverAuto:"false" clause. If it leaked into the page query or a facet base, an
    // APPLICATION view would silently narrow to manually-waived data.
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    resource.query(req);

    ArgumentCaptor<GlobalSearchRequest> pageRequests = ArgumentCaptor.forClass(GlobalSearchRequest.class);
    verify(searchIndexClient, atLeastOnce()).searchGlobal(pageRequests.capture());
    assertThat(pageRequests.getAllValues())
        .isNotEmpty()
        .allSatisfy(request -> assertThat(request.baseQuery().toString())
            .as("APPLICATION page query must not carry the waiver auto/manual restriction")
            .doesNotContain("policyWaiverAuto"));

    // The fixed-vocabulary facet bases are the other place the clause could appear; APPLICATION has none
    // of those facets, so no counted base may mention it either.
    verify(searchIndexClient, never()).count(contains("policyWaiverAuto"));
  }

  @Test
  public void applicationQuery_pageSizeOne_pagesWithCursor() {
    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 1, null, null, false);
    IndexQueryResponse first = resource.query(req);
    assertThat(first.rows()).hasSize(1);
    assertThat(first.nextSearchAfter()).isNotBlank();

    IndexQueryRequest next = new IndexQueryRequest("APPLICATION", Map.of(), 2, 1, null, first.nextSearchAfter(), false);
    IndexQueryResponse second = resource.query(next);
    assertThat(second.rows()).hasSize(1);
    assertThat(second.rows().get(0).getId()).isNotEqualTo(first.rows().get(0).getId());
  }

  @Test
  public void violationQuery_applicationNameFilter_returnsMatchingViolation() {
    // Regression: applicationName is registered in FieldMap only for APPLICATION until widened to the
    // violation types; without the widening the whole VIOLATION request compiles to MatchNoDocsQuery
    // and silently returns zero rows.
    IndexQueryRequest req = new IndexQueryRequest(
        "VIOLATION", Map.of("applications", List.of("Acme Prod")), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.entityType()).isEqualTo("VIOLATION");
    assertThat(response.rows()).extracting(IndexQueryRow::getId).containsExactly("pv-1");
  }

  @Test
  public void waiverQuery_returnsWaiverRowsWithExpectedFields() {
    // includeAutoWaivers Classic default is both kinds; still pass true explicitly for clarity.
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("includeAutoWaivers", true), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.entityType()).isEqualTo("WAIVER");
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("w-acme-1", "w-acme-2", "w-widget-1", "w-acme-app-1", "w-auto-1");
    assertThat(response.rows()).allSatisfy(r -> assertThat(r.getSource()).isEqualTo("local"));

    IndexQueryRow manual = response.rows()
        .stream()
        .filter(r -> r.getId().equals("w-acme-1"))
        .findFirst()
        .orElseThrow();
    assertThat(manual.getTitle()).isEqualTo("Security High");
    // Org-scoped waiver subtitle is the owning organization's display name, not the raw scope enum.
    assertThat(manual.getSubtitle()).isEqualTo("Acme");
    assertThat(manual.getFields()).containsEntry("policyName", "Security High")
        .containsEntry("threatLevel", 8)
        .containsEntry("waivedBy", "alice")
        .containsEntry("reason", "waived for release")
        .containsEntry("auto", false)
        .containsEntry("scopeOwnerType", "ORGANIZATION");

    // App-scoped waiver subtitle is the owning application's display name.
    IndexQueryRow appScoped = response.rows()
        .stream()
        .filter(r -> r.getId().equals("w-acme-app-1"))
        .findFirst()
        .orElseThrow();
    assertThat(appScoped.getSubtitle()).isEqualTo("Acme Prod");

    IndexQueryRow auto = response.rows()
        .stream()
        .filter(r -> r.getId().equals("w-auto-1"))
        .findFirst()
        .orElseThrow();
    assertThat(auto.getFields()).containsEntry("auto", true);
    // Auto-waiver title is synthesized on the read side (not indexed); policyName field stays null.
    assertThat(auto.getTitle()).isEqualTo("Auto-waiver (threat >= 10)");
    assertThat(auto.getFields().get("policyName")).isNull();
  }

  @Test
  public void waiverQuery_policyFilter_doesNotMatchSyntheticAutoWaiverTitle() {
    // The synthetic "Auto-waiver ..." label is composed on the read side and never indexed, so the
    // policy filter (on the indexed policyWaiverPolicyName) cannot match an auto waiver by it.
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("policy", List.of("Auto-waiver (threat >= 10)"), "includeAutoWaivers", true),
        1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId).doesNotContain("w-auto-1");
    assertThat(response.rows()).isEmpty();
  }

  @Test
  public void waiverQuery_freeTextQuery_doesNotMatchSyntheticAutoWaiverTitle() {
    // Free-text search over the indexed fields must not match the read-side synthetic label either.
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("query", "Auto-waiver", "includeAutoWaivers", true), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId).doesNotContain("w-auto-1");
  }

  @Test
  public void waiverQuery_organizationsFilter_narrowsToScopedOrg() {
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("organizations", List.of("Widget Co")), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId).containsExactly("w-widget-1");
  }

  @Test
  public void waiverQuery_policyThreatLevelRange_narrowsByThreat() {
    // Classic absent includeAutoWaivers includes both kinds; threat range [7,10] keeps high-threat
    // manuals plus auto w-auto-1 (threat 10) and drops w-acme-2 (threat 4).
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("policyThreatLevel", List.of(7, 10)), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("w-acme-1", "w-widget-1", "w-auto-1");
  }

  @Test
  public void waiverQuery_includeAutoWaiversTrue_returnsBothKinds() {
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("includeAutoWaivers", true), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("w-acme-1", "w-acme-2", "w-widget-1", "w-acme-app-1", "w-auto-1");
  }

  @Test
  public void waiverQuery_includeAutoWaiversFalse_returnsManualOnly() {
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("includeAutoWaivers", false), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("w-acme-1", "w-acme-2", "w-widget-1", "w-acme-app-1");
    assertThat(response.rows()).extracting(IndexQueryRow::getId).doesNotContain("w-auto-1");
  }

  @Test
  public void waiverQuery_includeAutoWaiversAbsent_includesBothKinds() {
    // Classic: omitting includeAutoWaivers includes both manual and auto waivers.
    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("w-acme-1", "w-acme-2", "w-widget-1", "w-acme-app-1", "w-auto-1");
  }

  @Test
  public void waiverQuery_includeAutoWaiversExplicitNull_includesBothKinds() {
    // Classic: explicit JSON null behaves like omitting the key — both kinds.
    Map<String, Object> filters = new HashMap<>();
    filters.put("includeAutoWaivers", null);
    IndexQueryRequest req = new IndexQueryRequest("WAIVER", filters, 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("w-acme-1", "w-acme-2", "w-widget-1", "w-acme-app-1", "w-auto-1");
  }

  @Test
  public void waiverQuery_includeAutoWaiversNonBoolean_mapsTo400() {
    assertMappedStatus(
        new IndexQueryRequest("WAIVER", Map.of("includeAutoWaivers", "yes"), 1, 25, null, null, false), 400);
  }

  @Test
  public void waiverQuery_isAutoTrue_returnsAutoOnly() {
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("isAuto", List.of("true")), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId).containsExactly("w-auto-1");
  }

  @Test
  public void waiverQuery_expiryStatusActive_includesNeverAndExcludesExpired() {
    // active expands to active∪never so permanent waivers are not hidden (parity with expiry:"active").
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("expiryStatus", List.of(PolicyWaiverExpiryStatuses.ACTIVE)), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("w-acme-1", "w-acme-2", "w-acme-app-1", "w-auto-1")
        .doesNotContain("w-widget-1");
  }

  // WAIVER org facet bucket VALUES (the whole-corpus counts, now id-keyed on parentOrganizationId)
  // are covered by the real-index service-layer tests (see IndexQueryServiceFacetsRealIndexTest,
  // CLM-45220), not this mock-based endpoint test. (Removed
  // waiverQuery_facetsRequested_areWholeCorpusOrgCounts, CLM-44713 slice 2b.)

  @Test
  public void waiverQuery_applicationsFilter_narrowsToAppScopedWaiver() {
    // App-scoped w-acme-app-1 carries applicationName "Acme Prod"; org-scoped waivers carry none, so
    // the applications filter narrows to the app-scoped waiver only (org-scoped excluded).
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("applications", List.of("Acme Prod")), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId).containsExactly("w-acme-app-1");
  }

  @Test
  public void waiverQuery_applicationIdFilter_narrowsToAppScopedWaiver() {
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("applicationId", List.of("acme-prod")), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId).containsExactly("w-acme-app-1");
  }

  @Test
  public void waiverQuery_policyFilter_narrowsByPolicyName() {
    // includeAutoWaivers true, but auto w-auto-1 carries no indexed policy name, so it does not match.
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("policy", List.of("Security High"), "includeAutoWaivers", true), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    // Security High manual waivers across scopes: w-acme-1 (org), w-widget-1 (org), w-acme-app-1 (app).
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("w-acme-1", "w-widget-1", "w-acme-app-1");
  }

  @Test
  public void waiverQuery_expiryActive_excludesExpiredWaivers() {
    // includeAutoWaivers true to cover both kinds. w-widget-1 has a past expiry (expired); everything
    // else either has a future expiry (w-acme-1) or no expiry (never expires) -> all active.
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("expiry", "active", "includeAutoWaivers", true), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("w-acme-1", "w-acme-2", "w-acme-app-1", "w-auto-1");
    assertThat(response.rows()).extracting(IndexQueryRow::getId).doesNotContain("w-widget-1");
  }

  @Test
  public void waiverQuery_expiryExpired_returnsOnlyPastExpiryWaivers() {
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("expiry", "expired", "includeAutoWaivers", true), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    // Only w-widget-1 has a past expiry; never-expiring (null) docs are treated as active, not expired.
    assertThat(response.rows()).extracting(IndexQueryRow::getId).containsExactly("w-widget-1");
  }

  @Test
  public void waiverQuery_expiryInvalidValue_mapsTo400() {
    assertMappedStatus(
        new IndexQueryRequest("WAIVER", Map.of("expiry", "soon"), 1, 25, null, null, false), 400);
  }

  @Test
  public void waiverQuery_facetsRequested_includeAutoManualBuckets() {
    // includeAutoWaivers true so both auto and manual rows seed the "auto" facet buckets (true/false).
    when(searchIndexClient.count(org.mockito.ArgumentMatchers.contains("policyWaiverAuto:\"true\"")))
        .thenReturn(3L);
    when(searchIndexClient.count(org.mockito.ArgumentMatchers.contains("policyWaiverAuto:\"false\"")))
        .thenReturn(7L);
    // NUMERIC facet via aggregateCountByField
    when(searchIndexClient.aggregateCountByField(any(), eq("policyWaiverThreatLevel"), any()))
        .thenReturn(new com.sonatype.insight.brain.search.index.MetricAggregationResult(0L, Map.of()));

    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("includeAutoWaivers", true), 1, 25, null, null, true);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.facets()).isNotNull().containsKey("auto");
    Map<String, Long> autoCounts = new HashMap<>();
    response.facets().get("auto").forEach(b -> autoCounts.put(b.value(), b.count()));
    assertThat(autoCounts).containsEntry("true", 3L).containsEntry("false", 7L);
  }

  @Test
  public void waiverQuery_facetsDefaultView_autoFacetIsWholeCorpus() {
    // Classic both-kinds default view: the auto/manual facet still reports BOTH true and false counts
    // over the whole corpus (it tells the user what flipping the include toggle would show), so its
    // count base must NOT inherit the default policyWaiverAuto:"false" exclusion clause.
    when(searchIndexClient.count(contains("policyWaiverAuto:\"true\"")))
        .thenReturn(4L);
    when(searchIndexClient.count(contains("policyWaiverAuto:\"false\"")))
        .thenReturn(9L);
    // NUMERIC facet via aggregateCountByField
    when(searchIndexClient.aggregateCountByField(any(), eq("policyWaiverThreatLevel"), any()))
        .thenReturn(new com.sonatype.insight.brain.search.index.MetricAggregationResult(0L, Map.of()));

    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.facets()).isNotNull().containsKey("auto");
    assertAutoFacetBaseDropsManualRestriction();
    Map<String, Long> autoCounts = new HashMap<>();
    response.facets().get("auto").forEach(b -> autoCounts.put(b.value(), b.count()));
    // Both buckets present with real whole-corpus counts even though the page shows only manual rows.
    assertThat(autoCounts).containsEntry("true", 4L).containsEntry("false", 9L);
  }

  @Test
  public void waiverQuery_facetsExplicitIncludeAutoWaiversFalse_autoFacetIsWholeCorpus() {
    // Explicit includeAutoWaivers:false restricts rows to manual only.
    // The auto/manual facet must still report BOTH buckets over the whole corpus: the explicit-false
    // restriction must be dropped from the facet base too, not only the default one.
    when(searchIndexClient.count(contains("policyWaiverAuto:\"true\"")))
        .thenReturn(5L);
    when(searchIndexClient.count(contains("policyWaiverAuto:\"false\"")))
        .thenReturn(11L);
    // NUMERIC facet via aggregateCountByField
    when(searchIndexClient.aggregateCountByField(any(), eq("policyWaiverThreatLevel"), any()))
        .thenReturn(new com.sonatype.insight.brain.search.index.MetricAggregationResult(0L, Map.of()));

    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("includeAutoWaivers", false), 1, 25, null, null, true);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.facets()).isNotNull().containsKey("auto");
    assertAutoFacetBaseDropsManualRestriction();
    Map<String, Long> autoCounts = new HashMap<>();
    response.facets().get("auto").forEach(b -> autoCounts.put(b.value(), b.count()));
    // The "true" bucket reports the whole-corpus auto count, not 0, despite the explicit-false toggle.
    assertThat(autoCounts).containsEntry("true", 5L).containsEntry("false", 11L);
  }

  /**
   * Captures every {@link SearchIndexClient#count(String)} query and asserts the auto/manual facet base
   * had the manual-only {@code policyWaiverAuto:"false"} restriction stripped. The auto "true" bucket is
   * built as {@code <facetBase> AND policyWaiverAuto:"true"}; if the base still carried the manual-only
   * restriction the query would read {@code ... policyWaiverAuto:"false" ... AND policyWaiverAuto:"true"},
   * which counts 0. A {@code contains}-based stub cannot see that -- it would match either query and let
   * an un-dropped restriction pass -- so we assert on the captured string directly.
   */
  private void assertAutoFacetBaseDropsManualRestriction() {
    ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
    verify(searchIndexClient, atLeastOnce()).count(queries.capture());
    String autoTrueBucketQuery = queries.getAllValues()
        .stream()
        .filter(q -> q.contains("policyWaiverAuto:\"true\""))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no count() query built the auto 'true' bucket"));
    assertThat(autoTrueBucketQuery)
        .as("auto/manual facet base must drop the manual-only restriction so it counts the whole corpus")
        .doesNotContain("policyWaiverAuto:\"false\"");
  }

  @Test
  public void waiverQuery_rowsCarryWaiverDetailHref() {
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("includeAutoWaivers", true), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);

    // Org-scoped manual waiver: /preview/waivers/{ownerType-lowercased}/{ownerId}/{waiverId}.
    IndexQueryRow orgScoped = response.rows()
        .stream()
        .filter(r -> r.getId().equals("w-acme-1"))
        .findFirst()
        .orElseThrow();
    assertThat(orgScoped.getHref()).isEqualTo("/preview/waivers/organization/org-w-acme-1/w-acme-1");

    // App-scoped waiver carries its application owner id.
    IndexQueryRow appScoped = response.rows()
        .stream()
        .filter(r -> r.getId().equals("w-acme-app-1"))
        .findFirst()
        .orElseThrow();
    assertThat(appScoped.getHref()).isEqualTo("/preview/waivers/application/acme-prod/w-acme-app-1");

    // Auto waiver carries the same scope owner id/type production indexes, so its href resolves too.
    IndexQueryRow autoScoped = response.rows()
        .stream()
        .filter(r -> r.getId().equals("w-auto-1"))
        .findFirst()
        .orElseThrow();
    assertThat(autoScoped.getHref()).isEqualTo("/preview/waivers/organization/org-w-auto-1/w-auto-1");
  }

  @Test
  public void waiverQuery_facetsRequested_includeThreatLevelBuckets() {
    // Numeric threat-level facet counts via aggregateCountByField (the discrete vocabulary is 0-10,
    // built as point ranges [v, v]).
    Map<String, Long> buckets = new HashMap<>();
    buckets.put("8", 3L);
    buckets.put("9", 2L);
    buckets.put("10", 1L);
    when(searchIndexClient.aggregateCountByField(any(), eq("policyWaiverThreatLevel"), any()))
        .thenReturn(new com.sonatype.insight.brain.search.index.MetricAggregationResult(6L, buckets));

    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("includeAutoWaivers", true), 1, 25, null, null, true);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.facets()).isNotNull().containsKey("threatLevel");
    Map<String, Long> threatCounts = new HashMap<>();
    response.facets().get("threatLevel").forEach(b -> threatCounts.put(b.value(), b.count()));
    assertThat(threatCounts).containsEntry("8", 3L).containsEntry("9", 2L).containsEntry("10", 1L);
  }

  @Test
  public void waiverQuery_pageSizeOne_pagesWithCursor() {
    IndexQueryRequest req = new IndexQueryRequest("WAIVER", Map.of(), 1, 1, null, null, false);
    IndexQueryResponse first = resource.query(req);
    assertThat(first.rows()).hasSize(1);
    assertThat(first.nextSearchAfter()).isNotBlank();

    IndexQueryRequest next = new IndexQueryRequest("WAIVER", Map.of(), 2, 1, null, first.nextSearchAfter(), false);
    IndexQueryResponse second = resource.query(next);
    assertThat(second.rows()).hasSize(1);
    assertThat(second.rows().get(0).getId()).isNotEqualTo(first.rows().get(0).getId());
  }

  @Test
  public void waiverQuery_scopedUser_seesOnlyPermittedWaivers() {
    // Fail-closed RBAC through the PRODUCTION permission clause: a user permitted only Acme's context
    // resolves to a TermInSetQuery over the indexed allowedContextIds field (built by the real
    // AbstractSearchIndexClient.buildAllowedContextIdsLuceneFilter), ANDed onto the base query by the
    // real wrapWithPermissionFilter. runRealSearch runs that composed query against Lucene, so the
    // scoping is driven by the real permission wiring, not the harness's org-name stand-in.
    final RealPermissionClause permissions = new RealPermissionClause();
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission())
        .thenReturn(Set.of(contextIdFor("Acme")));
    when(searchIndexClient.buildAllowedContextIdsFilter(any()))
        .thenAnswer(inv -> permissions.buildFilter(inv.getArgument(0)));
    when(searchIndexClient.wrapWithPermissionFilter(any(), any()))
        .thenAnswer(inv -> permissions.wrap(inv.getArgument(0), inv.getArgument(1)));

    // includeAutoWaivers true so the RBAC assertion covers both manual and auto Acme waivers. The
    // Widget Co waiver (w-widget-1) carries a different allowedContextId, so it must be filtered out
    // by the permission clause itself.
    IndexQueryRequest req = new IndexQueryRequest(
        "WAIVER", Map.of("includeAutoWaivers", true), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.rows()).extracting(IndexQueryRow::getId)
        .containsExactlyInAnyOrder("w-acme-1", "w-acme-2", "w-acme-app-1", "w-auto-1");
    assertThat(response.rows()).extracting(IndexQueryRow::getId).doesNotContain("w-widget-1");
  }

  /**
   * Exposes the real {@link AbstractSearchIndexClient} permission-clause methods
   * ({@code buildAllowedContextIdsLuceneFilter} / {@code wrapWithPermissionFilter}) so the RBAC test
   * drives the production permission wiring rather than a harness stand-in. Construction passes nulls
   * for the DI collaborators none of which are touched by the permission-clause methods.
   */
  private static final class RealPermissionClause
      extends AbstractSearchIndexClient
  {
    private RealPermissionClause() {
      super(null, null, null, null, null, null, null, null, null, null, null, null, null, null,
          null, null, null, null, null, null, null, null);
    }

    org.apache.lucene.search.Query buildFilter(final Set<String> permittedContextIds) {
      return buildAllowedContextIdsLuceneFilter(permittedContextIds);
    }

    org.apache.lucene.search.Query wrap(
        final org.apache.lucene.search.Query baseQuery,
        final org.apache.lucene.search.Query permissionFilter)
    {
      return wrapWithPermissionFilter(baseQuery, permissionFilter);
    }

    @Override
    protected void updateMaxQueryClauseCount() {
    }

    @Override
    protected boolean isChangeSpecificError(final Exception e) {
      return false;
    }

    @Override
    protected boolean isSystemicError(final Exception e) {
      return false;
    }

    @Override
    public long count(final String metricQuery) {
      return 0;
    }

    @Override
    public long countDistinct(final String metricQuery, final List<String> compositeKeyFields) {
      return 0;
    }

    @Override
    public com.sonatype.insight.brain.search.index.MetricAggregationResult aggregateCountByField(
        final String metricQuery,
        final String bucketField,
        final Map<String, int[]> ranges)
    {
      return null;
    }

    @Override
    public com.sonatype.insight.brain.search.index.MetricAggregationResult aggregateCountByFloatField(
        final String metricQuery,
        final String bucketField,
        final Map<String, float[]> ranges,
        final String distinctField)
    {
      return null;
    }

    @Override
    public Map<String, Long> countDistinctGroupedBy(
        final String metricQuery,
        final String groupField,
        final String distinctField,
        final java.util.Collection<String> groupValues)
    {
      return java.util.Map.of();
    }

    @Override
    public Map<String, Map<String, Long>> countDistinctGroupedByBands(
        final String metricQuery,
        final String groupField,
        final String distinctField,
        final java.util.Collection<String> groupValues,
        final String bandField,
        final Map<String, int[]> bands)
    {
      return java.util.Map.of();
    }

    @Override
    public com.sonatype.insight.brain.search.results.SearchResultDTO searchIndex(
        final String q,
        final int pageSize,
        final int page,
        final boolean allComponents,
        final boolean isSbomManagerMode,
        final List<String> searchAfter)
    {
      return null;
    }

    @Override
    public void populateIndex() {
    }

    @Override
    public void updateIndex(
        final List<com.sonatype.insight.brain.model.SearchIndexChange> changes,
        final java.util.function.Consumer<com.sonatype.insight.brain.model.SearchIndexChange> cb)
    {
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
  }

  @Test(expected = jakarta.ws.rs.NotFoundException.class)
  public void flagOff_returns404() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    resource.query(new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, false));
  }

  @Test(expected = jakarta.ws.rs.NotFoundException.class)
  public void flagOffWithNullBody_returns404_notLeakingViaBadRequest() {
    // The flag gate runs before the null-body check, so a disabled endpoint stays hidden (404)
    // rather than revealing its existence with a 400 on a malformed body.
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    resource.query(null);
  }

  @Test(expected = jakarta.ws.rs.BadRequestException.class)
  public void unknownEntityType_returns400() {
    resource.query(new IndexQueryRequest("BOGUS", Map.of(), 1, 25, null, null, false));
  }

  @Test
  public void unknownEntityType_bodyNeverEchoesRawInput() {
    try {
      resource.query(new IndexQueryRequest("secretBogusEntity", Map.of(), 1, 25, null, null, false));
      throw new AssertionError("expected the query to throw");
    }
    catch (jakarta.ws.rs.BadRequestException e) {
      ErrorResponse mapped = new ErrorResponseGenerator().mapExceptionAndLog(e);
      assertThat(mapped.getStatusCode()).isEqualTo(400);
      String body = mapped.getMessageBody();
      assertThat(body).doesNotContain("secretBogusEntity");
      // The message enumerates the valid entityType values instead of echoing the caller's input.
      assertThat(body).contains("APPLICATION").contains("VIOLATION").contains("POLICY");
    }
  }

  @Test
  public void unknownFilterKey_mapsTo400() {
    assertMappedStatus(
        new IndexQueryRequest("APPLICATION", Map.of("bogusKey", "x"), 1, 25, null, null, false), 400);
  }

  @Test
  public void unknownFilterKey_bodyCarriesCodeAndNeverEchoesRawInput() throws Exception {
    Map<String, Object> body = mappedBody(
        new IndexQueryRequest("APPLICATION", Map.of("secretBogusKey", "x"), 1, 25, null, null, false));
    assertThat(body).containsEntry("code", FilterValidationException.Code.INVALID_FILTER.name());
    assertThat(String.valueOf(body.get("message"))).doesNotContain("secretBogusKey");
    assertThat(body).doesNotContainValue("secretBogusKey");
  }

  @Test
  public void disallowedSort_mapsTo400() {
    assertMappedStatus(
        new IndexQueryRequest("POLICY", Map.of(), 1, 25, "name", null, false), 400);
  }

  @Test
  public void disallowedSort_bodyCarriesCodeAndNeverEchoesRawInput() throws Exception {
    Map<String, Object> body =
        mappedBody(new IndexQueryRequest("POLICY", Map.of(), 1, 25, "secretSortName", null, false));
    assertThat(body).containsEntry("code", FilterValidationException.Code.SORT_NOT_ALLOWED.name());
    assertThat(String.valueOf(body.get("message"))).doesNotContain("secretSortName");
    assertThat(body).doesNotContainValue("secretSortName");
  }

  @Test
  public void policyQuery_waiverCount_countsWaiversReferencingEachPolicy() {
    // Whole-corpus, RBAC-scoped count of POLICY_WAIVER docs whose policyWaiverPolicyId equals the row's
    // policy id. pol-sec-high has 3 manual waivers; pol-orphan has none (absent stub -> mock default 0).
    when(searchIndexClient.count(org.mockito.ArgumentMatchers.contains("policyWaiverPolicyId:\"pol-sec-high\"")))
        .thenReturn(3L);
    when(searchIndexClient.count(org.mockito.ArgumentMatchers.contains("policyWaiverPolicyId:\"pol-orphan\"")))
        .thenReturn(0L);

    IndexQueryRequest req = new IndexQueryRequest("POLICY", Map.of(), 1, 25, null, null, false);
    IndexQueryResponse response = resource.query(req);

    Map<String, Object> byId = new HashMap<>();
    response.rows().forEach(r -> byId.put(r.getId(), r.getFields().get("waiverCount")));
    assertThat(byId).containsEntry("pol-sec-high", 3L).containsEntry("pol-orphan", 0L);
  }

  @Test
  public void policyQuery_waiverCount_countQueryScopesToPolicyWaiverDocsAndPolicyId() {
    when(searchIndexClient.count(any())).thenReturn(1L);
    resource.query(new IndexQueryRequest("POLICY", Map.of(), 1, 25, null, null, false));
    ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
    verify(searchIndexClient, atLeastOnce()).count(queries.capture());
    // Each waiverCount query is scoped to POLICY_WAIVER docs AND a specific policy id (RBAC applied
    // inside count()), never an unscoped or cross-item-type count.
    assertThat(queries.getAllValues())
        .filteredOn(q -> q.contains("policyWaiverPolicyId:"))
        .isNotEmpty()
        .allSatisfy(q -> assertThat(q).contains("itemType:policy_waiver"));
  }

  @Test
  public void policyQuery_waiverCount_rbacScopedZeroWhenNoReadableContexts() {
    // A caller with no readable contexts counts 0 (SearchIndexClient.count fails closed); the mock
    // default (0) models that, so every policy row reports waiverCount 0 rather than a leaked total.
    IndexQueryResponse response = resource.query(new IndexQueryRequest("POLICY", Map.of(), 1, 25, null, null, false));
    assertThat(response.rows()).isNotEmpty();
    assertThat(response.rows()).allSatisfy(r -> assertThat(r.getFields().get("waiverCount")).isEqualTo(0L));
  }

  @Test
  public void pageSizeOutOfRange_mapsTo400() {
    assertMappedStatus(
        new IndexQueryRequest("APPLICATION", Map.of(), 1, 999999, null, null, false), 400);
  }

  @Test
  public void pageBeyondFirstWithoutCursor_mapsTo400() {
    assertMappedStatus(
        new IndexQueryRequest("APPLICATION", Map.of(), 5, 25, null, null, false), 400);
  }

  @Test
  public void pageOneWithCursor_mapsTo400() {
    assertMappedStatus(
        new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, "some-stale-cursor", false), 400);
  }

  @Test
  public void staleCursor_mapsTo410() {
    // Mint a cursor at pageSize=1, then re-present it at pageSize=25: the generation token no longer
    // matches, so the service throws StaleCursorException, which the mapper renders as 410.
    IndexQueryResponse first =
        resource.query(new IndexQueryRequest("APPLICATION", Map.of(), 1, 1, null, null, false));
    assertThat(first.nextSearchAfter()).isNotBlank();
    assertMappedStatus(
        new IndexQueryRequest("APPLICATION", Map.of(), 2, 25, null, first.nextSearchAfter(), false), 410);
  }

  private void assertMappedStatus(final IndexQueryRequest request, final int expectedStatus) {
    try {
      resource.query(request);
      throw new AssertionError("expected the query to throw");
    }
    catch (RuntimeException e) {
      int status = new ErrorResponseGenerator().mapExceptionAndLog(e).getStatusCode();
      assertThat(status).isEqualTo(expectedStatus);
    }
  }

  /** Drives the thrown {@link FilterValidationException} through the real mapper and parses the JSON body. */
  @SuppressWarnings("unchecked")
  private Map<String, Object> mappedBody(final IndexQueryRequest request) throws Exception {
    try {
      resource.query(request);
      throw new AssertionError("expected the query to throw");
    }
    catch (FilterValidationException e) {
      Response response = new FilterValidationExceptionMapper().toResponse(e);
      assertThat(response.getStatus()).isEqualTo(400);
      String json = new ObjectMapper().writeValueAsString(response.getEntity());
      return new ObjectMapper().readValue(json, Map.class);
    }
  }

  private GlobalSearchResult runRealSearch(final GlobalSearchRequest request) throws Exception {
    List<String> after = request.searchAfter();
    // Slice by doc id from the cursor tuple: match-all hits share a score, so Lucene's score-based
    // searchAfter does not page reliably for a fabricated marker.
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
      SearchResultItemDTO dto = new SearchResultItemDTO(doc);
      rows.add(dto);
      if (i == returnCount - 1 && ordered.size() > request.pageSize()) {
        nextSearchAfter = List.of(String.valueOf(hit.doc));
      }
    }
    return new GlobalSearchResult(rows, all.totalHits.value, nextSearchAfter);
  }

  private static Document policyViolationDoc(final String violationId, final String appName, final String orgName) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_VIOLATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_VIOLATION_ID.label, violationId, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_NAME.label, appName, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_ID.label, appName.toLowerCase().replace(' ', '-'), Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_ID.label, orgName.toLowerCase().replace(' ', '-'), Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    return doc;
  }

  private static Document policyDoc(
      final String policyId,
      final String policyName,
      final String orgName,
      final int threatLevel)
  {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_ID.label, policyId, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_NAME.label, policyName, Store.YES));
    doc.add(new IntPoint(FieldIdentifier.POLICY_THREAT_LEVEL.label, threatLevel));
    doc.add(new StoredField(FieldIdentifier.POLICY_THREAT_LEVEL.label, threatLevel));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_ID.label, orgName.toLowerCase().replace(' ', '-'), Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    addAllowedContextId(doc, orgName);
    return doc;
  }

  private static Document manualWaiverDoc(
      final String waiverId,
      final String policyName,
      final String orgName,
      final int threatLevel,
      final String waivedBy,
      final Long expiresAtEpochMs)
  {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_WAIVER.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_ID.label, waiverId, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label, policyName, Store.YES));
    // Manual waivers carry a policyId + reason; auto waivers do not.
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label, "pol-" + waiverId, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_REASON.label, "waived for release", Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label, "ORGANIZATION", Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID.label, "org-" + waiverId, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_WAIVED_BY.label, waivedBy, Store.YES));
    doc.add(new IntPoint(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, threatLevel));
    doc.add(new StoredField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, threatLevel));
    doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_AUTO.label, "false", Store.YES));
    doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_IS_AUTO.label, "false", Store.YES));
    // Mirror production: a null expiry writes no epoch point, so the doc is never in the expired range.
    if (expiresAtEpochMs != null) {
      doc.add(new LongPoint(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label, expiresAtEpochMs));
      final String status = expiresAtEpochMs < Instant.now().toEpochMilli()
          ? PolicyWaiverExpiryStatuses.EXPIRED
          : PolicyWaiverExpiryStatuses.ACTIVE;
      doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label, status, Store.YES));
    }
    else {
      doc.add(new StringField(
          FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label, PolicyWaiverExpiryStatuses.NEVER, Store.YES));
    }
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    addAllowedContextId(doc, orgName);
    return doc;
  }

  // Denormalized permission-filter field the production RBAC clause (TermInSetQuery over
  // allowedContextIds) matches on. Case-sensitive keyword, so index the raw context id.
  private static void addAllowedContextId(final Document doc, final String orgName) {
    doc.add(new StringField(FieldIdentifier.ALLOWED_CONTEXT_IDS.label, contextIdFor(orgName), Store.YES));
  }

  private static String contextIdFor(final String orgName) {
    return "ctx-" + orgName.toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
  }

  /**
   * An application-scoped manual waiver: it carries applicationName/applicationId (written by
   * {@code setOwner(Application)} in production) so the applications/applicationId filters can match it,
   * whereas org-scoped waivers carry neither.
   */
  private static Document appScopedWaiverDoc(
      final String waiverId,
      final String policyName,
      final String orgName,
      final String appName,
      final String appId,
      final int threatLevel)
  {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_WAIVER.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_ID.label, waiverId, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label, policyName, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label, "pol-" + waiverId, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_REASON.label, "waived for release", Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label, "APPLICATION", Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID.label, appId, Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_WAIVED_BY.label, "dave", Store.YES));
    doc.add(new IntPoint(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, threatLevel));
    doc.add(new StoredField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, threatLevel));
    doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_AUTO.label, "false", Store.YES));
    doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_IS_AUTO.label, "false", Store.YES));
    doc.add(new StringField(
        FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label, PolicyWaiverExpiryStatuses.NEVER, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_NAME.label, appName, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_ID.label, appId, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    addAllowedContextId(doc, orgName);
    return doc;
  }

  private static Document autoWaiverDoc(final String waiverId, final String orgName, final int threatLevel) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.POLICY_WAIVER.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_ID.label, waiverId, Store.YES));
    // Auto waivers carry no indexed policy name (nor policyId/reason); the display title is
    // synthesized on the read side so the label is never text-searchable and can change without a
    // reindex. Mirrors DocumentBuilderHelper leaving policyWaiverPolicyName null for auto waivers.
    doc.add(new IntPoint(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, threatLevel));
    doc.add(new StoredField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, threatLevel));
    doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_AUTO.label, "true", Store.YES));
    doc.add(new StringField(FieldIdentifier.POLICY_WAIVER_IS_AUTO.label, "true", Store.YES));
    doc.add(new StringField(
        FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label, PolicyWaiverExpiryStatuses.NEVER, Store.YES));
    // Auto waivers still carry scope owner id/type (DocumentBuilderHelper sets both), so the
    // read-side waiverHref resolves to a valid detail link like the manual/app fixtures do.
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label, "ORGANIZATION", Store.YES));
    doc.add(new TextField(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID.label, "org-" + waiverId, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    addAllowedContextId(doc, orgName);
    return doc;
  }

  private static Document appDoc(final String publicId, final String name, final String orgName) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_PUBLIC_ID.label, publicId, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_ID.label, publicId + "-id", Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_NAME.label, name, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_ID.label, orgName.toLowerCase().replace(' ', '-'), Store.YES));
    // The organizations filter rewrites to parentOrganizationName, so index it or the filter matches nothing.
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    return doc;
  }
}
