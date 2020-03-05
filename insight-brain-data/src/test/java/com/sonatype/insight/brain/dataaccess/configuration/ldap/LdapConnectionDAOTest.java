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

import static org.assertj.core.api.Assertions.assertThat;

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
    char[] systemPassword = "systemPassword".toCharArray();
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
    assertThat(conn.getId()).isNull(); // sanity check
    dao.insert(conn);

    // select by id

    LdapConnection echo = dao.getById(conn.getId());
    assertThat(echo).isNotNull();
    assertThat(echo.getProtocol()).isEqualTo(protocol);
    assertThat(echo.getHostname()).isEqualTo(hostname);
    assertThat(echo.getPort()).isEqualTo(port);
    assertThat(echo.getSearchBase()).isEqualTo(searchBase);
    assertThat(echo.getAuthenticationMethod()).isEqualTo(authenticationMethod);
    assertThat(echo.getSaslRealm()).isEqualTo(saslRealm);
    assertThat(echo.getSystemUsername()).isEqualTo(systemUsername);
    assertThat(echo.getSystemPassword()).isEqualTo(systemPassword);
    assertThat(echo.getConnectionTimeout()).isEqualTo(connectionTimeout);
    assertThat(echo.getRetryDelay()).isEqualTo(retryDelay);

    // update

    char[] changedPassword = "changed_password".toCharArray();
    conn.setSystemPassword(changedPassword);
    dao.update(conn);
    echo = dao.getById(conn.getId());
    assertThat(echo.getSystemPassword()).isEqualTo(changedPassword);

    // delete
    dao.delete(conn);
    assertThat(dao.getById(conn.getId())).isNull();
  }

  @Test
  public void testHighPortNumbers() {
    LdapConnection conn = createLdapConnection();
    conn.setPort(65535);
    dao.insert(conn);
    assertThat(conn.getId()).isNotNull();
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
