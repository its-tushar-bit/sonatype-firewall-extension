/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Test;

public class ComponentLabelResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private ComponentLabelDAO compLabelDAO = new ComponentLabelDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ComponentLabelResource.SERVICE_PATH);
  }

  @Test
  public void testGetComponentLabels() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().parameter(IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad");
    testAuthzGet(request);

    grantReadPermission(org.getId());

    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId(), "bad");
    testAuthzGet(request);
  }

  @Test
  public void testSetComponentLabel() throws Exception {
    grantWritePermission(app.getId());
    Label label = tempEntity.newLabel(org.getId());
    String hash = "bad";

    HttpRequest request = restRequest().parameter(IdUtils.TYPE_APPLICATION, app.getPublicId(), hash).body(label);
    testAuthzPost(request, 204);
    compLabelDAO.delete(compLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), hash, label.getId()));

    grantWritePermission(org.getId());

    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId(), hash);
    testAuthzPost(request, 204);
    compLabelDAO.delete(compLabelDAO.getByOwnerIdAndHashAndLabelId(org.getId(), hash, label.getId()));
  }

  @Test
  public void testRemoveComponentLabel() throws Exception {
    grantWritePermission(app.getId());
    Label label = tempEntity.newLabel(org.getId());
    String hash = "bad";

    tempEntity.newComponentLabel(app.getId(), label.getId(), hash);
    HttpRequest request = restRequest().path("{labelId}").parameter(IdUtils.TYPE_APPLICATION, app.getPublicId(), hash,
        label.getId());
    testAuthzDelete(request);

    grantWritePermission(org.getId());

    tempEntity.newComponentLabel(org.getId(), label.getId(), hash);
    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId(), hash, label.getId());
    testAuthzDelete(request);
  }
}
