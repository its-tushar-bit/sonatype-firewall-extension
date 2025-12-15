/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.google.inject.AbstractModule;

import com.sonatype.insight.brain.model.policy.ConditionValidator;
import com.sonatype.insight.brain.model.policy.ConstraintValidator;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.NotificationsValidator;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.UserNotificationValidator;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotificationValidator;
import com.sonatype.insight.brain.policy.ActivePolicyViolationsWithActionFailService;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.PathForwardInspector;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.policy.PolicyMonitoringTask;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.componentanalysis.ComponentAnalysisService;
import com.sonatype.insight.brain.policy.evaluator.PersistedPolicyEvaluationPollingResultCleaner;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertEmailResolver;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertEmailer;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertScmNotifier;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultUtils;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitor;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler;
import com.sonatype.insight.brain.policy.evaluator.PolicyNotificationUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.ReportComponentService;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.violation.PolicyViolationConstraintFactsJsonAsyncDbMigration;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.violation.RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.AiModelContentConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentEndOfLifeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentFormatConditionType;
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
import com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeInspector;
import com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeScheduler;
import com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeTask;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilityReachabilityStatusService;
import com.sonatype.insight.brain.model.policy.PolicyValidator;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertSourceCodeOrganizer;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.utils.FirewallForContainerImagesHelper;

/**
 * Guice module providing explicit bindings for Policy components.
 * This replaces Sisu's automatic @Named component discovery.
 */
public class PolicyModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    // Request static injection for ConditionTypes to initialize condition type registry
    // Sisu called static @Inject methods automatically; pure Guice requires explicit request
    binder().requestStaticInjection(ConditionTypes.class);

    // Bind all 36 ConditionType classes
    bind(AgeInDaysConditionType.class);
    bind(AiModelContentConditionType.class);
    bind(ComponentCategoryConditionType.class);
    bind(ComponentEndOfLifeConditionType.class);
    bind(ComponentFormatConditionType.class);
    bind(CoordinatesConditionType.class);
    bind(DataSourceConditionType.class);
    bind(DependencyTypeConditionType.class);
    bind(DeprecatedSecurityVulnerabilityConditionType.class);
    bind(DerivativeAiModelConditionType.class);
    bind(HygieneRatingConditionType.class);
    bind(IacControlConditionType.class);
    bind(IdentificationSourceConditionType.class);
    bind(IntegrityRatingConditionType.class);
    bind(KevStatusConditionType.class);
    bind(LabelConditionType.class);
    bind(LicenseConditionType.class);
    bind(LicenseStatusConditionType.class);
    bind(LicenseThreatGroupConditionType.class);
    bind(LicenseThreatGroupLevelConditionType.class);
    bind(MatchStateConditionType.class);
    bind(PackageUrlConditionType.class);
    bind(ProprietaryConditionType.class);
    bind(ProprietaryNameConflictConditionType.class);
    bind(RelativePopularityConditionType.class);
    bind(SecurityVulnerabilityCategoryConditionType.class);
    bind(SecurityVulnerabilityCustomCVSSVectorStringConditionType.class);
    bind(SecurityVulnerabilityCustomRemediationConditionType.class);
    bind(SecurityVulnerabilityCweConditionType.class);
    bind(SecurityVulnerabilityDetectionConditionType.class);
    bind(SecurityVulnerabilityEpssScoreConditionType.class);
    bind(SecurityVulnerabilityResearchConditionType.class);
    bind(SecurityVulnerabilitySeverityConditionType.class);
    bind(SecurityVulnerabilitySourceConditionType.class);
    bind(SecurityVulnerabilityStatusConditionType.class);
    bind(VulnerabilityGroupConditionType.class);

    // Additional policy bindings for requireExplicitBindings
    bind(WaivedComponentUpgradeInspector.class);
    bind(ApiVulnerabilityReachabilityStatusService.class);
    bind(PolicyValidator.class);
    bind(PolicyAlertSourceCodeOrganizer.class);
    bind(ComponentPolicyEvaluator.class);
    bind(FirewallForContainerImagesHelper.class);
    // Note: TelemetryUtils and ComponentLoaderFactory are already bound in CoreServiceModule

    bind(ActivePolicyViolationsWithActionFailService.class);
    bind(ComponentAnalysisService.class);
    bind(LegacyViolationService.class);
    bind(PathForwardInspector.class);
    bind(PersistedPolicyEvaluationPollingResultCleaner.class);
    bind(PolicyAlertEmailResolver.class);
    bind(PolicyAlertEmailer.class);
    bind(PolicyAlertNotifier.class);
    bind(PolicyAlertScmNotifier.class);
    bind(PolicyAlertUtil.class);
    bind(PolicyEvaluateService.class);
    bind(PolicyEvaluationPollingResultUtils.class);
    bind(PolicyEvaluationUtil.class);
    bind(PolicyImportExport.class);
    bind(PolicyMonitor.class);
    bind(PolicyMonitorScheduler.class);
    bind(PolicyMonitoringTask.class);
    bind(PolicyNotificationUtil.class);
    bind(PolicyViolationConstraintFactsJsonAsyncDbMigration.class);
    bind(PolicyViolationLoader.class);
    bind(PolicyViolationLoggerFactory.class);
    bind(ReportComponentService.class);
    bind(RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration.class);
    bind(ScanPolicyEvaluator.class);
    bind(StageTypeService.class);
    bind(WaivedComponentUpgradeScheduler.class);
    bind(WaivedComponentUpgradeTask.class);

    // Additional bindings for requireExplicitBindings
    bind(ConditionValidator.class);
    bind(ConstraintValidator.class);
    bind(JiraNotificationValidator.class);
    bind(NotificationsValidator.class);
    bind(RoleNotificationValidator.class);
    bind(UserNotificationValidator.class);
    bind(WebhookNotificationValidator.class);
  }
}
