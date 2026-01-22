/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.prefs.Preferences;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.license.model.CLMLicenseBuilder;

import org.sonatype.licensing.PreferencesFactory;

@Named
@Typed(PreferencesFactory.class)
@Singleton
public class DatabasePreferencesFactory
    implements PreferencesFactory
{
  private final DatabasePreferences licensePreferences;

  @Inject
  public DatabasePreferencesFactory(final ProductLicenseDAO productLicenseDAO) {
    licensePreferences = new DatabasePreferences(productLicenseDAO);
  }

  @Override
  public Preferences nodeForPackage(Class<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Preferences nodeForPath(String absolutePath) {
    if (!CLMLicenseBuilder.PREFERENCES_PATH.equals(absolutePath)) {
      throw new IllegalArgumentException("Invalid node pathname: " + absolutePath);
    }
    return licensePreferences;
  }
}
