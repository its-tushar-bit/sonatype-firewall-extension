/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ApplicationDAOTest
    extends AbstractDbDAOTest
{
  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Before
  public void setupApplication() {
    application = new Application();
    application.setName("valid name");
    application.setPublicId("valid public id");
  }

  @After
  public void cleanUp() {
    if (applicationDAO.getById(application.getId()) != null) {
      applicationDAO.delete(application);
    }
  }

  @Test
  public void testCRUD() throws Exception {
    // Create
    createDefaultApplication();

    BufferedImage image = new BufferedImage(420, 420, BufferedImage.TYPE_INT_ARGB);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", byteArrayOutputStream);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    File iconDir = tmpDir.newFolder();
    File appIconDir = new File(iconDir, applicationId);
    Assert.assertFalse(appIconDir.exists());
    new IconDAO().setIcon(applicationId, iconDir, byteArrayInputStream);
    Assert.assertTrue(appIconDir.isDirectory());

    // Get the icon
    byte[] iconBytes = new IconDAO().getIcon(applicationId, iconDir);
    Assert.assertNotNull(iconBytes);
    Assert.assertTrue(iconBytes.length > 0);

    // Update
    Application application = applicationDAO.getById(applicationId);
    application.setName("ApplicationDAOTest New name");
    applicationDAO.update(application);
    application = applicationDAO.getById(applicationId);
    Assert.assertEquals("ApplicationDAOTest New name", application.getName());

    // Get All
    List<Application> applications = applicationDAO.getAll();
    Assert.assertEquals(1, applications.size());
    Assert.assertEquals(applicationId, applications.get(0).getId());

    // Delete
    applicationDAO.deleteWithIcon(application, iconDir);
    application = applicationDAO.getById(applicationId);
    Assert.assertNull(application);
    Assert.assertFalse(appIconDir.getAbsolutePath(), appIconDir.exists());
  }

  @Test
  public void testUpdateOrganizationId() {
    Organization organization1 = createOrganization("testUpdateOrganizationId 1");
    Organization organization2 = createOrganization("testUpdateOrganizationId 2");

    applicationDAO.insert(application);
    application.setOrganizationId(organization1.getId());
    applicationDAO.update(application);
    application = applicationDAO.getById(application.getId());
    assertEquals(organization1.getId(), application.getOrganizationId());
    // Update again with the same organization id - should not fail
    application.setName("testUpdateOrganizationId");
    applicationDAO.update(application);
    application = applicationDAO.getById(application.getId());
    assertEquals(organization1.getId(), application.getOrganizationId());

    // Update with a different organization id - should fail
    application.setOrganizationId(organization2.getId());
    try {
      applicationDAO.update(application);
      fail("Expected InvalidApplicationException");
    }
    catch (InvalidApplicationException expected) {
      assertEquals("Cannot change the parent organization of an application.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullPublicId_Insert() {
    application.setPublicId(null);
    try {
      applicationDAO.insert(application);
      fail("Expected InvalidApplicationException");
    }
    catch (InvalidApplicationException expected) {
      assertEquals("ID is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullPublicId_Update() {
    applicationDAO.insert(application);
    application.setPublicId(" ");
    application.setName(application.getName() + "1");
    try {
      applicationDAO.update(application);
      fail("Expected InvalidApplicationException");
    }
    catch (InvalidApplicationException expected) {
      assertEquals("ID is required.", expected.getMessage());
    }
  }

  @Test
  public void testPublicIdIsCaseInsensitive() {
    String appPublicId = "testPublicIdIsCaseInsensitive";

    Application application = new Application();
    application.setName("test");
    application.setPublicId(appPublicId);
    ApplicationDAO applicationDAO = new ApplicationDAO();
    applicationDAO.insert(application);
    String applicationId = application.getId();

    Assert.assertEquals(appPublicId, application.getPublicId());
    Assert.assertEquals(appPublicId.toLowerCase(Locale.ENGLISH), application.getPublicIdLowercase());

    application = applicationDAO.getById(applicationId);
    Assert.assertNotNull(application);
    Assert.assertEquals(appPublicId, application.getPublicId());
    Assert.assertEquals(appPublicId.toLowerCase(Locale.ENGLISH), application.getPublicIdLowercase());

    application = applicationDAO.getByPublicId(appPublicId);
    Assert.assertNotNull(application);
    Assert.assertEquals(applicationId, application.getId());

    application = applicationDAO.getByPublicId(appPublicId.toLowerCase(Locale.ENGLISH));
    Assert.assertNotNull(application);
    Assert.assertEquals(applicationId, application.getId());

    application = applicationDAO.getByPublicId(appPublicId.toUpperCase(Locale.ENGLISH));
    Assert.assertNotNull(application);
    Assert.assertEquals(applicationId, application.getId());
  }

  @Test
  public void testValidateNullName_Insert() {
    application.setName(null);
    try {
      applicationDAO.insert(application);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullName_Update() {
    application.setName("testValidateNullName");
    assertEquals("testvalidatenullname", application.getNameLowercaseNoWhitespace());
    applicationDAO.insert(application);

    application.setName(null);
    assertNull(application.getNameLowercaseNoWhitespace());
    try {
      applicationDAO.update(application);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Insert() {
    application.setName(" ");
    try {
      applicationDAO.insert(application);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    application.setName("testValidateEmptyName");
    assertEquals("testvalidateemptyname", application.getNameLowercaseNoWhitespace());
    applicationDAO.insert(application);

    application.setName(" ");
    assertEquals("", application.getNameLowercaseNoWhitespace());
    try {
      applicationDAO.update(application);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    for (String name: INVALID_ALPHANUMERIC) {
      application.setName(name);
      try {
        applicationDAO.insert(application);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    application.setName("testValidateNameInvalidChars");
    applicationDAO.insert(application);
    for (String name: INVALID_ALPHANUMERIC) {
      application.setName(name);
      try {
        applicationDAO.update(application);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    for (String name : INVALID_SPACING_NAMES) {
      application.setName(name);
      try {
        applicationDAO.insert(application);
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
    application.setName("testValidateNameSpaces");
    applicationDAO.insert(application);

    for (String name : INVALID_SPACING_NAMES) {
      application.setName(name);
      try {
        applicationDAO.update(application);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must not have leading or trailing spaces, or have two spaces in a row.",
            expected.getMessage());
      }
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    application.setName(name);
    applicationDAO.insert(application);

    assertEquals(name, application.getName());
    assertEquals("teststringwithcaseandwhitespace", application.getNameLowercaseNoWhitespace());

    String name1 = "TEST String      With    cASE and      whitespace";
    Application application1 = applicationDAO.getByName(name1);
    assertNotNull(application1);
    assertEquals(application.getId(), application1.getId());
  }

  @Test
  public void testDuplicateName_Insert() {
    application.setName("testDuplicateName");
    applicationDAO.insert(application);

    Application application1 = new Application();
    application1.setPublicId("testDuplicateName1");
    application1.setName("Test Duplicate Name");
    try {
      applicationDAO.insert(application1);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Test Duplicate Name is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testDuplicateName_Update() {
    application.setName("testDuplicateName");
    applicationDAO.insert(application);

    Application application1 = new Application();
    application1.setPublicId("testpublicid1");
    application1.setName("testDuplicateName1");
    applicationDAO.insert(application1);
    applicationsToDelete.add(application1);

    application1.setName("Test Duplicate Name");
    try {
      applicationDAO.update(application1);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Test Duplicate Name is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testCascadeDeleteToLabels() {
    application.setName("testCascadeDeleteToLabels");
    applicationDAO.insert(application);

    LabelDAO labelDAO = new LabelDAO();
    Label label = new Label(application.getId(), "testCascadeDeleteToLabels", Color.blue);
    labelDAO.insert(label);

    applicationDAO.delete(application);
  }

  @Test
  public void testCascadeDeleteToPolicyWaivers() {
    application.setName("testCascadeDeleteToPolicyWaivers");
    applicationDAO.insert(application);

    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", "MyPolicyId", application.getId(),
        "My comment");
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    policyWaiverDAO.insert(policyWaiver);
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(application.getId());
    assertEquals(1, policyWaivers.size());

    applicationDAO.delete(application);
    policyWaivers = policyWaiverDAO.getByOwnerId(application.getId());
    assertEquals(0, policyWaivers.size());
  }

  @Test
  public void testCascadeDeleteToLicenseOverrides() {
    application.setName("testCascadeDeleteToLicenseOverrides");
    applicationDAO.insert(application);

    LicenseOverride licenseOverride = new LicenseOverride(application.getId(), "groupId", "artifactId", "version",
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    licenseOverrideDAO.insert(licenseOverride);
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(application.getId());
    assertEquals(1, licenseOverrides.size());

    applicationDAO.delete(application);
    licenseOverrides = licenseOverrideDAO.getByOwnerId(application.getId());
    assertEquals(0, licenseOverrides.size());
  }

  @Test
  public void testCascadeDeleteToMembershipMappings() {
    application.setName("testCascadeDeleteToMembershipMappings");
    applicationDAO.insert(application);

    String roleId = new RoleDAO().getApplicationRoles().get(0).getId();
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    membershipMappingDAO.setMembershipMappingsForContextAndRole(application.getId(), roleId,
        Arrays.asList(new MembershipMapping("admin", MemberType.USER)));

    applicationDAO.delete(application);

    assertEquals(Arrays.asList(), membershipMappingDAO.getByContextId(application.getId()));
  }

  @Test
  public void testCascadeDeleteToApplicationTags() {
    createDefaultApplication();

    Tag tag = createTag("testCascadeDeleteToApplicationTags name", "testCascadeDeleteToApplicationTags description",
        organization.getId());

    ApplicationTagDAO appTagDAO = new ApplicationTagDAO();
    ApplicationTag appTag = new ApplicationTag(applicationId, tag.getId());
    appTagDAO.insert(appTag);

    applicationDAO.delete(application);

    assertThat(appTagDAO.getByApplicationId(applicationId), is(empty()));
  }

  @Test
  public void testConflictingLicenseThreatGroups() {
    application.setName("testConflictingLicenseThreatGroups");
    applicationDAO.insert(application);
    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    LicenseThreatGroup appLicenseThreatGroup = new LicenseThreatGroup(application.getId(),
        "testConflictingLicenseThreatGroups", 2);
    licenseThreatGroupDAO.insert(appLicenseThreatGroup);

    organization = createOrganization("testConflictingLicenseThreatGroups");
    LicenseThreatGroup orgLicenseThreatGroup = new LicenseThreatGroup(organization.getId(),
        "test conflictingLicenseThreatGroups", 4);
    licenseThreatGroupDAO.insert(orgLicenseThreatGroup);

    application.setOrganizationId(organization.getId());
    try {
      applicationDAO.update(application);
      fail("Expected InvalidApplicationException");
    }
    catch (InvalidApplicationException expected) {
      assertEquals(
          "Both the application and the organization have a license threat group with the same name 'testConflictingLicenseThreatGroups'",
          expected.getMessage());
    }
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    application.setName(name + "a");
    try {
      applicationDAO.insert(application);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    application.setName(name);
    applicationDAO.insert(application);
  }

  @Test
  public void testValidateNameLength_Update() {
    applicationDAO.insert(application);

    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    application.setName(name + "a");
    try {
      applicationDAO.update(application);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    application.setName(name);
    applicationDAO.update(application);
  }

  @Test
  public void testCascadeDeleteToPolicyMonitoring() {
    application.setName("testCascadeDeleteToPolicyMonitoring");
    applicationDAO.insert(application);

    PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(application.getId(), Stage.ID_RELEASE);
    policyMonitoringDAO.insert(policyMonitoring);
    assertThat(policyMonitoringDAO.getByOwnerId(application.getId()), is(notNullValue()));

    applicationDAO.delete(application);

    assertThat(policyMonitoringDAO.getByOwnerId(application.getId()), is(nullValue()));
  }
}
