/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Stream;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * Dashboard-specific CSV utility class
 *
 * @since 1.24.0
 */
public final class Csv
{
  private static final DateTimeFormatter UTC_FILENAME_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

  private Csv() {
    // utility class
  }

  /**
   * Fill out the supplied response with required headers and data for downloading the supplied
   * results as a CSV file.
   *
   * <p>
   * Buffered overload: the entire collection is iterated inside a {@link StreamingOutput} but
   * the writer flushes only at end of iteration. Suitable for callers that produce small,
   * already-materialised result sets.
   *
   * @param response The response to fill in
   * @param fileNamePrefix The file name prefix to use before timestamp
   * @param headerLine The CSV header line
   * @param results The CSV content
   */
  public static ResponseBuilder generate(
      final ResponseBuilder response,
      final String fileNamePrefix,
      final String headerLine,
      final Collection<? extends CsvWritable> results)
  {
    return generateInternal(
        response, fileNamePrefix, headerLine, results.stream(), false /* flushPerRow */, false /* utf8Bom */);
  }

  /**
   * Like {@link #generate(ResponseBuilder, String, String, Collection)} but prefixes the file with a
   * UTF-8 BOM so Excel opens Unicode CSV correctly (Martha Vulnerabilities export / CLM-42216).
   */
  public static ResponseBuilder generateWithUtf8Bom(
      final ResponseBuilder response,
      final String fileNamePrefix,
      final String headerLine,
      final Collection<? extends CsvWritable> results)
  {
    return generateInternal(
        response, fileNamePrefix, headerLine, results.stream(), false /* flushPerRow */, true /* utf8Bom */);
  }

  /**
   * Stream variant of {@link #generate(ResponseBuilder, String, String, Collection)}.
   *
   * <p>
   * Lazy: the supplied {@link Stream} is consumed only when JAX-RS invokes the
   * {@link StreamingOutput} write callback, and is closed via try-with-resources so the
   * stream's {@code onClose} handlers fire. The writer flushes once after all rows are
   * written; for callers that need bytes flowing aggressively (e.g. behind an ALB with a
   * 60s idle timeout) use the {@code flushPerRow} overload.
   *
   * @param response The response to fill in
   * @param fileNamePrefix The file name prefix to use before timestamp
   * @param headerLine The CSV header line
   * @param results The CSV content as a lazy stream; closed when the response body finishes writing
   */
  public static ResponseBuilder generate(
      final ResponseBuilder response,
      final String fileNamePrefix,
      final String headerLine,
      final Stream<? extends CsvWritable> results)
  {
    return generateInternal(response, fileNamePrefix, headerLine, results, false /* flushPerRow */, false);
  }

  /**
   * Stream variant with explicit flush control.
   *
   * <p>
   * Set {@code flushPerRow=true} for endpoints behind a load balancer or proxy with an
   * idle-timeout that may drop slow exports. Per-row flushing trades extra syscalls for
   * keeping bytes flowing on sparse exports. CLM-38045: ALB drops connections with no
   * activity for 60s; flushing after each row prevents this on large date-range exports
   * that produce few rows per file.
   *
   * @param response The response to fill in
   * @param fileNamePrefix The file name prefix to use before timestamp
   * @param headerLine The CSV header line
   * @param results The CSV content as a lazy stream; closed when the response body finishes writing
   * @param flushPerRow {@code true} to flush after every row written (proxy-friendly);
   *          {@code false} to flush once at end of iteration (default)
   */
  public static ResponseBuilder generate(
      final ResponseBuilder response,
      final String fileNamePrefix,
      final String headerLine,
      final Stream<? extends CsvWritable> results,
      final boolean flushPerRow)
  {
    return generateInternal(response, fileNamePrefix, headerLine, results, flushPerRow, false);
  }

  private static ResponseBuilder generateInternal(
      final ResponseBuilder response,
      final String fileNamePrefix,
      final String headerLine,
      final Stream<? extends CsvWritable> results,
      final boolean flushPerRow,
      final boolean utf8Bom)
  {
    final Date now = new Date();
    final String filename = fileNamePrefix + "-" + UTC_FILENAME_TIMESTAMP.format(Instant.now()) + ".csv";

    response.lastModified(now);
    response.expires(now);
    response.type("text/csv; charset=utf-8");
    response.header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(filename));

    StreamingOutput stream = os -> {
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
          Stream<? extends CsvWritable> rows = results)
      {
        if (utf8Bom) {
          writer.write('\uFEFF');
        }
        writer.write(headerLine);
        if (flushPerRow) {
          writer.flush();
        }
        rows.forEach(row -> {
          try {
            writer.write("\r\n");
            writer.write(row.toCsvLine());
            if (flushPerRow) {
              // CLM-38045: ALB / corporate-proxy idle timeout (typ. 60s) drops the connection if the
              // response stalls. Aggressive per-row flush prevents this on sparse exports where row
              // arrival is slow. Trade-off: one syscall per row vs. dropped exports. Do NOT remove
              // without measuring against a real proxy under load. Disabled by default for callers
              // that don't need it (most CSV producers in this codebase).
              writer.flush();
            }
          }
          catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
      }
    };

    return response.entity(stream);
  }
}
