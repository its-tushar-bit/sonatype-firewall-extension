/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.IntegrityRating;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.105
 */
@Named
@Singleton
public class FirewallReleaseIntegrityLicenseListener
    implements ProductLicenseListener
{
  private static final Logger log = LoggerFactory.getLogger(FirewallReleaseIntegrityLicenseListener.class);

  public static final String POLICY_NAME = "Integrity-Rating";

  private final InsightConfig insightConfig;

  private final ProductLicense productLicense;

  private final AuditRecorder auditRecorder;

  @Inject
  public FirewallReleaseIntegrityLicenseListener(
      InsightConfig insightConfig,
      ProductLicense productLicense,
      AuditRecorder auditRecorder)
  {
    this.insightConfig = insightConfig;
    this.productLicense = productLicense;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public void productLicenseChanged() {
    if (!insightConfig.isExperimentalFeatureEnabled(Feature.FIREWALL_AUTO_UNQUARANTINE)) {
      return;
    }

    if (!productLicense.hasFeature(LicensedFeature.FIREWALL) ||
        !productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY)) {
      return;
    }

    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      tx.begin();
      SystemConfigurationProperty existingAdpLicenseProperty = systemConfigurationPropertyDAO
          .getByName(tx, SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED);
      if (existingAdpLicenseProperty == null) {
        installIntegrityRatingPolicy(tx);

        SystemConfigurationProperty newProperty =
            new SystemConfigurationProperty(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED,
                String.valueOf(true));
        systemConfigurationPropertyDAO.insert(tx, newProperty);
      }
      else if (!Boolean.parseBoolean(existingAdpLicenseProperty.getValue())) {
        installIntegrityRatingPolicy(tx);

        existingAdpLicenseProperty.setValue(String.valueOf(true));
        systemConfigurationPropertyDAO.update(tx, existingAdpLicenseProperty);
      }
      tx.commit();
    }
  }

  private void installIntegrityRatingPolicy(TransactionContext tx) {
    String policyName = findFirstUnusedPolicyName(POLICY_NAME);

    log.info("Installing '{}' policy for Firewall + ADP license", policyName);
    Constraint pendingConstraint = new Constraint(null, "Pending integrity rating", LogicalOperator.OR);
    pendingConstraint.addCondition(new Condition(IntegrityRatingConditionType.ID, "is", IntegrityRating.PENDING
        .getId()));
    Constraint suspiciousConstraint = new Constraint(null, "Suspicious integrity rating", LogicalOperator.OR);
    suspiciousConstraint.addCondition(new Condition(IntegrityRatingConditionType.ID, "is", IntegrityRating.SUSPICIOUS
        .getId()));
    Policy policy = new Policy();
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    policy.setName(policyName);
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policy.addConstraint(pendingConstraint);
    policy.addConstraint(suspiciousConstraint);
    policy.setThreatLevel(9);

    try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.CREATE_POLICY)) {
      new PolicyDAO().insert(tx, policy);
      AuditData.get().setOwner(new OwnerDAO().getById(Organization.ROOT_ORGANIZATION_ID)).setPolicyWithDetails(policy);
    }
  }

  private String findFirstUnusedPolicyName(String basePolicyName) {
    PolicyDAO policyDAO = new PolicyDAO();
    int i = 0;
    String policyName = basePolicyName;
    while (!policyDAO.getByName(policyName).isEmpty()) {
      policyName = basePolicyName + "-" + ++i;
    }

    return policyName;
  }
}
