/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
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

  @After
  public void after() throws Exception {
    //Since these tests create apps detached from TemporaryEntity, ensure they are deleted
    for(Application app : applicationDAO.getAll()) {
      applicationDAO.delete(app);
    }
  }

  @Test
  public void testCRUD() throws Exception {
    // Create
    Application app = tempEntity.newApplication(organization.getId());

    BufferedImage image = new BufferedImage(420, 420, BufferedImage.TYPE_INT_ARGB);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", byteArrayOutputStream);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    File iconDir = tmpDir.newFolder();
    File appIconDir = new File(iconDir, app.getId());
    Assert.assertFalse(appIconDir.exists());
    new IconDAO().setIcon(app.getId(), iconDir, byteArrayInputStream);
    Assert.assertTrue(appIconDir.isDirectory());

    // Get the icon
    byte[] iconBytes = new IconDAO().getIcon(app.getId(), iconDir);
    Assert.assertNotNull(iconBytes);
    Assert.assertTrue(iconBytes.length > 0);

    // Update
    app = applicationDAO.getById(app.getId());
    app.setName("ApplicationDAOTest New name");
    applicationDAO.update(app);
    app = applicationDAO.getById(app.getId());
    Assert.assertEquals("ApplicationDAOTest New name", app.getName());

    // Delete
    applicationDAO.deleteWithIcon(app, iconDir);
    app = applicationDAO.getById(app.getId());
    Assert.assertNull(app);
    Assert.assertFalse(appIconDir.getAbsolutePath(), appIconDir.exists());
  }

  @Test
  public void testGetAll() throws Exception {
    // Create a few apps
    int appCount = 3;
    tempEntity.newApplications(organization.getId(), appCount);

    // getAll should return appCount + 1, to account for app created by AbstractDbDAOTest
    assertThat(applicationDAO.getAll(), hasSize(appCount + 1));
  }

  @Test
  public void testUpdateOrganizationId() {
    Organization organization1 = tempEntity.newOrganization("testUpdateOrganizationId 1");

    // Update with a different organization id - should fail
    application.setOrganizationId(organization1.getId());
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
    Application app = new Application(null, "name", organization.getId());
    try {
      applicationDAO.insert(app);
      fail("Expected InvalidApplicationException");
    }
    catch (InvalidApplicationException expected) {
      assertEquals("ID is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullPublicId_Update() {
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

    Application app = new Application(appPublicId, "test", organization.getId());
    ApplicationDAO applicationDAO = new ApplicationDAO();
    applicationDAO.insert(app);
    String applicationId = app.getId();

    Assert.assertEquals(appPublicId, app.getPublicId());
    Assert.assertEquals(appPublicId.toLowerCase(Locale.ENGLISH), app.getPublicIdLowercase());

    app = applicationDAO.getById(applicationId);
    Assert.assertNotNull(app);
    Assert.assertEquals(appPublicId, app.getPublicId());
    Assert.assertEquals(appPublicId.toLowerCase(Locale.ENGLISH), app.getPublicIdLowercase());

    app = applicationDAO.getByPublicId(appPublicId);
    Assert.assertNotNull(app);
    Assert.assertEquals(applicationId, app.getId());

    app = applicationDAO.getByPublicId(appPublicId.toLowerCase(Locale.ENGLISH));
    Assert.assertNotNull(app);
    Assert.assertEquals(applicationId, app.getId());

    app = applicationDAO.getByPublicId(appPublicId.toUpperCase(Locale.ENGLISH));
    Assert.assertNotNull(app);
    Assert.assertEquals(applicationId, app.getId());
  }

  @Test
  public void testValidateNullName_Insert() {
    Application app = new Application("publicId", null, organization.getId());
    try {
      applicationDAO.insert(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullName_Update() {
    Application app = new Application("publicId", "testValidateNullName", organization.getId());
    assertEquals("testvalidatenullname", app.getNameLowercaseNoWhitespace());
    applicationDAO.insert(app);

    app.setName(null);
    assertNull(app.getNameLowercaseNoWhitespace());
    try {
      applicationDAO.update(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Insert() {
    Application app = new Application("publicId", " ", organization.getId());
    try {
      applicationDAO.insert(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    Application app = new Application("publicId", "testValidateEmptyName", organization.getId());
    assertEquals("testvalidateemptyname", app.getNameLowercaseNoWhitespace());
    applicationDAO.insert(app);

    app.setName(" ");
    assertEquals("", app.getNameLowercaseNoWhitespace());
    try {
      applicationDAO.update(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    Application app = new Application("publicId", "name", organization.getId());
    for (String name: INVALID_ALPHANUMERIC) {
      app.setName(name);
      try {
        applicationDAO.insert(app);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
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
    Application app = new Application("publicId", "name", organization.getId());
    for (String name : INVALID_SPACING_NAMES) {
      app.setName(name);
      try {
        applicationDAO.insert(app);
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

    Application app = tempEntity.newApplication(name, "publicId", organization.getId());

    assertEquals(name, app.getName());
    assertEquals("teststringwithcaseandwhitespace", app.getNameLowercaseNoWhitespace());

    String name1 = "TEST String      With    cASE and      whitespace";
    Application application1 = applicationDAO.getByName(name1);
    assertNotNull(application1);
    assertEquals(app.getId(), application1.getId());
  }

  @Test
  public void testDuplicateName_Insert() {
    tempEntity.newApplication("testDuplicateName", "publicId", organization.getId());
    try {
      tempEntity.newApplication("Test Duplicate Name", "publicId2", organization.getId());
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Test Duplicate Name is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testDuplicateName_Update() {
    tempEntity.newApplication("testDuplicateName", "publicId", organization.getId());

    Application application1 = tempEntity.newApplication(application.getOrganizationId());
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
    LabelDAO labelDAO = new LabelDAO();
    Label label = new Label(application.getId(), "testCascadeDeleteToLabels", Color.blue);
    labelDAO.insert(label);

    applicationDAO.delete(application);
    assertThat(labelDAO.getByOwnerId(application.getId()), hasSize(0));
  }

  @Test
  public void testCascadeDeleteToPolicyWaivers() {
    Policy policy = tempEntity.newPolicy(application.getId(), "testCascadeDeleteToPolicyWaivers");
    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policy.getId(), application.getId(),
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
  public void testCascadeDeleteToPolicyEvaluations() {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "testCascadeDeleteToPolicyEvaluations");

    applicationDAO.delete(application);
    policyEvaluation = new PolicyEvaluationDAO().getById(policyEvaluation.getId());
    assertThat(policyEvaluation, is(nullValue()));
  }

  @Test
  public void testCascadeDeleteToPolicies() {
    tempEntity.newPolicy(application.getId(), "testCascadeDeleteToPolicies");
    PolicyDAO policyDAO = new PolicyDAO();
    List<Policy> policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies, hasSize(1));

    applicationDAO.delete(application);
    policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies, is(empty()));
  }

  @Test
  public void testCascadeDeleteToLicenseOverrides() {
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
    String roleId = new RoleDAO().getApplicationRoles().get(0).getId();
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    membershipMappingDAO.setMembershipMappingsForContextAndRole(application.getId(), roleId,
        Arrays.asList(new MembershipMapping("admin", MemberType.USER)));

    applicationDAO.delete(application);

    assertEquals(Arrays.asList(), membershipMappingDAO.getByContextId(application.getId()));
  }

  @Test
  public void testCascadeDeleteToApplicationTags() {
    Tag tag = tempEntity.newTag(organization.getId());

    ApplicationTagDAO appTagDAO = new ApplicationTagDAO();
    ApplicationTag appTag = new ApplicationTag(applicationId, tag.getId());
    appTagDAO.insert(appTag);

    applicationDAO.delete(application);

    assertThat(appTagDAO.getByApplicationId(applicationId), is(empty()));
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    Application app = new Application("publicId", name + "a", organization.getId());
    try {
      applicationDAO.insert(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    app.setName(name);
    applicationDAO.insert(app);
  }

  @Test
  public void testValidateNameLength_Update() {
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
    PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(application.getId(), Stage.ID_RELEASE);
    policyMonitoringDAO.insert(policyMonitoring);
    assertThat(policyMonitoringDAO.getByOwnerId(application.getId()), is(notNullValue()));

    applicationDAO.delete(application);

    assertThat(policyMonitoringDAO.getByOwnerId(application.getId()), is(nullValue()));
  }
}
