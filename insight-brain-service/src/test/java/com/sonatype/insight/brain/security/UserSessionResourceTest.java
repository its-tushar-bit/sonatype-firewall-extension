/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.HttpCookie;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserSessionResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserSessionResource.SERVICE_PATH);
  }

  private HttpResponse logout(HttpCookie cookie) throws Exception {
    return restRequest().path(UserSessionResource.LOGOUT_PATH).cookie(cookie).anon().delete();
  }

  private HttpResponse login() throws Exception {
    return login(null, null);
  }

  private HttpResponse login(String username, String password) throws Exception {
    return restRequest().auth(username, password).post();
  }

  private HttpResponse status(HttpCookie cookie) throws Exception {
    return restRequest().cookie(cookie).anon().get();
  }

  @Test
  public void testSessionManagement() throws Exception {

    // uninstall license and should find all these tests run uninhibited as they are unlicensed paths
    getTestProductLicenseManager().uninstallLicense();

    // now run the test with bad username
    HttpResponse response = login("admin2", "admin");
    assertResponseStatus(401, response);
    assertEquals(response.getHeader("WWW-Authenticate"), "nonBrowserPromptingBasic realm=\"application\"");
    assertEquals("", response.getResponseBody());

    // now run the test with bad password
    response = login(User.ADMIN_USERNAME, "wrong password");
    assertResponseStatus(401, response);
    assertEquals(response.getHeader("WWW-Authenticate"), "nonBrowserPromptingBasic realm=\"application\"");
    assertEquals("", response.getResponseBody());

    // now run the test with no header, validate failure
    response = login();
    assertResponseStatus(401, response);
    assertEquals(response.getHeader("WWW-Authenticate"), "nonBrowserPromptingBasic realm=\"application\"");
    assertEquals("", response.getResponseBody());

    // now run with valid data
    response = login(User.ADMIN_USERNAME, "admin123");
    assertResponseStatus(204, response);

    // validate cookie is present
    HttpCookie loggedInSessionCookie = extractSessionCookie(response);
    Assert.assertFalse(loggedInSessionCookie.getValue().equals("deleteMe"));

    // logout is successful
    response = logout(getSessionCookie(response));
    assertResponseStatus(204, response);

    // logout removes session id
    HttpCookie logoutSessionCookie = extractSessionCookie(response);
    Assert.assertTrue(logoutSessionCookie.getValue().equals("deleteMe"));
  }

  @Test
  public void testStatus() throws Exception {

    // uninstall license and should find all these tests run uninhibited as they are unlicensed paths
    getTestProductLicenseManager().uninstallLicense();

    // logged out by default, so 401 expected
    HttpResponse response = status(null);
    assertResponseStatus(401, response);

    response = login(User.ADMIN_USERNAME, "admin123");
    assertResponseStatus(204, response);

    HttpCookie jsessionIdCookie = extractSessionCookie(response);

    response = status(jsessionIdCookie);
    assertResponseStatus(200, response);
    AuthenticationStatus status = fromJson(response, AuthenticationStatus.class);
    Assert.assertTrue(status.isAuthenticated());
    Assert.assertEquals(User.ADMIN_USERNAME, status.getUsername());

    response = logout(jsessionIdCookie);
    assertResponseStatus(204, response);

    // this cookie should no longer be valid
    response = status(jsessionIdCookie);
    assertResponseStatus(401, response);
  }

  @Test
  public void testLogoutNoAuth() throws Exception {
    // no cookie, no auth
    HttpResponse response = logout(null);
    assertResponseStatus(204, response);
  }
}
