/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.util.Set;

public class SSOEmbedUrlRequest
{
  public String requestId;

  public String usernameAndRealm;

  public String userFirstName;

  public String userLastName;

  public String dashboardKey;

  public Set<String> userPermissions;

  public Set<String> applicationIds;

  public String embedDomain;

  public SSOEmbedUrlRequest() {
    // for jackson
  }

  public SSOEmbedUrlRequest(
      String requestId,
      String usernameAndRealm,
      String userFirstName,
      String userLastName,
      String dashboardKey,
      Set<String> userPermissions,
      Set<String> applicationIds,
      String embedDomain)
  {
    this.requestId = requestId;
    this.usernameAndRealm = usernameAndRealm;
    this.userFirstName = userFirstName;
    this.userLastName = userLastName;
    this.dashboardKey = dashboardKey;
    this.userPermissions = userPermissions;
    this.applicationIds = applicationIds;
    this.embedDomain = embedDomain;
  }
}
