/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import com.sonatype.insight.brain.api.v2.SystemConfigurationPropertyFeatureTestHelper;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtilsTestHelper;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderTestHelper;
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
import com.sonatype.insight.brain.model.policy.conditions.KevStatusConditionType;
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
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityEpssScoreConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityResearchConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.VulnerabilityGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.testing.AbstractBaseIntegrationTest;

/**
 * <p>
 * Helper to manage special static injection cases for tests.
 * </p>
 *
 * <p>
 * Background: In late 2023 the database layer received a large overhaul
 * (<a href="https://sonatype.atlassian.net/browse/CLM-26741">see CLM-26741</a>)
 * with the goal to make the database classes easier to use and extend for the future. Two of the primary goals were to
 * get the DAO classes managed by dependency injection, and to remove some of the heavy use of statics in the code
 * (statics are by their nature not easily extensible). As part of that effort some usage was still extensive enough
 * that a full refactor was out of scope, so the remaining static collaborators still need explicit bootstrap wiring.
 * <p>
 * <p>
 * At runtime the application bootstrap initializes those statics. For tests that do not exercise the full bootstrap,
 * the initialization must be handled manually. This is where this helper class is meant to assist. It is intended to
 * be used from the various base test classes to ensure consistency. Also note that these
 * injections need to be re-executed for each test as (due to the nature of statics) they may get overwritten from test
 * to test. For example, test order is random and it could execute an integration test (those extending
 * {@link AbstractBaseIntegrationTest}) followed by a component test (those extending {@link AbstractComponentTest},
 * followed by another integration test.
 */
public class StaticInjectionTestHelper
{
  public static void inject(final DAOFactory daoFactory) {
    initConditionTypes(daoFactory);
    initConditionValueTypes(daoFactory);
    ComponentDetailsLoaderTestHelper.inject(daoFactory);
    SystemConfigurationPropertyFeatureTestHelper.inject(daoFactory);
    ConfigurationUtilsTestHelper.inject(daoFactory);
  }

  private static void initConditionTypes(final DAOFactory daoFactory) {
    ConditionTypes.injectConditionTypes(
        new AgeInDaysConditionType(),
        new CoordinatesConditionType(),
        new ComponentFormatConditionType(),
        new PackageUrlConditionType(),
        new LabelConditionType(daoFactory.createLabelDAO()),
        new LicenseConditionType(daoFactory.createLicenseDAO()),
        new LicenseStatusConditionType(),
        new LicenseThreatGroupConditionType(daoFactory.createLicenseThreatGroupDAO(), daoFactory.createLicenseDAO()),
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
        new VulnerabilityGroupConditionType(daoFactory.createVulnerabilityGroupDAO()),
        new SecurityVulnerabilityCustomCVSSVectorStringConditionType(),
        new ComponentEndOfLifeConditionType(),
        new DerivativeAiModelConditionType(),
        new AiModelContentConditionType(),
        new SecurityVulnerabilityDetectionConditionType(),
        new KevStatusConditionType(),
        new SecurityVulnerabilityEpssScoreConditionType());
  }

  private static void initConditionValueTypes(final DAOFactory daoFactory) {
    ConditionValueTypes.injectConditionValueTypes(
        daoFactory.createComponentCategoryDAO(),
        daoFactory.createLicenseDAO(),
        daoFactory.createLicenseThreatGroupDAO(),
        daoFactory.createLabelDAO(),
        daoFactory.createVulnerabilityGroupDAO());
  }
}
