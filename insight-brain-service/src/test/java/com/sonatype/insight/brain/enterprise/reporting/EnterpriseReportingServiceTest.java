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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.cache.LoadingCache;
import com.google.inject.Binder;
import com.google.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.DEFAULT_GUAVA_CACHE_KEY;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_CURRENT_VERSION_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_SSO_EMBED_URL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

  @Inject
  private EnterpriseReportingService enterpriseReportingService;

  @Inject
  private InsightWork insightWork;

  @Captor
  private ArgumentCaptor<SSOEmbedUrlRequest> lookerSSOEmbedUrlHdsRequestArgumentCaptor;

  private final LoadingCache<String, EnterpriseReportingConfigDTO> mockConfigCache = mock(LoadingCache.class);

  private final LoadingCache<String, Integer> mockLatestVersionCache = mock(LoadingCache.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    binder.bind(CurrentUser.class).toInstance(currentUserMock);
    binder.bind(UserDAO.class).toInstance(mockUserDAO);
    binder.bind(SamlUserDAO.class).toInstance(mockSamlUserDAO);
    binder.bind(MembershipMappingService.class).toInstance(mockMembershipMappingService);
    super.configure(binder);
  }

  @Before
  public void setup() {
    enableFeature();
  }

  @After
  public void after() {
    disableFeature();
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureEnabled_InternalRealm() {
    when(currentUserMock.getUserPrincipal()).thenReturn(new UserPrincipal("username", "displayName", InternalRealm.ID));
    createSSOEmbedUrl_FeatureEnabled();
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureEnabled_SamlRealm() {
    when(currentUserMock.getUserPrincipal()).thenReturn(new UserPrincipal("username", "displayName", SamlRealm.ID));
    createSSOEmbedUrl_FeatureEnabled();
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureEnabled_OtherRealm() {
    when(currentUserMock.getUserPrincipal()).thenReturn(new UserPrincipal("username", "displayName", "other"));
    createSSOEmbedUrl_FeatureEnabled();
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureEnabled_ConfigError() throws Exception {
    enterpriseReportingService = new EnterpriseReportingService(hdsClientMock, currentUserMock, mockUserDAO,
        mockSamlUserDAO, mockMembershipMappingService, insightWork, mockConfigCache,
        mockGetLookerDashboardMetadata(),
        0, mockLatestVersionCache);
    String expectedUrl = "looker.url.com";
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenReturn(new SSOEmbedUrlDTO(expectedUrl));
    when(mockConfigCache.get(DEFAULT_GUAVA_CACHE_KEY)).thenThrow(
        new ExecutionException(new RuntimeException("error")));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatExceptionOfType(InternalServerException.class)
        .isThrownBy(() -> enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("test")))
        .withMessage("unable to load Enterprise Reporting configuration from Sonatype Data Services");
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureDisabled() {
    disableFeature();
    assertThatExceptionOfType(NotAuthorizedException.class).isThrownBy(
        () -> enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("test")));
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
  public void testGetBaseUrl_Error() throws Exception {
    enterpriseReportingService = new EnterpriseReportingService(hdsClientMock, currentUserMock, mockUserDAO,
        mockSamlUserDAO, mockMembershipMappingService, insightWork, mockConfigCache,
        mockGetLookerDashboardMetadata(), 0, mockLatestVersionCache);
    when(mockConfigCache.get(DEFAULT_GUAVA_CACHE_KEY)).thenThrow(
        new ExecutionException(new RuntimeException("error")));

    assertThatExceptionOfType(InternalServerException.class).isThrownBy(() -> enterpriseReportingService.getBaseUrl())
        .withMessage("unable to load Enterprise Reporting configuration from Sonatype Data Services");
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

    // Initial Load - Version loader is triggered.
    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.get().dashboardMetadata);
    verify(hdsClientMock, times(1)).get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(hdsClientMock, times(1)).get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);

    Mockito.clearInvocations(hdsClientMock);

    // Second call - Loader is not yet triggered - Versions are the same.
    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.get().dashboardMetadata);
    verify(hdsClientMock, times(0)).get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(hdsClientMock, times(1)).get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);

    Mockito.clearInvocations(hdsClientMock);

    // Third call - Loader is triggered - Different versions.
    enterpriseReportingService.currentVersionCache.invalidateAll();
    when(hdsClientMock.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(2));
    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.get().dashboardMetadata);
    verify(hdsClientMock, times(1)).get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(hdsClientMock, times(1)).get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
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

  private void createSSOEmbedUrl_FeatureEnabled() {
    String expectedUrl = "looker.url.com";
    String expectedBaseUrl = "base.looker.com";
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenReturn(new SSOEmbedUrlDTO(expectedUrl));
    when(hdsClientMock.get(EnterpriseReportingConfigDTO.class, ENTERPRISE_REPORTING_CONFIG_PATH))
        .thenReturn(new EnterpriseReportingConfigDTO(expectedBaseUrl));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));
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
    assertThat(result).isNotNull();
    assertThat(result.url).isEqualTo(expectedUrl);
    assertThat(result.baseUrl).isEqualTo(expectedBaseUrl);
  }

  private void enableFeature() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .INTEGRATED_ENTERPRISE_REPORTING.setEnabled(true);
  }

  private void disableFeature() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .INTEGRATED_ENTERPRISE_REPORTING.setEnabled(false);
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
  public void testGetIcon_notFound() throws URISyntaxException {
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

  private void createServiceWithDashboardMetadata(AtomicReference<DashboardMetadataListDTO> dashboardData) {
    insightWork = mock(InsightWork.class);
    enterpriseReportingService = new EnterpriseReportingService(hdsClientMock, currentUserMock, mockUserDAO,
        mockSamlUserDAO, mockMembershipMappingService, insightWork, mockConfigCache, dashboardData, 0,
        mockLatestVersionCache);
  }
}
