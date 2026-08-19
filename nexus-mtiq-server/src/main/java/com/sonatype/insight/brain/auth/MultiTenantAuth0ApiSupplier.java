/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.auth;

import jakarta.inject.Named;

import com.auth0.client.auth.Auth0AuthAPI;
import com.auth0.client.mgmt.Auth0ManagementAPI;

@Named
public class MultiTenantAuth0ApiSupplier
{
  public Auth0AuthAPI getAuthApi(final String authDomain, final String clientId, final String clientSecret) {
    return new Auth0AuthAPI(authDomain, clientId, clientSecret);
  }

  public Auth0ManagementAPI getManagementApi(final String domain, final String accessToken) {
    return new Auth0ManagementAPI(domain, accessToken);
  }
}
