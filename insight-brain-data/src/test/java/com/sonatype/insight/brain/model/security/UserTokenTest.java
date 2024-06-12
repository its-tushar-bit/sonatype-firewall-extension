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
  public void testIsSsoUser_Saml() {
    UserToken samlUserToken = createUserToken(SamlUser.SAML_REALM_ID);
    UserToken otherUserToken = createUserToken("other");

    assertThat(samlUserToken.isSsoUser()).isTrue();
    assertThat(otherUserToken.isSsoUser()).isFalse();
  }

  @Test
  public void testIsSsoUser_OAuth2() {
    UserToken samlUserToken = createUserToken(OAuth2User.OAUTH2_REALM_ID);
    UserToken otherUserToken = createUserToken("other");

    assertThat(samlUserToken.isSsoUser()).isTrue();
    assertThat(otherUserToken.isSsoUser()).isFalse();
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
