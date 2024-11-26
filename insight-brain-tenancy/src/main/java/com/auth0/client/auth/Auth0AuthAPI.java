/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.auth0.client.auth;

import com.auth0.exception.Auth0Exception;
import com.auth0.net.VoidRequest;
import okhttp3.OkHttpClient;

public class Auth0AuthAPI
    extends AuthAPI
{
  public Auth0AuthAPI(final String domain, final String clientId, final String clientSecret) {
    super(domain, clientId, clientSecret);
  }

  public Void resetPassword(String email, String connection, String clientId, String organizationId) {
    String url =
        getBaseUrl().newBuilder().addPathSegment("dbconnections").addPathSegment("change_password").build().toString();
    VoidRequest request = new VoidRequest(getClient(), url, "POST");
    request.addParameter("client_id", clientId);
    request.addParameter("email", email);
    request.addParameter("connection", connection);
    request.addParameter("organization", organizationId);
    try {
      return request.execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public OkHttpClient getClient() {
    return super.getClient();
  }
}
