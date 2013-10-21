/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.StringPart;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ApplicationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGenerateIcon() throws Exception {
    User user = tempEntity.newUser();
    String hash = "abababababababababab";
    String url = getRestUrl(ApplicationResource.SERVICE_PATH + '/' + ApplicationResource.GENERATE_ICON_PATH, hash);
    setSaasResponseForURI("rest/application/icon/generate/" + hash, 200, new byte[0]);
    Response response = RestAccess.get(url, user.getUsername(), user.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetApplication() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(ApplicationResource.SERVICE_PATH + '/' + ApplicationResource.GET_APPLICATION_PATH,
        app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetApplicationManagementSummary() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(ApplicationResource.SERVICE_PATH + '/'
        + ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARY, app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetIcon() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(ApplicationResource.SERVICE_PATH + '/' + ApplicationResource.GET_APPLICATION_ICON_PATH,
        app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(307, response);
  }

  @Test
  public void testSetIcon() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(
        getRestUrl(ApplicationResource.SERVICE_PATH + '/' + ApplicationResource.ICON_PATH));
    builder.addBodyPart(new StringPart("applicationId", app.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    RestAccess.addAuthorization(builder, unauthorized.getUsername(), unauthorized.getPassword());
    Response response = builder.execute().get();
    assertResponseStatus(403, response);

    builder = AuthedRestAccess.getClient().preparePost(
        getRestUrl(ApplicationResource.SERVICE_PATH + '/' + ApplicationResource.ICON_PATH));
    builder.addBodyPart(new StringPart("applicationId", app.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    RestAccess.addAuthorization(builder, authorized.getUsername(), authorized.getPassword());
    response = builder.execute().get();
    assertResponseStatus(204, response);
  }

  @Test
  public void testSetIconSync() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(
        getRestUrl(ApplicationResource.SERVICE_PATH + '/' + ApplicationResource.ICON_PATH_SYNC));
    builder.addBodyPart(new StringPart("applicationId", app.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    RestAccess.addAuthorization(builder, unauthorized.getUsername(), unauthorized.getPassword());
    Response response = builder.execute().get();
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), is("Insufficient permissions"));

    builder = AuthedRestAccess.getClient().preparePost(
        getRestUrl(ApplicationResource.SERVICE_PATH + '/' + ApplicationResource.ICON_PATH_SYNC));
    builder.addBodyPart(new StringPart("applicationId", app.getId()));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    RestAccess.addAuthorization(builder, authorized.getUsername(), authorized.getPassword());
    response = builder.execute().get();
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), is(""));
  }

  @Test
  public void testAddApplication() throws Exception {
    Application app = new Application("test-app", "test-app", org.getId());
    Role role = tempEntity.newRole(true, Permission.WRITE);
    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(ApplicationResource.SERVICE_PATH);
    Response response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(app));
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), toJson(app));
    assertResponseStatus(200, response);
    app = fromJson(response, Application.class);
    new ApplicationDAO().delete(app);
  }

  @Test
  public void testUpdateApplication() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(ApplicationResource.SERVICE_PATH);
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(app));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(app));
    assertResponseStatus(200, response);
  }

  @Test
  public void testDeleteApplication() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(ApplicationResource.SERVICE_PATH + '/' + ApplicationResource.GET_APPLICATION_PATH,
        app.getPublicId());
    Response response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);
  }
}
