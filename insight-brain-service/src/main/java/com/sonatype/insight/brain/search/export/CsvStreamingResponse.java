/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.utils.HttpHeaderUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the streaming CSV response for a list export.
 * <p>
 * Header/transport decisions, each one deliberate:
 * <ul>
 * <li>{@code Content-Type: text/csv; charset=utf-8} — the charset travels in the Content-Type
 * parameter. No {@code Content-Encoding} header is set at all: {@code Content-Encoding} names a
 * content coding (gzip/deflate), and putting a charset there produces an invalid header that broke
 * downstream clients before (CLM-38675).</li>
 * <li>No {@code Content-Length} and no {@code Transfer-Encoding} set by hand. The row count is not
 * known until the walk finishes, so the container picks the framing: chunked on HTTP/1.1, and
 * connection-close delimited on HTTP/1.0, which is what keeps an HTTP/1.0 client working
 * (CLM-37981). Hard-coding {@code Transfer-Encoding: chunked} is what breaks HTTP/1.0.</li>
 * <li>The header row is written and flushed before the first index page is fetched, and every row
 * is flushed as it is written, so bytes start moving immediately and never stall for the length of
 * a slow walk. An idle proxy/ALB (typically 60s) therefore has no window to drop the connection
 * (CLM-38045).</li>
 * <li>A UTF-8 BOM is written first so Excel opens non-ASCII exports correctly, matching the
 * Vulnerabilities export precedent.</li>
 * </ul>
 * <p>
 * Because the header is flushed before the first page is fetched, the 200 status is committed before the
 * walk can fail. A later-page failure therefore cannot become a 4xx/5xx, so it is instead made visible in
 * three ways: a structured {@code log.error} carrying the export name, an in-band {@code #} error line
 * appended to the body, and no trailing blank line -- so a failed export is self-describing rather than
 * indistinguishable from a complete file.
 */
public final class CsvStreamingResponse
{
  private static final DateTimeFormatter UTC_FILENAME_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

  static final String CONTENT_TYPE = "text/csv; charset=utf-8";

  /** CSV line terminator; CRLF per RFC 4180, matching the repo's other exports. */
  private static final String EOL = "\r\n";

  private static final Logger log = LoggerFactory.getLogger(CsvStreamingResponse.class);

  /**
   * In-band notice appended when the row walk fails after the response was already committed. Prefixed
   * with {@code #} for the same reason the truncation notice is: a consumer must be able to tell it from
   * data.
   */
  static final String WALK_FAILED_NOTICE =
      "# Export failed before all rows were written; this file is incomplete. Retry the export.";

  private CsvStreamingResponse() {
  }

  /**
   * @param fileNamePrefix filename stem; the response filename is {@code <prefix>-<utc timestamp>.csv}.
   * @param columns header labels + per-row value readers.
   * @param rows lazily consumed; the iterator is advanced only while the response body is being
   *          written, so no full result set is ever held in memory.
   */
  public static <R> Response build(
      final String fileNamePrefix,
      final List<CsvColumn<R>> columns,
      final Iterator<R> rows)
  {
    final Date now = new Date();
    final String filename = fileNamePrefix + "-" + UTC_FILENAME_TIMESTAMP.format(Instant.now()) + ".csv";
    final String headerLine = CsvRowFormatter.headerLine(columns);

    final StreamingOutput body = os -> {
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
        writer.write('\uFEFF');
        writer.write(headerLine);
        // Flush the header before touching the index, so the client sees bytes immediately even if
        // the first page is slow. This is the ALB-idle-timeout guard, not a micro-optimisation.
        writer.flush();
        int written = 0;
        try {
          while (rows.hasNext() && written < CsvExportLimits.MAX_ROWS) {
            final R row = rows.next();
            writeLine(writer, CsvRowFormatter.toCsvLine(row, columns));
            written++;
          }
          if (written >= CsvExportLimits.MAX_ROWS && rows.hasNext()) {
            writeLine(writer, CsvExportLimits.TRUNCATION_NOTICE);
          }
        }
        catch (RuntimeException e) {
          // The status and header row are already flushed, so this can no longer become a 4xx/5xx. Make the
          // failure visible instead: log it with the export name and row count, and mark the body itself.
          log.error("CSV export '{}' failed after {} row(s); returning an incomplete file",
              fileNamePrefix, written, e);
          writeLine(writer, WALK_FAILED_NOTICE);
          writer.flush();
          return;
        }
        writer.write(EOL);
      }
    };

    return Response.ok()
        .type(CONTENT_TYPE)
        .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(filename))
        .lastModified(now)
        .expires(now)
        .entity(body)
        .build();
  }

  /** Writes one data line and flushes, keeping bytes flowing on a slow walk. */
  private static void writeLine(final Writer writer, final String line) {
    try {
      writer.write(EOL);
      writer.write(line);
      writer.flush();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
