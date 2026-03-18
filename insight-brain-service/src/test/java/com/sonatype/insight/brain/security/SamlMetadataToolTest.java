/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.io.Resources;
import org.junit.Test;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

public class SamlMetadataToolTest
    extends AbstractComponentTest
{
  @Inject
  private SamlMetadataTool samlMetadataTool;

  private EntityDescriptorType parse(String resourceName) {
    try {
      String xmlMetadata =
          Resources.toString(getClass().getResource("/SamlMetadataToolTest/" + resourceName), StandardCharsets.UTF_8);
      return samlMetadataTool.parseEntityDescriptor(xmlMetadata);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  public void testParseEntityDescriptor_WithEntitiesDescriptor() {
    assertThat(parse("with-entities-descriptor.xml").getEntityID()).isEqualTo("test-entity-id");
  }

  @Test
  public void testParseEntityDescriptor_WithoutEntitiesDescriptor() {
    assertThat(parse("without-entities-descriptor.xml").getEntityID()).isEqualTo("test-entity-id");
  }

  @Test
  public void testParseEntityDescriptor_UnknownElement() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> parse("unknown-element.xml"))
        .withMessageStartingWith("Invalid SAML metadata")
        .withMessageContaining("UnknownElement");
  }

  @Test
  public void testParseEntityDescriptor_NoEntityDescriptors() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> parse("no-entity-descriptors.xml"))
        .withMessageStartingWith("Invalid SAML metadata"); // schema validation errors are localized...
  }

  @Test
  public void testParseEntityDescriptor_MultipleEntityDescriptors() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> parse("multiple-entity-descriptors.xml"))
        .withMessage("Invalid SAML entity descriptor count: 2");
  }

  @Test
  public void testParseEntityDescriptor_MultipleIdentityProviderDescriptors() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> parse("multiple-idp-descriptors.xml"))
        .withMessage("Invalid SAML identity provider count: 2");
  }

  @Test
  public void testParseEntityDescriptor_NoSuitableSingleSignOnBinding() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> parse("no-suitable-sso-binding.xml"))
        .withMessageContaining("supports neither POST nor Redirect binding for SSO");
  }

  @Test
  public void testParseEntityDescriptor_Keycloak() {
    EntityDescriptorType entityDescriptor = parse("identity-provider-keycloak.xml");
    assertThat(entityDescriptor.getEntityID()).isEqualTo("http://localhost:8080/auth/realms/master");
    assertThat(entityDescriptor.getChoiceType()).hasSize(1);
    assertThat(entityDescriptor.getChoiceType().get(0).getDescriptors()).hasSize(1);
    IDPSSODescriptorType idpDescriptor =
        entityDescriptor.getChoiceType().get(0).getDescriptors().get(0).getIdpDescriptor();
    assertThat(idpDescriptor.isWantAuthnRequestsSigned()).isTrue();
    assertThat(idpDescriptor.getSingleSignOnService())
        .extracting(endpoint -> endpoint.getLocation().toString(), endpoint -> endpoint.getBinding().toString())
        .containsExactly(
            tuple("http://localhost:8080/auth/realms/master/protocol/saml",
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"),
            tuple("http://localhost:8080/auth/realms/master/protocol/saml",
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"),
            tuple("http://localhost:8080/auth/realms/master/protocol/saml",
                "urn:oasis:names:tc:SAML:2.0:bindings:SOAP"));
    assertThat(idpDescriptor.getSingleLogoutService())
        .extracting(endpoint -> endpoint.getLocation().toString(), endpoint -> endpoint.getBinding().toString())
        .containsExactly(
            tuple("http://localhost:8080/auth/realms/master/protocol/saml",
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"),
            tuple("http://localhost:8080/auth/realms/master/protocol/saml",
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"));
    assertThat(idpDescriptor.getKeyDescriptor()).extracting(KeyDescriptorType::getUse)
        .containsExactly(KeyTypes.SIGNING);
    assertThat(idpDescriptor.getKeyDescriptor())
        .extracting(key -> key.getKeyInfo().getElementsByTagNameNS("*", "X509Certificate"))
        .allSatisfy(nodes -> {
          assertThat(nodes.getLength()).isEqualTo(1);
          assertThat(nodes.item(0).getTextContent()).matches("[0-9a-zA-Z+/=]+");
        });
    assertThat(idpDescriptor.getNameIDFormat()).containsExactly("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
        "urn:oasis:names:tc:SAML:2.0:nameid-format:transient", "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified",
        "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress");
  }

  @Test
  public void testParseEntityDescriptor_ExternalDTD() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> parse("external-dtd.xml"))
        .withMessageStartingWith("Invalid SAML metadata")
        .withMessageContaining("http", "accessExternalDTD");
  }
}
