/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

/**
 * Regression: Ana / {@code POST /rest/search/index-query} sorts use {@link SortedNumericSortField},
 * whose {@link SortField#getType()} is always {@link SortField.Type#CUSTOM}. Encoding
 * {@code nextSearchAfter} must resolve the numeric type or every page-1 request with more hits than
 * pageSize fails with {@code Unsupported SortField.Type in searchAfter: CUSTOM}.
 */
public class LuceneSearchIndexClientSearchAfterEncodeTest
{
  @Test
  public void sortValueType_sortedNumericLong_resolvesNumericTypeNotCustom() {
    SortedNumericSortField field = new SortedNumericSortField("policyWaiverCreatedAt", SortField.Type.LONG, true);
    assertThat(field.getType()).isEqualTo(SortField.Type.CUSTOM);
    assertThat(LuceneSearchIndexClient.sortValueType(field)).isEqualTo(SortField.Type.LONG);
  }

  @Test
  public void sortValueType_sortedNumericFloat_resolvesNumericTypeNotCustom() {
    SortedNumericSortField field = new SortedNumericSortField("cvssScore", SortField.Type.FLOAT, true);
    assertThat(LuceneSearchIndexClient.sortValueType(field)).isEqualTo(SortField.Type.FLOAT);
  }

  @Test
  public void sortValueType_plainString_unchanged() {
    SortField field = new SortField("documentKey", SortField.Type.STRING);
    assertThat(LuceneSearchIndexClient.sortValueType(field)).isEqualTo(SortField.Type.STRING);
  }

  @Test
  public void encodeSortValue_sortedNumericLong_encodesWithoutCustomTypeError() {
    SortedNumericSortField field = new SortedNumericSortField("policyWaiverCreatedAt", SortField.Type.LONG, true);
    assertThatCode(() -> LuceneSearchIndexClient.encodeSortValue(field, 1_700_000_000_000L))
        .doesNotThrowAnyException();
    assertThat(LuceneSearchIndexClient.encodeSortValue(field, 1_700_000_000_000L))
        .isEqualTo("1700000000000");
    assertThat(LuceneSearchIndexClient.encodeSortValue(field, null)).isEqualTo("MIN");
  }

  @Test
  public void encodeSortValue_string_stillEncodesBytesRef() {
    SortField field = new SortField("documentKey", SortField.Type.STRING);
    assertThat(LuceneSearchIndexClient.encodeSortValue(field, new BytesRef("abc")))
        .isEqualTo("abc");
  }
}
