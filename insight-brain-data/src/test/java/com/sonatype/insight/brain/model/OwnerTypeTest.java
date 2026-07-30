/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OwnerTypeTest
{
  @Test
  public void testFromString_hostedRepositoryComponent() {
    assertThat(OwnerType.fromString("hosted_repository_component"))
        .isEqualTo(OwnerType.HOSTED_REPOSITORY_COMPONENT);
    assertThat(OwnerType.fromString("HOSTED_REPOSITORY_COMPONENT"))
        .isEqualTo(OwnerType.HOSTED_REPOSITORY_COMPONENT);
  }

  @Test
  public void testToString_hostedRepositoryComponent() {
    assertThat(OwnerType.HOSTED_REPOSITORY_COMPONENT.toString())
        .isEqualTo("hosted_repository_component");
  }

  @Test
  public void testGetParentType_hostedRepositoryComponentReturnsRepository() {
    assertThat(OwnerType.HOSTED_REPOSITORY_COMPONENT.getParentType())
        .isEqualTo(OwnerType.REPOSITORY);
  }
}
