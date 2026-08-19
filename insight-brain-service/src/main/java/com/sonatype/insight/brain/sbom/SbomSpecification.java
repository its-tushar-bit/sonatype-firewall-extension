/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import org.apache.commons.lang3.StringUtils;

import com.sonatype.insight.scan.model.ItemContentType;

public enum SbomSpecification
{
  CYCLONEDX("CycloneDx"),
  SPDX("SPDX");

  private final String specification;

  SbomSpecification(final String specification) {
    this.specification = specification;
  }

  public static SbomSpecification fromValue(String value) {
    if (StringUtils.equals(value, CYCLONEDX.specification)) {
      return CYCLONEDX;
    }

    if (StringUtils.equals(value, SPDX.specification)) {
      return SPDX;
    }
    return null;
  }

  public ItemContentType toItemContentType() {
    return this == CYCLONEDX ? ItemContentType.SBOM : ItemContentType.SPDX;
  }

  @Override
  public String toString() {
    return specification;
  }
}
