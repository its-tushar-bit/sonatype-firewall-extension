/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.fieldmap;

/**
 * Value shape of a filter; selects which family of Lucene query the compiler builds.
 *
 * <ul>
 * <li>{@code KEYWORD} — whole-value match, values lower-cased ({@code LowerCaseKeywordAnalyzer}).</li>
 * <li>{@code TEXT} — tokenized ({@code StandardAnalyzer}); phrases and prefixes are meaningful.</li>
 * <li>{@code NUMERIC} — {@code IntPoint} / {@code FloatPoint} range or exact queries.</li>
 * </ul>
 */
public enum FieldKind
{
  TEXT,
  KEYWORD,
  NUMERIC
}
