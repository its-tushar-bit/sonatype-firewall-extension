/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.dto;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;

public class SecurityConfigurationDTO
{
  private List<String> adminEmails;

  private String base64IdentityProviderXml;

  private ApiSamlConfigurationDTO samlConfiguration;

  public List<String> getAdminEmails() {
    return adminEmails;
  }

  public void setAdminEmails(final List<String> adminEmails) {
    this.adminEmails = adminEmails;
  }

  public String getBase64IdentityProviderXml() {
    return base64IdentityProviderXml;
  }

  public void setBase64IdentityProviderXml(final String base64IdentityProviderXml) {
    this.base64IdentityProviderXml = base64IdentityProviderXml;
  }

  public ApiSamlConfigurationDTO getSamlConfiguration() {
    return samlConfiguration;
  }

  public void setSamlConfiguration(final ApiSamlConfigurationDTO samlConfiguration) {
    this.samlConfiguration = samlConfiguration;
  }
}
