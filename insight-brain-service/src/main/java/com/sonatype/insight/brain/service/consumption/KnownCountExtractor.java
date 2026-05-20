/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.lang.reflect.Array;
import java.util.Collection;

import jakarta.annotation.Nullable;

/**
 * Extracts the count of known components from HDS response objects.
 * Different response types encode their results differently.
 *
 * @since 1.204
 */
public final class KnownCountExtractor
{
  private KnownCountExtractor() {
  }

  /**
   * Extract the component count from an HDS response.
   *
   * @param response the deserialized HDS response object
   * @return the number of components, or 0 if response is null
   */
  public static int extractCount(@Nullable Object response) {
    if (response == null) {
      return 0;
    }
    if (response.getClass().isArray()) {
      return Array.getLength(response);
    }
    if (response instanceof Collection<?> collection) {
      return collection.size();
    }
    // For single-object responses (e.g. individual component lookup), count as 1
    return 1;
  }
}
