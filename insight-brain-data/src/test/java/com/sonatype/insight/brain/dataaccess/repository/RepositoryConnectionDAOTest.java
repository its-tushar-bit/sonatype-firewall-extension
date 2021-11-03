/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryConnectionDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryConnectionDAO dao = new RepositoryConnectionDAO();

  @Test
  public void testCRUD() throws Exception {
    //Create
    RepositoryConnection connection = new RepositoryConnection("appId", "url", "user", "pass".toCharArray());
    dao.insert(connection);
    assertThat(connection.getId()).isNotNull();

    //Delete
    dao.delete(connection);
    assertThat(dao.getById(connection.getId())).isNull();

    //Read
    connection = tempEntity.newRepositoryConnection();
    connection = dao.getById(connection.getId());
    assertRepositoryConnection(connection, "ownerId", "baseUrl", "username", "password");

    //Update
    connection.setOwnerId("updatedAppId");
    connection.setBaseUrl("updatedUrl");
    connection.setUsername("updatedUser");
    connection.setPassword("updatedPass".toCharArray());
    dao.update(connection);
    connection = dao.getById(connection.getId());
    assertRepositoryConnection(connection, "updatedAppId", "updatedUrl", "updatedUser", "updatedPass");
  }

  @Test
  public void testGetByOwnerId() {
    RepositoryConnection conn1 = tempEntity.newRepositoryConnection("owner1", "url1", "u1", "p1".toCharArray());
    tempEntity.newRepositoryConnection(conn1.getOwnerId(), "url2", "u2", "p2".toCharArray());
    tempEntity.newRepositoryConnection("anotherOwnerId", "url3", "u3", "p3".toCharArray());

    List<RepositoryConnection> connections = dao.getByOwnerId(conn1.getOwnerId());
    assertThat(connections).hasSize(2).extracting(RepositoryConnection::getBaseUrl)
        .containsExactlyInAnyOrder("url1", "url2");
  }

  private void assertRepositoryConnection(
      RepositoryConnection connection,
      String ownerId,
      String baseUrl,
      String username,
      String password)
  {
    assertThat(connection.getOwnerId()).isEqualTo(ownerId);
    assertThat(connection.getBaseUrl()).isEqualTo(baseUrl);
    assertThat(connection.getUsername()).isEqualTo(username);
    assertThat(Objects.deepEquals(connection.getPassword(), password.toCharArray())).isTrue();
  }
}
