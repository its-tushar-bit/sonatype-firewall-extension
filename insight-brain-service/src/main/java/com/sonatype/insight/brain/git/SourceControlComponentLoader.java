/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.git.SourceControlComponentDetails.ComponentInfo;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;

/**
 * Used by the IQ for SCM feature to retrieve component details useful for the auto PRs and PR commenting flows,
 * in particular whether a component is a direct or transitive dependency and its display name.
 */
@Named
@Singleton
public class SourceControlComponentLoader
{
  private final ReportService reportService;

  private final ApplicationDAO applicationDAO;

  @Inject
  SourceControlComponentLoader(final ReportService reportService, final ApplicationDAO applicationDAO) {
    this.reportService = reportService;
    this.applicationDAO = applicationDAO;
  }

  public SourceControlComponentDetails getSourceControlComponentDetails(
      final String applicationId,
      final String scanId) throws IOException
  {
    Application application = applicationDAO.getByIdNotNull(applicationId);
    return getSourceControlComponentDetails(application, scanId);
  }

  public SourceControlComponentDetails getSourceControlComponentDetails(
      final Application application,
      final String scanId) throws IOException
  {
    Objects.requireNonNull(application, "application is required");
    Objects.requireNonNull(scanId, "scanId is required");

    final SourceControlComponentDetails componentDetails = new SourceControlComponentDetails();

    File reportFile = reportService.getReport(application.getId(), scanId);
    ReportEntry bomReportEntry = Report.getEntry(reportFile, Report.BOM_JSON_FILENAME);
    ReportEntry dependenciesReportEntry = Report.getEntry(reportFile, Report.DEPENDENCIES_JSON_FILENAME);

    ComponentDAO componentDAO = new ComponentDAO(application);
    List<Component> components;
    if (dependenciesReportEntry == null) {
      // only display name will be available; no dependency information
      components = componentDAO
          .getAll(null /* license data */, null /* security data */, bomReportEntry.buf, null  /* dependency data */);
    }
    else {
      // display name and dependency information will be available
      components = componentDAO
          .getAll(null /* license data */, null /* security data */, bomReportEntry.buf, dependenciesReportEntry.buf);
    }
    collectComponentInfo(componentDetails, components);

    return componentDetails;
  }

  private void collectComponentInfo(
      final SourceControlComponentDetails componentDetails,
      final List<Component> components)
  {
    for (Component component : components) {
      String hash = component.getHash();
      ComponentInfo componentInfo = new ComponentInfo(component.getDisplayName(), component.getDirectDependency());
      componentDetails.getHashToComponentInfoMap().put(hash, componentInfo);

      ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
      if (componentIdentifier != null) {
        componentDetails.getIdentifierToComponentInfoMap().put(componentIdentifier, componentInfo);
      }
    }
  }

  /**
   * Adds mappings for the components from the cleared policy violations section of a policy violation diff;
   * some may not be included in the initial bom file.
   */
  public void enhanceSourceControlComponentDetails(
      final SourceControlComponentDetails componentDetails,
      final List<PolicyViolation> policyViolations)
  {
    if (policyViolations == null) {
      return;
    }
    Map<ComponentIdentifier, ComponentInfo> componentMap = componentDetails.getIdentifierToComponentInfoMap();
    for (PolicyViolation violation : policyViolations) {
      ComponentIdentifier componentIdentifier = violation.getComponentIdentifier();
      if (componentIdentifier != null && !componentMap.containsKey(componentIdentifier)) {
        componentMap.put(componentIdentifier,
            new ComponentInfo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString(), null));
      }
    }
  }
}
