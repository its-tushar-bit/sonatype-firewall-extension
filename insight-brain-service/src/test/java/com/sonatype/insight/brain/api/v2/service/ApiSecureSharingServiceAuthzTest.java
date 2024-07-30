/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSecureSharingServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiSecureSharingService service;

  @Before
  public void before() {
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationsWithPermissions_Unauthenticated() {
    service.getApplicationsWithPermissions(Collections.emptySet(), 1, 10);
  }

  @Test
  public void testGetApplicationsWithPermissions_Unauthorized() {
    login();
    // Unauthorized access is allowed
    // Note that Authorized tests are in the corresponding service and resource class
    // This is because authorization is part of the service / dao methods instead of via annotations
    assertThat(service.getApplicationsWithPermissions(Collections.emptySet(), 1, 10)).isNotNull();
  }
}
