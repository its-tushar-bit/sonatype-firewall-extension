/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authz.UnauthenticatedException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class EnterpriseReportingService
{
  private final HdsClient hdsClient;

  private final CurrentUser currentUser;

  private static final Logger log = LoggerFactory.getLogger(EnterpriseReportingService.class);

  public static final String ENTERPRISE_REPORTING_BASE_PATH = "rest/enterpriseReporting";

  public static final String ENTERPRISE_REPORTING_SSO_EMBED_URL_PATH = ENTERPRISE_REPORTING_BASE_PATH + "/ssoEmbedUrl";

  public static final String ENTERPRISE_REPORTING_CONFIG_PATH = ENTERPRISE_REPORTING_BASE_PATH  + "/config";

  public static final String ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH =
      ENTERPRISE_REPORTING_BASE_PATH + "/dashboards";

  public static final String ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH = ENTERPRISE_REPORTING_BASE_PATH + "/icons";

  public static final String DEFAULT_CONFIG_CACHE_KEY = "default";

  public static final String DEFAULT_DASHBOARD_CACHE_KEY = "default";

  private static final Duration MAX_AGE = Duration.ofDays(1);

  private final LoadingCache<String, EnterpriseReportingConfigDTO> lookerConfigCache;

  private final LoadingCache<String, DashboardMetadataListDTO> lookerDashboardMetadataCache;

  private final UserDAO userDAO;

  private final MembershipMappingService membershipMappingService;

  private final SamlUserDAO samlUserDAO;

  private final InsightWork insightWork;

  @Inject
  public EnterpriseReportingService(
      final HdsClient hdsClient,
      final CurrentUser currentUser,
      final UserDAO userDAO,
      final SamlUserDAO samlUserDAO,
      final MembershipMappingService membershipMappingService,
      final InsightWork insightWork)
  {
    this.hdsClient = hdsClient;
    this.currentUser = currentUser;
    this.userDAO = userDAO;
    this.samlUserDAO = samlUserDAO;
    this.membershipMappingService = membershipMappingService;
    this.insightWork = insightWork;
    this.lookerConfigCache = CacheBuilder.newBuilder().expireAfterWrite(MAX_AGE)
        .build(newEnterpriseReportingConfigCacheLoader());
    this.lookerDashboardMetadataCache = CacheBuilder.newBuilder().expireAfterWrite(MAX_AGE)
        .build(newLookerDashboardMetadataLoader());
  }

  //for testing only
  EnterpriseReportingService(
      final HdsClient hdsClient,
      final CurrentUser currentUser,
      final UserDAO userDAO,
      final SamlUserDAO samlUserDAO,
      final MembershipMappingService membershipMappingService,
      final InsightWork insightWork,
      final LoadingCache<String, EnterpriseReportingConfigDTO> configCache,
      final LoadingCache<String, DashboardMetadataListDTO> dashboardCache)
  {
    this.hdsClient = hdsClient;
    this.currentUser = currentUser;
    this.userDAO = userDAO;
    this.samlUserDAO = samlUserDAO;
    this.membershipMappingService = membershipMappingService;
    this.insightWork = insightWork;
    this.lookerConfigCache = configCache;
    this.lookerDashboardMetadataCache = dashboardCache;
  }

  private CacheLoader<String, EnterpriseReportingConfigDTO> newEnterpriseReportingConfigCacheLoader() {
    return new CacheLoader<String, EnterpriseReportingConfigDTO>()
    {
      @Override
      public EnterpriseReportingConfigDTO load(@NotNull final String key) {
        return hdsClient.get(EnterpriseReportingConfigDTO.class, ENTERPRISE_REPORTING_CONFIG_PATH);
      }
    };
  }

  private CacheLoader<String, DashboardMetadataListDTO> newLookerDashboardMetadataLoader() {
    return new CacheLoader<String, DashboardMetadataListDTO>()
    {
      @Override
      public DashboardMetadataListDTO load(@NotNull final String key) {
        DashboardMetadataListDTO dashboardMetadataListDTO =
            hdsClient.get(DashboardMetadataListDTO.class,
                ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
        downloadAndCacheDashboardIcons();
        return dashboardMetadataListDTO;
      }
    };
  }

  SSOEmbedUrlDTO createSSOEmbedUrl(DashboardRequestDTO lookerDashboard) {
    AuditData.get().setLookerDashboard(lookerDashboard);
    checkLookerIntegratedEnterpriseReportingEnabled();
    validateLookerDashboardValue(lookerDashboard);
    String requestId = UUID.randomUUID().toString().replace("-", "");
    SSOEmbedUrlDTO result = hdsClient.post(SSOEmbedUrlDTO.class, ENTERPRISE_REPORTING_SSO_EMBED_URL_PATH,
        buildRequest(requestId, lookerDashboard.dashboard));
    result.baseUrl = getBaseUrl();
    return result;
  }

  public String getBaseUrl() {
    try {
      return lookerConfigCache.get(DEFAULT_CONFIG_CACHE_KEY).baseUrl;
    }
    catch (ExecutionException e) {
      throw new InternalServerException("unable to load Enterprise Reporting configuration from " +
          "Sonatype Data Services", e);
    }
  }

  public DashboardMetadataListDTO getLookerDashboardMetadata() {
    checkLookerIntegratedEnterpriseReportingEnabled();
    try {
      return lookerDashboardMetadataCache.get(DEFAULT_DASHBOARD_CACHE_KEY);
    }
    catch (ExecutionException e) {
      throw new InternalServerException("unable to load Integrated Enterprise Reporting metadata from " +
          "Sonatype Data Services", e);
    }
  }

  private void validateLookerDashboardValue(DashboardRequestDTO lookerDashboard) {
    if (lookerDashboard == null || StringUtils.isBlank(lookerDashboard.dashboard)) {
      log.debug("Bad data in request dashboard is null or empty");
      throw new BadRequestException("Dashboard is null or empty");
    }
  }

  private SSOEmbedUrlRequest buildRequest(String requestId, String lookerDashboard) {
    log.debug("Submitting Enterprise Reporting SSOEmbedUrl request {} for dashboard {}", requestId, lookerDashboard);
    Pair<String, String> names = getUserFirstAndLastnames();
    final UserPrincipal userPrincipal = currentUser.getUserPrincipal();

    return new SSOEmbedUrlRequest(requestId, names.getLeft(), names.getRight(), lookerDashboard,
        membershipMappingService.getPermissionsForUserPrincipal(userPrincipal.getUsername(),
            userPrincipal.getMembership()),
        membershipMappingService.getApplicationIdsForUser(userPrincipal.getUsername(), userPrincipal.getMembership()));
  }

  private Pair<String, String> getUserFirstAndLastnames() {
    UserPrincipal principal = currentUser.getUserPrincipal();
    if (principal == null) {
      //At a minimum the user needs to be logged into access looker. see CLM-27812
      throw new UnauthenticatedException("Anonymous access forbidden for createSSOEmbedUrl");
    }

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
    if (!SystemConfigurationPropertyFeature.INTEGRATED_ENTERPRISE_REPORTING.isEnabled()) {
      throw new NotAuthorizedException(SystemConfigurationPropertyFeature.INTEGRATED_ENTERPRISE_REPORTING.getId()
          + " feature is disabled.");
    }
  }

  void downloadAndCacheDashboardIcons() {
    try (InputStream is = hdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH)) {
      byte[] fetchedIcons = IOUtils.toByteArray(is);
      deleteDashboardIcons();
      extractIconFiles(fetchedIcons);
    }
    catch (IOException e ) {
      log.debug("Error when saving dashboard icons", e);
    }
  }

  private void deleteDashboardIcons() {
    File iconsDirectory = insightWork.getIerDashboardIconsDirectory();
    if (iconsDirectory.exists()) {
      try (Stream<Path> paths = Files.walk(iconsDirectory.toPath())) {
        log.debug("Deleting cached dashboard icon files located in {}", iconsDirectory.getPath());
        paths.map(it -> new File(String.valueOf(it))).forEach(File::delete);
      }
      catch (IOException e) {
        log.debug("Error deleting dashboard icon(s)", e);
      }
    }
  }

  private void extractIconFiles(byte[] iconsZipFile) {
    try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(iconsZipFile))) {
      for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
        File iconsDirectory = insightWork.getIerDashboardIconsDirectory();
        Path iconPath = iconsDirectory.toPath().resolve(ze.getName()).normalize();
        Files.createDirectories(iconPath.getParent());
        Files.copy(zipIn, iconPath);
        log.debug("Cached dashboard icon {}", iconPath);
      }
    }
    catch (IOException e) {
      log.debug("Error caching dashboard icon", e);
    }
  }

  public byte[] getIcon(String iconName) {
    Path basePath = Paths.get(insightWork.getIerDashboardIconsDirectory().getPath());
    Path filePath = basePath.resolve(iconName).normalize();
    if (!filePath.startsWith(basePath)) {
      throw new BadRequestException("Icon name cannot contain ../ or ..\\");
    }
    return getIconImage(iconName);
  }

  private byte[] getIconImage(String iconName) {
    File iconImage = new File(insightWork.getIerDashboardIconsDirectory(), iconName);
    if (iconImage.exists()) {
      try {
        return Files.readAllBytes(Paths.get(iconImage.toURI()));
      }
      catch (IOException e) {
        throw new InternalServerException("Could not read icon image", e);
      }
    }
    throw new NotFoundException("Icon named " + iconName + " was not found");
  }
}
