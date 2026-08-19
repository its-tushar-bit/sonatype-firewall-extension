/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

public enum SbomComponentSortableField
{
  TYPE("dependency_type"),
  PERCENTAGE_ANNOTATED("percentage_annotated"),
  RELEASE_STATUS_PERCENTAGE("release_status_percentage"),
  VULNERABILITIES("vulnerabilities"),
  DISPLAY_NAME("display_name");

  private final String column;

  SbomComponentSortableField(final String column) {
    this.column = column;
  }

  public String getColumn() {
    return column;
  }
}
