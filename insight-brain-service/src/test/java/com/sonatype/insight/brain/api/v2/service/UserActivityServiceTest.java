/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.api.v2.dto.ApiActivityEventDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivitySummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivityDetailDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivityFilterOptionsDTO;
import com.sonatype.insight.brain.audit.AuditLogFilesProvider;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.brain.security.SecurityAspectControl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.Mock;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserActivityServiceTest
{
  @TempDir
  public File tempFolder;

  @Mock
  private AuditLogFilesProvider mockAuditLogFilesProvider;

  @Mock
  private ClusterLockManager mockClusterLockManager;

  @Mock
  private ClusterLock mockClusterLock;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @BeforeEach
  public void bindSecurityManager() {
    SecurityManager securityManager = mock(SecurityManager.class);
    ThreadContext.bind(securityManager);
    SecurityAspectControl.disableEnforcement();
    SimplePrincipalCollection principals = new SimplePrincipalCollection("admin", "testRealm");
    Subject subject = new Subject.Builder(securityManager)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(subject);
  }

  @AfterEach
  public void unbindSecurityManager() {
    SecurityAspectControl.enableEnforcement();
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  private static final int TEST_USER_LIMIT = 5;

  // Common audit record constants
  private static final String JOHN_LOGIN =
      "{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"john.doe\"," +
          "\"type\":\"login\",\"domain\":\"authentication\",\"requestMethod\":\"POST\"," +
          "\"requestUri\":\"/api/v2/auth/login\",\"remoteIpAddress\":\"192.168.1.100\"}";

  private static final String JANE_LOGIN =
      "{\"timestamp\":\"2024-03-13T16:30:45.123Z\",\"username\":\"jane.smith\"," +
          "\"type\":\"login\",\"domain\":\"authentication\",\"requestMethod\":\"POST\"," +
          "\"requestUri\":\"/api/v2/auth/login\",\"remoteIpAddress\":\"192.168.1.101\"}";

  private static final String JOHN_REPORT_VIEW =
      "{\"timestamp\":\"2024-03-13T15:30:45.123Z\",\"username\":\"john.doe\"," +
          "\"type\":\"view\",\"domain\":\"reporting\",\"requestMethod\":\"GET\"," +
          "\"requestUri\":\"/api/v2/reports\",\"remoteIpAddress\":\"192.168.1.100\"}";

  private static final String JOHN_CREATE_APP =
      "{\"timestamp\":\"2024-03-13T16:30:45.123Z\",\"username\":\"john.doe\"," +
          "\"type\":\"create\",\"domain\":\"governance\",\"requestMethod\":\"POST\"," +
          "\"requestUri\":\"/api/v2/applications\",\"remoteIpAddress\":\"192.168.1.100\"}";

  private static final String JANE_EVALUATE_APP =
      "{\"timestamp\":\"2024-03-13T15:30:45.123Z\",\"username\":\"jane.smith\"," +
          "\"type\":\"EVALUATE_APPLICATION\",\"requestMethod\":\"GET\"," +
          "\"requestUri\":\"/api/v2/applications/123/reports\",\"remoteIpAddress\":\"192.168.1.101\"}";

  private static final String SYSTEM_USER_ACTIVITY =
      "{\"timestamp\":\"2024-03-13T15:30:45.123Z\",\"username\":\"*SYSTEM\"," +
          "\"type\":\"EVALUATE_APPLICATION\",\"requestMethod\":\"GET\"," +
          "\"requestUri\":\"/api/v2/applications/123/reports\",\"remoteIpAddress\":\"192.168.1.101\"}";

  private static final String UNKNOWN_USER_ACTIVITY =
      "{\"timestamp\":\"2024-03-13T16:30:45.123Z\",\"username\":\"*UNKNOWN\"," +
          "\"type\":\"VIEW_COMPONENT_INFORMATION\",\"requestMethod\":\"GET\"," +
          "\"requestUri\":\"/api/v2/components/abc\",\"remoteIpAddress\":\"192.168.1.102\"}";

  private static final String TEST_USER_LOGIN =
      "{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"test.user\"," +
          "\"type\":\"login\",\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/auth/login\"," +
          "\"remoteIpAddress\":\"192.168.1.100\"}";

  private UserActivityService userActivityService;

  @BeforeEach
  public void setUp() {
    when(mockClusterLockManager.createForAuditJsonFileStore(any())).thenReturn(mockClusterLock);
    userActivityService = new UserActivityService(mockAuditLogFilesProvider, mockClusterLockManager,
        mockShutdownHandler, TEST_USER_LIMIT);
  }

  // Helper methods for test setup
  private void setupMockAuditFiles(File... files) {
    when(mockAuditLogFilesProvider.getAuditLogFiles(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(files));
  }

  private File createBasicLoginScenario() throws IOException {
    return createAuditLogFile("audit.log", JOHN_LOGIN, JANE_LOGIN);
  }

  private File createSystemUserScenario() throws IOException {
    return createAuditLogFile("audit.log", JOHN_LOGIN, SYSTEM_USER_ACTIVITY, UNKNOWN_USER_ACTIVITY);
  }

  private File createFilteringScenario() throws IOException {
    return createAuditLogFile("audit.log",
        JOHN_LOGIN, // authentication domain
        JOHN_REPORT_VIEW, // reporting domain
        JOHN_CREATE_APP // governance domain
    );
  }

  private File createSingleUserScenario() throws IOException {
    return createAuditLogFile("audit.log", TEST_USER_LOGIN);
  }

  private File createMultiUserLoginScenario() throws IOException {
    return createAuditLogFile("audit.log", JOHN_LOGIN, JANE_LOGIN);
  }

  private File createExportTestScenario() throws IOException {
    return createAuditLogFile("audit.log", JOHN_LOGIN, JOHN_REPORT_VIEW, JANE_LOGIN);
  }

  private File createMultiDomainFilterScenario() throws IOException {
    return createAuditLogFile("audit.log",
        "{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"create\",\"domain\":\"governance.component.vulnerability\"," +
            "\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/components\",\"remoteIpAddress\":\"192.168.1.100\"}",
        "{\"timestamp\":\"2024-03-13T15:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"update\",\"domain\":\"governance.policy.evaluation\"," +
            "\"requestMethod\":\"PUT\",\"requestUri\":\"/api/v2/policies/123\",\"remoteIpAddress\":\"192.168.1.100\"}",
        "{\"timestamp\":\"2024-03-13T16:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"view\",\"domain\":\"reporting.dashboard.filter\"," +
            "\"requestMethod\":\"GET\",\"requestUri\":\"/api/v2/reports\",\"remoteIpAddress\":\"192.168.1.100\"}",
        "{\"timestamp\":\"2024-03-13T17:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"configure\",\"domain\":\"security.ldap.server\"," +
            "\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/security/ldap\",\"remoteIpAddress\":\"192.168.1.100\"}");
  }

  // Helper for creating dynamic user scenarios (pagination tests)
  private File createMultipleUsersScenario(int userCount) throws IOException {
    String[] lines = new String[userCount];
    for (int i = 1; i <= userCount; i++) {
      lines[i - 1] = "{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"user" + i +
          "\",\"type\":\"login\",\"requestMethod\":\"POST\"," +
          "\"requestUri\":\"/api/v2/auth/login\",\"remoteIpAddress\":\"192.168.1.100\"}";
    }
    return createAuditLogFile("audit.log", lines);
  }

  @Test
  public void testGetUserActivitySummary_withValidDateRange_returnsResults() throws IOException {
    // Given
    File auditFile = createAuditLogFile("audit.log", JOHN_LOGIN, JANE_EVALUATE_APP);
    setupMockAuditFiles(auditFile);

    // When
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", null, 100, 0);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.users).hasSize(2);
    assertThat(result.totalUsers).isEqualTo(2);
    assertThat(result.dateRange.startDate).isEqualTo("2024-03-10");
    assertThat(result.dateRange.endDate).isEqualTo("2024-03-13");
    assertThat(result.pagination.limit).isEqualTo(100);
    assertThat(result.pagination.offset).isEqualTo(0);

    // Check user data
    assertThat(result.users.get(0).username).isIn("john.doe", "jane.smith");
    assertThat(result.users.get(0).loginCount).isGreaterThanOrEqualTo(0);
    assertThat(result.users.get(0).lastActive).isNotNull();
  }

  @Test
  public void testGetUserActivitySummary_withUsernameFilter_returnsFilteredResults() throws IOException {
    // Given
    File auditFile = createAuditLogFile("audit.log", JOHN_LOGIN, JANE_EVALUATE_APP);
    setupMockAuditFiles(auditFile);

    // When
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.users).hasSize(1);
    assertThat(result.users.get(0).username).isEqualTo("john.doe");
    assertThat(result.users.get(0).loginCount).isEqualTo(1);
  }

  @Test
  public void testGetUserActivitySummary_withEmptyFiles_returnsEmptyResults() throws IOException {
    // Given
    File emptyAuditFile = createAuditLogFile("audit.log");

    when(mockAuditLogFilesProvider.getAuditLogFiles(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(emptyAuditFile));

    // When
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", null, 100, 0);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.users).isEmpty();
    assertThat(result.totalUsers).isEqualTo(0);
  }

  @Test
  public void testGetUserActivitySummary_filtersSystemAndAnonymousUsers() throws IOException {
    // Given
    File auditFile = createSystemUserScenario();
    setupMockAuditFiles(auditFile);

    // When
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", null, 100, 0);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.users).hasSize(1);
    assertThat(result.users.get(0).username).isEqualTo("john.doe");
  }

  @Test
  public void testGetUserActivityDetail_withValidUser_returnsDetailedResults() throws IOException {
    // Given
    File auditFile = createAuditLogFile("audit.log",
        "{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"login\",\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/auth/login\"," +
            "\"remoteIpAddress\":\"192.168.1.100\",\"userAgent\":\"Mozilla/5.0\"}",
        "{\"timestamp\":\"2024-03-13T15:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"EVALUATE_APPLICATION\",\"requestMethod\":\"GET\"," +
            "\"requestUri\":\"/api/v2/applications/123/reports\"," +
            "\"remoteIpAddress\":\"192.168.1.100\",\"userAgent\":\"Mozilla/5.0\"}");

    when(mockAuditLogFilesProvider.getAuditLogFiles(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(auditFile));

    // When
    ApiUserActivityDetailDTO result = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, null, null, null);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.username).isEqualTo("john.doe");
    assertThat(result.activities).hasSize(2);
    assertThat(result.activities.get(0).timestamp).isNotNull();
    assertThat(result.activities.get(0).domain).isNull(); // domain field added
    assertThat(result.activities.get(0).type).isIn("login", "EVALUATE_APPLICATION");
    assertThat(result.activities.get(0).method).isIn("POST", "GET");
    assertThat(result.activities.get(0).errorType).isNull(); // errorType field added
  }

  @Test
  public void testGetUserActivityDetail_withMissingUsername_throwsException() {
    // When/Then
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userActivityService.getUserActivityDetail(
            "2024-03-10", "2024-03-13", null, 100, 0, null, null, null))
        .withMessage("username is required for detailed activity");
  }

  @Test
  public void testGetUserActivitySummary_withInvalidStartDate_throwsException() {
    // When/Then
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userActivityService.getUserActivitySummary(
            "invalid-date", "2024-03-13", null, 100, 0))
        .withMessageContaining("startUtcDate 'invalid-date' is invalid");
  }

  @Test
  public void testGetUserActivitySummary_withInvalidEndDate_throwsException() {
    // When/Then
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userActivityService.getUserActivitySummary(
            "2024-03-10", "invalid-date", null, 100, 0))
        .withMessageContaining("endUtcDate 'invalid-date' is invalid");
  }

  @Test
  public void testGetUserActivitySummary_withEndDateBeforeStartDate_throwsException() {
    // When/Then
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userActivityService.getUserActivitySummary(
            "2024-03-13", "2024-03-10", null, 100, 0))
        .withMessage("startUtcDate must be before endUtcDate");
  }

  @Test
  public void testGetUserActivitySummary_withFutureEndDate_throwsException() {
    String futureDate = LocalDate.now().plusDays(1).toString();

    // When/Then
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userActivityService.getUserActivitySummary(
            "2024-03-10", futureDate, null, 100, 0))
        .withMessage("endUtcDate cannot be in the future");
  }

  @Test
  public void testGetUserActivitySummary_withPagination_returnsCorrectPage() throws IOException {
    // Given - Create audit file with multiple users
    File auditFile = createMultipleUsersScenario(5);
    setupMockAuditFiles(auditFile);

    // When
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", null, 2, 1);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.users).hasSize(2);
    assertThat(result.totalUsers).isEqualTo(5);
    assertThat(result.pagination.limit).isEqualTo(2);
    assertThat(result.pagination.offset).isEqualTo(1);
    assertThat(result.pagination.hasMore).isTrue();
  }

  @Test
  public void testGetUserActivitySummary_withExcessiveDateRange_throwsException() {
    String startDate = LocalDate.now().minusDays(35).toString();
    String endDate = LocalDate.now().toString();

    // When/Then
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userActivityService.getUserActivitySummary(
            startDate, endDate, null, 100, 0))
        .withMessageContaining("Date range too large. Maximum allowed range is 30 days");
  }

  @Test
  public void testGetUserActivityDetail_withExcessiveDateRange_throwsException() {
    String startDate = LocalDate.now().minusDays(35).toString();
    String endDate = LocalDate.now().toString();

    // When/Then
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userActivityService.getUserActivityDetail(
            startDate, endDate, "testuser", 100, 0, null, null, null))
        .withMessageContaining("Date range too large. Maximum allowed range is 30 days");
  }

  @Test
  public void testGetUserActivitySummary_handlesNonExistentFiles_gracefully() throws IOException {
    // Given - Non-existent file
    File nonExistentFile = new File(tempFolder, "non-existent.log");

    when(mockAuditLogFilesProvider.getAuditLogFiles(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(nonExistentFile));

    // When
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", null, 100, 0);

    // Then - Should handle gracefully and return empty results
    assertThat(result).isNotNull();
    assertThat(result.users).isEmpty();
    assertThat(result.totalUsers).isEqualTo(0);
  }

  @Test
  public void testGetUserActivitySummary_handlesCorruptedAuditLines_gracefully() throws IOException {
    // Given - Audit file with mix of valid and corrupted JSON lines
    File auditFile = createAuditLogFile("audit.log",
        JOHN_LOGIN,
        "{ corrupted json line without proper format",
        "not even close to json",
        JANE_LOGIN,
        "{ \"incomplete\": json");
    setupMockAuditFiles(auditFile);

    // When
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", null, 100, 0);

    // Then - Should process only valid lines and skip corrupted ones
    assertThat(result).isNotNull();
    assertThat(result.users).hasSize(2); // Only the 2 valid JSON lines
    assertThat(result.totalUsers).isEqualTo(2);
  }

  @Test
  public void testTenantThreadPoolExecutorIsUsed() throws IOException {
    // Given - Create audit file with valid data
    File auditFile = createSingleUserScenario();
    setupMockAuditFiles(auditFile);

    // When - Call the service methods
    ApiUserActivitySummaryDTO summaryResult = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", null, 100, 0);

    ApiUserActivityDetailDTO detailResult = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "test.user", 100, 0, null, null, null);

    // Then - Verify results are correct (indicating tenant thread pool was used successfully)
    assertThat(summaryResult).isNotNull();
    assertThat(summaryResult.users).hasSize(1);
    assertThat(summaryResult.users.get(0).username).isEqualTo("test.user");

    assertThat(detailResult).isNotNull();
    assertThat(detailResult.username).isEqualTo("test.user");
    assertThat(detailResult.activities).hasSize(1);

    // Verify tenant thread pool executor exists and is configured
    assertThat(userActivityService.getExecutors()).isNotNull();
  }

  @Test
  public void testGetUserActivitySummary_withUserLimit_stopsAtLimit() throws IOException {
    // Given - Create audit file with more users than the test limit (5)
    File auditFile = createMultipleUsersScenario(8); // 8 users, exceeds limit of 5
    setupMockAuditFiles(auditFile);

    // When - General query (no username filter)
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", null, 100, 0);

    // Then - Should limit users to the configured test limit
    assertThat(result).isNotNull();
    assertThat(result.totalUsers).isLessThanOrEqualTo(TEST_USER_LIMIT);
    assertThat(result.users).hasSizeLessThanOrEqualTo(TEST_USER_LIMIT);
  }

  @Test
  public void testGetUserActivitySummary_withUsernameFilter_bypassesLimit() throws IOException {
    // Given - Create audit file with more users than limit, including target user at end
    StringBuilder auditData = new StringBuilder();
    for (int i = 1; i <= 7; i++) { // 7 other users, exceeds limit of 5
      auditData.append("{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"user")
          .append(i)
          .append("\",\"type\":\"login\",\"requestMethod\":\"POST\"," +
              "\"requestUri\":\"/api/v2/auth/login\",\"remoteIpAddress\":\"192.168.1.100\"}\n");
    }
    // Add target user at the end (after limit would normally be hit)
    auditData.append("{\"timestamp\":\"2024-03-13T15:30:45.123Z\",\"username\":\"target.user\"," +
        "\"type\":\"login\",\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/auth/login\"," +
        "\"remoteIpAddress\":\"192.168.1.100\"}\n");

    File auditFile = createAuditLogFile("audit.log", auditData.toString().split("\n"));

    when(mockAuditLogFilesProvider.getAuditLogFiles(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(auditFile));

    // When - User-specific query (should bypass limit)
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", "target.user", 100, 0);

    // Then - Should find the target user despite exceeding general limit
    assertThat(result).isNotNull();
    assertThat(result.users).hasSize(1);
    assertThat(result.users.get(0).username).isEqualTo("target.user");
    assertThat(result.users.get(0).loginCount).isEqualTo(1);
  }

  @Test
  public void testGetUserActivitySummary_withUserCountBelowLimit_processesAll() throws IOException {
    // Given - Create audit file with fewer users than limit
    StringBuilder auditData = new StringBuilder();
    for (int i = 1; i <= 3; i++) { // 3 users, below limit of 5
      auditData.append("{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"user")
          .append(i)
          .append("\",\"type\":\"login\",\"requestMethod\":\"POST\"," +
              "\"requestUri\":\"/api/v2/auth/login\",\"remoteIpAddress\":\"192.168.1.100\"}\n");
    }

    File auditFile = createAuditLogFile("audit.log", auditData.toString().split("\n"));

    when(mockAuditLogFilesProvider.getAuditLogFiles(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(auditFile));

    // When - General query with count below limit
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", null, 100, 0);

    // Then - Should process all users (no limit triggered)
    assertThat(result).isNotNull();
    assertThat(result.totalUsers).isEqualTo(3);
    assertThat(result.users).hasSize(3);
  }

  @Test
  public void testGetUserActivitySummary_withEmptyUsernameFilter_treatedAsGeneralQuery() throws IOException {
    // Test that empty username filter is treated as general query, not user-specific
    File auditFile = createAuditLogFile("audit.log",
        "{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"user1\"," +
            "\"type\":\"login\",\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/auth/login\"," +
            "\"remoteIpAddress\":\"192.168.1.100\"}",
        "{\"timestamp\":\"2024-03-13T15:30:45.123Z\",\"username\":\"user2\"," +
            "\"type\":\"login\",\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/auth/login\"," +
            "\"remoteIpAddress\":\"192.168.1.101\"}");

    when(mockAuditLogFilesProvider.getAuditLogFiles(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(auditFile));

    // When - Test with empty string username filter
    ApiUserActivitySummaryDTO result = userActivityService.getUserActivitySummary(
        "2024-03-10", "2024-03-13", "", 100, 0);

    // Then - Should return all users (empty filter treated as no filter)
    assertThat(result).isNotNull();
    assertThat(result.users).hasSize(2);
    assertThat(result.totalUsers).isEqualTo(2);
  }

  @Test
  public void testGetFilterOptions_returnsExpectedFilterOptions() {
    // When
    ApiUserActivityFilterOptionsDTO result = userActivityService.getFilterOptions();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.domains).isNotNull().isNotEmpty();
    assertThat(result.activityTypes).isNotNull().isNotEmpty();
    assertThat(result.errorTypes).isNotNull().hasSize(13);

    // Verify error statuses contain expected values (Success + all AuditRecorder constants)
    assertThat(result.errorTypes).contains("Success", "bad-request", "bad-authentication",
        "bad-session", "unauthenticated", "unlicensed", "unauthorized", "not-found",
        "bad-gateway", "service-unavailable", "gateway-timeout", "server-error", "client-error");

    // Verify domains and types are sorted
    assertThat(result.domains).isSorted();
    assertThat(result.activityTypes).isSorted();

    // Verify no null or empty values
    assertThat(result.domains).doesNotContainNull();
    assertThat(result.activityTypes).doesNotContainNull();
    assertThat(result.errorTypes).doesNotContainNull();
  }

  @Test
  public void testGetUserActivityDetail_withFilters_returnsFilteredResults() throws IOException {
    // Given
    File auditFile = createAuditLogFile("audit.log",
        "{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"login\",\"domain\":\"authentication\",\"requestMethod\":\"POST\"," +
            "\"requestUri\":\"/api/v2/auth/login\",\"remoteIpAddress\":\"192.168.1.100\"}",
        "{\"timestamp\":\"2024-03-13T15:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"view\",\"domain\":\"reporting\",\"requestMethod\":\"GET\"," +
            "\"requestUri\":\"/api/v2/applications/123/reports\",\"remoteIpAddress\":\"192.168.1.100\"}",
        "{\"timestamp\":\"2024-03-13T16:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"create\",\"domain\":\"governance\",\"requestMethod\":\"POST\"," +
            "\"requestUri\":\"/api/v2/applications\",\"remoteIpAddress\":\"192.168.1.100\"," +
            "\"error\":\"Validation failed\"}");

    when(mockAuditLogFilesProvider.getAuditLogFiles(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(auditFile));

    // When - Filter by activity type
    ApiUserActivityDetailDTO resultByType = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, List.of("view"), null, null);

    // When - Filter by domain
    ApiUserActivityDetailDTO resultByDomain = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, null, List.of("authentication"), null);

    // When - Filter by specific error status
    ApiUserActivityDetailDTO resultByError = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, null, null, List.of("Validation failed"));

    // When - Filter by success status
    ApiUserActivityDetailDTO resultBySuccess = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, null, null, List.of("Success"));

    // Then - Verify filtering by activity type
    assertThat(resultByType).isNotNull();
    assertThat(resultByType.activities).hasSize(1);
    assertThat(resultByType.activities.get(0).type).isEqualTo("view");
    assertThat(resultByType.activities.get(0).domain).isEqualTo("reporting");

    // Then - Verify filtering by domain
    assertThat(resultByDomain).isNotNull();
    assertThat(resultByDomain.activities).hasSize(1);
    assertThat(resultByDomain.activities.get(0).domain).isEqualTo("authentication");
    assertThat(resultByDomain.activities.get(0).type).isEqualTo("login");

    // Then - Verify filtering by specific error status
    assertThat(resultByError).isNotNull();
    assertThat(resultByError.activities).hasSize(1);
    assertThat(resultByError.activities.get(0).errorType).isEqualTo("Validation failed");
    assertThat(resultByError.activities.get(0).type).isEqualTo("create");

    // Then - Verify filtering by success status
    assertThat(resultBySuccess).isNotNull();
    assertThat(resultBySuccess.activities).hasSize(2); // login and view activities (no errors)
    assertThat(resultBySuccess.activities.get(0).errorType).isNull();
  }

  @Test
  public void testGetUserActivityDetail_withMultiSelectFilters_returnsFilteredResults() throws IOException {
    // Given - Create audit file with various activities
    File auditFile = createAuditLogFile("audit.log",
        "{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"login\",\"domain\":\"authentication\",\"requestMethod\":\"POST\"," +
            "\"requestUri\":\"/api/v2/auth/login\",\"remoteIpAddress\":\"192.168.1.100\"}",
        "{\"timestamp\":\"2024-03-13T15:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"logout\",\"domain\":\"authentication\",\"requestMethod\":\"POST\"," +
            "\"requestUri\":\"/api/v2/auth/logout\",\"remoteIpAddress\":\"192.168.1.100\"}",
        "{\"timestamp\":\"2024-03-13T16:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"view\",\"domain\":\"reporting\",\"requestMethod\":\"GET\"," +
            "\"requestUri\":\"/api/v2/applications/123/reports\",\"remoteIpAddress\":\"192.168.1.100\"}",
        "{\"timestamp\":\"2024-03-13T17:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"create\",\"domain\":\"governance\",\"requestMethod\":\"POST\"," +
            "\"requestUri\":\"/api/v2/applications\",\"remoteIpAddress\":\"192.168.1.100\"," +
            "\"error\":\"Validation failed\"}",
        "{\"timestamp\":\"2024-03-13T18:30:45.123Z\",\"username\":\"john.doe\"," +
            "\"type\":\"update\",\"domain\":\"governance\",\"requestMethod\":\"PUT\"," +
            "\"requestUri\":\"/api/v2/applications/456\",\"remoteIpAddress\":\"192.168.1.100\"," +
            "\"error\":\"Not found\"}");
    when(mockAuditLogFilesProvider.getAuditLogFiles(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(auditFile));

    // When - Filter by multiple activity types
    List<String> activityTypes = List.of("login", "logout");
    ApiUserActivityDetailDTO resultByMultipleTypes = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, activityTypes, null, null);

    // When - Filter by multiple domains
    List<String> domains = List.of("authentication", "reporting");
    ApiUserActivityDetailDTO resultByMultipleDomains = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, null, domains, null);

    // When - Filter by multiple error types
    List<String> errorTypes = List.of("Validation failed", "Not found");
    ApiUserActivityDetailDTO resultByMultipleErrors = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, null, null, errorTypes);

    // When - Filter by combination: multiple activity types + success status
    List<String> mixedErrorTypes = List.of("Success");
    ApiUserActivityDetailDTO resultByCombination = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, activityTypes, null, mixedErrorTypes);

    // Then - Verify filtering by multiple activity types (login, logout)
    assertThat(resultByMultipleTypes).isNotNull();
    assertThat(resultByMultipleTypes.activities).hasSize(2);
    assertThat(resultByMultipleTypes.activities)
        .allSatisfy(activity -> assertThat(activity.type).isIn("login", "logout"));
    assertThat(resultByMultipleTypes.activities)
        .allSatisfy(activity -> assertThat(activity.domain).isEqualTo("authentication"));

    // Then - Verify filtering by multiple domains (authentication, reporting)
    assertThat(resultByMultipleDomains).isNotNull();
    assertThat(resultByMultipleDomains.activities).hasSize(3); // login, logout, view
    assertThat(resultByMultipleDomains.activities)
        .allSatisfy(activity -> assertThat(activity.domain).isIn("authentication", "reporting"));

    // Then - Verify filtering by multiple error types
    assertThat(resultByMultipleErrors).isNotNull();
    assertThat(resultByMultipleErrors.activities).hasSize(2); // create, update
    assertThat(resultByMultipleErrors.activities)
        .allSatisfy(activity -> assertThat(activity.errorType).isIn("Validation failed", "Not found"));

    // Then - Verify combination filtering (login/logout + success)
    assertThat(resultByCombination).isNotNull();
    assertThat(resultByCombination.activities).hasSize(2); // login, logout (both successful)
    assertThat(resultByCombination.activities)
        .allSatisfy(activity -> {
          assertThat(activity.type).isIn("login", "logout");
          assertThat(activity.errorType).isNull(); // Success means no error
        });
  }

  @Test
  public void testExtractTopLevelDomain_withVariousDomainPatterns() {
    // Test top-level domains (should remain unchanged)
    assertThat(UserActivityService.extractTopLevelDomain("authentication")).isEqualTo("authentication");
    assertThat(UserActivityService.extractTopLevelDomain("governance")).isEqualTo("governance");
    assertThat(UserActivityService.extractTopLevelDomain("reporting")).isEqualTo("reporting");

    // Test two-level domains (should be truncated to top level)
    assertThat(UserActivityService.extractTopLevelDomain("security.user")).isEqualTo("security");
    assertThat(UserActivityService.extractTopLevelDomain("governance.policy")).isEqualTo("governance");
    assertThat(UserActivityService.extractTopLevelDomain("reporting.dashboard")).isEqualTo("reporting");

    // Test three-level domains (should be truncated to top level)
    assertThat(UserActivityService.extractTopLevelDomain("governance.component.vulnerability"))
        .isEqualTo("governance");
    assertThat(UserActivityService.extractTopLevelDomain("security.ldap.server"))
        .isEqualTo("security");
    assertThat(UserActivityService.extractTopLevelDomain("reporting.dashboard.filter"))
        .isEqualTo("reporting");

    // Test four+ level domains (should be truncated to top level)
    assertThat(UserActivityService.extractTopLevelDomain("governance.component.obligation.attribution"))
        .isEqualTo("governance");
    assertThat(UserActivityService.extractTopLevelDomain("security.ldap.server.connection"))
        .isEqualTo("security");
    assertThat(UserActivityService.extractTopLevelDomain("reporting.dashboard.component.details"))
        .isEqualTo("reporting");
  }

  @Test
  public void testGetFilterOptions_containsTopLevelDomains() {
    // When
    ApiUserActivityFilterOptionsDTO result = userActivityService.getFilterOptions();

    // Then - Verify we have top-level domains
    assertThat(result.domains).isNotNull().isNotEmpty();

    // Verify some expected top-level domains are present
    assertThat(result.domains).contains("governance", "security", "reporting");

    // Verify domains are sorted
    assertThat(result.domains).isSorted();

    // Verify no null or empty values
    assertThat(result.domains).doesNotContainNull();
    assertThat(result.domains).allSatisfy(domain -> assertThat(domain).isNotEmpty());

    // Verify domain count is significantly reduced - should be around 10-15 top-level domains
    assertThat(result.domains.size()).isBetween(8, 20);

    // Verify all domains are top-level (no dots)
    assertThat(result.domains).allSatisfy(domain -> {
      long dotCount = domain.chars().filter(ch -> ch == '.').count();
      assertThat(dotCount).isEqualTo(0);
    });
  }

  @Test
  public void testGetUserActivityDetail_withTopLevelDomainFiltering() throws IOException {
    // Given - Create audit file with multi-level domain events
    File auditFile = createMultiDomainFilterScenario();
    setupMockAuditFiles(auditFile);

    // When - Filter by top-level domain "governance" (should match both governance events)
    List<String> governanceDomain = List.of("governance");
    ApiUserActivityDetailDTO governanceResult = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, null,
        governanceDomain, null);

    // When - Filter by top-level domain "reporting"
    List<String> reportingDomain = List.of("reporting");
    ApiUserActivityDetailDTO reportingResult = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, null,
        reportingDomain, null);

    // When - Filter by multiple top-level domains
    List<String> multipleDomains = List.of("governance", "security");
    ApiUserActivityDetailDTO multipleResult = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "john.doe", 100, 0, null,
        multipleDomains, null);

    // Then - Verify governance filter matches both governance events
    assertThat(governanceResult).isNotNull();
    assertThat(governanceResult.activities).hasSize(2);
    assertThat(governanceResult.activities)
        .allSatisfy(activity -> assertThat(activity.domain).startsWith("governance"));

    // Then - Verify reporting filter matches reporting event
    assertThat(reportingResult).isNotNull();
    assertThat(reportingResult.activities).hasSize(1);
    assertThat(reportingResult.activities.get(0).domain).isEqualTo("reporting.dashboard.filter");

    // Then - Verify multiple domain filter works correctly
    assertThat(multipleResult).isNotNull();
    assertThat(multipleResult.activities).hasSize(3); // 2 governance + 1 security
    assertThat(multipleResult.activities).allSatisfy(activity -> assertThat(activity.domain).satisfiesAnyOf(
        domain -> assertThat(domain).startsWith("governance"),
        domain -> assertThat(domain).startsWith("security")));
  }

  @Test
  public void testApiActivityEventDTO_includesUsernameField() throws IOException {
    File auditFile = createAuditLogFile("audit.log",
        "{\"timestamp\":\"2024-03-13T14:30:45.123Z\",\"username\":\"test.user\"," +
            "\"type\":\"login\",\"domain\":\"authentication\",\"requestMethod\":\"POST\"," +
            "\"requestUri\":\"/api/v2/auth/login\",\"remoteIpAddress\":\"192.168.1.100\"," +
            "\"userAgent\":\"Mozilla/5.0\",\"error\":null}");

    when(mockAuditLogFilesProvider.getAuditLogFiles(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(auditFile));

    List<ApiActivityEventDTO> exportResult;
    try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2024-03-10", "2024-03-13", null, null, null, Set.of(), Set.of(), Set.of()))
    {
      exportResult = stream.collect(Collectors.toList());
    }

    var detailResult = userActivityService.getUserActivityDetail(
        "2024-03-10", "2024-03-13", "test.user", 100, 0, null, null, null);

    assertThat(exportResult).hasSize(1);
    assertThat(exportResult.get(0).username).isEqualTo("test.user");
    assertThat(exportResult.get(0).timestamp).isEqualTo("2024-03-13T14:30:45.123Z");

    assertThat(detailResult.activities).hasSize(1);
    assertThat(detailResult.activities.get(0).username).isEqualTo("test.user");
    assertThat(detailResult.activities.get(0).timestamp).isEqualTo("2024-03-13T14:30:45.123Z");
  }

  @Test
  public void testStreamAllUserActivitiesForExport_returnsAllMatchingEventsAcrossFiles() throws Exception {
    // Given two audit files, one with 3 events for 'alice' and one with 2 events for 'bob'
    File file1 = givenAuditFile("audit-1.json",
        auditLine("alice", "2026-05-01T10:00:00Z"),
        auditLine("alice", "2026-05-01T10:05:00Z"),
        auditLine("alice", "2026-05-01T10:10:00Z"));
    File file2 = givenAuditFile("audit-2.json",
        auditLine("bob", "2026-05-02T09:00:00Z"),
        auditLine("bob", "2026-05-02T09:05:00Z"));
    when(mockAuditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(List.of(file1, file2));

    try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2026-05-01", "2026-05-20", null, null, null, Set.of(), Set.of(), Set.of()))
    {
      List<ApiActivityEventDTO> events = stream.collect(Collectors.toList());
      assertThat(events).hasSize(5);
      assertThat(events).filteredOn(e -> "alice".equals(e.username)).hasSize(3);
      assertThat(events).filteredOn(e -> "bob".equals(e.username)).hasSize(2);
      assertThat(events).extracting(e -> e.type).containsOnly("login");
      assertThat(events).extracting(e -> e.domain).containsOnly("security.user");
    }
  }

  @Test
  public void testStreamAuditFileForExport_releasesLockAndPropagatesWhenLockAcquisitionThrowsRuntimeException() throws Exception {
    File file = givenAuditFile("audit-runtime-fail.json", auditLine("alice", "2026-05-01T10:00:00Z"));
    when(mockAuditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(List.of(file));

    ClusterLock failingLock = mock(ClusterLock.class);
    doThrow(new IllegalStateException("simulated lock failure")).when(failingLock).lock();
    when(mockClusterLockManager.createForAuditJsonFileStore(anyString())).thenReturn(failingLock);

    // Lock is still released (no leak), but the exception now propagates so the caller
    // gets a 500 instead of an empty CSV. STL feedback: surface real errors.
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
          "2026-05-01", "2026-05-20", null, null, null, Set.of(), Set.of(), Set.of()))
      {
        stream.count();
      }
    }).withMessage("simulated lock failure");

    verify(failingLock).close(); // D5: lock released on the RuntimeException path
  }

  @Test
  public void testStreamAuditFileForExport_releasesLockWhenAuditFileCannotBeOpened() throws Exception {
    File missing = new File("/tmp/does-not-exist-" + System.nanoTime() + ".json");
    when(mockAuditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(List.of(missing));

    ClusterLock mockLock = mock(ClusterLock.class);
    when(mockClusterLockManager.createForAuditJsonFileStore(anyString())).thenReturn(mockLock);

    try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2026-05-01", "2026-05-20", null, null, null, Set.of(), Set.of(), Set.of()))
    {
      assertThat(stream.count()).isZero();
    }

    verify(mockLock).close();
  }

  @Test
  public void testStreamAuditFileForExport_releasesLockWhenStreamIsClosedEarly() throws Exception {
    File file = givenAuditFile("audit-many.json",
        auditLine("alice", "2026-05-01T10:00:00Z"),
        auditLine("alice", "2026-05-01T10:01:00Z"),
        auditLine("alice", "2026-05-01T10:02:00Z"));
    when(mockAuditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(List.of(file));

    ClusterLock mockLock = mock(ClusterLock.class);
    when(mockClusterLockManager.createForAuditJsonFileStore(anyString())).thenReturn(mockLock);

    Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2026-05-01", "2026-05-20", null, null, null, Set.of(), Set.of(), Set.of());
    // consumer reads one then abandons. close() must be called manually because Java
    // streams are not auto-closed by terminal operations like findFirst() — onClose() handlers
    // only run on close().
    stream.findFirst();
    stream.close();

    verify(mockLock, times(1)).close();
  }

  @Test
  public void testStreamAllUserActivitiesForExport_filtersByActivityType() throws Exception {
    File file = givenAuditFile("audit-filter-type.json",
        auditLineFull("alice", "2026-05-01T10:00:00Z", "security.user", "LOGIN", null),
        auditLineFull("alice", "2026-05-01T10:05:00Z", "governance.application", "VIEW_APPLICATION", null),
        auditLineFull("alice", "2026-05-01T10:10:00Z", "evaluation.application", "EVALUATE_APPLICATION", null));
    when(mockAuditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(List.of(file));

    try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2026-05-01", "2026-05-20", null, null, null, Set.of("LOGIN"), Set.of(), Set.of()))
    {
      List<ApiActivityEventDTO> events = stream.collect(Collectors.toList());
      assertThat(events).hasSize(1);
      assertThat(events.get(0).type).isEqualTo("LOGIN");
    }
  }

  @Test
  public void testStreamAllUserActivitiesForExport_filtersByDomainTopLevel() throws Exception {
    File file = givenAuditFile("audit-filter-domain.json",
        auditLineFull("alice", "2026-05-01T10:00:00Z", "security.user", "LOGIN", null),
        auditLineFull("alice", "2026-05-01T10:05:00Z", "governance.application", "VIEW_APPLICATION", null),
        auditLineFull("alice", "2026-05-01T10:10:00Z", "security.role", "VIEW_ROLE", null));
    when(mockAuditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(List.of(file));

    try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2026-05-01", "2026-05-20", null, null, null, Set.of(), Set.of("security"), Set.of()))
    {
      List<ApiActivityEventDTO> events = stream.collect(Collectors.toList());
      // top-level "security" matches both "security.user" and "security.role" but NOT "governance.application"
      assertThat(events).hasSize(2);
      assertThat(events).allSatisfy(e -> assertThat(e.domain).startsWith("security."));
    }
  }

  @Test
  public void testStreamAllUserActivitiesForExport_filtersByErrorTypeIncludingSuccess() throws Exception {
    File file = givenAuditFile("audit-filter-error.json",
        auditLineFull("alice", "2026-05-01T10:00:00Z", "security.user", "LOGIN", null), // success
        auditLineFull("alice", "2026-05-01T10:05:00Z", "security.user", "LOGIN", "bad-credentials"),
        auditLineFull("alice", "2026-05-01T10:10:00Z", "security.user", "LOGIN", "rate-limited"));
    when(mockAuditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(List.of(file));

    // Success-only — error == null
    try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2026-05-01", "2026-05-20", null, null, null, Set.of(), Set.of(), Set.of("Success")))
    {
      List<ApiActivityEventDTO> events = stream.collect(Collectors.toList());
      assertThat(events).hasSize(1);
      assertThat(events.get(0).errorType).isNull();
    }

    // Specific error type
    try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2026-05-01", "2026-05-20", null, null, null, Set.of(), Set.of(), Set.of("bad-credentials")))
    {
      List<ApiActivityEventDTO> events = stream.collect(Collectors.toList());
      assertThat(events).hasSize(1);
      assertThat(events.get(0).errorType).isEqualTo("bad-credentials");
    }

    // Multiple types in OR
    try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2026-05-01", "2026-05-20", null, null, null, Set.of(), Set.of(), Set.of("bad-credentials", "rate-limited")))
    {
      List<ApiActivityEventDTO> events = stream.collect(Collectors.toList());
      assertThat(events).hasSize(2);
    }
  }

  @Test
  public void testStreamAllUserActivitiesForExport_emptyResultWhenFilterMatchesNothing() throws Exception {
    File file = givenAuditFile("audit-filter-no-match.json",
        auditLineFull("alice", "2026-05-01T10:00:00Z", "security.user", "LOGIN", null));
    when(mockAuditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(List.of(file));

    try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2026-05-01", "2026-05-20", null, null, null, Set.of("DOES_NOT_EXIST"), Set.of(), Set.of()))
    {
      assertThat(stream.count()).isZero();
    }
  }

  // Helper methods for testing

  private File givenAuditFile(String name, String... jsonLines) throws IOException {
    File f = java.nio.file.Files.createFile(tempFolder.toPath().resolve(name)).toFile();
    Files.writeString(f.toPath(), String.join("\n", jsonLines));
    return f;
  }

  private static String auditLine(String username, String timestampIso) {
    return auditLineFull(username, timestampIso, "security.user", "login", null);
  }

  private static String auditLineFull(String username, String timestampIso, String domain, String type, String error) {
    StringBuilder sb = new StringBuilder("{");
    sb.append(String.format("\"username\":\"%s\",", username));
    sb.append(String.format("\"timestamp\":\"%s\",", timestampIso));
    sb.append(String.format("\"domain\":\"%s\",", domain));
    sb.append(String.format("\"type\":\"%s\"", type));
    if (error != null) {
      sb.append(String.format(",\"error\":\"%s\"", error));
    }
    sb.append("}");
    return sb.toString();
  }

  private File createAuditLogFile(String filename, String... lines) throws IOException {
    Path auditFile = java.nio.file.Files.createFile(tempFolder.toPath().resolve(filename));
    Files.write(auditFile, List.of(lines));
    return auditFile.toFile();
  }
}
