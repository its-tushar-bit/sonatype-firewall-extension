/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiCompositeSourceControlConfigValidatorServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  @Inject
  public ApiCompositeSourceControlConfigValidatorService service;

  @Test
  public void testGetCompositeSourceControlByOwner_Authorized() {
    grantReadPermission(app.getId());
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, "TOKEN",
        SourceControlProvider.GITHUB, null, null,
        "BASE_BRANCH", null);
    tempEntity.newSourceControl(app.getId(), VALID_URL, null, null);
    // should pass auth stage and promptly throw an exception because TOKEN can't be decrypted
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.validateSourceControlConfig(app.getId()))
        .withMessageContaining("org.sonatype.plexus.components.cipher.PlexusCipherException");
  }

  @Test
  public void testGetCompositeSourceControlByOwner_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.validateSourceControlConfig(app.getId()));
  }

  @Test
  public void testGetCompositeSourceControlByOwner_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.validateSourceControlConfig(app.getId()));
  }
}
