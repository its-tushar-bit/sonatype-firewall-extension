/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;

public class OAuth2ConfigurationDTO
{
  private String idpIssuer;

  private String idpJwksUrl;

  private String idpJwsAlgorithm;

  private String idpJwks;

  private String usernameClaim;

  private String firstNameClaim;

  private String lastNameClaim;

  private String emailClaim;

  private String groupsClaim;

  private String exactMatchClaimsJson;

  public OAuth2ConfigurationDTO() {
  }

  public OAuth2ConfigurationDTO(
      final String idpIssuer,
      final String idpJwksUrl,
      final String idpJwsAlgorithm,
      final String idpJwks)
  {
    this.idpIssuer = idpIssuer;
    this.idpJwksUrl = idpJwksUrl;
    this.idpJwsAlgorithm = idpJwsAlgorithm;
    this.idpJwks = idpJwks;
  }

  public String getIdpIssuer() {
    return idpIssuer;
  }

  public void setIdpIssuer(final String idpIssuer) {
    this.idpIssuer = idpIssuer;
  }

  public String getIdpJwksUrl() {
    return idpJwksUrl;
  }

  public void setIdpJwksUrl(final String idpJwksUrl) {
    this.idpJwksUrl = idpJwksUrl;
  }

  public String getIdpJwsAlgorithm() {
    return idpJwsAlgorithm;
  }

  public void setIdpJwsAlgorithm(final String idpJwsAlgorithm) {
    this.idpJwsAlgorithm = idpJwsAlgorithm;
  }

  public String getIdpJwks() {
    return idpJwks;
  }

  public void setIdpJwks(final String idpJwks) {
    this.idpJwks = idpJwks;
  }

  public String getUsernameClaim() {
    return usernameClaim;
  }

  public void setUsernameClaim(final String usernameClaim) {
    this.usernameClaim = usernameClaim;
  }

  public String getFirstNameClaim() {
    return firstNameClaim;
  }

  public void setFirstNameClaim(final String firstNameClaim) {
    this.firstNameClaim = firstNameClaim;
  }

  public String getLastNameClaim() {
    return lastNameClaim;
  }

  public void setLastNameClaim(final String lastNameClaim) {
    this.lastNameClaim = lastNameClaim;
  }

  public String getEmailClaim() {
    return emailClaim;
  }

  public void setEmailClaim(final String emailClaim) {
    this.emailClaim = emailClaim;
  }

  public String getGroupsClaim() {
    return groupsClaim;
  }

  public void setGroupsClaim(final String groupsClaim) {
    this.groupsClaim = groupsClaim;
  }

  public String getExactMatchClaimsJson() {
    return exactMatchClaimsJson;
  }

  public void setExactMatchClaimsJson(final String exactMatchClaimsJson) {
    this.exactMatchClaimsJson = exactMatchClaimsJson;
  }

  public static OAuth2Configuration fromDTO(OAuth2ConfigurationDTO oAuth2ConfigurationDTO) {
    OAuth2Configuration oAuth2Configuration =
        new OAuth2Configuration(oAuth2ConfigurationDTO.idpIssuer, oAuth2ConfigurationDTO.idpJwsAlgorithm,
            oAuth2ConfigurationDTO.idpJwksUrl,
            oAuth2ConfigurationDTO.idpJwks);
    oAuth2Configuration.setUsernameClaim(oAuth2ConfigurationDTO.usernameClaim);
    oAuth2Configuration.setFirstNameClaim(oAuth2ConfigurationDTO.firstNameClaim);
    oAuth2Configuration.setLastNameClaim(oAuth2ConfigurationDTO.lastNameClaim);
    oAuth2Configuration.setEmailClaim(oAuth2ConfigurationDTO.emailClaim);
    oAuth2Configuration.setGroupsClaim(oAuth2ConfigurationDTO.groupsClaim);
    oAuth2Configuration.setExactMatchClaimsJson(oAuth2ConfigurationDTO.exactMatchClaimsJson);
    return oAuth2Configuration;
  }

  public static OAuth2ConfigurationDTO toDTO(OAuth2Configuration oAuth2Configuration) {
    OAuth2ConfigurationDTO dto = new OAuth2ConfigurationDTO(
        oAuth2Configuration.getId(),
        oAuth2Configuration.getIdpJwksUrl(),
        oAuth2Configuration.getIdpJwsAlgorithm(),
        oAuth2Configuration.getIdpJwks());
    dto.setUsernameClaim(oAuth2Configuration.getUsernameClaim());
    dto.setFirstNameClaim(oAuth2Configuration.getFirstNameClaim());
    dto.setLastNameClaim(oAuth2Configuration.getLastNameClaim());
    dto.setEmailClaim(oAuth2Configuration.getEmailClaim());
    dto.setGroupsClaim(oAuth2Configuration.getGroupsClaim());
    dto.setExactMatchClaimsJson(oAuth2Configuration.getExactMatchClaimsJson());
    return dto;
  }
}
