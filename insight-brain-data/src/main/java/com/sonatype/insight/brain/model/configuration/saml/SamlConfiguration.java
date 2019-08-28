/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.saml;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @since 1.72
 */
public class SamlConfiguration
{
  @JsonIgnore
  private String id;

  private String identityProviderMetadataXml;

  private String entityId;

  private String firstNameAttributeName = "firstName";

  private String lastNameAttributeName = "lastName";

  private String emailAttributeName = "email";

  private String usernameAttributeName = "username";

  private String groupsAttributeName = "groups";

  @JsonIgnore
  private Certificate certificate;

  @JsonIgnore
  private PrivateKey decryptionKey;

  @JsonIgnore
  private KeyPair signingKeyPair;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFirstNameAttributeName() {
    return firstNameAttributeName;
  }

  public void setFirstNameAttributeName(String firstNameAttributeName) {
    this.firstNameAttributeName = firstNameAttributeName;
  }

  public String getLastNameAttributeName() {
    return lastNameAttributeName;
  }

  public void setLastNameAttributeName(String lastNameAttributeName) {
    this.lastNameAttributeName = lastNameAttributeName;
  }

  public String getEmailAttributeName() {
    return emailAttributeName;
  }

  public void setEmailAttributeName(String emailAttributeName) {
    this.emailAttributeName = emailAttributeName;
  }

  public String getUsernameAttributeName() {
    return usernameAttributeName;
  }

  public void setUsernameAttributeName(String usernameAttributeName) {
    this.usernameAttributeName = usernameAttributeName;
  }

  public String getGroupsAttributeName() {
    return groupsAttributeName;
  }

  public void setGroupsAttributeName(String groupsAttributeName) {
    this.groupsAttributeName = groupsAttributeName;
  }

  public String getIdentityProviderMetadataXml() {
    return identityProviderMetadataXml;
  }

  public void setIdentityProviderMetadataXml(String identityProviderMetadataXml) {
    this.identityProviderMetadataXml = identityProviderMetadataXml;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public Certificate getCertificate() {
    return certificate;
  }

  public void setCertificate(Certificate certificate) {
    this.certificate = certificate;
  }

  public PrivateKey getDecryptionKey() {
    return decryptionKey;
  }

  public void setDecryptionKey(PrivateKey decryptionKey) {
    this.decryptionKey = decryptionKey;
  }

  public KeyPair getSigningKeyPair() {
    return signingKeyPair;
  }

  public void setSigningKeyPair(KeyPair signingKeyPair) {
    this.signingKeyPair = signingKeyPair;
  }
}
