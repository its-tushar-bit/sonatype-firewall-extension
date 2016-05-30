/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

/**
 * Details about a JIRA server.
 *
 * @since 1.21.0
 */
public class JiraServerInfo
{
  private String baseUrl;

  private String version;

  private String buildNumber;

  // date and time appear to be ISO-8601 though docs/schema simply indicate string, so using string for now
  private String buildDate;

  private String serverTime;

  private String serverTitle;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public String getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(final String buildNumber) {
    this.buildNumber = buildNumber;
  }

  public String getBuildDate() {
    return buildDate;
  }

  public void setBuildDate(final String buildDate) {
    this.buildDate = buildDate;
  }

  public String getServerTime() {
    return serverTime;
  }

  public void setServerTime(final String serverTime) {
    this.serverTime = serverTime;
  }

  public String getServerTitle() {
    return serverTitle;
  }

  public void setServerTitle(final String serverTitle) {
    this.serverTitle = serverTitle;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "baseUrl='" + baseUrl + '\'' +
        ", version='" + version + '\'' +
        ", buildNumber='" + buildNumber + '\'' +
        ", buildDate='" + buildDate + '\'' +
        ", serverTime='" + serverTime + '\'' +
        ", serverTitle='" + serverTitle + '\'' +
        '}';
  }
}
