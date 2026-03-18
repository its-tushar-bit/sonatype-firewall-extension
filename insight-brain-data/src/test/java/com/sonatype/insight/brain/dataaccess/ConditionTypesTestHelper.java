/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.AiModelContentConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentEndOfLifeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentFormatConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DependencyTypeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DeprecatedSecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DerivativeAiModelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IacControlConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IdentificationSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.PackageUrlConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCustomCVSSVectorStringConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCustomRemediationConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCweConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityDetectionConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityResearchConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.VulnerabilityGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.KevStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityEpssScoreConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;

/**
 * {@link ConditionTypes} and {@link ConditionValueTypes} use static references. At runtime Guice
 * <a href="https://github.com/google/guice/wiki/Injections#static-injections">static-injection</a> is used to
 * populate the references. At test time we use this helper class.
 */
public class ConditionTypesTestHelper
{
  public static void initConditionTypes(final DAOFactory daoFactory) {
    ConditionTypes.injectConditionTypes(
        new AgeInDaysConditionType(),
        new CoordinatesConditionType(),
        new ComponentFormatConditionType(),
        new PackageUrlConditionType(),
        new LabelConditionType(daoFactory.createLabelDAO()),
        new LicenseConditionType(daoFactory.createLicenseDAO()),
        new LicenseStatusConditionType(),
        new LicenseThreatGroupConditionType(daoFactory.createLicenseThreatGroupDAO(), daoFactory.createLicenseDAO(),
            daoFactory.createOwnerDAO()),
        new LicenseThreatGroupLevelConditionType(),
        new RelativePopularityConditionType(),
        new MatchStateConditionType(),
        new DeprecatedSecurityVulnerabilityConditionType(),
        new SecurityVulnerabilitySeverityConditionType(),
        new SecurityVulnerabilityStatusConditionType(),
        new SecurityVulnerabilitySourceConditionType(daoFactory.createSystemConfigurationPropertyDAO()),
        new SecurityVulnerabilityResearchConditionType(),
        new ProprietaryConditionType(),
        new ProprietaryNameConflictConditionType(daoFactory.createRepositoryDAO()),
        new IdentificationSourceConditionType(),
        new ComponentCategoryConditionType(daoFactory.createComponentCategoryDAO()),
        new HygieneRatingConditionType(),
        new IntegrityRatingConditionType(),
        new DataSourceConditionType(),
        new DependencyTypeConditionType(),
        new SecurityVulnerabilityCategoryConditionType(),
        new SecurityVulnerabilityCweConditionType(),
        new SecurityVulnerabilityCustomRemediationConditionType(),
        new IacControlConditionType(daoFactory.createThirdPartyVulnerabilityDAO()),
        new VulnerabilityGroupConditionType(daoFactory.createVulnerabilityGroupDAO(), daoFactory.createOwnerDAO()),
        new SecurityVulnerabilityCustomCVSSVectorStringConditionType(),
        new ComponentEndOfLifeConditionType(), //
        new DerivativeAiModelConditionType(),
        new AiModelContentConditionType(),
        new SecurityVulnerabilityDetectionConditionType(),
        new KevStatusConditionType(),
        new SecurityVulnerabilityEpssScoreConditionType());
  }

  public static void initConditionValueTypes(final DAOFactory daoFactory) {
    ConditionValueTypes.injectConditionValueTypes(
        daoFactory.createComponentCategoryDAO(),
        daoFactory.createLicenseDAO(),
        daoFactory.createOwnerDAO(),
        daoFactory.createLicenseThreatGroupDAO(),
        daoFactory.createLabelDAO(),
        daoFactory.createVulnerabilityGroupDAO());
  }
}
