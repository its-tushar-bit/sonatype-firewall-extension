/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class LabelResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetLabels() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetApplicableLabels() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(LabelResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(LabelResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());
    Label label = tempEntity.newLabel(app.getId());

    String url = getRestUrl(LabelResource.SERVICE_PATH + "/applicable/context/{labelId}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), label.getId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());
    label = tempEntity.newLabel(org.getId());

    url = getRestUrl(LabelResource.SERVICE_PATH + "/applicable/context/{labelId}", IdUtils.TYPE_ORGANIZATION,
        org.getId(), label.getId());
    response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testAddLabel() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    Label label = new Label(null, "testing", null);
    String url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(label));
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), toJson(label));
    assertResponseStatus(200, response);
    label = fromJson(response, Label.class);
    new LabelDAO().delete(label);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    label = new Label(null, "testing", null);
    url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(label));
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), toJson(label));
    assertResponseStatus(200, response);
    label = fromJson(response, Label.class);
    new LabelDAO().delete(label);
  }

  @Test
  public void testUpdateLabel() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    Label label = tempEntity.newLabel(app.getId());
    String url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(label));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(label));
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    label = tempEntity.newLabel(org.getId());
    url = getRestUrl(LabelResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(label));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(label));
    assertResponseStatus(200, response);
  }

  @Test
  public void testDeleteLabel() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    Label label = tempEntity.newLabel(app.getId());
    String url = getRestUrl(LabelResource.SERVICE_PATH + "/{labelId}", IdUtils.TYPE_APPLICATION, app.getPublicId(),
        label.getId());
    Response response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    label = tempEntity.newLabel(org.getId());
    url = getRestUrl(LabelResource.SERVICE_PATH + "/{labelId}", IdUtils.TYPE_ORGANIZATION, org.getId(), label.getId());
    response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);
  }
}
