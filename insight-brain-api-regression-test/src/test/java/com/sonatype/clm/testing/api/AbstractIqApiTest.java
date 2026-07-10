/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.MediaType;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for REST API regression tests. Uses the embedded IQ Server exposed by
 * {@link AbstractResourceTest}; because {@code testCLMServer} is {@code static} in
 * {@code AbstractBaseIntegrationTest} and Failsafe runs with {@code reuseForks=true}, the
 * server is shared across every {@code AbstractIqApiTest} subclass within a fork —
 * per-test isolation is achieved via {@code TemporaryEntity}, not by re-booting the server.
 * Exposes typed HTTP helpers that hit {@code getRestBaseUrl()} with admin Basic auth by
 * default.
 *
 * <p>
 * Available to subclasses:
 * <ul>
 * <li>Authenticated HTTP helpers: {@link #apiGet}, {@link #apiPostJson}, {@link #apiPutJson},
 * {@link #apiPut}, {@link #apiDelete} — each emits a per-call breadcrumb ({@code API <METHOD>
 * <path> -> <status> (<ms>)}) into the per-class Failsafe output file
 * ({@code target/failsafe-reports/<ClassName>-output.txt}). {@code apiGet} and {@code apiDelete}
 * have query-parameter overloads that URL-encode values correctly — never embed
 * {@code ?foo=bar} in the path string; {@link HttpRequest#path} encodes {@code ?} to
 * {@code %3F} and routes to 404.
 * <li>Anonymous helpers for 401 contract tests across every verb: {@link #anonApiGet}
 * (plus a query-parameter overload), {@link #anonApiPostJson}, {@link #anonApiPutJson},
 * {@link #anonApiPut} (body-less, for endpoints like {@code /move/destination/}), and
 * {@link #anonApiDelete}. Same breadcrumb format as the authenticated helpers, tagged
 * {@code GET (anon)} / {@code POST (anon)} / etc.
 * <li>Raw builders: {@link #apiRequest()} (admin Basic auth), {@link #anonApiRequest()} (no
 * credentials) — use these only when the typed helpers don't fit (multipart uploads,
 * custom headers, etc.); raw calls do <em>not</em> emit breadcrumbs.
 * <li>Unique-data helpers: {@link #uniqueId(String)} ({@code prefix-<uuid>} for kebab-style ids)
 * and {@link #uniqueName(String)} ({@code prefix <uuid>} for display names). Always seed with
 * these — hardcoded ids collide across reused forks.
 * <li>{@code @Rule apiTestLogger} — brackets each method with {@code START} / {@code PASS} /
 * {@code FAIL} markers (with elapsed ms) in the per-class output file.
 * </ul>
 *
 * @see com.sonatype.clm.testing.api.categories.ApiRegressionTest
 * @see AbstractResourceTest
 */
public abstract class AbstractIqApiTest
    extends AbstractResourceTest
{
  private static final Logger log = LoggerFactory.getLogger(AbstractIqApiTest.class);

  @Rule
  public final TestWatcher apiTestLogger = new TestWatcher()
  {
    private long startedNanos;

    @Override
    protected void starting(final Description description) {
      startedNanos = System.nanoTime();
      log.info("===== START {}.{} =====", description.getTestClass().getSimpleName(),
          description.getMethodName());
    }

    @Override
    protected void succeeded(final Description description) {
      log.info("===== PASS  {} ({} ms) =====", description.getMethodName(), elapsedMs());
    }

    @Override
    protected void failed(final Throwable e, final Description description) {
      log.warn("===== FAIL  {} ({} ms): {}: {} =====", description.getMethodName(), elapsedMs(),
          e.getClass().getSimpleName(), e.getMessage(), e);
    }

    private long elapsedMs() {
      return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }
  };

  /** Authenticated request against the embedded IQ REST base URL. */
  protected HttpRequest apiRequest() {
    return restRequest();
  }

  /** Unauthenticated request (for 401 scenarios). */
  protected HttpRequest anonApiRequest() {
    return restRequest().anon();
  }

  /** GET against the embedded IQ server. */
  protected HttpResponse apiGet(final String relativePath) throws Exception {
    return exec("GET", relativePath, () -> apiRequest().path(relativePath).get());
  }

  /**
   * GET with a single named query parameter (one or more values). Use this overload instead of
   * embedding {@code ?foo=bar} in {@code relativePath} — {@link #apiRequest()}.{@code .path()}
   * URL-encodes the whole string and turns {@code ?} into {@code %3F}, which routes to 404.
   */
  protected HttpResponse apiGet(
      final String relativePath,
      final String queryName,
      final Object... queryValues) throws Exception
  {
    return exec("GET", pathWithQuery(relativePath, queryName, queryValues),
        () -> apiRequest().path(relativePath).query(queryName, queryValues).get());
  }

  /** Unauthenticated GET (for 401 scenarios). */
  protected HttpResponse anonApiGet(final String relativePath) throws Exception {
    return exec("GET (anon)", relativePath, () -> anonApiRequest().path(relativePath).get());
  }

  /** Unauthenticated GET with a single named query parameter (for 401 scenarios). */
  protected HttpResponse anonApiGet(
      final String relativePath,
      final String queryName,
      final Object... queryValues) throws Exception
  {
    return exec("GET (anon)", pathWithQuery(relativePath, queryName, queryValues),
        () -> anonApiRequest().path(relativePath).query(queryName, queryValues).get());
  }

  /** Unauthenticated POST with JSON body (for 401 scenarios on mutating verbs). */
  protected HttpResponse anonApiPostJson(final String relativePath, final Object body) throws Exception {
    return exec("POST (anon)", relativePath,
        () -> anonApiRequest().path(relativePath).body(body, MediaType.APPLICATION_JSON).post());
  }

  /** Unauthenticated PUT with JSON body (for 401 scenarios on mutating verbs). */
  protected HttpResponse anonApiPutJson(final String relativePath, final Object body) throws Exception {
    return exec("PUT (anon)", relativePath,
        () -> anonApiRequest().path(relativePath).body(body, MediaType.APPLICATION_JSON).put());
  }

  /**
   * Unauthenticated PUT without a body — mirrors {@link #apiPut(String)} for 401 scenarios on
   * body-less mutating endpoints (e.g. {@code /organizations/{id}/move/destination/{destId}}).
   */
  protected HttpResponse anonApiPut(final String relativePath) throws Exception {
    return exec("PUT (anon)", relativePath, () -> anonApiRequest().path(relativePath).put());
  }

  /** Unauthenticated DELETE (for 401 scenarios on mutating verbs). */
  protected HttpResponse anonApiDelete(final String relativePath) throws Exception {
    return exec("DELETE (anon)", relativePath, () -> anonApiRequest().path(relativePath).delete());
  }

  /** POST with JSON body. */
  protected HttpResponse apiPostJson(final String relativePath, final Object body) throws Exception {
    return exec("POST", relativePath,
        () -> apiRequest().path(relativePath).body(body, MediaType.APPLICATION_JSON).post());
  }

  /** PUT with JSON body. */
  protected HttpResponse apiPutJson(final String relativePath, final Object body) throws Exception {
    return exec("PUT", relativePath,
        () -> apiRequest().path(relativePath).body(body, MediaType.APPLICATION_JSON).put());
  }

  /**
   * PUT without a body — for endpoints whose arguments are entirely path/query parameters
   * (e.g. {@code /organizations/{id}/move/destination/{destinationId}}). Do not pass an empty
   * JSON body to those endpoints; it works today but is not part of the contract.
   */
  protected HttpResponse apiPut(final String relativePath) throws Exception {
    return exec("PUT", relativePath, () -> apiRequest().path(relativePath).put());
  }

  /** DELETE. */
  protected HttpResponse apiDelete(final String relativePath) throws Exception {
    return exec("DELETE", relativePath, () -> apiRequest().path(relativePath).delete());
  }

  /** DELETE with a single named query parameter. */
  protected HttpResponse apiDelete(
      final String relativePath,
      final String queryName,
      final Object... queryValues) throws Exception
  {
    return exec("DELETE", pathWithQuery(relativePath, queryName, queryValues),
        () -> apiRequest().path(relativePath).query(queryName, queryValues).delete());
  }

  /**
   * Times the call and logs {@code API <method> <path> -> <status> (<ms> ms)}, writing a WARN-level breadcrumb on
   * exception.
   */
  private static HttpResponse exec(
      final String method,
      final String path,
      final Callable<HttpResponse> call) throws Exception
  {
    long startedNanos = System.nanoTime();
    try {
      HttpResponse response = call.call();
      long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
      log.info("API {} {} -> {} ({} ms)", method, path, response.getStatusCode(), elapsedMs);
      return response;
    }
    catch (Exception e) {
      long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
      log.warn("API {} {} -> threw {} ({} ms): {}", method, path, e.getClass().getSimpleName(), elapsedMs,
          e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Builds the URL that will appear in the breadcrumb — values are URL-encoded so the log line
   * matches what the underlying {@link HttpRequest#query} actually sends over the wire (spaces
   * become {@code +}, {@code &} becomes {@code %26}, etc.). Do not use this to construct the
   * request URL — {@code apiRequest().query(name, values)} handles that.
   */
  private static String pathWithQuery(final String path, final String name, final Object[] values) {
    if (values == null || values.length == 0) {
      return path;
    }
    if (name == null) {
      throw new IllegalArgumentException(
          "Query parameter name is null; pass a non-null name or omit the parameter entirely");
    }
    String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
    StringBuilder sb = new StringBuilder(path).append('?');
    for (int i = 0; i < values.length; i++) {
      Object value = values[i];
      if (value == null) {
        throw new IllegalArgumentException(
            "Query value at index " + i + " for name '" + name + "' is null; "
                + "pass an empty string to send an empty value, or omit the parameter entirely");
      }
      if (i > 0) {
        sb.append('&');
      }
      sb.append(encodedName).append('=').append(URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8));
    }
    return sb.toString();
  }

  /** {@code prefix-<uuid>} for kebab-style identifiers (publicIds, slugs). */
  protected static String uniqueId(final String prefix) {
    return prefix + "-" + uuidShort();
  }

  /** {@code prefix <uuid>} for human-readable display names. */
  protected static String uniqueName(final String prefix) {
    return prefix + " " + uuidShort();
  }

  private static String uuidShort() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}
