/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.auth;

import java.util.Date;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.service.Auth0Config;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.auth0.client.auth.Auth0AuthAPI;
import com.auth0.client.mgmt.Auth0ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class MultiTenantAuth0ManagementService
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantAuth0ManagementService.class);

  private final MultiTenantAuth0ApiSupplier auth0ApiSupplier;

  private final Auth0Config auth0Config;

  private Auth0AuthAPI authAPI;

  private TokenHolder apiToken;

  private Auth0ManagementAPI auth0ManagementAPI;

  @Inject
  public MultiTenantAuth0ManagementService(final MultiTenantInsightConfig multiTenantInsightConfig,
                                           final MultiTenantAuth0ApiSupplier auth0ApiSupplier)
  {
    this.auth0Config = multiTenantInsightConfig.getAuth0Config();
    this.auth0ApiSupplier = auth0ApiSupplier;
  }

  public void createOrUpdateUser(final String email,
                                 final String firstName,
                                 final String lastName,
                                 final String connectionName,
                                 final String applicationId)
  {
    if (!isApiTokenValid()) {
      this.apiToken = requestApiToken();
      if (this.apiToken != null) {
        auth0ManagementAPI =
            auth0ApiSupplier.getManagementApi(auth0Config.getDomain(), this.apiToken.getAccessToken());
      }
    }

    if (auth0ManagementAPI == null) {
      throw new RuntimeException("Unable to initialise Auth0 Management API");
    }

    boolean userExists;
    try {
      userExists = auth0ManagementAPI.userExists(email, connectionName);
    }
    catch (Auth0Exception e) {
      log.warn("Unable to determine if user already exists in Auth0");
      throw new RuntimeException(e);
    }

    if (!userExists) {
      auth0ManagementAPI.createOrGetUser(email, firstName, lastName, connectionName);
      sendResetPassword(email, connectionName, applicationId);
    }
  }

  private void sendResetPassword(final String email, final String connectionName, final String applicationId) {
    try {
      getAuthApiLazily(auth0Config).resetPassword(email, connectionName, applicationId);
    }
    catch (RuntimeException e) {
      log.warn("Unable to send reset password");
      auth0ManagementAPI.deleteUserByEmail(email, connectionName);
      throw new RuntimeException(e);
    }
  }

  private Auth0AuthAPI getAuthApiLazily(final Auth0Config auth0Config) {
    if (this.authAPI == null) {
      this.authAPI = auth0ApiSupplier
          .getAuthApi(auth0Config.getDomain(), auth0Config.getClientId(), auth0Config.getClientSecret());
    }
    return authAPI;
  }

  private TokenHolder requestApiToken() {
    try {
      return getAuthApiLazily(auth0Config).requestToken(String.format("%sapi/v2/", auth0Config.getDomain())).execute();
    }
    catch (Auth0Exception exception) {
      log.error("Error getting Auth0 API token: {}", exception.getMessage());
    }
    return null;
  }

  private boolean isApiTokenValid() {
    return this.apiToken != null && new Date().before(this.apiToken.getExpiresAt());
  }
}
