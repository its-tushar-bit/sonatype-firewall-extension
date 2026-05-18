/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemConfigurationPropertyFeatureTest
{
  @Test
  public void testIsEnabledWithMap_PropertyAbsent_EnabledWhenAbsentTrue() {
    // Features with enabledWhenAbsent = true should return true when property is absent
    Map<String, SystemConfigurationProperty> emptyMap = Collections.emptyMap();

    assertThat(SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isEnabled(emptyMap)).isTrue();
    assertThat(SystemConfigurationPropertyFeature.CODE_INSIGHTS.isEnabled(emptyMap)).isTrue();
    assertThat(SystemConfigurationPropertyFeature.PR_COMMENTING.isEnabled(emptyMap)).isTrue();
  }

  @Test
  public void testIsEnabledWithMap_PropertyAbsent_EnabledWhenAbsentFalse() {
    // Features with enabledWhenAbsent = false should return false when property is absent
    Map<String, SystemConfigurationProperty> emptyMap = Collections.emptyMap();

    assertThat(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled(emptyMap)).isFalse();
    assertThat(SystemConfigurationPropertyFeature.SBOM_MANAGER.isEnabled(emptyMap)).isFalse();
  }

  @Test
  public void testIsEnabledWithMap_PropertyPresent_TrueValue() {
    Map<String, SystemConfigurationProperty> map = new HashMap<>();
    String propertyName = SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.getPropertyName();
    map.put(propertyName, new SystemConfigurationProperty(propertyName, "true"));

    // For features with enabledWhenAbsent = true, presence of row means disabled
    assertThat(SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isEnabled(map)).isFalse();
  }

  @Test
  public void testIsEnabledWithMap_PropertyPresent_FalseValue() {
    Map<String, SystemConfigurationProperty> map = new HashMap<>();
    String propertyName = SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.getPropertyName();
    map.put(propertyName, new SystemConfigurationProperty(propertyName, "false"));

    // For features with enabledWhenAbsent = true, presence of row means disabled (regardless of value)
    assertThat(SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isEnabled(map)).isFalse();
  }

  @Test
  public void testIsEnabledWithMap_AdvancedSearchEnabled() {
    // ADVANCED_SEARCH_ENABLED uses value parsing, not presence/absence
    Map<String, SystemConfigurationProperty> map = new HashMap<>();
    String propertyName = SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.getPropertyName();

    // Absent = disabled (special case - requires explicit "true" value)
    assertThat(SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled(map)).isFalse();

    // Value "true" = enabled
    map.put(propertyName, new SystemConfigurationProperty(propertyName, "true"));
    assertThat(SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled(map)).isTrue();

    // Value "false" = disabled
    map.put(propertyName, new SystemConfigurationProperty(propertyName, "false"));
    assertThat(SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled(map)).isFalse();
  }

  @Test
  public void testIsEnabledWithMap_AutoWaivers() {
    // AUTO_WAIVERS: enabled when absent OR when value is "true"
    Map<String, SystemConfigurationProperty> map = new HashMap<>();
    String propertyName = SystemConfigurationPropertyFeature.AUTO_WAIVERS.getPropertyName();

    // Absent = enabled (enabledWhenAbsent = true)
    assertThat(SystemConfigurationPropertyFeature.AUTO_WAIVERS.isEnabled(map)).isTrue();

    // Value "true" = enabled
    map.put(propertyName, new SystemConfigurationProperty(propertyName, "true"));
    assertThat(SystemConfigurationPropertyFeature.AUTO_WAIVERS.isEnabled(map)).isTrue();

    // Value "false" = disabled
    map.put(propertyName, new SystemConfigurationProperty(propertyName, "false"));
    assertThat(SystemConfigurationPropertyFeature.AUTO_WAIVERS.isEnabled(map)).isFalse();
  }

  @Test
  public void testIsEnabledWithMap_DeveloperSuggestNonBreakingVersion() {
    // Same logic as AUTO_WAIVERS: enabled when absent OR when value is "true"
    Map<String, SystemConfigurationProperty> map = new HashMap<>();
    String propertyName = SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.getPropertyName();

    assertThat(SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "true"));
    assertThat(SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "false"));
    assertThat(SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION.isEnabled(map)).isFalse();
  }

  @Test
  public void testIsEnabledWithMap_ContainerImagesEvalEnabled() {
    Map<String, SystemConfigurationProperty> map = new HashMap<>();
    String propertyName = SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.getPropertyName();

    assertThat(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "true"));
    assertThat(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "false"));
    assertThat(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled(map)).isFalse();
  }

  @Test
  public void testIsEnabledWithMap_Zscaler() {
    Map<String, SystemConfigurationProperty> map = new HashMap<>();
    String propertyName = SystemConfigurationPropertyFeature.ZSCALER.getPropertyName();

    assertThat(SystemConfigurationPropertyFeature.ZSCALER.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "true"));
    assertThat(SystemConfigurationPropertyFeature.ZSCALER.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "false"));
    assertThat(SystemConfigurationPropertyFeature.ZSCALER.isEnabled(map)).isFalse();
  }

  @Test
  public void testIsEnabledWithMap_ThirdPartyKevLookup() {
    Map<String, SystemConfigurationProperty> map = new HashMap<>();
    String propertyName = SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.getPropertyName();

    assertThat(SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "true"));
    assertThat(SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "false"));
    assertThat(SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.isEnabled(map)).isFalse();
  }

  @Test
  public void testIsEnabledWithMap_FirewallWaiverDashboardAndRenew() {
    Map<String, SystemConfigurationProperty> map = new HashMap<>();
    String propertyName = SystemConfigurationPropertyFeature.FIREWALL_WAIVER_DASHBOARD_AND_RENEW.getPropertyName();

    // Absent = disabled (enabledWhenAbsent = false)
    assertThat(SystemConfigurationPropertyFeature.FIREWALL_WAIVER_DASHBOARD_AND_RENEW.isEnabled(map)).isFalse();

    // Present = enabled (presence-based, value is irrelevant)
    map.put(propertyName, new SystemConfigurationProperty(propertyName, "true"));
    assertThat(SystemConfigurationPropertyFeature.FIREWALL_WAIVER_DASHBOARD_AND_RENEW.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "false"));
    assertThat(SystemConfigurationPropertyFeature.FIREWALL_WAIVER_DASHBOARD_AND_RENEW.isEnabled(map)).isTrue();
  }

  @Test
  public void testIsEnabledWithMap_ExitOnFatalError() {
    Map<String, SystemConfigurationProperty> map = new HashMap<>();
    String propertyName = SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.getPropertyName();

    assertThat(SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "true"));
    assertThat(SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.isEnabled(map)).isTrue();

    map.put(propertyName, new SystemConfigurationProperty(propertyName, "false"));
    assertThat(SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.isEnabled(map)).isFalse();
  }
}
