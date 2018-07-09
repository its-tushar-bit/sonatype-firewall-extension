/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PolicyViolationGrandfatheringService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationGrandfatheringService.class);

  private final ApplicationDAO applicationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyViolationPersistenceLocks policyViolationPersistenceLocks;

  @Inject
  public PolicyViolationGrandfatheringService(ApplicationDAO applicationDAO,
                                              PolicyViolationDAO policyViolationDAO,
                                              PolicyViolationPersistenceLocks policyViolationPersistenceLocks)
  {
    this.applicationDAO = applicationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyViolationPersistenceLocks = policyViolationPersistenceLocks;
  }

  @Authorize(permission = Permission.WRITE)
  public void revokeGrandfathering(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId) {
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    log.info("Revoking grandfathered policy violations for application '{}' (ID: {}).", app.getName(), app.getId());

    Object lock = policyViolationPersistenceLocks.getLock(app.getId());
    synchronized (lock) {
      try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
        tx.begin();

        List<PolicyViolation> grandfatheredPolicyViolations = policyViolationDAO
            .getUnfixedGrandfatheredByApplicationId(tx, app.getId());
        for (PolicyViolation grandfatheredPolicyViolation : grandfatheredPolicyViolations) {
          grandfatheredPolicyViolation.setGrandfatherTime(null);
          policyViolationDAO.update(tx, grandfatheredPolicyViolation);
        }

        tx.commit();
      }
    }
  }
}
