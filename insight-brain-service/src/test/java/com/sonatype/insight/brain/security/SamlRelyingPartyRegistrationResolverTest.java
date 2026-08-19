/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.service.BaseUrl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlRelyingPartyRegistrationResolverTest
{
  @Test
  public void testResolve_ReturnsNullWhenSamlNotConfigured() {
    SamlConfigurationCache samlConfigurationCache = mock(SamlConfigurationCache.class);
    when(samlConfigurationCache.get()).thenReturn(null);

    SamlRelyingPartyRegistrationResolver resolver =
        new SamlRelyingPartyRegistrationResolver(samlConfigurationCache, mock(BaseUrl.class));

    assertThat(resolver.resolve(mock(HttpServletRequest.class), null)).isNull();
  }

  @Test
  public void testBuild_RejectsMetadataWithoutVerificationCertificate() {
    // Identity-provider metadata with no signing certificate cannot verify SAML responses/assertions, so a
    // registration must not be built from it. ApiSamlConfigurationService surfaces this as a save-time error.
    SamlConfigurationCache samlConfigurationCache = mock(SamlConfigurationCache.class);
    SamlRelyingPartyRegistrationResolver resolver =
        new SamlRelyingPartyRegistrationResolver(samlConfigurationCache, mock(BaseUrl.class));

    SamlConfiguration samlConfiguration = new SamlConfiguration();
    samlConfiguration.setEntityId("http://localhost:8072/api/v2/config/saml/metadata");
    samlConfiguration.setIdentityProviderMetadataXml(idpMetadataWithoutVerificationCertificate());

    assertThatThrownBy(() -> resolver.build(samlConfiguration, "http://localhost:8072/saml"))
        .hasMessageContaining("verification certificates");
  }

  @Test
  public void testBuild_RejectsMetadataWithExternalEntity() {
    // XXE hardening: IdP metadata carrying a DOCTYPE / external entity must be rejected by the parser, not
    // resolved. OpenSAML's parser pool disallows DOCTYPE declarations. Replaces the deleted SamlMetadataTool
    // external-dtd / external-entity tests.
    SamlConfigurationCache samlConfigurationCache = mock(SamlConfigurationCache.class);
    SamlRelyingPartyRegistrationResolver resolver =
        new SamlRelyingPartyRegistrationResolver(samlConfigurationCache, mock(BaseUrl.class));

    SamlConfiguration samlConfiguration = new SamlConfiguration();
    samlConfiguration.setEntityId("http://localhost:8072/api/v2/config/saml/metadata");
    samlConfiguration.setIdentityProviderMetadataXml(idpMetadataWithExternalEntity());

    assertThatThrownBy(() -> resolver.build(samlConfiguration, "http://localhost:8072/saml"))
        .isInstanceOf(RuntimeException.class);
  }

  private static String idpMetadataWithoutVerificationCertificate() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"http://idp.local/saml\">"
        + "<IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
        + "<SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" "
        + "Location=\"http://idp.local/sso\"/>"
        + "</IDPSSODescriptor></EntityDescriptor>";
  }

  private static String idpMetadataWithExternalEntity() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<!DOCTYPE EntityDescriptor [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
        + "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"http://idp.local/saml\">"
        + "<IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
        + "<SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" "
        + "Location=\"http://idp.local/sso&xxe;\"/>"
        + "</IDPSSODescriptor></EntityDescriptor>";
  }
}
