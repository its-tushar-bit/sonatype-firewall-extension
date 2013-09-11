/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.util.Map;

import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

/**
 * Helper class for making authenticated REST calls from tests.
 * <p>
 * The methods that do not have username and password parameters use admin credentials by default.
 * 
 * @since 1.7
 */
public class AuthedRestAccess
{
  private static final String ADMIN_USERNAME = User.ADMIN_USERNAME;

  private static final String ADMIN_PASSWORD = "admin123";

  public static Response get(String urlString) throws Exception {
    return RestAccess.get(urlString, ADMIN_USERNAME, ADMIN_PASSWORD);
  }

  public static Response get(String urlString, Map<String, String> headers) throws Exception {
    return RestAccess.get(urlString, null /* params */, headers, ADMIN_USERNAME, ADMIN_PASSWORD, null /* cookie */);
  }

  public static Response post(String urlString, String body) throws Exception {
    return RestAccess.post(urlString, ADMIN_USERNAME, ADMIN_PASSWORD, body);
  }

  public static Response post(String urlString, String username, String password) throws Exception {
    return RestAccess.post(urlString, username, password);
  }

  public static Response put(String urlString, String body) throws Exception {
    return RestAccess.put(urlString, ADMIN_USERNAME, ADMIN_PASSWORD, body);
  }

  public static Response delete(String urlString) throws Exception {
    return RestAccess.delete(urlString, ADMIN_USERNAME, ADMIN_PASSWORD);
  }

  public static Response execute(BoundRequestBuilder builder) throws Exception {
    RestAccess.addAuthorization(builder, ADMIN_USERNAME, ADMIN_PASSWORD);
    return builder.execute().get();
  }

  public static AsyncHttpClient getClient() {
    return RestAccess.getClient();
  }
}
