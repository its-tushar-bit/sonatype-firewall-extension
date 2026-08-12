/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Authorization tests for ApiLifecycleResource.
 * <p>
 * Access to the Hosted Repository Scanning configuration is restricted to:
 * <ul>
 * <li>System Administrator — via {@code CONFIGURE_SYSTEM} permission at the global context.</li>
 * <li>Policy Administrator, Owner, Developer (and any custom role granted {@code READ}) —
 * when the user holds {@code READ} on at least one owner (root org, org, or application).</li>
 * </ul>
 *
 * @since 1.203
 */
public class ApiLifecycleResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  public void testGetRepositoryManagers_UnauthenticatedReturns401() throws Exception {
    HttpResponse response = restRequest().path("/api/v2/lifecycle/repositoryManagers").anon().get();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testGetRepositoryManagers_WithoutPermissionReturns403() throws Exception {
    HttpResponse response = restRequest().path("/api/v2/lifecycle/repositoryManagers").auth(unauthorized).get();
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testGetRepositoryManagers_WithConfigureSystemPermissionSucceeds() throws Exception {
    grantConfigureSystemPermission();

    HttpResponse response = restRequest().path("/api/v2/lifecycle/repositoryManagers").auth(authorized).get();
    assertThat(response.getStatusCode()).isLessThan(400);
  }

  @Test
  public void testGetRepositoryManagers_WithReadPermissionOnAnyOwnerSucceeds() throws Exception {
    // Covers Policy Administrator / Owner / Developer who hold READ instead of CONFIGURE_SYSTEM.
    grantReadPermission(org.getId());

    HttpResponse response = restRequest().path("/api/v2/lifecycle/repositoryManagers").auth(authorized).get();
    assertThat(response.getStatusCode()).isLessThan(400);
  }
}
