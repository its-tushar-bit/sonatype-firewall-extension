/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.prefs.Preferences;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.ProductLicense;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ProductLicenseDAOTest
    extends AbstractDbDAOTest
{
  private ProductLicenseDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createProductLicenseDAO();
  }

  @Test
  public void testCRUD() {
    assertThat(dao.get()).isNull();

    ProductLicense productLicense = createProductLicense();

    dao.insert(productLicense);

    assertProductLicense(dao.get(), productLicense);

    productLicense.setLicenseKey(productLicense.getLicenseKey() + "Different");
    productLicense.setLicenseDetails(productLicense.getLicenseDetails() + "Different");

    dao.update(productLicense);

    assertProductLicense(dao.get(), productLicense);

    dao.delete();

    assertThat(dao.get()).isNull();
  }

  @Test
  public void testInsert_EnforceSingleton() {
    dao.insert(createProductLicense());

    assertThatExceptionOfType(PersistenceException.class)
        .isThrownBy(() -> dao.insert(createProductLicense()))
        .withCauseInstanceOf(EntityExistsException.class);

    ProductLicense productLicense = createProductLicense();
    productLicense.setId("not-" + ProductLicenseDAO.SINGLETON_ENTITY_ID);

    assertThatExceptionOfType(PersistenceException.class)
        .isThrownBy(() -> dao.insert(productLicense))
        .withCauseInstanceOf(EntityExistsException.class);
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    dao.insert(createProductLicense());
    ProductLicense expected = createProductLicense();
    String notSingletonEntityId = "not-" + ProductLicenseDAO.SINGLETON_ENTITY_ID;
    expected.setLicenseDetails(expected.getLicenseDetails() + "Different");
    expected.setId(notSingletonEntityId);

    dao.update(expected);

    assertThat(dao.getById(notSingletonEntityId)).isNull();
    ProductLicense actual = dao.get();
    assertProductLicense(actual, expected);
  }

  @Test
  public void testLicenseKey_MaxLength() {
    ProductLicense expected = new ProductLicense();
    expected.setLicenseKey(StringUtils.repeat('A', Preferences.MAX_VALUE_LENGTH));

    dao.insert(expected);

    ProductLicense actual = dao.get();
    assertProductLicense(actual, expected);
  }

  @Test
  public void testLicenseDetails_MaxLength() {
    ProductLicense expected = new ProductLicense();
    expected.setLicenseKey("anything");
    expected.setLicenseDetails(StringUtils.repeat('A', Preferences.MAX_VALUE_LENGTH));

    dao.insert(expected);

    ProductLicense actual = dao.get();
    assertProductLicense(actual, expected);
  }

  private ProductLicense createProductLicense() {
    ProductLicense productLicense = new ProductLicense();
    productLicense.setLicenseKey("licenseKey");
    productLicense.setLicenseDetails("licenseDetails");
    return productLicense;
  }

  private void assertProductLicense(ProductLicense actual, ProductLicense expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(ProductLicenseDAO.SINGLETON_ENTITY_ID);
    assertThat(actual.getLicenseKey()).isEqualTo(expected.getLicenseKey());
    assertThat(actual.getLicenseDetails()).isEqualTo(expected.getLicenseDetails());
  }
}
