/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.artifactory;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link ArtifactoryConnectionDAOTest} (CLM-45228).
 */
@PostgresTest
public class ArtifactoryConnectionDAOPgTest
    extends AbstractDbDAOTest
{
  private ArtifactoryConnectionDAO dao;

  private DAOSecretRotator daoSecretRotator;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createArtifactoryConnectionDAO();
    daoSecretRotator = new DAOSecretRotator();
  }

  @Test
  public void testRotateEncryptedSecrets() throws SQLException {
    tempEntity.newArtifactoryConnection("ownerId1", "baseUrl1", "username1", "passwordOld1".toCharArray());
    tempEntity.newArtifactoryConnection("ownerId1", "baseUrl2", "username2", "passwordOld2".toCharArray());
    tempEntity.newArtifactoryConnection("ownerId2", "baseUrl3", "username3", "passwordOld3".toCharArray());
    tempEntity.newArtifactoryConnection("ownerId2", "baseUrl3", "username3", null);

    Function<String, String> secretRotator = secret -> secret.replace("Old", "New");

    daoSecretRotator.rotateEncryptedSecrets(dao, secretRotator);

    List<ArtifactoryConnection> results = dao.getAll();

    assertThat(results.stream().filter(ac -> ac.getPassword() == null).count()).isEqualTo(1);
    assertThat(results.stream().filter(ac -> ac.getPassword() != null).count()).isEqualTo(3);
    results.stream()
        .filter(ac -> ac.getPassword() != null)
        .forEach(ac -> {
          assertThat(String.valueOf(ac.getPassword())).doesNotContain("Old");
          assertThat(String.valueOf(ac.getPassword())).contains("New");
        });
  }
}
