/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.model.configuration.ProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class DatabasePreferencesTest
    extends AbstractComponentH2Test
{
  @Inject
  private ProductLicenseDAO productLicenseDAO;

  private DatabasePreferences databasePreferences;

  @BeforeEach
  public void init() {
    databasePreferences = new DatabasePreferences(productLicenseDAO);
  }

  @Test
  public void testPut_LicenseExists() {
    ProductLicense expected = tempEntity.setProductLicense();
    expected.setLicenseKey(expected.getLicenseKey() + "Different");
    expected.setLicenseDetails(null);

    databasePreferences.put(DatabasePreferences.LICENSE_KEY, expected.getLicenseKey());

    assertProductLicense(productLicenseDAO.get(), expected);
  }

  @Test
  public void testPut_LicenseDoesNotExist() {
    ProductLicense expected = new ProductLicense();
    expected.setLicenseKey("LICENSE_KEY");

    databasePreferences.put(DatabasePreferences.LICENSE_KEY, expected.getLicenseKey());

    assertProductLicense(productLicenseDAO.get(), expected);
  }

  @Test
  public void testPut_InvalidKey() {
    String invalidKey = "not-" + DatabasePreferences.LICENSE_KEY;

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> databasePreferences.put(invalidKey, "anything"))
        .withMessage("Invalid key name: " + invalidKey);
  }

  @Test
  public void testGet_LicenseExists() {
    ProductLicense expected = tempEntity.setProductLicense();

    assertThat(databasePreferences.get(DatabasePreferences.LICENSE_KEY, null))
        .isEqualTo(expected.getLicenseKey());
  }

  @Test
  public void testGet_LicenseDoesNotExist() {
    assertThat(databasePreferences.get(DatabasePreferences.LICENSE_KEY, null)).isNull();
  }

  @Test
  public void testGet_InvalidKey() {
    String invalidKey = "not-" + DatabasePreferences.LICENSE_KEY;

    assertThat(databasePreferences.get(invalidKey, null)).isNull();
  }

  @Test
  public void testRemove_LicenseExists() {
    tempEntity.setProductLicense();

    databasePreferences.remove(DatabasePreferences.LICENSE_KEY);

    assertThat(productLicenseDAO.get()).isNull();
  }

  @Test
  public void testRemove_LicenseDoesNotExist() {
    databasePreferences.remove(DatabasePreferences.LICENSE_KEY);

    assertThat(productLicenseDAO.get()).isNull();
  }

  @Test
  public void testRemove_InvalidKey() {
    String invalidKey = "not-" + DatabasePreferences.LICENSE_KEY;

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> databasePreferences.remove(invalidKey))
        .withMessage("Invalid key name: " + invalidKey);
  }

  @Test
  public void testGetByteArray_LicenseExists() {
    ProductLicense expected = tempEntity.setProductLicense();

    assertThat(databasePreferences.getByteArray(DatabasePreferences.LICENSE_KEY, null))
        .isEqualTo(Base64.getDecoder().decode(expected.getLicenseKey()));
  }

  @Test
  public void testGetByteArray_LicenseDoesNotExist() {
    assertThat(databasePreferences.getByteArray(DatabasePreferences.LICENSE_KEY, null)).isNull();
  }

  @Test
  public void testGetByteArray_InvalidKey() {
    String invalidKey = "not-" + DatabasePreferences.LICENSE_KEY;

    assertThat(databasePreferences.getByteArray(invalidKey, null)).isNull();
  }

  @Test
  public void testPutByteArray_LicenseExists() {
    ProductLicense expected = tempEntity.setProductLicense();
    byte[] expectedLicenseKey =
        (new String(Base64.getDecoder().decode(expected.getLicenseKey()), StandardCharsets.UTF_8) + "Different")
            .getBytes(StandardCharsets.UTF_8);
    expected.setLicenseKey(Base64.getEncoder().encodeToString(expectedLicenseKey));
    expected.setLicenseDetails(null);

    databasePreferences.putByteArray(DatabasePreferences.LICENSE_KEY, expectedLicenseKey);

    assertProductLicense(productLicenseDAO.get(), expected);
  }

  @Test
  public void testPutByteArray_LicenseDoesNotExist() {
    ProductLicense expected = new ProductLicense();
    byte[] expectedLicenseKey = "LICENSE_KEY".getBytes(StandardCharsets.UTF_8);
    expected.setLicenseKey(Base64.getEncoder().encodeToString(expectedLicenseKey));

    databasePreferences.putByteArray(DatabasePreferences.LICENSE_KEY, expectedLicenseKey);

    assertProductLicense(productLicenseDAO.get(), expected);
  }

  @Test
  public void testPutByteArray_InvalidKey() {
    String invalidKey = "not-" + DatabasePreferences.LICENSE_KEY;

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> databasePreferences.putByteArray(invalidKey, new byte[0]))
        .withMessage("Invalid key name: " + invalidKey);
  }

  @Test
  public void testIsUserNode() {
    assertThat(databasePreferences.isUserNode()).isTrue();
  }

  private void assertProductLicense(ProductLicense actual, ProductLicense expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(ProductLicenseDAO.SINGLETON_ENTITY_ID);
    assertThat(actual.getLicenseKey()).isEqualTo(expected.getLicenseKey());
    assertThat(actual.getLicenseDetails()).isEqualTo(expected.getLicenseDetails());
  }
}
