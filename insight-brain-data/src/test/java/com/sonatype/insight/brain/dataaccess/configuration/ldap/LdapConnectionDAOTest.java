/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LdapConnectionDAOTest
    extends AbstractDbDAOTest
{
  private LdapConnectionDAO dao = new LdapConnectionDAO();

  private LdapServer server;

  @Before
  public void createTestServer() {
    server = tempEntity.newLdapServer("testServer");
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
    assertNull(conn.getId()); // sanity check
    dao.insert(conn);

    // select by id

    LdapConnection echo = dao.getById(conn.getId());
    assertNotNull(echo);
    assertEquals(protocol, echo.getProtocol());
    assertEquals(hostname, echo.getHostname());
    assertEquals(port, echo.getPort());
    assertEquals(searchBase, echo.getSearchBase());
    assertEquals(authenticationMethod, echo.getAuthenticationMethod());
    assertEquals(saslRealm, echo.getSaslRealm());
    assertEquals(systemUsername, echo.getSystemUsername());
    assertEquals(systemPassword, echo.getSystemPassword());
    assertEquals(connectionTimeout, echo.getConnectionTimeout());
    assertEquals(retryDelay, echo.getRetryDelay());

    // update

    String changedPassword = "changed_password";
    conn.setSystemPassword(changedPassword);
    dao.update(conn);
    echo = dao.getById(conn.getId());
    assertEquals(changedPassword, echo.getSystemPassword());

    // delete
    dao.delete(conn);
    assertNull(dao.getById(conn.getId()));
  }

  @Test
  public void testHighPortNumbers() {
    LdapConnection conn = createLdapConnection();
    conn.setPort(65535);
    dao.insert(conn);
    assertNotNull(conn.getId());
  }

  private LdapConnection createLdapConnection() {
    LdapConnection conn = new LdapConnection();
    conn.setServerId(server.getId());
    conn.setHostname("localhost");
    conn.setPort(389);
    conn.setProtocol(LdapProtocol.LDAP);
    conn.setAuthenticationMethod(LdapAuthenticationMethod.NONE);
    return conn;
  }
}
