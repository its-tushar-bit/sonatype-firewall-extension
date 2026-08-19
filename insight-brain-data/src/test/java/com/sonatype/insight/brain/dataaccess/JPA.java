/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Comparator;
import java.util.Date;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

public class JPA
{
  /**
   * Fields to ignore during recursive comparison. These are internal implementation fields that should not be compared.
   */
  public static final String[] IGNORE_FIELDS = {};

  /**
   * AssertJ config for recursive field-by-field asserts to be used for entities that have java.util.Date fields.
   * java.util.Date fields may have different internal representations, so we compare by epoch value.
   */
  public static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIG =
      RecursiveComparisonConfiguration.builder()
          .withComparatorForType(Comparator.comparing(Date::getTime), Date.class)
          .build();

  public static <T> void assertEntityEquals(T actual, T expected) {
    assertThat(actual).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG).isEqualTo(expected);
  }

  public static <T> void assertContainsEntitiesExactlyElementsOf(Iterable<T> actual, Iterable<T> expected) {
    assertThat(actual).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyElementsOf(expected);
  }

  @SafeVarargs
  public static <T> void assertContainsEntitiesExactlyInAnyOrder(Iterable<T> actual, T... expected) {
    assertThat(actual).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(expected);
  }
}
