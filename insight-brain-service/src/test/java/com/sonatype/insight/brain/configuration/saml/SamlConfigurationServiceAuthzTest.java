/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.saml;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.SAML_ENABLED;

@Category(SlowTest.class)
public class SamlConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SamlConfigurationService samlConfigurationService;

  @Test
  public void testInsert_SamlEnabled() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration(null, null));
  }

  @Test(expected = NotFoundException.class)
  public void testInsert_SamlDisabled() {
    SAML_ENABLED.setEnabled(false);

    samlConfigurationService.insert(tempEntity.newSamlConfiguration(null, null));
  }
}
