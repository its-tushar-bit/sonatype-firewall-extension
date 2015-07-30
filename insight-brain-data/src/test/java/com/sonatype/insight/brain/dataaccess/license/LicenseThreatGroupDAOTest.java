/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;
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

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
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
  public void testCreateDefaultLicenseThreatGroups() throws Exception {
    List<LicenseThreatGroup> licenseThreatGroups =
        licenseThreatGroupDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Assert.assertEquals(LicenseThreatGroupDAO.DEFAULT_LICENSE_THREAT_GROUP_COUNT, licenseThreatGroups.size());
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
  public void testInsertLTGInApplication_ClashesWithApplication() throws Exception {
    // Add a group
    tempEntity.newLicenseThreatGroup(applicationId, "My group", 4);

    // Add another group with the same name
    LicenseThreatGroup group = newLicenseThreatGroup(applicationId, "mygroup");
    try {
      licenseThreatGroupDAO.insert(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
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
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();

    // Add a group
    LicenseThreatGroup group1 = tempEntity.newLicenseThreatGroup(applicationId, "My group 1", 4);

    // Add another group
    LicenseThreatGroup group2 = tempEntity.newLicenseThreatGroup(applicationId, "My group 2", 4);

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
      if (!"The threat level must be a number between 0 and 10.".equals(expected.getMessage())) {
        throw expected;
      }
    }

    group.setThreatLevel(11);
    try {
      dao.insert(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"The threat level must be a number between 0 and 10.".equals(expected.getMessage())) {
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
      if (!"The threat level must be a number between 0 and 10.".equals(expected.getMessage())) {
        throw expected;
      }
    }

    group.setThreatLevel(11);
    try {
      dao.update(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      if (!"The threat level must be a number between 0 and 10.".equals(expected.getMessage())) {
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
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      LicenseThreatGroup group = new LicenseThreatGroup(applicationId, name, 5);
      try {
        dao.insert(group);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0)), expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = new LicenseThreatGroup(applicationId, "testValidateNameInvalidChars", 5);
    dao.insert(group);
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      group.setName(name);
      try {
        dao.update(group);
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
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    LicenseThreatGroup group = tempEntity.newLicenseThreatGroup(applicationId, "a", 5);
    for (String name : NameHelperTest.VALID_NAMES) {
      group.setName(name);
      dao.update(group);
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
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

    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
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
      assertEquals("Cannot find a license threat group with ID fake id.", expected.getMessage());
    }
  }

  private void assertUpdateLicenseThreatGroupWithDuplicateName(final String ownerId, final LicenseThreatGroup group,
      final String groupName, final Owner expectedOwner)
  {
    // Update without changing the name
    group.setThreatLevel(6);
    licenseThreatGroupDAO.update(group);
    assertLicenseThreatGroup(ownerId, group.getName(), 6, group);

    // Update the group with a case-/whitespace-equivalent name
    group.setName(groupName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      licenseThreatGroupDAO.update(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      assertThat(expected.getMessage(),
          is("A license threat group with the same name already exists for the " + expectedOwner.getType() + " '"
              + expectedOwner.getName() + "'."));
    }
  }

  private void assertInsertLicenseThreatGroupWithDuplicateName(final String ownerId, final String groupName,
      final Owner expectedOwner)
  {
    // Add a group with a case-/whitespace-equivalent name
    LicenseThreatGroup group = newLicenseThreatGroup(ownerId,
        groupName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      licenseThreatGroupDAO.insert(group);
      Assert.fail("Expected InvalidLicenseThreatGroupException");
    }
    catch (InvalidLicenseThreatGroupException expected) {
      assertThat(expected.getMessage(),
          is("A license threat group with the same name already exists for the " + expectedOwner.getType() + " '"
              + expectedOwner.getName() + "'."));
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
