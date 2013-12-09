/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;

import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Test;

public class MembershipMappingResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetApplicableMembershipMappings() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(MembershipMappingResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getRestUrl(MembershipMappingResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzGet(url);
  }

  @Test
  public void testSetMembershipMappingForRole() throws Exception {
    grantWritePermission(app.getId());
    Role appRole = tempEntity.newRole(false);
    String json = toJson(Collections.emptyList());

    String url = getRestUrl(MembershipMappingResource.SERVICE_PATH + '/' + MembershipMappingResource.ROLE_PATH,
        IdUtils.TYPE_APPLICATION, app.getPublicId(), appRole.getId());
    testAuthzPut(url, json, 204);

    grantWritePermission(org.getId());

    url = getRestUrl(MembershipMappingResource.SERVICE_PATH + '/' + MembershipMappingResource.ROLE_PATH,
        IdUtils.TYPE_ORGANIZATION, org.getId(), appRole.getId());
    testAuthzPut(url, json, 204);
  }
}
