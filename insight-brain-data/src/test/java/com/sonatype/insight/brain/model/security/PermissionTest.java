/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PermissionTest
{
  @Test
  public void testPermissionsNotAllowedInCustomRoles() {
    for (Permission permission : Permission.values()) {
      switch (permission) {
        case CONFIGURE_SYSTEM:
        case EDIT_ROLES:
          assertThat(permission.isAllowedInCustomRoles(), is(false));
          break;
        default:
          assertThat(permission.isAllowedInCustomRoles(), is(true));
          break;
      }
    }
  }
}
