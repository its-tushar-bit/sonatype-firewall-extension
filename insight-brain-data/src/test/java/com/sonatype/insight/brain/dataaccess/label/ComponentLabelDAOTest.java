/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.label;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ComponentLabelDAOTest
    extends AbstractDbDAOTest
{
  private final String hash = "ababababab";

  private ComponentLabelDAO dao;

  private LabelDAO labelDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createComponentLabelDAO();
    labelDAO = daoFactory.createLabelDAO();
  }

  private Label newLabel(String name, String ownerId) {
    Label label = new Label();
    label.setLabel(name);
    label.setOwnerId(ownerId);
    labelDAO.insert(label);
    return label;
  }

  @Test
  public void testCRUD() {
    Label label = newLabel("label", application.getOrganizationId());

    ComponentLabel compLabel = new ComponentLabel(application.getId(), label.getId(), hash);
    dao.insert(compLabel);

    compLabel = dao.getById(compLabel.getId());
    assertThat(compLabel).isNotNull();

    dao.update(compLabel);
    compLabel.setOwnerId(application.getOrganizationId());
    dao.update(compLabel);
    compLabel = dao.getById(compLabel.getId());
    assertThat(compLabel.getOwnerId()).isEqualTo(application.getOrganizationId());

    dao.delete(compLabel);
    compLabel = dao.getById(compLabel.getId());
    assertThat(compLabel).isNull();
  }

  @Test
  public void testGetByOwnerIdAndHashAndLabelId() {
    Label label = newLabel("label", application.getId());

    ComponentLabel compLabel = new ComponentLabel(application.getId(), label.getId(), hash);
    dao.insert(compLabel);
    ComponentLabel entity = dao.getByOwnerIdAndHashAndLabelId(application.getId(), hash, label.getId());
    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(compLabel.getId());
  }

  @Test
  public void testInsertDuplicate() {
    Label label = newLabel("label", application.getId());

    dao.insert(new ComponentLabel(application.getId(), label.getId(), hash));
    assertThatThrownBy(() -> dao.insert(new ComponentLabel(application.getId(), label.getId(), hash)))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The label 'label' is already applied to the component ababababab.");
  }

  @Test
  public void testInsertNonApplicable() {
    Label label = newLabel("label", application.getId());

    ComponentLabel compLabel = new ComponentLabel(application.getOrganizationId(), label.getId(), hash);
    assertThatThrownBy(() -> dao.insert(compLabel)).isInstanceOf(BadRequestException.class)
        .hasMessage(
            "The label 'label' is not applicable for the selected context " + application.getOrganizationId() + ".");
  }

  @Test
  public void testUpdateDuplicate() {
    Label label = newLabel("label", application.getId());

    dao.insert(new ComponentLabel(application.getId(), label.getId(), hash));
    ComponentLabel compLabel = new ComponentLabel(application.getId(), label.getId(), hash + "0");
    dao.insert(compLabel);
    compLabel.setHash(hash);
    assertThatThrownBy(() -> dao.update(compLabel)).isInstanceOf(BadRequestException.class)
        .hasMessage("The label 'label' is already applied to the component ababababab.");
  }

  @Test
  public void testUpdateNonApplicable() {
    Label label = newLabel("label", application.getId());

    ComponentLabel compLabel = new ComponentLabel(application.getId(), label.getId(), hash);
    dao.insert(compLabel);
    compLabel.setOwnerId(application.getOrganizationId());
    assertThatThrownBy(() -> dao.update(compLabel)).isInstanceOf(BadRequestException.class)
        .hasMessage(
            "The label 'label' is not applicable for the selected context " + application.getOrganizationId() + ".");
  }

  @Test
  public void testOrganizationInheritance() {
    final Label orgLabel = new Label();
    orgLabel.setColor(Color.light_green);
    orgLabel.setLabel("org-label");
    orgLabel.setOwnerId(organization.getId());
    labelDAO.insert(orgLabel);
    final Label appLabel = new Label();
    appLabel.setColor(Color.light_green);
    appLabel.setLabel("app-label");
    appLabel.setOwnerId(application.getId());
    labelDAO.insert(appLabel);

    // sanity check
    List<ComponentLabel> componentLabels = dao.getByOwnerIdAndHashWithHierarchy(application.getId(), hash);
    assertThat(componentLabels).isEmpty();

    dao.insert(new ComponentLabel(application.getOrganizationId(), orgLabel.getId(), hash));
    dao.insert(new ComponentLabel(application.getId(), appLabel.getId(), hash));

    componentLabels = dao.getByOwnerIdAndHashWithHierarchy(application.getId(), hash);
    assertThat(componentLabels).hasSize(2);

    assertComponentLabel(appLabel, componentLabels.get(0));
    assertComponentLabel(orgLabel, componentLabels.get(1));
  }

  private void assertComponentLabel(Label expected, ComponentLabel actual) {
    assertThat(actual.getLabelId()).isEqualTo(expected.getId());
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
  }

  @Test
  public void testGetByLabelIdAndOwnerIds() {
    Label label1 = tempEntity.newLabel(organization.getId());
    Label label2 = tempEntity.newLabel(application.getId());

    ComponentLabel compLabel1 = tempEntity.newComponentLabel(application.getId(), label1.getId(), hash);
    tempEntity.newComponentLabel(organization.getId(), label1.getId(), hash);
    tempEntity.newComponentLabel(application.getId(), label2.getId(), hash);
    try (TransactionContext tx = dao.createTransactionContext()) {
      List<ComponentLabel> componentLabels = dao.getByLabelIdAndOwnerIds(tx, label1.getId(),
          Collections.singleton(application.getId()));
      assertThat(componentLabels).extracting(ComponentLabel::getId).containsExactly(compLabel1.getId());
    }
  }
}
