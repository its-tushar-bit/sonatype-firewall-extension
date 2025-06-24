/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.MultiLicense;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultLicenseNameProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultLicenseNameProviderTest
{
  @Mock
  private LicenseDAO licenseDAO;

  @Mock
  private MultiLicenseDAO multiLicenseDAO;

  @InjectMocks
  private DefaultLicenseNameProvider licenseNameProvider;

  private static final String REGULAR_LICENSE_ID = "regular-license";

  private static final String MULTI_LICENSE_ID = "multi-license";

  private static final String UNKNOWN_LICENSE_ID = "unknown-license";

  private static final String REGULAR_LICENSE_NAME = "Regular License";

  private static final String MULTI_LICENSE_NAME = "Multi License";

  @Before
  public void setUp() {
    // Create a regular license
    License license = new License();
    license.setId(REGULAR_LICENSE_ID);
    license.setShortDisplayName(REGULAR_LICENSE_NAME);

    // Create a multi-license
    MultiLicense multiLicense = new MultiLicense();
    multiLicense.setId(MULTI_LICENSE_ID);
    multiLicense.setShortDisplayName(MULTI_LICENSE_NAME);

    // Configure the mocks
    when(licenseDAO.getById(REGULAR_LICENSE_ID)).thenReturn(license);
    when(multiLicenseDAO.getById(MULTI_LICENSE_ID)).thenReturn(multiLicense);
    when(licenseDAO.getById(UNKNOWN_LICENSE_ID)).thenReturn(null);
  }

  @Test
  public void testGetShortDisplayName_RegularLicense() {
    // When getting a regular license name
    String displayName = licenseNameProvider.getShortDisplayName(REGULAR_LICENSE_ID, false);

    // Then it should return the correct name
    assertEquals("Regular license should return its short display name", REGULAR_LICENSE_NAME, displayName);
    System.out.println("[DEBUG_LOG] Regular license test passed: " + displayName);
  }

  @Test
  public void testGetShortDisplayName_MultiLicense() {
    // When getting a multi-license name
    String displayName = licenseNameProvider.getShortDisplayName(MULTI_LICENSE_ID, true);

    // Then it should return the correct name
    assertEquals("Multi-license should return its short display name", MULTI_LICENSE_NAME, displayName);
    System.out.println("[DEBUG_LOG] Multi-license test passed: " + displayName);
  }

  @Test
  public void testGetShortDisplayName_UnknownRegularLicense() {
    // When getting an unknown license name
    String displayName = licenseNameProvider.getShortDisplayName(UNKNOWN_LICENSE_ID, false);

    // Then it should return the license ID
    assertEquals("Unknown license should return its ID", UNKNOWN_LICENSE_ID, displayName);
    System.out.println("[DEBUG_LOG] Unknown license test passed: " + displayName);
  }

  @Test
  public void testGetShortDisplayName_UnknownMultiLicense() {
    // When getting an unknown license name
    String displayName = licenseNameProvider.getShortDisplayName(UNKNOWN_LICENSE_ID, true);

    // Then it should return the license ID
    assertEquals("Unknown license should return its ID", UNKNOWN_LICENSE_ID, displayName);
    System.out.println("[DEBUG_LOG] Unknown license test passed: " + displayName);
  }
}
