/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @since 1.6
 */
public class LicenseOverrideDAOTest
    extends AbstractDbDAOTest
{
  private void testCRUD(String ownerId) throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();
    String groupId = "g";
    String artifactId = "a";
    String version = "v";
    LicenseOverrideStatus status = LicenseOverrideStatus.OVERRIDDEN;
    String licenseId = "Apache-2.0";
    String comment = null;

    // Create
    LicenseOverride licenseOverride = new LicenseOverride(ownerId, groupId, artifactId, version, status, licenseId,
        comment);
    dao.insert(licenseOverride);
    assertNotNull(licenseOverride.getId());
    licenseOverride = dao.getById(licenseOverride.getId());
    assertNotNull(licenseOverride);
    assertLicenseOverride(ownerId, groupId, artifactId, version, status, licenseId, comment, licenseOverride);

    // Update
    comment = "No comments";
    licenseOverride.setComment(comment);
    dao.update(licenseOverride);
    licenseOverride = dao.getById(licenseOverride.getId());
    assertNotNull(licenseOverride);
    assertLicenseOverride(ownerId, groupId, artifactId, version, status, licenseId, comment, licenseOverride);

    // Delete
    dao.delete(licenseOverride);
    licenseOverride = dao.getById(licenseOverride.getId());
    assertNull(licenseOverride);
  }

  private void assertLicenseOverride(String ownerId, String groupId, String artifactId, String version,
      LicenseOverrideStatus status, String licenseId, String comment, LicenseOverride actual)
  {
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(groupId, actual.getGroupId());
    assertEquals(artifactId, actual.getArtifactId());
    assertEquals(version, actual.getVersion());
    assertEquals(status, actual.getStatus());
    assertEquals(licenseId, actual.getLicenseId());
    assertEquals(comment, actual.getComment());
  }

  @Test
  public void testCRUD_Application() throws Exception {
    testCRUD(applicationId);
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    testCRUD(organization.getId());
  }

  @Test
  public void testCommentTooLong() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();
    LicenseOverride override = new LicenseOverride(applicationId, "gid", "aid", "1.0", LicenseOverrideStatus.OPEN,
        null, StringUtils.repeat("X", LicenseOverrideDAO.MAX_COMMENT_SIZE + 1));
    try {
      dao.insert(override);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("Comment length must not exceed 1000 characters", expected.getMessage());
    }
    override.setComment(StringUtils.repeat("X", LicenseOverrideDAO.MAX_COMMENT_SIZE));
    dao.insert(override);
    override.setComment(StringUtils.repeat("X", LicenseOverrideDAO.MAX_COMMENT_SIZE + 1));
    try {
      dao.update(override);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("Comment length must not exceed 1000 characters", expected.getMessage());
    }
  }

  @Test
  public void testInvalidLicenseId_Insert() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    LicenseOverride override = new LicenseOverride(applicationId, "gid", "aid", "1.0",
        LicenseOverrideStatus.OVERRIDDEN, "FataMorganaId", "My comment");
    try {
      dao.insert(override);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("A license with id 'FataMorganaId' does not exist.", expected.getMessage());
    }

    override = new LicenseOverride(applicationId, "gid", "aid", "1.0", LicenseOverrideStatus.SELECTED,
        "FataMorganaId", "My comment");
    try {
      dao.insert(override);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("A license with id 'FataMorganaId' does not exist.", expected.getMessage());
    }
  }

  @Test
  public void testInvalidLicenseId_Update() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    LicenseOverride override = new LicenseOverride(applicationId, "gid", "aid", "1.0",
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    dao.insert(override);

    override.setLicenseId("FataMorganaId");
    try {
      dao.update(override);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("A license with id 'FataMorganaId' does not exist.", expected.getMessage());
    }

    override.setStatus(LicenseOverrideStatus.SELECTED);
    try {
      dao.update(override);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("A license with id 'FataMorganaId' does not exist.", expected.getMessage());
    }
  }

  @Test
  public void testNullLicenseId_Insert() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      LicenseOverride override = new LicenseOverride(applicationId, "gid", "aid", "1.0", status, null /* licenseId */,
          "My comment");
      switch (status) {
        case ACKNOWLEDGED:
        case CONFIRMED:
        case OPEN:
          dao.insert(override);
          dao.delete(override);
          break;
        case OVERRIDDEN:
        case SELECTED:
          try {
            dao.insert(override);
            fail("Expected BadRequestException");
          }
          catch (BadRequestException expected) {
            assertEquals("Expected not null license id for license override", expected.getMessage());
          }
          break;
        default:
          fail("Unknown license override status: " + status.getId());
      }
    }
  }

  @Test
  public void testNullLicenseId_Update() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();
    LicenseOverride override = new LicenseOverride(applicationId, "gid", "aid", "1.0", LicenseOverrideStatus.OPEN,
        null /* licenseId */, "My comment");
    dao.insert(override);

    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      override.setStatus(status);
      switch (status) {
        case ACKNOWLEDGED:
        case CONFIRMED:
        case OPEN:
          dao.update(override);
          break;
        case OVERRIDDEN:
        case SELECTED:
          try {
            dao.update(override);
            fail("Expected BadRequestException");
          }
          catch (BadRequestException expected) {
            assertEquals("Expected not null license id for license override", expected.getMessage());
          }
          break;
        default:
          fail("Unknown license override status: " + status.getId());
      }
    }
  }

  @Test
  public void testNotNullLicenseId_Insert() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();

    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      LicenseOverride override = new LicenseOverride(applicationId, "gid", "aid", "1.0", status, "Apache-2.0",
          "My comment");
      switch (status) {
        case ACKNOWLEDGED:
        case CONFIRMED:
        case OPEN:
          try {
            dao.insert(override);
            fail("Expected BadRequestException");
          }
          catch (BadRequestException expected) {
            assertEquals("Expected null license id for license override", expected.getMessage());
          }
          break;
        case OVERRIDDEN:
        case SELECTED:
          dao.insert(override);
          dao.delete(override);
          break;
        default:
          fail("Unknown license override status: " + status.getId());
      }
    }
  }

  @Test
  public void testNotNullLicenseId_Update() throws Exception {
    LicenseOverrideDAO dao = new LicenseOverrideDAO();
    LicenseOverride override = new LicenseOverride(applicationId, "gid", "aid", "1.0",
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    dao.insert(override);

    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      override.setStatus(status);
      switch (status) {
        case ACKNOWLEDGED:
        case CONFIRMED:
        case OPEN:
          try {
            dao.update(override);
            fail("Expected BadRequestException");
          }
          catch (BadRequestException expected) {
            assertEquals("Expected null license id for license override", expected.getMessage());
          }
          break;
        case OVERRIDDEN:
        case SELECTED:
          dao.update(override);
          break;
        default:
          fail("Unknown license override status: " + status.getId());
      }
    }
  }
}
