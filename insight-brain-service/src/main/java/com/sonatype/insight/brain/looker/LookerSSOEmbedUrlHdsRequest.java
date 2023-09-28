/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.looker;

import java.util.Set;

public class LookerSSOEmbedUrlHdsRequest
{
  public String requestId;

  public String userFirstName;

  public String userLastName;

  public String dashboardKey;

  public Set<String> userPermissions;

  public LookerSSOEmbedUrlHdsRequest() {
    //for jackson
  }

  public LookerSSOEmbedUrlHdsRequest(
      String requestId,
      String userFirstName,
      String userLastName,
      String dashboardKey,
      Set<String> userPermissions)
  {
    this.requestId = requestId;
    this.userFirstName = userFirstName;
    this.userLastName = userLastName;
    this.dashboardKey = dashboardKey;
    this.userPermissions = userPermissions;
  }
}
