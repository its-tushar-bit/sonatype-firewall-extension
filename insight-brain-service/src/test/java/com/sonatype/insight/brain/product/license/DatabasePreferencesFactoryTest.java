/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.prefs.Preferences;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.CLMLicenseBuilder;

import org.sonatype.licensing.product.util.LicenseContent;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DatabasePreferencesFactoryTest
    extends AbstractComponentTest
{
  @Inject
  private DatabasePreferencesFactory databasePreferencesFactory;

  @Inject
  private LicenseContent licenseContent;

  @Test
  public void testNodeForPath_LicensePath() {
    Preferences preferences = databasePreferencesFactory.nodeForPath(CLMLicenseBuilder.PREFERENCES_PATH);

    assertThat(preferences).isNotNull();
  }

  @Test
  public void testNodeForPath_NotLicensePath() {
    String invalidPathname = "not-" + CLMLicenseBuilder.PREFERENCES_PATH;

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> databasePreferencesFactory.nodeForPath(invalidPathname))
        .withMessage("Invalid node pathname: " + invalidPathname);
  }

  @Test
  public void testLicenseContentRaw() {
    ProductLicense expected = tempEntity.setProductLicense();

    assertThat(licenseContent.raw()).isEqualTo(
        Base64.getDecoder().decode(expected.getLicenseKey().getBytes(StandardCharsets.UTF_8)));
  }
}
