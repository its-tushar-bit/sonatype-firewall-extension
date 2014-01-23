/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.error.exception.BadRequestException;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ComponentLabelDAOTest
    extends AbstractDbDAOTest
{
  private final String hash = "ababababab";

  @Before
  public void before() {
    createDefaultApplication();
  }

  private Label newLabel(String name, String ownerId) {
    LabelDAO labelDAO = new LabelDAO();
    Label label = new Label();
    label.setLabel(name);
    label.setOwnerId(ownerId);
    labelDAO.insert(label);
    return label;
  }

  @Test
  public void testCRUD() {
    Label label = newLabel("label", application.getOrganizationId());

    ComponentLabelDAO dao = new ComponentLabelDAO();
    ComponentLabel compLabel = new ComponentLabel(applicationId, label.getId(), hash);
    dao.insert(compLabel);

    compLabel = dao.getById(compLabel.getId());
    Assert.assertThat(compLabel, IsNull.notNullValue());

    dao.update(compLabel);
    compLabel.setOwnerId(application.getOrganizationId());
    dao.update(compLabel);
    compLabel = dao.getById(compLabel.getId());
    Assert.assertThat(compLabel.getOwnerId(), IsEqual.equalTo(application.getOrganizationId()));

    dao.delete(compLabel);
    compLabel = dao.getById(compLabel.getId());
    Assert.assertThat(compLabel, IsNull.nullValue());
  }

  @Test
  public void testGetByOwnerIdAndHashAndLabelId() {
    Label label = newLabel("label", applicationId);

    ComponentLabelDAO dao = new ComponentLabelDAO();
    ComponentLabel compLabel = new ComponentLabel(applicationId, label.getId(), hash);
    dao.insert(compLabel);
    ComponentLabel entity = dao.getByOwnerIdAndHashAndLabelId(applicationId, hash, label.getId());
    Assert.assertThat(entity, IsNull.notNullValue());
    Assert.assertThat(entity.getId(), IsEqual.equalTo(compLabel.getId()));
  }

  @Test
  public void testInsertDuplicate() {
    Label label = newLabel("label", applicationId);

    ComponentLabelDAO dao = new ComponentLabelDAO();
    ComponentLabel compLabel = new ComponentLabel(applicationId, label.getId(), hash);
    dao.insert(compLabel);
    compLabel = new ComponentLabel(applicationId, label.getId(), hash);
    try {
      dao.insert(compLabel);
      Assert.fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      Assert.assertThat(expected.getMessage(),
          IsEqual.equalTo("The label 'label' is already applied to the component ababababab"));
    }
  }

  @Test
  public void testInsertNonApplicable() {
    Label label = newLabel("label", applicationId);

    ComponentLabelDAO dao = new ComponentLabelDAO();
    ComponentLabel compLabel = new ComponentLabel(application.getOrganizationId(), label.getId(), hash);
    try {
      dao.insert(compLabel);
      Assert.fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      Assert.assertThat(
          expected.getMessage(),
          IsEqual.equalTo("The label 'label' is not applicable for the selected context "
              + application.getOrganizationId()));
    }
  }

  @Test
  public void testUpdateDuplicate() {
    Label label = newLabel("label", applicationId);

    ComponentLabelDAO dao = new ComponentLabelDAO();
    dao.insert(new ComponentLabel(applicationId, label.getId(), hash));
    ComponentLabel compLabel = new ComponentLabel(applicationId, label.getId(), hash + "0");
    dao.insert(compLabel);
    compLabel.setHash(hash);
    try {
      dao.update(compLabel);
      Assert.fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      Assert.assertThat(expected.getMessage(),
          IsEqual.equalTo("The label 'label' is already applied to the component ababababab"));
    }
  }

  @Test
  public void testUpdateNonApplicable() {
    Label label = newLabel("label", applicationId);

    ComponentLabelDAO dao = new ComponentLabelDAO();
    ComponentLabel compLabel = new ComponentLabel(applicationId, label.getId(), hash);
    dao.insert(compLabel);
    compLabel.setOwnerId(application.getOrganizationId());
    try {
      dao.update(compLabel);
      Assert.fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      Assert.assertThat(
          expected.getMessage(),
          IsEqual.equalTo("The label 'label' is not applicable for the selected context "
              + application.getOrganizationId()));
    }
  }

  @Test
  public void testOrganizationInheritance() {
    final LabelDAO labelDAO = new LabelDAO();
    final Label orgLabel = new Label();
    orgLabel.setColor(Color.white);
    orgLabel.setLabel("org-label");
    orgLabel.setOwnerId(organization.getId());
    labelDAO.insert(orgLabel);
    final Label appLabel = new Label();
    appLabel.setColor(Color.white);
    appLabel.setLabel("app-label");
    appLabel.setOwnerId(application.getId());
    labelDAO.insert(appLabel);

    ComponentLabelDAO dao = new ComponentLabelDAO();

    // sanity check
    List<ComponentLabel> componentLabels = dao.getByOwnerIdAndHash(applicationId, hash);
    Assert.assertNotNull(componentLabels);
    Assert.assertEquals(0, componentLabels.size());

    dao.insert(new ComponentLabel(application.getOrganizationId(), orgLabel.getId(), hash));
    dao.insert(new ComponentLabel(applicationId, appLabel.getId(), hash));

    componentLabels = dao.getByOwnerIdAndHash(applicationId, hash);
    Assert.assertNotNull(componentLabels);
    Assert.assertEquals(2, componentLabels.size());

    Assert.assertEquals(orgLabel.getId(), componentLabels.get(0).getLabelId());
    Assert.assertEquals(orgLabel.getOwnerId(), componentLabels.get(0).getOwnerId());

    Assert.assertEquals(appLabel.getId(), componentLabels.get(1).getLabelId());
    Assert.assertEquals(appLabel.getOwnerId(), componentLabels.get(1).getOwnerId());
  }
}
