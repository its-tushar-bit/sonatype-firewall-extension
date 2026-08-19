/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aideveloper;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class AiDeveloperOptInResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected void afterDatabaseReset() {
    SystemConfigurationPropertyDAO.invalidateEntireCache();
  }

  /**
   * The opt-in unlocks AI Developer for everyone on the server, so authentication is the only requirement — the
   * {@code authorized} user here holds no permissions.
   */
  @Test
  public void optInRequiresAuthenticationOnly() throws Exception {
    testAuthcGet(restRequest().path(AiDeveloperOptInResource.RESOURCE_PATH));
    testAuthcPost(restRequest().path(AiDeveloperOptInResource.RESOURCE_PATH));
  }
}
