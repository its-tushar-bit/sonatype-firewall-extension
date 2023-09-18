/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.looker;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.InternalServerErrorException;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
class LookerService
{
  private final HdsClient hdsClient;

  private final CurrentUser currentUser;

  private static final Logger log = LoggerFactory.getLogger(LookerService.class);

  public static final String LOOKER_SSO_EMBED_URL_PATH = "rest/looker/ssoEmbedUrl";

  public static final String LOOKER_CONFIG_PATH = "rest/looker/config";

  public static final String DEFAULT_CONFIG_CACHE_KEY = "default";

  private static final Duration MAX_AGE = Duration.ofDays(1);

  private final LoadingCache<String, LookerConfigDTO> lookerConfigCache;

  private final UserDAO userDAO;

  @Inject
  public LookerService(final HdsClient hdsClient, final CurrentUser currentUser, final UserDAO userDAO) {
    this.hdsClient = hdsClient;
    this.currentUser = currentUser;
    this.userDAO = userDAO;
    this.lookerConfigCache = CacheBuilder.newBuilder().expireAfterWrite(MAX_AGE).build(newLookerConfigCacheLoader());
  }

  //for testing only
  LookerService(
      final HdsClient hdsClient,
      final CurrentUser currentUser,
      final UserDAO userDAO,
      final LoadingCache<String, LookerConfigDTO> cache)
  {
    this.hdsClient = hdsClient;
    this.currentUser = currentUser;
    this.userDAO = userDAO;
    this.lookerConfigCache = cache;
  }

  private CacheLoader<String, LookerConfigDTO> newLookerConfigCacheLoader() {
    return new CacheLoader<String, LookerConfigDTO>()
    {
      @Override
      public LookerConfigDTO load(@NotNull final String key) {
        return hdsClient.get(LookerConfigDTO.class, LOOKER_CONFIG_PATH);
      }
    };
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
    return hdsClient.post(SSOEmbedUrlDTO.class, LOOKER_SSO_EMBED_URL_PATH,
        buildRequest(requestId, lookerDashboard.dashboard));
  }

  public LookerConfigDTO getLookerConfig() {
    try {
      return lookerConfigCache.get(DEFAULT_CONFIG_CACHE_KEY);
    }
    catch (ExecutionException e) {
      throw new InternalServerErrorException("unable to load looker configuration from sonatype data services", e);
    }
  }

  private void validateLookerDashboardValue(LookerDashboardDTO lookerDashboard) {
    if (lookerDashboard == null || StringUtils.isBlank(lookerDashboard.dashboard)) {
      log.debug("Bad data in request dashboard is null or empty");
      throw new BadRequestException("Dashboard is null or empty");
    }
  }

  private LookerSSOEmbedUrlHdsRequest buildRequest(String requestId, String lookerDashboard) {
    log.debug("Submitting Looker SSOEmbedUrl request {} for dashboard {}", requestId, lookerDashboard);
    User user = userDAO.getByUsernameNotNull(currentUser.getUserPrincipal().getUsername());
    return new LookerSSOEmbedUrlHdsRequest(requestId, user.getFirstName(), user.getLastName(), lookerDashboard);
  }

  private void checkLookerIntegratedEnterpriseReportingEnabled() {
    if (!SystemConfigurationPropertyFeature.LOOKER_INTEGRATED_ENTERPRISE_REPORTING.isEnabled()) {
      throw new NotAuthorizedException(SystemConfigurationPropertyFeature.LOOKER_INTEGRATED_ENTERPRISE_REPORTING.getId()
          + " feature is disabled.");
    }
  }
}
