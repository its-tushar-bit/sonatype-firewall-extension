/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.securesharing;

import java.util.Date;

import com.sonatype.insight.json.store.ApiDateFormat;

public class ApiSecureSharingSbomDTO
{
  public String id;

  public String sbomVersion;

  @ApiDateFormat
  public Date created;

  public ApiSecureSharingSbomDTO() {
  }

  public ApiSecureSharingSbomDTO(final String id, final String sbomVersion, final Date created) {
    this.id = id;
    this.sbomVersion = sbomVersion;
    this.created = created;
  }
}
