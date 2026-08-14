/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.io.InputStream;

import com.sonatype.insight.brain.product.license.ProductLicenseService;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantLicenseServiceTest
    extends AbstractMultiTenantTest
{
  public static final String LICENSE_FILE_NAME = "license.lic";

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  private ProductLicenseService licenseService;

  @Mock
  private InputStream inputStream;

  private TenantUtil tenantUtil;

  private TenantLicenseService underTest;

  @BeforeEach
  public void setup() {
    tenantUtil = new TenantUtil();
    underTest = new TenantLicenseService(tenantUtil, tenantValidator, licenseService);
  }

  @Test
  public void shouldUpdateALicense() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      underTest.updateLicense(inputStream, LICENSE_FILE_NAME, tenant.tenantSlug);

      verify(licenseService).installLicenseNoAuthz(inputStream, LICENSE_FILE_NAME);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenTenantDoesntExist() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      assertThatThrownBy(() -> underTest.updateLicense(inputStream, LICENSE_FILE_NAME, tenant.tenantSlug))
          .withFailMessage("Tenant doesn't exist")
          .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenUsingGlobalTenant() {
    testAsGlobalTenant(tenant -> {
      assertThatThrownBy(() -> underTest.updateLicense(inputStream, LICENSE_FILE_NAME, tenant.tenantSlug))
          .withFailMessage("Invalid tenant")
          .isInstanceOf(BadRequestException.class);
    });
  }
}
