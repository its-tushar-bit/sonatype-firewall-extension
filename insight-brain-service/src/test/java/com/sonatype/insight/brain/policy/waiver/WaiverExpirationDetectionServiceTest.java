/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.webhook.WaiverExpirationEvent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WaiverExpirationDetectionService.
 *
 * @since 1.179.0
 */
@ExtendWith(MockitoExtension.class)
public class WaiverExpirationDetectionServiceTest
{
  @Mock
  private PolicyWaiverDAO policyWaiverDAO;

  @Mock
  private PolicyDAO policyDAO;

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private RepositoryDAO repositoryDAO;

  @Mock
  private RepositoryManagerDAO repositoryManagerDAO;

  @Mock
  private AsyncEventBus asyncEventBus;

  @Mock
  private BaseUrl baseUrl;

  @Mock
  private ProductLicense productLicense;

  @Mock
  private WaiverExpirationEmailer waiverExpirationEmailer;

  @Mock
  private ApiWaiverExpirationNotificationConfigService notificationConfigService;

  @Mock
  private WaiverExpirationNotificationConfigDAO notificationConfigDAO;

  @Mock
  private TransactionContext transactionContext;

  @Mock
  private Policy mockPolicy;

  @Mock
  private Application mockApplication;

  private WaiverExpirationDetectionService service;

  @BeforeEach
  public void setUp() {
    service = spy(new WaiverExpirationDetectionService(
        policyWaiverDAO,
        policyDAO,
        applicationDAO,
        organizationDAO,
        repositoryDAO,
        repositoryManagerDAO,
        asyncEventBus,
        baseUrl,
        productLicense,
        waiverExpirationEmailer,
        notificationConfigService,
        notificationConfigDAO));

    // Email section: no notification days configured by default — keeps tests focused on webhooks
    lenient().when(notificationConfigDAO.findAllNotificationDays()).thenReturn(Collections.emptyList());

    lenient().when(policyWaiverDAO.createTransactionContext()).thenReturn(transactionContext);
    lenient().when(baseUrl.get()).thenReturn("http://localhost:8070");
    lenient().when(productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES)).thenReturn(true);

