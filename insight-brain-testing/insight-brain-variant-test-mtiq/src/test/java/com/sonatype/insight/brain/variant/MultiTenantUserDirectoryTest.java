/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.users.MultiTenantUserDirectory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@MtiqTest
class MultiTenantUserDirectoryTest
{
  private MtiqTestContext ctx;

  private UserDirectory userDirectory;

  @BeforeEach
  void setup() {
    userDirectory = ctx.lookup(UserDirectory.class);
  }

  @Test
  void testMultiTenantImpl() {
    // ensure correct class is wired up for UserDirectory
    assertThat(userDirectory).isInstanceOf(MultiTenantUserDirectory.class);
  }

  @Test
  void testIsGroupSearchDisabled_thirdPartyIdp() {
    // "group search disabled" means that the system is in a configuration where not all possible user groups
    // can be found via search. This is the case with a third party IdP because we cannot search the third party IdP
    // for groups that we havent' seen yet
    assertThat(userDirectory.isGroupSearchDisabled()).isTrue();
  }

  @Test
  void testIsGroupSearchDisabled_sonatypeIdp() {
    ctx.testAsTestTenant(t -> ctx.tempEntity()
        .newSystemConfigurationProperty(
            SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE, String.valueOf(true)));

    assertThat(userDirectory.isGroupSearchDisabled()).isFalse();
  }
}
