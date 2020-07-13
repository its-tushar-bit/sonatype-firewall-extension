/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

public class ApiCompositeSourceControlConfigValidatorServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  @Inject
  public ApiCompositeSourceControlConfigValidatorService service;

  @Test(expected = IllegalStateException.class)
  public void testGetCompositeSourceControlByOwner_Authorized() {
    grantManageAutomaticSourceControlPermission();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, "TOKEN",
            SourceControlProvider.GITHUB, null, null,
            "BASE_BRANCH", null);
    tempEntity.newSourceControl(app.getId(), VALID_URL, null, null);
    // should pass auth stage and promptly throw an exception because TOKEN can't be decrypted
    service.validateSourceControlConfig(app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetCompositeSourceControlByOwner_Unauthenticated() {
    service.validateSourceControlConfig(app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetCompositeSourceControlByOwner_Unauthorized() {
    login();
    service.validateSourceControlConfig(app.getId());
  }
}
