/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LabelDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testLabelWithSpaces() throws Exception {
    LabelDAO dao = new LabelDAO();
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("My label");

    // Insert
    try {
      dao.insert(label);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      if (!"The label text cannot contain spaces".equals(expected.getMessage())) {
        throw expected;
      }
    }

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel("My UpdatedLabel");
    try {
      dao.update(label);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      if (!"The label text cannot contain spaces".equals(expected.getMessage())) {
        throw expected;
      }
    }
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
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      if (!"The label text cannot contain tabs".equals(expected.getMessage())) {
        throw expected;
      }
    }

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel("My\tUpdatedLabel");
    try {
      dao.update(label);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      if (!"The label text cannot contain tabs".equals(expected.getMessage())) {
        throw expected;
      }
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
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      if (!"The label text cannot be null or empty".equals(expected.getMessage())) {
        throw expected;
      }
    }

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel(null);
    try {
      dao.update(label);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      if (!"The label text cannot be null or empty".equals(expected.getMessage())) {
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
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      if (!"The label text cannot be null or empty".equals(expected.getMessage())) {
        throw expected;
      }
    }

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel(" ");
    try {
      dao.update(label);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException expected) {
      if (!"The label text cannot be null or empty".equals(expected.getMessage())) {
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
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException e) {
      assertEquals("The label text must not exceed 50 characters", e.getMessage());
    }

    // Update
    label.setLabel("MyLabel");
    dao.insert(label);
    label.setLabel(StringUtils.repeat("X", LabelDAO.MAX_NAME_SIZE + 1));
    try {
      dao.update(label);
      fail("Expected InvalidLabelException");
    }
    catch (InvalidLabelException e) {
      assertEquals("The label text must not exceed 50 characters", e.getMessage());
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
    label.setColor(Color.white);
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
  public void testCRUD() throws Exception {
    LabelDAO dao = new LabelDAO();

    // Create
    Label label = new Label();
    label.setOwnerId(applicationId);
    label.setLabel("MyLabel");
    label.setColor(Color.blue);
    label.setDescription("My label   description.");
    dao.insert(label);
    Assert.assertNotNull(label.getId());

    label = dao.getById(label.getId());
    Assert.assertNotNull(label);
    assertLabel(applicationId, "MyLabel", Color.blue, "My label   description.", label);

    // Update
    label.setLabel("MyUpdatedLabel");
    dao.update(label);

    label = dao.getById(label.getId());
    Assert.assertNotNull(label);
    assertLabel(applicationId, "MyUpdatedLabel", Color.blue, "My label   description.", label);

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
    label.setColor(Color.blue);
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
    label.setColor(Color.blue);
    labelDAO.insert(label);

    // Add another label with the same name
    label = new Label();
    label.setOwnerId(applicationId);
    label.setColor(Color.blue);
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
    label1.setColor(Color.blue);
    labelDAO.insert(label1);

    // Add another label
    Label label2 = new Label();
    label2.setOwnerId(applicationId);
    label2.setColor(Color.blue);
    label2.setLabel("MyLabel2");
    labelDAO.insert(label2);

    // Update without changing the name
    label2.setColor(Color.red);
    labelDAO.update(label2);
    assertLabel(applicationId, "MyLabel2", Color.red, null, label2);

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
    label1.setColor(Color.blue);
    labelDAO.insert(label1);

    // direct insert of duplicate label
    try {
      Label label2 = new Label();
      label2.setOwnerId(organization.getId());
      label2.setLabel("MyLabel");
      label2.setColor(Color.blue);
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
    label2.setColor(Color.blue);
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
    label1.setColor(Color.blue);
    labelDAO.insert(label1);

    // direct insert of duplicate label
    try {
      Label label2 = new Label();
      label2.setOwnerId(applicationId);
      label2.setLabel("MyLabel");
      label2.setColor(Color.blue);
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
    label2.setColor(Color.blue);
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
  public void testGetByOwnerId_inheritedLabels() {
    LabelDAO labelDAO = new LabelDAO();

    Label label1 = new Label();
    label1.setOwnerId(organization.getId());
    label1.setLabel("org-label");
    label1.setColor(Color.blue);
    labelDAO.insert(label1);

    Label label2 = new Label();
    label2.setOwnerId(applicationId);
    label2.setLabel("app-label");
    label2.setColor(Color.blue);
    labelDAO.insert(label2);

    assertLabels(Arrays.asList(label2), labelDAO.getByOwnerId(applicationId, false));

    assertLabels(Arrays.asList(label1, label2), labelDAO.getByOwnerId(applicationId, true));
  }

  @Test
  public void testGetByIdNotNull() {
    try {
      new LabelDAO().getByIdNotNull("fake id");
      fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      assertEquals("Cannot find a label with id fake id", expected.getMessage());
    }
  }

  @Test
  public void testLongDescription() {
    LabelDAO labelDAO = new LabelDAO();
    Label label = new Label(organization.getId(), "testLongDescriptionLabel", Color.black);
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
    final Map<String, Label> actualMap = new HashMap<String, Label>();
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
