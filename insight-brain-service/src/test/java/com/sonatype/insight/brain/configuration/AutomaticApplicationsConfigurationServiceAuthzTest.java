/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class AutomaticApplicationsConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private AutomaticApplicationsConfigurationService service;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    // Need to mock this for telemetry requests, otherwise the real client takes a while to timeout.
    binder.bind(HdsClient.class).toInstance(mock(HdsClient.class));
  }

  @Test
  public void testUpdate_Authorized() {
    grantManageAutomaticApplicationCreationPermission();
    service.update(new AutomaticApplicationsConfiguration(true, org.getId()));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdate_Unauthorized() {
    login();
    service.update(new AutomaticApplicationsConfiguration(true, "testId"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdate_Unauthenticated() {
    service.update(new AutomaticApplicationsConfiguration(true, "testId"));
  }

  @Test
  public void testGet_Authorized() {
    grantManageAutomaticApplicationCreationPermission();
    service.get();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGet_Unauthorized() {
    login();
    service.get();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGet_Unauthenticated() {
    service.get();
  }
}
