/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;


public class ApiApplicationServiceAuthzTest
    extends AbstractServiceAuthzTest
{

  @Inject
  private ApiApplicationService apiApplicationService;

  private ApplicationDAO applicationDAO = new ApplicationDAO();


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
  public void testAddApplication_Authorized() {
    grantWritePermission(org.getId());

    ApiApplicationDTO applicationDTO = createApplicationDTO();
    applicationDTO = apiApplicationService.addApplication(applicationDTO);

    // Now clean up by deleting the application
    tempEntity.register(applicationDAO.getByIdNotNull(applicationDTO.id));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddApplication_Unauthenticated() {
    ApiApplicationDTO applicationDTO = createApplicationDTO();
    apiApplicationService.addApplication(applicationDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddApplication_UnauthorizedButAuthenticated() {
    login();
    ApiApplicationDTO applicationDTO = createApplicationDTO();
    apiApplicationService.addApplication(applicationDTO);
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
