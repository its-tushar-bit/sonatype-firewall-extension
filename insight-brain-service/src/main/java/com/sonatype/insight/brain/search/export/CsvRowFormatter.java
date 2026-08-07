/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.utils.CsvWritable;

/**
 * Renders a row as a CSV line against a column list, reusing the repo-wide quoting/escaping rules in
 * {@link CsvWritable} so these exports quote and escape identically to every other IQ CSV.
 * <p>
 * A multi-valued field (a {@link Collection}, e.g. an application's categories) is joined with
 * {@code "; "} into a single cell rather than spilling into extra columns, so every row has exactly
 * as many cells as the header has columns.
 * <p>
 * Cell values are additionally neutralized against spreadsheet formula injection: these exports carry
 * user- and scan-controlled free text (waiver reason/comment, vulnerability description, component and
 * application names), and a cell starting with a formula trigger would execute on open in Excel or
 * Sheets. See {@link #neutralizeFormula}.
 */
public final class CsvRowFormatter
{
  /** Separator for multiple values inside one cell. Not a comma, so a joined cell needs no re-quoting. */
  static final String MULTI_VALUE_SEPARATOR = "; ";

  /** Decimal numbers, the only form exempt from the formula guard. See {@link #isCsvNumber}. */
  private static final Pattern CSV_NUMBER = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

  private CsvRowFormatter() {
  }

  public static <R> String headerLine(final List<CsvColumn<R>> columns) {
    final StringJoiner joiner = new StringJoiner(",");
    for (CsvColumn<R> column : columns) {
      joiner.add(escape(column.header()));
    }
    return joiner.toString();
  }

  public static <R> String toCsvLine(final R row, final List<CsvColumn<R>> columns) {
    final StringJoiner joiner = new StringJoiner(",");
    for (CsvColumn<R> column : columns) {
      joiner.add(escape(render(column.value().apply(row))));
    }
    return joiner.toString();
  }

  /** Flattens a value to a single cell string; {@code null} renders empty. */
  private static String render(final Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Collection<?> many) {
      final StringJoiner joiner = new StringJoiner(MULTI_VALUE_SEPARATOR);
      for (Object element : many) {
        if (element != null) {
          joiner.add(String.valueOf(element));
        }
      }
      return joiner.toString();
    }
    return String.valueOf(value);
  }

  /**
   * Escape then quote, in that order: doubling embedded quotes first means the quoting pass sees the
   * already-escaped text and wraps it correctly. Reversing the order would double the wrapping quotes
   * too and corrupt the cell. Formula neutralization runs first so the guarding prefix is inside the
   * quoted cell rather than outside it.
   * <p>
   * A guarded cell whose value starts with whitespace is wrapped even when it holds none of
   * {@link CsvWritable}'s special characters: that leading tab, carriage return, or space is invisible and
   * a reader is free to strip it from an unwrapped cell, which would separate the apostrophe from the
   * value it guards. Wrapping pins the two together and keeps the cell round-tripping byte for byte.
   */
  private static String escape(final String value) {
    final String guarded = neutralizeFormula(value);
    final String escaped = CsvWritable.escapeDoubleQuotes(guarded);
    final String quoted = CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(escaped);
    final boolean guardedLeadingSpace =
        !guarded.equals(value) && !value.isEmpty() && isSkippableLeadingSpace(value.charAt(0));
    if (guardedLeadingSpace && quoted.equals(escaped)) {
      return "\"" + escaped + "\"";
    }
    return quoted;
  }

  /**
   * Prefixes a leading formula trigger ({@code = + - @}, or a tab / carriage return) with an apostrophe,
   * so the cell is read as literal text instead of being evaluated. The apostrophe is not displayed by
   * Excel or Sheets.
   * <p>
   * Leading spaces are skipped when looking for the trigger, because a spreadsheet trims a cell before
   * deciding whether it is a formula: {@code " =1+1"} evaluates just as {@code "=1+1"} does. A tab or
   * carriage return is not skipped, being a trigger in its own right.
   * <p>
   * A value that a CSV consumer reads as a number is left alone, so a negative number stays numeric
   * rather than becoming a text cell -- {@code -} is a trigger only when what follows it is not simply a
   * number.
   */
  private static String neutralizeFormula(final String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    int i = 0;
    while (i < value.length() && value.charAt(i) == ' ') {
      i++;
    }
    if (i == value.length()) {
      return value;
    }
    final char first = value.charAt(i);
    final boolean trigger =
        first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r';
    if (!trigger || isCsvNumber(value)) {
      return value;
    }
    return "'" + value;
  }

  /** Leading whitespace that a reader may strip from an unwrapped cell, detaching the guard prefix. */
  private static boolean isSkippableLeadingSpace(final char c) {
    return c == ' ' || c == '\t' || c == '\r' || c == '\n';
  }

  /**
   * Whether the whole value is a number in the decimal form a CSV consumer reads as numeric: an optional
   * sign, digits with at most one decimal point, and an optional decimal exponent.
   * <p>
   * Deliberately narrower than {@link Double#parseDouble}, whose grammar also accepts a {@code d}/{@code f}
   * type suffix, a hexadecimal float, and {@code Infinity}/{@code NaN}. A spreadsheet reads none of those
   * as a number, so treating them as numeric would exempt text such as {@code -5d} or {@code -NaN} from
   * the guard.
   */
  private static boolean isCsvNumber(final String value) {
    return CSV_NUMBER.matcher(value).matches();
  }
}
