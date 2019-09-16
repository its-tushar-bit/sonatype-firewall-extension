/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.junit.Test;
import org.keycloak.adapters.saml.SamlPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SamlAuthenticationTokenTest
{
  @Test
  public void testSamlAuthenticationToken_RequiresNonNullSamlPrincipal() {
    assertThatThrownBy(() -> {
      new SamlAuthenticationToken(null);
    }).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testGetPrincipal_ReturnsSamlPrincipal() {
    SamlPrincipal samlPrincipal = new SamlPrincipal();

    assertThat(new SamlAuthenticationToken(samlPrincipal).getPrincipal()).isSameAs(samlPrincipal);
  }

  @Test
  public void testGetCredentials_ReturnsNull() {
    assertThat(new SamlAuthenticationToken(new SamlPrincipal()).getCredentials()).isNull();
  }
}
