/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ApiComponentLabelResourceV2Test
    extends AbstractResourceTest
{
  private ComponentLabelDAO componentLabelDAO;

  @Before
  public void setUp() {
    componentLabelDAO = lookup(ComponentLabelDAO.class);
  }

  @Test
  public void testSetComponentLabel_Application() throws Exception {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");

    HttpResponse response = restRequest().path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2)
        .parameter(componentHash, label.getLabel(), OwnerType.APPLICATION, app.getId())
        .post();
    assertResponseStatus(204, response);

    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel).isNotNull();
  }

  @Test
  public void testSetComponentLabel_Organization() throws Exception {
    String componentHash = "bababababa";
    Organization org = tempEntity.newOrganization();
    Label label = tempEntity.newLabel(org.getId(), "label");

    HttpResponse response = restRequest().path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2)
        .parameter(componentHash, label.getLabel(), OwnerType.ORGANIZATION, org.getId())
        .post();
    assertResponseStatus(204, response);

    ComponentLabel componentLabel =
        componentLabelDAO.getByOwnerIdAndHashAndLabelId(org.getId(), componentHash, label.getId());
    assertThat(componentLabel).isNotNull();
  }

  @Test
  public void testDeleteComponentLabel_Application() throws Exception {
    String componentHash = "bababababa";
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getOrganizationId(), "label");

    componentLabelDAO.insert(new ComponentLabel(app.getId(), label.getId(), componentHash));
    ComponentLabel componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel).isNotNull();

    HttpResponse response = restRequest().path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2)
        .parameter(componentHash, label.getLabel(), OwnerType.APPLICATION, app.getId())
        .delete();
    assertResponseStatus(204, response);

    componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), componentHash,
        label.getId());
    assertThat(componentLabel).isNull();
  }

  @Test
  public void testDeleteComponentLabel_Organization() throws Exception {
    String componentHash = "bababababa";
    Organization org = tempEntity.newOrganization();
    Label label = tempEntity.newLabel(org.getId(), "label");

    componentLabelDAO.insert(new ComponentLabel(org.getId(), label.getId(), componentHash));
    ComponentLabel componentLabel =
        componentLabelDAO.getByOwnerIdAndHashAndLabelId(org.getId(), componentHash, label.getId());
    assertThat(componentLabel).isNotNull();

    HttpResponse response = restRequest().path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2)
        .parameter(componentHash, label.getLabel(), OwnerType.ORGANIZATION, org.getId())
        .delete();
    assertResponseStatus(204, response);

    componentLabel = componentLabelDAO.getByOwnerIdAndHashAndLabelId(org.getId(), componentHash, label.getId());
    assertThat(componentLabel).isNull();
  }
}
