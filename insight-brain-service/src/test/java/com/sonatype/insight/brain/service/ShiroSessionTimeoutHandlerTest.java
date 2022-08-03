/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collections;
import java.util.Map;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;

import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ShiroSessionTimeoutHandlerTest
    extends AbstractBrainServiceTest
{
  public static final String SESSION_TIMEOUT_MINUTES = "sessionTimeout";

  @Mock
  private ApiConfigurationService configurationService;

  @Mock
  private DefaultWebSessionManager defaultWebSessionManager;

  ShiroSessionTimeoutHandler shiroSessionTimeoutHandler;

  @Before
  public void setUp() throws Exception {
    openMocks(this);
    shiroSessionTimeoutHandler = new ShiroSessionTimeoutHandler(configurationService, defaultWebSessionManager);
  }

  @Test
  public void configurationChanged_withWrongConfig() {
    shiroSessionTimeoutHandler.configurationChanged(Collections.singleton("wrongConfig"));
    verify(defaultWebSessionManager, never()).setGlobalSessionTimeout(anyLong());
  }

  @Test
  public void configurationChanged_withCorrectConfig() {
    Map<String, Object> stringObjectMap = Collections.singletonMap(SESSION_TIMEOUT_MINUTES, 10);
    when(configurationService.getConfigurationNoAuthz(Collections.singleton(SESSION_TIMEOUT_MINUTES))).thenReturn(
        stringObjectMap);
    shiroSessionTimeoutHandler.configurationChanged(Collections.singleton(SESSION_TIMEOUT_MINUTES));
    verify(defaultWebSessionManager).setGlobalSessionTimeout(anyLong());
  }
}
