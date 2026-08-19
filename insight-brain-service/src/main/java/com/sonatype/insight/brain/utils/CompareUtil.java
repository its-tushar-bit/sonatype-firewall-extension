/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

/**
 * Utility class for comparing objects.
 */
public class CompareUtil
{
  /**
   * Compares two objects for null. If the first object is null and the second object is not null, 1 is returned. If the
   * second object is null and the first object is not null, -1 is returned. If both are null or both are not null, 0 is
   * returned, meaning that based on null comparison, the two objects are equal.
   *
   * @param object1 the first object
   * @param object2 the second object
   * @return 1 if the first object is null and the second object is not null, -1 if the second object is null and the
   *         first object is not null, 0 otherwise.
   */
  public static int compareObjectsByNull(final Object object1, final Object object2) {
    if (object1 == null && object2 != null) {
      return 1;
    }

    if (object2 == null && object1 != null) {
      return -1;
    }

    return 0;
  }

  /**
   * Compares two objects. If the objects are not null and implement the {@link Comparable} interface, the objects are
   * compared using the {@link Comparable#compareTo(Object)} method. If the objects are not null and do not implement
   * the {@link Comparable} interface, 0 is returned, meaning that the two objects are equal. If one of the objects is
   * null, the objects are compared by null using the {@link #compareObjectsByNull(Object, Object)} method.
   *
   * @param object1 the first object
   * @param object2 the second object
   * @return the result of the comparison
   * @param <T> the type of the objects
   */
  @SuppressWarnings("unchecked")
  public static <T> int compareTo(final T object1, final T object2) {
    int result = compareObjectsByNull(object1, object2);

    if (result != 0) {
      return result;
    }

    if (object1 instanceof Comparable && object2 instanceof Comparable) {
      return ((Comparable<T>) object1).compareTo(object2);
    }

    return result;
  }
}
