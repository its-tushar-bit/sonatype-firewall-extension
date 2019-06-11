/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.DescriptionHelper;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    assertThat(tag.getId()).isNotNull();

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag).isNotNull();
    assertTag(organization.getId(), "testCRUD Name", "testCRUD description", Color.yellow, tag);

    // Update
    tag.setName("Updated Name");
    tag.setColor(Color.dark_purple);
    dao.update(tag);

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag).isNotNull();
    assertTag(organization.getId(), "Updated Name", "testCRUD description", Color.dark_purple, tag);

    // Delete
    dao.delete(tag);

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag).isNull();
  }

  @Test
  public void testValidateNullName_Insert() {
    Tag tag = new Tag(organization.getId(), null /* name */, "description", Color.yellow);
    assertThatThrownBy(() -> {
      dao.insert(tag);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateNullName_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setName(null);
    assertThatThrownBy(() -> {
      dao.update(tag);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateEmptyName_Insert() {
    Tag tag = new Tag(organization.getId(), " " /* name */, "description", Color.yellow);
    assertThatThrownBy(() -> {
      dao.insert(tag);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateEmptyName_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setName(" ");
    assertThatThrownBy(() -> {
      dao.update(tag);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      tag.setName(name);
      assertThatThrownBy(() -> {
        dao.insert(tag);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      tag.setName(name);
      assertThatThrownBy(() -> {
        dao.update(tag);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameValidChars_Insert() {
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newTag(organization.getId(), name);
    }
  }

  @Test
  public void testValidateNameValidChars_Update() {
    Tag tag = tempEntity.newTag(organization.getId(), "a");
    for (String name : NameHelperTest.VALID_NAMES) {
      tag.setName(name);
      dao.update(tag);
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      tag.setName(name);
      assertThatThrownBy(() -> {
        dao.insert(tag);
      }).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testValidateNameSpaces_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      tag.setName(name);
      assertThatThrownBy(() -> {
        dao.update(tag);
      }).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    Tag tag = new Tag(organization.getId(), name, "description", Color.yellow);
    dao.insert(tag);

    assertThat(tag.getName()).isEqualTo(name);
    assertThat(tag.getNameLowercaseNoWhitespace()).isEqualTo("teststringwithcaseandwhitespace");
  }

  @Test
  public void testDuplicateName_Insert() {
    Tag tag = new Tag(organization.getId(), "testDuplicateName", "description", Color.yellow);
    dao.insert(tag);

    Tag tag1 = new Tag(organization.getId(), "Test Duplicate Name", "description", Color.yellow);
    assertThatThrownBy(() -> {
      dao.insert(tag1);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Test Duplicate Name is already used as a name.");
  }

  @Test
  public void testDuplicateName_Update() {
    Tag tag = new Tag(organization.getId(), "testDuplicateName", "description", Color.yellow);
    dao.insert(tag);

    Tag tag1 = new Tag(organization.getId(), "testDuplicateName1", "description", Color.yellow);
    dao.insert(tag1);

    tag1.setName("Test Duplicate Name");
    assertThatThrownBy(() -> {
      dao.update(tag1);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Test Duplicate Name is already used as a name.");
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    Tag tag = new Tag(organization.getId(), name + "a", "description", Color.yellow);
    assertThatThrownBy(() -> {
      dao.insert(tag);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name must be 60 characters or less.");

    tag.setName(name);
    dao.insert(tag);
  }

  @Test
  public void testValidateNameLength_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    tag.setName(name + "a");
    assertThatThrownBy(() -> {
      dao.update(tag);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name must be 60 characters or less.");

    tag.setName(name);
    dao.update(tag);
  }

  @Test
  public void testValidateDescriptionLength_Insert() {
    String description = StringUtils.repeat("a", DescriptionHelper.MAX_DESC_LENGTH);
    Tag tag = new Tag(organization.getId(), "name", description + "a", Color.yellow);
    assertThatThrownBy(() -> {
      dao.insert(tag);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("The description cannot be longer than 255 characters, the one supplied has 256 characters.");

    tag.setDescription(description);
    dao.insert(tag);
  }

  @Test
  public void testValidateDescriptionLength_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    String description = StringUtils.repeat("a", DescriptionHelper.MAX_DESC_LENGTH);
    tag.setDescription(description + "a");
    assertThatThrownBy(() -> {
      dao.update(tag);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("The description cannot be longer than 255 characters, the one supplied has 256 characters.");

    tag.setDescription(description);
    dao.update(tag);
  }

  @Test
  public void testValidateNullDescription_Insert() {
    Tag tag = new Tag(organization.getId(), "name", null /* description */, Color.yellow);
    assertThatThrownBy(() -> {
      dao.insert(tag);
    }).isInstanceOf(BadRequestException.class).hasMessage("The description is required.");
  }

  @Test
  public void testValidateNullDescription_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setDescription(null);
    assertThatThrownBy(() -> {
      dao.update(tag);
    }).isInstanceOf(BadRequestException.class).hasMessage("The description is required.");
  }

  @Test
  public void testValidateEmptyDescription_Insert() {
    Tag tag = new Tag(organization.getId(), "name", " " /* description */, Color.yellow);
    assertThatThrownBy(() -> {
      dao.insert(tag);
    }).isInstanceOf(BadRequestException.class).hasMessage("The description is required.");
  }

  @Test
  public void testValidateEmptyDescription_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setDescription(" ");
    assertThatThrownBy(() -> {
      dao.update(tag);
    }).isInstanceOf(BadRequestException.class).hasMessage("The description is required.");
  }

  @Test
  public void testValidateNullColor_Insert() {
    Tag tag = new Tag(organization.getId(), "name", "description", null);
    assertThatThrownBy(() -> {
      dao.insert(tag);
    }).isInstanceOf(InvalidTagException.class).hasMessage("The application category color must be assigned.");
  }

  @Test
  public void testValidateNullColor_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setColor(null);
    assertThatThrownBy(() -> {
      dao.update(tag);
    }).isInstanceOf(InvalidTagException.class).hasMessage("The application category color must be assigned.");
  }

  @Test
  public void testLegacyColorsInvalid() throws Exception {
    @SuppressWarnings("deprecation")
    Color[] legacyColors = new Color[] { Color.white, Color.grey, Color.black, Color.green, Color.red, Color.blue };

    Tag tag = new Tag();
    tag.setOrganizationId(organization.getId());
    tag.setName("MyLabel");
    tag.setDescription("description");

    // Insert
    for (Color color : legacyColors) {
      tag.setColor(color);
      assertThatThrownBy(() -> {
        dao.insert(tag);
      }).isInstanceOf(InvalidTagException.class)
          .hasMessage("The application category color " + color.toValue() + " is invalid.");
    }

    // Update
    tag.setColor(Color.dark_blue);
    dao.insert(tag);
    for (Color color : legacyColors) {
      tag.setColor(color);
      assertThatThrownBy(() -> {
        dao.update(tag);
      }).isInstanceOf(InvalidTagException.class)
          .hasMessage("The application category color " + color.toValue() + " is invalid.");
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

    assertThat(appTagDAO.getByTagId(tag.getId())).isEmpty();
  }

  @Test
  public void testDenyCascadeDeleteToPolicyTags() {
    Policy policy = tempEntity.newPolicy(organization);
    Tag tag = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policy.getId(), tag.getId());

    assertThatThrownBy(() -> {
      dao.delete(tag);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot delete the application category because it is associated with policies.");

    assertThat(new PolicyTagDAO().getByTagId(tag.getId())).hasSize(1);
  }

  private void assertTag(String orgId, String name, String description, Color color, Tag actual) {
    assertThat(actual.getOrganizationId()).isEqualTo(orgId);
    assertThat(actual.getName()).isEqualTo(name);
    assertThat(actual.getNameLowercaseNoWhitespace()).isEqualTo(NameHelper.normalize(name));
    assertThat(actual.getDescription()).isEqualTo(description);
    assertThat(actual.getColor()).isEqualTo(color);
  }

  private void assertAppliedTags(List<Tag> expected, List<Tag> actual) {
    assertThat(actual).hasSameSizeAs(expected);

    Set<String> tagIds = new HashSet<>();
    for (Tag tag : expected) {
      tagIds.add(tag.getId());
    }

    for (Tag tag : actual) {
      assertThat(tag.getId()).isIn(tagIds);
    }
  }

  @Test
  public void testNameClashWithChildOrgTag_Insert() throws Exception {
    String tagName = "some name";
    tempEntity.newTag(organization.getId(), tagName);

    // Add another tag with a case-/whitespace-equivalent name at parent owner level
    assertInsertTagWithDuplicateName(organization.getParentOrganizationId(), tagName, organization);
  }

  @Test
  public void testNameClashWithParentOrgTag_Insert() throws Exception {
    String tagName = "some name";
    tempEntity.newTag(organization.getParentOrganizationId(), tagName);

    Organization expectedOrg = new OrganizationDAO().getById(organization.getParentOrganizationId());

    // Add another tag with a case-/whitespace-equivalent name at child org level
    assertInsertTagWithDuplicateName(organization.getId(), tagName, expectedOrg);
  }

  @Test
  public void testNameClashWithChildOrgTag_Update() throws Exception {
    String tagName = "some name";
    tempEntity.newTag(organization.getId(), tagName);

    // Add another tag with a case-/whitespace-equivalent name at parent owner level
    assertUpdateTagWithDuplicateName(organization.getParentOrganizationId(), tagName, organization);
  }

  @Test
  public void testNameClashWithParentOrgTag_Update() throws Exception {
    String tagName = "some name";
    tempEntity.newTag(organization.getParentOrganizationId(), tagName);

    Organization expectedOrg = new OrganizationDAO().getById(organization.getParentOrganizationId());

    // Add another tag with a case-/whitespace-equivalent name at child org level
    assertUpdateTagWithDuplicateName(organization.getId(), tagName, expectedOrg);
  }

  private void assertInsertTagWithDuplicateName(String orgId, String tagName, Organization expectedOrg) {
    // Add a tag with a case-/whitespace-equivalent name
    Tag tag = new Tag(orgId, tagName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH), "description");
    assertThatThrownBy(() -> {
      new TagDAO().insert(tag);
    }).isInstanceOf(InvalidNameException.class).hasMessage(
        "An application category with the same name already exists for organization '" + expectedOrg.getName() + "'");
  }

  private void assertUpdateTagWithDuplicateName(String orgId, String tagName, Organization expectedOrg) {
    // Add a tag with a case-/whitespace-equivalent name
    Tag tag = tempEntity.newTag(orgId, "another name");
    tag.setName(tagName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> {
      new TagDAO().update(tag);
    }).isInstanceOf(InvalidNameException.class).hasMessage(
        "An application category with the same name already exists for organization '" + expectedOrg.getName() + "'");
  }
}
