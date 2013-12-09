/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Test;

public class ComponentLabelResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private ComponentLabelDAO compLabelDAO = new ComponentLabelDAO();

  @Test
  public void testGetComponentLabels() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(ComponentLabelResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad");
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getRestUrl(ComponentLabelResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId(), "bad");
    testAuthzGet(url);
  }

  @Test
  public void testSetComponentLabel() throws Exception {
    grantWritePermission(app.getId());
    Label label = tempEntity.newLabel(org.getId());
    String hash = "bad";

    String url = getRestUrl(ComponentLabelResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId(), hash);
    testAuthzPost(url, toJson(label), 204);
    compLabelDAO.delete(compLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), hash, label.getId()));

    grantWritePermission(org.getId());

    url = getRestUrl(ComponentLabelResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId(), hash);
    testAuthzPost(url, toJson(label), 204);
    compLabelDAO.delete(compLabelDAO.getByOwnerIdAndHashAndLabelId(org.getId(), hash, label.getId()));
  }

  @Test
  public void testRemoveComponentLabel() throws Exception {
    grantWritePermission(app.getId());
    Label label = tempEntity.newLabel(org.getId());
    String hash = "bad";

    ComponentLabel compLabel = new ComponentLabel(app.getId(), label.getId(), hash);
    compLabelDAO.insert(compLabel);
    String url = getRestUrl(ComponentLabelResource.SERVICE_PATH + "/{labelId}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), hash, label.getId());
    testAuthzDelete(url);

    grantWritePermission(org.getId());

    compLabel = new ComponentLabel(org.getId(), label.getId(), hash);
    compLabelDAO.insert(compLabel);
    url = getRestUrl(ComponentLabelResource.SERVICE_PATH + "/{labelId}", IdUtils.TYPE_ORGANIZATION, org.getId(), hash,
        label.getId());
    testAuthzDelete(url);
  }
}
