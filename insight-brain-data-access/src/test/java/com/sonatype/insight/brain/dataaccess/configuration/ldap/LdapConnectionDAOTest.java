/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.LinkedHashSet;
import java.util.Set;

import com.sonatype.insight.brain.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LdapConnectionDAOTest
    extends AbstractDbDAOTest
{
  private LdapServerDAO serverDao = new LdapServerDAO();

  private LdapConnectionDAO connDao = new LdapConnectionDAO();

  protected Set<LdapConnection> connsToDelete = new LinkedHashSet<LdapConnection>();

  private LdapServer server;

  @Before
  public void createTestServer() {
    server = new LdapServer();
    server.setName("testServer");
    serverDao.insert(server);
  }

  @Test
  public void testCRUD() {
    LdapProtocol protocol = LdapProtocol.LDAPS;
    String hostname = "hostname";
    int port = 389;
    String searchBase = "searchBase";
    LdapAuthenticationMethod authenticationMethod = LdapAuthenticationMethod.DIGESTMD5;
    String saslRealm = "saslRealm";
    String systemUsername = "systemUsername";
    String systemPassword = "systemPassword";
    int connectionTimeout = 123;
    int retryDelay = 345;

    // insert

    LdapConnection conn = new LdapConnection();
    conn.setServerId(server.getId());
    conn.setProtocol(protocol);
    conn.setHostname(hostname);
    conn.setPort(port);
    conn.setSearchBase(searchBase);
    conn.setAuthenticationMethod(authenticationMethod);
    conn.setSaslRealm(saslRealm);
    conn.setSystemUsername(systemUsername);
    conn.setSystemPassword(systemPassword);
    conn.setConnectionTimeout(connectionTimeout);
    conn.setRetryDelay(retryDelay);
    Assert.assertNull(conn.getId()); // sanity check
    connDao.insert(conn);

    // select by id

    LdapConnection echo = connDao.getById(conn.getId());
    Assert.assertNotNull(echo);
    Assert.assertEquals(protocol, echo.getProtocol());
    Assert.assertEquals(hostname, echo.getHostname());
    Assert.assertEquals(port, echo.getPort());
    Assert.assertEquals(searchBase, echo.getSearchBase());
    Assert.assertEquals(authenticationMethod, echo.getAuthenticationMethod());
    Assert.assertEquals(saslRealm, echo.getSaslRealm());
    Assert.assertEquals(systemUsername, echo.getSystemUsername());
    Assert.assertEquals(systemPassword, echo.getSystemPassword());
    Assert.assertEquals(connectionTimeout, echo.getConnectionTimeout());
    Assert.assertEquals(retryDelay, echo.getRetryDelay());

    // update

    String changedPassword = "changed_password";
    conn.setSystemPassword(changedPassword);
    connDao.update(conn);
    echo = connDao.getById(conn.getId());
    Assert.assertEquals(changedPassword, echo.getSystemPassword());

    // delete
    connDao.delete(conn);
    Assert.assertNull(connDao.getById(conn.getId()));
  }

  @Test
  public void testHighPortNumbers() {
    LdapConnection conn = createLdapConnection();
    conn.setPort(65535);
    connDao.insert(conn);
    Assert.assertNotNull(conn.getId());
  }

  protected LdapConnection createLdapConnection() {
    LdapConnection conn = new LdapConnection();
    conn.setServerId(server.getId());
    conn.setHostname("localhost");
    conn.setPort(389);
    conn.setProtocol(LdapProtocol.LDAP);
    conn.setAuthenticationMethod(LdapAuthenticationMethod.NONE);
    return conn;
  }

  protected LdapConnection insertLdapConnection() {
    LdapConnection conn = createLdapConnection();
    connDao.insert(conn);
    connsToDelete.add(conn);
    return conn;
  }

  @After
  @Override
  public void tearDown() {
    for (LdapConnection conn : connsToDelete) {
      conn = connDao.getById(conn.getId());
      if (conn != null) {
        connDao.delete(conn);
      }
    }
    serverDao.delete(server);
    super.tearDown();
  }
}
