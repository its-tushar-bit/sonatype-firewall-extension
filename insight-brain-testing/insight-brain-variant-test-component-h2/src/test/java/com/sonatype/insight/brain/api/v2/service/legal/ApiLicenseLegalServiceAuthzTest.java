/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import java.util.Date;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ApiLicenseLegalServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiLicenseLegalService apiLicenseLegalService;

  @Mock
  private ComponentInfoService mockComponentInfoService;

  @Mock
  private ApiLicenseLegalHdsService mockApiLicenseLegalHdsService;

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Unauthenticated() {
    setupResultForDashboard();

    ApiLicenseLegalApplicationDashboardResultDTO dto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10);
    assertThat(dto).isNotNull();
    assertThat(dto.results).isEmpty();
    assertThat(dto.totalResultsCount).isZero();
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Unauthorized() {
    setupResultForDashboard();
    login();

    ApiLicenseLegalApplicationDashboardResultDTO dto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10);
    assertThat(dto).isNotNull();
    assertThat(dto.results).isEmpty();
    assertThat(dto.totalResultsCount).isZero();
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Authorized() {
    setupResultForDashboard();
    grantLegalReviewerPermission(app.getId());

    ApiLicenseLegalApplicationDashboardResultDTO dto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10);
    assertThat(dto).isNotNull();
    assertThat(dto.results).isNotEmpty();
    assertThat(dto.totalResultsCount).isNotZero();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Unauthenticated() {
    setupResultForDashboard();
    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto).isNotNull();
    assertThat(resultDto.totalResultsCount).isZero();
    assertThat(resultDto.results).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Unauthorized() {
    setupResultForDashboard();
    login();
    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto).isNotNull();
    assertThat(resultDto.totalResultsCount).isZero();
    assertThat(resultDto.results).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Authorized() {
    setupResultForDashboard();
    grantLegalReviewerPermission(app.getId());
    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto).isNotNull();
    assertThat(resultDto.totalResultsCount).isPositive();
    assertThat(resultDto.results).isNotEmpty();
  }

  private void setupResultForDashboard() {
    OwnerComponent applicationComponent = tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID,
        "hash", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "MIT");
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid(), new Date());
  }

  @Test
  public void testGetLicenseLegalApplicationReport_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport(app));
  }

  @Test
  public void testGetLicenseLegalApplicationReport_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport(app));
  }

  @Test
  public void testGetLicenseLegalApplicationReport_Authorized() {
    grantLegalReviewerPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport(app));
  }

  @Test
  public void testGetLicenseLegalApplicationReportByStage_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport(app, BuildStageType.ID, false, false));
  }

  @Test
  public void testGetLicenseLegalApplicationReportByStage_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport(app, BuildStageType.ID, false, false));
  }

  @Test
  public void testGetLicenseLegalApplicationReportByStage_Authorized() {
    grantLegalReviewerPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport(app, BuildStageType.ID, false, false));
  }

  @Test
  public void testGetApiReportRawDataForMultiApplicationReport() {
    grantLegalReviewerPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> apiLicenseLegalService.getApiReportRawDataForMultiApplicationReport(app, BuildStageType.ID));
  }

  @Test
  public void testGetLicenseLegalComponentReport_ApplicationUnauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiLicenseLegalService.getLicenseLegalComponentReport(app.getType(), app.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_ApplicationUnauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiLicenseLegalService.getLicenseLegalComponentReport(app.getType(), app.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_ApplicationAuthorized() throws Exception {
    doThrow(new NotFoundException("Component not found"))
        .when(mockComponentInfoService)
        .getUnaugmentedComponentDetails(any(), any(), any(), any(), any());

    grantLegalReviewerPermission(app.getId());
    assertThrows(NotFoundException.class,
        () -> apiLicenseLegalService.getLicenseLegalComponentReport(app.getType(), app.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_OrganizationUnauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiLicenseLegalService.getLicenseLegalComponentReport(org.getType(), org.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_OrganizationUnauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiLicenseLegalService.getLicenseLegalComponentReport(org.getType(), org.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_OrganizationAuthorized() throws Exception {
    doThrow(new NotFoundException("Component not found"))
        .when(mockComponentInfoService)
        .getUnaugmentedComponentDetails(any(), any(), any(), any(), any());

    grantLegalReviewerPermission(org.getId());
    assertThrows(NotFoundException.class,
        () -> apiLicenseLegalService.getLicenseLegalComponentReport(org.getType(), org.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_RootOrganizationUnauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID,
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_RootOrganizationUnauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID,
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_RootOrganizationAuthorized() throws Exception {
    doThrow(new NotFoundException("Component not found"))
        .when(mockComponentInfoService)
        .getUnaugmentedComponentDetails(any(), any(), any(), any(), any());

    grantLegalReviewerPermission(Organization.ROOT_ORGANIZATION_ID);
    assertThrows(NotFoundException.class,
        () -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.ORGANIZATION,
            Organization.ROOT_ORGANIZATION_ID,
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null, null, null, null));
  }
}
