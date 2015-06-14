/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Test;

public class LabelResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LabelResource.SERVICE_PATH);
  }

  @Test
  public void testGetLabels() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().parameter(IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzGet(request);

    grantReadPermission(org.getId());

    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzGet(request);
  }

  @Test
  public void testGetApplicableLabels() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path("applicable").parameter(IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzGet(request);

    grantReadPermission(org.getId());

    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzGet(request);
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    grantWritePermission(app.getId());
    Label label = tempEntity.newLabel(app.getId());

    HttpRequest request = restRequest().path("applicable/context/{labelId}").parameter(IdUtils.TYPE_APPLICATION,
        app.getPublicId(), label.getId());
    testAuthzGet(request);
  }

  @Test
  public void testAddLabel() throws Exception {
    grantWritePermission(app.getId());

    HttpRequest request = restRequest().parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()).body(
        new Label(null, "testing"));
    HttpResponse response = testAuthzPost(request);
    new LabelDAO().delete(response.getBody(Label.class));

    grantWritePermission(org.getId());

    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId());
    response = testAuthzPost(request);
    new LabelDAO().delete(response.getBody(Label.class));
  }

  @Test
  public void testUpdateLabel() throws Exception {
    grantWritePermission(app.getId());

    HttpRequest request = restRequest().parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()).body(
        tempEntity.newLabel(app.getId()));
    testAuthzPut(request);

    grantWritePermission(org.getId());

    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId()).body(tempEntity.newLabel(org.getId()));
    testAuthzPut(request);
  }

  @Test
  public void testDeleteLabel() throws Exception {
    grantWritePermission(app.getId());

    HttpRequest request = restRequest().path("{labelId}").parameter(IdUtils.TYPE_APPLICATION, app.getPublicId(),
        tempEntity.newLabel(app.getId()).getId());
    testAuthzDelete(request);

    grantWritePermission(org.getId());

    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId(), tempEntity.newLabel(org.getId()).getId());
    testAuthzDelete(request);
  }
}
