/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_CURRENT_VERSION_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionAcquire;
import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionGenerateTokens;
import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionGenerateTokensResponse;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrlConfiguration;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.solution.Solution;
import com.sonatype.insight.brain.solution.SolutionResolver;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.util.HashUtils;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

public class EnterpriseReportingServiceTest
    extends AbstractComponentTest
{
  private final String embedDomain = "http%3A%2F%2Flocalhost%3A8070";

  private static final List<DashboardGroupMetadataDTO> DASHBOARD_GROUP_METADATA = Collections.emptyList();

  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private CurrentUser mockCurrentUser;

  @Mock
  private MembershipMappingService mockMembershipMappingService;

  @Mock
  private UserDAO mockUserDAO;

  @Mock
  private SsoUserService mockSsoUserService;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private Configuration mockConfiguration;

  @Mock
  private SolutionResolver mockSolutionResolver;

  private EnterpriseReportingService enterpriseReportingService;

  @Inject
  private ApplicationService applicationService;

  @Inject
  private InsightWork insightWork;

  @Before
  public void setUpEnterpriseReportingService() {
    setupMockConfigurationInstance();
    enterpriseReportingService = new EnterpriseReportingService(
        mockHdsClient,
        mockCurrentUser,
        mockUserDAO,
        mockSsoUserService,
        mockMembershipMappingService,
        applicationService,
        insightWork,
        mockTaskScheduler,
        mockConfiguration,
        mockSolutionResolver);
  }

  private void setupMockConfigurationInstance() {
    lenient().when(mockConfiguration.getEventBusMaxThreadPoolSize()).thenReturn(1);
    lenient().when(mockConfiguration.getEnterpriseReportingVersionCacheExpirationInMinutes()).thenReturn(1);
  }

  @Test
  public void testGetBaseUrl() {
    String expectedBaseUrl = "https://sonatypeexternaldev.cloud.looker.com/";
    when(mockHdsClient.get(EnterpriseReportingConfigDTO.class,
        EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH))
            .thenReturn(new EnterpriseReportingConfigDTO(expectedBaseUrl));
    assertThat(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).isEqualTo(expectedBaseUrl);
  }

  @Test
  public void testGetBaseUrl_Multitenant() {
    String expectedTenant1BaseUrl = "https://sonatypeexternaldev.cloud.looker.com/";
    String expectedTenant2BaseUrl = "https://sonatypeexternaldev.us-east.cloud.looker.com/";
    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      when(mockHdsClient.get(EnterpriseReportingConfigDTO.class,
          EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH))
              .thenReturn(new EnterpriseReportingConfigDTO(expectedTenant1BaseUrl));
    });
    testAsTenant(tenant1, t1 -> {
      assertThat(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).isEqualTo(expectedTenant1BaseUrl);
    });
    Tenant tenant2 = testAsNewTenant(testName, t1 -> {
      when(mockHdsClient.get(EnterpriseReportingConfigDTO.class,
          EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH))
              .thenReturn(new EnterpriseReportingConfigDTO(expectedTenant2BaseUrl));
    });
    testAsTenant(tenant2, t1 -> {
      assertThat(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).isEqualTo(expectedTenant2BaseUrl);
    });
    testAsTenant(tenant1, t1 -> {
      assertThat(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).isEqualTo(expectedTenant1BaseUrl);
    });
    testAsTenant(tenant2, t1 -> {
      assertThat(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).isEqualTo(expectedTenant2BaseUrl);
    });
  }

  @Test
  public void testDashboardMetadata() {
    DashboardMetadataListDTO expected = mockGetLookerDashboardMetadata();
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(expected);

    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.dashboardMetadata);
    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardGroupMetadata)
        .hasSameElementsAs(expected.dashboardGroupMetadata);
    assertThat(enterpriseReportingService.getDashboardMetadata().version).isEqualTo(expected.version);

    verify(mockHdsClient, times(1)).get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
    verify(mockHdsClient, times(1)).get(DashboardMetadataListDTO.class, ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(mockHdsClient, never()).get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH);

    verifyScheduledTaskVersionCache(1);
    Mockito.clearInvocations(mockHdsClient);
    Mockito.clearInvocations(mockTaskScheduler);

    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.dashboardMetadata);
    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardGroupMetadata)
        .hasSameElementsAs(expected.dashboardGroupMetadata);
    assertThat(enterpriseReportingService.getDashboardMetadata().version).isEqualTo(expected.version);

    verify(mockHdsClient, never()).get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
    verify(mockHdsClient, never()).get(DashboardMetadataListDTO.class, ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(mockHdsClient, never()).get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH);

    verify(mockTaskScheduler, times(0)).scheduleOneTimeTaskForAllOtherNodes(any(), any());
    Mockito.clearInvocations(mockHdsClient);
    Mockito.clearInvocations(mockTaskScheduler);

    enterpriseReportingService.currentDashboardsVersionSupplier.reset();
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(2));
    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.dashboardMetadata);
    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardGroupMetadata)
        .hasSameElementsAs(expected.dashboardGroupMetadata);
    assertThat(enterpriseReportingService.getDashboardMetadata().version).isEqualTo(expected.version);

    verify(mockHdsClient, times(1)).get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
    verify(mockHdsClient, times(1)).get(DashboardMetadataListDTO.class, ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(mockHdsClient, never()).get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH);
    verifyScheduledTaskVersionCache(2);
  }

  @Test
  public void testGetDashboardMetadata_Error() {
    DashboardMetadataListDTO expected = mockGetLookerDashboardMetadata();
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(expected);
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));

    enterpriseReportingService.getDashboardMetadata();

    assertThat(enterpriseReportingService.currentDashboardsVersionSupplier.get()).isEqualTo(1);
    verify(mockHdsClient, times(1)).get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
    verify(mockHdsClient, times(1)).get(DashboardMetadataListDTO.class, ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(mockHdsClient, never()).get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH);

    Mockito.clearInvocations(mockHdsClient);

    enterpriseReportingService.currentDashboardsVersionSupplier.reset();
    when(mockHdsClient.get(any(), any())).thenThrow(new NotFoundException("Not found"));
    assertThatThrownBy(() -> enterpriseReportingService.getDashboardMetadata()).hasMessageContaining("Not found");
    verify(mockHdsClient, times(1)).get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
    verify(mockHdsClient, never()).get(DashboardMetadataListDTO.class, ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(mockHdsClient, never()).get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH);
  }

  @Test
  public void testGetDashboardMetadata_ErrorFirstHdsCall() {
    when(mockHdsClient.get(any(), anyString())).thenAnswer(invocationOnMock -> {
      String path = invocationOnMock.getArgument(1);
      if (ENTERPRISE_REPORTING_CURRENT_VERSION_PATH.equals(path)) {
        return new DashboardsVersionDTO(1);
      }
      else {
        throw new BadGatewayException("error");
      }
    });

    assertThatThrownBy(() -> enterpriseReportingService.getDashboardMetadata())
        .isInstanceOf(BadGatewayException.class)
        .hasMessageContaining("error");
  }

  @Test
  public void testCacheDashboardIcons_FirstCacheLoad() throws Exception {
    String firstIconImageFileName = "icon-1.png";
    byte[] firstIconBytes = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + firstIconImageFileName)
        .toURI()));
    byte[] firstIconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip")
        .toURI()));
    DashboardMetadataDTO firstDashboardMetadataDTO = createDashboardMetadata(firstIconImageFileName);
    var version = new DashboardsVersionDTO(1);
    DashboardMetadataListDTO firstDashboardMetadataListDTO =
        new DashboardMetadataListDTO(version,
            Collections.singletonList(firstDashboardMetadataDTO),
            DASHBOARD_GROUP_METADATA);
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(version);
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(firstDashboardMetadataListDTO);
    when(mockHdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(firstIconZipFile));

    assertThat(enterpriseReportingService.getIcon(firstIconImageFileName)).isEqualTo(firstIconBytes);

    assertDashboardIconImage(firstIconBytes, firstIconImageFileName);
  }

  @Test
  public void testCacheDashboardIcons_FirstCacheLoad_MultipleIcons() throws Exception {
    String firstIconImageFileName = "icon-1.png";
    byte[] firstIconBytes = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + firstIconImageFileName)
        .toURI()));
    byte[] iconsZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icons.zip")
        .toURI()));
    var version = new DashboardsVersionDTO(1);
    DashboardMetadataDTO firstDashboardMetadataDTO = createDashboardMetadata(firstIconImageFileName);
    String secondIconImageFileName = "icon-2.png";
    byte[] secondIconBytes = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + secondIconImageFileName)
        .toURI()));
    DashboardMetadataDTO secondDashboardMetadataDTO = createDashboardMetadata(secondIconImageFileName);
    DashboardMetadataListDTO dashboardMetadataListDTO =
        new DashboardMetadataListDTO(version,
            Arrays.asList(firstDashboardMetadataDTO, secondDashboardMetadataDTO),
            DASHBOARD_GROUP_METADATA);
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(version);
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(dashboardMetadataListDTO);
    when(mockHdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(iconsZipFile));

    assertThat(enterpriseReportingService.getIcon(firstIconImageFileName)).isEqualTo(firstIconBytes);
    assertThat(enterpriseReportingService.getIcon(secondIconImageFileName)).isEqualTo(secondIconBytes);

    assertDashboardIconImage(firstIconBytes, firstIconImageFileName);
    assertDashboardIconImage(secondIconBytes, secondIconImageFileName);
  }

  @Test
  public void testCacheDashboardIcons_CacheReloadWithNoIconUpdates() throws Exception {
    String firstIconImageFileName = "icon-1.png";
    byte[] firstIconBytes = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + firstIconImageFileName)
        .toURI()));
    byte[] firstIconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip")
        .toURI()));
    var version = new DashboardsVersionDTO(1);
    DashboardMetadataDTO firstDashboardMetadataDTO = createDashboardMetadata(firstIconImageFileName);
    DashboardMetadataListDTO firstDashboardMetadataListDTO =
        new DashboardMetadataListDTO(version,
            Collections.singletonList(firstDashboardMetadataDTO),
            DASHBOARD_GROUP_METADATA);
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(version);
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(firstDashboardMetadataListDTO);
    when(mockHdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(firstIconZipFile));
    assertThat(enterpriseReportingService.getIcon(firstIconImageFileName)).isEqualTo(firstIconBytes);
    assertDashboardIconImage(firstIconBytes, firstIconImageFileName);
    enterpriseReportingService.currentDashboardsVersionSupplier.reset();

    assertThat(enterpriseReportingService.getIcon(firstIconImageFileName)).isEqualTo(firstIconBytes);
    assertDashboardIconImage(firstIconBytes, firstIconImageFileName);
  }

  @Test
  public void testGetIcon_CacheReloadWithIconUpdates() throws Exception {
    String firstIconImageFileName = "icon-1.png";
    byte[] firstIconBytes = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + firstIconImageFileName)
        .toURI()));
    byte[] firstIconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip")
        .toURI()));
    var version = new DashboardsVersionDTO(1);
    DashboardMetadataDTO firstDashboardMetadataDTO = createDashboardMetadata(firstIconImageFileName);
    DashboardMetadataListDTO firstDashboardMetadataListDTO =
        new DashboardMetadataListDTO(version,
            Collections.singletonList(firstDashboardMetadataDTO),
            DASHBOARD_GROUP_METADATA);

    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(version);
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(firstDashboardMetadataListDTO);
    when(mockHdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(firstIconZipFile));

    assertThat(enterpriseReportingService.getIcon(firstIconImageFileName)).isEqualTo(firstIconBytes);

    assertDashboardIconImage(firstIconBytes, firstIconImageFileName);

    String secondIconImageFileName = "icon-2.png";
    byte[] secondIconBytes = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + secondIconImageFileName)
        .toURI()));
    byte[] secondIconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-2.zip")
        .toURI()));
    DashboardsVersionDTO secondVersion = new DashboardsVersionDTO(2);
    DashboardMetadataDTO secondDashboardMetadataDTO = createDashboardMetadata(secondIconImageFileName);
    DashboardMetadataListDTO secondDashboardMetadataListDTO =
        new DashboardMetadataListDTO(secondVersion,
            Collections.singletonList(secondDashboardMetadataDTO),
            DASHBOARD_GROUP_METADATA);

    enterpriseReportingService.currentDashboardsVersionSupplier.reset();
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(secondVersion);
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(secondDashboardMetadataListDTO);
    when(mockHdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(secondIconZipFile));

    assertThat(enterpriseReportingService.getIcon(secondIconImageFileName)).isEqualTo(secondIconBytes);

    assertDashboardIconImage(secondIconBytes, secondIconImageFileName);
  }

  private static String generateRandomString() {
    return TemporaryEntity.uuid().substring(0, 8);
  }

  private static DashboardMetadataDTO createDashboardMetadata(String iconImage) {
    final var dashboardId = generateRandomString();
    final var groupId = generateRandomString();
    final var title = generateRandomString();
    final var category = "partner"; // Use valid category for testing (no license required)
    final var description = generateRandomString();
    final var features = Collections.singletonList(generateRandomString());
    final var accessButtonText = generateRandomString();
    final var previewImage = iconImage;
    final var previewImageIcon = generateRandomString();
    final var priority = 1;
    final var spotlight = false;
    final var dashboardPath = "dashboards/rolling_recap::rolling_recap";
    final String spotlightColor = null;
    final String spotlightText = null;
    return new DashboardMetadataDTO(dashboardId, groupId, title, category, description, features, accessButtonText,
        previewImage, previewImageIcon, priority, spotlight, dashboardPath, spotlightColor, spotlightText);
  }

  private static DashboardGroupMetadataDTO createDashboardGroupMetadata() {
    final var groupId = generateRandomString();
    final var description = generateRandomString();
    final var features = Collections.singletonList(generateRandomString());
    final var previewImageIcon = generateRandomString();
    final var spotlight = false;
    final String spotlightColor = null;
    final String spotlightText = null;
    final var title = generateRandomString();
    return new DashboardGroupMetadataDTO(groupId, description, features, previewImageIcon, spotlight,
        spotlightColor, spotlightText, title);
  }

  private static DashboardMetadataListDTO mockGetLookerDashboardMetadata() {
    var version = new DashboardsVersionDTO(1);
    return new DashboardMetadataListDTO(version, Arrays.asList(createDashboardMetadata("icon-1.png"),
        createDashboardMetadata("icon-2.png"), createDashboardMetadata("icon-3.png")),
        Arrays.asList(createDashboardGroupMetadata()));
  }

  private void assertDashboardIconImage(
      byte[] expectedIconsImageBytes,
      String expectedIconImageFileName) throws IOException
  {
    File iconsDirectory = insightWork.getIerDashboardIconsDirectory();
    File actualIconImageFile = new File(iconsDirectory, expectedIconImageFileName);
    byte[] actualIconsImageFileBytes = Files.readAllBytes(actualIconImageFile.toPath());
    assertThat(actualIconImageFile.exists()).isTrue();
    assertThat(actualIconImageFile.isFile()).isTrue();
    assertThat(actualIconsImageFileBytes).containsExactly(expectedIconsImageBytes);
  }

  @Test
  public void testGetIcon_valid_onlyRequestedOnce() throws Exception {
    String iconName = "icon-1.png";
    var version = new DashboardsVersionDTO(1);
    DashboardMetadataListDTO dashboardMetadataListDTO =
        new DashboardMetadataListDTO(version, Collections.singletonList(createDashboardMetadata(iconName)),
            DASHBOARD_GROUP_METADATA);
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(version);
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(dashboardMetadataListDTO);
    byte[] iconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip")
        .toURI()));
    when(mockHdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(iconZipFile));

    AtomicReference<byte[]> t1IconImage = new AtomicReference<>();
    testAsNewTenant(testName, t1 -> {
      byte[] iconImage = enterpriseReportingService.getIcon(iconName);
      assertThat(iconImage).isNotNull();
      t1IconImage.set(iconImage);
      verify(mockHdsClient, times(1)).get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
      verify(mockHdsClient, times(1)).get(DashboardMetadataListDTO.class,
          ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
      verify(mockHdsClient, times(1)).get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH);
      verifyScheduledTaskVersionCache(1);
    });

    Mockito.clearInvocations(mockHdsClient);
    Mockito.clearInvocations(mockTaskScheduler);

    AtomicReference<byte[]> t2IconImage = new AtomicReference<>();
    testAsNewTenant(testName, t2 -> {
      byte[] iconImage = enterpriseReportingService.getIcon(iconName);
      assertThat(iconImage).isNotNull();
      t2IconImage.set(iconImage);
      verify(mockHdsClient, never()).get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
      verify(mockHdsClient, never()).get(DashboardMetadataListDTO.class, ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
      verify(mockHdsClient, never()).get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH);
      verify(mockTaskScheduler, never()).scheduleOneTimeTaskForAllOtherNodes(eq(enterpriseReportingService), any());
    });

    assertThat(t1IconImage.get()).isEqualTo(t2IconImage.get());
  }

  @Test
  public void testGetIcon_notFound() {
    final List<DashboardMetadataDTO> emptyDashboardMetadata = Collections.emptyList();
    var version = new DashboardsVersionDTO(1);
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(version);
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(
            new DashboardMetadataListDTO(version, emptyDashboardMetadata, DASHBOARD_GROUP_METADATA));

    assertThatThrownBy(() -> enterpriseReportingService.getIcon("rolling-recap1.svg"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testGetIcon_badRequest() throws Exception {
    String iconName = "icon-1.png";
    var version = new DashboardsVersionDTO(1);
    DashboardMetadataListDTO dashboardMetadataListDTO =
        new DashboardMetadataListDTO(version, Collections.singletonList(createDashboardMetadata(iconName)),
            DASHBOARD_GROUP_METADATA);
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(version);
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(dashboardMetadataListDTO);
    byte[] iconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip")
        .toURI()));
    lenient().when(mockHdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(iconZipFile));

    assertThatThrownBy(() -> enterpriseReportingService.getIcon("../" + iconName))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testExecute_UpdateCache() throws Exception {
    EnterpriseReportingService spyEnterpriseReportingService =
        spy(enterpriseReportingService);

    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    jobDataMap.put(EnterpriseReportingService.TASK_PARAM_CURRENT_VERSION, "1");
    // Set the current version to an older version
    spyEnterpriseReportingService.currentDashboardsVersionSupplier.setMemoizedValue(-1);

    spyEnterpriseReportingService.execute(mockJobExecutionContext);

    assertThat(spyEnterpriseReportingService.currentDashboardsVersionSupplier.getMemoizedValue()).isEqualTo(1);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(
        JobBuilder.newJob(EnterpriseReportingService.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testAcquireEmbedSession_Success() {
    // Mock user queries to mock that a user has logged in
    // Given
    mockEmbedSessionParams("baseUrl/");
    final ArgumentCaptor<Map<String, String>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    EmbedCookielessSessionAcquire expectedResponse =
        new EmbedCookielessSessionAcquire("authTokenResponse", 300, "navTokenResponse", 400, "apiTokenResponse", 500,
            "sessionTokenResponse", 600);

    when(mockHdsClient.post(eq(EmbedCookielessSessionAcquire.class),
        anyString(),
        any(),
        mapArgumentCaptor.capture(),
        anyString())).thenReturn(expectedResponse);

    // When
    EmbedCookielessSessionAcquire sessionAcquireResult =
        enterpriseReportingService.acquireEmbedSession("dashboardId", embedDomain,
            "Mozilla/:::::");

    // Then
    assertSessionAcquireResult(sessionAcquireResult);

    Map<String, String> value = mapArgumentCaptor.getValue();
    assertThat(value.size()).isOne();
    assertThat(value.get("baseUrl")).isEqualTo("baseUrl/");
  }

  @Test
  public void testAcquireEmbedSession_Success_EmptyQueryParams() {
    // Mock user queries to mock that a user has logged in
    // Given
    mockEmbedSessionParams(null);
    final ArgumentCaptor<Map<String, String>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    EmbedCookielessSessionAcquire expectedResponse =
        new EmbedCookielessSessionAcquire("authTokenResponse", 300, "navTokenResponse", 400, "apiTokenResponse", 500,
            "sessionTokenResponse", 600);

    when(mockHdsClient.post(eq(EmbedCookielessSessionAcquire.class),
        anyString(),
        any(),
        mapArgumentCaptor.capture(),
        anyString())).thenReturn(expectedResponse);

    // When
    EmbedCookielessSessionAcquire sessionAcquireResult =
        enterpriseReportingService.acquireEmbedSession("dashboardId", embedDomain,
            "Mozilla/:::::");

    // Then
    assertSessionAcquireResult(sessionAcquireResult);

    Map<String, String> value = mapArgumentCaptor.getValue();
    assertThat(value.size()).isZero();
  }

  private void mockEmbedSessionParams(String baseUrl) {
    when(mockCurrentUser.getUserPrincipal()).thenReturn(new UserPrincipal("username", "displayName", InternalRealm.ID));
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(new BaseUrlConfiguration(baseUrl, false));
  }

  private void assertSessionAcquireResult(EmbedCookielessSessionAcquire sessionAcquireResult) {
    assertThat(sessionAcquireResult).isNotNull();

    assertThat(sessionAcquireResult.getAuthenticationToken()).isEqualTo("authTokenResponse");
    assertThat(sessionAcquireResult.getAuthenticationTokenTtl()).isEqualTo(300);
    assertThat(sessionAcquireResult.getNavigationToken()).isEqualTo("navTokenResponse");
    assertThat(sessionAcquireResult.getNavigationTokenTtl()).isEqualTo(400);
    assertThat(sessionAcquireResult.getApiToken()).isEqualTo("apiTokenResponse");
    assertThat(sessionAcquireResult.getApiTokenTtl()).isEqualTo(500);
    assertThat(sessionAcquireResult.getSessionReferenceToken()).isEqualTo("sessionTokenResponse");
    assertThat(sessionAcquireResult.getSessionReferenceTokenTtl()).isEqualTo(600);
  }

  @Test
  public void testAcquireEmbedSession_BadRequest_missingParameters() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> enterpriseReportingService.acquireEmbedSession(null, embedDomain,
            "Mozilla/:::::"))
        .withMessage("Dashboard is null or empty");
  }

  @Test
  public void testGenerateEmbedTokens_Success() throws Exception {
    EmbedCookielessSessionGenerateTokensResponse expectedResponse =
        new EmbedCookielessSessionGenerateTokensResponse("navToken", 200, "apiToken", 300,
            "sessionRefTokenResponse", 400);
    when(mockHdsClient.put(eq(EmbedCookielessSessionGenerateTokensResponse.class), anyString(), any(),
        anyString())).thenReturn(
            expectedResponse);

    EmbedCookielessSessionGenerateTokens tokenRequestDto =
        new EmbedCookielessSessionGenerateTokens("navToken", "apiToken", "oldSessionToken");
    EmbedCookielessSessionGenerateTokensResponse embedSessionResponse =
        enterpriseReportingService.generateEmbedTokens(tokenRequestDto, "Mozilla/:::::");
    assertThat(embedSessionResponse).isNotNull();

    assertThat(embedSessionResponse.getNavigationToken()).isEqualTo("navToken");
    assertThat(embedSessionResponse.getNavigationTokenTtl()).isEqualTo(200);
    assertThat(embedSessionResponse.getApiToken()).isEqualTo("apiToken");
    assertThat(embedSessionResponse.getApiTokenTtl()).isEqualTo(300);
    assertThat(embedSessionResponse.getSessionReferenceToken()).isEqualTo("sessionRefTokenResponse");
    assertThat(embedSessionResponse.getSessionReferenceTokenTtl()).isEqualTo(400);
  }

  @Test
  public void testGenerateEmbedTokens_BadRequest_MissingParameters() throws Exception {
    final EmbedCookielessSessionGenerateTokens tokenRequestDtoNoNav =
        new EmbedCookielessSessionGenerateTokens(null, "apiToken", "oldSessionToken");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> enterpriseReportingService.generateEmbedTokens(tokenRequestDtoNoNav, "Mozilla/:::::"))
        .withMessage("Navigation token is null or empty");

    final EmbedCookielessSessionGenerateTokens tokenRequestDtoNoApi =
        new EmbedCookielessSessionGenerateTokens("navToken", null, "oldSessionToken");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> enterpriseReportingService.generateEmbedTokens(tokenRequestDtoNoApi, "Mozilla/:::::"))
        .withMessage("Api token is null or empty");

    final EmbedCookielessSessionGenerateTokens tokenRequestDtoNoSessionRef =
        new EmbedCookielessSessionGenerateTokens("navToken", "apiToken", null);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> enterpriseReportingService.generateEmbedTokens(tokenRequestDtoNoSessionRef, "Mozilla/:::::"))
        .withMessage("Session reference token is null or empty");
  }

  @Test
  public void testCreateSSOEmbedUrlRequest() {
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Some App", "SOME_APP", organization.getId());
    final Application application2 = tempEntity.newApplication("Some App 2", "SOME_APP2", organization.getId());
    final Application application3 = tempEntity.newApplication("Some App 3", "SOME_APP3", organization.getId());
    final Application application4 = tempEntity.newApplication("Some App 4", "SOME_APP4", organization.getId());

    UserPrincipal userPrincipal = new UserPrincipal("username", "displayName", InternalRealm.ID);
    String dashboardId = "dashboardId";
    String embedDomain = "http://sonatype.sonatype.sonatype.com";

    when(mockCurrentUser.getUserPrincipal()).thenReturn(userPrincipal);

    SSOEmbedUrlRequest ssoEmbedUrlRequest = enterpriseReportingService
        .createEmbedRequest(dashboardId, embedDomain);

    Set<String> obfuscatedApplicationIds = new HashSet<>();
    obfuscatedApplicationIds.add(HashUtils.hash(application.getId(), HashUtils.SHA1));
    obfuscatedApplicationIds.add(HashUtils.hash(application2.getId(), HashUtils.SHA1));
    obfuscatedApplicationIds.add(HashUtils.hash(application3.getId(), HashUtils.SHA1));
    obfuscatedApplicationIds.add(HashUtils.hash(application4.getId(), HashUtils.SHA1));

    assertThat(ssoEmbedUrlRequest.embedDomain).isEqualTo(embedDomain);
    assertThat(ssoEmbedUrlRequest.dashboardKey).isEqualTo(dashboardId);
    assertThat(ssoEmbedUrlRequest.usernameAndRealm).isEqualTo("username@Internal");
    assertThat(ssoEmbedUrlRequest.userPermissions).isEmpty();
    assertThat(ssoEmbedUrlRequest.applicationIds)
        .containsExactlyInAnyOrder(obfuscatedApplicationIds.toArray(new String[]{}));
  }

  @Test
  public void testFilterByLicenseAndFeatureFlags_FirewallLicenseAndFeatureEnabled_ReturnsFirewallDashboards() {
    EnterpriseReportingService spy = Mockito.spy(enterpriseReportingService);
    Mockito.doReturn(true).when(spy).isFirewallReportingEnabled();

    when(mockSolutionResolver.getLicensedSolutions())
        .thenReturn(Set.of(Solution.FIREWALL));
    List<DashboardMetadataDTO> allDashboards = List.of(
        createDashboard("firewall-1", "firewall"),
        createDashboard("enterprise-1", "enterprise"));

    List<DashboardMetadataDTO> result = spy.filterByLicenseAndFeatureFlags(allDashboards);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).dashboardId).isEqualTo("firewall-1");
  }

  @Test
  public void testFilterByLicenseAndFeatureFlags_FirewallLicenseButFeatureDisabled_FiltersOutFirewall() {
    EnterpriseReportingService spy = Mockito.spy(enterpriseReportingService);
    Mockito.doReturn(false).when(spy).isFirewallReportingEnabled();

    when(mockSolutionResolver.getLicensedSolutions())
        .thenReturn(Set.of(Solution.FIREWALL));
    List<DashboardMetadataDTO> allDashboards = List.of(
        createDashboard("firewall-1", "firewall"));

    List<DashboardMetadataDTO> result = spy.filterByLicenseAndFeatureFlags(allDashboards);

    assertThat(result).isEmpty();
  }

  @Test
  public void testFilterByLicenseAndFeatureFlags_LifecycleLicense_ReturnsEnterpriseAndDataInsight() {
    when(mockSolutionResolver.getLicensedSolutions())
        .thenReturn(Set.of(Solution.LIFECYCLE));
    List<DashboardMetadataDTO> allDashboards = List.of(
        createDashboard("enterprise-1", "enterprise"),
        createDashboard("datainsight-1", "dataInsight"),
        createDashboard("firewall-1", "firewall"));

    List<DashboardMetadataDTO> result = enterpriseReportingService.filterByLicenseAndFeatureFlags(allDashboards);

    assertThat(result).hasSize(2);
    assertThat(result).extracting("category").containsExactlyInAnyOrder("enterprise", "dataInsight");
  }

  @Test
  public void testFilterByLicenseAndFeatureFlags_BothLicenses_ReturnsAllAuthorized() {
    EnterpriseReportingService spy = Mockito.spy(enterpriseReportingService);
    Mockito.doReturn(true).when(spy).isFirewallReportingEnabled();

    when(mockSolutionResolver.getLicensedSolutions())
        .thenReturn(Set.of(Solution.LIFECYCLE,
            Solution.FIREWALL));
    List<DashboardMetadataDTO> allDashboards = List.of(
        createDashboard("enterprise-1", "enterprise"),
        createDashboard("firewall-1", "firewall"),
        createDashboard("datainsight-1", "dataInsight"));

    List<DashboardMetadataDTO> result = spy.filterByLicenseAndFeatureFlags(allDashboards);

    assertThat(result).hasSize(3);
  }

  @Test
  public void testFilterByLicenseAndFeatureFlags_NoLicense_ReturnsOnlyPartner() {
    when(mockSolutionResolver.getLicensedSolutions()).thenReturn(Collections.emptySet());
    List<DashboardMetadataDTO> allDashboards = List.of(
        createDashboard("enterprise-1", "enterprise"),
        createDashboard("partner-1", "partner"),
        createDashboard("firewall-1", "firewall"));

    List<DashboardMetadataDTO> result = enterpriseReportingService.filterByLicenseAndFeatureFlags(allDashboards);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).category).isEqualTo("partner");
  }

  @Test
  public void testFilterByLicenseAndFeatureFlags_UnknownCategory_DeniesAccess() {
    when(mockSolutionResolver.getLicensedSolutions())
        .thenReturn(Set.of(Solution.LIFECYCLE));
    DashboardMetadataDTO unknownDashboard = createDashboard("unknown-1", "new-category");

    List<DashboardMetadataDTO> result =
        enterpriseReportingService.filterByLicenseAndFeatureFlags(List.of(unknownDashboard));

    assertThat(result).isEmpty();
  }

  @Test
  public void testFilterByLicenseAndFeatureFlags_NullCategory_DeniesAccess() {
    when(mockSolutionResolver.getLicensedSolutions())
        .thenReturn(Set.of(Solution.LIFECYCLE));
    DashboardMetadataDTO nullCategoryDashboard = createDashboard("null-1", null);

    List<DashboardMetadataDTO> result =
        enterpriseReportingService.filterByLicenseAndFeatureFlags(List.of(nullCategoryDashboard));

    assertThat(result).isEmpty();
  }

  private DashboardMetadataDTO createDashboard(String id, String category) {
    DashboardMetadataDTO dashboard = new DashboardMetadataDTO();
    dashboard.dashboardId = id;
    dashboard.category = category;
    dashboard.title = "Test Dashboard";
    dashboard.description = "Test Description";
    dashboard.features = Collections.singletonList("feature1");
    dashboard.accessButtonText = "Access";
    dashboard.previewImage = "image.png";
    dashboard.previewImageIcon = "icon.png";
    dashboard.priority = 1;
    dashboard.spotlight = false;
    dashboard.dashboardPath = "dashboards/test::test";
    return dashboard;
  }

  private void verifyScheduledTaskVersionCache(Integer latestVersion) {
    Map<String, String> expectedParameters = new HashMap<>();
    expectedParameters.put(EnterpriseReportingService.TASK_PARAM_CURRENT_VERSION, latestVersion.toString());
    verify(mockTaskScheduler)
        .scheduleOneTimeTaskForAllOtherNodes(enterpriseReportingService, expectedParameters);
  }
}
