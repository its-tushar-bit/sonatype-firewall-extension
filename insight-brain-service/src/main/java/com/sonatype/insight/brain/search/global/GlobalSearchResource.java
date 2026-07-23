/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.codahale.metrics.annotation.Timed;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.jaxrs.JsonUtils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * JAX-RS resource backing the Global Search frontend. Exposes the paginated full-results endpoint at
 * {@code /rest/search/results}.
 *
 * <p>
 * Mounted under {@code /rest} — internal UI-backing surface, not the supported public {@code /api/v2}
 * surface, so it carries no OpenAPI/Swagger annotations. The legacy {@code /api/v2/search/advanced}
 * endpoint is intentionally untouched.
 *
 * <p>
 * The endpoint is gated by the {@code GLOBAL_SEARCH} feature flag. When disabled, authenticated and
 * authorized callers receive {@code 404 Not Found}; unauthenticated callers still receive the normal
 * {@code 401} from the Shiro filter chain before this handler runs, and authenticated callers with no
 * readable context receive {@code 403}.
 */
@Named
@Singleton
@Timed
@Path(GlobalSearchResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class GlobalSearchResource
{
  public static final String RESOURCE_PATH = "rest/search";

  public static final String RESULTS_PATH = "results";

  /** Single source of truth lives on {@link ResultsRequest#MAX_QUERY_LENGTH}. */
  static final int MAX_QUERY_LENGTH = ResultsRequest.MAX_QUERY_LENGTH;

  /** Cap on the {@code source} query-string length. Well-known values are short; anything longer is a mistake. */
  static final int MAX_SOURCE_LENGTH = 32;

  /** Cap on the {@code tab} query-string length. Well-known values are short; anything longer is a mistake. */
  static final int MAX_TAB_LENGTH = 32;

  /** Cap on the {@code sort} query-string length. Allowlisted sort keys are short; anything longer is a mistake. */
  static final int MAX_SORT_LENGTH = 64;

  private final ResultsService resultsService;

  private final SearchIndexClient searchIndexClient;

  @Inject
  public GlobalSearchResource(
      final ResultsService resultsService,
      final SearchIndexClient searchIndexClient)
  {
    this.resultsService = resultsService;
    this.searchIndexClient = searchIndexClient;
  }

  /**
   * Tabbed full-results endpoint. Returns up to {@code pageSize} rows for the requested {@link Tab}, plus a
   * capped {@code totalEstimate} and an opaque {@code nextSearchAfter} cursor for the next page.
   *
   * <p>
   * Pagination uses {@code page} + {@code pageSize} for offsets strictly less than
   * {@link ResultsRequest#DEEP_PAGINATION_THRESHOLD} rows and the opaque {@code searchAfter} cursor for
   * that offset and deeper. A stale cursor (after a shard rebalance, full reindex, or sort-allowlist update) is
   * rejected with HTTP 410 and the {@link StaleCursorExceptionMapper#RETRY_HINT_HEADER} hint header.
   *
   * <p>
   * Parameters are taken as bare types and validated manually so the feature-flag gate runs first.
   * Otherwise a flag-off endpoint would still surface {@code 400 Bad Request} for missing / invalid input
   * and leak the endpoint's existence to callers who shouldn't see it.
   */
  @GET
  @Path(RESULTS_PATH)
  public Response getResults(
      @QueryParam("q") final String q,
      @QueryParam("tab") final String tab,
      @QueryParam("page") final Integer page,
      @QueryParam("pageSize") final Integer pageSize,
      @QueryParam("sort") final String sort,
      @QueryParam("searchAfter") final String searchAfter,
      @QueryParam("source") final String source)
  {
    verifyGlobalSearchEnabled();
    verifyReadOnAnyContext();

    final String validatedQ = validateQuery(q);
    final Tab parsedTab = validateTab(tab);
    final int parsedPage = validatePage(page);
    final int parsedPageSize = validatePageSize(pageSize);
    final SearchSource parsedSource = validateSource(source);
    final String validatedSort = validateSort(sort);

    ResultsRequest request = new ResultsRequest(
        validatedQ, parsedTab, parsedPage, parsedPageSize, validatedSort, searchAfter, parsedSource);
    ResultsResponse body = resultsService.search(request);

    // Response envelope carries `warnings` inline for redundancy, and additionally exposes them
    // on the `X-Search-Warnings` header (ASCII-encoded JSON array) so callers that only inspect
    // headers — e.g. curl / fetch API in the frontend spec — still see parser + compiler
    // warnings emitted by the AST pipeline.
    Response.ResponseBuilder rb = Response.ok(body);
    if (body.getWarnings() != null && !body.getWarnings().isEmpty()) {
      rb.header("X-Search-Warnings", encodeWarningsHeader(body.getWarnings()));
    }
    return rb.build();
  }

  /**
   * Encode the parser + compiler warnings for the {@code X-Search-Warnings} header. HTTP header
   * values must be ASCII (ByteString) — non-ASCII characters (em-dashes, accented characters in
   * user-supplied field names, etc.) are Unicode-escaped so the header write never crashes.
   */
  static String encodeWarningsHeader(final List<String> warnings) {
    // Use the shared JSON mapper to produce the array (consistent escaping with the JSON body), then
    // run a straightforward \\uXXXX pass over any code points >= 0x80.
    String json;
    try {
      json = JsonUtils.toJson(warnings);
    }
    catch (IOException e) {
      // Serializing a List<String> is not a realistic failure mode; defensively fall back to a
      // bracketed count so the header still communicates "warnings exist".
      return "[\"warning-serialization-failed:" + warnings.size() + "\"]";
    }
    StringBuilder out = new StringBuilder(json.length());
    for (int i = 0; i < json.length(); i++) {
      char c = json.charAt(i);
      if (c < 0x80) {
        out.append(c);
      }
      else {
        out.append("\\u");
        String hex = Integer.toHexString(c);
        for (int pad = hex.length(); pad < 4; pad++) {
          out.append('0');
        }
        out.append(hex);
      }
    }
    return out.toString();
  }

  /**
   * Parse the {@code source} query parameter. Accepts any case (upper, lower, mixed) so {@code ?source=Local}
   * and {@code ?source=LOCAL} are equivalent. {@code null} or blank falls back to {@link SearchSource#DEFAULT}.
   * Rejects unknown values with {@code 400 Bad Request} and never echoes the caller-supplied value.
   */
  static SearchSource validateSource(final String source) {
    if (source != null && source.length() > MAX_SOURCE_LENGTH) {
      throw new BadRequestException("source must not exceed " + MAX_SOURCE_LENGTH + " characters");
    }
    try {
      return SearchSource.fromWireValue(source);
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException("unknown source");
    }
  }

  /**
   * Length-guard the {@code sort} query parameter, consistent with the {@code tab} / {@code source} caps.
   * {@code null} / blank passes through unchanged (no sort requested); over-length is rejected with a generic
   * message that never echoes the caller-supplied value. Allowlist enforcement (and the value itself) is left
   * to {@link ResultsService#validateSort}.
   */
  static String validateSort(final String sort) {
    if (sort != null && sort.length() > MAX_SORT_LENGTH) {
      throw new BadRequestException("sort must not exceed " + MAX_SORT_LENGTH + " characters");
    }
    return sort;
  }

  private static void verifyGlobalSearchEnabled() {
    // 404 (not 403) when the flag is off, so a disabled endpoint is indistinguishable from absent.
    if (!SystemConfigurationPropertyFeature.GLOBAL_SEARCH.isEnabled()) {
      throw new NotFoundException("Not Found");
    }
  }

  private void verifyReadOnAnyContext() {
    // Anonymous callers are rejected upstream with 401 by the Shiro requireAuth filter, so any request
    // reaching here is authenticated; a caller with no readable context is authenticated-but-forbidden.
    // Gate on the same read-context source the row filter uses, so the gate and filter cannot diverge.
    final Set<String> readContextIds = searchIndexClient.getCurrentUserContextIdsWithReadPermission();
    if (readContextIds.isEmpty()) {
      throw new ForbiddenException("Not authorized");
    }
  }

  /**
   * Shared query-string validation. Enforces normalization (strip, non-blank, max length, no control
   * characters).
   */
  static String validateQuery(final String query) {
    if (query == null) {
      throw new BadRequestException("query must not be blank");
    }
    final String normalized = query.strip();
    if (normalized.isEmpty()) {
      throw new BadRequestException("query must not be blank");
    }
    if (normalized.length() > MAX_QUERY_LENGTH) {
      throw new BadRequestException("query length must not exceed " + MAX_QUERY_LENGTH);
    }
    if (containsControlChars(normalized)) {
      throw new BadRequestException("query contains invalid characters");
    }
    return normalized;
  }

  /**
   * Parse the {@code tab} query parameter. Accepts any case (upper, lower, mixed) so {@code ?tab=all} and
   * {@code ?tab=ALL} are equivalent — keeps the query string forgiving to casual frontend clients without
   * weakening validation. Rejects unknown values with {@code 400 Bad Request} and never echoes the
   * caller-supplied value.
   */
  private static Tab validateTab(final String tab) {
    if (tab == null || tab.isBlank()) {
      throw new BadRequestException("tab must not be blank");
    }
    if (tab.length() > MAX_TAB_LENGTH) {
      throw new BadRequestException("tab must not exceed " + MAX_TAB_LENGTH + " characters");
    }
    try {
      return Tab.valueOf(tab.toUpperCase(Locale.ROOT));
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException("unknown tab");
    }
  }

  private static int validatePage(final Integer page) {
    int value = page == null ? 1 : page;
    if (value < 1) {
      throw new BadRequestException("page must be >= 1");
    }
    if (value > ResultsRequest.MAX_PAGE) {
      throw new BadRequestException("page must be <= " + ResultsRequest.MAX_PAGE);
    }
    return value;
  }

  private static int validatePageSize(final Integer pageSize) {
    int value = pageSize == null ? ResultsRequest.DEFAULT_PAGE_SIZE : pageSize;
    if (value < 1) {
      throw new BadRequestException("pageSize must be >= 1");
    }
    if (value > ResultsRequest.MAX_PAGE_SIZE) {
      throw new BadRequestException("pageSize must be <= " + ResultsRequest.MAX_PAGE_SIZE);
    }
    return value;
  }

  private static boolean containsControlChars(final String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isISOControl(c)) {
        return true;
      }
    }
    return false;
  }
}
