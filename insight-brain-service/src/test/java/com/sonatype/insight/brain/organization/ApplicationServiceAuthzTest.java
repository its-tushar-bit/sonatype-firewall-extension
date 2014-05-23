/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.common.collect.Sets;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
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

  @Test
  public void testGetApplicationsByPublicIdsAndTagIds_Authorized() throws Exception {
    grantReadPermission(app.getId());
    final List<Application> applications = applicationService
        .getApplicationsByPublicIdsAndTagIds(Sets.newHashSet(app.getPublicId()), null);
    assertThat(applications, hasSize(1));
  }

  @Test()
  public void testGetApplicationsByPublicIdsAndTagIds_TwoAppsAuthorized() throws Exception {
    Application app2 = tempEntity.newApplication("App2", "appPubId2", org.getId());
    grantReadPermission(app.getId());
    grantReadPermission(app2.getId());

    final List<Application> applications = applicationService.getApplicationsByPublicIdsAndTagIds(
        Sets.newHashSet(app.getPublicId(), app2.getPublicId()), null);
    assertThat(applications, hasSize(2));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationsByPublicIdsAndTagIds_Unauthenticated() throws Exception {
    applicationService.getApplicationsByPublicIdsAndTagIds(
        Sets.newHashSet(app.getPublicId()), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicationsByPublicIdsAndTagIds_NotAuthorized() throws Exception {
    login();
    applicationService.getApplicationsByPublicIdsAndTagIds(
        Sets.newHashSet(app.getPublicId()), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicationsByPublicIdsAndTagIds_TwoAppsOneNotAuthorized() throws Exception {
    Application app2 = tempEntity.newApplication("App2", "appPubId2", org.getId());
    grantReadPermission(app.getId());
    applicationService.getApplicationsByPublicIdsAndTagIds(
        Sets.newHashSet(app.getPublicId(), app2.getPublicId()), null);
  }

  @Test()
  public void testGetApplicationsByPublicIdsAndTagIds_OnlySeesAppsWithPermission() throws Exception {
    Application app2 = tempEntity.newApplication("App2", "appPubId2", org.getId());
    grantReadPermission(app.getId());

    //request with nothing specified, should only see app1
    List<Application> applications = applicationService.getApplicationsByPublicIdsAndTagIds(null, null);
    assertThat(applications, hasSize(1));
    assertEquals(app.getId(), applications.get(0).getId());

    //now app2 permission and it should show up
    grantReadPermission(app2.getId());
    applications = applicationService.getApplicationsByPublicIdsAndTagIds(null, null);
    assertThat(applications, hasSize(2));
    Set<String> ids = new HashSet<>();
    for (Application a : applications) {
      ids.add(a.getId());
    }
    assertThat(ids, containsInAnyOrder(app.getId(), app2.getId()));
  }

}
