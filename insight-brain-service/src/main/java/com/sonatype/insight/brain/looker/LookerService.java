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
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

  private final SamlUserDAO samlUserDAO;

  @Inject
  public LookerService(
      final HdsClient hdsClient,
      final CurrentUser currentUser,
      final UserDAO userDAO,
      final SamlUserDAO samlUserDAO)
  {
    this.hdsClient = hdsClient;
    this.currentUser = currentUser;
    this.userDAO = userDAO;
    this.samlUserDAO = samlUserDAO;
    this.lookerConfigCache = CacheBuilder.newBuilder().expireAfterWrite(MAX_AGE).build(newLookerConfigCacheLoader());
  }

  //for testing only
  LookerService(
      final HdsClient hdsClient,
      final CurrentUser currentUser,
      final UserDAO userDAO,
      final SamlUserDAO samlUserDAO,
      final LoadingCache<String, LookerConfigDTO> cache)
  {
    this.hdsClient = hdsClient;
    this.currentUser = currentUser;
    this.userDAO = userDAO;
    this.samlUserDAO = samlUserDAO;
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
    Pair<String, String> names = getUserFirstAndLastnames();
    return new LookerSSOEmbedUrlHdsRequest(requestId, names.getLeft(), names.getRight(), lookerDashboard);
  }

  private Pair<String, String> getUserFirstAndLastnames() {
    UserPrincipal principal = currentUser.getUserPrincipal();
    switch (principal.getRealmId()) {
      case InternalRealm.ID:
        User user = getInternalUser(principal.getUsername());
        return Pair.of(user.getFirstName(), user.getLastName());
      case SamlRealm.ID:
        SamlUser samlUser = getSamlUser(principal.getUsername());
        return Pair.of(samlUser.getFirstName(), samlUser.getLastName());
      default:
        return Pair.of(principal.getDisplayName(), "");
    }
  }

  private SamlUser getSamlUser(final String username) {
    return samlUserDAO.getByUsernameNotNull(username);
  }

  private User getInternalUser(final String username) {
    return userDAO.getByUsernameNotNull(username);
  }

  private void checkLookerIntegratedEnterpriseReportingEnabled() {
    if (!SystemConfigurationPropertyFeature.LOOKER_INTEGRATED_ENTERPRISE_REPORTING.isEnabled()) {
      throw new NotAuthorizedException(SystemConfigurationPropertyFeature.LOOKER_INTEGRATED_ENTERPRISE_REPORTING.getId()
          + " feature is disabled.");
    }
  }
}
