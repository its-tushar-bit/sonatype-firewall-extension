/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Quoting/escaping behaviour of the shared list-export CSV formatter. Drives the formatter through a
 * simple {@code Map} row so the assertions are about CSV rendering, not about any entity's columns.
 */
public class CsvRowFormatterTest
{
  private static final List<CsvColumn<Map<String, Object>>> TWO_COLUMNS = List.of(
      CsvColumn.of("First", row -> row.get("a")),
      CsvColumn.of("Second", row -> row.get("b")));

  private static String line(final Object a, final Object b) {
    // HashMap-safe: values may be null, so build via an explicit mutable map rather than Map.of.
    final java.util.Map<String, Object> row = new java.util.HashMap<>();
    row.put("a", a);
    row.put("b", b);
    return CsvRowFormatter.toCsvLine(row, TWO_COLUMNS);
  }

  @Test
  public void headerLine_rendersColumnHeadersCommaSeparated() {
    assertThat(CsvRowFormatter.headerLine(TWO_COLUMNS)).isEqualTo("First,Second");
  }

  @Test
  public void plainValues_areNotQuoted() {
    assertThat(line("acme-prod", "Acme")).isEqualTo("acme-prod,Acme");
  }

  @Test
  public void nullValue_rendersAsEmptyField() {
    assertThat(line(null, "Acme")).isEqualTo(",Acme");
    assertThat(line("Acme", null)).isEqualTo("Acme,");
  }

  /** A comma inside a value must be quoted, otherwise it would split into an extra column. */
  @Test
  public void valueWithComma_isQuoted() {
    assertThat(line("Smith, John", "x")).isEqualTo("\"Smith, John\",x");
  }

  /**
   * A quote inside a value is doubled AND the cell is wrapped. Escaping must happen before quoting;
   * the reverse order would double the wrapping quotes and corrupt the cell.
   */
  @Test
  public void valueWithQuote_isEscapedAndQuoted() {
    assertThat(line("say \"hi\"", "x")).isEqualTo("\"say \"\"hi\"\"\",x");
  }

  /** An embedded newline must be quoted so it stays inside one logical CSV record. */
  @Test
  public void valueWithNewline_isQuoted() {
    assertThat(line("line1\nline2", "x")).isEqualTo("\"line1\nline2\",x");
    assertThat(line("line1\r\nline2", "x")).isEqualTo("\"line1\r\nline2\",x");
  }

  @Test
  public void valueWithCommaQuoteAndNewline_isEscapedAndQuotedTogether() {
    assertThat(line("a,b \"c\"\nd", "x")).isEqualTo("\"a,b \"\"c\"\"\nd\",x");
  }

  /** Unicode passes through unaltered; the response writes UTF-8 with a BOM. */
  @Test
  public void unicodeValue_isPreservedAndNotQuoted() {
    assertThat(line("Ünïcodé-app-日本語", "Ωmega")).isEqualTo("Ünïcodé-app-日本語,Ωmega");
  }

  @Test
  public void unicodeValueWithComma_isQuoted() {
    assertThat(line("東京, 日本", "x")).isEqualTo("\"東京, 日本\",x");
  }

  /** A multi-valued field collapses into ONE cell, so the row keeps the header's column count. */
  @Test
  public void collectionValue_isJoinedIntoASingleCell() {
    assertThat(line(List.of("Finance", "Retail"), "x")).isEqualTo("Finance; Retail,x");
  }

  @Test
  public void collectionValue_withCommaInAnElement_isQuotedOnce() {
    assertThat(line(List.of("Finance, EU", "Retail"), "x")).isEqualTo("\"Finance, EU; Retail\",x");
  }

  @Test
  public void emptyCollectionValue_rendersAsEmptyField() {
    assertThat(line(List.of(), "x")).isEqualTo(",x");
  }

  @Test
  public void numericAndBooleanValues_renderAsPlainText() {
    assertThat(line(9, true)).isEqualTo("9,true");
  }

  @Test
  public void formulaTriggeringCell_isPrefixedSoSpreadsheetsTreatItAsText() {
    // These exports carry user- and scan-controlled free text (waiver reason/comment, vulnerability
    // description, component and app names). A cell starting with a formula trigger would execute on open
    // in Excel/Sheets, so it is prefixed with an apostrophe and read as literal text instead.
    assertThat(line("=1+1", "x")).isEqualTo("'=1+1,x");
    assertThat(line("+SUM(A1)", "x")).isEqualTo("'+SUM(A1),x");
    assertThat(line("@import", "x")).isEqualTo("'@import,x");
    // No comma/quote/newline in this payload, so it needs no RFC-4180 wrapping -- only the guard prefix.
    assertThat(line("-cmd|'/c calc'!A1", "x")).isEqualTo("'-cmd|'/c calc'!A1,x");
    // A trigger AND a comma: the guard prefix goes inside the quoted cell, not outside it.
    assertThat(line("=HYPERLINK(\"a\",\"b\")", "x")).isEqualTo("\"'=HYPERLINK(\"\"a\"\",\"\"b\"\")\",x");
  }

  @Test
  public void ordinaryAndNumericCells_areNotPrefixed() {
    // The guard must not touch normal data, including negative numbers, which stay numeric cells.
    assertThat(line("plain text", "x")).isEqualTo("plain text,x");
    assertThat(line(9, "x")).isEqualTo("9,x");
    assertThat(line(-5, "x")).isEqualTo("-5,x");
    assertThat(line("-2.5", "x")).isEqualTo("-2.5,x");
  }

