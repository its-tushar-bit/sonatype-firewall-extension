/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LabelDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testValidateLabelNameInvalidChars_Insert() {
    LabelDAO dao = new LabelDAO();
    for (String labelName : NameHelperTest.INVALID_CHARACTERS) {
      try {
        dao.insert(new Label(applicationId, labelName, Color.light_green));
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException e) {
        assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Label name", labelName.charAt(0)), e.getMessage());
      }
    }
  }

  @Test
  public void testValidateLabelNameInvalidChars_Update() {
    LabelDAO dao = new LabelDAO();
    Label label = tempEntity.newLabel(applicationId, "label", Color.light_green);
    for (String labelName : NameHelperTest.INVALID_CHARACTERS) {
      label.setLabel(labelName);
      try {
        dao.update(label);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException e) {
        assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Label name", labelName.charAt(0)), e.getMessage());
      }
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
    try {
      dao.update(oldLabel);
      Assert.fail("Updates to older labels should be validated.");
    }
    catch (InvalidNameException e) {
      assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Label name", '*'), e.getMessage());
    }

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
    Assert.assertNotNull(label);
    assertLabel(applicationId, "My Label", Color.dark_blue, "My label description.", label);

    // Update
    label.setLabel("My Updated Label");
    dao.update(label);
    label = dao.getById(label.getId());
    Assert.assertNotNull(label);
    assertLabel(applicationId, "My Updated Label", Color.dark_blue, "My label description.", label);
  }

  @Test
  public void testLabelWithTabs() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("My\tlabel");

    // Insert
    try {
      dao.insert(label);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Label name", '\t'), expected.getMessage());
    }

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel("My\tUpdatedLabel");
    try {
      dao.update(label);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Label name", '\t'), expected.getMessage());
    }
  }

  @Test
  public void testLabelNull() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel(null);

    // Insert
    try {
      dao.insert(label);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      if (!"Label name is required.".equals(expected.getMessage())) {
        throw expected;
      }
    }

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel(null);
    try {
      dao.update(label);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      if (!"Label name is required.".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testLabelEmpty() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel(" ");

    // Insert
    try {
      dao.insert(label);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      if (!"Label name is required.".equals(expected.getMessage())) {
        throw expected;
      }
    }

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel(" ");
    try {
      dao.update(label);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      if (!"Label name is required.".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testLabelWithTooLongName() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel(StringUtils.repeat("X", LabelDAO.MAX_NAME_SIZE + 1));

    // Insert
    try {
      dao.insert(label);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException e) {
      assertEquals("Label name must be " + LabelDAO.MAX_NAME_SIZE + " characters or less.", e.getMessage());
    }

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel(StringUtils.repeat("X", LabelDAO.MAX_NAME_SIZE + 1));
    try {
      dao.update(label);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException e) {
      assertEquals("Label name must be " + LabelDAO.MAX_NAME_SIZE + " characters or less.", e.getMessage());
    }
  }

  @Test
  public void testSetColorToNull() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("MyLabel");
    label.setColor(null);

    // Insert
    try {
      dao.insert(label);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException e) {
      assertEquals("The label color must be assigned.", e.getMessage());
    }

    // Update
    label.setColor(Color.dark_blue);
    dao.insert(label);
    label.setColor(null);
    try {
      dao.update(label);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException e) {
      assertEquals("The label color must be assigned.", e.getMessage());
    }
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
      try {
        label.setColor(color);
        dao.insert(label);
        fail("Expected InvalidLabelException");
      }
      catch (InvalidLabelException e) {
        assertEquals("The label color " + color.toValue() + " is invalid.", e.getMessage());
      }
    }

    // Update
    label.setColor(Color.dark_blue);
    dao.insert(label);
    for (Color color : legacyColors) {
      try {
        label.setColor(color);
        dao.update(label);
        fail("Expected InvalidLabelException");
      }
      catch (InvalidLabelException e) {
        assertEquals("The label color " + color.toValue() + " is invalid.", e.getMessage());
      }
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
    Assert.assertNotNull(label.getId());

    label = dao.getById(label.getId());
    Assert.assertNotNull(label);
    assertLabel(applicationId, "MyLabel", Color.dark_blue, "My label   description.", label);

    // Update
    label.setLabel("MyUpdatedLabel");
    dao.update(label);

    label = dao.getById(label.getId());
    Assert.assertNotNull(label);
    assertLabel(applicationId, "MyUpdatedLabel", Color.dark_blue, "My label   description.", label);

    // Delete
    dao.delete(label);

    label = dao.getById(label.getId());
    Assert.assertNull(label);
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
    Assert.assertNotNull(label.getId());

    ComponentLabel componentLabel = new ComponentLabel();
    componentLabel.setOwnerId(applicationId);
    componentLabel.setLabelId(label.getId());
    componentLabel.setHash("ababababab");
    componentLabelDAO.insert(componentLabel);

    // Delete
    labelDAO.delete(label);

    label = labelDAO.getById(label.getId());
    Assert.assertNull(label);
  }

  @Test
  public void testAddDuplicateLabel() throws Exception {
    LabelDAO labelDAO = new LabelDAO();

    // Add a label
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("MyLabel");
    label.setColor(Color.dark_blue);
    labelDAO.insert(label);

    // Add another label with the same name
    label = new Label();
    label.setOwnerId(applicationId);
    label.setColor(Color.dark_blue);
    label.setLabel("MyLabel");
    try {
      labelDAO.insert(label);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      String expectedMessage = String.format("A label with name '%s' already exists in application '%s'.",
          label.getLabel(), application.getName());
      if (!expectedMessage.equals(expected.getMessage())) {
        throw expected;
      }
    }
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
    try {
      labelDAO.update(label2);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      String expectedMessage = String.format("A label with name '%s' already exists in application '%s'.",
          label1.getLabel(), application.getName());
      if (!expectedMessage.equals(expected.getMessage())) {
        throw expected;
      }
    }
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
    try {
      Label label2 = new Label();
      label2.setOwnerId(organization.getId());
      label2.setLabel("MyLabel");
      label2.setColor(Color.dark_blue);
      labelDAO.insert(label2);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      final String expectedMessage = String.format("A label with name '%s' already exists in application(s) '%s'.",
          label1.getLabel(), application.getName());
      if (!expectedMessage.equals(expected.getMessage())) {
        throw expected;
      }
    }

    // rename label to become a duplicate
    Label label2 = new Label();
    label2.setOwnerId(organization.getId());
    label2.setLabel("MyLabel2");
    label2.setColor(Color.dark_blue);
    labelDAO.insert(label2);
    try {
      label2.setLabel("MyLabel");
      labelDAO.update(label2);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      String expectedMessage = String.format("A label with name '%s' already exists in application(s) '%s'.",
          label1.getLabel(), application.getName());
      if (!expectedMessage.equals(expected.getMessage())) {
        throw expected;
      }
    }

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
    try {
      Label label2 = new Label();
      label2.setOwnerId(applicationId);
      label2.setLabel("MyLabel");
      label2.setColor(Color.dark_blue);
      labelDAO.insert(label2);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      String expectedMessage = String.format("A label with name '%s' already exists in organization '%s'.",
          label1.getLabel(), organization.getName());
      if (!expectedMessage.equals(expected.getMessage())) {
        throw expected;
      }
    }

    // rename label to become a duplicate
    Label label2 = new Label();
    label2.setOwnerId(applicationId);
    label2.setLabel("MyLabel2");
    label2.setColor(Color.dark_blue);
    labelDAO.insert(label2);
    try {
      label2.setLabel("MyLabel");
      labelDAO.update(label2);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      final String expectedMessage = String.format("A label with name '%s' already exists in organization '%s'.",
          label1.getLabel(), organization.getName());
      if (!expectedMessage.equals(expected.getMessage())) {
        throw expected;
      }
    }

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
    try {
      labelDAO.insert(label4);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException e) {
      final String expectedMessage = String.format(
          "A label with name '%s' already exists in application(s) '%s' organization(s) '%s' '%s'.", label1.getLabel(),
          application.getName(), org1.getName(), org2.getName());
      assertThat(e.getMessage(), is(expectedMessage));
    }
  }

  @Test
  public void testDuplicateLabelInOrganizationUpHierarchy() throws Exception {
    LabelDAO labelDAO = new LabelDAO();

    tempEntity.newLabel(organization.getParentOrganizationId(), "MyLabel");

    Label label2 = new Label();
    label2.setOwnerId(organization.getId());
    label2.setLabel("MyLabel");
    label2.setColor(Color.dark_blue);
    try {
      labelDAO.insert(label2);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException e) {
      final String expectedMessage = String.format(
          "A label with name '%s' already exists in organization 'Root Organization'.", label2.getLabel());
      assertThat(e.getMessage(), is(expectedMessage));
    }
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
    try {
      new LabelDAO().getByIdNotNull("fake id");
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("Cannot find a label with ID fake id.", expected.getMessage());
    }
  }

  @Test
  public void testLongDescription() {
    LabelDAO labelDAO = new LabelDAO();
    Label label = new Label(organization.getId(), "testLongDescriptionLabel", Color.dark_purple);
    label.setDescription(StringUtils.leftPad("", LabelDAO.MAX_DESC_SIZE + 1, "a"));
    try {
      labelDAO.insert(label);
      fail("Should have thrown InvalidLabelException");
    }
    catch (InvalidLabelException e) {
      assertThat(e.getMessage(), startsWith("The label description can't be longer than"));
    }
    label.setDescription("valid");
    labelDAO.insert(label);
    label.setDescription(StringUtils.leftPad("", LabelDAO.MAX_DESC_SIZE + 1, "a"));
    try {
      labelDAO.update(label);
      fail("Should have thrown InvalidLabelException");
    }
    catch (InvalidLabelException e) {
      assertThat(e.getMessage(), startsWith("The label description can't be longer than"));
    }
  }

  private void assertLabels(Collection<Label> expected, Collection<Label> actual) {
    final Map<String, Label> expectedMap = toLabelsMap(expected);
    final Map<String, Label> actualMap = toLabelsMap(actual);

    assertEquals(expectedMap.keySet(), actualMap.keySet());
  }

  private Map<String, Label> toLabelsMap(Collection<Label> actual) {
    final Map<String, Label> actualMap = new HashMap<>();
    for (Label label : actual) {
      actualMap.put(label.getId(), label);
    }
    return actualMap;
  }

  private void assertLabel(String applicationId, String label, Color color, String description, Label actual) {
    assertEquals(applicationId, actual.getOwnerId());
    assertEquals(label, actual.getLabel());
    assertEquals(label.toLowerCase(Locale.ENGLISH), actual.getLabelLowercase());
    assertEquals(color, actual.getColor());
    assertEquals(description, actual.getDescription());
  }
}
