/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ProductLicenseServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ProductLicenseService productLicenseService;

  @Mock
  private TaskScheduler taskSchedulerMock;

  private InputStream getLicense() {
    return new ByteArrayInputStream(new byte[1]);
  }

  @Test
  public void testValidateLicense_Authenticated() {
    login();
    productLicenseService.validateLicense();
  }

  @Test
  public void testInstallLicense_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class /* the license is invalid */,
        () -> productLicenseService.installLicense(getLicense(), "test.lic"));
  }

  @Test
  public void testInstallLicense_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> productLicenseService.installLicense(getLicense(), "test.lic"));
  }

  @Test
  public void testInstallLicense_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> productLicenseService.installLicense(getLicense(), "test.lic"));
  }

  @Test
  public void testUninstallLicense_Authorized() {
    grantConfigureSystemPermission();
    productLicenseService.uninstallLicense();
  }

  @Test
  public void testUninstallLicense_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> productLicenseService.uninstallLicense());
  }

  @Test
  public void testUninstallLicense_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> productLicenseService.uninstallLicense());
  }

  @Test
  public void testGetLicenseInfo_Authorized() {
    grantConfigureSystemPermission();
    productLicenseService.getLicenseInfo();
  }

  @Test
  public void testGetLicenseInfo_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> productLicenseService.getLicenseInfo());
  }

  @Test
  public void testGetLicenseInfo_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> productLicenseService.getLicenseInfo());
  }
}
