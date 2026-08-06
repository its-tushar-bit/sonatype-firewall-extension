/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.Sort;
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

  /**
   * Full cursor round-trip for each numeric width plus the appended keyword tie-breaker: encode a
   * boundary tuple, then decode it back into the {@link FieldDoc} Lucene is handed for page 2. Decoding
   * keys off the same {@code sortValueType}, so an INT sort must come back as an {@link Integer} (not a
   * {@link Long}) — a widened slot makes Lucene's INT comparator reject the cursor.
   */
  @Test
  public void searchAfterCursor_roundTripsEachNumericWidthAndTheKeywordTieBreaker() {
    Sort sort = new Sort(
        new SortedNumericSortField("policyViolationThreatLevel", SortField.Type.INT, true),
        new SortedNumericSortField("policyWaiverCreatedAt", SortField.Type.LONG, true),
        new SortedNumericSortField("cvssScore", SortField.Type.FLOAT, true),
        new SortField("documentKey", SortField.Type.STRING));
    Object[] boundary = {7, 1_700_000_000_000L, 7.5f, new BytesRef("doc-key-1")};

    List<String> encoded = new ArrayList<>();
    for (int i = 0; i < boundary.length; i++) {
      encoded.add(LuceneSearchIndexClient.encodeSortValue(sort.getSort()[i], boundary[i]));
    }
    assertThat(encoded).containsExactly("7", "1700000000000", "7.5", "doc-key-1");

    FieldDoc decoded = LuceneSearchIndexClient.decodeFieldDocAfter(encoded, sort);

    assertThat(decoded.fields).containsExactly(7, 1_700_000_000_000L, 7.5f, new BytesRef("doc-key-1"));
    // The INT slot must decode to Integer: SortedNumericSortField reads the cursor value at its declared
    // width, so a Long here is rejected the same way a Long missingValue is on an INT sort.
    assertThat(decoded.fields[0]).isInstanceOf(Integer.class);
    assertThat(decoded.fields[1]).isInstanceOf(Long.class);
    assertThat(decoded.fields[2]).isInstanceOf(Float.class);
  }

  /** A missing numeric boundary encodes to a sentinel that decodes back to the type's MIN, not to null. */
  @Test
  public void searchAfterCursor_roundTripsMissingNumericBoundaryAsTypedSentinel() {
    Sort sort = new Sort(
        new SortedNumericSortField("policyViolationThreatLevel", SortField.Type.INT, true),
        new SortedNumericSortField("policyWaiverCreatedAt", SortField.Type.LONG, true));

    List<String> encoded = List.of(
        LuceneSearchIndexClient.encodeSortValue(sort.getSort()[0], null),
        LuceneSearchIndexClient.encodeSortValue(sort.getSort()[1], null));
    assertThat(encoded).containsExactly("MIN", "MIN");

    FieldDoc decoded = LuceneSearchIndexClient.decodeFieldDocAfter(encoded, sort);

    assertThat(decoded.fields).containsExactly(Integer.MIN_VALUE, Long.MIN_VALUE);
  }
}
