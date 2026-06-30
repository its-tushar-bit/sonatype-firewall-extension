/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

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
 * Base class for REST API regression tests. Boots the embedded IQ Server via
 * {@link AbstractResourceTest} and exposes typed HTTP helpers that hit {@code getRestBaseUrl()}
 * with admin Basic auth by default.
 *
 * <p>
 * Available to subclasses:
 * <ul>
 * <li>Authenticated HTTP helpers: {@link #apiGet}, {@link #apiPostJson}, {@link #apiPutJson},
 * {@link #apiDelete} — each emits a per-call breadcrumb ({@code API <METHOD> <path> -> <status>
 * (<ms>)}) into the per-class Failsafe output file ({@code target/failsafe-reports/<ClassName>-output.txt}).
 * <li>Anonymous helper: {@link #anonApiGet} for 401 contract tests (same breadcrumb format,
 * tagged {@code GET (anon)}).
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
          e.getClass().getSimpleName(), e.getMessage());
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

  /** Unauthenticated GET (for 401 scenarios). */
  protected HttpResponse anonApiGet(final String relativePath) throws Exception {
    return exec("GET (anon)", relativePath, () -> anonApiRequest().path(relativePath).get());
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

  /** DELETE. */
  protected HttpResponse apiDelete(final String relativePath) throws Exception {
    return exec("DELETE", relativePath, () -> apiRequest().path(relativePath).delete());
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
          e.getMessage());
      throw e;
    }
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
