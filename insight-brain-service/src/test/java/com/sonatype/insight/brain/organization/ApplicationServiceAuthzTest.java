/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
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

  @Test
  public void testDeleteApplicationById_DockerProxyAuthorized() throws Exception {
    Organization organization = tempEntity.newOrgWithRepoManagerAndProxyRepo(
        "My Organization",
        "dockerOrg",
        "docker",
        true,
        true);
    Application application = tempEntity.newApplication(organization.getId());
    grantEvaluateComponentPermission(application.getId());
    applicationService.deleteApplicationByPublicId(application.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteApplicationByPublicId_Unauthenticated() throws Exception {
    applicationService.deleteApplicationByPublicId(app.getPublicId());
  }

  @Test
  public void testGetAllApplications_Authorized() {
    grantReadPermission(app.getId());
    final List<Application> applications = applicationService.getApplications();
    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId());
  }

  @Test
  public void testGetAllApplications_Unauthenticated() {
    List<Application> applications = applicationService.getApplications();
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplicationsWithoutRelatedRepositoriesOrderedByName_Authorized() {
    grantReadPermission(app.getId());

    // Create an app with both a related repository manager and repository
    Organization orgWithRelatedRepo = tempEntity.newOrganizationWithRepositoryManager("org-with-repo");
    Application containerApplication = tempEntity.newApplication(orgWithRelatedRepo.getId());
    grantReadPermission(containerApplication.getId());

    final List<Application> applications = applicationService.getApplicationsWithoutRelatedRepositoriesOrderedByName();
    assertThat(applications).extracting(Application::getId).containsExactly(app.getId());
  }

  @Test
  public void testGetApplicationsWithoutRelatedRepositoriesOrderedByName_Unauthenticated() {
    List<Application> applications = applicationService.getApplicationsWithoutRelatedRepositoriesOrderedByName();
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplicationsWithoutRelatedRepositoriesOrderedByName_Unauthorized() {
    login();
    List<Application> applications = applicationService.getApplicationsWithoutRelatedRepositoriesOrderedByName();
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetAllWithoutRelatedRepositories_Authorized() {
    grantReadPermission(app.getId());
    final List<Application> applications = applicationService.getAllWithoutRelatedRepositories();
    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId());
  }

  @Test
  public void testGetAllWithoutRelatedRepositories_Unauthenticated() {
    List<Application> applications = applicationService.getAllWithoutRelatedRepositories();
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetAllWithoutRelatedRepositories_Unauthorized() {
    login();
    List<Application> applications = applicationService.getAllWithoutRelatedRepositories();
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetAllWithoutRelatedRepositoriesWithReadPermission() {
    grantReadPermission(app.getId());
    Application newApp = tempEntity.newApplication(org.getId());
    Organization organization = tempEntity.newOrganizationWithRepositoryManager("My Organization");
    Application dockerApp = tempEntity.newApplication(organization.getId());

    List<Application> applications = applicationService.getAllWithoutRelatedRepositories();

    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId());
    assertThat(applications).extracting(Application::getName).doesNotContain(dockerApp.getName());

    grantReadPermission(newApp.getId());
    grantReadPermission(dockerApp.getId());
    applications = applicationService.getAllWithoutRelatedRepositories();
    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId(), newApp.getId());
    assertThat(applications).extracting(Application::getName).doesNotContain(dockerApp.getName());
  }

  @Test
  public void testGetApplicationsAndCheckIfAll_Authorized() {
    grantReadPermission(app.getId());
    Pair<List<Application>, Boolean> result = applicationService.getApplicationsAndCheckIfAll();

    assertThat(result).isNotNull();
    assertThat(result.getLeft()).extracting(Application::getId).containsExactly(app.getId());
    assertThat(result.getRight()).isTrue();

    // New application where the user has no permission
    tempEntity.newApplicationWithParent();

    result = applicationService.getApplicationsAndCheckIfAll();

    assertThat(result).isNotNull();
    assertThat(result.getLeft()).extracting(Application::getId).containsExactly(app.getId());
    assertThat(result.getRight()).isFalse();
  }

  @Test
  public void testGetApplicationsAndCheckIfAll_Unauthorized() {
    login();
    Pair<List<Application>, Boolean> result = applicationService.getApplicationsAndCheckIfAll();

    assertThat(result).isNotNull();
    assertThat(result.getLeft()).isEmpty();
    assertThat(result.getRight()).isFalse();
  }

  @Test
  public void testGetApplicationsAndCheckIfAll_Unauthenticated() {
    Pair<List<Application>, Boolean> result = applicationService.getApplicationsAndCheckIfAll();

    assertThat(result).isNotNull();
    assertThat(result.getLeft()).isEmpty();
    assertThat(result.getRight()).isFalse();
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_Authorized() {
    tempEntity.newApplication(app.getOrganizationId());

    grantEvaluateComponentPermission(app.getId());
    Map<String, String> applicationNames = applicationService.getApplicationNamesForEvaluateComponent();
    assertThat(applicationNames).hasSize(1).containsEntry(app.getPublicId(), app.getName());
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_Unauthorized() {
    login();
    Map<String, String> applicationNames = applicationService.getApplicationNamesForEvaluateComponent();
    assertThat(applicationNames).isEmpty();
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_Unauthenticated() {
    Map<String, String> applicationNames = applicationService.getApplicationNamesForEvaluateComponent();
    assertThat(applicationNames).isEmpty();
  }

  @Test
  public void testValidateApplicationPublicId_Authorized() {
    grantWritePermission(app.getId());
    String value = applicationService.validateApplicationPublicId(app.getPublicId());
    assertThat(value).isEqualTo("OK");
  }

  @Test(expected = UnauthorizedException.class)
  public void testValidateApplicationPublicId_Unauthorized() {
    login();
    applicationService.validateApplicationPublicId(app.getPublicId());
  }

  @Test
  public void testGetApplicationByPublicIdForRead_Authorized() {
    grantReadPermission(app.getId());

    applicationService.getApplicationByPublicIdForRead(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationByPublicIdForLegalReviewer_Unauthenticated() {
    applicationService.getApplicationByPublicIdForLegalReviewer(app.getPublicId());
  }

  @Test
  public void testGetApplicationByPublicIdForLegalReviewer_Authorized() {
    grantPermission(app.getId(), Permission.LEGAL_REVIEWER);

    applicationService.getApplicationByPublicIdForLegalReviewer(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationByPublicIdForRead_Unauthenticated() {
    applicationService.getApplicationByPublicIdForRead(app.getPublicId());
  }

  @Test
  public void testGetApplicationByPublicIdNotNull_Authorized() {
    grantReadPermission(app.getId());

    applicationService.getApplicationByPublicIdNotNull(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationByPublicIdNotNull_Unauthenticated() {
    applicationService.getApplicationByPublicIdNotNull(app.getPublicId());
  }

  @Test
  public void testUpdateApplication_Authorized() {
    grantWritePermission(app.getId());

    String newName = "TestUpdateName";
    app.setName(newName);
    applicationService.updateApplication(app);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateApplication_Unauthenticated() {
    app.setName("TestUpdateName");
    applicationService.updateApplication(app);
  }

  @Test
  public void testGetApplicationsByPublicIdsAndTagIds_Authorized_byApp() {
    grantReadPermission(app.getId());
    final List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(null,
        Sets.newHashSet(app.getId()), null);
    assertThat(applications).hasSize(1);
  }

  @Test
  public void testGetApplicationsByPublicIdsAndTagIds_Authorized_byOrg() {
    grantReadPermission(app.getId());
    final List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Sets.newHashSet(app.getParentOwnerId()), null, null);
    assertThat(applications).hasSize(1);
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_FilteredAuthorized() {
    grantReadPermission(app.getId());
    List<Application> applications =
        applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(null, null, null);
    assertThat(applications).hasSize(1).extracting(Application::getId).containsExactlyInAnyOrder(app.getId());
  }

  @Test()
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_TwoAppsAuthorized() {
    Application app2 = tempEntity.newApplication("App2", "appPubId2", org.getId());
    grantReadPermission(app.getId());
    grantReadPermission(app2.getId());

    final List<Application> applications = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        null, Sets.newHashSet(app.getId(), app2.getId()), null);
    assertThat(applications).hasSize(2);
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgAuthorized() {
    Application app2 = tempEntity.newApplication("App2", "appPubId2", org.getId());
    grantReadPermission(app.getId());
    grantReadPermission(app2.getId());

    final List<Application> applications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(Sets.newHashSet(app.getParentOwnerId()), null, null);
    assertThat(applications).hasSize(2);
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_Unauthenticated() {
    List<Application> applications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, Sets.newHashSet(app.getId()), null);
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_NotAuthorized() {
    login();
    List<Application> applications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, Sets.newHashSet(app.getId()), null);
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_withOrg_NotAuthorized() {
    login();
    List<Application> applications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(Sets.newHashSet(app.getParentOwnerId()), null, null);
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_TwoAppsOneNotAuthorized() {
    Application app2 = tempEntity.newApplication("App2", "appPubId2", org.getId());
    grantReadPermission(app.getId());
    List<Application> applications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, Sets.newHashSet(app.getId(), app2.getId()), null);
    assertThat(applications).extracting(Application::getId).containsExactlyInAnyOrder(app.getId());
  }

  @Test()
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OnlySeesAppsWithPermission() {
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
