/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.integration.Goal;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.FilePathRegex;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ProprietaryConfigServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ProprietaryConfigService proprietaryConfigService;

  @Test
  public void testGetProprietaryConfigHierarchy_Unauthorized() {
    login();
    Assertions.assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> proprietaryConfigService.getProprietaryConfigHierarchy(org.getType(), org.getId()));
  }

  @Test
  public void testGetProprietaryConfigHierarchy_Unauthenticated() {
    Assertions.assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> proprietaryConfigService.getProprietaryConfigHierarchy(org.getType(), org.getId()));
  }

  @Test
  public void testGetProprietaryConfigHierarchy_Authorized() {
    grantReadPermission(org.getId());
    proprietaryConfigService.getProprietaryConfigHierarchy(org.getType(), org.getId());
  }

  @Test
  public void testUpsertProprietaryConfig_Unauthorized() {
    grantReadPermission(org.getId());
    Assertions.assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> proprietaryConfigService.upsertProprietaryConfig(org.getType(), org.getId(),
            new ProprietaryConfig()));
  }

  @Test
  public void testUpsertProprietaryConfig_Unauthenticated() {
    Assertions.assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> proprietaryConfigService.upsertProprietaryConfig(org.getType(), org.getId(),
            new ProprietaryConfig()));
  }

  @Test
  public void testUpsertProprietaryConfig_Authorized() {
    grantManageProprietaryPermission(org.getId());
    proprietaryConfigService.upsertProprietaryConfig(org.getType(), org.getId(), new ProprietaryConfig());
  }

  @Test
  public void testAddFilePathRegexToProprietaryConfig_Unauthorized() {
    grantReadPermission(org.getId());
    Assertions.assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> proprietaryConfigService.addFilePathRegexToProprietaryConfig(org.getType(), org.getId(),
            null));
  }

  @Test
  public void testAddFilePathRegexToProprietaryConfig_Unauthenticated() {
    Assertions.assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> proprietaryConfigService.addFilePathRegexToProprietaryConfig(org.getType(), org.getId(),
            null));
  }

  @Test
  public void testAddFilePathRegexToProprietaryConfig_Authorized() {
    grantManageProprietaryPermission(org.getId());
    proprietaryConfigService.addFilePathRegexToProprietaryConfig(org.getType(), org.getId(), new FilePathRegex());
  }

  @Test
  public void testGetProprietaryConfig_NoGoal_Authorized() {
    login();
    proprietaryConfigService.getProprietaryConfig((Goal) null, null);
  }

  @Test
  public void testGetProprietaryConfig_NoGoal_Unauthenticated() {
    proprietaryConfigService.getProprietaryConfig((Goal) null, null);
  }

  @Test
  public void testGetProprietaryConfig_EvaluateApplication_Authorized() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    proprietaryConfigService.getProprietaryConfig(Goal.EVALUATE_APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetProprietaryConfig_EvaluateApplication_Unauthorized() {
    login();
    Assertions.assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> proprietaryConfigService.getProprietaryConfig(Goal.EVALUATE_APPLICATION,
            app.getPublicId()));
  }

  @Test
  public void testGetProprietaryConfig_EvaluateApplication_Unauthenticated() {
    Assertions.assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> proprietaryConfigService.getProprietaryConfig(Goal.EVALUATE_APPLICATION,
            app.getPublicId()));
  }

  @Test
  public void testGetProprietaryConfig_EvaluateComponent_Authorized() {
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);
    proprietaryConfigService.getProprietaryConfig(Goal.EVALUATE_COMPONENT, app.getPublicId());
  }

  @Test
  public void testGetProprietaryConfig_EvaluateComponent_Unauthorized() {
    login();
    Assertions.assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> proprietaryConfigService.getProprietaryConfig(Goal.EVALUATE_COMPONENT,
            app.getPublicId()));
  }

  @Test
  public void testGetProprietaryConfig_EvaluateComponent_Unauthenticated() {
    Assertions.assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> proprietaryConfigService.getProprietaryConfig(Goal.EVALUATE_COMPONENT,
            app.getPublicId()));
  }
}
