/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.CaseFormat;

public enum ProductMode
{
  // This enum intentionally only has one value at the moment as null is used for non SBOM manager products
  SBOM_MANAGER;

  @Override
  @JsonValue
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
  }

  public static ProductMode fromString(final String name) {
    if (name == null) {
      return null;
    }

    return valueOf(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name));
  }
}
