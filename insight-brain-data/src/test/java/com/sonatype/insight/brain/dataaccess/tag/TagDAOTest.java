/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @since 1.9
 */
public class TagDAOTest
    extends AbstractDbDAOTest
{
  private TagDAO dao = new TagDAO();

  @Before
  public void before() {
    organization = tempEntity.newOrganization("TagDAOTest");
  }

  @Test
  public void testCRUD() throws Exception {
    // Create
    Tag tag = new Tag(organization.getId(), "testCRUD Name", "testCRUD description", Color.yellow);
    dao.insert(tag);
    assertThat(tag.getId(), notNullValue());

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag, notNullValue());
    assertTag(organization.getId(), "testCRUD Name", "testCRUD description", Color.yellow, tag);

    // Update
    tag.setName("Updated Name");
    tag.setColor(Color.black);
    dao.update(tag);

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag, notNullValue());
    assertTag(organization.getId(), "Updated Name", "testCRUD description", Color.black, tag);

    // Delete
    dao.delete(tag);

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag, nullValue());
  }

  @Test
  public void testValidateNullName_Insert() {
    Tag tag = new Tag(organization.getId(), null /* name */, "description", Color.yellow);
    try {
      dao.insert(tag);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullName_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setName(null);
    try {
      dao.update(tag);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Insert() {
    Tag tag = new Tag(organization.getId(), " " /* name */, "description", Color.yellow);
    try {
      dao.insert(tag);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setName(" ");
    try {
      dao.update(tag);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    for (String name : INVALID_ALPHANUMERIC) {
      tag.setName(name);
      try {
        dao.insert(tag);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);
    for (String name: INVALID_ALPHANUMERIC) {
      tag.setName(name);
      try {
        dao.update(tag);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    for (String name : INVALID_SPACING_NAMES) {
      tag.setName(name);
      try {
        dao.insert(tag);
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
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    for (String name : INVALID_SPACING_NAMES) {
      tag.setName(name);
      try {
        dao.update(tag);
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

    Tag tag = new Tag(organization.getId(), name, "description", Color.yellow);
    dao.insert(tag);

    assertEquals(name, tag.getName());
    assertEquals("teststringwithcaseandwhitespace", tag.getNameLowercaseNoWhitespace());
  }

  @Test
  public void testDuplicateName_Insert() {
    Tag tag = new Tag(organization.getId(), "testDuplicateName", "description", Color.yellow);
    dao.insert(tag);

    Tag tag1 = new Tag(organization.getId(), "Test Duplicate Name", "description", Color.yellow);
    try {
      dao.insert(tag1);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Test Duplicate Name is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testDuplicateName_Update() {
    Tag tag = new Tag(organization.getId(), "testDuplicateName", "description", Color.yellow);
    dao.insert(tag);

    Tag tag1 = new Tag(organization.getId(), "testDuplicateName1", "description", Color.yellow);
    dao.insert(tag1);

    tag1.setName("Test Duplicate Name");
    try {
      dao.update(tag1);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Test Duplicate Name is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    Tag tag = new Tag(organization.getId(), name + "a", "description", Color.yellow);
    try {
      dao.insert(tag);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    tag.setName(name);
    dao.insert(tag);
  }

  @Test
  public void testValidateNameLength_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    tag.setName(name + "a");
    try {
      dao.update(tag);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    tag.setName(name);
    dao.update(tag);
  }

  @Test
  public void testValidateDescriptionLength_Insert() {
    String description = StringUtils.repeat("a", TagDAO.MAX_DESC_SIZE);
    Tag tag = new Tag(organization.getId(), "name", description + "a", Color.yellow);
    try {
      dao.insert(tag);
      fail("Expected InvalidTagException");
    }
    catch (InvalidTagException e) {
      assertThat(e.getMessage(),
          is("The description cannot be longer than 255 characters, the one supplied has 256 characters."));
    }

    tag.setDescription(description);
    dao.insert(tag);
  }

  @Test
  public void testValidateDescriptionLength_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    String description = StringUtils.repeat("a", TagDAO.MAX_DESC_SIZE);
    tag.setDescription(description + "a");
    try {
      dao.update(tag);
      fail("Expected InvalidTagException");
    }
    catch (InvalidTagException e) {
      assertThat(e.getMessage(),
          is("The description cannot be longer than 255 characters, the one supplied has 256 characters."));
    }

    tag.setDescription(description);
    dao.update(tag);
  }

  @Test
  public void testValidateNullDescription_Insert() {
    Tag tag = new Tag(organization.getId(), "name", null /* description */, Color.yellow);
    try {
      dao.insert(tag);
      fail("Expected InvalidTagException");
    }
    catch (InvalidTagException expected) {
      assertEquals("The description is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullDescription_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setDescription(null);
    try {
      dao.update(tag);
      fail("Expected InvalidTagException");
    }
    catch (InvalidTagException expected) {
      assertEquals("The description is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyDescription_Insert() {
    Tag tag = new Tag(organization.getId(), "name", " " /* description */, Color.yellow);
    try {
      dao.insert(tag);
      fail("Expected InvalidTagException");
    }
    catch (InvalidTagException expected) {
      assertEquals("The description is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyDescription_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setDescription(" ");
    try {
      dao.update(tag);
      fail("Expected InvalidTagException");
    }
    catch (InvalidTagException expected) {
      assertEquals("The description is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullColor_Insert() {
    Tag tag = new Tag(organization.getId(), "name", "description", null);
    try {
      dao.insert(tag);
      fail("Expected InvalidTagException");
    }
    catch (InvalidTagException expected) {
      assertEquals("The tag color must be assigned.", expected.getMessage());
    }
  }
  
  @Test
  public void testValidateNullColor_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setColor(null);
    try {
      dao.update(tag);
      fail("Expected InvalidTagException");
    }
    catch (InvalidTagException expected) {
      assertEquals("The tag color must be assigned.", expected.getMessage());
    }
  }

  @Test
  public void testGetAppliedApplicationTags() {
    Application app1 = tempEntity.newApplication(organization.getId());
    Application app2 = tempEntity.newApplication(organization.getId());

    List<Tag> app1Tags = new ArrayList<>();
    List<Tag> app2Tags = new ArrayList<>();

    app1Tags.add(tempEntity.newTag(organization.getId()));
    app1Tags.add(tempEntity.newTag(organization.getId()));
    app2Tags.add(tempEntity.newTag(organization.getId()));
    app2Tags.add(tempEntity.newTag(organization.getId()));

    for (Tag tag : app1Tags) {
      tempEntity.newApplicationTag(app1.getId(), tag.getId());
    }

    for (Tag tag : app2Tags) {
      tempEntity.newApplicationTag(app2.getId(), tag.getId());
    }

    assertAppliedTags(app1Tags, dao.getByApplicationId(app1.getId()));
    assertAppliedTags(app2Tags, dao.getByApplicationId(app2.getId()));
  }

  @Test
  public void testCascadeDeleteToApplicationTags() {
    Application app = tempEntity.newApplication(organization.getId());
    Tag tag = tempEntity.newTag(organization.getId());

    ApplicationTagDAO appTagDAO = new ApplicationTagDAO();
    ApplicationTag appTag = new ApplicationTag(app.getId(), tag.getId());
    appTagDAO.insert(appTag);

    dao.delete(tag);

    assertThat(appTagDAO.getByTagId(tag.getId()), is(empty()));
  }

  @Test
  public void testDenyCascadeDeleteToPolicyTags() {
    Policy policy = tempEntity.newPolicy(organization.getId(), "TagDAOTest");
    Tag tag = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policy.getId(), tag.getId());

    try {
      dao.delete(tag);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("Cannot delete the tag because it is associated with policies"));
    }

    assertThat(new PolicyTagDAO().getByTagId(tag.getId()), hasSize(1));
  }

  @Test
  public void testGetAppliedToPolicyByOrgId() {
    Organization org1 = tempEntity.newOrganization("org1");
    Organization org2 = tempEntity.newOrganization("org2");

    List<Tag> org1Tags = new ArrayList<>();
    List<Tag> org2Tags = new ArrayList<>();

    Policy policy1 = tempEntity.newPolicy(org1.getId(), "TagDAOTest1");
    Policy policy2 = tempEntity.newPolicy(org1.getId(), "TagDAOTest2");
    Policy policy3 = tempEntity.newPolicy(org2.getId(), "TagDAOTest3");
    Policy policy4 = tempEntity.newPolicy(org2.getId(), "TagDAOTest4");

    //Create tags and apply to policies
    org1Tags.add(tempEntity.newTag(org1.getId()));
    tempEntity.newPolicyTag(policy1.getId(), org1Tags.get(0).getId());
    org1Tags.add(tempEntity.newTag(org1.getId()));
    tempEntity.newPolicyTag(policy2.getId(), org1Tags.get(1).getId());
    org1Tags.add(tempEntity.newTag(org1.getId()));
    tempEntity.newPolicyTag(policy1.getId(), org1Tags.get(2).getId());
    tempEntity.newPolicyTag(policy2.getId(), org1Tags.get(2).getId());
    org2Tags.add(tempEntity.newTag(org2.getId()));
    tempEntity.newPolicyTag(policy3.getId(), org2Tags.get(0).getId());
    org2Tags.add(tempEntity.newTag(org2.getId()));
    tempEntity.newPolicyTag(policy4.getId(), org2Tags.get(1).getId());
    org2Tags.add(tempEntity.newTag(org2.getId()));
    tempEntity.newPolicyTag(policy3.getId(), org2Tags.get(2).getId());
    tempEntity.newPolicyTag(policy4.getId(), org2Tags.get(2).getId());

    //Create tags but do not apply to policies
    tempEntity.newTag(org1.getId());
    tempEntity.newTag(org2.getId());

    assertAppliedTags(org1Tags, dao.getAppliedToPolicyByOrganizationId(org1.getId()));
    assertAppliedTags(org2Tags, dao.getAppliedToPolicyByOrganizationId(org2.getId()));
  }

  private void assertTag(String orgId, String name, String description, Color color, Tag actual) {
    assertThat(actual.getOrganizationId(), is(orgId));
    assertThat(actual.getName(), is(name));
    assertThat(actual.getNameLowercaseNoWhitespace(), is(NameHelper.normalize(name)));
    assertThat(actual.getDescription(), is(description));
    assertThat(actual.getColor(), is(color));
  }

  private void assertAppliedTags(List<Tag> expected, List<Tag> actual) {
    assertThat(actual, hasSize(expected.size()));

    Set<String> tagIds = new HashSet<>();
    for (Tag tag : expected) {
      tagIds.add(tag.getId());
    }

    for (Tag tag : actual) {
      assertThat(tagIds.contains(tag.getId()), is(true));
    }
  }
}