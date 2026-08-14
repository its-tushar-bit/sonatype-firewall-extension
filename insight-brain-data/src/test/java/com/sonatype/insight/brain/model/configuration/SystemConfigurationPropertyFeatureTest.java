/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemConfigurationPropertyFeatureTest
    extends AbstractDataTest
{
  private SystemConfigurationPropertyDAO dao;

  @BeforeEach
  public void setUp() {
    dao = daoFactory.createSystemConfigurationPropertyDAO();
  }

  @Test
  public void isEnabled_enabledWhenAbsent_returnsTrueWhenPropertyAbsent() {
    // TRANSITIVE_SOLVER has enabledWhenAbsent = true
    assertThat(SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isEnabled()).isTrue();
  }

  @Test
  public void isEnabled_enabledWhenAbsent_returnsFalseWhenPropertyPresent() {
    // TRANSITIVE_SOLVER has enabledWhenAbsent = true, so presence means disabled
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.getPropertyName(), "true");

    assertThat(SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isEnabled()).isFalse();
  }

  @Test
  public void isEnabled_disabledWhenAbsent_returnsFalseWhenPropertyAbsent() {
    // BUILT_FROM_SOURCE has enabledWhenAbsent = false
    assertThat(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()).isFalse();
  }

  @Test
  public void isEnabled_disabledWhenAbsent_returnsTrueWhenPropertyPresent() {
    // BUILT_FROM_SOURCE has enabledWhenAbsent = false, so presence means enabled
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getPropertyName(), "false");

    assertThat(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()).isTrue();
  }

  @Test
  public void isEnabled_withTransaction_enabledWhenAbsent() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThat(SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isEnabled(tx)).isTrue();
    }
  }

  @Test
  public void isEnabled_withTransaction_disabledWhenPresent() {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.getPropertyName(), "true");

    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThat(SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isEnabled(tx)).isFalse();
    }
  }

  @Test
  public void isStored_returnsTrueWhenPropertyPresent() {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.getPropertyName(), "true");

    assertThat(SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isStored()).isTrue();
  }

  @Test
  public void isStored_returnsFalseWhenPropertyAbsent() {
    assertThat(SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER.isStored()).isFalse();
  }

  @Test
  public void isEnabled_advancedSearchEnabled_usesValueParsing() {
    // ADVANCED_SEARCH_ENABLED uses value parsing, not presence/absence
    String propertyName = SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.getPropertyName();

    // Absent = disabled
    assertThat(SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled()).isFalse();

    // Value "true" = enabled
    dao.set(propertyName, "true");
    assertThat(SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled()).isTrue();

    // Value "false" = disabled
    dao.set(propertyName, "false");
    assertThat(SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled()).isFalse();
  }

  @Test
  public void isEnabled_autoWaivers_enabledWhenAbsentOrValueTrue() {
    // AUTO_WAIVERS: enabled when absent OR when value is "true"
    String propertyName = SystemConfigurationPropertyFeature.AUTO_WAIVERS.getPropertyName();

    // Absent = enabled (enabledWhenAbsent = true)
    assertThat(SystemConfigurationPropertyFeature.AUTO_WAIVERS.isEnabled()).isTrue();

    // Value "true" = enabled
    dao.set(propertyName, "true");
    assertThat(SystemConfigurationPropertyFeature.AUTO_WAIVERS.isEnabled()).isTrue();

    // Value "false" = disabled
    dao.set(propertyName, "false");
    assertThat(SystemConfigurationPropertyFeature.AUTO_WAIVERS.isEnabled()).isFalse();
  }

  @Test
  public void isEnabled_containerImagesEvalEnabled_enabledWhenAbsentOrValueTrue() {
    String propertyName = SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.getPropertyName();

    assertThat(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled()).isTrue();

    dao.set(propertyName, "true");
    assertThat(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled()).isTrue();

    dao.set(propertyName, "false");
    assertThat(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled()).isFalse();
  }

  @Test
  public void isEnabled_zscaler_enabledWhenAbsentOrValueTrue() {
    String propertyName = SystemConfigurationPropertyFeature.ZSCALER.getPropertyName();

    assertThat(SystemConfigurationPropertyFeature.ZSCALER.isEnabled()).isTrue();

    dao.set(propertyName, "true");
    assertThat(SystemConfigurationPropertyFeature.ZSCALER.isEnabled()).isTrue();

    dao.set(propertyName, "false");
    assertThat(SystemConfigurationPropertyFeature.ZSCALER.isEnabled()).isFalse();
  }

  @Test
  public void isEnabled_thirdPartyKevLookup_enabledWhenAbsentOrValueTrue() {
    String propertyName = SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.getPropertyName();

    assertThat(SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.isEnabled()).isTrue();

    dao.set(propertyName, "true");
    assertThat(SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.isEnabled()).isTrue();

    dao.set(propertyName, "false");
    assertThat(SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.isEnabled()).isFalse();
  }

  @Test
  public void isEnabled_exitOnFatalError_enabledWhenAbsentOrValueTrue() {
    String propertyName = SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.getPropertyName();

    assertThat(SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.isEnabled()).isTrue();

    dao.set(propertyName, "true");
    assertThat(SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.isEnabled()).isTrue();

    dao.set(propertyName, "false");
    assertThat(SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.isEnabled()).isFalse();
  }

  @Test
  public void setEnabled_togglesFeature() {
    // Start disabled (absent, enabledWhenAbsent = false)
    assertThat(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()).isFalse();

    // Enable it
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    assertThat(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()).isTrue();

    // Disable it
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(false);
    assertThat(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()).isFalse();
  }

  // Tests for presence-based feature flags (enabledWhenAbsent = false)

  @Test
  public void presenceBasedFlags_DisabledWhenAbsent() {
    // Features with enabledWhenAbsent = false should return false when property is absent
    assertThat(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()).isFalse();
    assertThat(SystemConfigurationPropertyFeature.SBOM_MANAGER.isEnabled()).isFalse();
    assertThat(SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.isEnabled()).isFalse();
    assertThat(SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_ANONYMOUS_ENABLED.isEnabled()).isFalse();
    assertThat(SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED.isEnabled()).isFalse();
    assertThat(SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_DEFAULT_TO_PREVIEW.isEnabled()).isFalse();
    assertThat(SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_DISABLE_SWITCH_FEEDBACK.isEnabled()).isFalse();
    assertThat(SystemConfigurationPropertyFeature.SLO_VIOLATION_FEED.isEnabled()).isFalse();
  }

  @Test
  public void previewNexusOneUi_PropertyPresent_FeatureEnabled() {
    assertPreviewFlagEnabledWhenPropertyPresent(SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI);
  }

  @Test
  public void previewNexusOneUi_PropertyPresentFalseValue_StillEnabled() {
    // Presence-based: even a value of "false" means enabled, because enabledWhenAbsent = false
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName(), "false");

    assertThat(SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.isEnabled()).isTrue();
  }

  @Test
  public void previewNexusOneUiAnonymousEnabled() {
    assertPreviewFlagEnabledWhenPropertyPresent(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_ANONYMOUS_ENABLED);
  }

  @Test
  public void previewNexusOneUiLoggedInEnabled() {
    assertPreviewFlagEnabledWhenPropertyPresent(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED);
  }

  @Test
  public void previewNexusOneUiDefaultToPreview() {
    assertPreviewFlagEnabledWhenPropertyPresent(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_DEFAULT_TO_PREVIEW);
  }

  @Test
  public void previewNexusOneUiDisableSwitchFeedback() {
    assertPreviewFlagEnabledWhenPropertyPresent(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_DISABLE_SWITCH_FEEDBACK);
  }

  private void assertPreviewFlagEnabledWhenPropertyPresent(
      SystemConfigurationPropertyFeature feature)
  {
    tempEntity.newSystemConfigurationProperty(feature.getPropertyName(), "true");

    assertThat(feature.isEnabled()).isTrue();
  }
}