  /**
   * npm scoped package names start with {@code @}, so they are the formula trigger that actually occurs
   * in real exported data rather than a contrived payload. Asserted on the exact emitted cell.
   */
  @Test
  public void scopedNpmComponentNames_areGuarded() {
    assertThat(line("@para-snack/core 0.0.8", "0.0.8")).isEqualTo("'@para-snack/core 0.0.8,0.0.8");
    assertThat(line("@microsoft/applicationinsights-teechannel-js", "x"))
        .isEqualTo("'@microsoft/applicationinsights-teechannel-js,x");
    // The coordinates cell of the same row percent-encodes the scope, so it is not a trigger at all.
    assertThat(line("pkg:a-name/%40para-snack%2Fcore@0.0.8", "x"))
        .isEqualTo("pkg:a-name/%40para-snack%2Fcore@0.0.8,x");
  }

  /**
   * A tab- or CR-leading cell is wrapped as well as prefixed. A leading tab or CR is invisible and is
   * stripped by a spreadsheet before it parses the cell, so the guard prefix alone can be separated from
   * the value it guards; wrapping keeps the two together and keeps the cell round-tripping.
   */
  @Test
  public void tabOrCarriageReturnLeadingCell_isGuardedAndQuoted() {
    assertThat(line("\tSUM(A1)", "x")).isEqualTo("\"'\tSUM(A1)\",x");
    assertThat(line("\r=1+1", "x")).isEqualTo("\"'\r=1+1\",x");
  }

  /**
   * A spreadsheet trims leading whitespace before deciding whether a cell is a formula, so a trigger
   * behind a space is still a trigger.
   */
  @Test
  public void triggerBehindLeadingWhitespace_isGuarded() {
    assertThat(line(" =1+1", "x")).isEqualTo("\"' =1+1\",x");
    assertThat(line("  @import", "x")).isEqualTo("\"'  @import\",x");
  }

  /**
   * The numeric exemption covers values a CSV consumer reads as numbers. Java's own float syntax is wider
   * than that -- a type suffix, a hex float, or a non-finite word is text to a spreadsheet, and text that
   * begins with a trigger, so it is guarded rather than exempted.
   */
  @Test
  public void valuesThatAreNotCsvNumbers_areGuardedEvenThoughJavaWouldParseThem() {
    assertThat(line("-5d", "x")).isEqualTo("'-5d,x");
    assertThat(line("-5f", "x")).isEqualTo("'-5f,x");
    assertThat(line("-Infinity", "x")).isEqualTo("'-Infinity,x");
    assertThat(line("-NaN", "x")).isEqualTo("'-NaN,x");
  }

  /** Genuine numbers, in every form a CSV consumer reads as numeric, stay unguarded and analysable. */
  @Test
  public void genuineNegativeNumbers_stayNumeric() {
    assertThat(line("-5", "x")).isEqualTo("-5,x");
    assertThat(line("-3.2", "x")).isEqualTo("-3.2,x");
    assertThat(line(-3.2, "x")).isEqualTo("-3.2,x");
    assertThat(line("-0", "x")).isEqualTo("-0,x");
    assertThat(line("-1e6", "x")).isEqualTo("-1e6,x");
    assertThat(line("-1.5E-3", "x")).isEqualTo("-1.5E-3,x");
    assertThat(line("+7", "x")).isEqualTo("+7,x");
  }

  /** Whatever the guard emits must parse back to the original value in a normal CSV reader. */
  @Test
  public void guardedCells_roundTripThroughACsvReader() {
    assertThat(firstCellReadBack("@para-snack/core 0.0.8")).isEqualTo("'@para-snack/core 0.0.8");
    assertThat(firstCellReadBack("\tSUM(A1)")).isEqualTo("'\tSUM(A1)");
    assertThat(firstCellReadBack(" =1+1")).isEqualTo("' =1+1");
    assertThat(firstCellReadBack("=HYPERLINK(\"a\",\"b\")")).isEqualTo("'=HYPERLINK(\"a\",\"b\")");
    // An unguarded numeric cell round-trips unchanged, with no stray prefix.
    assertThat(firstCellReadBack("-3.2")).isEqualTo("-3.2");
  }

  /**
   * Parses the first cell of an emitted line back with RFC-4180 rules (wrapping quotes removed, doubled
   * quotes collapsed), so the assertions above are about what a reader actually recovers.
   */
  private static String firstCellReadBack(final Object value) {
    final String emitted = line(value, "x");
    final String cell = emitted.substring(0, emitted.length() - ",x".length());
    if (cell.startsWith("\"") && cell.endsWith("\"")) {
      return cell.substring(1, cell.length() - 1).replace("\"\"", "\"");
    }
    return cell;
  }

  /** Every row must have exactly as many cells as the header has columns. */
  @Test
  public void everyRow_hasTheSameCellCountAsTheHeader() {
    final int headerCells = CsvRowFormatter.headerLine(TWO_COLUMNS).split(",", -1).length;
    assertThat(headerCells).isEqualTo(2);
    assertThat(line("plain", "plain").split(",", -1)).hasSize(2);
    assertThat(line(null, null).split(",", -1)).hasSize(2);
  }
}
