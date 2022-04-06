/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.artifactory;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactoryConnectionDAOTest
    extends AbstractDbDAOTest
{
  private final ArtifactoryConnectionDAO dao = new ArtifactoryConnectionDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    ArtifactoryConnection artifactoryConnection =
        new ArtifactoryConnection("ownerId", "baseUrl", "username", "password".toCharArray());
    dao.insert(artifactoryConnection);
    tempEntity.register(artifactoryConnection);
    assertThat(artifactoryConnection.getId()).isNotNull();

    // Read
    assertThat(dao.getById(artifactoryConnection.getId())).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(artifactoryConnection);

    // Update
    artifactoryConnection.setOwnerId(artifactoryConnection.getOwnerId() + "2");
    artifactoryConnection.setBaseUrl(artifactoryConnection.getBaseUrl() + "2");
    artifactoryConnection.setUsername(artifactoryConnection.getUsername() + "2");
    artifactoryConnection.setPassword((String.valueOf(artifactoryConnection.getPassword()) + "2").toCharArray());
    dao.update(artifactoryConnection);
    assertThat(dao.getById(artifactoryConnection.getId())).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
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

    assertThat(dao.getById(artifactoryConnection.getId())).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(artifactoryConnection);
  }
}
