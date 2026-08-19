/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;

/**
 * @since 1.105
 *
 *        For mtiq we only want to run this listener per tenant. Global tenant is not aware of any firewall feature.
 *        Moreover, if the global were to be included the value would be configured for global on startup and due to the
 *        fallback method employed by the <code>systemConfigurationPropertyDAO.getByName</code> would then prevent
 *        tenants from
 *        getting initialised correctly.
 */
@Named
@Singleton
public class FirewallReleaseIntegrityLicenseListener
    implements ProductLicenseListener, TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(FirewallReleaseIntegrityLicenseListener.class);

  private final ProductLicense productLicense;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final OwnerDAO ownerDAO;

  private final AuditRecorder auditRecorder;

  public boolean disableForTesting;

  @Inject
  public FirewallReleaseIntegrityLicenseListener(
      final ProductLicense productLicense,
      final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO,
      final PolicyMonitoringDAO policyMonitoringDAO,
      final OwnerDAO ownerDAO,
      final AuditRecorder auditRecorder)
  {
    this.productLicense = productLicense;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.autoUnquarantinePolicyConditionTypeDAO = autoUnquarantinePolicyConditionTypeDAO;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.ownerDAO = ownerDAO;
    this.auditRecorder = auditRecorder;
  }

  @Override
  public void productLicenseChanged() {
    if (disableForTesting || !productLicense.hasFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE) ||
        !productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY))
    {
      return;
    }

    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      tx.begin();
      SystemConfigurationProperty existingLicenseProperty = systemConfigurationPropertyDAO
          .getByName(tx, SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED);
      if (existingLicenseProperty == null || !Boolean.parseBoolean(existingLicenseProperty.getValue())) {
        enablePolicyMonitoringForAllRepositories(tx);
        addIntegrityRatingConditionTypeForMonitoring(tx);

        setConfigurationProperty(tx, existingLicenseProperty != null);
      }
      tx.commit();
    }
  }

  private void addIntegrityRatingConditionTypeForMonitoring(TransactionContext tx) {
    if (null == autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID)) {
      autoUnquarantinePolicyConditionTypeDAO
          .insert(tx, new AutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID));
    }
  }

  private void setConfigurationProperty(TransactionContext tx, boolean propertyExists) {
    SystemConfigurationProperty property =
        new SystemConfigurationProperty(SystemConfigurationProperty.FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED,
            String.valueOf(true));
    if (propertyExists) {
      systemConfigurationPropertyDAO.update(tx, property);
    }
    else {
      systemConfigurationPropertyDAO.insert(tx, property);
    }
  }

  private void enablePolicyMonitoringForAllRepositories(TransactionContext tx) {
    if (policyMonitoringDAO.getByOwnerId(tx, REPOSITORY_CONTAINER_ID).isEmpty()) {
      log.info("Enabling policy monitoring for all repositories");
      PolicyMonitoring policyMonitoring = new PolicyMonitoring();
      policyMonitoring.setOwnerId(REPOSITORY_CONTAINER_ID);
      policyMonitoring.setStageTypeId(StageTypes.PROXY.getId());
      try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING)) {
        policyMonitoringDAO.insert(tx, policyMonitoring);
        AuditData.get().setOwner(ownerDAO.getById(REPOSITORY_CONTAINER_ID)).setStageId(StageTypes.PROXY.getId());
      }
    }
  }
}
