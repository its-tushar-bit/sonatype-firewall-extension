/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SloViolationsRestResourceTest
    extends AbstractResourceTest
{
  private static final String PASS_CODE = "a-pass-code";

  private static final String SCAN_ID = "57e6e8169eca4b5a8e5d48d624c9e1ee";

  private Application application;

  private String path;

  @Before
  public void setup() {
    SystemConfigurationPropertyFeature.SLO_VIOLATION_FEED.setEnabled(true);

    application = tempEntity.newApplicationWithParent("slo-app");
    tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, SCAN_ID);

    path = "rest/slo/" + application.getPublicId() + "/violations";
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeature.SLO_VIOLATION_FEED.setEnabled(false);
  }

  private UserToken newTokenFor(final User user) {
    final String hashedPassCode = getCLMServer().getInstance(PasswordService.class).encryptPassword(PASS_CODE);
    return tempEntity.newUserToken(user.getUsername(), user.getUsername() + "-code", hashedPassCode, InternalRealm.ID);
  }

  @Test
  public void unauthenticated_returns401() throws Exception {
    final HttpResponse response = restRequest()
        .anon()
        .path(path)
        .get();

    assertResponseStatus(401, response);
  }

  @Test
  public void tokenWithoutReadPermission_returns403() throws Exception {
    final User user = tempEntity.newUser();
    final UserToken token = newTokenFor(user);

    final HttpResponse response = restRequest()
        .anon()
        .auth(token.getUserCode(), PASS_CODE)
        .path(path)
        .get();

    assertResponseStatus(403, response);
  }

  @Test
  public void tokenWithReadPermission_returns200_noSession() throws Exception {
    final User user = createUserWithPermissions(Permission.READ);
    final UserToken token = newTokenFor(user);

    final HttpResponse response = restRequest()
        .anon()
        .auth(token.getUserCode(), PASS_CODE)
        .path(path)
        .get();

    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();
  }

  @Test
  public void featureFlagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.SLO_VIOLATION_FEED.setEnabled(false);

    final HttpResponse response = restRequest()
        .path(path)
        .get();

    assertResponseStatus(404, response);
  }
}
