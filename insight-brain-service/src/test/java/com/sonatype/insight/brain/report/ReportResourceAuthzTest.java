/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class ReportResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testAugmentData() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());
    String json = "{}";

    String url = getRestUrl(ReportResource.SERVICE_PATH + "/augmentData/{path}", app.getPublicId(), "scanId",
        "test.json");
    Response response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), json);
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), json);
    assertResponseStatus(200, response);
  }
}
