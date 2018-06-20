/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ApiComponentLabelResourceV2Test
    extends AbstractResourceTest
{
  private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  @Test
  public void testSetApplicationComponentLabel() throws Exception {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");

    HttpResponse response = restRequest().path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2)
        .parameter(componentHash, label.getLabel(), app.getId()).post();
    assertResponseStatus(204, response);

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel, is(notNullValue()));
  }

  @Test
  public void testDeleteApplicationComponentLabel() throws Exception {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");

    componentLabelDAO.insert(new ComponentLabel(app.getId(), label.getId(), componentHash));
    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel, is(notNullValue()));

    HttpResponse response = restRequest().path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2)
        .parameter(componentHash, label.getLabel(), app.getId()).delete();
    assertResponseStatus(204, response);

    componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel, is(nullValue()));
  }
}
