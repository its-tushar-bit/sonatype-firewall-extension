/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

public enum ItemType
{
  ORGANIZATION,
  APPLICATION,
  NON_VULNERABLE_COMPONENT,
  SECURITY_VULNERABILITY,
  APPLICATION_CATEGORY,
  COMPONENT_LABEL,
  POLICY,
  SBOM_METADATA,
  POLICY_VIOLATION,
  LEGAL_VIOLATION,
  POLICY_WAIVER;

  /**
   * The name to use when constructing a search Term for this item type. For whatever reason it must be lowercase
   */
  public String searchFieldName() {
    return name().toLowerCase();
  }
}
