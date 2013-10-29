/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.StringPart;
import org.junit.Test;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class OrganizationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetAll() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(OrganizationResource.SERVICE_PATH);
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(200, response);
    Organization[] entities = fromJson(response, Organization[].class);
    assertThat(entities, is(emptyArray()));

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
    entities = fromJson(response, Organization[].class);
    assertThat(entities.length, is(1));
    assertThat(entities[0].getId(), is(org.getId()));
  }

  @Test
  public void testGenerateIcon() throws Exception {
    String hash = "abababababababababab";
    String url = getRestUrl(OrganizationResource.SERVICE_PATH + '/' + OrganizationResource.GENERATE_ICON_PATH, hash);
    setSaasResponseForURI("rest/application/icon/generate/" + hash, 200, new byte[0]);
    Response response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testAddOrganization() throws Exception {
    Organization org = new Organization("test-org");
    Role role = tempEntity.newRole(true, Permission.WRITE);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());

    String url = getRestUrl(OrganizationResource.SERVICE_PATH);
    Response response = testAuthzPost(url, toJson(org));
    org = fromJson(response, Organization.class);
    new OrganizationDAO().delete(org);
  }

  @Test
  public void testUpdateOrganization() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(OrganizationResource.SERVICE_PATH);
    testAuthzPut(url, toJson(org));
  }

  @Test
  public void testGetIcon() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(OrganizationResource.SERVICE_PATH + '/' + OrganizationResource.GET_ICON_PATH, org.getId());
    testAuthzGet(url, 307);
  }

  @Test
  public void testSetIcon() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(
        getRestUrl(OrganizationResource.SERVICE_PATH + '/' + OrganizationResource.ICON_PATH));
    builder.addBodyPart(new StringPart("organizationId", org.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    RestAccess.addAuthorization(builder, unauthorized.getUsername(), unauthorized.getPassword());
    Response response = builder.execute().get();
    assertResponseStatus(403, response);

    builder = AuthedRestAccess.getClient().preparePost(
        getRestUrl(OrganizationResource.SERVICE_PATH + '/' + OrganizationResource.ICON_PATH));
    builder.addBodyPart(new StringPart("organizationId", org.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    RestAccess.addAuthorization(builder, authorized.getUsername(), authorized.getPassword());
    response = builder.execute().get();
    assertResponseStatus(204, response);
  }

  @Test
  public void testSetIconSync() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(
        getRestUrl(OrganizationResource.SERVICE_PATH + '/' + OrganizationResource.ICON_PATH_SYNC));
    builder.addBodyPart(new StringPart("organizationId", org.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    RestAccess.addAuthorization(builder, unauthorized.getUsername(), unauthorized.getPassword());
    Response response = builder.execute().get();
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), is("Insufficient permissions"));

    builder = AuthedRestAccess.getClient().preparePost(
        getRestUrl(OrganizationResource.SERVICE_PATH + '/' + OrganizationResource.ICON_PATH_SYNC));
    builder.addBodyPart(new StringPart("organizationId", org.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    RestAccess.addAuthorization(builder, authorized.getUsername(), authorized.getPassword());
    response = builder.execute().get();
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), is(""));
  }

  @Test
  public void testDeleteOrganization() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(OrganizationResource.SERVICE_PATH + '/' + OrganizationResource.DELETE_ORGANIZATION_PATH,
        org.getId());
    testAuthzDelete(url);
  }
}