    // Stub owner DAOs to return empty lists (tests focus on applications)
    // Mark as lenient since not all tests exercise this code path
    lenient().when(organizationDAO.getByIds(anySet())).thenReturn(Collections.emptyList());
    lenient().when(repositoryDAO.getByIds(anySet())).thenReturn(Collections.emptyList());
    lenient().when(repositoryManagerDAO.getByIds(anySet())).thenReturn(Collections.emptyList());
  }

  @Test
  public void testDetectsWaiversExpiringIn24Hours() throws Exception {
    // Given: A waiver that will expire in 24 hours (created 30 days ago, expires in 24 hours)
    Date now = new Date(System.currentTimeMillis());
    Date createTime = new Date(now.getTime() - (30L * 24 * 60 * 60 * 1000)); // Created 30 days ago
    Date expiryTime = new Date(now.getTime() + (24 * 60 * 60 * 1000)); // Expires in 24 hours
    PolicyWaiver expiring24HourWaiver = createTestWaiverWithCreateTime("waiver-1", createTime, expiryTime);

    when(policyWaiverDAO.getUpcomingExpiringWaivers(eq(transactionContext), any(Date.class), any(Date.class)))
        .thenReturn(Collections.emptyList()) // 7-day window
        .thenReturn(Collections.singletonList(expiring24HourWaiver)); // 24-hour window

    // Mock policy and application lookups - using batch getByIds() after N+1 optimization
    when(policyDAO.getByIds(anySet())).thenReturn(List.of(mockPolicy));
    when(mockPolicy.getId()).thenReturn("policy-1");
    when(mockPolicy.getName()).thenReturn("Test Policy");
    when(mockPolicy.getThreatLevel()).thenReturn(7);

    when(applicationDAO.getByIds(anySet())).thenReturn(List.of(mockApplication));
    when(mockApplication.getId()).thenReturn("app-1");
    when(mockApplication.getPublicId()).thenReturn("app-public-1");
    when(mockApplication.getName()).thenReturn("Test Application");
    when(mockApplication.getType()).thenReturn(OwnerType.APPLICATION);

    // When: Service runs
    service.run();

    // Then: Event is posted with EXPIRING_IN_24_HOURS status
    ArgumentCaptor<WaiverExpirationEvent> eventCaptor = ArgumentCaptor.forClass(WaiverExpirationEvent.class);
    verify(asyncEventBus, times(1)).post(eventCaptor.capture());

    WaiverExpirationEvent event = eventCaptor.getValue();
    assertThat(event.status).isEqualTo("EXPIRING_IN_24_HOURS");
    assertThat(event.waiverId).isEqualTo("waiver-1");
    assertThat(event.policyName).isEqualTo("Test Policy");
    assertThat(event.applicationName).isEqualTo("Test Application");
  }

  @Test
  public void testDetectsWaiversExpiringIn7Days() throws Exception {
    // Given: A waiver that will expire in 7 days (created 30 days ago, expires in 7 days)
    Date now = new Date(System.currentTimeMillis());
    Date createTime = new Date(now.getTime() - (30L * 24 * 60 * 60 * 1000)); // Created 30 days ago
    Date expiryTime = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000)); // Expires in 7 days
    PolicyWaiver expiring7DayWaiver = createTestWaiverWithCreateTime("waiver-2", createTime, expiryTime);

    when(policyWaiverDAO.getUpcomingExpiringWaivers(eq(transactionContext), any(Date.class), any(Date.class)))
        .thenReturn(Collections.singletonList(expiring7DayWaiver)) // 7-day window
        .thenReturn(Collections.emptyList()); // 24-hour window

    // Mock policy and application lookups - using batch getByIds() after N+1 optimization
    when(policyDAO.getByIds(anySet())).thenReturn(List.of(mockPolicy));
    when(mockPolicy.getId()).thenReturn("policy-1");
    when(mockPolicy.getName()).thenReturn("Test Policy");
    when(mockPolicy.getThreatLevel()).thenReturn(7);

    when(applicationDAO.getByIds(anySet())).thenReturn(List.of(mockApplication));
    when(mockApplication.getId()).thenReturn("app-1");
    when(mockApplication.getPublicId()).thenReturn("app-public-1");
    when(mockApplication.getName()).thenReturn("Test Application");
    when(mockApplication.getType()).thenReturn(OwnerType.APPLICATION);

    // When: Service runs
    service.run();

    // Then: Event is posted with EXPIRING_IN_7_DAYS status
    ArgumentCaptor<WaiverExpirationEvent> eventCaptor = ArgumentCaptor.forClass(WaiverExpirationEvent.class);
    verify(asyncEventBus, times(1)).post(eventCaptor.capture());

    WaiverExpirationEvent event = eventCaptor.getValue();
    assertThat(event.status).isEqualTo("EXPIRING_IN_7_DAYS");
    assertThat(event.waiverId).isEqualTo("waiver-2");
  }

  @Test
  public void testDetectsBoth7DayAnd24HourWaivers() throws Exception {
    // Given: One waiver expiring in 7 days and one expiring in 24 hours
    Date now = new Date(System.currentTimeMillis());
    Date createTime = new Date(now.getTime() - (30L * 24 * 60 * 60 * 1000)); // Created 30 days ago
    Date expiryTime7Days = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000)); // Expires in 7 days
    Date expiryTime24Hours = new Date(now.getTime() + (24 * 60 * 60 * 1000)); // Expires in 24 hours

    PolicyWaiver expiring7DayWaiver = createTestWaiverWithCreateTime("waiver-1", createTime, expiryTime7Days);
    PolicyWaiver expiring24HourWaiver = createTestWaiverWithCreateTime("waiver-2", createTime, expiryTime24Hours);

    when(policyWaiverDAO.getUpcomingExpiringWaivers(eq(transactionContext), any(Date.class), any(Date.class)))
        .thenReturn(Collections.singletonList(expiring7DayWaiver)) // 7-day window
        .thenReturn(Collections.singletonList(expiring24HourWaiver)); // 24-hour window

    // Mock policy and application lookups - using batch getByIds() after N+1 optimization
    when(policyDAO.getByIds(anySet())).thenReturn(List.of(mockPolicy));
    when(mockPolicy.getId()).thenReturn("policy-1");
    when(mockPolicy.getName()).thenReturn("Test Policy");
    when(mockPolicy.getThreatLevel()).thenReturn(7);

    when(applicationDAO.getByIds(anySet())).thenReturn(List.of(mockApplication));
    when(mockApplication.getId()).thenReturn("app-1");
    when(mockApplication.getPublicId()).thenReturn("app-public-1");
    when(mockApplication.getName()).thenReturn("Test Application");
    when(mockApplication.getType()).thenReturn(OwnerType.APPLICATION);

    // When: Service runs
    service.run();

    // Then: Two events are posted, one for each status
    ArgumentCaptor<WaiverExpirationEvent> eventCaptor = ArgumentCaptor.forClass(WaiverExpirationEvent.class);
    verify(asyncEventBus, times(2)).post(eventCaptor.capture());

    List<WaiverExpirationEvent> events = eventCaptor.getAllValues();
    assertThat(events).hasSize(2);
    assertThat(events.get(0).status).isEqualTo("EXPIRING_IN_7_DAYS");
    assertThat(events.get(0).waiverId).isEqualTo("waiver-1");
    assertThat(events.get(1).status).isEqualTo("EXPIRING_IN_24_HOURS");
    assertThat(events.get(1).waiverId).isEqualTo("waiver-2");
  }

  @Test
  public void testHandlesNoWaivers() throws Exception {
    // Given: No waivers found in either window
    when(policyWaiverDAO.getUpcomingExpiringWaivers(eq(transactionContext), any(Date.class), any(Date.class)))
        .thenReturn(Collections.emptyList());

    // When: Service runs
    service.run();

    // Then: No events are posted
    verify(asyncEventBus, times(0)).post(any(WaiverExpirationEvent.class));
  }

  @Test
  public void testExtractsComponentFormatFromPurl() throws Exception {
    // Given: A waiver with a package URL expiring in 24 hours
    Date now = new Date(System.currentTimeMillis());
    Date createTime = new Date(now.getTime() - (30L * 24 * 60 * 60 * 1000)); // Created 30 days ago
    Date expiryTime = new Date(now.getTime() + (24 * 60 * 60 * 1000)); // Expires in 24 hours
    PolicyWaiver waiver = createTestWaiverWithCreateTime("waiver-1", createTime, expiryTime);
    waiver.setAssociatedPackageUrl("pkg:maven/com.example/my-component@1.0.0");

    when(policyWaiverDAO.getUpcomingExpiringWaivers(eq(transactionContext), any(Date.class), any(Date.class)))
        .thenReturn(Collections.emptyList()) // 7-day window
        .thenReturn(Collections.singletonList(waiver)); // 24-hour window

    // Mock policy and application lookups - using batch getByIds() after N+1 optimization
    when(policyDAO.getByIds(anySet())).thenReturn(List.of(mockPolicy));
    when(mockPolicy.getId()).thenReturn("policy-1");
    when(mockPolicy.getName()).thenReturn("Test Policy");
    when(applicationDAO.getByIds(anySet())).thenReturn(List.of(mockApplication));
    when(mockApplication.getId()).thenReturn("app-1");
    when(mockApplication.getPublicId()).thenReturn("app-public-1");

    // When: Service runs
    service.run();

    // Then: Event has correct component format extracted
    ArgumentCaptor<WaiverExpirationEvent> eventCaptor = ArgumentCaptor.forClass(WaiverExpirationEvent.class);
    verify(asyncEventBus, times(1)).post(eventCaptor.capture());

    WaiverExpirationEvent event = eventCaptor.getValue();
    assertThat(event.componentFormat).isEqualTo("maven");
    assertThat(event.componentPackageUrl).isEqualTo("pkg:maven/com.example/my-component@1.0.0");
  }

  @Test
  public void testSevenDayWaiverGetsOnly24HourNotice() throws Exception {
    // Given: A waiver with 7-day duration (created today, expires in 7 days)
    // Should NOT get 7-day notice, only 24-hour notice
    Date now = new Date(System.currentTimeMillis());
    Date createTime = new Date(now.getTime() - (6 * 24 * 60 * 60 * 1000)); // Created 6 days ago
    // Expires in 24 hours (total duration = 7 days)
    Date expiryTime7Days = new Date(now.getTime() + (24 * 60 * 60 * 1000));
    PolicyWaiver sevenDayWaiver = createTestWaiverWithCreateTime("waiver-short", createTime, expiryTime7Days);

    when(policyWaiverDAO.getUpcomingExpiringWaivers(eq(transactionContext), any(Date.class), any(Date.class)))
        .thenReturn(Collections.emptyList()) // 7-day window - waiver has duration <= 7, so skipped
        .thenReturn(Collections.singletonList(sevenDayWaiver)); // 24-hour window

    // Mock policy and application lookups - using batch getByIds() after N+1 optimization
    when(policyDAO.getByIds(anySet())).thenReturn(List.of(mockPolicy));
    when(mockPolicy.getId()).thenReturn("policy-1");
    when(mockPolicy.getName()).thenReturn("Test Policy");
    when(mockPolicy.getThreatLevel()).thenReturn(7);

    when(applicationDAO.getByIds(anySet())).thenReturn(List.of(mockApplication));
    when(mockApplication.getId()).thenReturn("app-1");
    when(mockApplication.getPublicId()).thenReturn("app-public-1");
    when(mockApplication.getName()).thenReturn("Test Application");
    when(mockApplication.getType()).thenReturn(OwnerType.APPLICATION);

    // When: Service runs
    service.run();

    // Then: Only 24-hour notice is sent (no 7-day notice)
    ArgumentCaptor<WaiverExpirationEvent> eventCaptor = ArgumentCaptor.forClass(WaiverExpirationEvent.class);
    verify(asyncEventBus, times(1)).post(eventCaptor.capture());

    WaiverExpirationEvent event = eventCaptor.getValue();
    assertThat(event.status).isEqualTo("EXPIRING_IN_24_HOURS");
    assertThat(event.waiverId).isEqualTo("waiver-short");
  }

  @Test
  public void testThirtyDayWaiverGetsBothNotices() throws Exception {
    // Given: A waiver with 30-day duration
    // Should get BOTH 7-day and 24-hour notices
    Date now = new Date(System.currentTimeMillis());
    Date createTime = new Date(now.getTime() - (23L * 24 * 60 * 60 * 1000)); // Created 23 days ago

    // Create two instances of the same waiver for different detection windows
    Date expiryTime7Days = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000)); // For 7-day detection
    Date expiryTime24Hours = new Date(now.getTime() + (24 * 60 * 60 * 1000)); // For 24-hour detection

    PolicyWaiver thirtyDayWaiver7Day = createTestWaiverWithCreateTime("waiver-long", createTime, expiryTime7Days);
    PolicyWaiver thirtyDayWaiver24Hour = createTestWaiverWithCreateTime("waiver-long", createTime, expiryTime24Hours);

    when(policyWaiverDAO.getUpcomingExpiringWaivers(eq(transactionContext), any(Date.class), any(Date.class)))
        .thenReturn(Collections.singletonList(thirtyDayWaiver7Day)) // 7-day window
        .thenReturn(Collections.singletonList(thirtyDayWaiver24Hour)); // 24-hour window

    // Mock policy and application lookups - using batch getByIds() after N+1 optimization
    when(policyDAO.getByIds(anySet())).thenReturn(List.of(mockPolicy));
    when(mockPolicy.getId()).thenReturn("policy-1");
    when(mockPolicy.getName()).thenReturn("Test Policy");
    when(mockPolicy.getThreatLevel()).thenReturn(7);

    when(applicationDAO.getByIds(anySet())).thenReturn(List.of(mockApplication));
    when(mockApplication.getId()).thenReturn("app-1");
    when(mockApplication.getPublicId()).thenReturn("app-public-1");
    when(mockApplication.getName()).thenReturn("Test Application");
    when(mockApplication.getType()).thenReturn(OwnerType.APPLICATION);

    // When: Service runs
    service.run();

    // Then: Both 7-day and 24-hour notices are sent
    ArgumentCaptor<WaiverExpirationEvent> eventCaptor = ArgumentCaptor.forClass(WaiverExpirationEvent.class);
    verify(asyncEventBus, times(2)).post(eventCaptor.capture());

    List<WaiverExpirationEvent> events = eventCaptor.getAllValues();
    assertThat(events).hasSize(2);
    assertThat(events.get(0).status).isEqualTo("EXPIRING_IN_7_DAYS");
    assertThat(events.get(0).waiverId).isEqualTo("waiver-long");
    assertThat(events.get(1).status).isEqualTo("EXPIRING_IN_24_HOURS");
    assertThat(events.get(1).waiverId).isEqualTo("waiver-long");
  }

  @Test
  public void testSkipsWaiverWithNullCreateTime() throws Exception {
    // Given: A waiver with null createTime (should be skipped)
    Date now = new Date(System.currentTimeMillis());
    Date expiryTime = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000));
    PolicyWaiver waiverWithNullCreateTime = createTestWaiver("waiver-null-create", expiryTime);
    // createTime is null by default in createTestWaiver

    when(policyWaiverDAO.getUpcomingExpiringWaivers(eq(transactionContext), any(Date.class), any(Date.class)))
        .thenReturn(Collections.singletonList(waiverWithNullCreateTime)) // 7-day window
        .thenReturn(Collections.emptyList()); // 24-hour window

    // When: Service runs
    service.run();

    // Then: No events are posted (waiver is skipped)
    verify(asyncEventBus, times(0)).post(any(WaiverExpirationEvent.class));
  }

  @Test
  public void testSkipsWaiverWithNullExpiryTime() throws Exception {
    // Given: A waiver with null expiryTime (should be skipped)
    Date now = new Date(System.currentTimeMillis());
    Date createTime = new Date(now.getTime() - (30L * 24 * 60 * 60 * 1000));
    PolicyWaiver waiverWithNullExpiry = createTestWaiverWithCreateTime("waiver-null-expiry", createTime, null);

    when(policyWaiverDAO.getUpcomingExpiringWaivers(eq(transactionContext), any(Date.class), any(Date.class)))
        .thenReturn(Collections.singletonList(waiverWithNullExpiry)) // 7-day window
        .thenReturn(Collections.emptyList()); // 24-hour window

    // When: Service runs
    service.run();

    // Then: No events are posted (waiver is skipped)
    verify(asyncEventBus, times(0)).post(any(WaiverExpirationEvent.class));
  }

  @Test
  public void testRepositoryContainerOwnerResolvedCorrectlyInEmailPath() throws Exception {
    // Given: A waiver scoped to the Repository Container (ownerId = "REPOSITORY_CONTAINER_ID")
    // expiring in 1 day, with email notification configured for day 1
    Date now = new Date(System.currentTimeMillis());
    Date createTime = new Date(now.getTime() - (30L * 24 * 60 * 60 * 1000));
    Date expiryTime = new Date(now.getTime() + (24 * 60 * 60 * 1000));
    PolicyWaiver waiver = createTestWaiverWithCreateTime("waiver-repo-container", createTime, expiryTime);
    waiver.setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    // Email path: notificationConfigDAO returns day=1 so the email path is triggered
    when(notificationConfigDAO.findAllNotificationDays()).thenReturn(List.of("1"));

    // getUpcomingExpiringWaivers for the email path (single call covering today+1 to today+maxDay+1)
    when(policyWaiverDAO.getUpcomingExpiringWaivers(eq(transactionContext), any(Date.class), any(Date.class)))
        .thenReturn(Collections.emptyList()) // webhook 7-day window
        .thenReturn(Collections.emptyList()) // webhook 24-hour window
        .thenReturn(Collections.singletonList(waiver)); // email path window

    when(policyDAO.getByIds(anySet())).thenReturn(List.of(mockPolicy));
    when(mockPolicy.getId()).thenReturn("policy-1");
    when(mockPolicy.getName()).thenReturn("Test Policy");
    when(mockPolicy.getThreatLevel()).thenReturn(7);

    // applicationDAO returns nothing — owner is repository container, not application
    when(applicationDAO.getByIds(anySet())).thenReturn(Collections.emptyList());

    // Config service returns a config that includes day 1
    ApiWaiverExpirationNotificationConfigDTO config = new ApiWaiverExpirationNotificationConfigDTO();
    config.setNotificationDays(List.of(1));
    config.setRecipientType("DIRECT");
    config.setDirectEmails(List.of("test@example.com"));
    when(notificationConfigService.getConfig(RepositoryContainer.REPOSITORY_CONTAINER_ID)).thenReturn(config);

    // When: Service runs
    service.run();

    // Then: Email is sent — capture the event passed to the emailer and verify owner name is resolved
    ArgumentCaptor<WaiverExpirationEvent> eventCaptor = ArgumentCaptor.forClass(WaiverExpirationEvent.class);
    verify(waiverExpirationEmailer, times(1)).send(eventCaptor.capture(), any());

    WaiverExpirationEvent event = eventCaptor.getValue();
    assertThat(event.applicationName).isEqualTo("Repository Managers");
    assertThat(event.iqReportUrl).isNull(); // No report URL for non-application owners
  }

  private PolicyWaiver createTestWaiver(String id, Date expiryTime) {
    PolicyWaiver waiver = new PolicyWaiver();
    waiver.setId(id);
    waiver.setExpiryTime(expiryTime);
    waiver.setPolicyId("policy-1");
    waiver.setOwnerId("app-1");
    waiver.setComment("Test waiver");
    waiver.setCreatorId("test-user");
    waiver.setCreatorName("test@example.com");
    waiver.setAssociatedPackageUrl("pkg:maven/com.example/test@1.0.0");
    return waiver;
  }

  private PolicyWaiver createTestWaiverWithCreateTime(String id, Date createTime, Date expiryTime) {
    PolicyWaiver waiver = createTestWaiver(id, expiryTime);
    waiver.setCreateTime(createTime);
    return waiver;
  }
}
