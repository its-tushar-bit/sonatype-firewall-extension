/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.service.ConfigurationProperty.PUBLIC_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiConfigurationService service;

  @Test
  public void testGetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.getConfiguration(Collections.emptySet()));
  }

  @Test
  public void testGetConfiguration_QuarantineMessage_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> service
            .getConfiguration(Collections.singleton(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE)));
  }

  @Test
  public void testGetConfiguration_Unauthorized() {
    login();
    Set<String> allPropertiesThatRequireConfigureSystemPermission =
        Arrays.stream(ConfigurationProperty.PROPERTIES)
            .map(property -> property.getName())
            .filter(property -> !PUBLIC_PROPERTIES.contains(property))
            .collect(Collectors.toSet());
    for (String property : allPropertiesThatRequireConfigureSystemPermission) {
      assertThrows(UnauthorizedException.class,
          () -> service.getConfiguration(Collections.singleton(property)),
          "Insufficient permissions");
    }
  }

  @Test
  public void testGetConfiguration_WithConfigureSystemPermission_Authorized() {
    grantConfigureSystemPermission();
    Set<String> allPropertiesThatRequireConfigureSystemPermission =
        Arrays.stream(ConfigurationProperty.PROPERTIES)
            .map(x -> x.getName())
            .collect(Collectors.toSet());
    assertNotNull(service.getConfiguration(allPropertiesThatRequireConfigureSystemPermission));
  }

  @Test
  public void testGetConfiguration_WithEvaluateComponentPermission_Authorized() {
    grantEvaluateComponentPermission(Organization.ROOT_ORGANIZATION_ID);
    assertNotNull(
        service.getConfiguration(Collections.singleton(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE)));
  }

  @Test
  public void testGetConfiguration_WithEvaluateComponentPermissionAtRepositoryManager_Authorized() {
    grantEvaluateComponentPermission(repositoryManager.getId());
    assertNotNull(
        service.getConfiguration(Collections.singleton(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE)));
  }

  @Test
  public void testGetConfiguration_WithEvaluateComponentPermissionAtRepository_NotAuthorized() {
    grantEvaluateComponentPermission(repository.getId());
    assertThrows(UnauthorizedException.class,
        () -> service
            .getConfiguration(Collections.singleton(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE)),
        "Insufficient permissions");
  }

  @Test
  public void testGetConfiguration_WithEvaluateComponentPermission_NotAuthorized() {
    grantEvaluateComponentPermission(Organization.ROOT_ORGANIZATION_ID);
    Set<String> allPropertiesThatRequireConfigureSystemPermission =
        Arrays.stream(ConfigurationProperty.PROPERTIES)
            .map(property -> property.getName())
            .filter(property -> !property.equals(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE))
            .filter(property -> !PUBLIC_PROPERTIES.contains(property))
            .collect(Collectors.toSet());
    for (String property : allPropertiesThatRequireConfigureSystemPermission) {
      assertThrows(UnauthorizedException.class,
          () -> service.getConfiguration(Collections.singleton(property)),
          "Insufficient permissions");
    }
  }

  @Test
  public void testGetConfiguration_PublicProperties_Authorized() {
    login();
    assertThat(PUBLIC_PROPERTIES)
        .allSatisfy(property -> assertThat(service.getConfiguration(Collections.singleton(property))).isNotNull());
  }

  @Test
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class, () -> service.getConfiguration(Collections.emptySet()));
  }

  @Test
  public void testSetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.setConfiguration(Collections.emptyMap()));
  }

  @Test
  public void testSetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.setConfiguration(Collections.emptyMap()));
  }

  @Test
  public void testSetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class, () -> service.setConfiguration(Collections.emptyMap()));
  }

  @Test
  public void testDeleteConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.deleteConfiguration(Collections.emptySet()));
  }

  @Test
  public void testDeleteConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.deleteConfiguration(Collections.emptySet()));
  }

  @Test
  public void testDeleteConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class, () -> service.deleteConfiguration(Collections.emptySet()));
  }
}
