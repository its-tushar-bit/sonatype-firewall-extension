/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.looker;

import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.hds.DefaultHdsClient;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
class LookerService
{
  private final DefaultHdsClient hdsClient;

  private final CurrentUser currentUser;

  private static final Logger log = LoggerFactory.getLogger(LookerService.class);

  public static final String LOOKER_HDS_RESOURCE_PATH = "rest/looker/ssoEmbedUrl";

  @Inject
  public LookerService(final DefaultHdsClient hdsClient, final CurrentUser currentUser) {
    this.hdsClient = hdsClient;
    this.currentUser = currentUser;
  }

  @Authorize(permission = Permission.READ)
  SSOEmbedUrlDTO createSSOEmbedUrl(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId,
      LookerDashboardDTO lookerDashboard)
  {
    AuditData.get().setLookerDashboard(lookerDashboard);
    checkLookerIntegratedEnterpriseReportingEnabled();
    validateLookerDashboardValue(lookerDashboard);
    String requestId = UUID.randomUUID().toString().replace("-", "");
    return hdsClient.post(SSOEmbedUrlDTO.class, LOOKER_HDS_RESOURCE_PATH,
        buildRequest(requestId, lookerDashboard.dashboard));
  }

  private void validateLookerDashboardValue(LookerDashboardDTO lookerDashboard) {
    if (lookerDashboard == null || StringUtils.isBlank(lookerDashboard.dashboard)) {
      log.debug("Bad data in request dashboard is null or empty");
      throw new BadRequestException("Dashboard is null or empty");
    }
  }

  private LookerSSOEmbedUrlHdsRequest buildRequest(String requestId, String lookerDashboard) {
    log.debug("Submitting Looker SSOEmbedUrl request {} for dashboard {}", requestId, lookerDashboard);
    return new LookerSSOEmbedUrlHdsRequest(requestId,
        currentUser.getUserPrincipal().getDisplayName(),
        "",
        lookerDashboard);
  }

  private void checkLookerIntegratedEnterpriseReportingEnabled() {
    if (!SystemConfigurationPropertyFeature.LOOKER_INTEGRATED_ENTERPRISE_REPORTING.isEnabled()) {
      throw new NotAuthorizedException(SystemConfigurationPropertyFeature.LOOKER_INTEGRATED_ENTERPRISE_REPORTING.getId()
          + " feature is disabled.");
    }
  }
}
