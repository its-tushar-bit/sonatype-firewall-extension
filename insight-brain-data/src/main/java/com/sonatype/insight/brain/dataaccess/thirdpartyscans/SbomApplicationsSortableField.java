/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

public enum SbomApplicationsSortableField
{
  IMPORT_DATE("import_date"),
  APPLICATION_NAME("application_name"),
  LATEST_SBOM_VERSION("latest_sbom_version"),
  PERCENTAGE_ANNOTATED("percentage_annotated"),

  RELEASE_STATUS_PERCENTAGE("release_status_percentage"),
  VULNERABILITY("vulnerability");

  private final String column;

  SbomApplicationsSortableField(final String column) {
    this.column = column;
  }

  public String getColumn() {
    return column;
  }
}
