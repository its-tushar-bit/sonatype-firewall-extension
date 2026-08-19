/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryConnectionDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryConnectionDAO dao;

  private DAOSecretRotator daoSecretRotator;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryConnectionDAO();
    daoSecretRotator = new DAOSecretRotator();
  }

  @Test
  public void testCRUD() {
    // Create
    RepositoryConnection connection =
        new RepositoryConnection("appId", "url", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    dao.insert(connection);
    assertThat(connection.getId()).isNotNull();

    // Delete
    dao.delete(connection);
    assertThat(dao.getById(connection.getId())).isNull();

    // Read
    connection = tempEntity.newRepositoryConnection();
    connection = dao.getById(connection.getId());
    assertRepositoryConnection(connection, "ownerId", "baseUrl", RepositoryFormat.GENERIC, "username", "password");

    // Update
    connection.setOwnerId("updatedAppId");
    connection.setBaseUrl("updatedUrl");
    connection.setFormat(RepositoryFormat.MAVEN);
    connection.setUsername("updatedUser");
    connection.setPassword("updatedPass".toCharArray());
    dao.update(connection);
    connection = dao.getById(connection.getId());
    assertRepositoryConnection(connection, "updatedAppId", "updatedUrl", RepositoryFormat.MAVEN, "updatedUser",
        "updatedPass");
  }

  @Test
  public void testGetByOwnerId() {
    RepositoryConnection conn1 =
        tempEntity.newRepositoryConnection("owner1", "url1", RepositoryFormat.MAVEN, "u1", "p1".toCharArray());
    tempEntity.newRepositoryConnection(conn1.getOwnerId(), "url2", "u2", "p2".toCharArray());
    tempEntity.newRepositoryConnection("anotherOwnerId", "url3", "u3", "p3".toCharArray());

    List<RepositoryConnection> connections = dao.getByOwnerId(conn1.getOwnerId());
    assertThat(connections).hasSize(2)
        .extracting(RepositoryConnection::getBaseUrl)
        .containsExactlyInAnyOrder("url1", "url2");
  }

  @Test
  public void testGetByOwnerIdAndFormats_Single() {
    RepositoryConnection conn1 =
        tempEntity.newRepositoryConnection("owner1", "url1", RepositoryFormat.GENERIC, "u1", "p1".toCharArray());
    tempEntity.newRepositoryConnection("owner1", "url2", RepositoryFormat.MAVEN, "u2", "p2".toCharArray());
    tempEntity.newRepositoryConnection("owner1", "url3", RepositoryFormat.NPM, "u3", "p3".toCharArray());
    tempEntity.newRepositoryConnection("owner2", "url4", RepositoryFormat.GENERIC, "u4", "p4".toCharArray());

    List<RepositoryConnection> connections = dao.getByOwnerIdAndFormats("owner1", RepositoryFormat.GENERIC);
    assertThat(connections).usingRecursiveFieldByFieldElementComparator().containsExactly(conn1);
  }

  @Test
  public void testGetByOwnerIdAndFormats_Multiple() {
    RepositoryConnection conn1 =
        tempEntity.newRepositoryConnection("owner1", "url1", RepositoryFormat.GENERIC, "u1", "p1".toCharArray());
    RepositoryConnection conn2 =
        tempEntity.newRepositoryConnection("owner1", "url2", RepositoryFormat.MAVEN, "u2", "p2".toCharArray());
    tempEntity.newRepositoryConnection("owner1", "url3", RepositoryFormat.NPM, "u3", "p3".toCharArray());
    tempEntity.newRepositoryConnection("owner2", "url4", RepositoryFormat.MAVEN, "u4", "p4".toCharArray());

    List<RepositoryConnection> connections =
        dao.getByOwnerIdAndFormats("owner1", RepositoryFormat.MAVEN, RepositoryFormat.GENERIC);
    assertThat(connections).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(conn1, conn2);
  }

  @Test
  public void testGetByOwnerIdAndBaseUrl() {
    tempEntity.newRepositoryConnection("owner1", "url1", "u1", "p1".toCharArray());

    RepositoryConnection connection = dao.getByOwnerIdAndBaseUrl("owner1", "url1");
    assertThat(connection).isNotNull();
    assertThat(connection.getId()).isNotNull();
    assertThat(connection.getUsername()).isEqualTo("u1");
    assertThat(connection.getPassword()).isEqualTo("p1".toCharArray());
  }

  @Test
  public void testGetByOwnerIdAndFormat() {
    RepositoryConnection expectedConnection =
        tempEntity.newRepositoryConnection("owner1", "url1", RepositoryFormat.MAVEN, "u1", "p1".toCharArray());
    tempEntity.newRepositoryConnection("owner2", "url1", RepositoryFormat.MAVEN, "u1", "p1".toCharArray());
    tempEntity.newRepositoryConnection("owner1", "url1", RepositoryFormat.NPM, "u1", "p1".toCharArray());

    RepositoryConnection connection = dao.getByOwnerIdAndFormat("owner1", RepositoryFormat.MAVEN);
    assertThat(connection).usingRecursiveComparison().isEqualTo(expectedConnection);
  }

  @Test
  public void testGetByIdAndOwnerId() {
    Organization organization = tempEntity.newOrganization();
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(organization.getId());

    assertThat(dao.getByIdAndOwnerId(repositoryConnection.getId(), organization.getId())).usingRecursiveComparison()
        .isEqualTo(repositoryConnection);
  }

  @Test
  public void testGetByIdAndOwnerId_BadOwnerId() {
    Organization organization = tempEntity.newOrganization();
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(organization.getId());

    assertThat(dao.getByIdAndOwnerId(repositoryConnection.getId(), tempEntity.newOrganization().getId())).isNull();
  }

  @Test
  public void testGetByIdAndOwnerId_BadId() {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newRepositoryConnection(organization.getId());

    assertThat(dao.getByIdAndOwnerId("doesNotExist", organization.getId())).isNull();
  }

  @Test
  public void testDeleteAll() {
    RepositoryConnection repositoryConnection1 =
        tempEntity.newRepositoryConnection("owner2", "url1", RepositoryFormat.MAVEN, "u1", "p1".toCharArray());
    RepositoryConnection repositoryConnection2 =
        tempEntity.newRepositoryConnection("owner1", "url1", RepositoryFormat.NPM, "u1", "p1".toCharArray());

    dao.deleteAll();

    assertThat(dao.getById(repositoryConnection1.getId())).isNull();
    assertThat(dao.getById(repositoryConnection2.getId())).isNull();
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
