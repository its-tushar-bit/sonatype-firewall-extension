/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class SystemConfigurationPropertyServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SystemConfigurationPropertyService service;

  @Test
  public void testGetByName_Unauthenticated() throws Exception {
    service.getByName("SUCCESS_METRICS_ENABLED");
  }

  @Test
  public void testGetByName_Unauthorized() throws Exception {
    login();

    service.getByName("SUCCESS_METRICS_ENABLED");
  }

  @Test
  public void testGetByName_Authorized() throws Exception {
    grantConfigureSystemPermission();

    service.getByName("SUCCESS_METRICS_ENABLED");
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdate_Unauthorized() {
    login();

    service.update(new SystemConfigurationProperty("SUCCESS_METRICS_ENABLED", "false"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdate_Unauthenticated() {
    service.update(new SystemConfigurationProperty("SUCCESS_METRICS_ENABLED", "false"));
  }

  @Test
  public void testUpdate_Authorized() {
    grantConfigureSystemPermission();

    try {
      service.update(new SystemConfigurationProperty("SUCCESS_METRICS_ENABLED", "false"));
    }
    finally {
      service.update(new SystemConfigurationProperty("SUCCESS_METRICS_ENABLED", "true"));
    }
  }
}
