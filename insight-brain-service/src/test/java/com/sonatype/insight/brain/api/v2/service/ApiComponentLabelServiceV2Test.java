/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ApiComponentLabelServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiComponentLabelServiceV2 apiComponentLabelService;

  private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  @Test
  public void testSetApplicationComponentLabel() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");

    apiComponentLabelService.setApplicationComponentLabel(app.getId(), componentHash, label.getLabel());

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel, is(notNullValue()));
  }

  @Test
  public void testSetApplicationComponentLabel_LabelIgnoresCaseSensitivity() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");
    String upperCaseLabelName = label.getLabel().toUpperCase();

    apiComponentLabelService.setApplicationComponentLabel(app.getId(), componentHash, upperCaseLabelName);

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel, is(notNullValue()));
  }

  @Test
  public void testSetApplicationComponentLabel_HashTruncation() {
    String shortHash = "babababababababababa";
    String componentHash = shortHash + "cdcdcdcdcdcdcdcd";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");

    apiComponentLabelService.setApplicationComponentLabel(app.getId(), componentHash, label.getLabel());

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), shortHash,
        label.getId());
    assertThat(componentLabel, is(notNullValue()));
  }

  @Test
  public void testSetApplicationComponentLabel_UnknownLabel() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    String label = "FAKELABEL";

    try {
      apiComponentLabelService.setApplicationComponentLabel(app.getId(), componentHash, label);
      fail("Expected NotFoundException to be thrown.");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Could not find a label with name 'FAKELABEL' for application with ID " + app.getId() + "."));
    }
  }

  @Test
  public void testDeleteApplicationComponentLabel() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");
    componentLabelDAO.insert(new ComponentLabel(app.getId(), label.getId(), componentHash));

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel, is(notNullValue()));

    apiComponentLabelService.deleteApplicationComponentLabel(app.getId(), componentHash, label.getLabel());

    componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel, is(nullValue()));
  }

  @Test
  public void testDeleteApplicationComponentLabel_LabelIgnoresCaseSensitivity() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");
    String upperCaseLabelName = label.getLabel().toUpperCase();
    componentLabelDAO.insert(new ComponentLabel(app.getId(), label.getId(), componentHash));

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel, is(notNullValue()));

    apiComponentLabelService.deleteApplicationComponentLabel(app.getId(), componentHash, upperCaseLabelName);

    componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel, is(nullValue()));
  }

  @Test
  public void testDeleteApplicationComponentLabel_HashTruncation() {
    String shortHash = "babababababababababa";
    String componentHash = shortHash + "cdcdcdcdcdcdcdcd";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");
    componentLabelDAO.insert(new ComponentLabel(app.getId(), label.getId(), shortHash));

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), shortHash,
        label.getId());
    assertThat(componentLabel, is(notNullValue()));

    apiComponentLabelService.deleteApplicationComponentLabel(app.getId(), componentHash, label.getLabel());

    componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), shortHash,
        label.getId());
    assertThat(componentLabel, is(nullValue()));
  }

  @Test
  public void testDeleteApplicationComponentLabel_UnknownLabel() {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    String label = "FAKELABEL";

    try {
      apiComponentLabelService.deleteApplicationComponentLabel(app.getId(), componentHash, label);
      fail("Expected NotFoundException to be thrown.");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Could not find a label with name 'FAKELABEL' for application with ID " + app.getId() + "."));
    }
  }
}
