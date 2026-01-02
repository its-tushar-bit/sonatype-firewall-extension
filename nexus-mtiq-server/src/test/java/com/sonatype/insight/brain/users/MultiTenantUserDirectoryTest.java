/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class MultiTenantUserDirectoryTest
    extends AbstractMultiTenantBaseIntegrationResourceTest
{
  UserDirectory userDirectory;

  @Before
  public void setup() {
    userDirectory = super.getTestCLMServer().getCLMServer().getInstance(UserDirectory.class);
  }

  @Test
  public void testMultiTenantImpl() {
    // ensure correct class is wired up for UserDirectory
    assertThat(userDirectory).isInstanceOf(MultiTenantUserDirectory.class);
  }

  @Test
  public void testIsGroupSearchDisabled_thirdPartyIdp() {
    // "group search disabled" means that the system is in a configuration where not all possible user groups
    // can be found via search. This is the case with a third party IdP because we cannot search the third party IdP
    // for groups that we havent' seen yet
    assertThat(userDirectory.isGroupSearchDisabled()).isTrue();
  }

  @Test
  public void testIsGroupSearchDisabled_sonatypeIdp() {
    tenantTemporaryEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));

    assertThat(userDirectory.isGroupSearchDisabled()).isFalse();
  }
}
