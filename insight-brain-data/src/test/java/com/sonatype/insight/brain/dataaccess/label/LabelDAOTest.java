/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LabelDAOTest
    extends AbstractDbDAOTest
{
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private SearchIndexChangeDAO searchIndexChangeDAO;

  private ComponentLabelDAO componentLabelDAO;

  private LabelDAO labelDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    searchIndexChangeDAO = daoFactory.createSearchIndexChangeDAO();
    labelDAO = daoFactory.createLabelDAO();
    componentLabelDAO = daoFactory.createComponentLabelDAO();
  }

  @Test
  public void testValidateLabelNameInvalidChars_Insert() {
    for (String labelName : NameHelperTest.INVALID_CHARACTERS) {
      assertThatThrownBy(
          () -> labelDAO.insert(new Label(application.getId(), labelName, Color.light_green))).isInstanceOf(
              InvalidNameException.class)
              .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Label name",
                  labelName.charAt(0));
    }
  }

  @Test
  public void testValidateLabelNameInvalidChars_Update() {
    Label label = tempEntity.newLabel(application.getId(), "label", Color.light_green);
    for (String labelName : NameHelperTest.INVALID_CHARACTERS) {
      label.setLabel(labelName);
      assertThatThrownBy(() -> labelDAO.update(label)).isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Label name",
              labelName.charAt(0));
    }
  }

  @Test
  public void testValidateLabelNameValidChars_Insert() {
    for (String labelName : NameHelperTest.VALID_NAMES) {
      tempEntity.newLabel(application.getId(), labelName, Color.light_green);
    }
  }

  @Test
  public void testValidateLabelNameValidChars_Update() {
    Label label = tempEntity.newLabel(application.getId(), "label", Color.light_green);
    for (String labelName : NameHelperTest.VALID_NAMES) {
      label.setLabel(labelName);
      labelDAO.update(label);
    }
  }

  @Test
  public void testOlderLabelUpdate() {
    Label oldLabel =
        tempEntity.newLabelWithInvalidLabelText(application.getId(), "*/clearly_not_valid", Color.dark_blue);

    // Update old label without changing label text.
    oldLabel.setColor(Color.light_green);
    labelDAO.update(oldLabel);
    assertLabel(application.getId(), "*/clearly_not_valid", Color.light_green, null, oldLabel);

    // Attempt to update old label's label text using invalid characters.
    oldLabel.setLabel("*/a_new_invalid_name");
    Label labelToUpdate = oldLabel;
    assertThatThrownBy(() -> labelDAO.update(labelToUpdate)).isInstanceOf(InvalidNameException.class)
        .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Label name", '*');

    // Should be able to update an older label with a valid label.
    oldLabel.setLabel("_.- a valid label -._");
    labelDAO.update(oldLabel);
    oldLabel = labelDAO.getByIdNotNull(oldLabel.getId());
    assertLabel(application.getId(), "_.- a valid label -._", Color.light_green, null, oldLabel);
  }

  @Test
  public void testLabelWithSpaces() {
    Label label = new Label();
    label.setOwnerId(application.getId());
    label.setLabel("My Label");
    label.setDescription("My label description.");
    label.setColor(Color.dark_blue);

    // Create
    labelDAO.insert(label);

    label = labelDAO.getById(label.getId());
    assertThat(label).isNotNull();
    assertLabel(application.getId(), "My Label", Color.dark_blue, "My label description.", label);

    // Update
    label.setLabel("My Updated Label");
    labelDAO.update(label);
    label = labelDAO.getById(label.getId());
    assertThat(label).isNotNull();
    assertLabel(application.getId(), "My Updated Label", Color.dark_blue, "My label description.", label);
  }

  @Test
  public void testLabelWithTabs() {
    Label label = new Label();
    label.setOwnerId(application.getId());
    label.setLabel("My\tlabel");

    // Insert
    assertThatThrownBy(() -> labelDAO.insert(label)).isInstanceOf(InvalidNameException.class)
        .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Label name", '\t');

    // Update
    label.setLabel("MyLabel");
    labelDAO.insert(label);
    label.setLabel("My\tUpdatedLabel");
    assertThatThrownBy(() -> labelDAO.update(label)).isInstanceOf(InvalidNameException.class)
        .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Label name", '\t');
  }

  @Test
  public void testLabelNull() {
    Label label = new Label();
    label.setOwnerId(application.getId());
    label.setLabel(null);

    // Insert
    assertThatThrownBy(() -> labelDAO.insert(label)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Label name is required.");

    // Update
    label.setLabel("MyLabel");
    labelDAO.insert(label);
    label.setLabel(null);
    assertThatThrownBy(() -> labelDAO.update(label)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Label name is required.");
  }

  @Test
  public void testLabelEmpty() {
    Label label = new Label();
    label.setOwnerId(application.getId());
    label.setLabel(" ");

    // Insert
    assertThatThrownBy(() -> labelDAO.insert(label)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Label name is required.");

    // Update
    label.setLabel("MyLabel");
    labelDAO.insert(label);
    label.setLabel(" ");
    assertThatThrownBy(() -> labelDAO.update(label)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Label name is required.");
  }

  @Test
  public void testLabelWithTooLongName() {
    Label label = new Label();
    label.setOwnerId(application.getId());
    label.setLabel(StringUtils.repeat("X", LabelDAO.MAX_NAME_SIZE + 1));

    // Insert
    assertThatThrownBy(() -> labelDAO.insert(label)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Label name must be " + LabelDAO.MAX_NAME_SIZE + " characters or less.");

    // Update
    label.setLabel("MyLabel");
    labelDAO.insert(label);
    label.setLabel(StringUtils.repeat("X", LabelDAO.MAX_NAME_SIZE + 1));
    assertThatThrownBy(() -> labelDAO.update(label)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Label name must be " + LabelDAO.MAX_NAME_SIZE + " characters or less.");
  }

  @Test
  public void testSetColorToNull() {
    Label label = new Label();
    label.setOwnerId(application.getId());
    label.setLabel("MyLabel");
    label.setColor(null);

    // Insert
    assertThatThrownBy(() -> labelDAO.insert(label)).isInstanceOf(InvalidLabelException.class)
        .hasMessage("The label color must be assigned.");

    // Update
    label.setColor(Color.dark_blue);
    labelDAO.insert(label);
    label.setColor(null);
    assertThatThrownBy(() -> labelDAO.update(label)).isInstanceOf(InvalidLabelException.class)
        .hasMessage("The label color must be assigned.");
  }

  @Test
  public void testLegacyColorsInvalid() {
    @SuppressWarnings("deprecation")
    Color[] legacyColors = new Color[]{Color.white, Color.grey, Color.black, Color.green, Color.red, Color.blue};

    Label label = new Label();
    label.setOwnerId(application.getId());
    label.setLabel("MyLabel");

    // Insert
    for (Color color : legacyColors) {
      label.setColor(color);
      assertThatThrownBy(() -> labelDAO.insert(label)).isInstanceOf(InvalidLabelException.class)
          .hasMessage("The label color " + color.toValue() + " is invalid.");
    }

    // Update
    label.setColor(Color.dark_blue);
    labelDAO.insert(label);
    for (Color color : legacyColors) {
      label.setColor(color);
      assertThatThrownBy(() -> labelDAO.update(label)).isInstanceOf(InvalidLabelException.class)
          .hasMessage("The label color " + color.toValue() + " is invalid.");
    }
  }

  @Test
  public void testCRUD() {
    // Create
    Label label = new Label();
    label.setOwnerId(application.getId());
    label.setLabel("MyLabel");
    label.setColor(Color.dark_blue);
    label.setDescription("My label   description.");
    labelDAO.insert(label);
    assertThat(label.getId()).isNotNull();

    label = labelDAO.getById(label.getId());
    assertThat(label).isNotNull();
    assertLabel(application.getId(), "MyLabel", Color.dark_blue, "My label   description.", label);

    // Update
    label.setLabel("MyUpdatedLabel");
    labelDAO.update(label);

    label = labelDAO.getById(label.getId());
    assertThat(label).isNotNull();
    assertLabel(application.getId(), "MyUpdatedLabel", Color.dark_blue, "My label   description.", label);

    // Delete
    labelDAO.delete(label);

    label = labelDAO.getById(label.getId());
    assertThat(label).isNull();
  }

  @Test
  public void testCascadeDelete() {
    // Create
    Label label = new Label();
    label.setOwnerId(application.getId());
    label.setLabel("MyLabel");
    label.setColor(Color.dark_blue);
    labelDAO.insert(label);
    assertThat(label.getId()).isNotNull();

    ComponentLabel componentLabel = new ComponentLabel();
    componentLabel.setOwnerId(application.getId());
    componentLabel.setLabelId(label.getId());
    componentLabel.setHash("ababababab");
    componentLabelDAO.insert(componentLabel);

    // Delete
    labelDAO.delete(label);

    label = labelDAO.getById(label.getId());
    assertThat(label).isNull();
  }

  @Test
  public void testAddDuplicateLabel() {
    // Add a label
    {
      Label label = new Label();
      label.setOwnerId(application.getId());
      label.setLabel("MyLabel");
      label.setColor(Color.dark_blue);
      labelDAO.insert(label);
    }

    // Add another label with the same name
    Label label = new Label();
    label.setOwnerId(application.getId());
    label.setColor(Color.dark_blue);
    label.setLabel("MyLabel");
    assertThatThrownBy(() -> labelDAO.insert(label)).isInstanceOf(InvalidLabelException.class)
        .hasMessage(
            "A label with name '%s' already exists in application '%s'.", label.getLabel(), application.getName());
  }

  @Test
  public void testUpdateDuplicateLabel() {
    // Add a label
    Label label1 = new Label();
    label1.setOwnerId(application.getId());
    label1.setLabel("MyLabel1");
    label1.setColor(Color.dark_blue);
    labelDAO.insert(label1);

    // Add another label
    Label label2 = new Label();
    label2.setOwnerId(application.getId());
    label2.setColor(Color.dark_blue);
    label2.setLabel("MyLabel2");
    labelDAO.insert(label2);

    // Update without changing the name
    label2.setColor(Color.dark_red);
    labelDAO.update(label2);
    assertLabel(application.getId(), "MyLabel2", Color.dark_red, null, label2);

    // Update with a conflicting name
    label2.setLabel(label1.getLabel());
    assertThatThrownBy(() -> labelDAO.update(label2)).isInstanceOf(InvalidLabelException.class)
        .hasMessage(
            "A label with name '%s' already exists in application '%s'.", label1.getLabel(), application.getName());
  }

  @Test
  public void testDuplicateLabelInApplication() {
    Label label1 = new Label();
    label1.setOwnerId(application.getId());
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
    }).isInstanceOf(InvalidLabelException.class)
        .hasMessage(
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
    }).isInstanceOf(InvalidLabelException.class)
        .hasMessage(
            "A label with name '%s' already exists in application(s) '%s'.", label1.getLabel(), application.getName());
  }

  @Test
  public void testDuplicateLabelInOrganization() {
    Label label1 = new Label();
    label1.setOwnerId(organization.getId());
    label1.setLabel("MyLabel");
    label1.setColor(Color.dark_blue);
    labelDAO.insert(label1);

    // direct insert of duplicate label
    assertThatThrownBy(() -> {
      Label label2 = new Label();
      label2.setOwnerId(application.getId());
      label2.setLabel("MyLabel");
      label2.setColor(Color.dark_blue);
      labelDAO.insert(label2);
    }).isInstanceOf(InvalidLabelException.class)
        .hasMessage(
            "A label with name '%s' already exists in organization '%s'.", label1.getLabel(), organization.getName());

    // rename label to become a duplicate
    Label label2 = new Label();
    label2.setOwnerId(application.getId());
    label2.setLabel("MyLabel2");
    label2.setColor(Color.dark_blue);
    labelDAO.insert(label2);
    assertThatThrownBy(() -> {
      label2.setLabel("MyLabel");
      labelDAO.update(label2);
    }).isInstanceOf(InvalidLabelException.class)
        .hasMessage(
            "A label with name '%s' already exists in organization '%s'.", label1.getLabel(), organization.getName());
  }

  @Test
  public void testDuplicateLabelInApplicationAndOrganizationDownHierarchy() {
    Label label1 = tempEntity.newLabel(application.getId(), "MyLabel");

    Organization org1 = tempEntity.newOrganization("org1");
    tempEntity.newLabel(org1.getId(), "MyLabel");

    Organization org2 = tempEntity.newOrganization("org2");
    tempEntity.newLabel(org2.getId(), "MyLabel");

    Label label4 = new Label();
    label4.setOwnerId(org1.getParentOrganizationId());
    label4.setLabel("MyLabel");
    label4.setColor(Color.dark_blue);
    assertThatThrownBy(() -> labelDAO.insert(label4)).isInstanceOf(InvalidLabelException.class)
        .hasMessage(
            "A label with name '%s' already exists in application(s) '%s' organization(s) '%s' '%s'.",
            label1.getLabel(),
            application.getName(), org1.getName(), org2.getName());
  }

  @Test
  public void testDuplicateLabelInOrganizationUpHierarchy() {
    tempEntity.newLabel(organization.getParentOrganizationId(), "MyLabel");

    Label label2 = new Label();
    label2.setOwnerId(organization.getId());
    label2.setLabel("MyLabel");
    label2.setColor(Color.dark_blue);
    assertThatThrownBy(() -> labelDAO.insert(label2)).isInstanceOf(InvalidLabelException.class)
        .hasMessage("A label with name '%s' already exists in organization 'Root Organization'.", label2.getLabel());
  }

  @Test
  public void testGetByOwnerId() {
    tempEntity.newLabel(organization.getParentOrganizationId(), "parent-org-label");
    Label label2 = tempEntity.newLabel(organization.getId(), "org-label");
    Label label3 = tempEntity.newLabel(application.getId(), "app-label");

    assertLabels(Collections.singletonList(label3), labelDAO.getByOwnerId(application.getId()));

    assertLabels(Collections.singletonList(label2), labelDAO.getByOwnerId(organization.getId()));
  }

  @Test
  public void testGetByOwnerIdWithHierarchy() {
    Label label1 = tempEntity.newLabel(organization.getParentOrganizationId(), "parent-org-label");
    Label label2 = tempEntity.newLabel(organization.getId(), "org-label");
    Label label3 = tempEntity.newLabel(application.getId(), "app-label");

    assertLabels(Arrays.asList(label1, label2, label3), labelDAO.getByOwnerIdWithHierarchy(application.getId()));

    assertLabels(Arrays.asList(label1, label2), labelDAO.getByOwnerIdWithHierarchy(organization.getId()));
  }

  @Test
  public void testGetByIdNotNull() {
    assertThatThrownBy(() -> labelDAO.getByIdNotNull("fake id")).isInstanceOf(NotFoundException.class)
        .hasMessage("Label with ID fake id does not exist.");
  }

  @Test
  public void testLongDescription() {
    Label label = new Label(organization.getId(), "testLongDescriptionLabel", Color.dark_purple);
    label.setDescription(StringUtils.leftPad("", LabelDAO.MAX_DESC_SIZE + 1, "a"));
    assertThatThrownBy(() -> labelDAO.insert(label)).isInstanceOf(InvalidLabelException.class)
        .hasMessageStartingWith("The label description can't be longer than");
    label.setDescription("valid");
    labelDAO.insert(label);
    label.setDescription(StringUtils.leftPad("", LabelDAO.MAX_DESC_SIZE + 1, "a"));
    assertThatThrownBy(() -> labelDAO.update(label)).isInstanceOf(InvalidLabelException.class)
        .hasMessageStartingWith("The label description can't be longer than");
  }

  @Test
  public void testCRUD_RecordSearchIndexChange() {
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.LABEL);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(label.getId());
    searchIndexChangeDAO.delete(searchIndexChanges.get(0));

    labelDAO.update(label);
    searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.LABEL);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(label.getId());
    searchIndexChangeDAO.delete(searchIndexChanges.get(0));

    labelDAO.delete(label);
    searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.LABEL);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(label.getId());
  }

  @Test
  public void testGetByOwnerIdAndLabel() {
    String labelText = "Org-Label";
    tempEntity.newLabel(tempEntity.newOrganization().getId(), labelText);
    tempEntity.newLabel(organization.getId(), "Another-" + labelText);

    assertThat(labelDAO.getByLabelWithHierarchy(labelText, application.getId())).isNull();

    Label label = tempEntity.newLabel(organization.getId(), labelText);

    Label found = labelDAO.getByLabelWithHierarchy(labelText, application.getId());
    assertThat(found).isNotNull();
    assertThat(found.getId()).isEqualTo(label.getId());
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
