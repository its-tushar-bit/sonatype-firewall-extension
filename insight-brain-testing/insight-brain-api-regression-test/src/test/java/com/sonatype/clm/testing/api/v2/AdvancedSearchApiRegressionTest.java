/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import java.util.Map;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;

import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code api/v2/search/advanced}.
 *
 * <p>
 * The embedded test server does not use OpenSearch — it falls back to a DB-based search
 * that returns 200 with empty results. Full coverage of OpenSearch error states (409) lives
 * in the Advanced Search integration suite which boots a real OpenSearch instance.
 *
 * <p>
 * <b>CLM-37981 note:</b> a real regression guard for the HTTP/1.0 export bug would require
 * an HTTP/1.0-capable client, which the current test harness does not expose. Coverage is
 * tracked under CLM-37981; a follow-up ticket should reference that issue once harness
 * support is available.
 *
 * <p>
 * Covers: search 200 with empty results; CSV export 200 with CSV content type; index rebuild
 * trigger (204); and the unauthenticated auth contract (401) for all three paths.
 */
public class AdvancedSearchApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String SEARCH_BASE = PublicApiPaths.ADVANCED_SEARCH_RESOURCE_PATH_V2;

  private static final String INDEX_PATH = SEARCH_BASE + "/index";

  private static final String EXPORT_PATH = SEARCH_BASE + "/export/csv";

  private static final String TEST_QUERY = "log4j";

  @Test
  public void testSearch_noResults_returns200() throws Exception {
    HttpResponse response = apiGet(SEARCH_BASE, "query", TEST_QUERY);
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("groupingByDTOS").isArray();
  }

  @Test
  public void testRebuildIndex_returns204() throws Exception {
    // apiPostJson with empty body: endpoint is @POST with no body — empty map is ignored by server
    HttpResponse response = apiPostJson(INDEX_PATH, Map.of());
    assertResponseStatus(204, response);
  }

  @Test
  public void testExportCsv_returns200WithCsvContentType() throws Exception {
    HttpResponse response = apiGet(EXPORT_PATH, "query", TEST_QUERY);
    assertResponseStatus(200, response);
    assertThat(response.getHeader("Content-Type")).contains("csv");
    assertThat(response.getBodyText()).contains("Organization"); // first column header in CSV export
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testSearch_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(SEARCH_BASE);
    assertResponseStatus(401, response);
  }

  /** Auth contract on POST: unauthenticated callers get 401 before the body is parsed. */
  @Test
  public void testRebuildIndex_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(INDEX_PATH, Map.of());
    assertResponseStatus(401, response);
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testExportCsv_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(EXPORT_PATH);
    assertResponseStatus(401, response);
  }
}
