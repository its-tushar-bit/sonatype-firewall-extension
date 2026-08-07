/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.junit.Test;

/**
 * Transport-level behaviour of the streaming CSV response: headers, BOM, row cap + truncation notice,
 * the empty result set, and laziness (nothing is pulled from the row source until the body is
 * written).
 */
public class CsvStreamingResponseTest
{
  private static final List<CsvColumn<Map<String, Object>>> COLUMNS = List.of(
      CsvColumn.of("Name", row -> row.get("name")));

  private static Map<String, Object> row(final String name) {
    return Map.of("name", name);
  }

  private static Response build(final List<Map<String, Object>> rows) {
    return CsvStreamingResponse.build("applications", COLUMNS, rows.iterator());
  }

  private static String bodyOf(final Response response) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    ((StreamingOutput) response.getEntity()).write(out);
    return out.toString(StandardCharsets.UTF_8);
  }

  @Test
  public void contentType_isTextCsvWithUtf8Charset() {
    assertThat(build(List.of()).getMediaType().toString()).isEqualTo("text/csv;charset=utf-8");
  }

  /**
   * CLM-38675: a charset must never be sent as a Content-Encoding. Content-Encoding names a content
   * coding (gzip/deflate); an invalid value there broke downstream clients. Assert the header is
   * absent entirely rather than merely "not utf-8".
   */
  @Test
  public void contentEncoding_isNeverSet() {
    final Response response = build(List.of(row("a")));
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_ENCODING)).isNull();
    assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.CONTENT_ENCODING);
  }

  /**
   * CLM-37981: an HTTP/1.0 client must still work. The row count is unknown up front, so neither
   * Content-Length nor a hand-set Transfer-Encoding may be present — the container picks the framing
   * (chunked on 1.1, connection-close delimited on 1.0). Hard-coding chunked is what breaks 1.0.
   */
  @Test
  public void noContentLengthOrTransferEncodingIsSetByHand() {
    final Response response = build(List.of(row("a")));
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_LENGTH)).isNull();
    assertThat(response.getHeaderString("Transfer-Encoding")).isNull();
  }

  @Test
  public void contentDisposition_isAttachmentWithTimestampedCsvFilename() {
    final String disposition = build(List.of()).getHeaderString(HttpHeaders.CONTENT_DISPOSITION);
    assertThat(disposition).startsWith("attachment; filename=\"applications-");
    assertThat(disposition).contains(".csv\"");
    // RFC 5987 form for non-ASCII safety, matching the repo's other exports.
    assertThat(disposition).contains("filename*=UTF-8''applications-");
  }

  @Test
  public void body_startsWithUtf8BomThenHeaderRow() throws Exception {
    final String body = bodyOf(build(List.of()));
    assertThat(body).startsWith("\uFEFFName");
  }

  /** An empty result set is a 200 with a header-only CSV, never an error and never a blank body. */
  @Test
  public void emptyResultSet_writesHeaderRowOnly() throws Exception {
    final Response response = build(List.of());
    assertThat(response.getStatus()).isEqualTo(200);
    final List<String> lines = dataLines(bodyOf(response));
    assertThat(lines).isEmpty();
  }

  @Test
  public void rows_areWrittenAfterTheHeaderSeparatedByCrLf() throws Exception {
    final String body = bodyOf(build(List.of(row("first"), row("second"))));
    assertThat(body).startsWith("\uFEFFName\r\nfirst\r\nsecond");
    assertThat(dataLines(body)).containsExactly("first", "second");
  }

  /** Nothing is pulled from the row source until the response body is actually written. */
  @Test
  public void rowSource_isNotConsumedUntilTheBodyIsWritten() throws Exception {
    final CountingIterator rows = new CountingIterator(3);
    final Response response = CsvStreamingResponse.build("applications", COLUMNS, rows);
    assertThat(rows.consumed).isZero();
    bodyOf(response);
    assertThat(rows.consumed).isEqualTo(3);
  }

  /**
   * At the cap the file stops and says so, rather than silently truncating. Asserts against the
   * documented constant so the cap and the notice cannot drift apart.
   */
  @Test
  public void atTheRowCap_stopsAndAppendsTheTruncationNotice() throws Exception {
    // One more row available than the cap allows, so the cap is genuinely hit.
    final CountingIterator rows = new CountingIterator(CsvExportLimits.MAX_ROWS + 1);
    final String body = bodyOf(CsvStreamingResponse.build("applications", COLUMNS, rows));
    final List<String> lines = dataLines(body);
    assertThat(lines).hasSize(CsvExportLimits.MAX_ROWS + 1);
    assertThat(lines.get(CsvExportLimits.MAX_ROWS)).isEqualTo(CsvExportLimits.TRUNCATION_NOTICE);
    // Exactly the cap of DATA rows was written; nothing beyond it.
    assertThat(lines.subList(0, CsvExportLimits.MAX_ROWS)).allSatisfy(l -> assertThat(l).startsWith("row-"));
  }

  /** Exactly at the cap with no further rows is NOT truncation, so no notice is appended. */
  @Test
  public void exactlyAtTheRowCapWithNoMoreRows_appendsNoNotice() throws Exception {
    final CountingIterator rows = new CountingIterator(CsvExportLimits.MAX_ROWS);
    final List<String> lines = dataLines(bodyOf(CsvStreamingResponse.build("applications", COLUMNS, rows)));
    assertThat(lines).hasSize(CsvExportLimits.MAX_ROWS);
    assertThat(lines).doesNotContain(CsvExportLimits.TRUNCATION_NOTICE);
  }

  @Test
  public void walkFailureAfterCommit_marksTheBodyIncomplete() throws Exception {
    // The header is flushed before the first page is fetched (the ALB guard), so the 200 is already
    // committed and a later-page failure cannot become a 4xx/5xx. It must therefore be visible in the body
    // instead of yielding a truncated file indistinguishable from a complete one.
    final Iterator<Map<String, Object>> failing = new Iterator<>()
    {
      private int served;

      @Override
      public boolean hasNext() {
        return true;
      }

      @Override
      public Map<String, Object> next() {
        if (served++ < 2) {
          return row("row-" + served);
        }
        throw new IllegalStateException("index generation changed mid-walk");
      }
    };

    final List<String> lines =
        dataLines(bodyOf(CsvStreamingResponse.build("applications", COLUMNS, failing)));

    // The rows written before the failure are kept, and the file says it is incomplete.
    assertThat(lines).containsExactly("row-1", "row-2", CsvStreamingResponse.WALK_FAILED_NOTICE);
  }

  @Test
  public void truncationNotice_isAcommentLine_notMistakableForData() {
    // A bare single-cell notice reads as a final data row to a spreadsheet user and as a wrong-column-count
    // record to a script; the '#' prefix makes it skippable.
    assertThat(CsvExportLimits.TRUNCATION_NOTICE).startsWith("#");
    assertThat(CsvStreamingResponse.WALK_FAILED_NOTICE).startsWith("#");
  }

  /** Body data lines, with the BOM, header row, and trailing blank removed. */
  private static List<String> dataLines(final String body) {
    final String withoutBom = body.startsWith("\uFEFF") ? body.substring(1) : body;
    final String[] parts = withoutBom.split("\r\n", -1);
    final List<String> lines = new ArrayList<>();
    // parts[0] is the header row; the final element is the trailing empty string after the last CRLF.
    for (int i = 1; i < parts.length; i++) {
      if (!(i == parts.length - 1 && parts[i].isEmpty())) {
        lines.add(parts[i]);
      }
    }
    return lines;
  }

  /** Yields {@code total} rows and records how many were actually pulled. */
  private static final class CountingIterator
      implements Iterator<Map<String, Object>>
  {
    private final int total;

    private int consumed;

    private CountingIterator(final int total) {
      this.total = total;
    }

    @Override
    public boolean hasNext() {
      return consumed < total;
    }

    @Override
    public Map<String, Object> next() {
      final Map<String, Object> row = row("row-" + consumed);
      consumed++;
      return row;
    }
  }
}
