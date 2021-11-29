/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryConnectionDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryConnectionDAO dao = new RepositoryConnectionDAO();

  @Test
  public void testCRUD() throws Exception {
    //Create
    RepositoryConnection connection =
        new RepositoryConnection("appId", "url", RepositoryFormat.GENERIC, "user", "pass".toCharArray());
    dao.insert(connection);
    assertThat(connection.getId()).isNotNull();

    //Delete
    dao.delete(connection);
    assertThat(dao.getById(connection.getId())).isNull();

    //Read
    connection = tempEntity.newRepositoryConnection();
    connection = dao.getById(connection.getId());
    assertRepositoryConnection(connection, "ownerId", "baseUrl", RepositoryFormat.GENERIC, "username", "password");

    //Update
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
    assertThat(connections).hasSize(2).extracting(RepositoryConnection::getBaseUrl)
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
  public void testGetByOwnerIdWithHierarchy_Application() {
    tempEntity.newRepositoryConnection("other");
    Application application = tempEntity.newApplicationWithParent();
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;
    String orgId = application.getParentOwnerId();
    String appId = application.getId();

    // None
    assertThat(dao.getByOwnerIdWithHierarchy(appId)).isEmpty();

    // Only root org
    RepositoryConnection rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(dao.getByOwnerIdWithHierarchy(appId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(rootOrgRepositoryConnection);

    // Root org and org
    RepositoryConnection orgRepositoryConnection = tempEntity.newRepositoryConnection(orgId);
    assertThat(dao.getByOwnerIdWithHierarchy(appId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(orgRepositoryConnection);

    // Root org, org, and app
    RepositoryConnection appRepositoryConnection = tempEntity.newRepositoryConnection(appId);
    assertThat(dao.getByOwnerIdWithHierarchy(appId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(appRepositoryConnection);

    // Org and app
    dao.delete(rootOrgRepositoryConnection);
    assertThat(dao.getByOwnerIdWithHierarchy(appId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(appRepositoryConnection);

    // Only app
    dao.delete(orgRepositoryConnection);
    assertThat(dao.getByOwnerIdWithHierarchy(appId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(appRepositoryConnection);

    // Root org and app
    rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(dao.getByOwnerIdWithHierarchy(appId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(appRepositoryConnection);

    // Only org
    dao.delete(rootOrgRepositoryConnection);
    dao.delete(appRepositoryConnection);
    orgRepositoryConnection = tempEntity.newRepositoryConnection(orgId);
    assertThat(dao.getByOwnerIdWithHierarchy(appId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(orgRepositoryConnection);
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_Organization() {
    tempEntity.newRepositoryConnection("other");
    Organization organization = tempEntity.newOrganization();
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;
    String orgId = organization.getId();

    // None
    assertThat(dao.getByOwnerIdWithHierarchy(orgId)).isEmpty();

    // Only root org
    RepositoryConnection rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(dao.getByOwnerIdWithHierarchy(orgId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(rootOrgRepositoryConnection);

    // Root org and org
    RepositoryConnection orgRepositoryConnection = tempEntity.newRepositoryConnection(orgId);
    assertThat(dao.getByOwnerIdWithHierarchy(orgId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(orgRepositoryConnection);

    // Only org
    dao.delete(rootOrgRepositoryConnection);
    assertThat(dao.getByOwnerIdWithHierarchy(orgId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(orgRepositoryConnection);
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_RootOrganization() {
    tempEntity.newRepositoryConnection("other");
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;

    // None
    assertThat(dao.getByOwnerIdWithHierarchy(rootOrgId)).isEmpty();

    // Only root org
    RepositoryConnection rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId);
    assertThat(dao.getByOwnerIdWithHierarchy(rootOrgId)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(rootOrgRepositoryConnection);
  }

  @Test
  public void testGetByOwnerIdAndFormatWithHierarchy_Application() {
    tempEntity.newRepositoryConnection("other", RepositoryFormat.MAVEN);
    Application application = tempEntity.newApplicationWithParent();
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;
    String orgId = application.getParentOwnerId();
    String appId = application.getId();
    tempEntity.newRepositoryConnection(rootOrgId, RepositoryFormat.NPM);
    tempEntity.newRepositoryConnection(orgId, RepositoryFormat.NPM);
    tempEntity.newRepositoryConnection(appId, RepositoryFormat.NPM);

    // None
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(appId, RepositoryFormat.MAVEN)).isEmpty();

    // Only root org
    RepositoryConnection rootOrgRepositoryConnection =
        tempEntity.newRepositoryConnection(rootOrgId, RepositoryFormat.MAVEN);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(appId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(rootOrgRepositoryConnection);

    // Root org and org
    RepositoryConnection orgRepositoryConnection = tempEntity.newRepositoryConnection(orgId, RepositoryFormat.MAVEN);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(appId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator().containsExactly(orgRepositoryConnection);

    // Root org, org, and app
    RepositoryConnection appRepositoryConnection = tempEntity.newRepositoryConnection(appId, RepositoryFormat.MAVEN);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(appId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator().containsExactly(appRepositoryConnection);

    // Org and app
    dao.delete(rootOrgRepositoryConnection);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(appId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator().containsExactly(appRepositoryConnection);

    // Only app
    dao.delete(orgRepositoryConnection);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(appId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator().containsExactly(appRepositoryConnection);

    // Root org and app
    rootOrgRepositoryConnection = tempEntity.newRepositoryConnection(rootOrgId, RepositoryFormat.MAVEN);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(appId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator().containsExactly(appRepositoryConnection);

    // Only org
    dao.delete(rootOrgRepositoryConnection);
    dao.delete(appRepositoryConnection);
    orgRepositoryConnection = tempEntity.newRepositoryConnection(orgId, RepositoryFormat.MAVEN);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(appId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator().containsExactly(orgRepositoryConnection);
  }

  @Test
  public void testGetByOwnerIdAndFormatWithHierarchy_Organization() {
    tempEntity.newRepositoryConnection("other", RepositoryFormat.MAVEN);
    Organization organization = tempEntity.newOrganization();
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;
    String orgId = organization.getId();
    tempEntity.newRepositoryConnection(rootOrgId, RepositoryFormat.NPM);
    tempEntity.newRepositoryConnection(orgId, RepositoryFormat.NPM);

    // None
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(orgId, RepositoryFormat.MAVEN)).isEmpty();

    // Only root org
    RepositoryConnection rootOrgRepositoryConnection =
        tempEntity.newRepositoryConnection(rootOrgId, RepositoryFormat.MAVEN);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(orgId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(rootOrgRepositoryConnection);

    // Root org and org
    RepositoryConnection orgRepositoryConnection = tempEntity.newRepositoryConnection(orgId, RepositoryFormat.MAVEN);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(orgId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator().containsExactly(orgRepositoryConnection);

    // Only org
    dao.delete(rootOrgRepositoryConnection);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(orgId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator().containsExactly(orgRepositoryConnection);
  }

  @Test
  public void testGetByOwnerIdAndFormatWithHierarchy_RootOrganization() {
    tempEntity.newRepositoryConnection("other", RepositoryFormat.MAVEN);
    String rootOrgId = Organization.ROOT_ORGANIZATION_ID;
    tempEntity.newRepositoryConnection(rootOrgId, RepositoryFormat.NPM);

    // None
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(rootOrgId, RepositoryFormat.MAVEN)).isEmpty();

    // Only root org
    RepositoryConnection rootOrgRepositoryConnection =
        tempEntity.newRepositoryConnection(rootOrgId, RepositoryFormat.MAVEN);
    assertThat(dao.getByOwnerIdAndFormatsWithHierarchy(rootOrgId,
        RepositoryFormat.MAVEN)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(rootOrgRepositoryConnection);
  }

  @Test
  public void testGetByOwnerIdAndBaseUrl() throws Exception {
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
