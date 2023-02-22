/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.io.InputStream;

import com.sonatype.insight.brain.product.license.ProductLicenseService;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantLicenseServiceTest
    extends MultiTenantTestSupport
{
  public static final String TENANT_NAME = "test";

  public static final String LICENSE_FILE_NAME = "license.lic";

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  private ProductLicenseService licenseService;

  @Mock
  private InputStream inputStream;

  private TenantUtil tenantUtil;

  private TenantLicenseService underTest;

  @Before
  @Override
  public void setup() {
    super.setup();
    tenantUtil = new TenantUtil();
    underTest = new TenantLicenseService(tenantUtil, tenantValidator, licenseService);

    when(tenantValidator.validateTenantExists(TENANT_NAME)).thenReturn(true);
  }

  @Test
  public void shouldUpdateALicense() {
    testAsNewTenant(tenant -> {
      underTest.updateLicense(inputStream, LICENSE_FILE_NAME, TENANT_NAME);

      verify(licenseService).installLicenseNoAuthz(inputStream, LICENSE_FILE_NAME);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenTenantDoesntExist() {
    when(tenantValidator.validateTenantExists(TENANT_NAME)).thenReturn(false);

    testAsNewTenant(tenant -> {
      assertThatThrownBy(() -> underTest.updateLicense(inputStream, LICENSE_FILE_NAME, TENANT_NAME))
          .withFailMessage("Tenant doesn't exist")
          .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_whenUsingGlobalTenant() {
    testAs(Tenant.GLOBAL_TENANT, tenant -> {
      assertThatThrownBy(() -> underTest.updateLicense(inputStream, LICENSE_FILE_NAME, TENANT_NAME))
          .withFailMessage("Invalid tenant")
          .isInstanceOf(BadRequestException.class);
    });
  }
}
