/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.ReverseProxyAuthenticationConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ReverseProxyAuthcTest
    extends AbstractBrainServiceTest
{
  @Rule
  public TestLdapServer ldapServer = new TestLdapServer();

  private final Configurator ENABLED = new Configurator()
  {
    @Override
    public void configure(InsightConfig config) {
      config.getReverseProxyAuthentication().setEnabled(true);
    }
  };

  @Override
  public void initTest() throws Exception {
    ldapServer.start();
    ldapServer.loadData("/ReverseProxyAuthcTest/ldap_users.ldif");
    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), this.ldapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // defer brain server initialization to actual test method
  }

  @Test
  public void testDisabledByDefault() throws Exception {
    initServer(null);

    ReverseProxyAuthenticationConfig reverseProxyAuthcConfig = new ReverseProxyAuthenticationConfig();
    assertThat(reverseProxyAuthcConfig.isEnabled(), is(false));
    assertThat(reverseProxyAuthcConfig.getUsernameHeader(), is("REMOTE_USER"));
    HttpResponse response = restRequest().path("rest/anything").header("REMOTE_USER", "testuser").anon().get();
    assertResponseStatus(401, response);
  }

  @Test
  public void testEnabled_DefaultHeader() throws Exception {
    initServer(ENABLED);

    HttpRequest request = restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.SERVICE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie(), is(notNullValue()));
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated(), is(true));
    assertThat(authStatus.isClmUser(), is(false));
    assertThat(authStatus.getDisplayName(), is("John Doe"));

    response = request.subpath(PublicApiPaths.ORG_SERVICE_PATH).get();
    assertResponseStatus(401, response);
  }

  @Test
  public void testEnabled_CustomHeader() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.getReverseProxyAuthentication().setEnabled(true);
        config.getReverseProxyAuthentication().setUsernameHeader("X-Remote-User");
      }
    });

    HttpRequest request = restRequest().header("X-Remote-User", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.SERVICE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie(), is(notNullValue()));
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated(), is(true));
    assertThat(authStatus.isClmUser(), is(false));
    assertThat(authStatus.getDisplayName(), is("John Doe"));
  }

  @Test
  public void testEnabled_SessionCreatedForAnyRequest() throws Exception {
    initServer(ENABLED);

    HttpRequest request = restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath("rest/anything").get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(notNullValue()));
  }

  @Test
  public void testEnabled_UserMustExistInLdap() throws Exception {
    initServer(ENABLED);

    HttpRequest request = restRequest().header("REMOTE_USER", "unknown-user").anon();
    HttpResponse response = request.subpath("rest/anything").get();
    assertResponseStatus(401, response);
  }
}
