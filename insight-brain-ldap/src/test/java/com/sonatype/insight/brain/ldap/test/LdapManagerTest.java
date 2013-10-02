/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap.test;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.naming.NamingException;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.ldap.EmbeddedLdapServer;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;

import org.sonatype.guice.bean.containers.InjectedTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static com.sonatype.insight.brain.ldap.EmbeddedLdapServer.newEmbeddedLdapServer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

/**
 * @since 1.7
 */
public class LdapManagerTest
    extends InjectedTest
{
  private final LdapServerDAO serverDao = new LdapServerDAO();

  private EmbeddedLdapServer ldapServer;

  private LdapServer serverDetails;

  @Inject
  private LdapManager manager;

  @Test
  public void testConnection() throws Exception {
    startLdapServer();

    LdapConnection conn = manager.loadConnection(serverDetails.getId());
    conn.setServerId(serverDetails.getId());
    conn.setProtocol(LdapProtocol.LDAP);
    conn.setHostname(ldapServer.getHostname());
    conn.setPort(ldapServer.getPort());

    manager.testConnection(conn);
  }

  @Test
  public void testBadSearchBase() throws Exception {
    startLdapServer();

    LdapConnection conn = manager.loadConnection(serverDetails.getId());
    conn.setServerId(serverDetails.getId());
    conn.setProtocol(LdapProtocol.LDAP);
    conn.setHostname(ldapServer.getHostname());
    conn.setPort(ldapServer.getPort());
    conn.setSearchBase("!@£$%^&*()");

    try {
      manager.testConnection(conn);
      fail("Expected NamingException");
    }
    catch (NamingException expected) {
    }
  }

  @Test
  public void testUserMapping() throws Exception {
    startLdapServer();

    LdapConnection conn = manager.loadConnection(serverDetails.getId());
    conn.setServerId(serverDetails.getId());
    conn.setProtocol(LdapProtocol.LDAP);
    conn.setHostname(ldapServer.getHostname());
    conn.setPort(ldapServer.getPort());
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMapping umap = new LdapUserMapping();
    umap.setServerId(serverDetails.getId());
    umap.setUserBaseDN("ou=users");
    umap.setUserObjectClass("person");
    umap.setUserIDAttribute("uid");
    umap.setUserRealNameAttribute("cn");
    umap.setUserEmailAttribute("mail");
    umap.setUserPasswordAttribute("userPassword");
    umap.setUserSubtree(true);

    List<LdapUser> users = manager.testUserMapping(umap, -1);
    assertThat(users, hasSize(2));

    Collections.sort(users); // sorts on username

    LdapUser user = users.get(0);
    assertThat(user.getUsername(), is("test_user"));
    assertThat(user.getDn(), is("uid=test_user,ou=users,dc=company,dc=com"));
    assertThat(user.getRealName(), is("Test User"));
    assertThat(user.getEmail(), is("test.user@company.com"));
    assertThat(user.getPassword(), nullValue()); // make sure password is not passed back

    user = users.get(1);
    assertThat(user.getUsername(), is("test_user2"));
    assertThat(user.getDn(), is("uid=test_user2,ou=users,dc=company,dc=com"));
    assertThat(user.getRealName(), is("Test User 2"));
    assertThat(user.getEmail(), is("test.user2@company.com"));
    assertThat(user.getPassword(), nullValue()); // make sure password is not passed back
  }

  @Test
  public void testUserLogin() throws Exception {
    startLdapServer();

    LdapConnection conn = manager.loadConnection(serverDetails.getId());
    conn.setServerId(serverDetails.getId());
    conn.setProtocol(LdapProtocol.LDAP);
    conn.setHostname(ldapServer.getHostname());
    conn.setPort(ldapServer.getPort());
    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMapping umap = new LdapUserMapping();
    umap.setServerId(serverDetails.getId());
    umap.setUserBaseDN("ou=users");
    umap.setUserObjectClass("person");
    umap.setUserIDAttribute("uid");
    umap.setUserRealNameAttribute("givenName");
    umap.setUserEmailAttribute("mail");
    umap.setUserSubtree(true);

    manager.testUserLogin(umap, "test_user", "far2simple".toCharArray());
  }

  public LdapManagerTest startLdapServer() throws Exception {
    serverDetails = new LdapServer();
    serverDetails.setName("Test Server");
    serverDao.insert(serverDetails);

    ldapServer = newEmbeddedLdapServer();
    ldapServer.start();

    return this;
  }

  @After
  public void stopLdapServer() throws Exception {
    if (ldapServer != null) {
      ldapServer.stop();
      ldapServer = null;
    }

    for (LdapServer s : serverDao.getAll()) {
      serverDao.delete(s);
    }
    Assert.assertEquals(0, serverDao.getAll().size());
  }
}
