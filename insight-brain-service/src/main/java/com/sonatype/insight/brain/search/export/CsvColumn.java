/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import java.util.function.Function;

/**
 * One CSV column: the header label plus how to read the value off a row.
 *
 * @param header the header-row label.
 * @param value reads the raw value from a row; may return {@code null} for an absent value, which
 *          renders as an empty field.
 */
public record CsvColumn<R>(String header, Function<R, Object> value)
{
  public static <R> CsvColumn<R> of(final String header, final Function<R, Object> value) {
    return new CsvColumn<>(header, value);
  }
}
