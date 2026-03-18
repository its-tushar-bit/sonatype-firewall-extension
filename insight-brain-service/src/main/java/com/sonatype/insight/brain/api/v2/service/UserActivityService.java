/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiActivityEventDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiDateRangeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPaginationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivityDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivityDetailDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivityFilterOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivitySummaryDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditLogFilesProvider;
import com.sonatype.insight.brain.audit.AuditErrorType;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.ws.rs.InternalServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.time.temporal.ChronoUnit.DAYS;

@Named
@Singleton
public class UserActivityService
{
  private static final Logger log = LoggerFactory.getLogger(UserActivityService.class);

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final int DEFAULT_LIMIT = 100;

  private static final int MAX_LIMIT = 1000;

  private static final int MAX_DATE_RANGE_DAYS = 30;

  private static final int DEFAULT_MAX_READ_USER_ACTIVITIES = 10000;

  private final int maxReadUserActivities;

  private static final long MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024; // 100MB per file

  private static final long MAX_TOTAL_FILE_SIZE_BYTES = 500 * 1024 * 1024; // 500MB total

  private static final int MAX_FILES_TO_PROCESS = 50;

  private static final int MEMORY_CHECK_INTERVAL = 5000; // Check every 5000 lines for better performance

  private static final int CORE_THREAD_POOL_SIZE_PER_TENANT = 2;

  private static final int MAX_THREAD_POOL_SIZE_PER_TENANT = 4;

  private static final int THREAD_KEEP_ALIVE_TIME_SECONDS = 60;

  private static final int REQUEST_TIMEOUT_SECONDS = 30;

  /**
   * Extracts top-level domain (first part before dot) for grouping.
   * Examples:
   * - "governance.component.identity" -> "governance"
   * - "security.user" -> "security"
   * - "authentication" -> "authentication"
   */
  static String extractTopLevelDomain(String domain) {
    String[] parts = domain.split("\\.");
    return parts[0];
  }

  // Pre-calculated filter options for performance optimization
  private static final List<String> CACHED_DOMAINS = new ArrayList<>(Arrays.stream(AuditEvent.values())
      .map(AuditEvent::getDomain)
      .map(UserActivityService::extractTopLevelDomain)
      .collect(Collectors.toCollection(TreeSet::new)));

  private static final List<String> CACHED_ACTIVITY_TYPES = new ArrayList<>(Arrays.stream(AuditEvent.values())
      .map(AuditEvent::getType)
      .collect(Collectors.toCollection(TreeSet::new)));

  // Error type filter options specific to user activity filtering
  private static final List<String> CACHED_ERROR_TYPES = createErrorTypeFilterOptions();

  private final AuditLogFilesProvider auditLogFilesProvider;

  private final ClusterLockManager clusterLockManager;

  private final TenantReference<TenantThreadPoolExecutor> executors;

  @Inject
  public UserActivityService(
      final AuditLogFilesProvider auditLogFilesProvider,
      final ClusterLockManager clusterLockManager,
      final ShutdownHandler shutdownHandler)
  {
    this(auditLogFilesProvider, clusterLockManager, shutdownHandler, DEFAULT_MAX_READ_USER_ACTIVITIES);
  }

