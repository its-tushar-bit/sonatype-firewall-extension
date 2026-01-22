/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Named;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType.EDTDescriptorChoiceType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.util.JAXPValidationUtil;
import org.xml.sax.SAXException;

import static java.util.stream.Collectors.toList;

@Named
public class SamlMetadataTool
{
  public static final URI POST_BINDING = URI.create("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");

  public static final URI REDIRECT_BINDING = URI.create("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");

  public SamlMetadataTool() throws IOException, SAXException {
    configureValidator();
  }

  private void configureValidator() throws IOException, SAXException {
    Validator validator = JAXPValidationUtil.validator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
  }

  public EntityDescriptorType parseEntityDescriptor(String xmlMetadata) {
    Object metadata;
    try {
      JAXPValidationUtil.validator().validate(new StreamSource(new StringReader(xmlMetadata)));
      metadata = SAMLParser.getInstance().parse(new StreamSource(new StringReader(xmlMetadata)));
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Invalid SAML metadata: " + e.getMessage(), e);
    }
    EntityDescriptorType entityDescriptor;
    if (metadata instanceof EntityDescriptorType) {
      entityDescriptor = (EntityDescriptorType) metadata;
    }
    else if (metadata instanceof EntitiesDescriptorType) {
      EntitiesDescriptorType entities = (EntitiesDescriptorType) metadata;
      if (entities.getEntityDescriptor().size() != 1) {
        throw new IllegalArgumentException(
            "Invalid SAML entity descriptor count: " + entities.getEntityDescriptor().size());
      }
      entityDescriptor = (EntityDescriptorType) entities.getEntityDescriptor().get(0);
    }
    else {
      throw new IllegalArgumentException("Invalid SAML metadata type: " + metadata.getClass());
    }
    List<IDPSSODescriptorType> idpDescriptors =
        entityDescriptor.getChoiceType().stream().flatMap(choiceType -> choiceType.getDescriptors().stream())
            .map(EDTDescriptorChoiceType::getIdpDescriptor).filter(Objects::nonNull).collect(toList());
    if (idpDescriptors.size() != 1) {
      throw new IllegalArgumentException("Invalid SAML identity provider count: " + idpDescriptors.size());
    }
    if (idpDescriptors.get(0).getSingleSignOnService().stream().map(EndpointType::getBinding)
        .noneMatch(binding -> POST_BINDING.equals(binding) || REDIRECT_BINDING.equals(binding))) {
      throw new IllegalArgumentException("SAML identity provider supports neither POST nor Redirect binding for SSO");
    }
    return entityDescriptor;
  }
}
