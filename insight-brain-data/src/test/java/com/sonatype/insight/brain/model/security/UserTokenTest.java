/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTokenTest
{
  @Test
  public void testIsSamlUser() {
    UserToken samlUserToken = createUserToken(SamlUser.SAML_REALM_ID);
    UserToken otherUserToken = createUserToken("other");

    assertThat(samlUserToken.isSamlUser()).isTrue();
    assertThat(otherUserToken.isSamlUser()).isFalse();
  }

  @Test
  public void testIsInternalUser() {
    UserToken internalUserToken = createUserToken(User.INTERNAL_REALM_ID);
    UserToken otherUserToken = createUserToken("other");

    assertThat(internalUserToken.isInternalUser()).isTrue();
    assertThat(otherUserToken.isInternalUser()).isFalse();
  }

  private UserToken createUserToken(String realmId) {
    UserToken userToken = new UserToken();
    userToken.setRealmId(realmId);
    return userToken;
  }
}
