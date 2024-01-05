/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.utils.IdUtils;

/**
 * {@link Report} uses static references. At runtime Guice
 * <a href="https://github.com/google/guice/wiki/Injections#static-injections">static-injection</a> is used to
 * populate the references. At test time we use this helper class.
 */
public class ReportTestHelper
{
  public static void inject(final DAOFactory daoFactory) {
    ThirdPartyComponentDAO thirdPartyComponentService = new ThirdPartyComponentDAO(null);
    IdUtils idUtils = new IdUtils(daoFactory.createApplicationDAO(), daoFactory.createOrganizationDAO(),
        daoFactory.createRepositoryDAO(), daoFactory.createRepositoryManagerDAO());
    ProprietaryConfigService proprietaryConfigService =
        new ProprietaryConfigService(daoFactory.createProprietaryConfigDAO(), daoFactory.createOwnerDAO(), idUtils);
    ComponentLoaderFactory componentLoaderFactory =
        new ComponentLoaderFactory(daoFactory.createMultiLicenseDAO(), daoFactory.createLicenseThreatGroupDAO(),
            daoFactory.createLicenseThreatGroupLicenseDAO(),
            daoFactory.createLicenseOverrideDAO(), daoFactory.createSecurityVulnerabilityOverrideDAO(),
            daoFactory.createOwnerDAO(), daoFactory.createComponentLabelDAO(),
            daoFactory.createVulnerabilityCustomRemediationDAO(), daoFactory.createVulnerabilityCustomCweDAO(),
            daoFactory.createVulnerabilityCustomCvssVectorDAO(),
            daoFactory.createVulnerabilityCustomCvssSeverityDAO());
    Report.inject(
        componentLoaderFactory,
        thirdPartyComponentService,
        daoFactory.createLicenseDAO(),
        daoFactory.createHashComponentIdentifierDAO(),
        daoFactory.createSecurityVulnerabilityOverrideDAO(),
        daoFactory.createMultiLicenseDAO(),
        daoFactory.createLicenseOverrideDAO(),
        daoFactory.createLicenseThreatGroupDAO(),
        daoFactory.createApplicationDAO(),
        daoFactory.createInnerSourceComponentDAO(),
        proprietaryConfigService
    );
  }
}
