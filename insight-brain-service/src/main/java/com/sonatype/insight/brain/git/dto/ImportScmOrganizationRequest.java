/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.dto;

public class ImportScmOrganizationRequest
{
  public String scmHostUrl;

  public int desiredSubOrganizationCount = 0;

  /**
   * limit the total number of repository imports for this request.
   * <ul>
   * <li>limit < 0 - unlimited (i.e. import all repositories)</li>
   * <li>limit = 0 - no repositories to import (invalid request)</li>
   * </ul>
   */
  public int importLimit = -1;

  public ImportScmOrganizationRequest() {
    // no-op
  }

  public ImportScmOrganizationRequest(
      final String scmHostUrl,
      final Integer importLimit,
      final Integer desiredSubOrganizationCount)
  {

    this.scmHostUrl = scmHostUrl;
    this.importLimit = importLimit;
    this.desiredSubOrganizationCount = desiredSubOrganizationCount;
  }
}
