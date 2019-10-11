/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Collections;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserPrincipalTest
{
  @Test
  public void testEveryoneIsAMemberInAuthenticatedUsersGroup() {
    UserPrincipal userPrincipal = new UserPrincipal("username", "displayName", true /* isInternaluser */);
    assertThat(userPrincipal.getMembership()).containsExactly(Group.AUTHENTICATED_USERS_GROUP_ID);

    userPrincipal =
        new UserPrincipal("username", "displayName", true /* isInternaluser */, Collections.singleton("SomeGroup"));
    assertThat(userPrincipal.getMembership()).containsExactly("SomeGroup", Group.AUTHENTICATED_USERS_GROUP_ID);
  }
}
