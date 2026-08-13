/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link RepositoryConnectionDAOTest} (CLM-45228).
 */
@PostgresTest
public class RepositoryConnectionDAOPgTest
    extends AbstractDbDAOTest
{
  private RepositoryConnectionDAO dao;

  private DAOSecretRotator daoSecretRotator;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryConnectionDAO();
    daoSecretRotator = new DAOSecretRotator();
  }

  @Test
  public void testRotateEncryptedSecrets() throws SQLException {
    tempEntity.newRepositoryConnection("owner1", "url1", RepositoryFormat.MAVEN, "u1", "passwordOld1".toCharArray());
    tempEntity.newRepositoryConnection("owner2", "url2", RepositoryFormat.NPM, "u1", "passwordOld2".toCharArray());
    tempEntity.newRepositoryConnection("owner3", "url3", RepositoryFormat.GENERIC, "u1", "passwordOld3".toCharArray());
    tempEntity.newRepositoryConnection("owner4", "url4", RepositoryFormat.MAVEN, "u1", null);

    Function<String, String> secretRotator = secret -> secret.replace("Old", "New");

    daoSecretRotator.rotateEncryptedSecrets(dao, secretRotator);

    List<RepositoryConnection> results = dao.getAll();

    assertThat(results.stream().filter(rc -> rc.getPassword() == null).count()).isEqualTo(1);
    assertThat(results.stream().filter(rc -> rc.getPassword() != null).count()).isEqualTo(3);
    results.stream()
        .filter(rc -> rc.getPassword() != null)
        .forEach(rc -> {
          assertThat(String.valueOf(rc.getPassword())).doesNotContain("Old");
          assertThat(String.valueOf(rc.getPassword())).contains("New");
        });
  }

  private void assertRepositoryConnection(
      RepositoryConnection connection,
      String ownerId,
      String baseUrl,
      RepositoryFormat format,
      String username,
      String password)
  {
    assertThat(connection.getOwnerId()).isEqualTo(ownerId);
    assertThat(connection.getBaseUrl()).isEqualTo(baseUrl);
    assertThat(connection.getFormat()).isEqualTo(format);
    assertThat(connection.getUsername()).isEqualTo(username);
    assertThat(Objects.deepEquals(connection.getPassword(), password.toCharArray())).isTrue();
  }
}
