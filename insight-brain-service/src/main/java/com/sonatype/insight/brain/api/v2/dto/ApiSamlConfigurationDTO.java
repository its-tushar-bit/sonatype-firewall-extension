/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @since 1.72
 */
@Schema(description = "Enter the SAML configuration" +
    "<ul>" +
    "<li>`identityProviderName` the name of the Identity Provider that is displayed on the login page " +
    "when SAML is configured.</li>" +
    "<li>`entityId` is the URI that IQ Server uses to identify itself in requests to the SSO" +
    "service.</li>" +
    "<li>`firstNameAttribute` is the SAML attribute that IQ Server extracts from the login " +
    "response of the identity provider and uses as the user's first name.</li>" +
    "<li>`lastNameAttribute` is the SAML attribute that IQ Server extracts from the login " +
    "response of the identity provider and uses as the user's last name.</li>" +
    "<li>`emailAttributeName` is the SAML attribute that IQ Server extracts from the login " +
    "response of the identity provider to determine the user's email address.</li>" +
    "<li>`usernameAttributeName` is the SAML attribute that IQ Server extracts from the login " +
    "response of the identity provider to determine the username or id.</li>" +
    "<li>`groupAttributeName` is the SAML attribute that IQ Server extracts from the login " +
    "response of the identity provider to determine the groups the user belongs to.</li>" +
    "<li>`validateResponseSignature` indicates whether the SAML responses from the identity provider  " +
    "are cryptographically signed. A `null` value indicates that this setting is derived from the SAML " +
    "metadata from the identity provider performing signature validation if a signing key " +
    "(`KeyDescriptor`) is included." +
    "<li>`validateAssertionSignature` indicates whether the SAML assertions from the identity provider " +
    " are cryptographically signed. A `null` value indicates that this setting is derived from  " +
    "the SAML metadata from the identity provider performing signature validation if a signing key " +
    "(`KeyDescriptor`) is included.</li>" +
    "<li>`identityProviderMetadataXml` is the metadata of the identity provider.</li>" +
    "</ul>")
public class ApiSamlConfigurationDTO
{
  public String identityProviderName;

  public String entityId;

  public String firstNameAttributeName;

  public String lastNameAttributeName;

  public String emailAttributeName;

  public String usernameAttributeName;

  public String groupsAttributeName;

  public Boolean validateResponseSignature;

  public Boolean validateAssertionSignature;
}
