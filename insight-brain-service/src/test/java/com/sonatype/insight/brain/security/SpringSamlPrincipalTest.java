/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringSamlPrincipalTest
{
  private SamlPrincipalAttributes principal(Map<String, List<Object>> attributes) {
    return new SpringSamlPrincipal(new DefaultSaml2AuthenticatedPrincipal("jsmith", attributes));
  }

  @Test
  public void testGetName_ReturnsNameId() {
    assertThat(principal(Map.of()).getName()).isEqualTo("jsmith");
  }

  @Test
  public void testGetAttribute_ReturnsFirstValue() {
    SamlPrincipalAttributes principal = principal(Map.of("groups", List.of("admins", "devs")));
    assertThat(principal.getAttribute("groups")).isEqualTo("admins");
  }

  @Test
  public void testGetAttribute_ReturnsNullWhenAbsent() {
    assertThat(principal(Map.of()).getAttribute("missing")).isNull();
  }

  @Test
  public void testGetAttributes_ReturnsAllValuesAsStrings() {
    SamlPrincipalAttributes principal = principal(Map.of("groups", List.of("admins", "devs")));
    assertThat(principal.getAttributes("groups")).containsExactly("admins", "devs");
  }

  @Test
  public void testGetAttributes_ReturnsEmptyWhenAbsent() {
    assertThat(principal(Map.of()).getAttributes("missing")).isEmpty();
  }

  @Test
  public void testFriendlyAccessors_AreEmpty() {
    SamlPrincipalAttributes principal = principal(Map.of("email", List.of("jsmith@example.com")));
    assertThat(principal.getFriendlyAttribute("email")).isNull();
    assertThat(principal.getFriendlyAttributes("email")).isEmpty();
    assertThat(principal.getFriendlyNames()).isEmpty();
  }

  @Test
  public void testGetAllAttributes_ConvertsValuesToStrings() {
    SamlPrincipalAttributes principal = principal(Map.of("age", List.of(42)));
    assertThat(principal.getAllAttributes()).containsEntry("age", List.of("42"));
  }
}
