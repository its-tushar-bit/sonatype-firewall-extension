/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionAcquire;
import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionGenerateTokens;
import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionGenerateTokensResponse;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.security.SsoUser;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.utils.MostRecentMemoizingFunction;
import com.sonatype.insight.brain.utils.ResettableExpiringMemoizingSupplier;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.jaxrs.JsonUtils;
import com.sonatype.insight.scan.util.HashUtils;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.shiro.authz.UnauthenticatedException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class EnterpriseReportingService
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(EnterpriseReportingService.class);

  public static final String ENTERPRISE_REPORTING_BASE_PATH = "rest/enterpriseReporting";
  
  public static final String ENTERPRISE_REPORTING_CONFIG_PATH = ENTERPRISE_REPORTING_BASE_PATH + "/config";

  public static final String ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH =
      ENTERPRISE_REPORTING_BASE_PATH + "/dashboards";

  static final String ACQUIRE_EMBED_SESSION_URL_PATH = ENTERPRISE_REPORTING_BASE_PATH + "/acquireEmbedSession";

  static final String GENERATE_EMBED_TOKENS_URL_PATH = ENTERPRISE_REPORTING_BASE_PATH + "/generateEmbedTokens";

  public static final String ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH = ENTERPRISE_REPORTING_BASE_PATH + "/icons";

  public static final String ENTERPRISE_REPORTING_CURRENT_VERSION_PATH =
      ENTERPRISE_REPORTING_BASE_PATH + "/currentVersion";

  private static final String TASK_NAME = "UpdateEnterpriseDashboardLocalCache";

  // Visible for testing
  static final String TASK_PARAM_CURRENT_VERSION = "CURRENT_VERSION";

  // Visible for testing
  public final ResettableExpiringMemoizingSupplier<Integer> currentDashboardsVersionSupplier;

  private final TenantReference<ResettableExpiringMemoizingSupplier<String>>
      enterpriseReportingConfigDTOBaseUrlSupplier;

  private final MostRecentMemoizingFunction<Integer, DashboardMetadataListDTO> dashboardMetadataGetter =
      new MostRecentMemoizingFunction<>(version -> getDashboardMetadataListDTOFromHds());

  private final MostRecentMemoizingFunction<Integer, Function<String, Supplier<byte[]>>> iconGetter =
      new MostRecentMemoizingFunction<>(version -> getIcons());

  private final HdsClient hdsClient;

  private final CurrentUser currentUser;

  private final UserDAO userDAO;

  private final MembershipMappingService membershipMappingService;

  private final ApplicationService applicationService;

  private final SsoUserService ssoUserService;

  private final InsightWork insightWork;

  private final TaskScheduler taskScheduler;

  private final Configuration configuration;

  private final ReadWriteLock iconReadWriteLock = new ReentrantReadWriteLock();

  private final Lock iconReadLock = iconReadWriteLock.readLock();

  private final Lock iconWriteLock = iconReadWriteLock.writeLock();

  @Inject
  public EnterpriseReportingService(
      final HdsClient hdsClient,
      final CurrentUser currentUser,
      final UserDAO userDAO,
      final SsoUserService ssoUserService,
      final MembershipMappingService membershipMappingService,
      final ApplicationService applicationService,
      final InsightWork insightWork,
      final TaskScheduler taskScheduler,
      final Configuration configuration)
  {
    this.hdsClient = hdsClient;
    this.currentUser = currentUser;
    this.userDAO = userDAO;
    this.ssoUserService = ssoUserService;
    this.membershipMappingService = membershipMappingService;
    this.applicationService = applicationService;
    this.insightWork = insightWork;
    this.taskScheduler = taskScheduler;
    this.configuration = configuration;
    this.currentDashboardsVersionSupplier = createDashboardsCurrentVersionSupplier();
    this.enterpriseReportingConfigDTOBaseUrlSupplier =
        new TenantReference<>(this::createEnterpriseReportingConfigDTOBaseUrlSupplier);
  }

  private ResettableExpiringMemoizingSupplier<Integer> createDashboardsCurrentVersionSupplier() {
    return new ResettableExpiringMemoizingSupplier<>(
        () -> getDashboardsVersionDTOFromHds().version,
        Duration.ofMinutes(configuration.getEnterpriseReportingVersionCacheExpirationInMinutes()),
        value -> taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this,
            Collections.singletonMap(TASK_PARAM_CURRENT_VERSION, String.valueOf(value)))
    );
  }

  private ResettableExpiringMemoizingSupplier<String> createEnterpriseReportingConfigDTOBaseUrlSupplier() {
    return new ResettableExpiringMemoizingSupplier<>(() -> getEnterpriseReportingConfigDTOFromHds().baseUrl,
        Duration.ofHours(1));
  }

  public DashboardMetadataListDTO getDashboardMetadata() {
    return dashboardMetadataGetter.apply(currentDashboardsVersionSupplier.get());
  }

  public EmbedCookielessSessionAcquire acquireEmbedSession(
      String dashboardId,
      String encodedEmbedDomain,
      String clientUserAgent)
  {
    AuditData.get().setLookerDashboard(dashboardId);
    validate(dashboardId);
    HttpEntity entity;
    try {
      entity = new StringEntity(
          JsonUtils.toJson(createEmbedRequest(dashboardId,
              URLDecoder.decode(encodedEmbedDomain, StandardCharsets.UTF_8.name()))),
          ContentType.APPLICATION_JSON);
    }
    catch (IOException e) {
      throw new BadRequestException(e);
    }

    String baseUrlPath = configuration.getBaseUrlConfiguration().getBaseUrl();

    Map<String, String> queryParams = new HashMap<>();
    if (!StringUtils.isBlank(baseUrlPath)) {
      queryParams.put("baseUrl", baseUrlPath);
    }

    return hdsClient.post(EmbedCookielessSessionAcquire.class, ACQUIRE_EMBED_SESSION_URL_PATH, entity, queryParams,
        clientUserAgent);
  }

  public EmbedCookielessSessionGenerateTokensResponse generateEmbedTokens(
      EmbedCookielessSessionGenerateTokens embedCookielessSessionGenerateTokens,
      String clientUserAgent)
  {
    validateRequiredParameters(embedCookielessSessionGenerateTokens);
    HttpEntity entity;
    try {
      entity = new StringEntity(
          JsonUtils.toJson(embedCookielessSessionGenerateTokens), ContentType.APPLICATION_JSON);
    }
    catch (IOException e) {
      throw new BadRequestException(e);
    }
    return hdsClient.put(EmbedCookielessSessionGenerateTokensResponse.class, GENERATE_EMBED_TOKENS_URL_PATH, entity,
        clientUserAgent);
  }

  private void validateRequiredParameters(final EmbedCookielessSessionGenerateTokens dto) {
    if (dto == null) {
      log.debug("Required dto is null");
      throw new BadRequestException("Required dto is null");
    }

    if (StringUtils.isBlank(dto.getNavigationToken())) {
      log.debug("Bad data in request navigation token is null or empty");
      throw new BadRequestException("Navigation token is null or empty");
    }

    if (StringUtils.isBlank(dto.getApiToken())) {
      log.debug("Bad data in request api token is null or empty");
      throw new BadRequestException("Api token is null or empty");
    }

    if (StringUtils.isBlank(dto.getSessionReferenceToken())) {
      log.debug("Bad data in request session reference token is null or empty");
      throw new BadRequestException("Session reference token is null or empty");
    }
  }

  private void validate(final String dashboardId) {
    if (StringUtils.isBlank(dashboardId)) {
      log.debug("Bad data in request dashboard is null or empty");
      throw new BadRequestException("Dashboard is null or empty");
    }
  }

  public byte[] getIcon(final String iconName) {
    validateIconName(iconName);
    return iconGetter.apply(currentDashboardsVersionSupplier.get()).apply(iconName).get();
  }

  @VisibleForTesting
  SSOEmbedUrlRequest createEmbedRequest(final String lookerDashboard, final String embedDomain) {
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    if (userPrincipal == null) {
      // At a minimum the user needs to be logged into access looker. see CLM-27812
      throw new UnauthenticatedException("Anonymous access forbidden for createSSOEmbedUrl");
    }
    String username = userPrincipal.getUsername();
    Set<String> membership = userPrincipal.getMembership();

    String requestId = UUID.randomUUID().toString().replace("-", "");
    String usernameAndRealm = getUsernameAndRealm(userPrincipal);
    Set<String> userPermissions = membershipMappingService.getPermissionsForUserPrincipal(username, membership);
    Set<String> applicationIds = obfuscateApplicationIds(applicationService.getApplications().stream()
        .map(Application::getId).collect(Collectors.toSet()));

    return new SSOEmbedUrlRequest(
        requestId,
        usernameAndRealm,
        null,
        null,
        lookerDashboard,
        userPermissions,
        applicationIds,
        embedDomain
    );
  }

  private String getUsernameAndRealm(final UserPrincipal userPrincipal) {
    return String.format("%s@%s", userPrincipal.getUsername(), userPrincipal.getRealmId());
  }

  private Pair<String, String> getUserFirstAndLastNames(final UserPrincipal userPrincipal) {
    String realmId = userPrincipal.getRealmId();

    if (InternalRealm.ID.equals(realmId)) {
      User user = getInternalUser(userPrincipal.getUsername());
      return Pair.of(user.getFirstName(), user.getLastName());
    }

    if (ssoUserService.isSsoRealm(realmId)) {
      SsoUser ssoUser = getSsoUser(userPrincipal.getUsername());
      return Pair.of(ssoUser.getFirstName(), ssoUser.getLastName());
    }

    return Pair.of(userPrincipal.getDisplayName(), "");
  }

  private User getInternalUser(final String username) {
    return userDAO.getByUsernameNotNull(username);
  }

  private SsoUser getSsoUser(final String username) {
    return ssoUserService.getByUsernameNotNull(username);
  }

  public String getEnterpriseReportingConfigDTOBaseUrl() {
    return enterpriseReportingConfigDTOBaseUrlSupplier.get().get();
  }

  private void validateIconName(final String iconName) {
    DashboardMetadataListDTO dashboardMetadataListDTO =
        dashboardMetadataGetter.apply(currentDashboardsVersionSupplier.get());
    boolean iconNameNotFound = dashboardMetadataListDTO.dashboardMetadata.stream()
        .noneMatch(dashboardMetadataDTO -> iconName.equals(dashboardMetadataDTO.previewImage));
    if (iconNameNotFound) {
      throw new NotFoundException("Icon named " + iconName + " was not found");
    }
  }

  private Function<String, Supplier<byte[]>> getIcons() {
    InputStream dashboardIcons = getEnterpriseReportingDashboardIconsInputStreamFromHds();
    return getIcons(dashboardIcons);
  }

  private Function<String, Supplier<byte[]>> getIcons(final InputStream iconsZipInputStream) {
    Map<String, Supplier<byte[]>> result = new HashMap<>();
    try {
      iconWriteLock.lock();
      FileUtils.deleteDirectory(insightWork.getIerDashboardIconsDirectory());
      result = extractIconFiles(iconsZipInputStream);
    }
    catch (IOException e) {
      log.error("Error deleting old dashboard icon(s)", e);
    }
    finally {
      iconWriteLock.unlock();
    }
    return result::get;
  }

  private Map<String, Supplier<byte[]>> extractIconFiles(final InputStream iconsZipInputStream) {
    Map<String, Supplier<byte[]>> iconDataSupplierByIconName = new HashMap<>();
    try (ZipInputStream zipInputStream = new ZipInputStream(iconsZipInputStream)) {
      for (ZipEntry zipEntry; (zipEntry = zipInputStream.getNextEntry()) != null; ) {
        File iconsDirectory = insightWork.getIerDashboardIconsDirectory();
        String iconName = zipEntry.getName();
        Path iconPath = iconsDirectory.toPath().resolve(iconName).normalize();
        Files.createDirectories(iconPath.getParent());
        Files.copy(zipInputStream, iconPath);
        iconDataSupplierByIconName.put(iconName, createIconDataSupplier(iconPath));
        log.debug("Cached dashboard icon {}", iconPath);
      }
    }
    catch (IOException e) {
      log.error("Error extracting dashboard icon(s)", e);
    }
    return iconDataSupplierByIconName;
  }

  private Supplier<byte[]> createIconDataSupplier(final Path iconPath) {
    return () -> {
      try {
        iconReadLock.lock();
        return Files.readAllBytes(iconPath);
      }
      catch (IOException e) {
        log.error("Error reading dashboard icon {}", iconPath, e);
        throw new InternalServerException("Could not read icon image", e);
      }
      finally {
        iconReadLock.unlock();
      }
    };
  }

  private EnterpriseReportingConfigDTO getEnterpriseReportingConfigDTOFromHds() {
    return hdsClient.get(EnterpriseReportingConfigDTO.class, ENTERPRISE_REPORTING_CONFIG_PATH);
  }

  private DashboardsVersionDTO getDashboardsVersionDTOFromHds() {
    return hdsClient.get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
  }

  private DashboardMetadataListDTO getDashboardMetadataListDTOFromHds() {
    return hdsClient.get(DashboardMetadataListDTO.class, ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
  }

  private InputStream getEnterpriseReportingDashboardIconsInputStreamFromHds() {
    return hdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH);
  }

  private Set<String> obfuscateApplicationIds(Set<String> applicationIds) {
    return applicationIds.stream()
        .map(applicationId -> HashUtils.hash(applicationId, HashUtils.SHA1)).collect(Collectors.toSet());
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    JobDataMap mergedJobDataMap = context.getMergedJobDataMap();

    String latestVersionString = mergedJobDataMap.getString(TASK_PARAM_CURRENT_VERSION);
    if (StringUtils.isNotBlank(latestVersionString)) {
      int latestVersion = Integer.parseInt(latestVersionString);
      currentDashboardsVersionSupplier.setMemoizedValue(latestVersion);
    }
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  void clearEnterpriseReportingConfigDTOBaseUrlSupplierForTests() {
    if (enterpriseReportingConfigDTOBaseUrlSupplier != null &&
        enterpriseReportingConfigDTOBaseUrlSupplier.get() != null) {
      enterpriseReportingConfigDTOBaseUrlSupplier.get().reset();
    }
  }
}
