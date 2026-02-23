/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.ApplicationPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class LegacyViolationService
{
  private static final Logger log = LoggerFactory.getLogger(LegacyViolationService.class);

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final PolicyDAO policyDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final ProductLicense productLicense;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final ClusterLockManager clusterLockManager;

  @Inject
  public LegacyViolationService(
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      PolicyDAO policyDAO,
      PolicyViolationDAO policyViolationDAO,
      ProductLicense productLicense,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      ClusterLockManager clusterLockManager)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.policyDAO = policyDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.productLicense = productLicense;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.clusterLockManager = clusterLockManager;
  }

  private void validateLegacyViolationIsLicensed() {
    if (!productLicense.hasFeature(LicensedFeature.POLICY_GRANDFATHERING)) {
      log.debug("Legacy violations are not supported by the current license.");
      throw new InvalidLicenseException();
    }
  }

  @Authorize(permission = Permission.WRITE)
  public void revokeLegacyViolationStatus(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    validateLegacyViolationIsLicensed();
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    log.info("Revoking legacy violation status for application '{}' (ID: {}).", app.getName(), app.getId());

    Date now = new Date();
    ApplicationPolicyViolationLogger policyViolationLogger = policyViolationLoggerFactory.newLogger(now, app);

    try (ClusterLock clusterLock = clusterLockManager.createForPolicyViolations(app);
         TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      clusterLock.lock();
      tx.begin();
      List<PolicyViolation> legacyViolations =
          policyViolationDAO.getUnfixedLegacyViolationByApplicationId(tx, app.getId());
      policyViolationDAO.loadConstraintFacts(legacyViolations);
      for (PolicyViolation legacyViolation : legacyViolations) {
        legacyViolation.setLegacyViolationTime(null);
        policyViolationDAO.update(tx, legacyViolation);

        policyViolationLogger.add(PolicyViolationLogEvent.UNGRANDFATHER, legacyViolation);
        policyViolationLogger.add(PolicyViolationLogEvent.REVOKE_LEGACY_STATUS, legacyViolation);
      }

      tx.commit();

      policyViolationLogger.log();
      auditChangedPolicyViolationCount(legacyViolations.size());
    }
  }

  @Authorize(permission = Permission.WRITE)
  public void grantLegacyViolationStatus(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    validateLegacyViolationIsLicensed();
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    if (!isLegacyViolationEnabled(app.getId(), null)) {
      throw new BadRequestException(
          "Legacy violations are not enabled for application '" + app.getName() + "'.");
    }

    log.info("Granting legacy status for policy violations at application '{}' (ID: {}).", app.getName(), app.getId());

    Date now = new Date();
    ApplicationPolicyViolationLogger policyViolationLogger = policyViolationLoggerFactory.newLogger(now, app);

    try (ClusterLock clusterLock = clusterLockManager.createForPolicyViolations(app);
         TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      clusterLock.lock();
      tx.begin();
      List<PolicyViolation> policyViolations = policyViolationDAO.getUnfixedByApplicationId(tx, app.getId());
      policyViolationDAO.loadConstraintFacts(policyViolations);
      int changedPolicyViolationCount = 0;
      for (PolicyViolation policyViolation : policyViolations) {
        if (!policyViolation.isLegacyViolation()) {
          Policy policy = policyDAO.getById(tx, policyViolation.getPolicyId());
          if (policy == null || policy.isLegacyViolationAllowed()) {
            // Firewall (proxy stage) violations should not be marked as legacy
            if (!Stage.ID_PROXY.equals(policyViolation.getStageTypeId())) {
              policyViolation.setLegacyViolationTime(now);
              policyViolationDAO.update(tx, policyViolation);
              policyViolationLogger.add(PolicyViolationLogEvent.GRANDFATHER, policyViolation);
              policyViolationLogger.add(PolicyViolationLogEvent.GRANT_LEGACY_STATUS, policyViolation);

              changedPolicyViolationCount++;
            }
          }
        }
      }

      tx.commit();

      policyViolationLogger.log();
      auditChangedPolicyViolationCount(changedPolicyViolationCount);
    }
  }

  @Authorize(permission = Permission.READ)
  public LegacyViolationStatusDTO getLegacyViolationsStatus(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.allowChange = true;

    String parentOrgId;
    switch (ownerType) {
      case APPLICATION:
        Application app = applicationDAO.getByPublicIdNotNull(ownerId);
        legacyViolationStatusDTO.enabled = app.isLegacyViolationEnabled();
        parentOrgId = app.getOrganizationId();
        break;
      case ORGANIZATION:
        Organization org = organizationDAO.getByIdNotNull(ownerId);
        legacyViolationStatusDTO.enabled = org.isLegacyViolationEnabled();
        legacyViolationStatusDTO.allowOverride = org.isAllowLegacyViolationOverride();
        parentOrgId = org.getParentOrganizationId();
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }

    while (parentOrgId != null) {
      Organization org = organizationDAO.getByIdNotNull(parentOrgId);

      if (legacyViolationStatusDTO.enabledInParent == null) {
        legacyViolationStatusDTO.enabledInParent = org.isLegacyViolationEnabled();
      }

      if (!org.isAllowLegacyViolationOverride()) {
        legacyViolationStatusDTO.enabled = org.isLegacyViolationEnabled();
        legacyViolationStatusDTO.inheritedFromOrganizationName = org.getName();
        legacyViolationStatusDTO.allowChange = false;
      }
      else if (legacyViolationStatusDTO.enabled == null) {
        legacyViolationStatusDTO.enabled = org.isLegacyViolationEnabled();
        legacyViolationStatusDTO.inheritedFromOrganizationName = org.getName();
      }

      parentOrgId = org.getParentOrganizationId();
    }

    return legacyViolationStatusDTO;
  }

  @Authorize(permission = Permission.WRITE)
  public LegacyViolationStatusDTO setLegacyViolationStatus(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      LegacyViolationStatusDTO legacyViolationStatusDTO)
  {
    validateLegacyViolationIsLicensed();

    switch (ownerType) {
      case APPLICATION:
        Application app = applicationDAO.getByPublicIdNotNull(ownerId);
        app.setLegacyViolationEnabled(legacyViolationStatusDTO.enabled);
        applicationDAO.update(app);
        break;
      case ORGANIZATION:
        Organization org = organizationDAO.getByIdNotNull(ownerId);
        org.setLegacyViolationEnabled(legacyViolationStatusDTO.enabled);
        org.setAllowLegacyViolationOverride(legacyViolationStatusDTO.allowOverride);
        organizationDAO.update(org);
        AuditData.get()
            .setData("overrideByChild", legacyViolationStatusDTO.allowOverride ? "allow" : "disallow");
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
    AuditData.get().setData("localSetting",
        legacyViolationStatusDTO.enabled == null ? "inherit" : legacyViolationStatusDTO.enabled ? "enable" : "disable");

    return legacyViolationStatusDTO;
  }

  private void auditChangedPolicyViolationCount(int changedPolicyViolationCount) {
    AuditData.get().setData("changedPolicyViolationCount", changedPolicyViolationCount);
  }

  public boolean isLegacyViolationEnabled(String appId, String stageTypeId) {
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      return isLegacyViolationEnabled(tx, appId, stageTypeId);
    }
  }

  public boolean isLegacyViolationEnabled(TransactionContext tx, String appId, String stageTypeId) {
    Application app = applicationDAO.getById(tx, appId);
    Boolean enabled = app.isLegacyViolationEnabled();
    String parentOrgId = app.getOrganizationId();
    while (parentOrgId != null) {
      Organization org = organizationDAO.getById(tx, parentOrgId);

      if (!org.isAllowLegacyViolationOverride()) {
        enabled = org.isLegacyViolationEnabled();
      }
      else if (enabled == null) {
        enabled = org.isLegacyViolationEnabled();
      }

      parentOrgId = org.getParentOrganizationId();
    }

    if (enabled == null ||  (stageTypeId != null && stageTypeId.equals(Stage.ID_COMPLIANCE))) {
      enabled = false;
    }

    return enabled;
  }

  public static class LegacyViolationStatusDTO
  {
    /**
     * Whether legacy violations are enabled for this org/app. If null, then legacy status was never set for this
     * org/app.
     */
    public Boolean enabled;

    /**
     * Whether legacy violations are enabled for the parent org. If null, then no parent set this value.
     */
    public Boolean enabledInParent;

    /**
     * The name of the organization the legacy status is inherited from or null if it isn't inherited.
     */
    public String inheritedFromOrganizationName;

    /**
     * Whether children (orgs and apps) are allowed to override the legacy status.
     */
    public boolean allowOverride;

    /**
     * Whether the legacy status can be changed for this org/app (a parent org may not allow it to be
     * overridden).
     */
    public boolean allowChange;
  }
}
