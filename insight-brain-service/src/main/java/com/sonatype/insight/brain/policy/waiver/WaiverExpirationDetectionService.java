/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiWaiverExpirationNotificationConfigDTO;
import com.sonatype.insight.brain.api.v2.service.ApiWaiverExpirationNotificationConfigService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.WaiverExpirationNotificationConfigDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.webhook.WaiverExpirationEvent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to detect expiring waivers and emit webhook events and email notifications.
 *
 * @since 1.178.0
 */
@Named
public class WaiverExpirationDetectionService
    implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(WaiverExpirationDetectionService.class);

  private static final String EXPIRING_IN_7_DAYS = "EXPIRING_IN_7_DAYS";

  private static final String EXPIRING_IN_24_HOURS = "EXPIRING_IN_24_HOURS";

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyDAO policyDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final AsyncEventBus asyncEventBus;

  private final BaseUrl baseUrl;

  private final ProductLicense productLicense;

  private final WaiverExpirationEmailer waiverExpirationEmailer;

  private final ApiWaiverExpirationNotificationConfigService notificationConfigService;

  private final WaiverExpirationNotificationConfigDAO notificationConfigDAO;

  @Inject
  public WaiverExpirationDetectionService(
      final PolicyWaiverDAO policyWaiverDAO,
      final PolicyDAO policyDAO,
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final AsyncEventBus asyncEventBus,
      final BaseUrl baseUrl,
      final ProductLicense productLicense,
      final WaiverExpirationEmailer waiverExpirationEmailer,
      final ApiWaiverExpirationNotificationConfigService notificationConfigService,
      final WaiverExpirationNotificationConfigDAO notificationConfigDAO)
  {
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyDAO = policyDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.asyncEventBus = asyncEventBus;
    this.baseUrl = baseUrl;
    this.productLicense = productLicense;
    this.waiverExpirationEmailer = waiverExpirationEmailer;
    this.notificationConfigService = notificationConfigService;
    this.notificationConfigDAO = notificationConfigDAO;
  }

  @Override
  public void run() {
    if (!productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES)) {
      log.debug("Skipping waiver expiration detection - tenant {} lacks WEBHOOKS_FOR_REPOSITORIES",
          TenantThreadLocal.getTenant());
      return;
    }

    log.info("Starting Waiver Expiration Detection for tenant {}", TenantThreadLocal.getTenant());

    long start = System.currentTimeMillis();
    int eventsEmitted = 0;

    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      LocalDateTime now = LocalDateTime.now();
      ZoneId zoneId = ZoneId.systemDefault();

      // Window 1: 7-day advance warning (full day detection)
      LocalDate sevenDaysTargetDate = now.toLocalDate().plusDays(7);
      Date sevenDaysStart = Date.from(sevenDaysTargetDate.atStartOfDay(zoneId).toInstant());
      Date sevenDaysEnd = Date.from(sevenDaysTargetDate.plusDays(1).atStartOfDay(zoneId).toInstant());

      List<PolicyWaiver> sevenDayWaivers = policyWaiverDAO.getUpcomingExpiringWaivers(
          tx, sevenDaysStart, sevenDaysEnd);

      log.info("Found {} waivers expiring on {} for tenant {}",
          sevenDayWaivers.size(), sevenDaysTargetDate, TenantThreadLocal.getTenant());

      // Window 2: 24-hour advance warning (full day detection)
      LocalDate oneDayTargetDate = now.toLocalDate().plusDays(1);
      Date oneDayStart = Date.from(oneDayTargetDate.atStartOfDay(zoneId).toInstant());
      Date oneDayEnd = Date.from(oneDayTargetDate.plusDays(1).atStartOfDay(zoneId).toInstant());

      List<PolicyWaiver> oneDayWaivers = policyWaiverDAO.getUpcomingExpiringWaivers(
          tx, oneDayStart, oneDayEnd);

      log.info("Found {} waivers expiring on {} for tenant {}",
          oneDayWaivers.size(), oneDayTargetDate, TenantThreadLocal.getTenant());

      // Batch-load policies and owners to avoid N+1 queries
      Set<String> policyIds = new HashSet<>();
      Set<String> ownerIds = new HashSet<>();

      for (PolicyWaiver waiver : sevenDayWaivers) {
        if (waiver.getPolicyId() != null) {
          policyIds.add(waiver.getPolicyId());
        }
        if (waiver.getOwnerId() != null) {
          ownerIds.add(waiver.getOwnerId());
        }
      }
      for (PolicyWaiver waiver : oneDayWaivers) {
        if (waiver.getPolicyId() != null) {
          policyIds.add(waiver.getPolicyId());
        }
        if (waiver.getOwnerId() != null) {
          ownerIds.add(waiver.getOwnerId());
        }
      }

      Map<String, Policy> policiesById = new HashMap<>();
      Map<String, Owner> ownersById = new HashMap<>();

      if (!policyIds.isEmpty()) {
        List<Policy> policies = policyDAO.getByIds(policyIds);
        for (Policy policy : policies) {
          policiesById.put(policy.getId(), policy);
        }
        log.debug("Batch-loaded {} policies for {} waivers", policies.size(),
            sevenDayWaivers.size() + oneDayWaivers.size());
      }

      if (!ownerIds.isEmpty()) {
        List<Application> applications = applicationDAO.getByIds(ownerIds);
        for (Application application : applications) {
          ownersById.put(application.getId(), application);
        }
        log.debug("Batch-loaded {} applications for {} waivers", applications.size(),
            sevenDayWaivers.size() + oneDayWaivers.size());

        List<Repository> repositories = repositoryDAO.getByIds(ownerIds);
        for (Repository repository : repositories) {
          ownersById.put(repository.getId(), repository);
        }
        log.debug("Batch-loaded {} repositories for {} waivers", repositories.size(),
            sevenDayWaivers.size() + oneDayWaivers.size());
      }

      for (PolicyWaiver waiver : sevenDayWaivers) {
        try {
          // Validate createTime and expiryTime
          if (waiver.getCreateTime() == null || waiver.getExpiryTime() == null) {
            log.warn("Skipping waiver {} - missing createTime or expiryTime", waiver.getId());
            continue;
          }

          // Calculate original waiver duration
          long originalDurationMs = waiver.getExpiryTime().getTime() - waiver.getCreateTime().getTime();

          // Validate duration is not negative
          if (originalDurationMs < 0) {
            log.warn("Skipping waiver {} - invalid duration (expiryTime before createTime)", waiver.getId());
            continue;
          }

          // Integer division truncates toward zero.
          // Note: A waiver of exactly 7 days and 1 minute will have originalDurationDays == 7 (truncated).
          // The check is `> 7` so waivers must be strictly more than 7 complete days (8+ days) to
          // receive the 7-day notice.
          // This is intentional to avoid sending premature notifications for week-long waivers.
          long originalDurationDays = originalDurationMs / (24 * 60 * 60 * 1000);

          log.info("Waiver {} has original duration of {} days", waiver.getId(), originalDurationDays);

          // Only send 7-day notice if original duration > 7 days
          if (originalDurationDays > 7) {
            WaiverExpirationEvent event = createWaiverExpirationEvent(waiver, EXPIRING_IN_7_DAYS,
                policiesById, ownersById);
            asyncEventBus.post(event);
            eventsEmitted++;
            log.info("Posted 7-day expiration event for waiver {}", waiver.getId());
          }
          else {
            log.info("Skipping 7-day notice for waiver {} (duration {} days <= 7)",
                waiver.getId(), originalDurationDays);
          }
        }
        catch (Exception e) {
          log.error("Failed to emit 7-day waiver event for waiver {}", waiver.getId(), e);
        }
      }

      for (PolicyWaiver waiver : oneDayWaivers) {
        try {
          // Validate createTime and expiryTime
          if (waiver.getCreateTime() == null || waiver.getExpiryTime() == null) {
            log.warn("Skipping waiver {} - missing createTime or expiryTime", waiver.getId());
            continue;
          }

          // Validate duration is not negative
          long durationMs = waiver.getExpiryTime().getTime() - waiver.getCreateTime().getTime();
          if (durationMs < 0) {
            log.warn("Skipping waiver {} - invalid duration (expiryTime before createTime)", waiver.getId());
            continue;
          }

          // Always send 24-hour notice regardless of duration
          WaiverExpirationEvent event = createWaiverExpirationEvent(waiver, EXPIRING_IN_24_HOURS,
              policiesById, ownersById);
          asyncEventBus.post(event);
          eventsEmitted++;
          log.info("Posted 24-hour expiration event for waiver {}", waiver.getId());
        }
        catch (Exception e) {
          log.error("Failed to emit 24-hour waiver event for waiver {}", waiver.getId(), e);
        }
      }
    }
    catch (Exception e) {
      log.error("Error during waiver expiration detection for tenant {}", TenantThreadLocal.getTenant(), e);
    }

    // ── EMAIL SECTION — completely separate from webhooks ─────────────────────────────────────
    // Driven entirely by the notificationDays values stored in DB configs.
    // For each distinct threshold day across all configs, query waivers expiring on that day
    // and send email only to owners whose effective config includes that threshold.
    sendConfigDrivenEmailNotifications();

    log.info("Completed Waiver Expiration Detection in {} ms for tenant {}. Emitted {} events.",
        System.currentTimeMillis() - start, TenantThreadLocal.getTenant(), eventsEmitted);
  }

  /**
   * Config-driven email notification flow — runs independently of webhooks.
   *
   * <p>
   * Strategy:
   * <ol>
   * <li>Parse all distinct configured threshold days across the tenant (e.g. {@code {1,3,7,14}}).</li>
   * <li>Issue a <b>single</b> DB query for all waivers expiring between {@code today+1} and
   * {@code today+maxDay} (one round-trip regardless of how many threshold days exist).</li>
   * <li>For each waiver, compute {@code daysUntilExpiry} in-memory and check if the day falls
   * on a configured threshold.</li>
   * <li>Load the owner's effective config <b>once per waiver</b> and pass it directly into
   * {@link WaiverExpirationEmailer#send} so the emailer does not fetch it again.</li>
   * </ol>
   */
  private void sendConfigDrivenEmailNotifications() {
    // Step 1 — collect all distinct threshold days across all config rows
    Set<Integer> allThresholdDays;
    try {
      List<String> rawValues = notificationConfigDAO.findAllNotificationDays();
      allThresholdDays = new TreeSet<>();
      for (String raw : rawValues) {
        if (raw == null || raw.trim().isEmpty()) {
          continue;
        }
        for (String token : raw.split(",")) {
          token = token.trim();
          if (!token.isEmpty()) {
            try {
              allThresholdDays.add(Integer.parseInt(token));
            }
            catch (NumberFormatException e) {
              log.warn("Ignoring non-integer notification_days token '{}' in config", token);
            }
          }
        }
      }
    }
    catch (Exception e) {
      log.error("Failed to load notification day configs for email — skipping email notifications", e);
      return;
    }

    if (allThresholdDays.isEmpty()) {
      log.debug("No notification days configured — skipping email notifications");
      return;
    }

    int maxThresholdDay = allThresholdDays.stream().mapToInt(Integer::intValue).max().getAsInt();
    log.info("Config-driven email: threshold days={}, querying waivers expiring in next {} day(s)",
        allThresholdDays, maxThresholdDay);

    LocalDateTime now = LocalDateTime.now();
    ZoneId zoneId = ZoneId.systemDefault();
    LocalDate today = now.toLocalDate();

    // Step 2 — single query covering today+1 through today+maxThresholdDay (inclusive)
    Date windowStart = Date.from(today.plusDays(1).atStartOfDay(zoneId).toInstant());
    Date windowEnd = Date.from(today.plusDays(maxThresholdDay + 1).atStartOfDay(zoneId).toInstant());

    List<PolicyWaiver> allWaivers;
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      allWaivers = policyWaiverDAO.getUpcomingExpiringWaivers(tx, windowStart, windowEnd);
    }
    catch (Exception e) {
      log.error("Failed to query waivers for email notifications — skipping", e);
      return;
    }

    if (allWaivers.isEmpty()) {
      log.debug("No waivers expiring in the next {} day(s) — skipping email notifications", maxThresholdDay);
      return;
    }

    log.info("Found {} waiver(s) expiring in the next {} day(s) for email processing",
        allWaivers.size(), maxThresholdDay);

    // Step 3 — batch-load policies and owners once for all waivers
    Set<String> policyIds = new HashSet<>();
    Set<String> ownerIds = new HashSet<>();
    for (PolicyWaiver waiver : allWaivers) {
      if (waiver.getPolicyId() != null) {
        policyIds.add(waiver.getPolicyId());
      }
      if (waiver.getOwnerId() != null) {
        ownerIds.add(waiver.getOwnerId());
      }
    }

    Map<String, Policy> policiesById = new HashMap<>();
    Map<String, Owner> ownersById = new HashMap<>();

    if (!policyIds.isEmpty()) {
      List<Policy> policies = policyDAO.getByIds(policyIds);
      for (Policy policy : policies) {
        policiesById.put(policy.getId(), policy);
      }
    }
    if (!ownerIds.isEmpty()) {
      List<Organization> organizations = organizationDAO.getByIds(ownerIds);
      for (Organization organization : organizations) {
        ownersById.put(organization.getId(), organization);
      }
      List<Application> applications = applicationDAO.getByIds(ownerIds);
      for (Application application : applications) {
        ownersById.put(application.getId(), application);
      }
      List<Repository> repositories = repositoryDAO.getByIds(ownerIds);
      for (Repository repository : repositories) {
        ownersById.put(repository.getId(), repository);
      }
      List<RepositoryManager> repositoryManagers = repositoryManagerDAO.getByIds(ownerIds);
      for (RepositoryManager repositoryManager : repositoryManagers) {
        ownersById.put(repositoryManager.getId(), repositoryManager);
      }
      if (ownerIds.contains(RepositoryContainer.REPOSITORY_CONTAINER_ID)) {
        ownersById.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON);
      }
    }

    // Step 4 — for each waiver, compute days until expiry in-memory and send email if configured
    // Cache configs by ownerId to avoid repeated hierarchy walks for waivers on the same owner
    Map<String, ApiWaiverExpirationNotificationConfigDTO> configCache = new HashMap<>();

    for (PolicyWaiver waiver : allWaivers) {
      try {
        if (waiver.getOwnerId() == null || waiver.getExpiryTime() == null) {
          log.debug("Skipping email for waiver {} — ownerId or expiryTime is null", waiver.getId());
          continue;
        }

        // Compute how many full days away the waiver expires
        LocalDate expiryDate = waiver.getExpiryTime()
            .toInstant()
            .atZone(zoneId)
            .toLocalDate();
        long daysUntilExpiry = today.until(expiryDate, ChronoUnit.DAYS);

        if (!allThresholdDays.contains((int) daysUntilExpiry)) {
          // Waiver expires on a day that is not a configured threshold — skip
          continue;
        }

        // Step 4a — load config once per owner; cache to avoid repeated hierarchy walks
        if (!configCache.containsKey(waiver.getOwnerId())) {
          try {
            configCache.put(waiver.getOwnerId(), notificationConfigService.getConfig(waiver.getOwnerId()));
          }
          catch (Exception e) {
            log.error("Failed to load notification config for owner {} (waiver {})",
                waiver.getOwnerId(), waiver.getId(), e);
            continue;
          }
        }
        ApiWaiverExpirationNotificationConfigDTO config = configCache.get(waiver.getOwnerId());

        if (config == null || config.getNotificationDays() == null
            || !config.getNotificationDays().contains((int) daysUntilExpiry))
        {
          log.debug("Skipping email for waiver {} — day {} not in owner's effective config",
              waiver.getId(), daysUntilExpiry);
          continue;
        }

        // Use EXPIRING_IN_24_HOURS for 1-day threshold to match the existing webhook constant
        String emailStatus = (daysUntilExpiry == 1)
            ? EXPIRING_IN_24_HOURS
            : "EXPIRING_IN_" + daysUntilExpiry + "_DAYS";

        WaiverExpirationEvent event = createWaiverExpirationEvent(
            waiver, emailStatus, policiesById, ownersById);

        // Step 4b — pass config into emailer; no second getConfig call inside
        waiverExpirationEmailer.send(event, config);
        log.info("Sent email for waiver {} (days until expiry: {})", waiver.getId(), daysUntilExpiry);
      }
      catch (Exception e) {
        log.error("Failed to process email for waiver {}", waiver.getId(), e);
      }
    }
  }

  private WaiverExpirationEvent createWaiverExpirationEvent(
      PolicyWaiver waiver,
      String status,
      Map<String, Policy> policiesById,
      Map<String, Owner> ownersById)
  {
    WaiverExpirationEvent event = new WaiverExpirationEvent();

    // Basic waiver info
    event.initiator = "SYSTEM"; // System-generated event
    event.timestamp = LocalDateTime.now();
    event.waiverId = waiver.getId();
    event.expirationDate = waiver.getExpiryTime() != null
        ? LocalDateTime.ofInstant(waiver.getExpiryTime().toInstant(), ZoneId.systemDefault())
        : null;
    event.comment = waiver.getComment();
    event.status = status;

    // Creator info
    event.creatorUsername = waiver.getCreatorId();
    // Only populate creatorEmail if creatorName appears to be an email (contains '@')
    // creatorName may contain display name instead of email, so we validate before setting
    String creatorName = waiver.getCreatorName();
    event.creatorEmail = (creatorName != null && creatorName.contains("@")) ? creatorName : null;

    // Component info
    event.componentPackageUrl = waiver.getAssociatedPackageUrl();
    // Extract format and display name from PURL if available
    if (waiver.getAssociatedPackageUrl() != null && waiver.getAssociatedPackageUrl().startsWith("pkg:")) {
      String purl = waiver.getAssociatedPackageUrl();
      int slashIndex = purl.indexOf('/', 4); // Find first slash after "pkg:"
      if (slashIndex > 0) {
        event.componentFormat = purl.substring(4, slashIndex); // Extract format between "pkg:" and first "/"

        // Extract display name from PURL
        // Format: pkg:<format>/<namespace>/<name>@<version> or pkg:<format>/<name>@<version>
        String afterFormat = purl.substring(slashIndex + 1);
        // Remove any query parameters or qualifiers
        int queryIndex = afterFormat.indexOf('?');
        if (queryIndex > 0) {
          afterFormat = afterFormat.substring(0, queryIndex);
        }
        event.componentDisplayName = afterFormat;
      }
    }

    // Policy info - lookup from pre-loaded map to avoid N+1 queries
    event.policyId = waiver.getPolicyId();
    if (waiver.getPolicyId() != null) {
      Policy policy = policiesById.get(waiver.getPolicyId());
      if (policy != null) {
        log.debug("Found policy: id={}, name={}, threatLevel={}",
            policy.getId(), policy.getName(), policy.getThreatLevel());
        event.policyName = policy.getName();
        event.threatLevel = policy.getThreatLevel();
      }
      else {
        log.warn("Policy {} not found for waiver {}", waiver.getPolicyId(), waiver.getId());
      }
    }

    // Owner info (application or repository) - lookup from pre-loaded map to avoid N+1 queries
    event.applicationId = waiver.getOwnerId();
    if (waiver.getOwnerId() != null) {
      Owner owner = ownersById.get(waiver.getOwnerId());
      if (owner != null) {
        log.debug("Found owner: id={}, name={}, publicId={}, type={}",
            owner.getId(), owner.getName(), owner.getPublicId(), owner.getType());
        event.applicationPublicId = owner.getPublicId();
        event.applicationName = owner.getName();

        // Generate IQ report URL only for applications (not repositories)
        if (owner.getType() == OwnerType.APPLICATION) {
          if (baseUrl.get() != null && owner.getPublicId() != null) {
            event.iqReportUrl = String.format("%s/ui/links/application/%s/report/policy",
                baseUrl.get(), owner.getPublicId());
            log.debug("Generated IQ report URL: {}", event.iqReportUrl);
          }
          else {
            log.warn("Cannot generate IQ report URL - baseUrl is null: {}, publicId is null: {}",
                baseUrl.get() == null, owner.getPublicId() == null);
          }
        }
        else {
          log.debug("Skipping IQ report URL generation for owner type: {}", owner.getType());
        }
      }
      else {
        log.warn("Owner {} not found for waiver {}", waiver.getOwnerId(), waiver.getId());
      }
    }

    return event;
  }
}
