/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class LicenseThreatGroupDAOTest
    extends AbstractDbDAOTest
{
  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private void testCRUD(String ownerId) throws Exception {
    // Create
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("My group");
    group.setThreatLevel(4);
    licenseThreatGroupDAO.insert(group);
    assertNotNull(group.getId());

    group = licenseThreatGroupDAO.getById(group.getId());
    assertNotNull(group);
    assertLicenseThreatGroup(ownerId, "My group", 4, group);

    // Update
    group.setName("My updated name");
    licenseThreatGroupDAO.update(group);

    group = licenseThreatGroupDAO.getById(group.getId());
    assertNotNull(group);
    assertLicenseThreatGroup(ownerId, "My updated name", 4, group);

    // Delete
    licenseThreatGroupDAO.delete(group);

    group = licenseThreatGroupDAO.getById(group.getId());
    assertNull(group);
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
    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

    // Create
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(4);
    licenseThreatGroupDAO.insert(group);
    assertNotNull(group.getId());

    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
    licenseThreatGroupLicense.setOwnerId(applicationId);
    licenseThreatGroupLicense.setLicenseThreatGroupId(group.getId());
    licenseThreatGroupLicense.setLicenseId("UNSPECIFIED");
    licenseThreatGroupLicenseDAO.insert(licenseThreatGroupLicense);

    // Delete
    licenseThreatGroupDAO.delete(group);

    group = licenseThreatGroupDAO.getById(group.getId());
    assertNull(group);
  }

  @Test
  public void testInsertLTGInApplication_ClashesWithApplication() throws Exception {
    // Add a group
    tempEntity.newLicenseThreatGroup(applicationId, "My group", 4);

    // Add another group with the same name
    LicenseThreatGroup group = newLicenseThreatGroup(applicationId, "mygroup");
    try {
      licenseThreatGroupDAO.insert(group);
      fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"A license threat group with the same name already exists.".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testInsertLTGInApplication_ClashesWithOrganization() throws Exception {
    // Add a group to the organization
    tempEntity.newLicenseThreatGroup(organization.getId(), "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at application level
    assertInsertLicenseThreatGroupWithDuplicateName(applicationId, "My group", organization);
  }

  @Test
  public void testInsertLTGInOrganization_ClashesWithApplication() throws Exception {
    // Add a group to the application
    tempEntity.newLicenseThreatGroup(applicationId, "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at organization level
    assertInsertLicenseThreatGroupWithDuplicateName(organization.getId(), "My group", application);
  }

  @Test
  public void testInsertLTGInApplication_ClashesWithParentOrganization() throws Exception {
    Organization parentOrganization = organizationDAO.getById(organization.getParentOrganizationId());

    // Add a group to the parent organization
    tempEntity.newLicenseThreatGroup(parentOrganization.getId(), "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at application level
    assertInsertLicenseThreatGroupWithDuplicateName(applicationId, "My group", parentOrganization);
  }

  @Test
  public void testInsertLTGInParentOrganization_ClashesWithApplication() throws Exception {
    // Add a group to the application
    tempEntity.newLicenseThreatGroup(applicationId, "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at parent owner level
    assertInsertLicenseThreatGroupWithDuplicateName(organization.getParentOrganizationId(), "My group", application);
  }

  @Test
  public void testInsertLTGInOrganization_ClashesWithParentOrganization() throws Exception {
    Organization parentOrganization = organizationDAO.getById(organization.getParentOrganizationId());

    // Add a group to the parent organization
    tempEntity.newLicenseThreatGroup(parentOrganization.getId(), "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at organization level
    assertInsertLicenseThreatGroupWithDuplicateName(organization.getId(), "My group", parentOrganization);
  }

  @Test
  public void testInsertLTGInParentOrganization_ClashesWithOrganization() throws Exception {
    // Add a group to the organization
    tempEntity.newLicenseThreatGroup(organization.getId(), "My group", 4);

    // Add another group with a case-/whitespace-equivalent name at parent owner level
    assertInsertLicenseThreatGroupWithDuplicateName(organization.getParentOrganizationId(), "My group", organization);
  }

  @Test
  public void testUpdateLTGInApplication_ClashesWithApplication() throws Exception {
    // Add a group
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(applicationId, "My group 1", 4);

    // Add another group
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(applicationId, "My group 2", 4);

    // Update without changing the name
    group2.setThreatLevel(6);
    licenseThreatGroupDAO.update(group2);
    assertLicenseThreatGroup(applicationId, "My group 2", 6, group2);

    // Update with a conflicting name
    group2.setName(group1.getName());
    try {
      licenseThreatGroupDAO.update(group2);
      fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"A license threat group with the same name already exists.".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testUpdateLTGInApplication_ClashesWithOrganization() throws Exception {
    // Add a group to the organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(organization.getId(), "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(applicationId, "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(applicationId, group2, group1.getName(), organization);
  }

  @Test
  public void testUpdateLTGInOrganization_ClashesWithApplication() throws Exception {
    // Add a group to the organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(organization.getId(), "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(applicationId, "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(organization.getId(), group1, group2.getName(), application);
  }

  @Test
  public void testUpdateLTGInApplication_ClashesWithParentOrganization() throws Exception {
    Organization parentOrganization = organizationDAO.getById(organization.getParentOrganizationId());

    // Add a group to the parent organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(parentOrganization.getId(), "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(applicationId, "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(applicationId, group2, group1.getName(), parentOrganization);
  }

  @Test
  public void testUpdateLTGInParentOrganization_ClashesWithApplication() throws Exception {
    String parentOrganizationId = organization.getParentOrganizationId();

    // Add a group to the parent organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(parentOrganizationId, "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(applicationId, "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(parentOrganizationId, group1, group2.getName(), application);
  }

  //
  @Test
  public void testUpdateLTGInOrganization_ClashesWithParentOrganization() throws Exception {
    Organization parentOrganization = organizationDAO.getById(organization.getParentOrganizationId());

    // Add a group to the parent organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(parentOrganization.getId(), "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(organization.getId(), "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(organization.getId(), group2, group1.getName(), parentOrganization);
  }

  @Test
  public void testUpdateLTGInParentOrganization_ClashesWithOrganization() throws Exception {
    String parentOrganizationId = organization.getParentOrganizationId();

    // Add a group to the parent organization
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(parentOrganizationId, "My group 1", 4);

    // Add another group to the application
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(organization.getId(), "My group 2", 4);

    assertUpdateLicenseThreatGroupWithDuplicateName(parentOrganizationId, group1, group2.getName(), organization);
  }

  @Test
  public void testInsertInvalidThreatLevel() throws Exception {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(-1);
    try {
      licenseThreatGroupDAO.insert(group);
      fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"The threat level must be a number between 0 and 10.".equals(expected.getMessage())) {
        throw expected;
      }
    }

    group.setThreatLevel(11);
    try {
      licenseThreatGroupDAO.insert(group);
      fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"The threat level must be a number between 0 and 10.".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testUpdateInvalidThreatLevel() throws Exception {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(applicationId);
    group.setName("My group");
    group.setThreatLevel(1);
    licenseThreatGroupDAO.insert(group);
    group.setThreatLevel(-1);
    try {
      licenseThreatGroupDAO.update(group);
      fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"The threat level must be a number between 0 and 10.".equals(expected.getMessage())) {
        throw expected;
      }
    }

    group.setThreatLevel(11);
    try {
      licenseThreatGroupDAO.update(group);
      fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"The threat level must be a number between 0 and 10.".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  private void assertLicenseThreatGroup(String applicationId, String name, int threatLevel, LicenseThreatGroup actual) {
    assertEquals(applicationId, actual.getOwnerId());
    assertEquals(name, actual.getName());
    assertEquals(threatLevel, actual.getThreatLevel());
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, name, 5);
    licenseThreatGroupDAO.insert(group);

    assertEquals(name, group.getName());
    assertEquals("teststringwithcaseandwhitespace", group.getNameLowercaseNoWhitespace());

    String name1 = "TEST String      With    cASE and      whitespace";
    LicenseThreatGroup group1 = licenseThreatGroupDAO.getByOwnerIdAndName(applicationId, name1);
    assertNotNull(group1);
    assertEquals(group.getId(), group1.getId());
  }

  @Test
  public void testValidateEmptyName_Insert() {
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, " ", 5);
    try {
      licenseThreatGroupDAO.insert(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateEmptyName", 5);
    assertEquals("testvalidateemptyname", group.getNameLowercaseNoWhitespace());
    licenseThreatGroupDAO.insert(group);

    group.setName(" ");
    assertEquals("", group.getNameLowercaseNoWhitespace());
    try {
      licenseThreatGroupDAO.update(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      LicenseThreatGroup group = new LicenseThreatGroup(applicationId, name, 5);
      try {
        licenseThreatGroupDAO.insert(group);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0)), expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateNameInvalidChars", 5);
    licenseThreatGroupDAO.insert(group);
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      group.setName(name);
      try {
        licenseThreatGroupDAO.update(group);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0)), expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameValidChars_Insert() {
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newLicenseThreatGroup(applicationId, name, 5);
    }
  }

  @Test
  public void testValidateNameValidChars_Update() {
    LicenseThreatGroup group = tempEntity.newLicenseThreatGroup(applicationId, "a", 5);
    for (String name : NameHelperTest.VALID_NAMES) {
      group.setName(name);
      licenseThreatGroupDAO.update(group);
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      LicenseThreatGroup group = new LicenseThreatGroup(applicationId, name, 5);
      try {
        licenseThreatGroupDAO.insert(group);
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
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateNameSpaces", 5);
    licenseThreatGroupDAO.insert(group);

    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      group.setName(name);
      try {
        licenseThreatGroupDAO.update(group);
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
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, null /* name */, 5);
    try {
      licenseThreatGroupDAO.insert(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullName_Update() {
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateNullName", 5);
    assertEquals("testvalidatenullname", group.getNameLowercaseNoWhitespace());
    licenseThreatGroupDAO.insert(group);

    group.setName(null);
    assertNull(group.getNameLowercaseNoWhitespace());
    try {
      licenseThreatGroupDAO.update(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);

    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, name + "a", 5);
    try {
      licenseThreatGroupDAO.insert(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    group.setName(name);
    licenseThreatGroupDAO.insert(group);
  }

  @Test
  public void testValidateNameLength_Update() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);

    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateNameLengthUpdate", 5);
    licenseThreatGroupDAO.insert(group);

    group.setName(name + "a");
    try {
      licenseThreatGroupDAO.update(group);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    group.setName(name);
    licenseThreatGroupDAO.update(group);
  }

  @Test
  public void testGetByIdNotNull() {
    try {
      licenseThreatGroupDAO.getByIdNotNull("fake id");
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("Cannot find a license threat group with ID fake id.", expected.getMessage());
    }
  }

  private void assertUpdateLicenseThreatGroupWithDuplicateName(final String ownerId,
                                                               final LicenseThreatGroup group,
                                                               final String groupName,
                                                               final Owner expectedOwner)
  {
    // Update without changing the name
    group.setThreatLevel(6);
    licenseThreatGroupDAO.update(group);
    assertLicenseThreatGroup(ownerId, group.getName(), 6, group);

    // Update the group with a case-/whitespace-equivalent name
    group.setName(groupName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      licenseThreatGroupDAO.update(group);
      fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      assertThat(expected.getMessage(), is("A license threat group with the same name already exists for the "
          + expectedOwner.getType() + " '" + expectedOwner.getName() + "'."));
    }
  }

  private void assertInsertLicenseThreatGroupWithDuplicateName(final String ownerId,
                                                               final String groupName,
                                                               final Owner expectedOwner)
  {
    // Add a group with a case-/whitespace-equivalent name
    LicenseThreatGroup group = newLicenseThreatGroup(ownerId,
        groupName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      licenseThreatGroupDAO.insert(group);
      fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      assertThat(expected.getMessage(), is("A license threat group with the same name already exists for the "
          + expectedOwner.getType() + " '" + expectedOwner.getName() + "'."));
    }
  }

  private LicenseThreatGroup newLicenseThreatGroup(final String ownerId, final String groupName) {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName(groupName);
    group.setThreatLevel(5);
    return group;
  }
}
