/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
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

import static org.assertj.core.api.Assertions.assertThat;
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
  private ApiConfigurationService apiConfigurationService;

  TenantConfigurationService underTest;

  @Before
  @Override
  public void setup() {
    underTest = new TenantConfigurationService(apiConfigurationService, tenantUtil, tenantValidator);
  }

  @Test
  public void shouldGetSinglePropertyConfiguration() {
    testAsNewTenant(tenant -> {
      String expectedProperty = "baseUrl";
      Set<String> query = new HashSet<>(Arrays.asList(expectedProperty));
      Map<String, Object> expected = Collections.singletonMap(expectedProperty, "http://127.0.0.1:8070");

      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(apiConfigurationService.getConfigurationNoAuthz(query)).thenReturn(expected);

      assertThat(underTest.getPropertiesConfiguration(tenant.tenantSlug, query)).isEqualTo(expected);
    });
  }

  @Test
  public void shouldNotThrowRuntimeException_getPropertiesConfiguration_whenUsingGlobalTenant() {
    testAsGlobalTenant(global -> {
      when(tenantUtil.isGlobalTenant()).thenReturn(true);

      String expectedProperty = "baseUrl";
      Set<String> query = new HashSet<>(Arrays.asList(expectedProperty));
      Map<String, Object> expected = Collections.singletonMap(expectedProperty, "http://127.0.0.1:8070");

      when(apiConfigurationService.getConfigurationNoAuthz(query)).thenReturn(expected);

      assertThat(underTest.getPropertiesConfiguration(global.tenantSlug, query)).isEqualTo(expected);
    });
  }

  @Test
  public void shouldThrowRuntimeException_getPropertiesConfiguration_whenTenantDoesntExist() {
    final String errorMessage = "Tenant does not exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      Set<String> query = new HashSet<>(Arrays.asList("baseUrl"));

      assertThatThrownBy(() -> underTest.getPropertiesConfiguration(tenant.tenantSlug, query))
          .withFailMessage(errorMessage)
          .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_getPropertiesConfiguration_whenPropertyIsNotConfigurable() {
    final String errorMessage = "Property forceBaseUrl is not configurable.";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      Set<String> query = new HashSet<>(Arrays.asList("forceBaseUrl"));

      assertThatThrownBy(() -> underTest.getPropertiesConfiguration(tenant.tenantSlug, query))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldSetSinglePropertyConfiguration() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      String expectedProperty = "baseUrl";
      String expectedValue = "http://127.0.0.1:8070";
      Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

      underTest.setPropertiesConfiguration(tenant.tenantSlug, propertyConfiguration);

      verify(apiConfigurationService).setConfigurationNoAuthz(propertyConfiguration);
    });
  }

  @Test
  public void shouldNotThrowRuntimeException_setPropertiesConfiguration_whenUsingGlobalTenant() {
    testAsGlobalTenant(global -> {
      when(tenantUtil.isGlobalTenant()).thenReturn(true);

      String expectedProperty = "baseUrl";
      String expectedValue = "http://127.0.0.1:8070";
      Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

      underTest.setPropertiesConfiguration(global.tenantSlug, propertyConfiguration);

      verify(apiConfigurationService).setConfigurationNoAuthz(propertyConfiguration);
    });
  }

  @Test
  public void shouldThrowRuntimeException_setPropertiesConfiguration_whenTenantDoesntExist() {
    final String errorMessage = "Tenant does not exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      String expectedProperty = "baseUrl";
      String expectedValue = "http://127.0.0.1:8070";
      Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(tenant.tenantSlug, propertyConfiguration))
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

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(tenant.tenantSlug, propertyConfiguration))
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

      assertThatThrownBy(() -> underTest.setPropertiesConfiguration(tenant.tenantSlug, propertyConfiguration))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldDeleteSinglePropertyConfiguration() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      Set<String> query = new HashSet<>(Arrays.asList("baseUrl"));
      underTest.deletePropertiesConfiguration(tenant.tenantSlug, query);

      verify(apiConfigurationService).deleteConfigurationNoAuthz(query);
    });
  }

  @Test
  public void shouldNotThrowRuntimeException_deletePropertiesConfiguration_whenUsingGlobalTenant() {
    testAsGlobalTenant(global -> {
      when(tenantUtil.isGlobalTenant()).thenReturn(true);

      Set<String> query = new HashSet<>(Arrays.asList("baseUrl"));
      underTest.deletePropertiesConfiguration(global.tenantSlug, query);

      verify(apiConfigurationService).deleteConfigurationNoAuthz(query);
    });
  }

  @Test
  public void shouldThrowRuntimeException_deletePropertiesConfiguration_whenTenantDoesntExist() {
    final String errorMessage = "Tenant does not exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      Set<String> query = new HashSet<>(Arrays.asList("baseUrl"));

      assertThatThrownBy(() -> underTest.deletePropertiesConfiguration(tenant.tenantSlug, query))
          .withFailMessage(errorMessage)
          .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldThrowRuntimeException_deletePropertiesConfiguration_whenPropertyIsNotConfigurable() {
    final String errorMessage = "Property forceBaseUrl is not configurable.";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      Set<String> query = new HashSet<>(Arrays.asList("forceBaseUrl"));

      assertThatThrownBy(() -> underTest.deletePropertiesConfiguration(tenant.tenantSlug, query))
          .withFailMessage(errorMessage)
          .isInstanceOf(BadRequestException.class);
    });
  }
}
