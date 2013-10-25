/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class ComponentLabelResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private ComponentLabelDAO compLabelDAO = new ComponentLabelDAO();

  @Test
  public void testGetComponentLabels() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(ComponentLabelResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId(), "bad");
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(ComponentLabelResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId(), "bad");
    response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testSetComponentLabel() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());
    Label label = tempEntity.newLabel(org.getId());
    String hash = "bad";

    String url = getRestUrl(ComponentLabelResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId(), hash);
    Response response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(label));
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), toJson(label));
    assertResponseStatus(204, response);
    compLabelDAO.delete(compLabelDAO.getByOwnerIdAndHashAndLabelId(app.getId(), hash, label.getId()));

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(ComponentLabelResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId(), hash);
    response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(label));
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), toJson(label));
    assertResponseStatus(204, response);
    compLabelDAO.delete(compLabelDAO.getByOwnerIdAndHashAndLabelId(org.getId(), hash, label.getId()));
  }

  @Test
  public void testRemoveComponentLabel() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());
    Label label = tempEntity.newLabel(org.getId());
    String hash = "bad";

    ComponentLabel compLabel = new ComponentLabel(app.getId(), label.getId(), hash);
    compLabelDAO.insert(compLabel);
    String url = getRestUrl(ComponentLabelResource.SERVICE_PATH + "/{labelId}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), hash, label.getId());
    Response response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    compLabel = new ComponentLabel(org.getId(), label.getId(), hash);
    compLabelDAO.insert(compLabel);
    url = getRestUrl(ComponentLabelResource.SERVICE_PATH + "/{labelId}", IdUtils.TYPE_ORGANIZATION, org.getId(), hash,
        label.getId());
    response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);
  }
}
