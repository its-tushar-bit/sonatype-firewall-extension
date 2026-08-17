/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeatureTestSupport;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LowerCaseKeywordAnalyzer;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end CSV export of the My-Scan-Data (LOCAL) Components and Vulnerabilities lists, through the
 * real resource -> service -> local request builder -> Lucene path. Also pins the deliberate
 * catalog-source rejection, which is the written "known gap" for the Sonatype-catalog leg.
 */
public class CatalogCsvExportEndpointTest
{
  private SearchIndexClient searchIndexClient;

  private PermissionService permissionService;

  private CurrentUser currentUser;

  private Directory directory;

  private IndexReader reader;

  private IndexSearcher searcher;

  private CatalogResource resource;

  @BeforeEach
  public void setUp() throws Exception {
    SystemConfigurationPropertyFeatureTestSupport.install();
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    directory = new ByteBuffersDirectory();
    final Map<String, Analyzer> perField = new HashMap<>();
    perField.put(FieldIdentifier.ALLOWED_CONTEXT_IDS.label, new KeywordAnalyzer());
    final PerFieldAnalyzerWrapper analyzer =
        new PerFieldAnalyzerWrapper(new LowerCaseKeywordAnalyzer(), perField);
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
      writer.addDocument(componentDoc("hash-shiro", "org.apache.shiro:shiro-core", "maven", "Acme", "Acme Prod"));
      // A component name carrying a comma, a quote and non-ASCII, to prove escaping end-to-end.
      writer.addDocument(componentDoc("hash-odd", "widget, \"core\" Ünïcodé", "npm", "Widget Co", "Widget Inv"));
      writer.addDocument(vulnDoc("CVE-2024-0001", "shiro-core", "maven", "Acme", "Acme Prod", 9.8f));
      writer.addDocument(vulnDoc("CVE-2024-0002", "log4j", "maven", "Widget Co", "Widget Inv", 5.3f));
      writer.commit();
    }
    reader = DirectoryReader.open(directory);
    searcher = new IndexSearcher(reader);

    searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.isSearchPreviewEnabled()).thenReturn(true);
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("org-1"));
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenReturn(null);
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    when(searchIndexClient.buildPermittedQuery(any())).thenCallRealMethod();
    when(searchIndexClient.getLastIndexTime()).thenReturn(1000L);
    when(searchIndexClient.backendId()).thenReturn("lucene");
    when(searchIndexClient.count(any())).thenReturn(0L);
    when(searchIndexClient.countDistinct(any(), any())).thenReturn(0L);
    // Per-page grouped enrichment: a constant number of grouped reads per page, stubbed empty so the
    // computed columns read as zero rather than being absent.
    when(searchIndexClient.countDistinctGroupedBy(any(), any(), any(), any())).thenReturn(Map.of());
    when(searchIndexClient.countDistinctGroupedByBands(any(), any(), any(), any(), any(), any()))
        .thenReturn(Map.of());
    when(searchIndexClient.aggregateCountByFloatField(any(), any(), any(), any()))
        .thenReturn(new MetricAggregationResult(0L, Map.of()));
    when(searchIndexClient.searchGlobal(any(GlobalSearchRequest.class)))
        .thenAnswer(inv -> runRealSearch(inv.getArgument(0)));

    final SearchApiClient searchApiClient = mock(SearchApiClient.class);
    final IqLocalSearchService iq = new IqLocalSearchService(searchIndexClient);
    final CatalogService service = new CatalogService(iq, searchApiClient, searchIndexClient);

    currentUser = mock(CurrentUser.class);
    permissionService = mock(PermissionService.class);
    grantRead("org-1");
    resource = new CatalogResource(service, permissionService, currentUser);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (reader != null) {
      reader.close();
    }
    if (directory != null) {
      directory.close();
    }
    SystemConfigurationPropertyFeatureTestSupport.uninstall();
  }

  // ---------- header row + data ----------

  @Test
  public void componentExport_headerMatchesTheDocumentedColumns() throws Exception {
    assertThat(headerRow(export("COMPONENT", "local", Map.of()))).isEqualTo(
        "Component,Version,Coordinates,Ecosystem,Organization,Application,Affected Applications,"
            + "Critical Violations,High Violations,Medium Violations,Low Violations,Component Hash");
  }

  @Test
  public void vulnerabilityExport_headerMatchesTheDocumentedColumns() throws Exception {
    assertThat(headerRow(export("VULNERABILITY", "local", Map.of()))).isEqualTo(
        "Vulnerability,Severity,Status,Ecosystem,Component,Organization,Application,"
            + "Affected Applications,Affected Components,First Seen,Description");
  }

  @Test
  public void componentExport_writesADataRowPerComponent() throws Exception {
    assertThat(dataLines(export("COMPONENT", "local", Map.of()))).hasSize(2);
  }

  @Test
  public void vulnerabilityExport_rowsCarryTheReferenceAndSeverity() throws Exception {
    final List<String> lines = dataLines(export("VULNERABILITY", "local", Map.of()));
    assertThat(lines).anySatisfy(line -> assertThat(line).startsWith("CVE-2024-0001,9.8,"));
  }

  @Test
  public void componentExport_escapesCommasQuotesAndUnicodeInRealRowData() throws Exception {
    final List<String> lines =
        dataLines(export("COMPONENT", "local", Map.of("organizations", List.of("Widget Co"))));
    assertThat(lines).hasSize(1);
    assertThat(lines.get(0)).startsWith("\"widget, \"\"core\"\" Ünïcodé\",");
    assertThat(cellCount(lines.get(0)))
        .isEqualTo(cellCount(headerRow(export("COMPONENT", "local", Map.of()))));
  }

  /** The per-page grouped enrichment still runs on an export page, so computed columns are present. */
  @Test
  public void componentExport_carriesTheEnrichedComputedColumns() throws Exception {
    final List<String> lines =
        dataLines(export("COMPONENT", "local", Map.of("organizations", List.of("Acme"))));
    assertThat(lines).hasSize(1);
    // Affected Applications + the four severity counts read 0 (stubbed empty), never blank.
    final String[] cells = lines.get(0).split(",", -1);
    assertThat(cells[6]).isEqualTo("0");
    assertThat(cells[7]).isEqualTo("0");
    assertThat(cells[10]).isEqualTo("0");
  }

  // ---------- filter parity with the list endpoint ----------

  @Test
  public void export_matchesTheListEndpointUnderTheSameFilters() throws Exception {
    for (Map<String, Object> filters : List.of(
        Map.<String, Object>of(),
        Map.<String, Object>of("organizations", List.of("Acme")),
        Map.<String, Object>of("organizations", List.of("Widget Co"))))
    {
      final List<String> listed = listedComponentNames(filters);
      final List<String> exported = firstCells(dataLines(export("COMPONENT", "local", filters)));
      assertThat(exported)
          .as("export must match the list for filters %s", filters)
          .containsExactlyInAnyOrderElementsOf(listed);
    }
  }

  @Test
  public void applyingAFilter_narrowsTheCsvIdentically() throws Exception {
    assertThat(dataLines(export("COMPONENT", "local", Map.of()))).hasSize(2);
    final List<String> filtered =
        dataLines(export("COMPONENT", "local", Map.of("organizations", List.of("Acme"))));
    assertThat(firstCells(filtered)).containsExactly("org.apache.shiro:shiro-core");
  }

  @Test
  public void vulnerabilityExport_narrowsByOrganizationLikeTheList() throws Exception {
    final List<String> filtered =
        dataLines(export("VULNERABILITY", "local", Map.of("organizations", List.of("Widget Co"))));
    assertThat(firstCells(filtered)).containsExactly("CVE-2024-0002");
  }

  @Test
  public void emptyResultSet_writesHeaderRowOnly() throws Exception {
    final Response response =
        resource.exportCsv(request("COMPONENT", "local", Map.of("organizations", List.of("Nope Ltd"))));
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(dataLines(response)).isEmpty();
    assertThat(headerRow(response)).startsWith("Component,");
  }

  // ---------- the catalog-source known gap ----------

  /**
   * The Sonatype-catalog (Guide/HDS) leg is NOT exportable here: it is offset-paginated over a remote
   * store with a hard page ceiling and no cursor, so it cannot be walked to completion. Rejecting is
   * the honest answer; silently returning local rows for a catalog request would be a wrong answer that
   * looks right.
   */
  @Test
  public void catalogSourceExport_isRejectedRatherThanSilentlyServingLocalRows() {
    assertThatThrownBy(() -> resource.exportCsv(request("COMPONENT", "catalog", Map.of())))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining(CatalogResource.CATALOG_SOURCE_NOT_EXPORTABLE);
    assertThatThrownBy(() -> resource.exportCsv(request("VULNERABILITY", "catalog", Map.of())))
        .isInstanceOf(BadRequestException.class);
  }

  /** An omitted source defaults to LOCAL, matching the list endpoint's default. */
  @Test
  public void omittedSource_exportsTheLocalList() throws Exception {
    assertThat(dataLines(export("COMPONENT", null, Map.of()))).hasSize(2);
  }

  // ---------- RBAC parity ----------

  @Test
  public void noReadableContext_isForbiddenJustLikeTheListEndpoint() {
    grantNoRead();
    assertThatThrownBy(() -> resource.exportCsv(request("COMPONENT", "local", Map.of())))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> resource.search(request("COMPONENT", "local", Map.of())))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void exportedRows_areNarrowedByThePermissionFilter() throws Exception {
    // Drive the REAL permission clause: only the Acme context is readable, so the Widget Co row must
    // not reach the CSV. buildAllowedContextIdsFilter is a default interface method that throws on a
    // bare mock, so supply the production clause explicitly rather than thenCallRealMethod.
    when(searchIndexClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(Set.of("ctx-acme"));
    when(searchIndexClient.buildAllowedContextIdsFilter(any())).thenAnswer(inv -> allowedContextClause("ctx-acme"));
    when(searchIndexClient.wrapWithPermissionFilter(any(), any())).thenAnswer(inv -> new BooleanQuery.Builder()
        .add(inv.getArgument(0), Occur.MUST)
        .add(allowedContextClause("ctx-acme"), Occur.FILTER)
        .build());
    final List<String> exported = firstCells(dataLines(export("COMPONENT", "local", Map.of())));
    assertThat(exported).containsExactly("org.apache.shiro:shiro-core");
  }

  /** Production-shaped allowed-context clause over the case-sensitive allowedContextIds keyword. */
  private static Query allowedContextClause(final String contextId) {
    return new TermQuery(new Term(FieldIdentifier.ALLOWED_CONTEXT_IDS.label, contextId));
  }

  // ---------- transport ----------

  @Test
  public void contentType_isTextCsvWithUtf8Charset() {
    assertThat(resource.exportCsv(request("COMPONENT", "local", Map.of())).getMediaType().toString())
        .isEqualTo("text/csv;charset=utf-8");
  }

  /** CLM-38675: a charset must never travel as Content-Encoding. */
  @Test
  public void contentEncoding_isNeverSet() {
    assertThat(resource.exportCsv(request("COMPONENT", "local", Map.of()))
        .getHeaderString(HttpHeaders.CONTENT_ENCODING)).isNull();
  }

  /** CLM-37981: no hand-set framing headers, so an HTTP/1.0 client still works. */
  @Test
  public void noContentLengthOrTransferEncodingIsSetByHand() {
    final Response response = resource.exportCsv(request("COMPONENT", "local", Map.of()));
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_LENGTH)).isNull();
    assertThat(response.getHeaderString("Transfer-Encoding")).isNull();
  }

  @Test
  public void contentDisposition_carriesThePerModuleFilename() {
    assertThat(resource.exportCsv(request("COMPONENT", "local", Map.of()))
        .getHeaderString(HttpHeaders.CONTENT_DISPOSITION)).contains("filename=\"components-");
    assertThat(resource.exportCsv(request("VULNERABILITY", "local", Map.of()))
        .getHeaderString(HttpHeaders.CONTENT_DISPOSITION)).contains("filename=\"vulnerabilities-");
  }

  // ---------- gates + validation ----------

  @Test
  public void featureFlagOff_isNotFound() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
    assertThatThrownBy(() -> resource.exportCsv(request("COMPONENT", "local", Map.of())))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void nullBody_isBadRequest() {
    assertThatThrownBy(() -> resource.exportCsv(null)).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void unknownEntityType_isBadRequest() {
    assertThatThrownBy(() -> resource.exportCsv(request("NOPE", "local", Map.of())))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void unknownSource_isBadRequest() {
    assertThatThrownBy(() -> resource.exportCsv(request("COMPONENT", "nope", Map.of())))
        .isInstanceOf(BadRequestException.class);
  }

  // ---------- helpers ----------

  private static CatalogRequest request(
      final String entityType,
      final String source,
      final Map<String, Object> filters)
  {
    return new CatalogRequest(entityType, source, filters, null, null, null, null, false);
  }

  private Response export(final String entityType, final String source, final Map<String, Object> filters) {
    return resource.exportCsv(request(entityType, source, filters));
  }

  private List<String> listedComponentNames(final Map<String, Object> filters) {
    final CatalogResponse response =
        resource.search(new CatalogRequest("COMPONENT", "local", filters, 1, 100, null, null, false));
    return response.rows()
        .stream()
        .map(row -> String.valueOf(row.getFields().get("componentName")))
        .toList();
  }

  private void grantRead(final String contextId) {
    final UserPrincipal principal = mock(UserPrincipal.class);
    when(currentUser.getUserPrincipal()).thenReturn(principal);
    when(permissionService.getContextIdsForUserWithPermission(principal, Permission.READ))
        .thenReturn(Set.of(contextId));
  }

  private void grantNoRead() {
    final UserPrincipal principal = mock(UserPrincipal.class);
    when(currentUser.getUserPrincipal()).thenReturn(principal);
    when(permissionService.getContextIdsForUserWithPermission(principal, Permission.READ))
        .thenReturn(Set.of());
  }

  private static String bodyOf(final Response response) throws Exception {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    ((StreamingOutput) response.getEntity()).write(out);
    return out.toString(StandardCharsets.UTF_8);
  }

  private static String headerRow(final Response response) throws Exception {
    final String body = bodyOf(response);
    final String withoutBom = body.startsWith("\uFEFF") ? body.substring(1) : body;
    return withoutBom.split("\r\n", -1)[0];
  }

  private static List<String> dataLines(final Response response) throws Exception {
    final String body = bodyOf(response);
    final String withoutBom = body.startsWith("\uFEFF") ? body.substring(1) : body;
    final String[] parts = withoutBom.split("\r\n", -1);
    final List<String> lines = new ArrayList<>();
    for (int i = 1; i < parts.length; i++) {
      if (!(i == parts.length - 1 && parts[i].isEmpty())) {
        lines.add(parts[i]);
      }
    }
    return lines;
  }

  private static List<String> firstCells(final List<String> lines) {
    final List<String> out = new ArrayList<>(lines.size());
    for (String line : lines) {
      out.add(unquoteFirstCell(line));
    }
    return out;
  }

  private static String unquoteFirstCell(final String line) {
    if (!line.startsWith("\"")) {
      final int comma = line.indexOf(',');
      return comma < 0 ? line : line.substring(0, comma);
    }
    final StringBuilder sb = new StringBuilder();
    int i = 1;
    while (i < line.length()) {
      final char c = line.charAt(i);
      if (c == '"') {
        if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
          sb.append('"');
          i += 2;
          continue;
        }
        break;
      }
      sb.append(c);
      i++;
    }
    return sb.toString();
  }

  private static int cellCount(final String line) {
    int cells = 1;
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      final char c = line.charAt(i);
      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          i++;
        }
        else {
          inQuotes = !inQuotes;
        }
      }
      else if (c == ',' && !inQuotes) {
        cells++;
      }
    }
    return cells;
  }

  private GlobalSearchResult runRealSearch(final GlobalSearchRequest request) throws Exception {
    final List<String> after = request.searchAfter();
    final TopDocs all = searcher.search(request.baseQuery(), Math.max(1, reader.maxDoc()));
    final int startDocExclusive = (after != null && !after.isEmpty()) ? Integer.parseInt(after.get(0)) : -1;
    final List<ScoreDoc> ordered = new ArrayList<>();
    for (ScoreDoc sd : all.scoreDocs) {
      if (sd.doc > startDocExclusive) {
        ordered.add(sd);
      }
    }
    final List<SearchResultItemDTO> rows = new ArrayList<>();
    final int returnCount = Math.min(ordered.size(), request.pageSize());
    List<String> nextSearchAfter = List.of();
    for (int i = 0; i < returnCount; i++) {
      final ScoreDoc hit = ordered.get(i);
      rows.add(new SearchResultItemDTO(searcher.storedFields().document(hit.doc)));
      if (i == returnCount - 1 && ordered.size() > request.pageSize()) {
        nextSearchAfter = List.of(String.valueOf(hit.doc));
      }
    }
    return new GlobalSearchResult(rows, all.totalHits.value, nextSearchAfter);
  }

  private static Document componentDoc(
      final String hash,
      final String componentName,
      final String format,
      final String orgName,
      final String appName)
  {
    final Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.NON_VULNERABLE_COMPONENT.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.COMPONENT_HASH.label, hash, Store.YES));
    doc.add(new TextField(FieldIdentifier.COMPONENT_NAME.label, componentName, Store.YES));
    doc.add(new TextField(FieldIdentifier.COMPONENT_FORMAT.label, format, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_NAME.label, appName, Store.YES));
    addAllowedContextId(doc, orgName);
    return doc;
  }

  private static Document vulnDoc(
      final String vulnerabilityId,
      final String componentName,
      final String format,
      final String orgName,
      final String appName,
      final float severity)
  {
    final Document doc = new Document();
    doc.add(new TextField(FieldIdentifier.ITEM_TYPE.label, ItemType.SECURITY_VULNERABILITY.name(), Store.YES));
    doc.add(new TextField(FieldIdentifier.VULNERABILITY_ID.label, vulnerabilityId, Store.YES));
    doc.add(new StoredField(FieldIdentifier.VULNERABILITY_SEVERITY.label, severity));
    doc.add(new TextField(FieldIdentifier.COMPONENT_NAME.label, componentName, Store.YES));
    doc.add(new TextField(FieldIdentifier.COMPONENT_FORMAT.label, format, Store.YES));
    doc.add(new TextField(FieldIdentifier.ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, orgName, Store.YES));
    doc.add(new TextField(FieldIdentifier.APPLICATION_NAME.label, appName, Store.YES));
    addAllowedContextId(doc, orgName);
    return doc;
  }

  private static void addAllowedContextId(final Document doc, final String orgName) {
    final String contextId = "Acme".equals(orgName) ? "ctx-acme" : "ctx-widget";
    doc.add(new StringField(FieldIdentifier.ALLOWED_CONTEXT_IDS.label, contextId, Store.YES));
  }
}
