/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.LoginResource.AccountStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Cookie;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.apache.shiro.codec.Base64;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LoginResourceTest
    extends AbstractResourceTest
{
  public LoginResourceTest() {
    super(false);
  }
  
  private Response logout(Cookie cookie) throws Exception {
    return RestAccess.post(getRestBaseUrl() + LoginResource.SERVICE_PATH + "/logout", cookie);
  }

  private Response login() throws Exception {
    return login(null, null);
  }

  private Response login(String username, String password) throws Exception {
    Map<String, String> headers = new HashMap<String, String>();
    if (username != null) {
      headers.put("Authorization", "Basic " + Base64.encodeToString((username + ":" + password).getBytes("UTF-8")));
    }

    return RestAccess.post(getRestBaseUrl() + LoginResource.SERVICE_PATH + "/login", "", headers);
  }

  private Response status(Cookie cookie) throws Exception {
    return RestAccess.get(getRestBaseUrl() + LoginResource.SERVICE_PATH + "/status", cookie);
  }

  @Test
  public void testLogin() throws Exception {
    // now run the test with bad username
    Response response = login("admin2", "admin");
    assertResponseStatus(401, response);
    assertEquals("", response.getResponseBody());

    // now run the test with bad password
    response = login(User.ADMIN_USERNAME, "wrong password");
    assertResponseStatus(401, response);
    assertEquals("", response.getResponseBody());

    // now run the test with no header, validate failure
    response = login();
    assertResponseStatus(401, response);
    assertEquals("", response.getResponseBody());

    // now run with valid data
    response = login(User.ADMIN_USERNAME, "admin123");
    assertResponseStatus(204, response);

    // validate cookie is present
    Cookie loggedInSessionCookie = extractSessionCookie(response);
    Assert.assertFalse(loggedInSessionCookie.getValue().equals("deleteMe"));

    // logout is successful
    response = logout(response.getCookies().get(0));
    assertResponseStatus(204, response);
    
    // logout removes session id
    Cookie loggOutSessionCookie = extractSessionCookie(response);
    Assert.assertTrue(loggOutSessionCookie.getValue().equals("deleteMe"));
  }
  
  @Test
  public void testStatus() throws Exception {
    // logged out by default, so 200 expected with no username
    Response response = status(null);
    assertResponseStatus(200, response);
    AccountStatus status = JsonHelpers.fromJson(response.getResponseBody(), AccountStatus.class);
    Assert.assertNull(status.getAccount());

    response = login(User.ADMIN_USERNAME, "admin123");
    assertResponseStatus(204, response);

    Cookie jsessionIdCookie = extractSessionCookie(response);

    response = status(jsessionIdCookie);
    assertResponseStatus(200, response);
    status = JsonHelpers.fromJson(response.getResponseBody(), AccountStatus.class);
    Assert.assertEquals(User.ADMIN_USERNAME, status.getAccount());

    response = logout(jsessionIdCookie);
    assertResponseStatus(204, response);

    // this cookie should no longer be valid
    response = status(jsessionIdCookie);
    assertResponseStatus(200, response);
    status = JsonHelpers.fromJson(response.getResponseBody(), AccountStatus.class);
    Assert.assertNull(status.getAccount());
  }

  private Cookie extractSessionCookie(Response response) {
    for (Cookie cookie : response.getCookies()) {
      if( "JSESSIONID".equals(cookie.getName())) {
        return cookie;
      }
    }
    
    throw new IllegalStateException("Missing session cookie");
  }
}