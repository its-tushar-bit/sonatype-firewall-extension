/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.URL;

import javax.mail.internet.InternetAddress;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.validation.ValidationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsightConfig
    extends Configuration
{
  private static final Logger log = LoggerFactory.getLogger(InsightConfig.class);

  {
    setHttpConfiguration(new HttpConfig());
  }

  @Valid
  @NotNull
  @JsonProperty
  private ProxyConfig proxy = new ProxyConfig();

  @NotNull
  @JsonProperty
  private MailConfig mail = new MailConfig();

  @JsonProperty
  private String baseUrl;

  @NotNull
  @JsonProperty
  private String saasAddress = "https://clm.sonatype.com/";

  @NotNull
  @JsonProperty
  private String cdnUrl = "http://cdn.sonatype.com/";

  @NotNull
  @JsonProperty
  private String sonatypeWork = "sonatype-work/clm-server";

  @NotNull
  @JsonProperty
  private int releaseGraphCacheSize = 1000;

  @NotNull
  @JsonProperty
  @Min(0)
  @Max(23)
  private int policyMonitoringHour = 0;

  public ProxyConfig getProxyConfig() {
    return proxy;
  }

  public MailConfig getMailConfig() {
    return mail;
  }

  public int getReleaseGraphCacheSize() {
    return releaseGraphCacheSize;
  }

  public String getSaasAddress() {
    return saasAddress;
  }

  public File getSonatypeWork() {
    return new File(sonatypeWork);
  }

  public File getConfigDir() {
    return new File(sonatypeWork, "config");
  }

  public void setProxyConfig(ProxyConfig proxyConfig) {
    this.proxy = proxyConfig;
  }

  public void setMailConfig(final MailConfig mailConfig) {
    this.mail = mailConfig;
  }

  public void setReleaseGraphCacheSize(int releaseGraphCacheSize) {
    this.releaseGraphCacheSize = releaseGraphCacheSize;
  }

  public void setSaasAddress(final String saasAddress) {
    this.saasAddress = saasAddress;
  }

  public void setSonatypeWork(final String sonatypeWork) {
    this.sonatypeWork = sonatypeWork;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    if (baseUrl != null && !baseUrl.endsWith("/")) {
      this.baseUrl += '/';
    }
  }

  @ValidationMethod(message = "baseUrl is invalid")
  public boolean isValidBaseUrl() {
    try {
      String url = getBaseUrl();
      if (url != null) {
        new URL(url);
      }
      return true;
    }
    catch (Exception e) {
      log.error("Invalid baseUrl: {}", e.getMessage());
      return false;
    }
  }

  @ValidationMethod(message = "mail.systemEmail is invalid")
  public boolean isValidSystemMailAddress() {
    try {
      new InternetAddress(getMailConfig().getSystemEmail());
      return true;
    }
    catch (Exception e) {
      log.error("Invalid mail.systemEmail: {}", e.getMessage());
      return false;
    }
  }

  public String getCdnUrl() {
    return cdnUrl;
  }

  public void setCdnUrl(String cdnUrl) {
    this.cdnUrl = cdnUrl;
    if (cdnUrl != null && !cdnUrl.endsWith("/")) {
      this.cdnUrl += '/';
    }
  }

  @ValidationMethod(message = "cdnUrl is invalid")
  public boolean isValidCdnUrl() {
    try {
      String url = getCdnUrl();
      new URL(url);
      return true;
    }
    catch (Exception e) {
      log.error("Invalid cndUrl: {}", e.getMessage());
      return false;
    }
  }

  /**
   * @since 1.8
   */
  public int getPolicyMonitoringHour() {
    return policyMonitoringHour;
  }

  /**
   * @since 1.8
   */
  public void setPolicyMonitoringHour(final int policyMonitoringHour) {
    this.policyMonitoringHour = policyMonitoringHour;
  }
}
