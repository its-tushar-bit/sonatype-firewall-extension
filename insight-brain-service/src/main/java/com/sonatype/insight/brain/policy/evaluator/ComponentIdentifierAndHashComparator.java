/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Comparator;

import com.sonatype.insight.brain.model.policy.ComponentIdentifierAndHashComparable;

public class ComponentIdentifierAndHashComparator
    implements Comparator<ComponentIdentifierAndHashComparable>
{
  public static final Comparator<ComponentIdentifierAndHashComparable> COMPARATOR =
      new ComponentIdentifierAndHashComparator();

  @Override
  public int compare(ComponentIdentifierAndHashComparable o1, ComponentIdentifierAndHashComparable o2) {
    // Hash
    int result = compareNullableStrings(o1.getHash(), o2.getHash());
    if (result != 0) {
      return result;
    }

    // Component identifier
    result = nullCheck(o1.getComponentIdentifier(), o2.getComponentIdentifier());
    if (result != 0) {
      return result;
    }
    if (o1.getComponentIdentifier() != null) {
      result = o1.getComponentIdentifier().compareTo(o2.getComponentIdentifier());
    }
    return result;
  }

  // null is greater than not null
  private int compareNullableStrings(String s1, String s2) {
    int result = nullCheck(s1, s2);
    if (result != 0) {
      return result;
    }
    if (s1 == null) {
      return 0;
    }
    return s1.compareTo(s2);
  }

  /**
   * Null objects are treated as infinitely large.
   */
  private int nullCheck(Object o1, Object o2) {
    if (o1 == null && o2 != null) {
      return 1;
    }
    else if (o1 != null && o2 == null) {
      return -1;
    }

    return 0;
  }
}
