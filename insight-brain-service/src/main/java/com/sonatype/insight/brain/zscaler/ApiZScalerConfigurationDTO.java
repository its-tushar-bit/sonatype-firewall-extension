/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

public class ApiZScalerConfigurationDTO
{
  private String username;

  private String password;

  private String hostname;

  private String apiKey;

  private Boolean eulaAgreed;

  private boolean mavenFormatEnabled;

  private boolean npmFormatEnabled;

  private boolean pypiFormatEnabled;

  private boolean nugetFormatEnabled;

  public ApiZScalerConfigurationDTO() {
    // empty
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(final String hostname) {
    this.hostname = hostname;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(final String apiKey) {
    this.apiKey = apiKey;
  }

  public Boolean isEulaAgreed() {
    return eulaAgreed;
  }

  public void setEulaAgreed(final boolean eulaAgreed) {
    this.eulaAgreed = eulaAgreed;
  }

  public boolean isMavenFormatEnabled() {
    return mavenFormatEnabled;
  }

  public void setMavenFormatEnabled(final boolean mavenFormatEnabled) {
    this.mavenFormatEnabled = mavenFormatEnabled;
  }

  public boolean isNpmFormatEnabled() {
    return npmFormatEnabled;
  }

  public void setNpmFormatEnabled(final boolean npmFormatEnabled) {
    this.npmFormatEnabled = npmFormatEnabled;
  }

  public boolean isPypiFormatEnabled() {
    return pypiFormatEnabled;
  }

  public void setPypiFormatEnabled(final boolean pypiFormatEnabled) {
    this.pypiFormatEnabled = pypiFormatEnabled;
  }

  public boolean isNugetFormatEnabled() {
    return nugetFormatEnabled;
  }

  public void setNugetFormatEnabled(final boolean nugetFormatEnabled) {
    this.nugetFormatEnabled = nugetFormatEnabled;
  }
}
