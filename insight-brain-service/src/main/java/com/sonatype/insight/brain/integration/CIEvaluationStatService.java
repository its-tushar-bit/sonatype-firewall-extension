/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.integration;

import java.util.Date;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.CIEvaluationStatDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.162
 */
@Named
class CIEvaluationStatService
{
  private static final Logger log = LoggerFactory.getLogger(CIEvaluationStatService.class);

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  public CIEvaluationStatService(PolicyEvaluationDAO policyEvaluationDAO, ApplicationDAO applicationDAO) {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationDAO = applicationDAO;
  }

  CIEvaluationStatDTO getDataForAppsWithoutCITriggeredEvaluations(final long sinceUtcTimestamp) {
    checkReadPermission(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);

    Date sinceUtcDate = new Date(sinceUtcTimestamp);
    log.debug("Getting data for non CI/CD plugin-integrated applications from evaluations on or after {}",
        sinceUtcDate);

    int numAppsWithCI = policyEvaluationDAO.getCountOfApplicationsWithCITriggeredEvaluations(sinceUtcDate);
    int numTotalApps = applicationDAO.getCount();
    int numAppsWithoutCI = numTotalApps - numAppsWithCI;

    return new CIEvaluationStatDTO(numAppsWithoutCI, numTotalApps);
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
  }
}
