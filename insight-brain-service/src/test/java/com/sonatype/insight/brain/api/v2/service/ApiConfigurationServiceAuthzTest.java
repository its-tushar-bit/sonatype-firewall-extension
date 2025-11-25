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
import javax.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.service.ConfigurationProperty.PUBLIC_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class ApiConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiConfigurationService service;

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfiguration_Unauthenticated() {
    service.getConfiguration(Collections.emptySet());
  }

  @Test
  public void testGetConfiguration_Unauthorized() {
    login();
    Set<String> allPropertiesThatRequireConfigureSystemPermission =
        Arrays.stream(ConfigurationProperty.PROPERTIES).map(property -> property.getName())
            .filter(property -> !PUBLIC_PROPERTIES.contains(property))
            .collect(Collectors.toSet());
    for (String property : allPropertiesThatRequireConfigureSystemPermission) {
      assertThrows("Insufficient permissions",
          UnauthorizedException.class,
          () -> service.getConfiguration(Collections.singleton(property)));
    }
  }

  @Test
  public void testGetConfiguration_WithConfigureSystemPermission_Authorized() {
    grantConfigureSystemPermission();
    Set<String> allPropertiesThatRequireConfigureSystemPermission =
        Arrays.stream(ConfigurationProperty.PROPERTIES)
            .map(x -> x.getName()).collect(Collectors.toSet());
    assertNotNull(service.getConfiguration(allPropertiesThatRequireConfigureSystemPermission));
  }

  @Test
  public void testGetConfiguration_WithEvaluateComponentPermission_Authorized() {
    grantEvaluateComponentPermission(Organization.ROOT_ORGANIZATION_ID);
    assertNotNull(
        service.getConfiguration(Collections.singleton(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE)));
  }

  @Test
  public void testGetConfiguration_WithEvaluateComponentPermission_NotAuthorized() {
    grantEvaluateComponentPermission(Organization.ROOT_ORGANIZATION_ID);
    Set<String> allPropertiesThatRequireConfigureSystemPermission =
        Arrays.stream(ConfigurationProperty.PROPERTIES).map(property -> property.getName())
            .filter(property -> !property.equals(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE))
            .filter(property -> !PUBLIC_PROPERTIES.contains(property))
            .collect(Collectors.toSet());
    for (String property : allPropertiesThatRequireConfigureSystemPermission) {
      assertThrows("Insufficient permissions",
          UnauthorizedException.class,
          () -> service.getConfiguration(Collections.singleton(property)));
    }
  }

  @Test
  public void testGetConfiguration_PublicProperties_Authorized() {
    login();
    assertThat(PUBLIC_PROPERTIES)
        .allSatisfy(property ->
            assertThat(service.getConfiguration(Collections.singleton(property))).isNotNull());
  }

  @Test(expected = BadRequestException.class)
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.getConfiguration(Collections.emptySet());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetConfiguration_Unauthenticated() {
    service.setConfiguration(Collections.emptyMap());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetConfiguration_Unauthorized() {
    login();
    service.setConfiguration(Collections.emptyMap());
  }

  @Test(expected = BadRequestException.class)
  public void testSetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.setConfiguration(Collections.emptyMap());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteConfiguration_Unauthenticated() {
    service.deleteConfiguration(Collections.emptySet());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteConfiguration_Unauthorized() {
    login();
    service.deleteConfiguration(Collections.emptySet());
  }

  @Test(expected = BadRequestException.class)
  public void testDeleteConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.deleteConfiguration(Collections.emptySet());
  }
}
