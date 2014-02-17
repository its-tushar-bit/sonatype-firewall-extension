/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.ReportData.LicenseData;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 * 
 * @since 1.10
 */
@Named
public class ReportDataService
{
  private final InsightWork work;

  private final ApplicationDAO appDAO;

  private final MultiLicenseDAO multiLicenseDAO;

  @Inject
  public ReportDataService(InsightWork work, ApplicationDAO appDAO, MultiLicenseDAO multiLicenseDAO) {
    this.work = work;
    this.appDAO = appDAO;
    this.multiLicenseDAO = multiLicenseDAO;
  }

  @Authorize(permission = Permission.READ)
  public ReportData getData(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      String scanId) throws IOException
  {
    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);
    File reportFile = ReportResource.getReport(work, app.getId(), scanId);
    if (reportFile == null) {
      throw new NotFoundException("Could not find a report with id " + scanId);
    }

    ReportEntry bomEntry = Report.getEntry(reportFile, "bom.json");
    ReportEntry securityEntry = Report.getEntry(reportFile, "security.json");
    ReportEntry licenseEntry = Report.getEntry(reportFile, "licenses.json");
    if (bomEntry == null || securityEntry == null || licenseEntry == null) {
      throw new BadRequestException("The report with id " + scanId + " contains no component data");
    }

    ReportData data = new ReportData();

    JsonNode bomNode = JsonUtils.parse(bomEntry.buf);
    Map<String, ReportData.Component> componentsByHash = new HashMap<>();
    for (JsonNode node : bomNode.get("aaData")) {
      ReportData.Component component = new ReportData.Component();
      component.hash = JsonUtils.getNullableString(node.get("hash"));
      if (component.hash != null) {
        componentsByHash.put(component.hash, component);
      }
      ReportData.Coordinates coords = new ReportData.Coordinates();
      coords.groupId = JsonUtils.getNullableString(node.get("groupId"));
      if (coords.groupId != null) {
        coords.artifactId = JsonUtils.getNullableString(node.get("artifactId"));
        coords.version = JsonUtils.getNullableString(node.get("version"));
        component.mavenCoordinates = coords;
      }
      component.matchState = JsonUtils.getNullableString(node.get("matchState"));
      component.proprietary = node.path("proprietary").asBoolean();
      for (JsonNode path : node.path("pathnames")) {
        String pathname = path.asText();
        if (!pathname.startsWith("dependency:")) {
          component.pathnames.add(pathname);
        }
      }
      data.components.add(component);
    }

    JsonNode securityNode = JsonUtils.parse(securityEntry.buf);
    for (JsonNode node : securityNode.get("aaData")) {
      String hash = JsonUtils.getNullableString(node.get("hash"));
      ReportData.SecurityIssue sv = new ReportData.SecurityIssue();
      sv.source = JsonUtils.getNullableString(node.get("source"));
      sv.reference = JsonUtils.getNullableString(node.get("reference"));
      sv.score = JsonUtils.getNullableFloat(node.get("score"));
      sv.status = getStatus(JsonUtils.getNullableString(node.get("status")));
      ReportData.Component component = componentsByHash.get(hash);
      if (component.securityData == null) {
        component.securityData = new ReportData.SecurityData();
      }
      component.securityData.securityIssues.add(sv);
    }

    JsonNode licenseNode = JsonUtils.parse(licenseEntry.buf);
    for (JsonNode node : licenseNode.get("aaData")) {
      String hash = JsonUtils.getNullableString(node.get("hash"));
      ReportData.Component component = componentsByHash.get(hash);
      if (component.licenseData == null) {
        component.licenseData = new LicenseData();
      }
      component.licenseData.status = getStatus(JsonUtils.getNullableString(node.get("status")));
      convertLicenses(component.licenseData.declaredLicenses, node.path("declaredLicenses"));
      convertLicenses(component.licenseData.observedLicenses, node.path("observedLicenses"));
      convertLicenses(component.licenseData.overriddenLicenses, node.path("overriddenLicenses"));
    }

    return data;
  }

  private static String getStatus(String status) {
    return (status != null) ? status : "Open";
  }

  private void convertLicenses(List<ReportData.License> licenses, JsonNode licensesNode) {
    for (JsonNode node : licensesNode) {
      ReportData.License license = new ReportData.License();
      license.licenseName = node.asText();
      license.licenseId = multiLicenseDAO.getByNameNotNull(license.licenseName).getId();
      licenses.add(license);
    }
  }
}
