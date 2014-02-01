/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class LicenseThreatGroupDAOTest
    extends AbstractDbDAOTest
{
  private void testCRUD(String ownerId) throws Exception {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    // Create
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("My group");
    group.setThreatLevel(4);
    dao.insert(group);
    Assert.assertNotNull(group.getId());

    group = dao.getById(group.getId());
    Assert.assertNotNull(group);
    assertLicenseThreatGroup(ownerId, "My group", 4, group);

    // Update
    group.setName("My updated name");
    dao.update(group);

    group = dao.getById(group.getId());
    Assert.assertNotNull(group);
    assertLicenseThreatGroup(ownerId, "My updated name", 4, group);

    // Delete
    dao.delete(group);

    group = dao.getById(group.getId());
    Assert.assertNull(group);
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
  public void testCascadeDelete() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

    // Create
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(4);
    dao.insert(group);
    Assert.assertNotNull(group.getId());

    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setOwnerId(applicationId);
    licenseThreatGroupLicense.setLicenseThreatGroupId(group.getId());
    licenseThreatGroupLicense.setLicenseId("UNSPECIFIED");
    licenseThreatGroupLicenseDAO.insert(licenseThreatGroupLicense);

    // Delete
    dao.delete(group);

    group = dao.getById(group.getId());
    Assert.assertNull(group);
  }

  @Test
  public void testInsertDuplicateName_Application() throws Exception {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    // Add a group
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(4);
    dao.insert(group);

    // Add another group with the same name
    group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(5);
    try {
      dao.insert(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"A license threat group with the same name already exists".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testInsertDuplicateName_ApplicationOrganization() throws Exception {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    // Add a group to the organization
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(organization.getId());
    group.setName("My group");
    group.setThreatLevel(4);
    dao.insert(group);

    // Add another group with the same name to the application
    group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(5);
    try {
      dao.insert(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"A license threat group with the same name already exists for the parent organization".equals(expected
          .getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testInsertDuplicateName_OrganizationApplication() throws Exception {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    // Add a group to the application
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(4);
    dao.insert(group);

    // Add another group with the same name to the organization
    group = new LicenseThreatGroup();
    group.setOwnerId(organization.getId());
    group.setName("My group");
    group.setThreatLevel(5);
    try {
      dao.insert(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"A license threat group with the same name already exists for application 'AbstractDbDAOTest-AppName'"
          .equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testUpdateDuplicateName_Application() throws Exception {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    // Add a group
    LicenseThreatGroup group1 = new LicenseThreatGroup();
    group1.setOwnerId(applicationId);
    group1.setName("My group 1");
    group1.setThreatLevel(4);
    dao.insert(group1);

    // Add another group
    LicenseThreatGroup group2 = new LicenseThreatGroup();
    group2.setOwnerId(applicationId);
    group2.setName("My group 2");
    group2.setThreatLevel(4);
    dao.insert(group2);

    // Update without changing the name
    group2.setThreatLevel(6);
    dao.update(group2);
    assertLicenseThreatGroup(applicationId, "My group 2", 6, group2);

    // Update with a conflicting name
    group2.setName(group1.getName());
    try {
      dao.update(group2);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"A license threat group with the same name already exists".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testUpdateDuplicateName_ApplicationOrganization() throws Exception {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    // Add a group to the organization
    LicenseThreatGroup group1 = new LicenseThreatGroup();
    group1.setOwnerId(organization.getId());
    group1.setName("My group 1");
    group1.setThreatLevel(4);
    dao.insert(group1);

    // Add another group to the application
    LicenseThreatGroup group2 = new LicenseThreatGroup();
    group2.setOwnerId(applicationId);
    group2.setName("My group 2");
    group2.setThreatLevel(4);
    dao.insert(group2);

    // Update without changing the name
    group2.setThreatLevel(6);
    dao.update(group2);
    assertLicenseThreatGroup(applicationId, "My group 2", 6, group2);

    // Update with a conflicting name
    group2.setName(group1.getName());
    try {
      dao.update(group2);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"A license threat group with the same name already exists for the parent organization".equals(expected
          .getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testUpdateDuplicateName_OrganizationApplication() throws Exception {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    // Add a group to the organization
    LicenseThreatGroup group1 = new LicenseThreatGroup();
    group1.setOwnerId(organization.getId());
    group1.setName("My group 1");
    group1.setThreatLevel(4);
    dao.insert(group1);

    // Add another group to the application
    LicenseThreatGroup group2 = new LicenseThreatGroup();
    group2.setOwnerId(applicationId);
    group2.setName("My group 2");
    group2.setThreatLevel(4);
    dao.insert(group2);

    // Update without changing the name
    group1.setThreatLevel(6);
    dao.update(group1);
    assertLicenseThreatGroup(organization.getId(), "My group 1", 6, group1);

    // Update with a conflicting name
    group1.setName(group2.getName());
    try {
      dao.update(group1);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"A license threat group with the same name already exists for application 'AbstractDbDAOTest-AppName'"
          .equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testInsertInvalidThreatLevel() throws Exception {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(-1);
    try {
      dao.insert(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"The threat level must be a number between 0 and 10".equals(expected.getMessage())) {
        throw expected;
      }
    }

    group.setThreatLevel(11);
    try {
      dao.insert(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"The threat level must be a number between 0 and 10".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testUpdateInvalidThreatLevel() throws Exception {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(1);
    dao.insert(group);
    group.setThreatLevel(-1);
    try {
      dao.update(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"The threat level must be a number between 0 and 10".equals(expected.getMessage())) {
        throw expected;
      }
    }

    group.setThreatLevel(11);
    try {
      dao.update(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"The threat level must be a number between 0 and 10".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  private void assertLicenseThreatGroup(String applicationId, String name, int threatLevel, LicenseThreatGroup actual) {
    Assert.assertEquals(applicationId, actual.getOwnerId());
    Assert.assertEquals(name, actual.getName());
    Assert.assertEquals(threatLevel, actual.getThreatLevel());
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, name, 5);
    dao.insert(group);

    assertEquals(name, group.getName());
    assertEquals("teststringwithcaseandwhitespace", group.getNameLowercaseNoWhitespace());

    String name1 = "TEST String      With    cASE and      whitespace";
    LicenseThreatGroup group1 = dao.getByOwnerIdAndName(applicationId, name1);
    assertNotNull(group1);
    assertEquals(group.getId(), group1.getId());
  }

  @Test
  public void testValidateEmptyName_Insert() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, " ", 5);
    try {
      dao.insert(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateEmptyName", 5);
    assertEquals("testvalidateemptyname", group.getNameLowercaseNoWhitespace());
    dao.insert(group);

    group.setName(" ");
    assertEquals("", group.getNameLowercaseNoWhitespace());
    try {
      dao.update(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    for (String name: INVALID_ALPHANUMERIC) {
      LicenseThreatGroup group = new LicenseThreatGroup(applicationId, name, 5);
      try {
        dao.insert(group);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateNameInvalidChars", 5);
    dao.insert(group);
    for (String name: INVALID_ALPHANUMERIC) {
      group.setName(name);
      try {
        dao.update(group);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    for (String name : INVALID_SPACING_NAMES) {
      LicenseThreatGroup group = new LicenseThreatGroup(applicationId, name, 5);
      try {
        dao.insert(group);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must not have leading or trailing spaces, or have two spaces in a row.",
            expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameSpaces_Update() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateNameSpaces", 5);
    dao.insert(group);

    for (String name : INVALID_SPACING_NAMES) {
      group.setName(name);
      try {
        dao.update(group);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must not have leading or trailing spaces, or have two spaces in a row.",
            expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNullName_Insert() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, null /* name */, 5);
    try {
      dao.insert(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullName_Update() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateNullName", 5);
    assertEquals("testvalidatenullname", group.getNameLowercaseNoWhitespace());
    dao.insert(group);

    group.setName(null);
    assertNull(group.getNameLowercaseNoWhitespace());
    try {
      dao.update(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);

    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, name + "a", 5);
    try {
      dao.insert(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    group.setName(name);
    dao.insert(group);
  }

  @Test
  public void testValidateNameLength_Update() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);

    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateNameLengthUpdate", 5);
    dao.insert(group);

    group.setName(name + "a");
    try {
      dao.update(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    group.setName(name);
    dao.update(group);
  }

  @Test
  public void testGetByIdNotNull() {
    try {
      new LicenseThreatGroupDAO().getByIdNotNull("fake id");
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("Cannot find a license threat group with id fake id", expected.getMessage());
    }
  }
}
