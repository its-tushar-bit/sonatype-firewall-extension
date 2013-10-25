/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class LicenseThreatGroupResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetLicenseThreatGroups() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetApplicableLicenseThreatGroups() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_APPLICATION,
        app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testAddLicenseThreatGroup() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    LicenseThreatGroup ltg = new LicenseThreatGroup(null, "Test LTG", 5);

    String url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(ltg));
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), toJson(ltg));
    assertResponseStatus(200, response);
    ltg = fromJson(response, LicenseThreatGroup.class);
    new LicenseThreatGroupDAO().delete(ltg);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(ltg));
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), toJson(ltg));
    assertResponseStatus(200, response);
    ltg = fromJson(response, LicenseThreatGroup.class);
    new LicenseThreatGroupDAO().delete(ltg);
  }

  @Test
  public void testUpdateLicenseThreatGroup() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());

    String url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(ltg));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(ltg));
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());
    ltg = tempEntity.newLicenseThreatGroup(org.getId());

    url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(ltg));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(ltg));
    assertResponseStatus(200, response);
  }

  @Test
  public void testDeleteLicenseThreatGroup() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());

    String url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH + "/{ltgId}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), ltg.getId());
    Response response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());
    ltg = tempEntity.newLicenseThreatGroup(org.getId());

    url = getRestUrl(LicenseThreatGroupResource.SERVICE_PATH + "/{ltgId}", IdUtils.TYPE_ORGANIZATION, org.getId(),
        ltg.getId());
    response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);
  }
}
