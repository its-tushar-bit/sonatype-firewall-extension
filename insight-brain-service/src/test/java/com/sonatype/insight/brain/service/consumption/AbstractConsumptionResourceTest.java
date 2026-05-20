/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.After;
import org.junit.Before;

public abstract class AbstractConsumptionResourceTest
    extends AbstractResourceTest
{
  @Before
  public void enableConsumptionReporting() {
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(true);
  }

  @After
  public void disableConsumptionReporting() {
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.setEnabled(false);
  }

  protected User createSystemAdminUser() {
    User user = tempEntity.newUser();
    tempEntity.newMembershipMapping(
        Organization.ROOT_ORGANIZATION_ID,
        Role.SYSTEM_ADMIN_ROLE_ID,
        user.getUsername());
    return user;
  }

  protected User createUsageViewerUser() {
    User user = tempEntity.newUser();
    tempEntity.newMembershipMapping(
        Organization.ROOT_ORGANIZATION_ID,
        Role.USAGE_VIEWER_ROLE_ID,
        user.getUsername());
    return user;
  }
}
