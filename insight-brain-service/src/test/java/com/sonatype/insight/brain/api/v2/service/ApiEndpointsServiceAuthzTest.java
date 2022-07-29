/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;
import javax.ws.rs.core.Application;

import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ApiEndpointsServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiEndpointsService apiEndpointsService;

  @Mock
  private Application mockApplication;

  @Before
  public void before() {
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(true);
    ApiEndpointsService.OPEN_API_JSON_BY_API_TYPE.clear();
  }

  @After
  public void after() {
    ApiEndpointsService.OPEN_API_JSON_BY_API_TYPE.clear();
  }

  @Test
  public void testGetOpenAPI_Unauthenticated() {
    apiEndpointsService.getOpenAPI(mockApplication, ApiType.PUBLIC);
  }
}
