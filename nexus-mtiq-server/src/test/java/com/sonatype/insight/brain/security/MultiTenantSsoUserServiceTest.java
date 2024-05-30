/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.brain.users.MtiqUserDTO;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantSsoUserServiceTest
    extends AbstractMultiTenantTest
{
  @Mock
  SamlSsoUserProvider samlUserGroupHelper;

  private MultiTenantSsoUserService underTest;

  @Before
  public void setup() {
    underTest = new MultiTenantSsoUserService(samlUserGroupHelper);
  }

  @Test
  public void testUpsertByUsername_Saml() {
    MtiqUserDTO mtiqUserDTO = new MtiqUserDTO();
    mtiqUserDTO.setUsername("username");

    testAsNewTenant(t1 -> {
      underTest.upsertByUsername(mtiqUserDTO);

      verify(samlUserGroupHelper).upsertByUsername(any(SsoUser.class));
    });
  }
}
