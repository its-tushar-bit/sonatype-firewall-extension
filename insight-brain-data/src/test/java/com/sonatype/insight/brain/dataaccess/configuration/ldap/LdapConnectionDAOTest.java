/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapConnectionDAOTest
    extends AbstractDbDAOTest
{
  private DAOSecretRotator daoSecretRotator;

  private LdapConnectionDAO dao;

  private LdapServer ldapServer;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createLdapConnectionDAO();
    daoSecretRotator = new DAOSecretRotator();
    ldapServer = tempEntity.newLdapServer("testServer");
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

    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServer.getId());
    ldapConnection.setProtocol(protocol);
    ldapConnection.setHostname(hostname);
    ldapConnection.setPort(port);
    ldapConnection.setSearchBase(searchBase);
    ldapConnection.setAuthenticationMethod(authenticationMethod);
    ldapConnection.setSaslRealm(saslRealm);
    ldapConnection.setSystemUsername(systemUsername);
    ldapConnection.setSystemPassword(systemPassword);
    ldapConnection.setConnectionTimeout(connectionTimeout);
    ldapConnection.setRetryDelay(retryDelay);
    assertThat(ldapConnection.getId()).isNull(); // sanity check
    dao.insert(ldapConnection);

    // select by id

    LdapConnection echo = dao.getById(ldapConnection.getId());
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
    ldapConnection.setSystemPassword(changedPassword);
    dao.update(ldapConnection);
    echo = dao.getById(ldapConnection.getId());
    assertThat(echo.getSystemPassword()).isEqualTo(changedPassword);

    // delete
    dao.delete(ldapConnection);
    assertThat(dao.getById(ldapConnection.getId())).isNull();
  }

  @Test
  public void testHighPortNumbers() {
    LdapConnection ldapConnection = createLdapConnection();
    ldapConnection.setPort(65535);
    dao.insert(ldapConnection);
    assertThat(ldapConnection.getId()).isNotNull();
  }

  private LdapConnection createLdapConnection() {
    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServer.getId());
    ldapConnection.setHostname("localhost");
    ldapConnection.setPort(389);
    ldapConnection.setProtocol(LdapProtocol.LDAP);
    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.NONE);
    return ldapConnection;
  }
}
