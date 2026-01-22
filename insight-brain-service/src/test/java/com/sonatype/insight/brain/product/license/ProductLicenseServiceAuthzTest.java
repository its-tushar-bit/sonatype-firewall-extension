/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

public class ProductLicenseServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ProductLicenseService productLicenseService;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }

  private InputStream getLicense() {
    return new ByteArrayInputStream(new byte[1]);
  }

  @Test
  public void testValidateLicense_Authenticated() {
    login();
    productLicenseService.validateLicense();
  }

  @Test(expected = BadRequestException.class /* the license is invalid */)
  public void testInstallLicense_Authorized() {
    grantConfigureSystemPermission();
    productLicenseService.installLicense(getLicense(), "test.lic");
  }

  @Test(expected = UnauthorizedException.class)
  public void testInstallLicense_Unauthorized() {
    login();
    productLicenseService.installLicense(getLicense(), "test.lic");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testInstallLicense_Unauthenticated() {
    productLicenseService.installLicense(getLicense(), "test.lic");
  }

  @Test
  public void testUninstallLicense_Authorized() {
    grantConfigureSystemPermission();
    productLicenseService.uninstallLicense();
  }

  @Test(expected = UnauthorizedException.class)
  public void testUninstallLicense_Unauthorized() {
    login();
    productLicenseService.uninstallLicense();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUninstallLicense_Unauthenticated() {
    productLicenseService.uninstallLicense();
  }

  @Test
  public void testGetLicenseInfo_Authorized() {
    grantConfigureSystemPermission();
    productLicenseService.getLicenseInfo();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicenseInfo_Unauthorized() {
    login();
    productLicenseService.getLicenseInfo();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicenseInfo_Unauthenticated() {
    productLicenseService.getLicenseInfo();
  }
}
