/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.prefs.Preferences;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.configuration.ProductLicense;
import com.sonatype.insight.brain.product.license.DatabasePreferences;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.CLMLicenseBuilder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

public class ProductLicenseMigratorTest
    extends AbstractComponentTest
{
  private static final String LICENSE_KEY_DATA = "licenseKeyData";

  private static final String LICENSE_DETAILS_DATA = "licenseDetailsData";

  @Inject
  private ProductLicenseMigrator productLicenseMigrator;

  private ProductLicenseMigrator productLicenseMigratorSpy;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private ProductLicenseDAO productLicenseDAO;

  @Mock
  private Preferences rootNodeMock;

  @Mock
  private Preferences licenseNodeMock;

  @Before
  public void before() {
    productLicenseMigratorSpy = spy(productLicenseMigrator);
    lenient().when(productLicenseMigratorSpy.userRoot()).thenReturn(rootNodeMock);
    lenient().when(rootNodeMock.node(CLMLicenseBuilder.PREFERENCES_PATH)).thenReturn(licenseNodeMock);
  }

  @Test
  public void testMigrate_LicenseExists() {
    assertThat(migrationTrackerDAO.isTrackerPresent(ProductLicenseMigrator.MIGRATION_ID)).isFalse();
    createProductLicenseLocally(LICENSE_KEY_DATA, LICENSE_DETAILS_DATA);
    assertThat(productLicenseDAO.get()).isNull();

    productLicenseMigratorSpy.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(ProductLicenseMigrator.MIGRATION_ID)).isTrue();
    ProductLicense productLicense = productLicenseDAO.get();
    assertThat(productLicense).isNotNull();
    assertThat(productLicense.getLicenseKey()).isEqualTo(LICENSE_KEY_DATA);
    assertThat(productLicense.getLicenseDetails()).isEqualTo(LICENSE_DETAILS_DATA);
  }

  @Test
  public void testMigrate_LicenseExists_WithoutLicenseDetails() {
    assertThat(migrationTrackerDAO.isTrackerPresent(ProductLicenseMigrator.MIGRATION_ID)).isFalse();
    createProductLicenseLocally(LICENSE_KEY_DATA, null);
    assertThat(productLicenseDAO.get()).isNull();

    productLicenseMigratorSpy.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(ProductLicenseMigrator.MIGRATION_ID)).isTrue();
    ProductLicense productLicense = productLicenseDAO.get();
    assertThat(productLicense).isNotNull();
    assertThat(productLicense.getLicenseKey()).isEqualTo(LICENSE_KEY_DATA);
    assertThat(productLicense.getLicenseDetails()).isNull();
  }

  @Test
  public void testMigrate_LicenseDoesNotExist() {
    assertThat(migrationTrackerDAO.isTrackerPresent(ProductLicenseMigrator.MIGRATION_ID)).isFalse();
    assertThat(productLicenseDAO.get()).isNull();

    productLicenseMigratorSpy.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(ProductLicenseMigrator.MIGRATION_ID)).isTrue();
    assertThat(productLicenseDAO.get()).isNull();
  }

  @Test
  public void testMigrate_AlreadyMigrated() {
    ProductLicense productLicense = tempEntity.setProductLicense();
    migrationTrackerDAO.insert(new MigrationTracker(ProductLicenseMigrator.MIGRATION_ID));
    createProductLicenseLocally(productLicense.getLicenseKey() + "Different",
        productLicense.getLicenseDetails() + "Different");

    productLicenseMigratorSpy.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(ProductLicenseMigrator.MIGRATION_ID)).isTrue();
    ProductLicense actual = productLicenseDAO.get();
    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(ProductLicenseDAO.SINGLETON_ENTITY_ID);
    assertThat(actual.getLicenseKey()).isEqualTo(productLicense.getLicenseKey());
    assertThat(actual.getLicenseDetails()).isEqualTo(productLicense.getLicenseDetails());
  }

  private void createProductLicenseLocally(String licenseData, String licenseDetailsData) {
    lenient().when(licenseNodeMock.get(DatabasePreferences.LICENSE_KEY, null)).thenReturn(licenseData);
    if (licenseDetailsData != null) {
      lenient().when(licenseNodeMock.get(DatabasePreferences.LICENSE_DETAILS_KEY, null)).thenReturn(licenseDetailsData);
    }
  }
}
