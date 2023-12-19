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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authz.UnauthenticatedException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class EnterpriseReportingService
{
  private static final Logger log = LoggerFactory.getLogger(EnterpriseReportingService.class);

  public static final String ENTERPRISE_REPORTING_BASE_PATH = "rest/enterpriseReporting";

  public static final String ENTERPRISE_REPORTING_SSO_EMBED_URL_PATH = ENTERPRISE_REPORTING_BASE_PATH + "/ssoEmbedUrl";

  public static final String ENTERPRISE_REPORTING_CONFIG_PATH = ENTERPRISE_REPORTING_BASE_PATH + "/config";

  public static final String ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH =
      ENTERPRISE_REPORTING_BASE_PATH + "/dashboards";

  public static final String ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH = ENTERPRISE_REPORTING_BASE_PATH + "/icons";

  public static final String ENTERPRISE_REPORTING_CURRENT_VERSION_PATH =
      ENTERPRISE_REPORTING_BASE_PATH + "/currentVersion";

  public static final String DEFAULT_GUAVA_CACHE_KEY = "default";

  private AtomicReference<DashboardMetadataListDTO> dashboardMetadataRef = new AtomicReference<>();

  // Visible for testing
  final AtomicInteger currentVersion = new AtomicInteger(-1);

  // Visible for testing
  final LoadingCache<String, Integer> currentVersionCache;

  private final LoadingCache<String, EnterpriseReportingConfigDTO> lookerConfigCache;

  private final HdsClient hdsClient;

  private final CurrentUser currentUser;

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
      final InsightWork insightWork,
      final Configuration configuration)
  {
    this.hdsClient = hdsClient;
    this.currentUser = currentUser;
    this.userDAO = userDAO;
    this.samlUserDAO = samlUserDAO;
    this.membershipMappingService = membershipMappingService;
    this.insightWork = insightWork;
    this.lookerConfigCache = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofHours(1))
        .build(newEnterpriseReportingConfigCacheLoader());
    this.currentVersionCache = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(configuration.getEnterpriseReportingVersionCacheExpirationInMinutes()))
        .build(newCurrentVersionCacheLoader());
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
      final AtomicReference<DashboardMetadataListDTO> dashboardData,
      final int currentVersion,
      final LoadingCache<String, Integer> currentVersionCache)
  {
    this.hdsClient = hdsClient;
    this.currentUser = currentUser;
    this.userDAO = userDAO;
    this.samlUserDAO = samlUserDAO;
    this.membershipMappingService = membershipMappingService;
    this.insightWork = insightWork;
    this.lookerConfigCache = configCache;
    this.dashboardMetadataRef = dashboardData;
    this.currentVersion.set(currentVersion);
    this.currentVersionCache = currentVersionCache;
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

  private CacheLoader<String, Integer> newCurrentVersionCacheLoader() {
    return new CacheLoader<String, Integer>()
    {
      @Override
      public Integer load(@NotNull final String key) {
        DashboardsVersionDTO hdsVersion =
            hdsClient.get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
        if (hdsVersion.version > currentVersion.get()) {
          reloadCachesAndUpdateLocalVersion(hdsVersion);
        }
        return hdsVersion.version;
      }
    };
  }

  private void refreshCacheIfNeeded() {
    if (this.currentVersion.get() == -1) {
      DashboardsVersionDTO hdsVersion;
      hdsVersion = hdsClient.get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
      reloadCachesAndUpdateLocalVersion(hdsVersion);

      return;
    }

    //this will trigger a cache refresh if needed
    this.currentVersionCache.getUnchecked(DEFAULT_GUAVA_CACHE_KEY);
  }

  private void reloadCachesAndUpdateLocalVersion(
      final DashboardsVersionDTO hdsVersion)
  {
    try {
      cacheDashboardMetadata();
      cacheDashboardIcons();
      this.currentVersion.set(hdsVersion.version);
    }
    catch (Exception ex) {
      log.error("error while fetching dashboard metadata from HDS.", ex);
    }
  }

  private void cacheDashboardMetadata() {
    log.debug("refreshing enterprise reporting dashboard metadata cache");
    this.dashboardMetadataRef.set(hdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH));
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
      return lookerConfigCache.get(DEFAULT_GUAVA_CACHE_KEY).baseUrl;
    }
    catch (ExecutionException e) {
      throw new InternalServerException("unable to load Enterprise Reporting configuration from " +
          "Sonatype Data Services", e);
    }
  }

  public DashboardMetadataListDTO getDashboardMetadata() {
    checkLookerIntegratedEnterpriseReportingEnabled();
    refreshCacheIfNeeded();

    if (dashboardMetadataRef.get() == null) {
      throw new InternalServerException("Error while fetching dashboard metadata from Sonatype data services");
    }
    else {
      return dashboardMetadataRef.get();
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

  void cacheDashboardIcons() {
    try (InputStream is = hdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH)) {
      byte[] fetchedIcons = IOUtils.toByteArray(is);
      deleteDashboardIcons();
      extractIconFiles(fetchedIcons);
    }
    catch (IOException e) {
      log.error("Error when saving dashboard icons", e);
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
        log.error("Error deleting dashboard icon(s)", e);
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
      log.error("Error caching dashboard icon", e);
    }
  }

  public byte[] getIcon(String iconName) {
    boolean imageDoesNotExist = dashboardMetadataRef.get().dashboardMetadata.stream()
        .noneMatch(it -> it.previewImage.equals(iconName));
    if (imageDoesNotExist) {
      throw new NotFoundException("Icon named " + iconName + " was not found");
    }
    return getIconImage(iconName);
  }

  private byte[] getIconImage(String iconName) {
    File iconImage = new File(insightWork.getIerDashboardIconsDirectory(), iconName);
    try {
      return Files.readAllBytes(Paths.get(iconImage.toURI()));
    }
    catch (IOException e) {
      throw new InternalServerException("Could not read icon image", e);
    }
  }
}
