/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SamlAuthenticationTokenTest
{
  private static SamlPrincipalAttributes principal() {
    return new SpringSamlPrincipal(new DefaultSaml2AuthenticatedPrincipal("subject-name", Map.of()));
  }

  @Test
  public void testSamlAuthenticationToken_RequiresNonNullSamlPrincipal() {
    assertThatThrownBy(() -> new SamlAuthenticationToken(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testGetPrincipal_ReturnsSamlPrincipal() {
    SamlPrincipalAttributes samlPrincipal = principal();

    assertThat(new SamlAuthenticationToken(samlPrincipal).getPrincipal()).isSameAs(samlPrincipal);
  }

  @Test
  public void testGetCredentials_ReturnsNull() {
    assertThat(new SamlAuthenticationToken(principal()).getCredentials()).isNull();
  }
}
