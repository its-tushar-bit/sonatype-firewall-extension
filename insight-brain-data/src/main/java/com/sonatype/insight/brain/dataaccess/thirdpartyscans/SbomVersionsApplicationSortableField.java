/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

public enum SbomVersionsApplicationSortableField
{
  IMPORT_DATE("import_date"),
  RELEASE_STATUS("percentage_annotated"),
  VULNERABILITY("vulnerability");

  private final String column;

  SbomVersionsApplicationSortableField(final String column) {
    this.column = column;
  }

  public String getColumn() {
    return column;
  }

}
