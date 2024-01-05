/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater.LicenseData;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DefaultLicenseDataUpdaterTest
    extends AbstractBrainServiceIntegrationTest
{
  private static final License license1 = new License("license1", "l1Short", "l1Long");

  private static final License license2 = new License("license2", "l2Short", "l2Long");

  private MultiLicenseDAO multiLicenseDAO;

  private LicenseDAO licenseDAO;

  @Before
  public void setUp() {
    licenseDAO = lookup(LicenseDAO.class);
    multiLicenseDAO = lookup(MultiLicenseDAO.class);
  }

  @Test
  public void testLicense() {
    LicenseData licenseData = createLicenseData();
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
    String newId = "New license id";
    assertThat(licenseDAO.getById(newId)).isNull();

    License newLicense = new License();
    newLicense.setId(newId);
    newLicense.setShortDisplayName("New short name");
    newLicense.setLongDisplayName("New long name");
    licenseData.licenses.add(newLicense);
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
    assertThat(licenseDAO.getById(newId)).isNotNull();
  }

  @Test
  public void testMultiLicense_ById() {
    LicenseData licenseData = createLicenseData();
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
    String newId = "New license id1";
    assertThat(multiLicenseDAO.getById(newId)).isNull();

    MultiLicense newMultiLicense = new MultiLicense();
    newMultiLicense.setId(newId);
    newMultiLicense.setShortDisplayName("New short name1");
    newMultiLicense.setLongDisplayName("New long name");
    licenseData.multiLicenses.add(newMultiLicense);
    Set<String> multiLicenseMappings = new LinkedHashSet<>();
    multiLicenseMappings.add("GPL-2.0");
    licenseData.multiLicenseMappings.put(newId, multiLicenseMappings);
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
    assertThat(multiLicenseDAO.getById(newId)).isNotNull();
    assertThat(multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(newId).iterator().next().getId())
        .isEqualTo("GPL-2.0");
  }

  @Test
  public void testMultiLicense_ByName() {
    LicenseData licenseData = createLicenseData();
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
    String newId = "New license id2";
    String newName = "New short name2";
    assertThat(multiLicenseDAO.getByName(newName)).isNull();

    MultiLicense newMultiLicense = new MultiLicense();
    newMultiLicense.setId(newId);
    newMultiLicense.setShortDisplayName(newName);
    newMultiLicense.setLongDisplayName("New long name");
    licenseData.multiLicenses.add(newMultiLicense);
    Set<String> multiLicenseMappings = new LinkedHashSet<>();
    multiLicenseMappings.add("GPL-2.0");
    licenseData.multiLicenseMappings.put(newId, multiLicenseMappings);
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
    assertThat(multiLicenseDAO.getByName(newName)).isNotNull();
    assertThat(multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(newId).iterator().next().getId())
        .isEqualTo("GPL-2.0");
  }

  @Test
  public void testNoHdsServer() throws Exception {
    getHdsServer().stop();

    try {
      String newId = "New license id";
      assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> multiLicenseDAO.getById(newId))
          .withMessageStartingWith("Could not retrieve license data from Sonatype HDS:");
    }
    finally {
      getHdsServer().start();
    }
  }

  @Test
  public void testMultiLicense_WithNullMappedLicenses_IsNotAdded() {
    LicenseData licenseData = createLicenseData();
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
    MultiLicense mlWithNullMappedLicenses = new MultiLicense("mlWithNullMappedLicenses",
        "mlWithNullMappedLicensesShort", "mlWithNullMappedLicensesLong");
    assertThat(multiLicenseDAO.getById(mlWithNullMappedLicenses.getId())).isNull();

    licenseData.multiLicenses.add(mlWithNullMappedLicenses);
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);

    assertThat(multiLicenseDAO.getById(mlWithNullMappedLicenses.getId())).isNull();
  }

  @Test
  public void testMultiLicense_WithEmptyMappedLicenses_IsNotAdded() {
    LicenseData licenseData = createLicenseData();
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
    MultiLicense mlWithEmptyMappedLicenses = new MultiLicense("mlWithEmptyMappedLicenses",
        "mlWithEmptyMappedLicensesShort", "mlWithEmptyMappedLicensesLong");
    assertThat(multiLicenseDAO.getById(mlWithEmptyMappedLicenses.getId())).isNull();

    licenseData.multiLicenses.add(mlWithEmptyMappedLicenses);
    licenseData.multiLicenseMappings.put(mlWithEmptyMappedLicenses.getId(), new HashSet<>());
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);

    assertThat(multiLicenseDAO.getById(mlWithEmptyMappedLicenses.getId())).isNull();
  }

  @Test
  public void testExistingMultiLicense_CanHaveMappedLicenseAdded() {
    LicenseData licenseData = createLicenseData();
    MultiLicense existingMultiLicense = new MultiLicense("existingMultiLicense", "existingMultiLicenseShort",
        "existingMultiLicenseLong");
    licenseData.licenses.add(license1);
    licenseData.licenses.add(license2);
    licenseData.multiLicenses.add(existingMultiLicense);
    licenseData.multiLicenseMappings.put(existingMultiLicense.getId(), new HashSet<>(
        Collections.singletonList(license1.getId())));
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
    MultiLicense storedMultiLicense = multiLicenseDAO.getById(existingMultiLicense.getId());
    assertThat(licenseDAO.getAll()).contains(license1, license2);
    assertThat(storedMultiLicense).isEqualTo(existingMultiLicense);
    assertThat(multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(existingMultiLicense.getId()))
        .containsExactly(license1);

    licenseData.multiLicenseMappings.get(existingMultiLicense.getId()).add(license2.getId());
    hdsRespondWith(licenseData).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
    LicenseDataUpdater.update(licenseDAO, multiLicenseDAO);
    storedMultiLicense = multiLicenseDAO.getById(existingMultiLicense.getId());
    assertThat(storedMultiLicense).isEqualTo(existingMultiLicense);
    assertThat(multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(existingMultiLicense.getId()))
        .containsExactlyInAnyOrder(license1, license2);
  }

  private LicenseData createLicenseData() {
    LicenseData licenseData = new LicenseData();
    licenseData.licenses = new ArrayList<>();
    licenseData.multiLicenses = new ArrayList<>();
    licenseData.multiLicenseMappings = new LinkedHashMap<>();
    return licenseData;
  }
}
