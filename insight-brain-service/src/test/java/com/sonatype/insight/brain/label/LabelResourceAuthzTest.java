/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import org.junit.Test;

public class LabelResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetLabels() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzGet(url);
  }

  @Test
  public void testGetApplicableLabels() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(LabelResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getRestUrl(LabelResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzGet(url);
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    grantReadPermission(app.getId());
    Label label = tempEntity.newLabel(app.getId());

    String url = getRestUrl(LabelResource.SERVICE_PATH + "/applicable/context/{labelId}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), label.getId());
    testAuthzGet(url);

    grantReadPermission(org.getId());
    label = tempEntity.newLabel(org.getId());

    url = getRestUrl(LabelResource.SERVICE_PATH + "/applicable/context/{labelId}", IdUtils.TYPE_ORGANIZATION,
        org.getId(), label.getId());
    testAuthzGet(url);
  }

  @Test
  public void testAddLabel() throws Exception {
    grantWritePermission(app.getId());

    Label label = new Label(null, "testing");
    String url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = testAuthzPost(url, toJson(label));
    label = fromJson(response, Label.class);
    new LabelDAO().delete(label);

    grantWritePermission(org.getId());

    label = new Label(null, "testing");
    url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = testAuthzPost(url, toJson(label));
    label = fromJson(response, Label.class);
    new LabelDAO().delete(label);
  }

  @Test
  public void testUpdateLabel() throws Exception {
    grantWritePermission(app.getId());

    Label label = tempEntity.newLabel(app.getId());
    String url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzPut(url, toJson(label));

    grantWritePermission(org.getId());

    label = tempEntity.newLabel(org.getId());
    url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzPut(url, toJson(label));
  }

  @Test
  public void testDeleteLabel() throws Exception {
    grantWritePermission(app.getId());

    Label label = tempEntity.newLabel(app.getId());
    String url = getRestUrl(LabelResource.SERVICE_PATH + "/{labelId}", IdUtils.TYPE_APPLICATION, app.getPublicId(),
        label.getId());
    testAuthzDelete(url);

    grantWritePermission(org.getId());

    label = tempEntity.newLabel(org.getId());
    url = getRestUrl(LabelResource.SERVICE_PATH + "/{labelId}", IdUtils.TYPE_ORGANIZATION, org.getId(), label.getId());
    testAuthzDelete(url);
  }
}
