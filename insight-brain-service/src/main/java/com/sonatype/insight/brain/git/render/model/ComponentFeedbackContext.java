/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.git.render.ThreatLevelDisplay;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.fasterxml.jackson.annotation.JsonProperty;

import static java.util.Objects.requireNonNull;

/**
 * This context class represents all the variables needed to render the pullrequest-component-feedback.ftl Freemarker
 * template.
 */
public class ComponentFeedbackContext
{
  private final boolean htmlSupported;

  private final ThreatLevelDisplay threatLevelDisplay;

  private final String componentDetailLink;

  private final String componentDisplayName;

  private final SourceControlProvider provider;

  private final int breakingChangesCount;

  private final String suggestedVersion;

  private final String suggestedVersionType;

  private final boolean hasRemediationForDependencies;

  private final List<SecurityIssue> securityIssues;

  private final MDImages dependencyImage;

  private final String codeSuggestion;

  private final boolean hasSecurityIssues;

  private final boolean hasReducedSecurityData;

  public ComponentFeedbackContext(
      final boolean htmlSupported,
      final ThreatLevelDisplay threatLevelDisplay,
      final String componentDetailLink,
      final String componentDisplayName,
      final SourceControlProvider provider,
      final int breakingChangesCount,
      final String suggestedVersion,
      final String suggestedVersionType,
      final boolean hasRemediationForDependencies,
      final List<SecurityIssue> securityIssues,
      final MDImages dependencyImage,
      final String codeSuggestion,
      final boolean hasSecurityIssues,
      final boolean hasReducedSecurityData)
  {
    this.htmlSupported = htmlSupported;
    this.threatLevelDisplay = requireNonNull(threatLevelDisplay);
    this.componentDetailLink = componentDetailLink;
    this.componentDisplayName = requireNonNull(componentDisplayName);
    this.provider = requireNonNull(provider);
    this.breakingChangesCount = breakingChangesCount;
    this.suggestedVersion = suggestedVersion;
    this.suggestedVersionType = suggestedVersionType;
    this.hasRemediationForDependencies = hasRemediationForDependencies;
    this.securityIssues = requireNonNull(securityIssues);
    this.dependencyImage = dependencyImage;
    this.codeSuggestion = codeSuggestion;
    this.hasSecurityIssues = hasSecurityIssues;
    this.hasReducedSecurityData = hasReducedSecurityData;
  }

  public String getCodeSuggestion() {
    return codeSuggestion;
  }

  @JsonProperty("isHtmlSupported")
  public boolean isHtmlSupported() {
    return htmlSupported;
  }

  public ThreatLevelDisplay getThreatLevelDisplay() {
    return threatLevelDisplay;
  }

  public String getComponentDetailLink() {
    return componentDetailLink;
  }

  public String getFormattedDate() {
    return new SimpleDateFormat("MMM dd, yyyy").format(new Date());
  }

  public SourceControlProvider getProvider() {
    return provider;
  }

  public String getSuggestedVersion() {
    return suggestedVersion;
  }

  public String getSuggestedVersionType() {
    return suggestedVersionType;
  }

  public int getBreakingChangesCount() {
    return breakingChangesCount;
  }

  public boolean isHasRemediationForDependencies() {
    return hasRemediationForDependencies;
  }

  public List<SecurityIssue> getSecurityIssues() {
    return securityIssues;
  }

  public String getComponentDisplayName() {
    return componentDisplayName;
  }

  public MDImages getDependencyImage() {
    return dependencyImage;
  }

  public boolean isHasSecurityIssues() {
    return hasSecurityIssues;
  }

  public boolean isHasReducedSecurityData() {
    return hasReducedSecurityData;
  }
}
