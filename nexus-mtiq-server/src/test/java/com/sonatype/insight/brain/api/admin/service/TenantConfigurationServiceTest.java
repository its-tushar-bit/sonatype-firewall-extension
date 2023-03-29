/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.Collections;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantConfigurationServiceTest
    extends MultiTenantTestSupport
{
  @Mock
  private TenantUtil tenantUtil;

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  TenantConfigurationService underTest;

  @Before
  @Override
  public void setup() {
    underTest = new TenantConfigurationService(tenantUtil, tenantValidator, systemConfigurationPropertyDAO);
  }

  @Test
  public void shouldSetSinglePropertyConfiguration() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      String expectedProperty = "baseUrl";
      String expectedValue = "http://127.0.0.1:8070";
      Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

      underTest.setPropertiesConfiguration(propertyConfiguration, tenant.tenantSlug);

      verify(systemConfigurationPropertyDAO).set(expectedProperty, expectedValue + "/");
    });
  }

  @Test
  public void shouldThrowRuntimeException_setPropertiesConfiguration_whenUsingGlobalTenant() {
    final String errorMessage = "Invalid tenant";

    testAsGlobalTenant(global -> {
      when(tenantUtil.isGlobalTenant()).thenReturn(true);

      String expectedProperty = "baseUrl";
      String expectedValue = "http://127.0.0.1:8070";
      Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(propertyConfiguration, global.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_setPropertiesConfiguration_whenTenantDoesntExist() {
    final String errorMessage = "Tenant doesn't exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      String expectedProperty = "baseUrl";
      String expectedValue = "http://127.0.0.1:8070";
      Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(propertyConfiguration, tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_setPropertiesConfiguration_whenNoConfigurationProvided() {
    final String errorMessage = "No configuration was specified.";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      Map<String, Object> propertyConfiguration = Collections.emptyMap();

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(propertyConfiguration, tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_setPropertiesConfiguration_whenPropertyIsNotConfigurable() {
    final String errorMessage = "Property forceBaseUrl is not configurable.";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      String expectedProperty = "forceBaseUrl";
      boolean expectedValue = true;
      Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(propertyConfiguration, tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_setPropertiesConfiguration_whenPropertyHasInvalidValue() {
    final String errorMessage =
        "Invalid value for baseUrl, expected class java.lang.String, but got class java.lang.Boolean.";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      String expectedProperty = "baseUrl";
      boolean expectedValue = true;
      Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(propertyConfiguration, tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }
}
