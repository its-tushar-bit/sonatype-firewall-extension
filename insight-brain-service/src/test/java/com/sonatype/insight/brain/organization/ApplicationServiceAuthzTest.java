/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.common.collect.Sets;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId());

    grantReadPermission(newApp.getId());
    applications = applicationService.getApplications();
    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId(), newApp.getId());
  }

  @Test
  public void testAddApplication_Authorized() {
    grantAddApplicationPermission(org.getId());

    Application application = new Application();
    application.setName("My Application");
    application.setOrganizationId(org.getId());
    application.setPublicId("MyApp");

    // Test the add application
    applicationService.addApplication(application);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddApplication_Unauthenticated() {
    final Application application = new Application();
    applicationService.addApplication(application);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddApplication_Unauthorized() {
    grantWritePermission(app.getId());
    applicationService.addApplication(new Application());
  }

  @Test
  public void testDeleteApplicationById_Authorized() throws Exception {
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
    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId());
  }

  @Test
  public void testGetAllApplications_Unauthenticated() throws Exception {
    List<Application> applications = applicationService.getApplications();
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_Authorized() throws Exception {
    tempEntity.newApplication(app.getOrganizationId());

    grantEvaluateComponentPermission(app.getId());
    Map<String, String> applicationNames = applicationService.getApplicationNamesForEvaluateComponent();
    assertThat(applicationNames).hasSize(1).containsEntry(app.getPublicId(), app.getName());
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_Unauthorized() throws Exception {
    login();
    Map<String, String> applicationNames = applicationService.getApplicationNamesForEvaluateComponent();
    assertThat(applicationNames).isEmpty();
  }

  @Test
  public void testValidateApplicationPublicId_Authorized() throws Exception {
    grantWritePermission(app.getId());
    String value = applicationService.validateApplicationPublicId(app.getPublicId());
    assertThat(value).isEqualTo("OK");
  }

  @Test(expected = UnauthorizedException.class)
  public void testValidateApplicationPublicId_Unauthorized() throws Exception {
    grantReadPermission(app.getId());
    applicationService.validateApplicationPublicId(app.getPublicId());
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
  public void testGetApplicationsByPublicIdsAndTagIds_Authorized_byApp() throws Exception {
    grantReadPermission(app.getId());
    final List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(null,
        Sets.newHashSet(app.getId()), null);
    assertThat(applications).hasSize(1);
  }

  @Test
  public void testGetApplicationsByPublicIdsAndTagIds_Authorized_byOrg() throws Exception {
    grantReadPermission(app.getId());
    final List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Sets.newHashSet(app.getParentOwnerId()), null, null);
    assertThat(applications).hasSize(1);
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_FilteredAuthorized() throws Exception {
    grantReadPermission(app.getId());
    List<Application> applications =
        applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(null, null, null);
    assertThat(applications).hasSize(1).extracting(Application::getId).containsExactlyInAnyOrder(app.getId());
  }

  @Test()
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_TwoAppsAuthorized() throws Exception {
    Application app2 = tempEntity.newApplication("App2", "appPubId2", org.getId());
    grantReadPermission(app.getId());
    grantReadPermission(app2.getId());

    final List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        null, Sets.newHashSet(app.getId(), app2.getId()), null);
    assertThat(applications).hasSize(2);
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgAuthorized() throws Exception {
    Application app2 = tempEntity.newApplication("App2", "appPubId2", org.getId());
    grantReadPermission(app.getId());
    grantReadPermission(app2.getId());

    final List<Application> applications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(Sets.newHashSet(app.getParentOwnerId()), null, null);
    assertThat(applications).hasSize(2);
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_Unauthenticated() throws Exception {
    List<Application> applications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, Sets.newHashSet(app.getId()), null);
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_NotAuthorized() throws Exception {
    login();
    List<Application> applications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, Sets.newHashSet(app.getId()), null);
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_withOrg_NotAuthorized() throws Exception {
    login();
    List<Application> applications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(Sets.newHashSet(app.getParentOwnerId()), null, null);
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_TwoAppsOneNotAuthorized() throws Exception {
    Application app2 = tempEntity.newApplication("App2", "appPubId2", org.getId());
    grantReadPermission(app.getId());
    List<Application> applications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, Sets.newHashSet(app.getId(), app2.getId()), null);
    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId());
  }

  @Test()
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OnlySeesAppsWithPermission() throws Exception {
    Application app2 = tempEntity.newApplication("App2", "appPubId2", org.getId());
    grantReadPermission(app.getId());

    // request with nothing specified, should only see app1
    List<Application> applications =
        applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(null, null, null);
    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId());

    // now app2 permission and it should show up
    grantReadPermission(app2.getId());
    applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(null, null, null);
    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId(), app2.getId());
  }
}
