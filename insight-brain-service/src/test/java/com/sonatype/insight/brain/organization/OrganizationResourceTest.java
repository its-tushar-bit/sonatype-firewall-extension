/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import com.ning.http.multipart.StringPart;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OrganizationResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    // Create
    Organization organization = new Organization();
    organization.setName("OrganizationResourceTest");

    Response response = RestAccess.post(getServiceURL(), JsonHelpers.asJson(organization));
    assertResponseStatus(200, response);
    organization = JsonHelpers.fromJson(response.getResponseBody(), Organization.class);
    assertNotNull(organization);
    assertNotNull(organization.getId());
    assertEquals("OrganizationResourceTest", organization.getName());
    String organizationId = organization.getId();

    // Get
    response = RestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    Organization[] organizations = JsonHelpers.fromJson(response.getResponseBody(), Organization[].class);
    assertNotNull(organizations);
    assertEquals(1, organizations.length);
    organization = organizations[0];
    assertNotNull(organization);
    assertEquals(organizationId, organization.getId());
    assertEquals("OrganizationResourceTest", organization.getName());

    // Add invalid icon
    byte[] defaultIconByteArray = IconUtils.loadInvalidIcon();
    AsyncHttpClient.BoundRequestBuilder builder = RestAccess.getClient().preparePost(getSetIconServiceUrl());
    builder.addBodyPart(new StringPart("organizationId", organizationId));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("defaulticon_organization.png",
        defaultIconByteArray)));
    Future<Response> futureResponse = builder.execute();

    response = futureResponse.get();
    assertResponseStatus(400, response);
    Assert.assertEquals("defaulticon_organization.png is not a valid image.", response.getResponseBody());

    // Get icon (default icon)
    defaultIconByteArray = loadDefaultIcon();
    Response iconResponse = RestAccess.get(getGetIconServiceUrl(organizationId));
    assertResponseStatus(307, iconResponse);
    Assert.assertEquals(getRestBaseUrl() + "assets/img/defaulticon_organization.png",
        iconResponse.getHeader("Location"));

    // Add icon
    builder = RestAccess.getClient().preparePost(getSetIconServiceUrl());
    builder.addBodyPart(new StringPart("organizationId", organizationId));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("defaulticon_organization.png",
        defaultIconByteArray)));
    futureResponse = builder.execute();
    response = futureResponse.get();
    assertResponseStatus(204, response);

    // Get icon
    iconResponse = RestAccess.get(getGetIconServiceUrl(organizationId));
    assertResponseStatus(200, iconResponse);
    InputStream iconStream = iconResponse.getResponseBodyAsStream();
    BufferedImage icon = null;
    try {
      icon = ImageIO.read(iconStream);
    }
    finally {
      iconStream.close();
    }
    Assert.assertNotNull(icon);
    Assert.assertEquals(420, icon.getHeight());
    Assert.assertEquals(420, icon.getWidth());

    // Update
    organization.setName("OrganizationResourceTest updated");
    response = RestAccess.put(getServiceURL(), JsonHelpers.asJson(organization));
    assertResponseStatus(200, response);
    organization = JsonHelpers.fromJson(response.getResponseBody(), Organization.class);
    assertNotNull(organization);
    assertEquals(organizationId, organizationId);
    assertEquals("OrganizationResourceTest updated", organization.getName());

    // Update icon
    builder = RestAccess.getClient().preparePost(getSetIconServiceUrl());
    builder.addBodyPart(new StringPart("organizationId", organizationId));
    builder.addBodyPart(new StringPart("hasRobotSource", "false"));
    futureResponse = builder.execute();
    response = futureResponse.get();
    assertResponseStatus(204, response);

    // now create related objects to test delete cascades

    // application
    final ApplicationDAO applicationDAO = new ApplicationDAO();
    final Application application = new Application();
    application.setName("testapp");
    application.setPublicId("testapp");
    application.setOrganizationId(organization.getId());
    applicationDAO.insert(application);
    // policy
    final PolicyDAO policyDAO = new PolicyDAO(brain.getWorkDir());
    final Policy policy = new Policy();
    policy.setName("testpolicy");
    final Constraint constraint1 = new Constraint(null, "testconstraint", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint1);
    policy.setOwnerId(organizationId);
    policyDAO.insert(organizationId, policy);
    // note that other related objects (labels, license thread groups, etc) are deleted by DAO and tested at DAO level

    // Delete
    response = RestAccess.delete(getServiceURL() + "/" + organizationId);
    assertResponseStatus(204, response);
    Assert.assertNull(new OrganizationDAO().getById(organizationId));
    // Default icon redirect should be returned
    iconResponse = RestAccess.get(getServiceURL() + "/icon/" + organizationId);
    assertResponseStatus(307, iconResponse);
    Assert.assertEquals(getRestBaseUrl() + "assets/img/defaulticon_organization.png",
        iconResponse.getHeader("Location"));
    // assert related objects were deleted
    Assert.assertNull(applicationDAO.getById(application.getId()));
    Assert.assertNull(policyDAO.getByOwnerIdAndPolicyId(organizationId, policy.getId()));
  }

  @Test
  public void testAddOrganization_Unlicensed() throws Exception {
    uninstallLicense();
    Organization organization = new Organization();
    organization.setName("OrganizationResourceTest");

    Response response = RestAccess.post(getServiceURL(), JsonHelpers.asJson(organization));
    assertResponseStatus(402, response);
  }

  @Test
  public void testUpdateOrganization_Unlicensed() throws Exception {
    Organization organization = new Organization();
    organization.setName("OrganizationResourceTest");
    Response response = RestAccess.post(getServiceURL(), JsonHelpers.asJson(organization));
    assertResponseStatus(200, response);
    uninstallLicense();
    response = RestAccess.put(getServiceURL(), JsonHelpers.asJson(organization));
    assertResponseStatus(402, response);

    new OrganizationDAO().delete(organization);
  }

  @Test
  public void testGetAll_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = RestAccess.get(getServiceURL());
    assertResponseStatus(402, response);
  }

  @Test
  public void testGenerateIcon() throws Exception {
    String hashcode = "abababababababababab";
    String url = getGenerateIconServiceUrl(hashcode);
    String saasUrl = "rest/application/icon/generate/" + hashcode;
    setSaasResponseForURI(saasUrl, 200, loadDefaultIcon());
    Response response = RestAccess.get(url);
    assertResponseStatus(200, response);
    Assert.assertNotNull(response.getResponseBodyAsBytes());
  }

  private String getServiceURL() {
    return getRestBaseUrl() + OrganizationResource.SERVICE_PATH;
  }

  private String getGenerateIconServiceUrl(String hashcode) {
    return getServiceURL() + "/" + OrganizationResource.GENERATE_ICON_PATH.replace("{hashcode}", hashcode);
  }

  private String getGetIconServiceUrl(String organizationId) {
    return getServiceURL() + "/" + OrganizationResource.GET_ICON_PATH.replace("{organizationId}", organizationId);
  }

  private String getSetIconServiceUrl() {
    return getServiceURL() + "/" + OrganizationResource.ICON_PATH;
  }

  private byte[] loadDefaultIcon() throws IOException {
    return IconUtils.loadIcon("defaulticon_organization.png");
  }
}
