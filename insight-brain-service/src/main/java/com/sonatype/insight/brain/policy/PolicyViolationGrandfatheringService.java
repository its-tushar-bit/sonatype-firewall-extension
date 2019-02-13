/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.ApplicationPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PolicyViolationGrandfatheringService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationGrandfatheringService.class);

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final PolicyDAO policyDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyViolationPersistenceLocks policyViolationPersistenceLocks;

  private final CLMLicenseManager clmLicenseManager;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  public PolicyViolationGrandfatheringService(ApplicationDAO applicationDAO,
                                              OrganizationDAO organizationDAO,
                                              PolicyDAO policyDAO,
                                              PolicyViolationDAO policyViolationDAO,
                                              PolicyViolationPersistenceLocks policyViolationPersistenceLocks,
                                              CLMLicenseManager clmLicenseManager,
                                              PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.policyDAO = policyDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyViolationPersistenceLocks = policyViolationPersistenceLocks;
    this.clmLicenseManager = clmLicenseManager;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
  }

  private void validateGrandfatheringIsLicensed() {
    if (!clmLicenseManager.hasFeature(Feature.POLICY_GRANDFATHERING)) {
      log.debug("Policy violation grandfathering is not supported by the current license.");
      throw new InvalidLicenseException();
    }
  }

  @Authorize(permission = Permission.WRITE)
  public void revokeGrandfathering(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId) {
    validateGrandfatheringIsLicensed();
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    log.info("Revoking grandfathered policy violations for application '{}' (ID: {}).", app.getName(), app.getId());

    Object lock = policyViolationPersistenceLocks.getLock(app.getId());
    synchronized (lock) {
      Date now = new Date();
      ApplicationPolicyViolationLogger policyViolationLogger = policyViolationLoggerFactory.newLogger(now, app);

      try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
        tx.begin();

        List<PolicyViolation> grandfatheredPolicyViolations = policyViolationDAO
            .getUnfixedGrandfatheredByApplicationId(tx, app.getId());
        for (PolicyViolation grandfatheredPolicyViolation : grandfatheredPolicyViolations) {
          grandfatheredPolicyViolation.setGrandfatherTime(null);
          policyViolationDAO.update(tx, grandfatheredPolicyViolation);

          policyViolationLogger.add(PolicyViolationLogEvent.UNGRANDFATHER, grandfatheredPolicyViolation);
        }

        tx.commit();

        policyViolationLogger.log();
        auditChangedPolicyViolationCount(grandfatheredPolicyViolations.size());
      }
    }
  }

  @Authorize(permission = Permission.WRITE)
  public void grandfather(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId) {
    validateGrandfatheringIsLicensed();
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    if (!isPolicyViolationGrandfatheringEnabled(app.getId())) {
      throw new BadRequestException(
          "Policy violation grandfathering is not enabled for application '" + app.getName() + "'.");
    }

    log.info("Grandfathering policy violations for application '{}' (ID: {}).", app.getName(), app.getId());

    Object lock = policyViolationPersistenceLocks.getLock(app.getId());
    synchronized (lock) {
      Date now = new Date();
      ApplicationPolicyViolationLogger policyViolationLogger = policyViolationLoggerFactory.newLogger(now, app);

      try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
        tx.begin();

        List<PolicyViolation> policyViolations = policyViolationDAO.getUnfixedByApplicationId(tx, app.getId());
        int changedPolicyViolationCount = 0;
        for (PolicyViolation policyViolation : policyViolations) {
          if (!policyViolation.isGrandfathered()) {
            Policy policy = policyDAO.getById(tx, policyViolation.getPolicyId());
            if (policy == null || policy.isPolicyViolationGrandfatheringAllowed()) {
              policyViolation.setGrandfatherTime(now);
              policyViolationDAO.update(tx, policyViolation);

              policyViolationLogger.add(PolicyViolationLogEvent.GRANDFATHER, policyViolation);

              changedPolicyViolationCount++;
            }
          }
        }

        tx.commit();

        policyViolationLogger.log();
        auditChangedPolicyViolationCount(changedPolicyViolationCount);
      }
    }
  }

  @Authorize(permission = Permission.READ)
  public PolicyViolationGrandfatheringDTO getGrandfathering(@AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
                                                            @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = new PolicyViolationGrandfatheringDTO();
    policyViolationGrandfatheringDTO.allowChange = true;

    String parentOrgId;
    switch (ownerType) {
      case APPLICATION:
        Application app = applicationDAO.getByPublicIdNotNull(ownerId);
        policyViolationGrandfatheringDTO.enabled = app.isPolicyViolationGrandfatheringEnabled();
        parentOrgId = app.getOrganizationId();
        break;
      case ORGANIZATION:
        Organization org = organizationDAO.getByIdNotNull(ownerId);
        policyViolationGrandfatheringDTO.enabled = org.isPolicyViolationGrandfatheringEnabled();
        policyViolationGrandfatheringDTO.allowOverride = org.isAllowPolicyViolationGrandfatheringOverride();
        parentOrgId = org.getParentOrganizationId();
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }

    while (parentOrgId != null) {
      Organization org = organizationDAO.getByIdNotNull(parentOrgId);

      if (!org.isAllowPolicyViolationGrandfatheringOverride()) {
        policyViolationGrandfatheringDTO.enabled = org.isPolicyViolationGrandfatheringEnabled();
        policyViolationGrandfatheringDTO.inheritedFromOrganizationName = org.getName();
        policyViolationGrandfatheringDTO.allowChange = false;
      }
      else if (policyViolationGrandfatheringDTO.enabled == null) {
        policyViolationGrandfatheringDTO.enabled = org.isPolicyViolationGrandfatheringEnabled();
        policyViolationGrandfatheringDTO.inheritedFromOrganizationName = org.getName();
      }

      parentOrgId = org.getParentOrganizationId();
    }

    return policyViolationGrandfatheringDTO;
  }

  @Authorize(permission = Permission.WRITE)
  @SuppressWarnings("checkstyle:LineLength")
  public PolicyViolationGrandfatheringDTO setGrandfathering(@AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
                                                            @AuthzContext(AuthzContext.Key.ID) String ownerId,
                                                            PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO)
  {
    validateGrandfatheringIsLicensed();

    switch (ownerType) {
      case APPLICATION:
        Application app = applicationDAO.getByPublicIdNotNull(ownerId);
        app.setPolicyViolationGrandfatheringEnabled(policyViolationGrandfatheringDTO.enabled);
        applicationDAO.update(app);
        break;
      case ORGANIZATION:
        Organization org = organizationDAO.getByIdNotNull(ownerId);
        org.setPolicyViolationGrandfatheringEnabled(policyViolationGrandfatheringDTO.enabled);
        org.setAllowPolicyViolationGrandfatheringOverride(policyViolationGrandfatheringDTO.allowOverride);
        organizationDAO.update(org);
        AuditData.get()
            .setData("overrideByChild", policyViolationGrandfatheringDTO.allowOverride ? "allow" : "disallow");
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
    AuditData.get().setData("localSetting", policyViolationGrandfatheringDTO.enabled ==
        null ? "inherit" : policyViolationGrandfatheringDTO.enabled ? "enable" : "disable");

    return policyViolationGrandfatheringDTO;
  }

  private void auditChangedPolicyViolationCount(int changedPolicyViolationCount) {
    AuditData.get().setData("changedPolicyViolationCount", changedPolicyViolationCount);
  }

  public boolean isPolicyViolationGrandfatheringEnabled(String appId) {
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      return isPolicyViolationGrandfatheringEnabled(tx, appId);
    }
  }

  public boolean isPolicyViolationGrandfatheringEnabled(TransactionContext tx, String appId) {
    Application app = applicationDAO.getById(tx, appId);
    Boolean enabled = app.isPolicyViolationGrandfatheringEnabled();

    String parentOrgId = app.getOrganizationId();
    while (parentOrgId != null) {
      Organization org = organizationDAO.getById(tx, parentOrgId);

      if (!org.isAllowPolicyViolationGrandfatheringOverride()) {
        enabled = org.isPolicyViolationGrandfatheringEnabled();
      }
      else if (enabled == null) {
        enabled = org.isPolicyViolationGrandfatheringEnabled();
      }

      parentOrgId = org.getParentOrganizationId();
    }

    if (enabled == null) {
      enabled = false;
    }

    return enabled;
  }

  public static class PolicyViolationGrandfatheringDTO
  {
    /**
     * Whether grandfathering is enabled for this org/app. If null, then grandfathering was never set for this org/app.
     */
    public Boolean enabled;

    /**
     * The name of the organization the grandfathering status is inherited from or null if it isn't inherited.
     */
    public String inheritedFromOrganizationName;

    /**
     * Whether children (orgs and apps) are allowed to override the grandfathering status.
     */
    public boolean allowOverride;

    /**
     * Whether the grandfathering status can be changed for this org/app (a parent org may not allow it to be
     * overridden).
     */
    public boolean allowChange;
  }
}
