/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.InputStream;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ProductLicenseServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ProductLicenseService productLicenseService;

  private InputStream getLicense() {
    return getClass().getResourceAsStream("/productlicense/license.lic");
  }

  @Test
  public void testValidateLicense_Authenticated() {
    login();
    productLicenseService.validateLicense();
  }

  @Test
  public void testInstallLicense_Authorized() {
    grantConfigureSystemPermission();
    productLicenseService.installLicense(getLicense());
  }

  @Test(expected = UnauthorizedException.class)
  public void testInstallLicense_Unauthorized() {
    login();
    productLicenseService.installLicense(getLicense());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testInstallLicense_Unauthenticated() {
    productLicenseService.installLicense(getLicense());
  }

  @Test
  public void testUninstallLicense_Authorized() throws Exception {
    grantConfigureSystemPermission();
    productLicenseService.uninstallLicense();
  }

  @Test(expected = UnauthorizedException.class)
  public void testUninstallLicense_Unauthorized() throws Exception {
    login();
    productLicenseService.uninstallLicense();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUninstallLicense_Unauthenticated() throws Exception {
    productLicenseService.uninstallLicense();
  }
}
