/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Application;

import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;

public class ApiEndpointsServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiEndpointsService apiEndpointsService;

  @Mock
  private Application mockApplication;

  @Before
  public void before() {
    ApiEndpointsService.clearCaches();
  }

  @After
  public void after() {
    ApiEndpointsService.clearCaches();
  }

  @Test
  public void testGetOpenAPI_Unauthenticated_EnableUnauthenticatedPagesDisabled() {
    SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.setEnabled(false);

    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC))
        .withMessageContaining(
            "Anonymous access requires ENABLE_UNAUTHENTICATED_PAGES to be enabled.");

    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiEndpointsService.getOpenAPI(mockApplication, ApiType.EXPERIMENTAL))
        .withMessageContaining(
            "Anonymous access requires ENABLE_UNAUTHENTICATED_PAGES to be enabled.");
  }

  @Test
  public void testGetOpenAPI_Unauthenticated_EnableUnauthenticatedPagesEnabled() {
    assertThatNoException().isThrownBy(() -> apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC));
    assertThatNoException().isThrownBy(() -> apiEndpointsService.getOpenAPI(mockApplication, ApiType.EXPERIMENTAL));
  }
}
