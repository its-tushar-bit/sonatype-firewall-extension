/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

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
    testAuthzPost(url, json);
  }
}
