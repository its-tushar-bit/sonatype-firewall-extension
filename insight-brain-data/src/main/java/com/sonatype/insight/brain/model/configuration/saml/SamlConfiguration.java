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
  public static final int IDENTITY_PROVIDER_NAME_MAXIMUM_LENGTH = 200;

  private String id;

  private String identityProviderName = "identity provider";

  private String identityProviderMetadataXml;

  private String entityId;

  private String firstNameAttributeName = "firstName";

  private String lastNameAttributeName = "lastName";

  private String emailAttributeName = "email";

  private String usernameAttributeName = "username";

  private String groupsAttributeName = "groups";

  private Boolean validateResponseSignature;

  private Boolean validateAssertionSignature;

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

  public String getIdentityProviderName() {
    return identityProviderName;
  }

  public void setIdentityProviderName(String identityProviderName) {
    this.identityProviderName = identityProviderName;
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

  public Boolean getValidateResponseSignature() {
    return validateResponseSignature;
  }

  public void setValidateResponseSignature(Boolean validateResponseSignature) {
    this.validateResponseSignature = validateResponseSignature;
  }

  public Boolean getValidateAssertionSignature() {
    return validateAssertionSignature;
  }

  public void setValidateAssertionSignature(Boolean validateAssertionSignature) {
    this.validateAssertionSignature = validateAssertionSignature;
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
