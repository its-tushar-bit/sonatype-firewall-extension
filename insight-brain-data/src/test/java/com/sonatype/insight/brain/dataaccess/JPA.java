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
  public static final String[] IGNORE_FIELDS = {"pcStateManager", "pcDetachedState", "field", "sm"};

  /**
   * AssertJ config for recursive field-by-field asserts to be used for JPA entities that have java.util.Date fields.
   * OpenJPA 4.x changed the way java.util.Date fields are stored in memory and, unfortunately, the equals() method
   * doesn't work anymore. So we need to tell AssertJ to compare the epoch values for java.util.Date fields.
   */
  public static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIG =
      RecursiveComparisonConfiguration.builder()
          .withComparatorForType(Comparator.comparing(Date::getTime), Date.class)
          .withIgnoredFieldsMatchingRegexes(JPA.IGNORE_FIELDS)
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
