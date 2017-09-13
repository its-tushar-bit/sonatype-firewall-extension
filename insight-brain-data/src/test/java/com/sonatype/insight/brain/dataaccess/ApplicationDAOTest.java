/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationResolutionStateDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationResolutionState;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

public class ApplicationDAOTest
    extends AbstractDbDAOTest
{
  /** Prohibited application public ID whitespace characters. */
  public static final char[] PUBLIC_ID_WHITESPACE_CHARS = { '\t', '\n', '\u000B', '\f', '\r' };

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @After
  public void after() throws Exception {
    // Since these tests create apps detached from TemporaryEntity, ensure they are deleted
    for (Application app : applicationDAO.getAll()) {
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
    assertThat(appIconDir.exists(), is(false));
    new IconDAO().setIcon(app.getId(), iconDir, byteArrayInputStream);
    assertThat(appIconDir.isDirectory(), is(true));

    // Get the icon
    byte[] iconBytes = new IconDAO().getIcon(app.getId(), iconDir);
    assertThat(iconBytes, notNullValue());
    assertThat(iconBytes.length, greaterThan(0));

    // Update
    app = applicationDAO.getById(app.getId());
    app.setName("ApplicationDAOTest New name");
    applicationDAO.update(app);
    app = applicationDAO.getById(app.getId());
    assertThat(app.getName(), is("ApplicationDAOTest New name"));

    // Delete
    applicationDAO.deleteWithIcon(app, iconDir);
    app = applicationDAO.getById(app.getId());
    assertThat(app, nullValue());
    assertThat(appIconDir.getAbsolutePath(), appIconDir.exists(), is(false));
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
  public void testGetApplicationsByPublicIds() throws Exception {
    // Create a few applications
    int numApplication = 3;
    List<Application> applications = tempEntity.newApplications(organization.getId(), numApplication);
    Set<String> publicIds = new HashSet<>();
    for (Application app : applications) {
      publicIds.add(app.getPublicId());
    }

    // Note: applicationDAO.getByPublicIds returns an unmodifiable list, since we need to sort the list we create one
    List<Application> retrievedApplications = new ArrayList<>(applicationDAO.getByPublicIds(publicIds));
    assertThat(retrievedApplications, hasSize(numApplication));
    assertApplications(retrievedApplications, applications);
  }

  @Test
  public void testGetApplicationsByPublicIds_EmptySet() throws Exception {
    // Create a few applications
    tempEntity.newApplications(organization.getId(), 3);
    Set<String> publicIds = new HashSet<>();
    List<Application> retrievedApplications = new ArrayList<>(applicationDAO.getByPublicIds(publicIds));
    assertThat(retrievedApplications, hasSize(0));
  }

  @Test
  public void testGetApplicationsByTagIds() throws Exception {
    int numApplications = 3;
    Tag tag1 = tempEntity.newTag(organization.getId(), "foo");
    Tag tag2 = tempEntity.newTag(organization.getId(), "bar");
    List<Application> applications = tempEntity.newApplications(organization.getId(), numApplications);
    for (Application app : applications) {
      tempEntity.newApplicationTag(app.getId(), tag1.getId());
    }

    // assign second tag to one of the apps
    tempEntity.newApplicationTag(applications.get(0).getId(), tag2.getId());

    // searching by both tags should result in 3 unique apps
    List<Application> retrievedApplications = Lists
        .newArrayList(applicationDAO.getByTagIds(Sets.newHashSet(tag1.getId(), tag2.getId())));
    assertThat(retrievedApplications, hasSize(numApplications));
    assertApplications(retrievedApplications, applications);

    // find nothing without
    retrievedApplications = Lists.newArrayList(applicationDAO.getByTagIds(Sets.newHashSet("notMyTagId")));
    assertThat(retrievedApplications, hasSize(0));
  }

  @Test
  public void testGetApplicationsByTagIds_Untagged() {
    String tagName = "foo";
    Tag tag = tempEntity.newTag(organization.getId(), tagName);
    Application taggedApplication = tempEntity.newApplication(organization.getId());
    tempEntity.newApplicationTag(taggedApplication.getId(), tag.getId());

    // NOTE: the application created in AbstractDbDAOTest has no tags
    List<Application> allApplications = Lists.newArrayList(taggedApplication, application);

    // find both apps with tag and null
    List<Application> retrievedApplications = Lists
        .newArrayList(applicationDAO.getByTagIds(Sets.newHashSet(tag.getId(), null)));
    assertThat(retrievedApplications, hasSize(2));
    assertApplications(retrievedApplications, allApplications);

    // find just the untagged one with just null
    retrievedApplications = Lists.newArrayList(applicationDAO.getByTagIds(Sets.newHashSet((String) null)));
    assertThat(retrievedApplications, hasSize(1));
    assertApplications(retrievedApplications, Lists.newArrayList(application));

    // do not find the untagged one without null
    retrievedApplications = Lists.newArrayList(applicationDAO.getByTagIds(Sets.newHashSet(tag.getId())));
    assertThat(retrievedApplications, hasSize(1));
    assertApplications(retrievedApplications, Lists.newArrayList(taggedApplication));
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_Untagged() {
    int numTaggedApplication = 2;
    String tagName = "foo";
    Tag tag = tempEntity.newTag(organization.getId(), tagName);
    List<Application> taggedApplications = tempEntity.newApplications(organization.getId(), numTaggedApplication);
    for (Application app : taggedApplications) {
      tempEntity.newApplicationTag(app.getId(), tag.getId());
    }

    Application taggedApplication = taggedApplications.get(0);
    Application untaggedApplication = application;

    // this list will contain one of the two tagged apps and the untagged app
    List<Application> applications = Lists.newArrayList(taggedApplication, untaggedApplication);

    Set<String> applicationIdsToQuery = Sets.newHashSet(taggedApplication.getId(), untaggedApplication.getId());

    // find the tagged one that we expected and the untagged one
    List<Application> retrievedApplications = Lists
        .newArrayList(applicationDAO.getByIdsAndTagIds(applicationIdsToQuery, Sets.newHashSet(tag.getId(), null)));
    assertThat(retrievedApplications, hasSize(2));
    assertApplications(retrievedApplications, applications);

    // find just the untagged one with just null
    retrievedApplications = Lists
        .newArrayList(applicationDAO.getByIdsAndTagIds(applicationIdsToQuery, Sets.newHashSet((String) null)));
    assertThat(retrievedApplications, hasSize(1));
    assertApplications(retrievedApplications, Lists.newArrayList(untaggedApplication));

    // do not find the untagged one without null
    retrievedApplications = Lists
        .newArrayList(applicationDAO.getByIdsAndTagIds(applicationIdsToQuery, Sets.newHashSet(tag.getId())));
    assertThat(retrievedApplications, hasSize(1));
    assertApplications(retrievedApplications, Lists.newArrayList(taggedApplication));

    // do not find the untagged app if its id isn't in the app id list
    retrievedApplications = Lists.newArrayList(applicationDAO
        .getByIdsAndTagIds(Sets.newHashSet(taggedApplication.getId()), Sets.newHashSet(tag.getId(), null)));
    assertThat(retrievedApplications, hasSize(1));
    assertApplications(retrievedApplications, Lists.newArrayList(taggedApplication));
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
      assertThat(expected.getMessage(), is("Cannot change the parent organization of an application."));
    }
  }

  @Test
  public void testUpdateOrganizationId_Force() {
    Organization organization1 = tempEntity.newOrganization("testUpdateOrganizationId 1");

    application.setOrganizationId(organization1.getId());
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      applicationDAO.update(tx, application, true);
      tx.commit();
    }
    assertThat(applicationDAO.getById(application.getId()).getOrganizationId(), is(organization1.getId()));
  }

  @Test
  public void testValidatePublicIdValidChars_Insert() {
    for (String publicId : NameHelperTest.VALID_NAMES) {
      tempEntity.newApplication(tempEntity.uuid(), publicId.replaceAll("\\s", ""), organization.getId());
    }
  }

  @Test
  public void testValidatePublicIdValidChars_Update() {
    for (String publicId : NameHelperTest.VALID_NAMES) {
      application.setPublicId(publicId.replaceAll("\\s", ""));
      applicationDAO.update(application);
    }
  }

  @Test
  public void testValidatePublicIdInvalidChars_Insert() {
    Application app = new Application(null, "name", organization.getId());
    for (String publicId : NameHelperTest.INVALID_CHARACTERS) {
      app.setPublicId(publicId);
      try {
        applicationDAO.insert(app);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertThat(expected.getMessage(),
            is(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Public ID", publicId.charAt(0))));
      }
    }
  }

  @Test
  public void testValidatePublicIdInvalidChars_Update() {
    for (String publicId : NameHelperTest.INVALID_CHARACTERS) {
      application.setPublicId(publicId);
      try {
        applicationDAO.update(application);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertThat(expected.getMessage(),
            is(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Public ID", publicId.charAt(0))));
      }
    }
  }

  @Test
  public void testValidateNullPublicId_Insert() {
    Application app = new Application(null, "name", organization.getId());
    try {
      applicationDAO.insert(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertThat(expected.getMessage(), is("Public ID is required."));
    }
  }

  @Test
  public void testValidateNullPublicId_Update() {
    application.setPublicId(null);
    try {
      applicationDAO.update(application);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertThat(expected.getMessage(), is("Public ID is required."));
    }
  }

  @Test
  public void testValidateEmptyPublicId_Insert() {
    Application app = new Application("", "name", organization.getId());
    try {
      applicationDAO.insert(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertThat(expected.getMessage(), is("Public ID is required."));
    }
  }

  @Test
  public void testValidateEmptyPublicId_Update() {
    application.setPublicId("");
    try {
      applicationDAO.update(application);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertThat(expected.getMessage(), is("Public ID is required."));
    }
  }

  @Test
  public void testValidatePublicIdWithWhitespaces_Insert() {
    Application app = new Application(null, "name", organization.getId());
    for (char invalidChar : PUBLIC_ID_WHITESPACE_CHARS) {
      app.setPublicId("foo" + invalidChar + "bar");
      try {
        applicationDAO.insert(app);
        fail("Expected InvalidNameException");
      }
      catch (InvalidApplicationException expected) {
        assertThat(expected.getMessage(), is("Public ID cannot contain whitespaces."));
      }
      catch (InvalidNameException expected) {
        assertThat(expected.getMessage(), is(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Public ID", invalidChar)));
      }
    }
  }

  @Test
  public void testValidatePublicIdWithWhitespaces_Update() {
    for (char invalidChar : PUBLIC_ID_WHITESPACE_CHARS) {
      application.setPublicId("foo" + invalidChar + "bar");
      try {
        applicationDAO.update(application);
        fail("Expected InvalidNameException");
      }
      catch (InvalidApplicationException expected) {
        assertThat(expected.getMessage(), is("Public ID cannot contain whitespaces."));
      }
      catch (InvalidNameException expected) {
        assertThat(expected.getMessage(), is(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Public ID", invalidChar)));
      }
    }
  }

  @Test
  public void testValidatePublicIdIsDot_Insert() {
    Application app = new Application(".", "name", organization.getId());
    try {
      applicationDAO.insert(app);
      fail("Expected InvalidApplicationException");
    }
    catch (InvalidApplicationException expected) {
      assertThat(expected.getMessage(), is("Public ID cannot be '.' or '..'"));
    }
  }

  @Test
  public void testValidatePublicIdIsDot_Update() {
    application.setPublicId(".");
    try {
      applicationDAO.update(application);
      fail("Expected InvalidApplicationException");
    }
    catch (InvalidApplicationException expected) {
      assertThat(expected.getMessage(), is("Public ID cannot be '.' or '..'"));
    }
  }

  @Test
  public void testValidatePublicIdIsDotDot_Insert() {
    Application app = new Application("..", "name", organization.getId());
    try {
      applicationDAO.insert(app);
      fail("Expected InvalidApplicationException");
    }
    catch (InvalidApplicationException expected) {
      assertThat(expected.getMessage(), is("Public ID cannot be '.' or '..'"));
    }
  }

  @Test
  public void testValidatePublicIdIsDotDot_Update() {
    application.setPublicId("..");
    try {
      applicationDAO.update(application);
      fail("Expected InvalidApplicationException");
    }
    catch (InvalidApplicationException expected) {
      assertThat(expected.getMessage(), is("Public ID cannot be '.' or '..'"));
    }
  }

  @Test
  public void testValidatePublicIdIsMaxLength_Insert() {
    String publicId = StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH);
    Application app = new Application(publicId, "name", organization.getId());
    applicationDAO.insert(app);
    tempEntity.register(app);
    // No need to assert anything as this method throws an exception if not found
    applicationDAO.getByPublicIdNotNull(publicId);
  }

  @Test
  public void testValidatePublicIdIsMaxLength_Update() {
    final String publicId = StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH);
    application.setPublicId(publicId);
    applicationDAO.update(application);
    tempEntity.register(application);
    // No need to assert anything as this method throws an exception if not found
    applicationDAO.getByPublicIdNotNull(publicId);
  }

  @Test
  public void testValidatePublicIdTooLong_Insert() {
    Application app = new Application(StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH + 1), "name",
        organization.getId());
    try {
      applicationDAO.insert(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertThat(expected.getMessage(), is("Public ID must be " + ApplicationDAO.MAX_PUBLIC_ID_LENGTH
          + " characters or less."));
    }
  }

  @Test
  public void testValidatePublicIdTooLong_Update() {
    application.setPublicId(StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH + 1));
    try {
      applicationDAO.update(application);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertThat(expected.getMessage(), is("Public ID must be " + ApplicationDAO.MAX_PUBLIC_ID_LENGTH
          + " characters or less."));
    }
  }

  @Test
  public void testPublicIdIsCaseInsensitive() {
    String appPublicId = "testPublicIdIsCaseInsensitive";

    Application app = new Application(appPublicId, "test", organization.getId());
    ApplicationDAO applicationDAO = new ApplicationDAO();
    applicationDAO.insert(app);
    String applicationId = app.getId();

    assertThat(app.getPublicId(), is(appPublicId));
    assertThat(app.getPublicIdLowercase(), is(appPublicId.toLowerCase(Locale.ENGLISH)));

    app = applicationDAO.getById(applicationId);
    assertThat(app, notNullValue());
    assertThat(app.getPublicId(), is(appPublicId));
    assertThat(app.getPublicIdLowercase(), is(appPublicId.toLowerCase(Locale.ENGLISH)));

    app = applicationDAO.getByPublicId(appPublicId);
    assertThat(app, notNullValue());
    assertThat(app.getId(), is(applicationId));

    app = applicationDAO.getByPublicId(appPublicId.toLowerCase(Locale.ENGLISH));
    assertThat(app, notNullValue());
    assertThat(app.getId(), is(applicationId));

    app = applicationDAO.getByPublicId(appPublicId.toUpperCase(Locale.ENGLISH));
    assertThat(app, notNullValue());
    assertThat(app.getId(), is(applicationId));
  }

  @Test
  public void testValidateNullName_Insert() {
    Application app = new Application("publicId", null, organization.getId());
    try {
      applicationDAO.insert(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertThat(expected.getMessage(), is("Name is required."));
    }
  }

  @Test
  public void testValidateNullName_Update() {
    Application app = new Application("publicId", "testValidateNullName", organization.getId());
    assertThat(app.getNameLowercaseNoWhitespace(), is("testvalidatenullname"));
    applicationDAO.insert(app);

    app.setName(null);
    assertThat(app.getNameLowercaseNoWhitespace(), nullValue());
    try {
      applicationDAO.update(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertThat(expected.getMessage(), is("Name is required."));
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
      assertThat(expected.getMessage(), is("Name is required."));
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    Application app = new Application("publicId", "testValidateEmptyName", organization.getId());
    assertThat(app.getNameLowercaseNoWhitespace(), is("testvalidateemptyname"));
    applicationDAO.insert(app);

    app.setName(" ");
    assertThat(app.getNameLowercaseNoWhitespace(), is(""));
    try {
      applicationDAO.update(app);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertThat(expected.getMessage(), is("Name is required."));
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    Application app = new Application("publicId", "name", organization.getId());
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      app.setName(name);
      try {
        applicationDAO.insert(app);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertThat(expected.getMessage(), is(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0))));
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      application.setName(name);
      try {
        applicationDAO.update(application);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertThat(expected.getMessage(), is(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0))));
      }
    }
  }

  @Test
  public void testValidateNameValidChars_Insert() {
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newApplication(name, tempEntity.uuid(), organization.getId());
    }
  }

  @Test
  public void testValidateNameValidChars_Update() {
    Application app = tempEntity.newApplication("a", "publicId", organization.getId());
    for (String name : NameHelperTest.VALID_NAMES) {
      app.setName(name);
      applicationDAO.update(app);
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    Application app = new Application("publicId", "name", organization.getId());
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      app.setName(name);
      try {
        applicationDAO.insert(app);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertThat(expected.getMessage(),
            is("Name must not have leading or trailing spaces, or have two spaces in a row."));
      }
    }
  }

  @Test
  public void testValidateNameSpaces_Update() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      application.setName(name);
      try {
        applicationDAO.update(application);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertThat(expected.getMessage(),
            is("Name must not have leading or trailing spaces, or have two spaces in a row."));
      }
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    Application app = tempEntity.newApplication(name, "publicId", organization.getId());

    assertThat(app.getName(), is(name));
    assertThat(app.getNameLowercaseNoWhitespace(), is("teststringwithcaseandwhitespace"));

    String name1 = "TEST String      With    cASE and      whitespace";
    Application application1 = applicationDAO.getByName(name1);
    assertThat(application1, notNullValue());
    assertThat(application1.getId(), is(app.getId()));
  }

  @Test
  public void testDuplicatePublicId_Insert() {
    try {
      tempEntity.newApplication(tempEntity.uuid(), application.getPublicId(), organization.getId());
      fail("Duplicate value should fail");
    } catch (InvalidApplicationException e) {
      assertThat(e.getMessage(), is(application.getPublicId() + " is already used as an ID."));
    }
  }

  @Test
  public void testDuplicatePublicId_Update() {
    final String duplicatePublicId = "duplicatePublicId";
    tempEntity.newApplicationWithParent(duplicatePublicId);

    application.setPublicId(duplicatePublicId);

    try {
      applicationDAO.update(application);
      fail("Duplicate value should fail");
    } catch (InvalidApplicationException e) {
      assertThat(e.getMessage(), is(application.getPublicId() + " is already used as an ID."));
    }
  }

  @Test
  public void testDuplicateName_Insert() {
    tempEntity.newApplication("testDuplicateName", "publicId", organization.getId());
    try {
      tempEntity.newApplication("Test Duplicate Name", "publicId2", organization.getId());
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertThat(expected.getMessage(), is("Test Duplicate Name is already used as a name."));
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
      assertThat(expected.getMessage(), is("Test Duplicate Name is already used as a name."));
    }
  }

  @Test
  public void testCascadeDeleteToLabels() {
    LabelDAO labelDAO = new LabelDAO();
    Label label = new Label(application.getId(), "testCascadeDeleteToLabels", Color.dark_blue);
    labelDAO.insert(label);

    applicationDAO.delete(application);
    assertThat(labelDAO.getByOwnerId(application.getId()), hasSize(0));
  }

  @Test
  public void testCascadeDeleteToProprietaryConfig() {
    tempEntity.newProprietaryConfig(application.getId());

    applicationDAO.delete(application);
    assertThat(new ProprietaryConfigDAO().getByOwnerId(application.getId()), is(nullValue()));
  }

  @Test
  public void testCascadeDeleteToPolicyWaivers() {
    Policy policy = tempEntity.newPolicy(application.getId(), "testCascadeDeleteToPolicyWaivers");
    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policy.getId(), application.getId(),
        "My comment");
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    policyWaiverDAO.insert(policyWaiver);
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(application.getId());
    assertThat(policyWaivers.size(), is(1));

    applicationDAO.delete(application);
    policyWaivers = policyWaiverDAO.getByOwnerId(application.getId());
    assertThat(policyWaivers.size(), is(0));
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
    LicenseOverride licenseOverride = new LicenseOverride(application.getId(),
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    licenseOverrideDAO.insert(licenseOverride);
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(application.getId());
    assertThat(licenseOverrides.size(), is(1));

    applicationDAO.delete(application);
    licenseOverrides = licenseOverrideDAO.getByOwnerId(application.getId());
    assertThat(licenseOverrides.size(), is(0));
  }

  @Test
  public void testCascadeDeleteToSecurityVulnerabilityOverrides() {
    SecurityVulnerabilityOverride securityVulnerabilityOverride = tempEntity.newSecurityVulnerabilityOverride(
        application.getId(), "hash", "source", "refrenceId", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);

    applicationDAO.delete(application);

    assertThat(new SecurityVulnerabilityOverrideDAO().getById(securityVulnerabilityOverride.getId()), is(nullValue()));
  }

  @Test
  public void testCascadeDeleteToMembershipMappings() {
    String roleId = new RoleDAO().getApplicationRoles().get(0).getId();
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    membershipMappingDAO.setMembershipMappingsForContextAndRole(application.getId(), roleId,
        Arrays.asList(new MembershipMapping("admin", MemberType.USER)));

    applicationDAO.delete(application);

    assertThat(membershipMappingDAO.getByContextId(application.getId()), emptyCollectionOf(MembershipMapping.class));
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
  public void testCascadeDeleteToApplicationComponents() {
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(applicationId, BuildStageType.ID,
        "hash", ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));

    applicationDAO.delete(application);

    assertThat(new ApplicationComponentDAO().getById(applicationComponent.getId()), is(nullValue()));
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
      assertThat(expected.getMessage(), is("Name must be 60 characters or less."));
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
      assertThat(expected.getMessage(), is("Name must be 60 characters or less."));
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

  @Test
  public void testCascadeDeleteToPolicyViolationAggregations() {
    PolicyViolationAggregationDAO policyViolationAggregationDAO = new PolicyViolationAggregationDAO();
    PolicyViolationAggregation aggregation = tempEntity.newPolicyViolationAggregation(application.getId(), new Date());

    applicationDAO.delete(application);

    assertThat(policyViolationAggregationDAO.getById(aggregation.getId()), is(nullValue()));
  }

  @Test
  public void testCascadeDeleteToPolicyViolationResolutionStates() {
    PolicyViolationResolutionStateDAO policyViolationResolutionStateDAO = new PolicyViolationResolutionStateDAO();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan1",
        new Date());
    Policy policy = tempEntity.newPolicy(application.getId(), "policy1", 5);
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolationResolutionState resolutionState = tempEntity.newPolicyViolationResolutionState(application.getId(),
        policyViolation, BuildStageType.ID);

    applicationDAO.delete(application);

    assertThat(policyViolationResolutionStateDAO.getById(resolutionState.getId()), is(nullValue()));
  }

  @Test
  public void testGetApplicationsByContact() {
    final String contactName = "contactName";
    // Create some applications with and without contact name
    final int numApplications = 5;
    final Map<String, Application> expecteApplications = new HashMap<>();
    for (int i = 1; i <= numApplications; i++) {
      // Create some with contact name
      Application application = tempEntity.newApplication("app-with-contact-" + i, tempEntity.uuid(),
          organization.getId(), contactName);
      expecteApplications.put(application.getId(), application);
      // Create some without
      tempEntity.newApplication(organization.getId());
    }

    final List<Application> applications = applicationDAO.getByContactInternalName(contactName);
    assertThat(applications, hasSize(numApplications));
    for (final Application app : applications) {
      validateApplication(app, expecteApplications.get(app.getId()));
    }
  }

  private void validateApplication(Application actualApp, Application expectedApp) {
    assertThat(actualApp.getName(), is(expectedApp.getName()));
    assertThat(actualApp.getContactInternalName(), is(expectedApp.getContactInternalName()));
    assertThat(actualApp.getOrganizationId(), is(expectedApp.getOrganizationId()));
    assertThat(actualApp.getPublicId(), is(expectedApp.getPublicId()));
  }

  private void assertApplications(List<Application> actual, List<Application> expected) {
    Collections.sort(actual, new ApplicationComparator());
    Collections.sort(expected, new ApplicationComparator());

    for (int i = 0; i < actual.size(); i++) {
      Application actualApplication = actual.get(i);
      Application expectedApplication = expected.get(i);
      assertThat(actualApplication.getId(), is(expectedApplication.getId()));
      assertThat(actualApplication.getName(), is(expectedApplication.getName()));
      assertThat(actualApplication.getOrganizationId(), is(expectedApplication.getOrganizationId()));
      assertThat(actualApplication.getPublicId(), is(expectedApplication.getPublicId()));
      assertThat(actualApplication.getPublicIdLowercase(), is(expectedApplication.getPublicIdLowercase()));
      assertThat(actualApplication.getContactInternalName(), is(expectedApplication.getContactInternalName()));
      assertThat(actualApplication.getNameLowercaseNoWhitespace(),
          is(expectedApplication.getNameLowercaseNoWhitespace()));
    }
  }

  class ApplicationComparator
      implements Comparator<Application>
  {
    @Override
    public int compare(final Application o1, final Application o2) {
      return o1.getId().compareTo(o2.getId());
    }
  }

  @Test
  public void testUpdateApplicationWithInvalidPublicId() {
    // Applications can have invalid public IDs if they were created before the public ID validation was introduced. It
    // should be possible to update these applications without changing the public ID (which is not allowed anyway).
    String invalidAppId = "App Public Id !@#$%^&*()";
    Application app = tempEntity.newApplicationWithInvalidPublicId(invalidAppId);
    String newName = app.getName() + " Updated";
    app.setName(newName);
    applicationDAO.update(app);
    app = applicationDAO.getById(app.getId());
    assertThat(app.getName(), is(newName));
    assertThat(app.getPublicId(), is(invalidAppId));
  }
}
