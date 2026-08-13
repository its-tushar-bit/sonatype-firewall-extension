/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.artifactory;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactoryConnectionDAOTest
    extends AbstractDbDAOTest
{
  private ArtifactoryConnectionDAO dao;

  private DAOSecretRotator daoSecretRotator;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createArtifactoryConnectionDAO();
    daoSecretRotator = new DAOSecretRotator();
  }

  @Test
  public void testCRUD() {
    // Create
    ArtifactoryConnection artifactoryConnection =
        new ArtifactoryConnection("ownerId", "baseUrl", "username", "password".toCharArray());
    dao.insert(artifactoryConnection);
    assertThat(artifactoryConnection.getId()).isNotNull();

    // Read
    assertThat(dao.getById(artifactoryConnection.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(artifactoryConnection);

    // Update
    artifactoryConnection.setOwnerId(artifactoryConnection.getOwnerId() + "2");
    artifactoryConnection.setBaseUrl(artifactoryConnection.getBaseUrl() + "2");
    artifactoryConnection.setUsername(artifactoryConnection.getUsername() + "2");
    artifactoryConnection.setPassword((String.valueOf(artifactoryConnection.getPassword()) + "2").toCharArray());
    dao.update(artifactoryConnection);
    assertThat(dao.getById(artifactoryConnection.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(artifactoryConnection);

    // Delete
    dao.delete(artifactoryConnection);
    assertThat(dao.getById(artifactoryConnection.getId())).isNull();
  }

  @Test
  public void testGetById() {
    ArtifactoryConnection artifactoryConnection =
        tempEntity.newArtifactoryConnection("ownerId1", "baseUrl1", "username1", "password1".toCharArray());
    tempEntity.newArtifactoryConnection("ownerId2", "baseUrl2", "username2", "password2".toCharArray());

    assertThat(dao.getById(artifactoryConnection.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(artifactoryConnection);
  }

  @Test
  public void testGetByOwnerId() {
    tempEntity.newArtifactoryConnection("ownerId1", "baseUrl1", "username1", "password1".toCharArray());
    tempEntity.newArtifactoryConnection("ownerId2", "baseUrl2", "username2", "password2".toCharArray());

    assertThat(dao.getByOwnerId("ownerId1")).extracting(ArtifactoryConnection::getBaseUrl).isEqualTo("baseUrl1");
  }

  @Test
  public void testGetByIdAndOwnerId() {
    ArtifactoryConnection connection1 = tempEntity.newArtifactoryConnection(
        "ownerId1", "baseUrl1", "username1", "password1".toCharArray());
    ArtifactoryConnection connection2 = tempEntity.newArtifactoryConnection(
        "ownerId1", "baseUrl2", "username2", "password2".toCharArray());
    ArtifactoryConnection connection3 = tempEntity.newArtifactoryConnection(
        "ownerId2", "baseUrl3", "username3", "password3".toCharArray());

    assertThat(dao.getByIdAndOwnerId(connection1.getId(), "ownerId1")).extracting(ArtifactoryConnection::getBaseUrl)
        .isEqualTo("baseUrl1");
    assertThat(dao.getByIdAndOwnerId(connection2.getId(), "ownerId1")).extracting(ArtifactoryConnection::getBaseUrl)
        .isEqualTo("baseUrl2");
    assertThat(dao.getByIdAndOwnerId(connection3.getId(), "ownerId2")).extracting(ArtifactoryConnection::getBaseUrl)
        .isEqualTo("baseUrl3");
  }
}
