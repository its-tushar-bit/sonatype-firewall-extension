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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

/**
 * Tests for {@link DefaultLicenseNameProvider}.
 */
@ExtendWith(MockitoExtension.class)
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

  @BeforeEach
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
    lenient().when(licenseDAO.getById(REGULAR_LICENSE_ID)).thenReturn(license);
    lenient().when(multiLicenseDAO.getById(MULTI_LICENSE_ID)).thenReturn(multiLicense);
    lenient().when(licenseDAO.getById(UNKNOWN_LICENSE_ID)).thenReturn(null);
  }

  @Test
  public void testGetShortDisplayName_RegularLicense() {
    // When getting a regular license name
    String displayName = licenseNameProvider.getShortDisplayName(REGULAR_LICENSE_ID, false);

    // Then it should return the correct name
    assertEquals(REGULAR_LICENSE_NAME, displayName, "Regular license should return its short display name");
    System.out.println("[DEBUG_LOG] Regular license test passed: " + displayName);
  }

  @Test
  public void testGetShortDisplayName_MultiLicense() {
    // When getting a multi-license name
    String displayName = licenseNameProvider.getShortDisplayName(MULTI_LICENSE_ID, true);

    // Then it should return the correct name
    assertEquals(MULTI_LICENSE_NAME, displayName, "Multi-license should return its short display name");
    System.out.println("[DEBUG_LOG] Multi-license test passed: " + displayName);
  }

  @Test
  public void testGetShortDisplayName_UnknownRegularLicense() {
    // When getting an unknown license name
    String displayName = licenseNameProvider.getShortDisplayName(UNKNOWN_LICENSE_ID, false);

    // Then it should return the license ID
    assertEquals(UNKNOWN_LICENSE_ID, displayName, "Unknown license should return its ID");
    System.out.println("[DEBUG_LOG] Unknown license test passed: " + displayName);
  }

  @Test
  public void testGetShortDisplayName_UnknownMultiLicense() {
    // When getting an unknown license name
    String displayName = licenseNameProvider.getShortDisplayName(UNKNOWN_LICENSE_ID, true);

    // Then it should return the license ID
    assertEquals(UNKNOWN_LICENSE_ID, displayName, "Unknown license should return its ID");
    System.out.println("[DEBUG_LOG] Unknown license test passed: " + displayName);
  }
}
