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
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Color;
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
    organization = createOrganization("TagDAOTest");
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
  public void testNullColor() {
    // Create with null color
    Tag tag = new Tag(organization.getId(), "name", "description", null);
    dao.insert(tag);
    assertThat(tag.getId(), notNullValue());

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag, notNullValue());
    assertTag(organization.getId(), "name", "description", null, tag);

    // Update color from null to something
    tag.setColor(Color.black);
    dao.update(tag);

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag, notNullValue());
    assertTag(organization.getId(), "name", "description", Color.black, tag);

    // Update color back to null
    tag.setColor(null);
    dao.update(tag);

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag, notNullValue());
    assertTag(organization.getId(), "name", "description", null, tag);
  }

  @Test
  public void testGetAppliedApplicationTags() {
    Application app1 = createApplication("one", "one", organization.getId());
    Application app2 = createApplication("two", "two", organization.getId());

    List<Tag> app1Tags = new ArrayList<>();
    List<Tag> app2Tags = new ArrayList<>();

    app1Tags.add(createTag("tag1", "tag1", organization.getId()));
    app1Tags.add(createTag("tag2", "tag2", organization.getId()));
    app2Tags.add(createTag("tag3", "tag3", organization.getId()));
    app2Tags.add(createTag("tag4", "tag4", organization.getId()));

    for (Tag tag : app1Tags) {
      createApplicationTag(app1.getId(), tag.getId());
    }

    for (Tag tag : app2Tags) {
      createApplicationTag(app2.getId(), tag.getId());
    }

    assertAppliedApplicationTags(app1Tags, dao.getByApplicationId(app1.getId()));
    assertAppliedApplicationTags(app2Tags, dao.getByApplicationId(app2.getId()));
  }

  @Test
  public void testCascadeDeleteToApplicationTags() {
    Application app = createApplication("testCascadeDeleteToApplicationTags", "testCascadeDeleteToApplicationTags",
        organization.getId());
    Tag tag = createTag("testCascadeDeleteToApplicationTags name", "testCascadeDeleteToApplicationTags description",
        organization.getId());

    ApplicationTagDAO appTagDAO = new ApplicationTagDAO();
    ApplicationTag appTag = new ApplicationTag(app.getId(), tag.getId());
    appTagDAO.insert(appTag);

    dao.delete(tag);

    assertThat(appTagDAO.getByTagId(tag.getId()), is(empty()));
  }

  @Test
  public void testDenyCascadeDeleteToPolicyTags() {
    String policyId = "testDenyCascadeDeleteToPolicyTags_PolicyId";
    Tag tag = createTag("testDenyCascadeDeleteToPolicyTags name", "testDenyCascadeDeleteToPolicyTags description",
        organization.getId());
    createPolicyTag(policyId, tag.getId());

    try {
      dao.delete(tag);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("Cannot delete the tag because it is associated with policies"));
    }

    assertThat(new PolicyTagDAO().getByTagId(tag.getId()), hasSize(1));
  }

  private void assertTag(String orgId, String name, String description, Color color, Tag actual) {
    assertThat(actual.getOrganizationId(), is(orgId));
    assertThat(actual.getName(), is(name));
    assertThat(actual.getNameLowercaseNoWhitespace(), is(NameHelper.normalize(name)));
    assertThat(actual.getDescription(), is(description));
    assertThat(actual.getColor(), is(color));
  }

  private void assertAppliedApplicationTags(List<Tag> expected, List<Tag> actual) {
    assertThat(actual.size(), is(expected.size()));

    Set<String> tagIds = new HashSet<>();
    for (Tag tag : expected) {
      tagIds.add(tag.getId());
    }

    for (Tag tag : actual) {
      assertThat(tagIds.contains(tag.getId()), is(true));
    }
  }
}