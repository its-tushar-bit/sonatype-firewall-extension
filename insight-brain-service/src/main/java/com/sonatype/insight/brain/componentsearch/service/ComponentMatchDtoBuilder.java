/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.componentsearch.service;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.componentsearch.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ComponentMatchDtoBuilder
{
  private static final Logger log = LoggerFactory.getLogger(ComponentMatchDtoBuilder.class);

  private final BaseUrl baseUrl;

  @Inject
  public ComponentMatchDtoBuilder(final BaseUrl baseUrl) {
    this.baseUrl = baseUrl;
  }

  public ApplicationComponentMatchDTO buildMatch(
      final Application application,
      final PolicyEvaluation evaluation,
      final ApplicationComponent component,
      final String cveId,
      final List<PolicyViolation> componentViolations)
  {
    String componentHash = component.getHash();
    boolean hasViolation = !componentViolations.isEmpty();
    boolean isWaived = hasViolation && componentViolations.stream().allMatch(PolicyViolation::isWaived);
    Date evaluationDate = evaluation.getTime();

    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    ComponentDisplayInfo displayInfo = buildComponentDisplayInfo(componentIdentifier);

    ApplicationComponentMatchDTO match = new ApplicationComponentMatchDTO(
        application.getPublicId(),
        application.getName(),
        application.getId(),
        evaluation.getStageTypeId(),
        evaluationDate,
        displayInfo.packageUrl,
        displayInfo.displayName,
        componentHash != null ? componentHash : "",
        cveId,
        "",
        isWaived,
        hasViolation,
        evaluation.getScanId());

    match.setBaseUrl(baseUrl.get());
    return match;
  }

  private ComponentDisplayInfo buildComponentDisplayInfo(final ComponentIdentifier componentIdentifier) {
    PackageUrlIdentifier purlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    String packageUrl = purlIdentifier.getPackageUrl();

    String fullDisplayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
    String displayName = stripVersionFromDisplayName(fullDisplayName);

    return new ComponentDisplayInfo(
        packageUrl != null ? packageUrl : "",
        displayName);
  }

  private String stripVersionFromDisplayName(final String displayName) {
    if (displayName == null || displayName.isEmpty()) {
      return "";
    }

    int lastColonIndex = displayName.lastIndexOf(':');
    if (lastColonIndex > 0) {
      return displayName.substring(0, lastColonIndex);
    }

    return displayName;
  }

  private static class ComponentDisplayInfo
  {
    final String packageUrl;

    final String displayName;

    ComponentDisplayInfo(String packageUrl, String displayName) {
      this.packageUrl = packageUrl;
      this.displayName = displayName;
    }
  }
}
