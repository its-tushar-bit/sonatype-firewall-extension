/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class InsightWork
{
  private final InsightConfig insightConfig;

  @Inject
  public InsightWork(final InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  public File getWorkDir() {
    return insightConfig.getSonatypeWork();
  }

  public File getScanDir(final String appId) {
    return new File(insightConfig.getSonatypeWork(), "scan/" + appId);
  }

  public File getScanFile(final String appId, final String scanId) {
    return new File(getScanDir(appId), "scan-" + scanId + ".xml.gz");
  }

  public File getAuditDir(final String appId) {
    return new File(insightConfig.getSonatypeWork(), "audit/" + appId);
  }

  public File getReportDir() {
    return new File(insightConfig.getSonatypeWork(), "report");
  }

  public File getReportDir(final String appId) {
    return new File(insightConfig.getSonatypeWork(), "report/" + appId);
  }

  public File getReportDir(final String appId, final String scanId) {
    return new File(getReportDir(appId), scanId);
  }

  public File getReportFile(final String appId, final String scanId) {
    return new File(getReportDir(appId, scanId), "report.zip");
  }

  public File getApplicationIconDir() {
    return new File(insightConfig.getSonatypeWork(), "data/application");
  }

  public File getOrganizationIconDir() {
    return new File(insightConfig.getSonatypeWork(), "data/organization");
  }

  public File getDataDir() {
    return new File(insightConfig.getSonatypeWork(), "data");
  }
}
