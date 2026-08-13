/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link LdapConnectionDAOTest} (CLM-45228).
 */
@PostgresTest
public class LdapConnectionDAOPgTest
    extends AbstractDbDAOTest
{
  private DAOSecretRotator daoSecretRotator;

  private LdapConnectionDAO dao;

  private LdapServer ldapServer;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createLdapConnectionDAO();
    daoSecretRotator = new DAOSecretRotator();
    ldapServer = tempEntity.newLdapServer("testServer");
  }

  @Test
  public void testRotateEncryptedSecrets() throws SQLException {
    LdapServer ldapServer2 = tempEntity.newLdapServer("testServer2");
    LdapServer ldapServer3 = tempEntity.newLdapServer("testServer3");
    LdapServer ldapServer4 = tempEntity.newLdapServer("testServer4");

    tempEntity.newLdapConnection(ldapServer.getId(), "passwordOld1".toCharArray());
    tempEntity.newLdapConnection(ldapServer2.getId(), "passwordOld2".toCharArray());
    tempEntity.newLdapConnection(ldapServer3.getId(), "passwordOld3".toCharArray());
    tempEntity.newLdapConnection(ldapServer4.getId(), null);

    Function<String, String> secretRotator = secret -> secret.replace("Old", "New");

    daoSecretRotator.rotateEncryptedSecrets(dao, secretRotator);

    List<LdapConnection> results = dao.getAll();

    assertThat(results.stream().filter(lc -> lc.getSystemPassword() == null).count()).isEqualTo(1);
    assertThat(results.stream().filter(lc -> lc.getSystemPassword() != null).count()).isEqualTo(3);
    results.stream()
        .filter(lc -> lc.getSystemPassword() != null)
        .forEach(lc -> {
          assertThat(String.valueOf(lc.getSystemPassword())).doesNotContain("Old");
          assertThat(String.valueOf(lc.getSystemPassword())).contains("New");
        });
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
