/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class MembershipMappingResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetApplicableMembershipMappings() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(MembershipMappingResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(MembershipMappingResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testSetMembershipMappingForRole() throws Exception {
    Role role = tempEntity.newRole(false, Permission.ADMIN);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());
    Role appRole = tempEntity.newRole(false);
    String json = toJson(Collections.emptyList());

    String url = getRestUrl(MembershipMappingResource.SERVICE_PATH + '/' + MembershipMappingResource.ROLE_PATH,
        IdUtils.TYPE_APPLICATION, app.getPublicId(), appRole.getId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), json);
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), json);
    assertResponseStatus(204, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(MembershipMappingResource.SERVICE_PATH + '/' + MembershipMappingResource.ROLE_PATH,
        IdUtils.TYPE_ORGANIZATION, org.getId(), appRole.getId());
    response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), json);
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), json);
    assertResponseStatus(204, response);
  }
}
