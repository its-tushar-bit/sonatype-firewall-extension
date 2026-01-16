/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupVulnerabilityDAO;
import com.sonatype.insight.brain.model.Owner;

@Named
public class ComponentLoaderFactory
{
  private final MultiLicenseDAO multiLicenseDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private final OwnerDAO ownerDAO;

  private final VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO;

  private final VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO;

  private final VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO;

  private final VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO;

  private final ComponentLabelDAO componentLabelDAO;

  private final VulnerabilityGroupDAO vulnerabilityGroupDAO;

  private final VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO;

  @Inject
  public ComponentLoaderFactory(
      final MultiLicenseDAO multiLicenseDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO,
      final LicenseOverrideDAO licenseOverrideDAO,
      final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
      final OwnerDAO ownerDAO,
      final ComponentLabelDAO componentLabelDAO,
      final VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO,
      final VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO,
      final VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO,
      final VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO,
      final VulnerabilityGroupDAO vulnerabilityGroupDAO,
      final VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO)
  {
    this.ownerDAO = ownerDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.licenseThreatGroupLicenseDAO = licenseThreatGroupLicenseDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.componentLabelDAO = componentLabelDAO;
    this.vulnerabilityCustomRemediationDAO = vulnerabilityCustomRemediationDAO;
    this.vulnerabilityCustomCweDAO = vulnerabilityCustomCweDAO;
    this.vulnerabilityCustomCvssVectorDAO = vulnerabilityCustomCvssVectorDAO;
    this.vulnerabilityCustomCvssSeverityDAO = vulnerabilityCustomCvssSeverityDAO;
    this.vulnerabilityGroupDAO = vulnerabilityGroupDAO;
    this.vulnerabilityGroupVulnerabilityDAO = vulnerabilityGroupVulnerabilityDAO;
  }

  public ComponentLoader createComponentLoader(Owner owner) {
    return new ComponentLoader(owner, multiLicenseDAO, licenseThreatGroupDAO, licenseThreatGroupLicenseDAO,
        licenseOverrideDAO, securityVulnerabilityOverrideDAO, ownerDAO, componentLabelDAO,
        vulnerabilityCustomRemediationDAO, vulnerabilityCustomCweDAO, vulnerabilityCustomCvssVectorDAO,
        vulnerabilityCustomCvssSeverityDAO, vulnerabilityGroupDAO, vulnerabilityGroupVulnerabilityDAO);
  }
}
