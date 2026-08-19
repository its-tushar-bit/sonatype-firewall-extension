/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;

public class ComponentDetailsDTO
{
  public String matchState;

  public Set<License> declaredLicenses;

  public Set<License> observedLicenses;

  public Set<License> overriddenLicenses;

  public Set<License> effectiveLicenses;

  public LicenseStatus effectiveLicenseStatus;

  public Long catalogDate;

  public Integer relativePopularity;

  public String website;

  public Map<PolicyThreatCategory, Integer> policyMaxThreatLevelsByCategory;

  public int violatedPolicyCount;

  public Float highestSecurityVulnerabilitySeverity;

  public int securityVulnerabilityCount;

  public boolean majorRevisionStep;

  public String identificationSource;

  public String identificationSourceComment;

  public ComponentDisplayName displayName;

  public ComponentIdentifier componentIdentifier;

  public List<PolicyAlert> policyAlerts = Collections.emptyList();

  public Integer breakingChangesCount;

  public List<SecurityVulnerability> securityVulnerabilities;
}
