/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.v2.dto;

import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.brain.utils.CsvWritable;

public class PrioritizedComponent implements CsvWritable
{
  public static final String DEPENDENCY_TYPE_DIRECT = "Direct";

  public static final String DEPENDENCY_TYPE_TRANSITIVE = "Transitive";

  public static final String DEPENDENCY_TYPE_INNER_SOURCE = "Inner Source";

  public static final String DEPENDENCY_TYPE_UNKNOWN = "Unknown";

  private final String displayName;

  private final ComponentIdentifier componentIdentifier;

  private final String componentHash;

  private final String dependencyType;

  private final Boolean hasFailActionOnComponent;

  private String action;

  private final int highestThreat;

  public final String highestThreatPolicyName;

  public final String highestThreatPolicyConstraintName;

  private final boolean securityReachable;

  private final int priority;

  private ApiVersionChangeOptionType remediationType;

  private String remediationVersion;

  public PrioritizedComponent(
      final String displayName,
      final ComponentIdentifier componentIdentifier,
      final String componentHash,
      final String dependencyType,
      final Boolean hasFailActionOnComponent,
      final String action,
      final int highestThreat,
      final String highestThreatPolicyName,
      final String highestThreatPolicyConstraintName,
      final boolean securityReachable,
      final int priority,
      final DevelopmentPrioritizationComponentInfo prioritizationComponentInfo
  )
  {
    this.displayName = displayName;
    this.componentIdentifier = componentIdentifier;
    this.componentHash = componentHash;
    this.dependencyType = dependencyType;
    this.hasFailActionOnComponent = hasFailActionOnComponent;
    this.highestThreat = highestThreat;
    this.highestThreatPolicyName = highestThreatPolicyName;
    this.action = action;
    this.highestThreatPolicyConstraintName = highestThreatPolicyConstraintName;
    this.securityReachable = securityReachable;
    this.priority = priority;
    if (prioritizationComponentInfo != null) {
      this.remediationType = prioritizationComponentInfo.getRemediationType();
      this.remediationVersion = prioritizationComponentInfo.getRemediationVersion();
    }
  }

  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  public String getComponentHash() {
    return componentHash;
  }

  public String getDependencyType() {
    return dependencyType;
  }

  public Boolean getHasFailActionOnComponent() {
    return hasFailActionOnComponent;
  }

  public int getHighestThreat() {
    return highestThreat;
  }

  public String getHighestThreatPolicyName() {
    return highestThreatPolicyName;
  }

  public String getHighestThreatPolicyConstraintName() {
    return highestThreatPolicyConstraintName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getAction() {
    return action;
  }

  public boolean isSecurityReachable() {
    return securityReachable;
  }

  public int getPriority() {
    return priority;
  }

  public ApiVersionChangeOptionType getRemediationType() {
    return remediationType;
  }

  public String getRemediationVersion() {
    return remediationVersion;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrioritizedComponent that = (PrioritizedComponent) o;
    return highestThreat == that.highestThreat &&
        priority == that.priority &&
        securityReachable == that.securityReachable &&
        Objects.equals(displayName, that.displayName) &&
        Objects.equals(componentIdentifier, that.componentIdentifier) &&
        Objects.equals(componentHash,that.componentHash) &&
        Objects.equals(dependencyType, that.dependencyType) &&
        Objects.equals(hasFailActionOnComponent, that.hasFailActionOnComponent) &&
        Objects.equals(highestThreatPolicyName, that.highestThreatPolicyName) &&
        Objects.equals(highestThreatPolicyConstraintName, that.highestThreatPolicyConstraintName) &&
        Objects.equals(action, that.action) &&
        Objects.equals(remediationType, that.remediationType) &&
        Objects.equals(remediationVersion, that.remediationVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        displayName,
        componentIdentifier,
        componentHash,
        dependencyType,
        hasFailActionOnComponent,
        action,
        highestThreat,
        highestThreatPolicyName,
        highestThreatPolicyConstraintName,
        securityReachable,
        priority,
        remediationType,
        remediationVersion);
  }

  @Override
  public String toString() {
    return "PrioritizedComponent{" +
        "displayName='" + displayName + '\'' +
        ", componentIdentifier=" + componentIdentifier +
        ", componentHash=" + componentHash +
        ", dependencyType='" + dependencyType + '\'' +
        ", hasFailActionOnComponent=" + hasFailActionOnComponent +
        ", action=" + action +
        ", highestThreat=" + highestThreat +
        ", highestThreatPolicyName=" + highestThreatPolicyName +
        ", highestThreatPolicyConstraintName=" + highestThreatPolicyConstraintName +
        ", securityReachable=" + securityReachable +
        ", priority=" + priority +
        ", remediationType=" + remediationType +
        ", remediationVersion=" + remediationVersion +
        '}';
  }

  public static String getCsvHeader() {
    // this is the dto involved in the export
    return "Display Name,Component Identifier,Component Hash,Dependency Type,Has Fail Action On Component,Action," +
            "Highest Threat,Highest Threat Policy Name,Highest Threat Policy Constraint Name,Security Reachable," +
            "Priority,Remediation Type,Remediation Version";
  }

  @Override
  public String toCsvLine() {
    return joiner.useForNull("").join(displayName, componentIdentifier, componentHash, dependencyType,
            hasFailActionOnComponent, action, highestThreat, highestThreatPolicyName, highestThreatPolicyConstraintName,
            securityReachable, priority, remediationType, remediationVersion);
  }
}
