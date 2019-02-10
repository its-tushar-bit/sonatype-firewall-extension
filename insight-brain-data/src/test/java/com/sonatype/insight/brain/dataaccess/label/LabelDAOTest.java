/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LabelDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testValidateLabelNameInvalidChars_Insert() {
    LabelDAO dao = new LabelDAO();
    for (String labelName : NameHelperTest.INVALID_CHARACTERS) {
      assertThatThrownBy(() -> {
        dao.insert(new Label(applicationId, labelName, Color.light_green));
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Label name",
          labelName.charAt(0));
    }
  }

  @Test
  public void testValidateLabelNameInvalidChars_Update() {
    LabelDAO dao = new LabelDAO();
    Label label = tempEntity.newLabel(applicationId, "label", Color.light_green);
    for (String labelName : NameHelperTest.INVALID_CHARACTERS) {
      label.setLabel(labelName);
      assertThatThrownBy(() -> {
        dao.update(label);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Label name",
          labelName.charAt(0));
    }
  }

  @Test
  public void testValidateLabelNameValidChars_Insert() {
    for (String labelName : NameHelperTest.VALID_NAMES) {
      tempEntity.newLabel(applicationId, labelName, Color.light_green);
    }
  }

  @Test
  public void testValidateLabelNameValidChars_Update() {
    LabelDAO dao = new LabelDAO();
    Label label = tempEntity.newLabel(applicationId, "label", Color.light_green);
    for (String labelName : NameHelperTest.VALID_NAMES) {
      label.setLabel(labelName);
      dao.update(label);
    }
  }

  @Test
  public void testOlderLabelUpdate() throws Exception {
    Label oldLabel = tempEntity.newLabelWithInvalidLabelText(applicationId, "*/clearly_not_valid", Color.dark_blue);
    LabelDAO dao = new LabelDAO();

    // Update old label without changing label text.
    oldLabel.setColor(Color.light_green);
    dao.update(oldLabel);
    assertLabel(applicationId, "*/clearly_not_valid", Color.light_green, null, oldLabel);

    // Attempt to update old label's label text using invalid characters.
    oldLabel.setLabel("*/a_new_invalid_name");
    Label labelToUpdate = oldLabel;
    assertThatThrownBy(() -> {
      dao.update(labelToUpdate);
    }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Label name", '*');

    // Should be able to update an older label with a valid label.
    oldLabel.setLabel("_.- a valid label -._");
    dao.update(oldLabel);
    oldLabel = dao.getByIdNotNull(oldLabel.getId());
    assertLabel(applicationId, "_.- a valid label -._", Color.light_green, null, oldLabel);
  }

  @Test
  public void testLabelWithSpaces() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("My Label");
    label.setDescription("My label description.");
    label.setColor(Color.dark_blue);

    // Create
    dao.insert(label);

    label = dao.getById(label.getId());
    assertThat(label).isNotNull();
    assertLabel(applicationId, "My Label", Color.dark_blue, "My label description.", label);

    // Update
    label.setLabel("My Updated Label");
    dao.update(label);
    label = dao.getById(label.getId());
    assertThat(label).isNotNull();
    assertLabel(applicationId, "My Updated Label", Color.dark_blue, "My label description.", label);
  }

  @Test
  public void testLabelWithTabs() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("My\tlabel");

    // Insert
    assertThatThrownBy(() -> {
      dao.insert(label);
    }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Label name", '\t');

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel("My\tUpdatedLabel");
    assertThatThrownBy(() -> {
      dao.update(label);
    }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Label name", '\t');
  }

  @Test
  public void testLabelNull() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel(null);

    // Insert
    assertThatThrownBy(() -> {
      dao.insert(label);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Label name is required.");

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel(null);
    assertThatThrownBy(() -> {
      dao.update(label);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Label name is required.");
  }

  @Test
  public void testLabelEmpty() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel(" ");

    // Insert
    assertThatThrownBy(() -> {
      dao.insert(label);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Label name is required.");

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel(" ");
    assertThatThrownBy(() -> {
      dao.update(label);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Label name is required.");
  }

  @Test
  public void testLabelWithTooLongName() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel(StringUtils.repeat("X", LabelDAO.MAX_NAME_SIZE + 1));

    // Insert
    assertThatThrownBy(() -> {
      dao.insert(label);
    }).isInstanceOf(InvalidNameException.class)
        .hasMessage("Label name must be " + LabelDAO.MAX_NAME_SIZE + " characters or less.");

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel(StringUtils.repeat("X", LabelDAO.MAX_NAME_SIZE + 1));
    assertThatThrownBy(() -> {
      dao.update(label);
    }).isInstanceOf(InvalidNameException.class)
        .hasMessage("Label name must be " + LabelDAO.MAX_NAME_SIZE + " characters or less.");
  }

  @Test
  public void testSetColorToNull() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("MyLabel");
    label.setColor(null);

    // Insert
    assertThatThrownBy(() -> {
      dao.insert(label);
    }).isInstanceOf(InvalidLabelException.class).hasMessage("The label color must be assigned.");

    // Update
    label.setColor(Color.dark_blue);
    dao.insert(label);
    label.setColor(null);
    assertThatThrownBy(() -> {
      dao.update(label);
    }).isInstanceOf(InvalidLabelException.class).hasMessage("The label color must be assigned.");
  }

  @Test
  public void testLegacyColorsInvalid() throws Exception {
    @SuppressWarnings("deprecation")
    Color[] legacyColors = new Color[] { Color.white, Color.grey, Color.black, Color.green, Color.red, Color.blue };

    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("MyLabel");

    // Insert
    for (Color color : legacyColors) {
      label.setColor(color);
      assertThatThrownBy(() -> {
        dao.insert(label);
      }
      ).isInstanceOf(InvalidLabelException.class).hasMessage("The label color " + color.toValue() + " is invalid.");
    }

    // Update
    label.setColor(Color.dark_blue);
    dao.insert(label);
    for (Color color : legacyColors) {
      label.setColor(color);
      assertThatThrownBy(() -> {
        dao.update(label);
      }).isInstanceOf(InvalidLabelException.class).hasMessage("The label color " + color.toValue() + " is invalid.");
    }
  }

  @Test
  public void testCRUD() throws Exception {
    LabelDAO dao = new LabelDAO();

    // Create
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("MyLabel");
    label.setColor(Color.dark_blue);
    label.setDescription("My label   description.");
    dao.insert(label);
    assertThat(label.getId()).isNotNull();

    label = dao.getById(label.getId());
    assertThat(label).isNotNull();
    assertLabel(applicationId, "MyLabel", Color.dark_blue, "My label   description.", label);

    // Update
    label.setLabel("MyUpdatedLabel");
    dao.update(label);

    label = dao.getById(label.getId());
    assertThat(label).isNotNull();
    assertLabel(applicationId, "MyUpdatedLabel", Color.dark_blue, "My label   description.", label);

    // Delete
    dao.delete(label);

    label = dao.getById(label.getId());
    assertThat(label).isNull();
  }

  @Test
  public void testCascadeDelete() {
    LabelDAO labelDAO = new LabelDAO();
    ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

    // Create
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("MyLabel");
    label.setColor(Color.dark_blue);
    labelDAO.insert(label);
    assertThat(label.getId()).isNotNull();

    ComponentLabel componentLabel = new ComponentLabel();
    componentLabel.setOwnerId(applicationId);
    componentLabel.setLabelId(label.getId());
    componentLabel.setHash("ababababab");
    componentLabelDAO.insert(componentLabel);

    // Delete
    labelDAO.delete(label);

    label = labelDAO.getById(label.getId());
    assertThat(label).isNull();
  }

  @Test
  public void testAddDuplicateLabel() throws Exception {
    LabelDAO labelDAO = new LabelDAO();

    // Add a label
    {
      Label label = new Label();
      label.setOwnerId(applicationId);
      label.setLabel("MyLabel");
      label.setColor(Color.dark_blue);
      labelDAO.insert(label);
    }

    // Add another label with the same name
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setColor(Color.dark_blue);
    label.setLabel("MyLabel");
    assertThatThrownBy(() -> {
      labelDAO.insert(label);
    }).isInstanceOf(InvalidLabelException.class).hasMessage(
        "A label with name '%s' already exists in application '%s'.", label.getLabel(), application.getName());
  }

  @Test
  public void testUpdateDuplicateLabel() throws Exception {
    LabelDAO labelDAO = new LabelDAO();

    // Add a label
    Label label1 = new Label();
    label1.setOwnerId(applicationId);
    label1.setLabel("MyLabel1");
    label1.setColor(Color.dark_blue);
    labelDAO.insert(label1);

    // Add another label
    Label label2 = new Label();
    label2.setOwnerId(applicationId);
    label2.setColor(Color.dark_blue);
    label2.setLabel("MyLabel2");
    labelDAO.insert(label2);

    // Update without changing the name
    label2.setColor(Color.dark_red);
    labelDAO.update(label2);
    assertLabel(applicationId, "MyLabel2", Color.dark_red, null, label2);

    // Update with a conflicting name
    label2.setLabel(label1.getLabel());
    assertThatThrownBy(() -> {
      labelDAO.update(label2);
    }).isInstanceOf(InvalidLabelException.class).hasMessage(
        "A label with name '%s' already exists in application '%s'.", label1.getLabel(), application.getName());
  }

  @Test
  public void testDuplicateLabelInApplication() throws Exception {
    LabelDAO labelDAO = new LabelDAO();

    Label label1 = new Label();
    label1.setOwnerId(applicationId);
    label1.setLabel("MyLabel");
    label1.setColor(Color.dark_blue);
    labelDAO.insert(label1);

    // direct insert of duplicate label
    assertThatThrownBy(() -> {
      Label label2 = new Label();
      label2.setOwnerId(organization.getId());
      label2.setLabel("MyLabel");
      label2.setColor(Color.dark_blue);
      labelDAO.insert(label2);
    }
    ).isInstanceOf(InvalidLabelException.class).hasMessage(
        "A label with name '%s' already exists in application(s) '%s'.", label1.getLabel(), application.getName());

    // rename label to become a duplicate
    Label label2 = new Label();
    label2.setOwnerId(organization.getId());
    label2.setLabel("MyLabel2");
    label2.setColor(Color.dark_blue);
    labelDAO.insert(label2);
    assertThatThrownBy(() -> {
      label2.setLabel("MyLabel");
      labelDAO.update(label2);
    }).isInstanceOf(InvalidLabelException.class).hasMessage(
        "A label with name '%s' already exists in application(s) '%s'.", label1.getLabel(), application.getName());
  }

  @Test
  public void testDuplicateLabelInOrganization() throws Exception {
    LabelDAO labelDAO = new LabelDAO();

    Label label1 = new Label();
    label1.setOwnerId(organization.getId());
    label1.setLabel("MyLabel");
    label1.setColor(Color.dark_blue);
    labelDAO.insert(label1);

    // direct insert of duplicate label
    assertThatThrownBy(() -> {
      Label label2 = new Label();
      label2.setOwnerId(applicationId);
      label2.setLabel("MyLabel");
      label2.setColor(Color.dark_blue);
      labelDAO.insert(label2);
    }).isInstanceOf(InvalidLabelException.class).hasMessage(
        "A label with name '%s' already exists in organization '%s'.", label1.getLabel(), organization.getName());

    // rename label to become a duplicate
    Label label2 = new Label();
    label2.setOwnerId(applicationId);
    label2.setLabel("MyLabel2");
    label2.setColor(Color.dark_blue);
    labelDAO.insert(label2);
    assertThatThrownBy(() -> {
      label2.setLabel("MyLabel");
      labelDAO.update(label2);
    }).isInstanceOf(InvalidLabelException.class).hasMessage(
        "A label with name '%s' already exists in organization '%s'.", label1.getLabel(), organization.getName());
  }

  @Test
  public void testDuplicateLabelInApplicationAndOrganizationDownHierarchy() throws Exception {
    LabelDAO labelDAO = new LabelDAO();

    Label label1 = tempEntity.newLabel(applicationId, "MyLabel");

    Organization org1 = tempEntity.newOrganization("org1");
    tempEntity.newLabel(org1.getId(), "MyLabel");

    Organization org2 = tempEntity.newOrganization("org2");
    tempEntity.newLabel(org2.getId(), "MyLabel");

    Label label4 = new Label();
    label4.setOwnerId(org1.getParentOrganizationId());
    label4.setLabel("MyLabel");
    label4.setColor(Color.dark_blue);
    assertThatThrownBy(() -> {
      labelDAO.insert(label4);
    }).isInstanceOf(InvalidLabelException.class).hasMessage(
        "A label with name '%s' already exists in application(s) '%s' organization(s) '%s' '%s'.", label1.getLabel(),
        application.getName(), org1.getName(), org2.getName());
  }

  @Test
  public void testDuplicateLabelInOrganizationUpHierarchy() throws Exception {
    LabelDAO labelDAO = new LabelDAO();

    tempEntity.newLabel(organization.getParentOrganizationId(), "MyLabel");

    Label label2 = new Label();
    label2.setOwnerId(organization.getId());
    label2.setLabel("MyLabel");
    label2.setColor(Color.dark_blue);
    assertThatThrownBy(() -> {
      labelDAO.insert(label2);
    }).isInstanceOf(InvalidLabelException.class)
        .hasMessage("A label with name '%s' already exists in organization 'Root Organization'.", label2.getLabel());
  }

  @Test
  public void testGetByOwnerId_inheritedLabels() {
    LabelDAO labelDAO = new LabelDAO();

    Label label1 = tempEntity.newLabel(organization.getParentOrganizationId(), "parent-org-label");
    Label label2 = tempEntity.newLabel(organization.getId(), "org-label");
    Label label3 = tempEntity.newLabel(applicationId, "app-label");

    assertLabels(Arrays.asList(label3), labelDAO.getByOwnerId(applicationId, false));
    assertLabels(Arrays.asList(label1, label2, label3), labelDAO.getByOwnerId(applicationId, true));

    assertLabels(Arrays.asList(label2), labelDAO.getByOwnerId(organization.getId(), false));
    assertLabels(Arrays.asList(label1, label2), labelDAO.getByOwnerId(organization.getId(), true));
  }

  @Test
  public void testGetByIdNotNull() {
    assertThatThrownBy(() -> {
      new LabelDAO().getByIdNotNull("fake id");
    }).isInstanceOf(NotFoundException.class).hasMessage("Cannot find a label with ID fake id.");
  }

  @Test
  public void testLongDescription() {
    LabelDAO labelDAO = new LabelDAO();
    Label label = new Label(organization.getId(), "testLongDescriptionLabel", Color.dark_purple);
    label.setDescription(StringUtils.leftPad("", LabelDAO.MAX_DESC_SIZE + 1, "a"));
    assertThatThrownBy(() -> {
      labelDAO.insert(label);
    }).isInstanceOf(InvalidLabelException.class).hasMessageStartingWith("The label description can't be longer than");
    label.setDescription("valid");
    labelDAO.insert(label);
    label.setDescription(StringUtils.leftPad("", LabelDAO.MAX_DESC_SIZE + 1, "a"));
    assertThatThrownBy(() -> {
      labelDAO.update(label);
    }).isInstanceOf(InvalidLabelException.class).hasMessageStartingWith("The label description can't be longer than");
  }

  private void assertLabels(Collection<Label> expected, Collection<Label> actual) {
    assertThat(actual).usingElementComparator(Comparator.comparing(Label::getId))
        .containsExactlyInAnyOrderElementsOf(expected);
  }

  private void assertLabel(String applicationId, String label, Color color, String description, Label actual) {
    assertThat(actual.getOwnerId()).isEqualTo(applicationId);
    assertThat(actual.getLabel()).isEqualTo(label);
    assertThat(actual.getLabelLowercase()).isEqualTo(label.toLowerCase(Locale.ENGLISH));
    assertThat(actual.getColor()).isEqualTo(color);
    assertThat(actual.getDescription()).isEqualTo(description);
  }
}
