/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.StringPart;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class OrganizationResourceAuthzTest
    extends AbstractResourceAuthzTest
{

  @Test
  public void testGenerateIcon() throws Exception {
    String hash = "abababababababababab";
    String url = getRestUrl(OrganizationResource.SERVICE_PATH + '/' + OrganizationResource.GENERATE_ICON_PATH, hash);
    setSaasResponseForURI("rest/application/icon/generate/" + hash, 200, new byte[0]);
    Response response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetIcon() throws Exception {
    grantReadPermission(org.getId());

    String url = getRestUrl(OrganizationResource.SERVICE_PATH + '/' + OrganizationResource.GET_ICON_PATH, org.getId());
    testAuthzGet(url, 307);
  }

  @Test
  public void testSetIcon() throws Exception {
    grantWritePermission(org.getId());

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
    grantWritePermission(org.getId());

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
}
