/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.apache.shiro.codec.Base64;
import org.junit.After;
import org.junit.Test;

public class LoginResourceTest
    extends AbstractResourceTest
{
  @After
  public void logout() throws Exception {
    RestAccess.get(getRestBaseUrl() + LoginResource.SERVICE_PATH + "/logout");
  }

  private Response login() throws Exception {
    return login(null, null);
  }

  private Response login(String username, String password) throws Exception {
    Map<String, String> headers = new HashMap<String, String>();
    if (username != null) {
      String encoded = Base64.encodeToString(("Basic " + username + ":" + password).getBytes("UTF-8"));
      headers.put("Authorization", encoded);
    }

    return RestAccess.get(getRestBaseUrl() + LoginResource.SERVICE_PATH + "/login", headers);
  }

  @Test
  public void testLogin() throws Exception {
    // now run the test with bad username
    Response response = login("admin2", "admin");
    assertResponseStatus(401, response);

    // now run the test with bad password
    /**
     * TODO: currently no way to have invalid password, CLMRealm isn't complete
    response = login("admin", "admin2");
    assertResponseStatus(401, response);
    */

    // now run the test with no header, validate failure
    response = login();
    assertResponseStatus(400, response);

    // now run with valid data
    response = login("admin", "admin");
    assertResponseStatus(200, response);
  }
}