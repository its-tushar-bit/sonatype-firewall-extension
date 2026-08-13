/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.saml;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.SAML_ENABLED;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class SamlConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private SamlConfigurationService samlConfigurationService;

  @Test
  public void testInsert_SamlEnabled() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration(null, null));
  }

  @Test
  public void testInsert_SamlDisabled() {
    SAML_ENABLED.setEnabled(false);

    assertThrows(NotFoundException.class,
        () -> samlConfigurationService.insert(tempEntity.newSamlConfiguration(null, null)));
  }
}
