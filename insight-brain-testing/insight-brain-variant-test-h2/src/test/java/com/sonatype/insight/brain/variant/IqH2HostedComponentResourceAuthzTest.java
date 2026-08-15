/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the {@code AbstractResourceAuthzTest} fixture (authorized/unauthorized users) that the legacy
 * {@code HostedComponentResourceAuthzTest} inherited from its base class.
 * <p>
 * Authorization tests for HostedComponentResource. Tests verify that EVALUATE_APPLICATION permission is required to
 * upload scan files.
 */
@IqH2Test
class IqH2HostedComponentResourceAuthzTest
{
  private static final String UPLOAD_PATH = "api/v2/repositories/test-manager/test-repo/components";

  private IqTestContext ctx;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void createEntities() {
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();

    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
  }

  @AfterEach
  void tearDown() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon();
  }

  private void grantEvaluateApplicationPermission() {
    Role role = ctx.tempEntity().newRole(true /* global */, Permission.EVALUATE_APPLICATION);
    ctx.tempEntity()
        .newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());
  }

  @Test
  void testUploadScan_UnauthenticatedReturns401() throws Exception {
    HttpResponse response = restRequest().path(UPLOAD_PATH).anon().post();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testUploadScan_WithoutPermissionReturns403() throws Exception {
    HttpResponse response = restRequest().path(UPLOAD_PATH)
        .auth(unauthorized)
        .part("componentId", "test-component")
        .post();
    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testUploadScan_WithPermissionPassesAuthz() throws Exception {
    grantEvaluateApplicationPermission();

    HttpResponse response = restRequest().path(UPLOAD_PATH).auth(authorized).post();
    // 400 is expected: auth passed but request body is missing required fields
    assertThat(response.getStatusCode()).isNotEqualTo(401);
    assertThat(response.getStatusCode()).isNotEqualTo(403);
  }
}
