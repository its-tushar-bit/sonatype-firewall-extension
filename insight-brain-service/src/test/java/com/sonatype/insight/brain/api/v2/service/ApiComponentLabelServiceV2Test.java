/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiComponentLabelServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiComponentLabelServiceV2 apiComponentLabelService;

  @Inject
  private ComponentLabelDAO componentLabelDAO;

  @Test
  public void testSetComponentLabel_Organization() {
    String componentHash = "bababababa";
    Organization org = tempEntity.newOrganization();
    Label label = tempEntity.newLabel(org.getId(), "label");

    apiComponentLabelService.setComponentLabel(OwnerType.ORGANIZATION, org.getId(), componentHash, label.getLabel());

    ComponentLabel componentLabel =
        componentLabelDAO.getByOwnerIdAndHashAndLabelId(org.getId(), componentHash, label.getId());
    assertThat(componentLabel).isNotNull();
  }

  @Test
  public void testSetComponentLabel_Application() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");

    apiComponentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getId(), componentHash,
        label.getLabel());

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel).isNotNull();
  }

  @Test
  public void testSetComponentLabel_LabelIgnoresCaseSensitivity() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");
    String upperCaseLabelName = label.getLabel().toUpperCase();

    apiComponentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getId(), componentHash,
        upperCaseLabelName);

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel).isNotNull();
  }

  @Test
  public void testSetComponentLabel_HashTruncation() {
    String shortHash = "babababababababababa";
    String componentHash = shortHash + "cdcdcdcdcdcdcdcd";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");

    apiComponentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getId(), componentHash,
        label.getLabel());

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), shortHash,
        label.getId());
    assertThat(componentLabel).isNotNull();
  }

  @Test
  public void testSetComponentLabel_UnknownLabel() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    String label = "FAKELABEL";

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiComponentLabelService.setComponentLabel(OwnerType.APPLICATION, app.getId(), componentHash, label))
        .withMessage("Could not find a label with name 'FAKELABEL' for application with ID " + app.getId() + ".");
  }

  @Test
  public void testDeleteComponentLabel_Organization() {
    String componentHash = "bababababa";
    Organization org = tempEntity.newOrganization();
    Label label = tempEntity.newLabel(org.getId(), "label");
    componentLabelDAO.insert(new ComponentLabel(org.getId(), label.getId(), componentHash));

    apiComponentLabelService.deleteComponentLabel(OwnerType.ORGANIZATION, org.getId(), componentHash, label.getLabel());

    ComponentLabel componentLabel =
        componentLabelDAO.getByOwnerIdAndHashAndLabelId(org.getId(), componentHash, label.getId());
    assertThat(componentLabel).isNull();
  }

  @Test
  public void testDeleteComponentLabel_Application() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");
    componentLabelDAO.insert(new ComponentLabel(app.getId(), label.getId(), componentHash));

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel).isNotNull();

    apiComponentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getId(), componentHash,
        label.getLabel());

    componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel).isNull();
  }

  @Test
  public void testDeleteComponentLabel_LabelIgnoresCaseSensitivity() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");
    String upperCaseLabelName = label.getLabel().toUpperCase();
    componentLabelDAO.insert(new ComponentLabel(app.getId(), label.getId(), componentHash));

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel).isNotNull();

    apiComponentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getId(), componentHash,
        upperCaseLabelName);

    componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel).isNull();
  }

  @Test
  public void testDeleteComponentLabel_HashTruncation() {
    String shortHash = "babababababababababa";
    String componentHash = shortHash + "cdcdcdcdcdcdcdcd";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");
    componentLabelDAO.insert(new ComponentLabel(app.getId(), label.getId(), shortHash));

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), shortHash,
        label.getId());
    assertThat(componentLabel).isNotNull();

    apiComponentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getId(), componentHash,
        label.getLabel());

    componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), shortHash,
        label.getId());
    assertThat(componentLabel).isNull();
  }

  @Test
  public void testDeleteComponentLabel_UnknownLabel() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    String label = "FAKELABEL";

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiComponentLabelService.deleteComponentLabel(OwnerType.APPLICATION, app.getId(), componentHash, label))
        .withMessage("Could not find a label with name 'FAKELABEL' for application with ID " + app.getId() + ".");
  }
}
