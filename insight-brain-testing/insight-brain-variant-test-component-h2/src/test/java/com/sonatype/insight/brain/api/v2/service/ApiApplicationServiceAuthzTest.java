/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoriesListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiApplicationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiApplicationService apiApplicationService;

  @Test
  public void testGetApplication_Authorized() {
    grantReadPermission(app.getId());
    apiApplicationService.getApplicationById(app.getId());
  }

  @Test
  public void testGetApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiApplicationService.getApplicationById(app.getId()));
  }

  @Test
  public void testGetApplication_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiApplicationService.getApplicationById(app.getId()));
  }

  @Test
  public void testGetApplicationsWithReadPermission_Authorized() {
    grantReadPermission(app.getId());
    List<Application> applications = apiApplicationService.getApplicationsWithReadPermission(Collections.emptySet());
    assertThat(applications).hasSize(1);
  }

  @Test
  public void testGetApplicationsWithReadPermission_Unauthenticated() {
    List<Application> applications = apiApplicationService.getApplicationsWithReadPermission(Collections.emptySet());
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplicationsWithReadPermission_Unauthorized() {
    login();
    List<Application> applications = apiApplicationService.getApplicationsWithReadPermission(Collections.emptySet());
    assertThat(applications).isEmpty();
  }

  @Test
  public void testAddApplication_Authorized() {
    grantAddApplicationPermission(org.getId());

    ApiApplicationDTO applicationDTO = createApplicationDTO();
    apiApplicationService.addApplication(applicationDTO);
  }

  @Test
  public void testAddApplication_Unauthenticated() {
    ApiApplicationDTO applicationDTO = createApplicationDTO();
    assertThrows(UnauthenticatedException.class, () -> apiApplicationService.addApplication(applicationDTO));
  }

  @Test
  public void testAddApplication_Unauthorized() {
    grantWritePermission(org.getId());
    ApiApplicationDTO applicationDTO = createApplicationDTO();
    assertThrows(UnauthorizedException.class, () -> apiApplicationService.addApplication(applicationDTO));
  }

  @Test
  public void testUpdateApplication_Authorized() {
    grantWritePermission(app.getId());
    ApiApplicationDTO applicationDTO = ApiApplicationAdapter.convertToDTO(app, Collections.emptyList());
    apiApplicationService.updateApplication(applicationDTO);
  }

  @Test
  public void testUpdateApplication_Unauthenticated() {
    ApiApplicationDTO applicationDTO = ApiApplicationAdapter.convertToDTO(app, Collections.emptyList());
    assertThrows(UnauthenticatedException.class, () -> apiApplicationService.updateApplication(applicationDTO));
  }

  @Test
  public void testUpdateApplication_Unauthorized() {
    login();
    ApiApplicationDTO applicationDTO = ApiApplicationAdapter.convertToDTO(app, Collections.emptyList());
    assertThrows(UnauthorizedException.class, () -> apiApplicationService.updateApplication(applicationDTO));
  }

  @Test
  public void testDeleteApplication_Authorized() throws Exception {
    grantWritePermission(app.getId());
    apiApplicationService.deleteApplication(app.getId());
  }

  @Test
  public void testDeleteApplication_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class, () -> apiApplicationService.deleteApplication(app.getId()));
  }

  @Test
  public void testDeleteApplication_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class, () -> apiApplicationService.deleteApplication(app.getId()));
  }

  @Test
  public void testGetApplicationsByOrganizationId_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiApplicationService.getApplicationsByOrganizationId(org.getId()));
  }

  @Test
  public void testGetApplicationsByOrganizationId_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiApplicationService.getApplicationsByOrganizationId(org.getId()));
  }

  @Test
  public void testGetApplicationsByOrganizationId_Authorized() {
    grantReadPermission(org.getId());
    apiApplicationService.getApplicationsByOrganizationId(org.getId());
  }

  @Test
  public void testGetApplicationsByOrganizationId_NotFound() {
    login();
    String organizationId = "doesNotExist";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiApplicationService.getApplicationsByOrganizationId(organizationId))
        .withMessageContaining("Organization with ID " + organizationId + " does not exist.");
  }

  @Test
  public void testGetApplicationsWithAppliedCategories_Unauthenticated() {
    ApiApplicationCategoriesListDTO result =
        apiApplicationService.getApplicationsWithAppliedCategories(Collections.emptySet());
    assertThat(result).isNotNull();
    assertThat(result.applications).isEmpty();
  }

  @Test
  public void testGetApplicationsWithAppliedCategories_Unauthorized() {
    login();
    ApiApplicationCategoriesListDTO result =
        apiApplicationService.getApplicationsWithAppliedCategories(Collections.emptySet());
    assertThat(result).isNotNull();
    assertThat(result.applications).isEmpty();
  }

  @Test
  public void testGetApplicationsWithAppliedCategories_Authorized() {
    grantReadPermission(app.getId());
    ApiApplicationCategoriesListDTO result =
        apiApplicationService.getApplicationsWithAppliedCategories(Collections.emptySet());
    assertThat(result).isNotNull();
    assertThat(result.applications).hasSize(1);
  }

  private ApiApplicationDTO createApplicationDTO() {
    ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = "applicationPublicId";
    applicationDTO.name = "applicationName";
    applicationDTO.organizationId = org.getId();
    return applicationDTO;
  }
}
