/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultLicenseDataUpdaterTest
    extends AbstractBrainServiceTest
{
  private static final License license1 = new License("license1", "l1Short", "l1Long");

  private static final License license2 = new License("license2", "l2Short", "l2Long");

  private MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

  private LicenseDAO licenseDAO = new LicenseDAO();

  @Test
  public void testLicense() throws Exception {
    LicenseData licenseData = createLicenseData();
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);
    String newId = "New license id";
    LicenseDAO licenseDAO = new LicenseDAO();
    assertNull(licenseDAO.getById(newId));

    License newLicense = new License();
    newLicense.setId(newId);
    newLicense.setShortDisplayName("New short name");
    newLicense.setLongDisplayName("New long name");
    licenseData.licenses.add(newLicense);
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);
    assertNotNull(licenseDAO.getById(newId));
  }

  @Test
  public void testMultiLicense_ById() throws Exception {
    LicenseData licenseData = createLicenseData();
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);
    String newId = "New license id1";
    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
    assertNull(multiLicenseDAO.getById(newId));

    MultiLicense newMultiLicense = new MultiLicense();
    newMultiLicense.setId(newId);
    newMultiLicense.setShortDisplayName("New short name1");
    newMultiLicense.setLongDisplayName("New long name");
    licenseData.multiLicenses.add(newMultiLicense);
    Set<String> multiLicenseMappings = new LinkedHashSet<>();
    multiLicenseMappings.add("GPL-2.0");
    licenseData.multiLicenseMappings.put(newId, multiLicenseMappings);
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);
    assertNotNull(multiLicenseDAO.getById(newId));
    assertEquals("GPL-2.0", multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(newId).iterator().next().getId());
  }

  @Test
  public void testMultiLicense_ByName() throws Exception {
    LicenseData licenseData = createLicenseData();
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);
    String newId = "New license id2";
    String newName = "New short name2";
    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
    assertNull(multiLicenseDAO.getByName(newName));

    MultiLicense newMultiLicense = new MultiLicense();
    newMultiLicense.setId(newId);
    newMultiLicense.setShortDisplayName(newName);
    newMultiLicense.setLongDisplayName("New long name");
    licenseData.multiLicenses.add(newMultiLicense);
    Set<String> multiLicenseMappings = new LinkedHashSet<>();
    multiLicenseMappings.add("GPL-2.0");
    licenseData.multiLicenseMappings.put(newId, multiLicenseMappings);
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);
    assertNotNull(multiLicenseDAO.getByName(newName));
    assertEquals("GPL-2.0", multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(newId).iterator().next().getId());
  }

  @Test
  public void testNoHdsServer() throws Exception {
    getHdsServer().stop();

    try {
      String newId = "New license id";
      MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
      try {
        multiLicenseDAO.getById(newId);
        fail("Expected RuntimeException");
      }
      catch (RuntimeException e) {
        assertTrue(e.getMessage(), e.getMessage().startsWith("Could not retrieve license data from Sonatype HDS:"));
      }
    }
    finally {
      getHdsServer().start();
    }
  }

  @Test
  public void testMultiLicense_WithNullMappedLicenses_IsNotAdded() throws Exception {
    LicenseData licenseData = createLicenseData();
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);
    MultiLicense mlWithNullMappedLicenses = new MultiLicense("mlWithNullMappedLicenses",
        "mlWithNullMappedLicensesShort", "mlWithNullMappedLicensesLong");
    assertThat(multiLicenseDAO.getById(mlWithNullMappedLicenses.getId()), is(nullValue()));

    licenseData.multiLicenses.add(mlWithNullMappedLicenses);
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);

    assertThat(multiLicenseDAO.getById(mlWithNullMappedLicenses.getId()), is(nullValue()));
  }

  @Test
  public void testMultiLicense_WithEmptyMappedLicenses_IsNotAdded() throws Exception {
    LicenseData licenseData = createLicenseData();
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);
    MultiLicense mlWithEmptyMappedLicenses = new MultiLicense("mlWithEmptyMappedLicenses",
        "mlWithEmptyMappedLicensesShort", "mlWithEmptyMappedLicensesLong");
    assertThat(multiLicenseDAO.getById(mlWithEmptyMappedLicenses.getId()), is(nullValue()));

    licenseData.multiLicenses.add(mlWithEmptyMappedLicenses);
    licenseData.multiLicenseMappings.put(mlWithEmptyMappedLicenses.getId(), new HashSet<String>());
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);

    assertThat(multiLicenseDAO.getById(mlWithEmptyMappedLicenses.getId()), is(nullValue()));
  }

  @Test
  public void testExistingMultiLicense_CanHaveMappedLicenseAdded() throws Exception {
    LicenseData licenseData = createLicenseData();
    MultiLicense existingMultiLicense = new MultiLicense("existingMultiLicense", "existingMultiLicenseShort",
        "existingMultiLicenseLong");
    licenseData.licenses.add(license1);
    licenseData.licenses.add(license2);
    licenseData.multiLicenses.add(existingMultiLicense);
    licenseData.multiLicenseMappings.put(existingMultiLicense.getId(), new HashSet<>(Arrays.asList(license1.getId())));
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);
    MultiLicense storedMultiLicense = multiLicenseDAO.getById(existingMultiLicense.getId());
    assertThat(licenseDAO.getAll(), hasItems(license1, license2));
    assertThat(storedMultiLicense, is(equalTo(existingMultiLicense)));
    assertThat(multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(existingMultiLicense.getId()), contains(license1));

    licenseData.multiLicenseMappings.get(existingMultiLicense.getId()).add(license2.getId());
    setHdsResponseForURI(DefaultLicenseDataUpdater.HDS_LICENSE_PATH, licenseData, 200);
    LicenseDataUpdater.update();
    storedMultiLicense = multiLicenseDAO.getById(existingMultiLicense.getId());
    assertThat(storedMultiLicense, is(equalTo(existingMultiLicense)));
    assertThat(multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(existingMultiLicense.getId()),
        containsInAnyOrder(license1, license2));
  }

  private LicenseData createLicenseData() {
    LicenseData licenseData = new LicenseData();
    licenseData.licenses = new ArrayList<>();
    licenseData.multiLicenses = new ArrayList<>();
    licenseData.multiLicenseMappings = new LinkedHashMap<>();
    return licenseData;
  }
}
