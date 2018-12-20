/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionTest
{
  @Test
  public void testPermissionsNotAllowedInCustomRoles() {
    for (Permission permission : Permission.values()) {
      switch (permission) {
        case CONFIGURE_SYSTEM:
        case EDIT_ROLES:
          assertThat(permission.isAllowedInCustomRoles()).isFalse();
          break;
        default:
          assertThat(permission.isAllowedInCustomRoles()).isTrue();
          break;
      }
    }
  }
}
