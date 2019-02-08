/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiApplicationServiceAuthzTest
    extends AbstractServiceAuthzTest
{

  @Inject
  private ApiApplicationService apiApplicationService;

  private ApiApplicationAdapter apiApplicationAdapter = new ApiApplicationAdapter();

  @Test
  public void testGetApplication_Authorized() {
    grantReadPermission(app.getId());
    apiApplicationService.getApplicationById(app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplication_Unauthenticated() {
    apiApplicationService.getApplicationById(app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplication_UnauthorizedButAuthenticated() {
    login();
    apiApplicationService.getApplicationById(app.getId());
  }

  @Test
  public void testGetApplications_Authorized() {
    grantReadPermission(app.getId());
    List<Application> applications = apiApplicationService.getApplications(Collections.emptySet());
    assertThat(applications).hasSize(1);
  }

  @Test
  public void testGetApplications_Unauthenticated() {
    List<Application> applications = apiApplicationService.getApplications(Collections.emptySet());
    assertThat(applications).isEmpty();
  }

  @Test
  public void testGetApplications_UnauthorizedButAuthenticated() {
    login();
    List<Application> applications = apiApplicationService.getApplications(Collections.emptySet());
    assertThat(applications).isEmpty();
  }

  @Test
  public void testAddApplication_Authorized() {
    grantAddApplicationPermission(org.getId());

    ApiApplicationDTO applicationDTO = createApplicationDTO();
    apiApplicationService.addApplication(applicationDTO);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddApplication_Unauthenticated() {
    ApiApplicationDTO applicationDTO = createApplicationDTO();
    apiApplicationService.addApplication(applicationDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddApplication_UnauthorizedButAuthenticated() {
    grantWritePermission(org.getId());
    ApiApplicationDTO applicationDTO = createApplicationDTO();
    apiApplicationService.addApplication(applicationDTO);
  }

  @Test
  public void testUpdateApplication_Authorized() {
    grantWritePermission(app.getId());
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    apiApplicationService.updateApplication(applicationDTO);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateApplication_Unauthenticated() {
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    apiApplicationService.updateApplication(applicationDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateApplication_UnauthorizedButAuthenticated() {
    login();
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    apiApplicationService.updateApplication(applicationDTO);
  }

  @Test
  public void testDeleteApplication_Authorized() throws Exception {
    grantWritePermission(app.getId());
    apiApplicationService.deleteApplication(app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteApplication_Unauthenticated() throws Exception {
    apiApplicationService.deleteApplication(app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteApplication_UnauthorizedButAuthenticated() throws Exception {
    login();
    apiApplicationService.deleteApplication(app.getId());
  }

  private ApiApplicationDTO createApplicationDTO() {
    ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = "applicationPublicId";
    applicationDTO.name = "applicationName";
    applicationDTO.organizationId = org.getId();
    return applicationDTO;
  }
}
