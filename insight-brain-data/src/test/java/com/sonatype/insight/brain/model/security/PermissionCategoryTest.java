/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionCategoryTest
{
  @Test
  public void testPermissionCategories() {
    assertPermissionCategory(PermissionCategory.ADMINISTRATOR, "Administrator");
    assertPermissionCategory(PermissionCategory.REMEDIATION, "Remediation");
    assertPermissionCategory(PermissionCategory.IQ, "IQ");
  }

  private void assertPermissionCategory(final PermissionCategory permissionCategory, final String expectedDisplayName) {
    assertThat(permissionCategory.getDisplayName()).isEqualTo(expectedDisplayName);
  }
}
