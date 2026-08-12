/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Authorization tests for HostedComponentResource.
 * Tests verify that EVALUATE_APPLICATION permission is required to upload scan files.
 *
 * @since 1.203
 */
public class HostedComponentResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private static final String UPLOAD_PATH =
      "api/v2/repositories/test-manager/test-repo/components";

  @Before
  public void enableFeature() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
  }

  @After
  public void disableFeature() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  private void grantEvaluateApplicationPermission() {
    Role role = tempEntity.newRole(true /* global */, Permission.EVALUATE_APPLICATION);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());
  }

  @Test
  public void testUploadScan_UnauthenticatedReturns401() throws Exception {
    HttpResponse response = restRequest().path(UPLOAD_PATH).anon().post();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testUploadScan_WithoutPermissionReturns403() throws Exception {
    HttpResponse response = restRequest().path(UPLOAD_PATH)
        .auth(unauthorized)
        .part("componentId", "test-component")
        .post();
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testUploadScan_WithPermissionPassesAuthz() throws Exception {
    grantEvaluateApplicationPermission();

    HttpResponse response = restRequest().path(UPLOAD_PATH).auth(authorized).post();
    // 400 is expected: auth passed but request body is missing required fields
    assertThat(response.getStatusCode()).isNotEqualTo(401);
    assertThat(response.getStatusCode()).isNotEqualTo(403);
  }
}
