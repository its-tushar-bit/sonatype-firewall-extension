/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.StringReader;

import javax.inject.Named;
import javax.xml.transform.stream.StreamSource;

import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.util.JAXPValidationUtil;

@Named
public class SamlMetadataTool
{
  public EntityDescriptorType parseEntityDescriptor(String xmlMetadata) {
    Object metadata;
    try {
      JAXPValidationUtil.validator().validate(new StreamSource(new StringReader(xmlMetadata)));
      metadata = SAMLParser.getInstance().parse(new StreamSource(new StringReader(xmlMetadata)));
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Invalid SAML metadata: " + e.getMessage(), e);
    }
    if (metadata instanceof EntityDescriptorType) {
      return (EntityDescriptorType) metadata;
    }
    if (metadata instanceof EntitiesDescriptorType) {
      EntitiesDescriptorType entities = (EntitiesDescriptorType) metadata;
      if (entities.getEntityDescriptor().size() != 1) {
        throw new IllegalArgumentException(
            "Invalid SAML entity descriptor count: " + entities.getEntityDescriptor().size());
      }
      return (EntityDescriptorType) entities.getEntityDescriptor().get(0);
    }
    throw new IllegalArgumentException("Invalid SAML metadata type: " + metadata.getClass());
  }
}
