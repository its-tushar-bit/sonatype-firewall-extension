/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

public class ApiLicenseLegalServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiLicenseLegalService apiLicenseLegalService;

  @Mock
  private ComponentInfoService mockComponentInfoService;

  @Mock
  private ApiLicenseLegalHdsService mockApiLicenseLegalHdsService;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    Component component = new Component();
    component.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));
    lenient().when(mockComponentInfoService.augmentComponentDetails(any(), any())).thenReturn(component);
    binder.bind(ComponentInfoService.class).toInstance(mockComponentInfoService);
    lenient().when(mockApiLicenseLegalHdsService.getLicenseMetadata(any())).thenReturn(new ArrayList<>());
    lenient().when(mockApiLicenseLegalHdsService.getComponentLegalComments(any())).thenReturn(new HashSet<>());
    lenient().when(mockApiLicenseLegalHdsService.getComponentLegalFiles(any())).thenReturn(new HashSet<>());
    binder.bind(ApiLicenseLegalHdsService.class).toInstance(mockApiLicenseLegalHdsService);
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Unauthenticated() {
    setupResultForDashboard();
    assertThat(apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10))
        .isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Unauthorized() {
    setupResultForDashboard();
    login();
    assertThat(apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10))
        .isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Authorized() {
    setupResultForDashboard();
    grantLegalReviewerPermission(app.getId());
    assertThat(apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10))
        .isNotEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Unauthenticated() {
    setupResultForDashboard();
    assertThat(apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, null, null, null)).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Unauthorized() {
    setupResultForDashboard();
    login();
    assertThat(apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, null, null, null)).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Authorized() {
    setupResultForDashboard();
    grantLegalReviewerPermission(app.getId());
    assertThat(apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, null, null, null)).isNotEmpty();
  }

  private void setupResultForDashboard() {
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID,
        "hash", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "MIT");
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, tempEntity.uuid(), new Date());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicenseLegalApplicationReport_Unauthenticated() {
    apiLicenseLegalService.getLicenseLegalApplicationReport(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicenseLegalApplicationReport_Unauthorized() {
    login();
    apiLicenseLegalService.getLicenseLegalApplicationReport(app.getPublicId());
  }

  @Test(expected = NotFoundException.class)
  public void testGetLicenseLegalApplicationReport_Authorized() {
    grantLegalReviewerPermission(app.getId());
    apiLicenseLegalService.getLicenseLegalApplicationReport(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicenseLegalComponentReport_ApplicationUnauthenticated() throws Exception {
    apiLicenseLegalService.getLicenseLegalComponentReport(app.getType(), app.getPublicId(), ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicenseLegalComponentReport_ApplicationUnauthorized() throws Exception {
    login();
    apiLicenseLegalService.getLicenseLegalComponentReport(app.getType(), app.getPublicId(), ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null, null);
  }

  @Test
  public void testGetLicenseLegalComponentReport_ApplicationAuthorized() throws Exception {
    grantLegalReviewerPermission(app.getId());
    apiLicenseLegalService.getLicenseLegalComponentReport(app.getType(), app.getPublicId(), ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicenseLegalComponentReport_OrganizationUnauthenticated() throws Exception {
    apiLicenseLegalService.getLicenseLegalComponentReport(org.getType(), org.getPublicId(), ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicenseLegalComponentReport_OrganizationUnauthorized() throws Exception {
    login();
    apiLicenseLegalService.getLicenseLegalComponentReport(org.getType(), org.getPublicId(), ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null, null);
  }

  @Test
  public void testGetLicenseLegalComponentReport_OrganizationAuthorized() throws Exception {
    grantLegalReviewerPermission(org.getId());
    apiLicenseLegalService.getLicenseLegalComponentReport(org.getType(), org.getPublicId(), ComponentIdentifier
        .createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicenseLegalComponentReport_RootOrganizationUnauthenticated() throws Exception {
    apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicenseLegalComponentReport_RootOrganizationUnauthorized() throws Exception {
    login();
    apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null, null);
  }

  @Test
  public void testGetLicenseLegalComponentReport_RootOrganizationAuthorized() throws Exception {
    grantLegalReviewerPermission(Organization.ROOT_ORGANIZATION_ID);
    apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null, null);
  }
}
