/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSecureSharingServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiSecureSharingService service;

  @Before
  public void before() {
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationsWithPermissions_Unauthenticated() {
    service.getApplicationsWithPermissions(Collections.emptySet(), 1, 10);
  }

  @Test
  public void testGetApplicationsWithPermissions_Unauthorized() {
    login();
    // Unauthorized access is allowed
    // Note that Authorized tests are in the corresponding service and resource class
    // This is because authorization is part of the service / dao methods instead of via annotations
    assertThat(service.getApplicationsWithPermissions(Collections.emptySet(), 1, 10)).isNotNull();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testExportSbom_Unauthenticated() {
    Response response = service.exportSbom(app.getId(), "sbomVersion", null);
    assertThat(response).isNotNull();
  }

  @Test(expected = UnauthorizedException.class)
  public void testExportSbom_Unauthorized() {
    login();
    service.exportSbom(app.getId(), "sbomVersion", null);
  }

  @Test(expected = NotFoundException.class)
  public void testExportSbom_Authorized() {
    grantPermission(app.getId(), Permission.EXPORT_SBOM);
    service.exportSbom(app.getId(), "sbomVersion", null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetSbomMetadataByApplication_Unauthenticated() {
    service.getSbomMetadataByApplication(app.getId(), 1, 10);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSbomMetadataByApplication_Unauthorized() {
    login();
    service.getSbomMetadataByApplication(app.getId(), 1, 10);
  }

  @Test(expected = NotFoundException.class)
  public void testGetSbomMetadataByApplication_Authorized() {
    grantPermission(app.getId(), Permission.EXPORT_SBOM);
    service.getSbomMetadataByApplication("otherID", 1, 10);
  }
}
