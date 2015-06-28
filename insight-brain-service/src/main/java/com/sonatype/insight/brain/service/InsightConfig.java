/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.validation.ValidationMethod;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsightConfig
    extends Configuration
{
  private static final Logger log = LoggerFactory.getLogger(InsightConfig.class);

  public static final String DEFAULT_BACKUP_DIR = "db-backup";

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
  private String hdsUrl = "https://clm.sonatype.com/";

  @NotNull
  @JsonProperty
  private String cdnUrl = "http://cdn.sonatype.com/";

  @NotNull
  @JsonProperty
  private String sonatypeWork = "sonatype-work/clm-server";

  /**
   * The directory where db backups are created. If set to a relative path, then it is considered relative to the
   * {@link sonatypeWork} directory.
   * 
   * @since 1.15.0
   */
  @JsonProperty
  private String dbBackupDir = DEFAULT_BACKUP_DIR;

  @NotNull
  @JsonProperty
  private int releaseGraphCacheSize = 1000;

  /**
   * will be appended to the jdbc url, primarily intended for diagnostic usage
   */
  @JsonProperty
  private String additionalDBParams;

  /**
   * @since 1.14.2
   */
  @Min(1)
  @Max(99)
  @JsonProperty
  private Integer dbCacheSizePercent;

  @NotNull
  @JsonProperty
  @Min(0)
  @Max(23)
  private int policyMonitoringHour = 0;

  /**
   * @since 1.14.0
   */
  @NotNull
  @JsonProperty
  private boolean anonymousClientAccessAllowed = true;

  /**
   * @since 1.16.0
   */
  @NotNull
  @JsonProperty
  private boolean csrfProtection = true;

  /**
   * @since 1.14.0
   */
  @NotNull
  @JsonProperty
  @Size(max = 128)
  @Pattern(regexp = "[^\\p{Cntrl}]*")
  private String userAgentSuffix = "";

  /**
   * @since 1.16.0
   */
  @NotNull
  @JsonProperty
  private ReverseProxyAuthenticationConfig reverseProxyAuthentication = new ReverseProxyAuthenticationConfig();

  public ProxyConfig getProxyConfig() {
    return proxy;
  }

  public MailConfig getMailConfig() {
    return mail;
  }

  public int getReleaseGraphCacheSize() {
    return releaseGraphCacheSize;
  }

  public String getHdsUrl() {
    return hdsUrl;
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

  public void setHdsUrl(final String hdsUrl) {
    this.hdsUrl = hdsUrl;
  }

  @SuppressWarnings("unused")
  // for Jackson, supports deserialization of configs from 1.15-
  private void setSaasAddress(final String hdsUrl) {
    setHdsUrl(hdsUrl);
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

  public String getAdditionalDBParams() {
    return additionalDBParams;
  }

  public void setAdditionalDBParams(final String additionalDBParams) {
    this.additionalDBParams = additionalDBParams;
  }

  public Integer getDbCacheSizePercent() {
    return dbCacheSizePercent;
  }

  public void setDbCacheSizePercent(Integer dbCacheSizePercent) {
    this.dbCacheSizePercent = dbCacheSizePercent;
  }

  /**
   * @since 1.14.0
   */
  public boolean isAnonymousClientAccessAllowed() {
    return anonymousClientAccessAllowed;
  }

  /**
   * @since 1.14.0
   */
  public void setAnonymousClientAccessAllowed(final boolean anonymousClientAccessAllowed) {
    this.anonymousClientAccessAllowed = anonymousClientAccessAllowed;
  }

  public boolean isCsrfProtection() {
    return csrfProtection;
  }

  public void setCsrfProtection(boolean csrfProtection) {
    this.csrfProtection = csrfProtection;
  }

  /**
   * @since 1.14.0
   */
  public String getUserAgentSuffix() {
    return userAgentSuffix;
  }

  /**
   * @since 1.14.0
   */
  public void setUserAgentSuffix(final String userAgentSuffix) {
    this.userAgentSuffix = userAgentSuffix;
  }

  /**
   * @since 1.15.0
   */
  public File getDbBackupDir() {
    if (StringUtils.isBlank(dbBackupDir)) {
      dbBackupDir = DEFAULT_BACKUP_DIR;
    }

    File result = new File(dbBackupDir);
    if (!result.isAbsolute()) {
      result = new File(getSonatypeWork(), dbBackupDir);
    }

    return result;
  }

  void setDbBackupDir(String dbBackupDir) {
    this.dbBackupDir = dbBackupDir;
  }

  public ReverseProxyAuthenticationConfig getReverseProxyAuthentication() {
    return reverseProxyAuthentication;
  }

  void setReverseProxyAuthentication(ReverseProxyAuthenticationConfig reverseProxyAuthentication) {
    this.reverseProxyAuthentication = reverseProxyAuthentication;
  }
}