  /**
   * Constructor with configurable user activity limit (primarily for testing).
   */
  public UserActivityService(
      final AuditLogFilesProvider auditLogFilesProvider,
      final ClusterLockManager clusterLockManager,
      final ShutdownHandler shutdownHandler,
      final int maxReadUserActivities)
  {
    this.auditLogFilesProvider = auditLogFilesProvider;
    this.clusterLockManager = clusterLockManager;
    this.maxReadUserActivities = maxReadUserActivities;

    ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("UserActivity-%s")
        .build();

    this.executors = new TenantReference<>(() -> {
      TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
          CORE_THREAD_POOL_SIZE_PER_TENANT,
          MAX_THREAD_POOL_SIZE_PER_TENANT,
          THREAD_KEEP_ALIVE_TIME_SECONDS,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          threadFactory,
          new AbortPolicy(),
          "userActivity",
          "UserActivityService");

      // Configure proper shutdown behavior
      tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
      shutdownHandler.add(tenantThreadPoolExecutor);

      return tenantThreadPoolExecutor;
    });
  }

  private void validateParams(String startUtcDate, String endUtcDate, Integer limit, Integer offset) {
    validateRequiredFields(startUtcDate, endUtcDate);
    validatePaginationParameters(limit, offset);
  }

  /**
   * Creates error type filter options for user activity filtering.
   * Moved from ErrorStatus enum as it's specific to user activity filtering.
   */
  private static List<String> createErrorTypeFilterOptions() {
    List<String> options = new ArrayList<>();
    options.add("Success"); // for no error (auditEvent.error == null)
    options.addAll(AuditErrorType.getAllValues());
    return options;
  }

  @Authorize(permission = Permission.ACCESS_AUDIT_LOG)
  public ApiUserActivitySummaryDTO getUserActivitySummary(
      final String startUtcDate,
      final String endUtcDate,
      final String username,
      final Integer limit,
      final Integer offset)
  {
    validateParams(startUtcDate, endUtcDate, limit, offset);

    int actualLimit = limit != null ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
    int actualOffset = offset != null ? offset : 0;
    List<File> auditLogFiles = auditLogFilesProvider.getAuditLogFiles(
        LocalDate.parse(startUtcDate), LocalDate.parse(endUtcDate));

    Map<String, UserActivityData> userActivities = executeWithTimeout(() -> processAuditFiles(auditLogFiles, username));

    List<ApiUserActivityDTO> users = convertToUserActivityDTOs(userActivities, actualLimit, actualOffset);

    ApiUserActivitySummaryDTO response = new ApiUserActivitySummaryDTO();
    response.users = users;
    response.totalUsers = userActivities.size();
    response.dateRange = createDateRange(startUtcDate, endUtcDate);
    response.pagination = createPagination(actualLimit, actualOffset,
        userActivities.size() > actualOffset + actualLimit);

    return response;
  }

  @Authorize(permission = Permission.ACCESS_AUDIT_LOG)
  public ApiUserActivityDetailDTO getUserActivityDetail(
      final String startUtcDate,
      final String endUtcDate,
      final String username,
      final Integer limit,
      final Integer offset,
      final List<String> activityTypes,
      final List<String> domains,
      final List<String> errorTypes)
  {
    validateParams(startUtcDate, endUtcDate, limit, offset);

    if (username == null || username.trim().isEmpty()) {
      throw new BadRequestException("username is required for detailed activity");
    }

    int actualLimit = limit != null ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
    int actualOffset = offset != null ? offset : 0;
    List<File> auditLogFiles = auditLogFilesProvider.getAuditLogFiles(
        LocalDate.parse(startUtcDate), LocalDate.parse(endUtcDate));

    UserActivityResult result = executeWithTimeout(
        () -> processAuditFilesForUserWithPagination(auditLogFiles, username, actualLimit, actualOffset,
            activityTypes, domains, errorTypes));

    ApiUserActivityDetailDTO response = new ApiUserActivityDetailDTO();
    response.username = username;
    response.activities = result.activities;
    response.pagination = createPagination(actualLimit, actualOffset, result.hasMore);

    return response;
  }

  @Authorize(permission = Permission.ACCESS_AUDIT_LOG)
  public List<ApiActivityEventDTO> getAllUserActivitiesForExport(
      final String startUtcDate,
      final String endUtcDate,
      final String username,
      final Integer limit,
      final Integer offset,
      final List<String> activityTypes,
      final List<String> domains,
      final List<String> errorTypes)
  {
    validateParams(startUtcDate, endUtcDate, limit, offset);

    int actualLimit = limit != null ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
    int actualOffset = offset != null ? offset : 0;
    List<File> auditLogFiles = auditLogFilesProvider.getAuditLogFiles(
        LocalDate.parse(startUtcDate), LocalDate.parse(endUtcDate));

    UserActivityResult result = executeWithTimeout(
        () -> processAuditFilesForUserWithPagination(auditLogFiles, username, actualLimit, actualOffset,
            activityTypes, domains, errorTypes));

    return result.activities;
  }

  // Visible for testing
  TenantReference<TenantThreadPoolExecutor> getExecutors() {
    return executors;
  }

  /**
   * Checks if user activity tracking limit has been reached for general queries.
   * Only applies the limit when doing general queries (not user-specific filtering).
   *
   * @param usernameFilter the username filter (null for general queries)
   * @param userActivities current user activities map
   * @param context description of where the check is being performed (for logging)
   * @return true if the limit has been reached and processing should stop
   */
  private boolean hasReachedUserActivityLimit(
      String usernameFilter,
      Map<String, UserActivityData> userActivities,
      String context)
  {
    boolean isGeneralQuery = (usernameFilter == null || usernameFilter.trim().isEmpty());
    if (isGeneralQuery && userActivities.size() >= maxReadUserActivities) {
      log.debug("Reached maximum user activity limit ({}) {}.",
          maxReadUserActivities, context);
      return true;
    }
    return false;
  }

  /**
   * Checks if file processing should stop due to performance limits.
   *
   * @param usernameFilter the username filter being applied
   * @param userActivities current user activities map
   * @param lineCount current line number being processed
   * @param fileName name of the file being processed
   * @return true if processing should stop
   */
  private boolean shouldStopProcessing(
      String usernameFilter,
      Map<String, UserActivityData> userActivities,
      int lineCount,
      String fileName)
  {
    // Check user tracking limit - only apply when doing general queries (not user-specific)
    if (hasReachedUserActivityLimit(usernameFilter, userActivities, "at line " + lineCount)) {
      return true;
    }

    // Memory pressure check - conservative safety net for large datasets
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    long maxMemory = runtime.maxMemory();
    if (usedMemory > (maxMemory * 0.90)) {
      log.warn("Critical memory usage detected ({}%), stopping file processing at line {} in file {}",
          Math.round(usedMemory * 100.0 / maxMemory), lineCount, fileName);
      return true;
    }

    return false;
  }

  private Map<String, UserActivityData> processAuditFiles(List<File> auditLogFiles, String usernameFilter) {
    Map<String, UserActivityData> userActivities = new HashMap<>();
    long totalBytesProcessed = 0;
    int filesProcessed = 0;

    log.debug("Processing {} audit files for user activity analysis", auditLogFiles.size());

    // Sort files by size (smallest first) for better performance
    List<File> sortedFiles = auditLogFiles.stream()
        .filter(File::exists)
        .sorted((f1, f2) -> Long.compare(f1.length(), f2.length()))
        .toList();

    for (File file : sortedFiles) {
      try {
        // Performance limit checks
        if (filesProcessed >= MAX_FILES_TO_PROCESS) {
          log.info("Reached maximum file processing limit ({}). Skipping {} remaining files.",
              MAX_FILES_TO_PROCESS, sortedFiles.size() - filesProcessed);
          break;
        }

        if (file.length() > MAX_FILE_SIZE_BYTES) {
          log.warn("Skipping large audit file: {} ({}MB > {}MB limit)",
              file.getName(), file.length() / (1024 * 1024), MAX_FILE_SIZE_BYTES / (1024 * 1024));
          continue;
        }

        if (totalBytesProcessed + file.length() > MAX_TOTAL_FILE_SIZE_BYTES) {
          log.info("Reached total file size limit ({}MB). Processed {} files.",
              MAX_TOTAL_FILE_SIZE_BYTES / (1024 * 1024), filesProcessed);
          break;
        }

        // Check if we should continue processing (memory/time limits)
        if (hasReachedUserActivityLimit(usernameFilter, userActivities, "while processing files")) {
          break;
        }

        processAuditFile(file, userActivities, usernameFilter);
        totalBytesProcessed += file.length();
        filesProcessed++;
      }
      catch (IOException e) {
        log.warn("Failed to process audit file: {}", file.getName(), e);
      }
    }

    log.trace("Completed processing {} audit files ({} MB total). Processed {} user activities.",
        filesProcessed, String.format("%.2f", totalBytesProcessed / (1024.0 * 1024.0)), userActivities.size());
    return userActivities;
  }

  private void processAuditFile(
      File file,
      Map<String, UserActivityData> userActivities,
      String usernameFilter) throws IOException
  {
    if (!file.exists() || !file.canRead()) {
      log.debug("Cannot read audit file: {} (exists: {}, readable: {})",
          file.getAbsolutePath(), file.exists(), file.canRead());
      return;
    }

    log.debug("Processing audit file: {} (size: {} bytes)", file.getName(), file.length());

    String lockId = "audit-file-processing-" + file.getName();
    try (ClusterLock clusterLock = clusterLockManager.createForAuditJsonFileStore(lockId)) {
      clusterLock.lock();
      try (BufferedReader reader = createBufferedReader(file)) {
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null) {
          lineCount++;

          // Periodic checks for performance limits during file processing
          if (lineCount % MEMORY_CHECK_INTERVAL == 0) {
            if (shouldStopProcessing(usernameFilter, userActivities, lineCount, file.getName())) {
              break;
            }
          }

          try {
            AuditDTO auditEvent = objectMapper.readValue(line, AuditDTO.class);
            if (isRelevantAuditEvent(auditEvent, usernameFilter)) {
              processAuditEvent(auditEvent, userActivities);
            }
          }
          catch (Exception e) {
            log.debug("Failed to parse audit line {} in file {}: {}", lineCount, file.getName(), line, e);
          }
        }

        log.debug("Completed processing file: {} ({} lines processed)", file.getName(), lineCount);
      }
    }
  }

  private BufferedReader createBufferedReader(File file) throws IOException {
    if (file.getName().endsWith(".gz")) {
      return new BufferedReader(new InputStreamReader(
          new GZIPInputStream(Files.newInputStream(file.toPath()))));
    }
    else {
      return Files.newBufferedReader(file.toPath());
    }
  }

  private boolean isRelevantAuditEvent(AuditDTO auditEvent, String usernameFilter) {
    // Skip system events and anonymous users
    if (auditEvent.username == null ||
        MDCUsernameScope.SYSTEM.equals(auditEvent.username) ||
        MDCUsernameScope.ANONYMOUS.equals(auditEvent.username))
    {
      return false;
    }

    // Apply username filter if provided
    return usernameFilter == null || usernameFilter.trim().isEmpty() || usernameFilter.equals(auditEvent.username);
  }

  private void processAuditEvent(AuditDTO auditEvent, Map<String, UserActivityData> userActivities) {
    // Prevent excessive memory usage by limiting the number of users tracked
    if (userActivities.size() >= maxReadUserActivities && !userActivities.containsKey(auditEvent.username)) {
      log.debug("Reached maximum user tracking limit ({}), skipping user: {}",
          maxReadUserActivities, auditEvent.username);
      return;
    }

    UserActivityData userData = userActivities.computeIfAbsent(
        auditEvent.username, k -> new UserActivityData(auditEvent.username));

    userData.addEvent(auditEvent);
  }

  private UserActivityResult processAuditFilesForUserWithPagination(
      List<File> auditLogFiles,
      String username,
      int limit,
      int offset,
      List<String> activityTypes,
      List<String> domains,
      List<String> errorTypes)
  {
    List<ApiActivityEventDTO> events = new ArrayList<>();
    int currentOffset = 0;
    boolean hasMore = false;

    for (File file : auditLogFiles) {
      try {
        List<ApiActivityEventDTO> fileEvents = processAuditFileForUser(file, username, activityTypes, domains,
            errorTypes);

        for (ApiActivityEventDTO event : fileEvents) {
          if (currentOffset >= offset) {
            if (events.size() < limit) {
              events.add(event);
            }
            else {

              // Found more records than requested - set hasMore and stop
              hasMore = true;
              return new UserActivityResult(events, hasMore);
            }
          }
          currentOffset++;
        }
      }
      catch (IOException e) {
        log.debug("Failed to process audit file for user {}: {}", username, file.getName(), e);
      }
    }

    return new UserActivityResult(events, hasMore);
  }

  private static class UserActivityResult
  {
    public final List<ApiActivityEventDTO> activities;

    public final boolean hasMore;

    public UserActivityResult(List<ApiActivityEventDTO> activities, boolean hasMore) {
      this.activities = activities;
      this.hasMore = hasMore;
    }
  }

  private List<ApiActivityEventDTO> processAuditFileForUser(
      File file,
      String username,
      List<String> activityTypes,
      List<String> domains,
      List<String> errorTypes) throws IOException
  {
    List<ApiActivityEventDTO> events = new ArrayList<>();

    String lockId = "audit-file-processing-" + file.getName();
    try (ClusterLock clusterLock = clusterLockManager.createForAuditJsonFileStore(lockId)) {
      clusterLock.lock();
      try (BufferedReader reader = createBufferedReader(file)) {
        String line;
        while ((line = reader.readLine()) != null) {
          try {
            AuditDTO auditEvent = objectMapper.readValue(line, AuditDTO.class);
            if (isRelevantAuditEvent(auditEvent, username)
                && matchesFilters(auditEvent, activityTypes, domains, errorTypes))
            {
              events.add(convertToActivityEventDTO(auditEvent));
            }
          }
          catch (Exception e) {
            log.debug("Failed to parse audit line: {}", line, e);
          }
        }
      }
    }

    return events;
  }

  private boolean matchesFilters(
      AuditDTO auditEvent,
      List<String> activityTypes,
      List<String> domains,
      List<String> errorTypes)
  {
    // Filter by activity type
    if (activityTypes != null && !activityTypes.isEmpty()) {
      if (!activityTypes.contains(auditEvent.type)) {
        return false;
      }
    }

    // Filter by domain (using top-level domain matching)
    if (domains != null && !domains.isEmpty()) {
      String auditTopLevelDomain = extractTopLevelDomain(auditEvent.domain);
      if (!domains.contains(auditTopLevelDomain)) {
        return false;
      }
    }

    // Filter by error type
    if (errorTypes != null && !errorTypes.isEmpty()) {
      boolean matchesError = false;
      for (String errorType : errorTypes) {
        if ("Success".equals(errorType)) {
          // Filter for success: should have no error
          if (auditEvent.error == null) {
            matchesError = true;
            break;
          }
        }
        else {
          // Filter for specific error type: exact match required
          if (errorType.equals(auditEvent.error)) {
            matchesError = true;
            break;
          }
        }
      }
      // If none of the error types matched, return false
      if (!matchesError) {
        return false;
      }
    }

    return true;
  }

  private List<ApiUserActivityDTO> convertToUserActivityDTOs(
      Map<String, UserActivityData> userActivities,
      int limit,
      int offset)
  {
    return userActivities.values()
        .stream()
        .sorted((a, b) -> b.getLoginCount().compareTo(a.getLoginCount()))
        .skip(offset)
        .limit(limit)
        .map(this::convertToUserActivityDTO)
        .collect(Collectors.toList());
  }

  private ApiUserActivityDTO convertToUserActivityDTO(UserActivityData userData) {
    ApiUserActivityDTO dto = new ApiUserActivityDTO();
    dto.username = userData.getUsername();
    dto.loginCount = userData.getLoginCount();
    dto.lastActive = userData.getLastActive();

    return dto;
  }

  private ApiActivityEventDTO convertToActivityEventDTO(AuditDTO auditEvent) {
    ApiActivityEventDTO dto = new ApiActivityEventDTO();
    dto.username = auditEvent.username;
    dto.timestamp = auditEvent.timestamp;
    dto.domain = auditEvent.domain;
    dto.type = auditEvent.type;
    dto.method = auditEvent.requestMethod;
    dto.uri = auditEvent.requestUri;
    dto.ipAddress = auditEvent.remoteIpAddress;
    dto.userAgent = auditEvent.userAgent;
    dto.errorType = auditEvent.error;

    return dto;
  }

  private ApiDateRangeDTO createDateRange(String startDate, String endDate) {
    ApiDateRangeDTO dateRange = new ApiDateRangeDTO();
    dateRange.startDate = startDate;
    dateRange.endDate = endDate;
    return dateRange;
  }

  private <T> T executeWithTimeout(java.util.concurrent.Callable<T> task) {
    try {
      Future<T> future = executors.get().submit(task);
      return future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InternalServerErrorException("Service temporarily unavailable due to high load. Please retry.");
    }
    catch (java.util.concurrent.TimeoutException e) {
      throw new InternalServerErrorException(String.format(
          "Request timed out after %d seconds. Consider using shorter date ranges or more specific filters.",
          REQUEST_TIMEOUT_SECONDS));
    }
    catch (ExecutionException e) {
      throw new InternalServerErrorException("Error processing audit files: " + e.getCause().getMessage());
    }
  }

  private ApiPaginationDTO createPagination(int limit, int offset, boolean hasMore) {
    ApiPaginationDTO pagination = new ApiPaginationDTO();
    pagination.limit = limit;
    pagination.offset = offset;
    pagination.hasMore = hasMore;
    return pagination;
  }

  private static void validateRequiredFields(final String startUtcDate, final String endUtcDate) {
    if (startUtcDate == null || endUtcDate == null) {
      throw new BadRequestException("startUtcDate and endUtcDate must be defined");
    }

    LocalDate start;
    try {
      start = LocalDate.parse(startUtcDate);
    }
    catch (DateTimeParseException e) {
      throw new BadRequestException(String.format("startUtcDate '%s' is invalid", startUtcDate));
    }

    LocalDate end;
    try {
      end = LocalDate.parse(endUtcDate);
    }
    catch (DateTimeParseException e) {
      throw new BadRequestException(String.format("endUtcDate '%s' is invalid", endUtcDate));
    }

    if (end.isBefore(start)) {
      throw new BadRequestException("startUtcDate must be before endUtcDate");
    }

    if (end.isAfter(LocalDate.now())) {
      throw new BadRequestException("endUtcDate cannot be in the future");
    }

    long daysBetween = DAYS.between(start, end);
    if (daysBetween > MAX_DATE_RANGE_DAYS) {
      throw new BadRequestException(String.format(
          "Date range too large. Maximum allowed range is %d days, requested: %d days",
          MAX_DATE_RANGE_DAYS, daysBetween));
    }
  }

  private static void validatePaginationParameters(final Integer limit, final Integer offset) {
    if (limit != null && limit < 0) {
      throw new BadRequestException("limit must be greater than or equal to 0");
    }
    if (offset != null && offset < 0) {
      throw new BadRequestException("offset must be greater than or equal to 0");
    }
  }

  @Authorize(permission = Permission.ACCESS_AUDIT_LOG)
  public ApiUserActivityFilterOptionsDTO getFilterOptions() {
    ApiUserActivityFilterOptionsDTO response = new ApiUserActivityFilterOptionsDTO();

    // Use pre-calculated cached values for performance
    response.domains = CACHED_DOMAINS;
    response.activityTypes = CACHED_ACTIVITY_TYPES;
    response.errorTypes = CACHED_ERROR_TYPES;

    return response;
  }
}
