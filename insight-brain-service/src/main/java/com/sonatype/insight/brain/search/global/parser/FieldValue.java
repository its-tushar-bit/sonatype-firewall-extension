/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.parser;

/**
 * Value side of a {@code field:value} predicate produced by the query parser.
 */
public sealed interface FieldValue
    permits FieldValue.ExactValue,
    FieldValue.PhraseValue,
    FieldValue.PrefixValue,
    FieldValue.RangeValue,
    FieldValue.EmptyValue
{
  record ExactValue(String value)
      implements FieldValue
  {
  }

  record PhraseValue(String value)
      implements FieldValue
  {
  }

  record PrefixValue(String prefix)
      implements FieldValue
  {
  }

  record RangeValue(String lo, String hi, boolean loInclusive, boolean hiInclusive)
      implements FieldValue
  {
  }

  record EmptyValue()
      implements FieldValue
  {
  }
}
