/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.ReportData.LicenseData;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 * 
 * @since 1.9.1
 */
@Named
public class ReportDataService
{
  private final InsightWork work;

  private final ApplicationDAO appDAO;

  private final MultiLicenseDAO multiLicenseDAO;

  private final ComponentDAO componentDAO;

  @Inject
  public ReportDataService(InsightWork work, ApplicationDAO appDAO, MultiLicenseDAO multiLicenseDAO, ComponentDAO componentDAO) {
    this.work = work;
    this.appDAO = appDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.componentDAO = componentDAO;
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

    List<Component> components = componentDAO.getAll(app, licenseEntry.buf, securityEntry.buf, bomEntry.buf);

    ReportData data = new ReportData();
    for (Component comp : components) {
      ReportData.Component component = new ReportData.Component();
      component.hash = comp.getHash();
      ReportData.Coordinates coords = new ReportData.Coordinates();
      coords.groupId = comp.getGroupId();
      if (coords.groupId != null) {
        coords.artifactId = comp.getArtifactId();
        coords.version = comp.getVersion();
        component.mavenCoordinates = coords;
      }
      component.matchState = comp.getMatchState().getId();
      component.proprietary = comp.isProprietary();
      for (String pathname : comp.getPathnames()) {
        if (!pathname.startsWith("dependency:")) {
          component.pathnames.add(pathname);
        }
      }
      if (!MatchState.UNKNOWN.equals(comp.getMatchState())) {
        component.securityData = new ReportData.SecurityData();
        for (SecurityVulnerability vuln : comp.getSecurityVulnerabilities()) {
          ReportData.SecurityIssue sv = new ReportData.SecurityIssue();
          sv.source = vuln.getSource();
          sv.reference = vuln.getRefId();
          sv.severity = vuln.getSeverity();
          sv.status = vuln.getStatus().getName();
          component.securityData.securityIssues.add(sv);
        }
        component.licenseData = new LicenseData();
        component.licenseData.status = comp.getLicenseOverrideStatus().getName();
        convertLicenses(component.licenseData.declaredLicenses, comp.getDeclaredLicenseIds());
        convertLicenses(component.licenseData.observedLicenses, comp.getObservedLicenseIds());
        if (comp.getLicenseOverrideId() != null) {
          convertLicenses(component.licenseData.overriddenLicenses, Collections.singleton(comp.getLicenseOverrideId()));
        }
      }
      data.components.add(component);
    }

    return data;
  }

  private void convertLicenses(List<ReportData.License> licenses, Collection<String> licenseIds) {
    for (String licenseId : licenseIds) {
      ReportData.License license = new ReportData.License();
      license.licenseId = licenseId;
      license.licenseName = multiLicenseDAO.getByIdNotNull(licenseId).getShortDisplayName();
      licenses.add(license);
    }
  }
}
