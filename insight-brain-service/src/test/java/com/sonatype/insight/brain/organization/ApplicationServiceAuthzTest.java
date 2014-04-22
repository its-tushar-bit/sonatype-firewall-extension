/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ApplicationServiceAuthzTest
    extends AbstractServiceAuthzTest
{

  @Inject
  private ApplicationService applicationService;

  @Test
  public void testGetApplicationsWithReadPermission() {
    grantReadPermission(app.getId());
    Application newApp = tempEntity.newApplication(org.getId());

    List<Application> applications = applicationService.getApplications();

    Assert.assertThat(applications, hasSize(1));
    Assert.assertThat(app.getId(), equalTo(applications.get(0).getId()));

    grantReadPermission(newApp.getId());
    applications = applicationService.getApplications();
    Assert.assertThat(applications, hasSize(2));
  }

  @Test
  public void testAddApplication_Authorized() {
    grantWritePermission(org.getId());

    Application application = new Application();
    application.setName("My Application");
    application.setOrganizationId(org.getId());
    application.setPublicId("MyApp");

    // Test the add application
    application = applicationService.addApplication(application);

    // Now clean up by deleting the application
    tempEntity.register(application);
  }

  @Test
  public void testAddApplication_Unauthenticated() {
    final Application application = new Application();
    try {
      applicationService.addApplication(application);
      fail("Expected UnauthenticatedException");
    }
    catch (UnauthenticatedException ignore) {
      // Properly thrown exception.
    }
  }

  @Test
  public void testDeleteApplicationByPublicId_Authorized() throws Exception {
    grantWritePermission(app.getId());
    applicationService.deleteApplicationByPublicId(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteApplicationByPublicId_Unauthenticated() throws Exception {
    applicationService.deleteApplicationByPublicId(app.getPublicId());
  }

  @Test
  public void testGetAllApplications_Authorized() throws Exception {
    grantReadPermission(app.getId());
    final List<Application> applications = applicationService.getApplications();
    assertThat(applications, hasSize(1));
    final Application application = applications.get(0);
    assertThat(application.getId(), is(app.getId()));
    assertThat(application.getName(), is(app.getName()));
  }

  @Test
  public void testGetAllApplications_Unauthenticated() throws Exception {
    List<Application> applications = applicationService.getApplications();
    assertThat(applications, hasSize(0));
  }

  @Test
  public void testGetApplicationByPublicId_Authorized() throws Exception {
    grantReadPermission(app.getId());

    applicationService.getApplicationByPublicId(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationByPublicId_Unauthenticated() throws Exception {
    applicationService.getApplicationByPublicId(app.getPublicId());
  }

  @Test
  public void testGetApplicationByPublicIdNotNull_Authorized() throws Exception {
    grantReadPermission(app.getId());

    applicationService.getApplicationByPublicIdNotNull(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationByPublicIdNotNull_Unauthenticated() throws Exception {
    applicationService.getApplicationByPublicIdNotNull(app.getPublicId());
  }

  @Test
  public void testUpdateApplication_Authorized() throws Exception {
    grantWritePermission(app.getId());

    String newName = "TestUpdateName";
    app.setName(newName);
    applicationService.updateApplication(app);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateApplication_Unauthenticated() throws Exception {
    app.setName("TestUpdateName");
    applicationService.updateApplication(app);
  }
}
