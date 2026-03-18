/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.git.SourceControlComponentDetails.ComponentInfo;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;

import org.apache.commons.collections4.CollectionUtils;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DEPENDENCIES_JSON;

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

  private final ComponentLoaderFactory componentLoaderFactory;

  @Inject
  SourceControlComponentLoader(
      final ReportService reportService,
      final ApplicationDAO applicationDAO,
      final ComponentLoaderFactory componentLoaderFactory)
  {
    this.reportService = reportService;
    this.applicationDAO = applicationDAO;
    this.componentLoaderFactory = componentLoaderFactory;
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

    ApplicationReport applicationReport = reportService.getReport(application.getId(), scanId);
    ReportEntry bomReportEntry = applicationReport.getEntry(BOM_JSON.getName());
    ReportEntry dependenciesReportEntry = applicationReport.getEntry(DEPENDENCIES_JSON.getName());

    ComponentLoader componentLoader = componentLoaderFactory.createComponentLoader(application);
    List<Component> components;
    if (dependenciesReportEntry == null) {
      // only display name will be available; no dependency information
      components = componentLoader
          .getAll(null /* license data */, null /* security data */, bomReportEntry.buf, null /* dependency data */);
    }
    else {
      // display name and dependency information will be available
      components = componentLoader
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
    if (CollectionUtils.isEmpty(policyViolations)) {
      return;
    }
    Map<String, ComponentInfo> byHashMap = componentDetails.getHashToComponentInfoMap();
    Map<ComponentIdentifier, ComponentInfo> byIdentifierMap = componentDetails.getIdentifierToComponentInfoMap();
    for (PolicyViolation violation : policyViolations) {
      ComponentInfo componentInfo = null;
      ComponentIdentifier componentIdentifier = violation.getComponentIdentifier();
      if (componentIdentifier != null && !byIdentifierMap.containsKey(componentIdentifier)) {
        componentInfo =
            new ComponentInfo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString(), null);
        byIdentifierMap.put(componentIdentifier, componentInfo);
      }
      // update the hash-based map, if needed
      String hash = violation.getHash();
      if (hash != null && componentInfo != null) {
        byHashMap.putIfAbsent(hash, componentInfo);
      }
    }
  }

  public void enhanceSourceControlComponentDetailsWithDirectDependencyInformation(
      final SourceControlComponentDetails componentDetails,
      final List<PullRequestLineCommentDTO> pullRequestLineComments)
  {
    if (CollectionUtils.isEmpty(pullRequestLineComments)) {
      return;
    }
    Map<ComponentIdentifier, ComponentInfo> byIdentifierMap = componentDetails.getIdentifierToComponentInfoMap();
    Map<String, ComponentInfo> byHashMap = componentDetails.getHashToComponentInfoMap();
    for (PullRequestLineCommentDTO pullRequestLineComment : pullRequestLineComments) {
      ComponentIdentifier componentIdentifier = pullRequestLineComment.getComponentIdentifier();
      if (componentIdentifier != null && byIdentifierMap.containsKey(componentIdentifier)) {
        ComponentInfo componentInfo = byIdentifierMap.get(componentIdentifier);
        if (componentInfo != null &&
            (componentInfo.getDirectDependency() == null || !componentInfo.getDirectDependency()))
        {
          componentInfo =
              new ComponentInfo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString(), true);
          byIdentifierMap.put(componentIdentifier, componentInfo);
          // update the hash-based map, if needed
          String hash = pullRequestLineComment.getHash();
          if (hash != null) {
            byHashMap.put(hash, componentInfo);
          }
        }
      }
    }
  }
}
