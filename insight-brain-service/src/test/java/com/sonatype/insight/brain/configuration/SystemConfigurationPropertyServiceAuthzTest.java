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
import org.junit.Before;
import org.junit.Test;

public class SystemConfigurationPropertyServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String PROPERTY_NAME = "TEST-NAME";

  @Inject
  private SystemConfigurationPropertyService service;

  @Before
  public void init() {
    tempEntity.newSystemConfigurationProperty(PROPERTY_NAME, "TEST-VALUE");
  }

  @Test
  public void testGetByName_Unauthenticated() throws Exception {
    service.getByName(PROPERTY_NAME);
  }

  @Test
  public void testGetByName_Unauthorized() throws Exception {
    login();

    service.getByName(PROPERTY_NAME);
  }

  @Test
  public void testGetByName_Authorized() throws Exception {
    grantConfigureSystemPermission();

    service.getByName(PROPERTY_NAME);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdate_Unauthorized() {
    login();

    service.update(new SystemConfigurationProperty(PROPERTY_NAME, "false"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdate_Unauthenticated() {
    service.update(new SystemConfigurationProperty(PROPERTY_NAME, "false"));
  }

  @Test
  public void testUpdate_Authorized() {
    grantConfigureSystemPermission();
    service.update(new SystemConfigurationProperty(PROPERTY_NAME, "false"));
  }
}
