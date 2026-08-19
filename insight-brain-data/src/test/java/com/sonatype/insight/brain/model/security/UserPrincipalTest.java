/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserPrincipalTest
{
  @Test
  public void testEveryoneIsAMemberInAuthenticatedUsersGroup() {
    UserPrincipal userPrincipal = new UserPrincipal("username", "displayName", User.INTERNAL_REALM_ID);
    assertThat(userPrincipal.getMembership()).containsExactly(Group.AUTHENTICATED_USERS_GROUP_ID);

    userPrincipal =
        new UserPrincipal("username", "displayName", User.INTERNAL_REALM_ID, Collections.singleton("SomeGroup"));
    assertThat(userPrincipal.getMembership()).containsExactly("SomeGroup", Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  @Test
  public void testConvertToAndFromJson() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    UserPrincipal expected =
        new UserPrincipal("username", "displayName", User.INTERNAL_REALM_ID, Sets.newHashSet("group1", "group2"));

    UserPrincipal actual = objectMapper.readValue(objectMapper.writeValueAsString(expected), UserPrincipal.class);

    assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
  }
}
