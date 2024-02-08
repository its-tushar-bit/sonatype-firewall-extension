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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.cache.LoadingCache;
import com.google.inject.Binder;
import com.google.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_CURRENT_VERSION_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_SSO_EMBED_URL_PATH;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EnterpriseReportingServiceTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient hdsClientMock;

  @Mock
  private CurrentUser currentUserMock;

  @Mock
  private MembershipMappingService mockMembershipMappingService;

  @Mock
  private UserDAO mockUserDAO;

  @Mock
  private SamlUserDAO mockSamlUserDAO;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Inject
  private EnterpriseReportingService enterpriseReportingService;

  @Inject
  private InsightWork insightWork;

  @Captor
  private ArgumentCaptor<SSOEmbedUrlRequest> lookerSSOEmbedUrlHdsRequestArgumentCaptor;

  private final TenantReference<LoadingCache<String, EnterpriseReportingConfigDTO>> mockConfigCache =
      mock(TenantReference.class);

  private final LoadingCache<String, Integer> mockLatestVersionCache = mock(LoadingCache.class);

  private final Configuration mockConfiguration = mock(Configuration.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    binder.bind(CurrentUser.class).toInstance(currentUserMock);
    binder.bind(UserDAO.class).toInstance(mockUserDAO);
    binder.bind(SamlUserDAO.class).toInstance(mockSamlUserDAO);
    binder.bind(MembershipMappingService.class).toInstance(mockMembershipMappingService);
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    super.configure(binder);
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureEnabled_InternalRealm() {
    when(currentUserMock.getUserPrincipal()).thenReturn(new UserPrincipal("username", "displayName", InternalRealm.ID));
    when(mockUserDAO.getByUsernameNotNull("username")).thenReturn(
        new User("username", "password", "firstName", "lastName", "email"));
    createSSOEmbedUrl_FeatureEnabled(InternalRealm.ID, "firstName", "lastName");
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureEnabled_SamlRealm() {
    when(currentUserMock.getUserPrincipal()).thenReturn(new UserPrincipal("username", "displayName", SamlRealm.ID));
    when(mockSamlUserDAO.getByUsernameNotNull("username")).thenReturn(
        new SamlUser("username", "firstName", "lastName", "email", Collections.emptySet()));
    createSSOEmbedUrl_FeatureEnabled(SamlRealm.ID, "firstName", "lastName");
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureEnabled_OtherRealm() {
    when(currentUserMock.getUserPrincipal()).thenReturn(new UserPrincipal("username", "displayName", "other"));
    createSSOEmbedUrl_FeatureEnabled("other", "displayName", "");
  }

  @Test
  public void testCreateSSOEmbedUrl_MissingDashboardKey() {
    assertThatThrownBy(() -> enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO(null)))
        .isInstanceOf(BadRequestException.class).hasMessage("Dashboard is null or empty");
  }

  @Test
  public void testCreateSSOEmbedUrl_HdsBadRequest() {
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenThrow(new BadRequestException("Bad request"));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatThrownBy(() -> enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("test")))
        .isInstanceOf(BadRequestException.class).hasMessage("Bad request");
  }

  @Test
  public void testCreateSSOEmbedUrl_HdsNotFound() {
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenThrow(new NotFoundException("Not found"));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatThrownBy(() -> enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("test")))
        .isInstanceOf(NotFoundException.class).hasMessage("Not found");
  }

  @Test
  public void testCreateSSOEmbedUrl_LookerError() {
    String hdsError = "Error with Looker";
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenThrow(new ConflictException(hdsError));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatThrownBy(() -> enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("test")))
        .isInstanceOf(ConflictException.class)
        .hasMessage(hdsError);
  }

  @Test
  public void testGetBaseUrl() {
    String expectedBaseUrl = "https://sonatypeexternaldev.cloud.looker.com/";
    when(hdsClientMock.get(EnterpriseReportingConfigDTO.class,
        EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH))
        .thenReturn(new EnterpriseReportingConfigDTO(expectedBaseUrl));
    assertThat(enterpriseReportingService.getBaseUrl()).isEqualTo(expectedBaseUrl);
  }

  @Test
  public void testGetBaseUrl_Multitenant() {
    String expectedTenant1BaseUrl = "https://sonatypeexternaldev.cloud.looker.com/";
    String expectedTenant2BaseUrl = "https://sonatypeexternaldev.us-east.cloud.looker.com/";
    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      when(hdsClientMock.get(EnterpriseReportingConfigDTO.class,
          EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH))
          .thenReturn(new EnterpriseReportingConfigDTO(expectedTenant1BaseUrl));
    });
    testAs(tenant1, t1 -> {
      assertThat(enterpriseReportingService.getBaseUrl()).isEqualTo(expectedTenant1BaseUrl);
    });
    Tenant tenant2 = testAsNewTenant(testName, t1 -> {
      when(hdsClientMock.get(EnterpriseReportingConfigDTO.class,
          EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH))
          .thenReturn(new EnterpriseReportingConfigDTO(expectedTenant2BaseUrl));
    });
    testAs(tenant2, t1 -> {
      assertThat(enterpriseReportingService.getBaseUrl()).isEqualTo(expectedTenant2BaseUrl);
    });
    testAs(tenant1, t1 -> {
      assertThat(enterpriseReportingService.getBaseUrl()).isEqualTo(expectedTenant1BaseUrl);
    });
    testAs(tenant2, t1 -> {
      assertThat(enterpriseReportingService.getBaseUrl()).isEqualTo(expectedTenant2BaseUrl);
    });
  }

  @Test
  public void testDashboardMetadata() {
    AtomicReference<DashboardMetadataListDTO> expected = mockGetLookerDashboardMetadata();
    when(hdsClientMock.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(new byte[0]));
    when(hdsClientMock.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(expected.get());
    when(hdsClientMock.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));

    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.get().dashboardMetadata);

    verify(hdsClientMock, times(1)).get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(hdsClientMock, times(1)).get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);

    verifyScheduledTaskVersionCache(1);
    Mockito.clearInvocations(hdsClientMock);
    Mockito.clearInvocations(mockTaskScheduler);

    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.get().dashboardMetadata);

    verify(hdsClientMock, times(0)).get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(hdsClientMock, times(1)).get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);

    verify(mockTaskScheduler, times(0)).scheduleOneTimeTaskForAllOtherNodes(any(), any());
    Mockito.clearInvocations(hdsClientMock);
    Mockito.clearInvocations(mockTaskScheduler);

    enterpriseReportingService.currentVersionCache.invalidateAll();
    when(hdsClientMock.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(2));
    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.get().dashboardMetadata);

    verify(hdsClientMock, times(1)).get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(hdsClientMock, times(1)).get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
    verifyScheduledTaskVersionCache(2);
  }

  @Test
  public void testGetDashboardMetadata_Error() {
    AtomicReference<DashboardMetadataListDTO> expected = mockGetLookerDashboardMetadata();
    when(hdsClientMock.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(new byte[0]));
    when(hdsClientMock.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(expected.get());
    when(hdsClientMock.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));

    assertThat(enterpriseReportingService.currentVersion.get()).isEqualTo(-1);

    enterpriseReportingService.getDashboardMetadata();
    assertThat(enterpriseReportingService.currentVersion.get()).isEqualTo(1);

    when(hdsClientMock.get(any(), any())).thenThrow(new NotFoundException("Not found"));
    assertThatThrownBy(() -> enterpriseReportingService.getDashboardMetadata()).hasMessageContaining("Not found");

    assertThat(enterpriseReportingService.currentVersion.get()).isEqualTo(1);
  }

  @Test
  public void testGetDashboardMetadata_ErrorFirstHdsCall() {
    when(hdsClientMock.get(any(), anyString())).thenAnswer(invocationOnMock -> {
      String path = invocationOnMock.getArgument(1);
      if (ENTERPRISE_REPORTING_CURRENT_VERSION_PATH.equals(path)) {
        return new DashboardsVersionDTO(1);
      }
      else {
        throw new RuntimeException("error");
      }
    });

    assertThatThrownBy(() -> enterpriseReportingService.getDashboardMetadata())
        .isInstanceOf(InternalServerException.class)
        .hasMessageContaining("Error while fetching dashboard metadata from Sonatype data services");
  }

  @Test
  public void testCacheDashboardIcons_FirstCacheLoad() throws Exception {
    String expectedIconImageFileName = "icon-1.png";
    byte[] expectedIconsZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip").toURI()));
    byte[] expectedIconImageFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + expectedIconImageFileName).toURI()));
    when(hdsClientMock.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(expectedIconsZipFile));
    enterpriseReportingService.cacheDashboardIcons();

    assertDashboardIconImage(expectedIconImageFile, expectedIconImageFileName);
  }

  @Test
  public void testCacheDashboardIcons_FirstCacheLoad_MultipleIcons() throws Exception {
    String expectedIconImageFileName = "icon-1.png";
    String expectedSecondIconImageFileName = "icon-2.png";
    byte[] expectedIconsZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icons.zip").toURI()));
    byte[] expectedIconImageFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + expectedIconImageFileName).toURI()));
    byte[] expectedSecondIconImageFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + expectedSecondIconImageFileName).toURI()));
    when(hdsClientMock.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(expectedIconsZipFile));
    enterpriseReportingService.cacheDashboardIcons();

    assertDashboardIconImage(expectedIconImageFile, expectedIconImageFileName);
    assertDashboardIconImage(expectedSecondIconImageFile, expectedSecondIconImageFileName);
  }

  @Test
  public void testCacheDashboardIcons_CacheReloadWithNoIconUpdates() throws Exception {
    String expectedIconImageFileName = "icon-1.png";
    byte[] expectedIconsZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip").toURI()));
    byte[] expectedIconImageFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + expectedIconImageFileName).toURI()));
    when(hdsClientMock.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(expectedIconsZipFile));
    enterpriseReportingService.cacheDashboardIcons();
    when(hdsClientMock.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(expectedIconsZipFile));
    enterpriseReportingService.cacheDashboardIcons();

    assertDashboardIconImage(expectedIconImageFile, expectedIconImageFileName);
  }

  @Test
  public void testCacheDashboardIcons_CacheReloadWithIconUpdates() throws Exception {
    String firstIconImageFileName = "icon-1.png";
    String expectedIconImageFileName = "icon-2.png";
    File iconsDirectory = insightWork.getIerDashboardIconsDirectory();
    File firstIconImageFile = new File(iconsDirectory, firstIconImageFileName);
    byte[] firstIconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip").toURI()));
    byte[] expectedIconsZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-2.zip").toURI()));
    byte[] expectedIconImageFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + expectedIconImageFileName).toURI()));
    when(hdsClientMock.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(firstIconZipFile));
    enterpriseReportingService.cacheDashboardIcons();
    assertThat(firstIconImageFile.exists()).isTrue();

    when(hdsClientMock.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(expectedIconsZipFile));
    enterpriseReportingService.cacheDashboardIcons();

    assertThat(firstIconImageFile.exists()).isFalse();
    assertDashboardIconImage(expectedIconImageFile, expectedIconImageFileName);
  }

  private void createSSOEmbedUrl_FeatureEnabled(
      String realmId,
      String expectedUserFirstName,
      String expectedUserLastName)
  {
    String expectedUrl = "looker.url.com";
    String expectedBaseUrl = "base.looker.com";
    String expectedUsernameAndRealm = "username@" + realmId;
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenReturn(new SSOEmbedUrlDTO(expectedUrl));
    when(hdsClientMock.get(EnterpriseReportingConfigDTO.class, ENTERPRISE_REPORTING_CONFIG_PATH))
        .thenReturn(new EnterpriseReportingConfigDTO(expectedBaseUrl));
    final Set<String> permissionsForUserPrincipalMock = mockGetPermissionsForUserPrincipal();
    final Set<String> applicationIdsForUserMock = mockGetApplicationIdsForUser();

    when(hdsClientMock.post(any(), anyString(), any())).thenReturn(new SSOEmbedUrlDTO(expectedUrl));
    when(mockMembershipMappingService.getPermissionsForUserPrincipal(any(), any()))
        .thenReturn(permissionsForUserPrincipalMock);
    when(mockMembershipMappingService.getApplicationIdsForUser(any(), any()))
        .thenReturn(applicationIdsForUserMock);
    SSOEmbedUrlDTO result = enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("test"));

    verify(hdsClientMock).post(eq(SSOEmbedUrlDTO.class), eq(ENTERPRISE_REPORTING_SSO_EMBED_URL_PATH),
        lookerSSOEmbedUrlHdsRequestArgumentCaptor.capture());
    SSOEmbedUrlRequest actual = lookerSSOEmbedUrlHdsRequestArgumentCaptor.getValue();
    assertThat(actual).isNotNull();
    assertThat(actual.userPermissions).containsExactlyInAnyOrderElementsOf(permissionsForUserPrincipalMock);
    assertThat(actual.applicationIds).containsExactlyInAnyOrderElementsOf(applicationIdsForUserMock);
    assertThat(actual.userFirstName).isEqualTo(expectedUserFirstName);
    assertThat(actual.userLastName).isEqualTo(expectedUserLastName);
    assertThat(actual.usernameAndRealm).isEqualTo(expectedUsernameAndRealm);
    assertThat(result).isNotNull();
    assertThat(result.url).isEqualTo(expectedUrl);
    assertThat(result.baseUrl).isEqualTo(expectedBaseUrl);
  }

  private static Set<String> mockGetPermissionsForUserPrincipal() {
    return new HashSet<>(Arrays.asList(Permission.EDIT_ROLES.getDisplayName(),
        Permission.WAIVE_POLICY_VIOLATIONS.getDisplayName()));
  }

  private static Set<String> mockGetApplicationIdsForUser() {
    return new HashSet<>(Arrays.asList("appId1", "appId2"));
  }

  private static AtomicReference<DashboardMetadataListDTO> mockGetLookerDashboardMetadata() {
    return new AtomicReference<>(new DashboardMetadataListDTO(Arrays.asList(generateLookerDashboardMetadata(),
        generateLookerDashboardMetadata(), generateLookerDashboardMetadata())));
  }

  private static DashboardMetadataDTO generateLookerDashboardMetadata() {
    return new DashboardMetadataDTO(RandomStringUtils.random(8), RandomStringUtils.random(8),
        RandomStringUtils.random(8), Collections.singletonList(RandomStringUtils.random(8)),
        RandomStringUtils.random(8), RandomStringUtils.random(8), 1, false);
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
  public void testGetIcon_valid() throws URISyntaxException {
    String iconName = "rolling-recap.svg";
    AtomicReference<DashboardMetadataListDTO> dashboardData = new AtomicReference<>(new DashboardMetadataListDTO(
        Collections.singletonList(new DashboardMetadataDTO(RandomStringUtils.random(8), RandomStringUtils.random(8),
            RandomStringUtils.random(8), Collections.singletonList(RandomStringUtils.random(8)),
            RandomStringUtils.random(8), iconName, 1, false))));
    createServiceWithDashboardMetadata(dashboardData);
    when(insightWork.getIerDashboardIconsDirectory()).thenReturn(new
        File(getClass().getResource("/EnterpriseReportingServiceTest/").toURI()));
    byte[] iconImage = enterpriseReportingService.getIcon(iconName);
    assertNotNull(iconImage);
  }

  @Test
  public void testGetIcon_notFound() {
    createServiceWithDashboardMetadata(mockGetLookerDashboardMetadata());
    assertThatThrownBy(() -> enterpriseReportingService.getIcon("rolling-recap1.svg"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testGetIcon_badRequest() {
    createServiceWithDashboardMetadata(mockGetLookerDashboardMetadata());
    assertThatThrownBy(() -> enterpriseReportingService.getIcon("../rolling-recap1.svg"))
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
    spyEnterpriseReportingService.currentVersion.set(-1);

    try {
      spyEnterpriseReportingService.execute(mockJobExecutionContext);
    }
    catch (JobExecutionException e) {
      fail("Unexpected exception thrown: " + e.getMessage());
    }
    assertEquals(1, spyEnterpriseReportingService.currentVersion.get());
    verify(spyEnterpriseReportingService, times(1)).cacheDashboardMetadata();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(
        JobBuilder.newJob(EnterpriseReportingService.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  private void createServiceWithDashboardMetadata(AtomicReference<DashboardMetadataListDTO> dashboardData) {
    insightWork = mock(InsightWork.class);
    enterpriseReportingService = new EnterpriseReportingService(hdsClientMock, currentUserMock, mockUserDAO,
        mockSamlUserDAO, mockMembershipMappingService, insightWork, mockConfigCache, dashboardData, 0,
        mockLatestVersionCache,mockTaskScheduler, mockConfiguration);
  }

  private void verifyScheduledTaskVersionCache(Integer latestVersion) {
    Map<String, String> expectedParameters = new HashMap<>();
    expectedParameters.put(EnterpriseReportingService.TASK_PARAM_CURRENT_VERSION, latestVersion.toString());
    verify(mockTaskScheduler)
        .scheduleOneTimeTaskForAllOtherNodes(enterpriseReportingService, expectedParameters);
  }
}
