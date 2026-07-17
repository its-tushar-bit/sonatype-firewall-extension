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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.search.global.FilterValidationException;
import com.sonatype.insight.brain.search.global.FilterValidationExceptionMapper;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import jakarta.ws.rs.core.Response;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LowerCaseKeywordAnalyzer;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
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

public class IndexQueryEndpointTest
{
  private SearchIndexClient searchIndexClient;

  private Directory directory;

  private IndexReader reader;

  private IndexSearcher searcher;

  private IndexQueryResource resource;

  @Before
  public void setUp() throws Exception {
    SystemConfigurationPropertyFeatureTestSupport.install();
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(true);

    directory = new ByteBuffersDirectory();
    Map<String, Analyzer> perField = new HashMap<>();
    PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), perField);
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
      writer.addDocument(appDoc("acme-prod", "Acme Prod", "Acme"));
      writer.addDocument(appDoc("acme-dev", "Acme Dev", "Acme"));
      writer.addDocument(appDoc("widget-co", "Widget Inventory", "Widget Co"));
      writer.addDocument(policyViolationDoc("pv-1", "Acme Prod", "Acme"));
      writer.addDocument(policyViolationDoc("pv-2", "Widget Inventory", "Widget Co"));
      writer.commit();
    }
    reader = DirectoryReader.open(directory);
    searcher = new IndexSearcher(reader);

    searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.isGlobalSearchEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("org-1"));
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(searchIndexClient.getLastIndexTime()).thenReturn(1000L);
    when(searchIndexClient.backendId()).thenReturn("lucene");
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenAnswer(inv -> runRealSearch(inv.getArgument(0)));

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

  @Test
  public void applicationQuery_facetsRequested_areWholeCorpusCounts() {
    // Values are seeded from the returned page (Acme, Widget Co), but counts come from the whole-corpus
    // RBAC-scoped count() over the item type + filters, not the page tallies (which would be 2 and 1).
    when(searchIndexClient.count(org.mockito.ArgumentMatchers.contains("organizationName:\"Acme\"")))
        .thenReturn(50L);
    when(searchIndexClient.count(org.mockito.ArgumentMatchers.contains("organizationName:\"Widget Co\"")))
        .thenReturn(7L);

    IndexQueryRequest req = new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, true);
    IndexQueryResponse response = resource.query(req);
    assertThat(response.facets()).isNotNull();
    // Counts are whole-corpus filter-wide totals now, so the wire flag says so.
    assertThat(response.facetsOverPageOnly()).isFalse();
    assertThat(response.facets()).containsKey("organizationName");
    Map<String, Long> orgCounts = new HashMap<>();
    response.facets()
        .get("organizationName")
        .forEach(b -> orgCounts.put(b.value(), b.count()));
    assertThat(orgCounts).containsEntry("Acme", 50L).containsEntry("Widget Co", 7L);
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

  @Test(expected = jakarta.ws.rs.NotFoundException.class)
  public void flagOff_returns404() {
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(false);
    resource.query(new IndexQueryRequest("APPLICATION", Map.of(), 1, 25, null, null, false));
  }

  @Test(expected = jakarta.ws.rs.NotFoundException.class)
  public void flagOffWithNullBody_returns404_notLeakingViaBadRequest() {
    // The flag gate runs before the null-body check, so a disabled endpoint stays hidden (404)
    // rather than revealing its existence with a 400 on a malformed body.
    SystemConfigurationPropertyFeature.GLOBAL_SEARCH.setEnabled(false);
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
      SearchResultItemDTO dto = new SearchResultItemDTO();
      dto.itemType = doc.get(FieldIdentifier.ITEM_TYPE.label);
      dto.applicationName = doc.get(FieldIdentifier.APPLICATION_NAME.label);
      dto.applicationPublicId = doc.get(FieldIdentifier.APPLICATION_PUBLIC_ID.label);
      dto.organizationName = doc.get(FieldIdentifier.ORGANIZATION_NAME.label);
      dto.policyViolationId = doc.get(FieldIdentifier.POLICY_VIOLATION_ID.label);
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
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    return doc;
  }

  private static Document appDoc(final String publicId, final String name, final String orgName) {
    Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.APPLICATION.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_PUBLIC_ID.label, publicId, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_NAME.label, name, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    // The organizations filter rewrites to parentOrganizationName, so index it or the filter matches nothing.
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    return doc;
  }
}
