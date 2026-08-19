/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class IndexSessionCursors
{
  private static final String BACKEND_MARKER = "$backendId";

  private IndexSessionCursors() {
  }

  public static List<Object> encode(final String backendId, final List<?> backendCursor) {
    Objects.requireNonNull(backendId, "backendId");
    if (backendCursor == null || backendCursor.isEmpty()) {
      return List.of();
    }
    return List.of(BACKEND_MARKER, backendId, List.copyOf(backendCursor));
  }

  public static List<Object> decode(final String expectedBackendId, final List<Object> cursor) {
    if (cursor == null || cursor.isEmpty()) {
      return List.of();
    }
    if (cursor.size() != 3 || !BACKEND_MARKER.equals(cursor.get(0))) {
      throw new IllegalArgumentException("searchAfter cursor is not bound to backend " + expectedBackendId);
    }
    String actualBackendId = String.valueOf(cursor.get(1));
    if (!expectedBackendId.equals(actualBackendId)) {
      throw new IllegalArgumentException(
          "searchAfter cursor for backend " + actualBackendId + " cannot be used with backend " + expectedBackendId);
    }
    Object backendCursor = cursor.get(2);
    if (backendCursor instanceof List<?> list) {
      return List.copyOf(list);
    }
    if (backendCursor instanceof Object[] array) {
      return Arrays.asList(array);
    }
    throw new IllegalArgumentException("searchAfter cursor payload must be a list or array");
  }
}
