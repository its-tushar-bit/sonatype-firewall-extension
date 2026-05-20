/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KnownCountExtractorTest
{
  @Test
  public void extractCount_null_returnsZero() {
    int result = KnownCountExtractor.extractCount(null);
    assertThat(result).isEqualTo(0);
  }

  @Test
  public void extractCount_emptyArray_returnsZero() {
    int result = KnownCountExtractor.extractCount(new String[0]);
    assertThat(result).isEqualTo(0);
  }

  @Test
  public void extractCount_array_returnsArrayLength() {
    int result = KnownCountExtractor.extractCount(new String[]{"a", "b", "c"});
    assertThat(result).isEqualTo(3);
  }

  @Test
  public void extractCount_emptyCollection_returnsZero() {
    int result = KnownCountExtractor.extractCount(Collections.emptyList());
    assertThat(result).isEqualTo(0);
  }

  @Test
  public void extractCount_collection_returnsCollectionSize() {
    List<String> list = Arrays.asList("a", "b", "c", "d");
    int result = KnownCountExtractor.extractCount(list);
    assertThat(result).isEqualTo(4);
  }

  @Test
  public void extractCount_singleObject_returnsOne() {
    int result = KnownCountExtractor.extractCount("single string object");
    assertThat(result).isEqualTo(1);
  }

  @Test
  public void extractCount_singleInteger_returnsOne() {
    int result = KnownCountExtractor.extractCount(42);
    assertThat(result).isEqualTo(1);
  }

  @Test
  public void extractCount_singleObjectWithNullFields_returnsOne() {
    // A single object response (e.g., individual component lookup) counts as 1
    Object singleObject = new Object();
    int result = KnownCountExtractor.extractCount(singleObject);
    assertThat(result).isEqualTo(1);
  }
}
