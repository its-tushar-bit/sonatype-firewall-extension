/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ApiApplicationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiApplicationService apiApplicationService;

  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  @Test
  public void testGetApplication_Authorized() {
    grantReadPermission(app.getId());
    apiApplicationService.getApplication(app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplication_Unauthenticated() {
    apiApplicationService.getApplication(app.getId());
  }

  @Test
  public void testAddApplication_Authorized() {
    grantWritePermission(org.getId());

    final Application application = new Application();
    application.setName("My Application");
    application.setOrganizationId(org.getId());
    application.setPublicId("MyApp");

    // Test the add application
    final ApiApplicationDTO applicationDTO = apiApplicationService.addApplication(application);

    // Now clean up by deleting the application
    application.setId(applicationDTO.getId());
    applicationDAO.delete(application);
  }

  @Test
  public void testAddApplication_Unauthenticated() {
    final Application application = new Application();
    try {
      apiApplicationService.addApplication(application);
      fail("Expected UnauthenticatedException");
    }
    catch (UnauthenticatedException ignore) {
      // Properly thrown exception.
    }
  }

  @Test
  public void testDeleteApplicationAuthorized() throws Exception {
    grantWritePermission(app.getId());
    apiApplicationService.deleteApplication(app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteApplication_Unauthenticated() throws Exception {
    apiApplicationService.deleteApplication(app.getId());
  }
}
