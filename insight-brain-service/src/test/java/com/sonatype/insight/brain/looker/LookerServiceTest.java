/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.looker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.InternalServerErrorException;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.hds.DefaultHdsClient;
import com.sonatype.insight.brain.ier.IerDashboardMetadataDTO;
import com.sonatype.insight.brain.ier.IerDashboardMetadataListDTO;
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
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.InternalServerException;

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

import static com.sonatype.insight.brain.looker.LookerService.LOOKER_SSO_EMBED_URL_PATH;
import static com.sonatype.insight.brain.looker.LookerService.LOOKER_CONFIG_PATH;
import static com.sonatype.insight.brain.looker.LookerService.LOOKER_ICONS_PATH;
import static com.sonatype.insight.brain.looker.LookerService.LOOKER_DASHBOARDS_METADATA_PATH;
import static com.sonatype.insight.brain.looker.LookerService.DEFAULT_CONFIG_CACHE_KEY;
import static com.sonatype.insight.brain.looker.LookerService.DEFAULT_DASHBOARD_CACHE_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LookerServiceTest
    extends AbstractComponentTest
{
  @Mock
  private DefaultHdsClient hdsClientMock;

  @Mock
  private CurrentUser currentUserMock;

  @Mock
  private MembershipMappingService mockMembershipMappingService;

  @Mock
  private UserDAO mockUserDAO;

  @Mock
  private SamlUserDAO mockSamlUserDAO;

  @Inject
  private LookerService lookerService;

  @Inject
  private InsightWork insightWork;

  @Captor
  private ArgumentCaptor<LookerSSOEmbedUrlHdsRequest> lookerSSOEmbedUrlHdsRequestArgumentCaptor;

  private LoadingCache<String, LookerConfigDTO> mockConfigCache = mock(LoadingCache.class);

  private LoadingCache<String, IerDashboardMetadataListDTO> mockDashboardCache = mock(LoadingCache.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(DefaultHdsClient.class).toInstance(hdsClientMock);
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
    lookerService = new LookerService(hdsClientMock, currentUserMock, mockUserDAO, mockSamlUserDAO,
        mockMembershipMappingService, insightWork, mockConfigCache, mockDashboardCache);
    String expectedUrl = "looker.url.com";
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenReturn(new SSOEmbedUrlDTO(expectedUrl));
    when(mockConfigCache.get(DEFAULT_CONFIG_CACHE_KEY)).thenThrow(
        new ExecutionException(new RuntimeException("error")));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatExceptionOfType(InternalServerException.class)
        .isThrownBy(() -> lookerService.createSSOEmbedUrl(new LookerDashboardDTO("test")))
        .withMessage("unable to load Enterprise Reporting configuration from Sonatype Data Services");
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureDisabled() {
    disableFeature();
    assertThatExceptionOfType(NotAuthorizedException.class).isThrownBy(
        () -> lookerService.createSSOEmbedUrl(new LookerDashboardDTO("test")));
  }

  @Test
  public void testCreateSSOEmbedUrl_MissingDashboardKey() {
    assertThatThrownBy(() -> lookerService.createSSOEmbedUrl(new LookerDashboardDTO(null)))
        .isInstanceOf(BadRequestException.class).hasMessage("Dashboard is null or empty");
  }

  @Test
  public void testCreateSSOEmbedUrl_HdsBadRequest() {
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenThrow(new BadRequestException("Bad request"));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatThrownBy(() -> lookerService.createSSOEmbedUrl(new LookerDashboardDTO("test")))
        .isInstanceOf(BadRequestException.class).hasMessage("Bad request");
  }

  @Test
  public void testCreateSSOEmbedUrl_HdsNotFound() {
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenThrow(new NotFoundException("Not found"));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatThrownBy(() -> lookerService.createSSOEmbedUrl(new LookerDashboardDTO("test")))
        .isInstanceOf(NotFoundException.class).hasMessage("Not found");
  }

  @Test
  public void testCreateSSOEmbedUrl_LookerError() {
    String hdsError = "Error with Looker";
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenThrow(new ConflictException(hdsError));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatThrownBy(() -> lookerService.createSSOEmbedUrl(new LookerDashboardDTO("test")))
        .isInstanceOf(ConflictException.class)
        .hasMessage(hdsError);
  }

  @Test
  public void testGetBaseUrl() {
    String expectedBaseUrl = "https://sonatypeexternaldev.cloud.looker.com/";
    when(hdsClientMock.get(LookerConfigDTO.class, LOOKER_CONFIG_PATH))
        .thenReturn(new LookerConfigDTO(expectedBaseUrl));
    assertThat(lookerService.getBaseUrl()).isEqualTo(expectedBaseUrl);
  }

  @Test
  public void testGetLookerConfig_Error() throws Exception {
    lookerService = new LookerService(hdsClientMock, currentUserMock, mockUserDAO, mockSamlUserDAO,
        mockMembershipMappingService, insightWork, mockConfigCache, mockDashboardCache);
    when(mockConfigCache.get(DEFAULT_CONFIG_CACHE_KEY)).thenThrow(
        new ExecutionException(new RuntimeException("error")));

    assertThatExceptionOfType(InternalServerException.class).isThrownBy(() -> lookerService.getBaseUrl())
        .withMessage("unable to load Enterprise Reporting configuration from Sonatype Data Services");
  }

  @Test
  public void testGetLookerDashboardMetadata() {
    IerDashboardMetadataListDTO expected = mockGetLookerDashboardMetadata();
    when(hdsClientMock.get(InputStream.class, LOOKER_ICONS_PATH)).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(hdsClientMock.get(IerDashboardMetadataListDTO.class, LOOKER_DASHBOARDS_METADATA_PATH))
        .thenReturn(expected);

    assertThat(lookerService.getLookerDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.dashboardMetadata);
  }

  @Test
  public void testGetLookerDashboardMetadata_Error() throws Exception {
    lookerService = new LookerService(hdsClientMock, currentUserMock, mockUserDAO, mockSamlUserDAO,
        mockMembershipMappingService, insightWork, mockConfigCache, mockDashboardCache);
    when(mockDashboardCache.get(DEFAULT_DASHBOARD_CACHE_KEY)).thenThrow(
        new ExecutionException(new RuntimeException("error")));

    assertThatExceptionOfType(InternalServerErrorException.class).isThrownBy(() ->
            lookerService.getLookerDashboardMetadata())
        .withMessage("unable to load Integrated Enterprise Reporting metadata from Sonatype Data Services");
  }

  @Test
  public void testEvaluateDashboardIcons_FirstCacheLoad() throws IOException {
    String expectedIconImageFileName = "icon-1.png";
    byte[] expectedIconsZipFile = Files
        .readAllBytes(Paths.get(getClass().getResource("/LookerServiceTest/icon-1.zip").getPath()));
    byte[] expectedIconImageFile = Files
        .readAllBytes(Paths.get(getClass().getResource("/LookerServiceTest/" + expectedIconImageFileName).getPath()));
    when(hdsClientMock.get(InputStream.class, LOOKER_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(expectedIconsZipFile));
    lookerService.downloadAndCacheDashboardIcons();

    assertDashboardIconImage(expectedIconImageFile, expectedIconImageFileName);
  }

  @Test
  public void testEvaluateDashboardIcons_FirstCacheLoad_MultipleIcons() throws IOException {
    String expectedIconImageFileName = "icon-1.png";
    String expectedSecondIconImageFileName = "icon-2.png";
    byte[] expectedIconsZipFile = Files
        .readAllBytes(Paths.get(getClass().getResource("/LookerServiceTest/icons.zip").getPath()));
    byte[] expectedIconImageFile = Files
        .readAllBytes(Paths.get(getClass().getResource("/LookerServiceTest/" + expectedIconImageFileName).getPath()));
    byte[] expectedSecondIconImageFile = Files
        .readAllBytes(Paths.get(getClass().getResource("/LookerServiceTest/" + expectedSecondIconImageFileName)
            .getPath()));
    when(hdsClientMock.get(InputStream.class, LOOKER_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(expectedIconsZipFile));
    lookerService.downloadAndCacheDashboardIcons();

    assertDashboardIconImage(expectedSecondIconImageFile, expectedSecondIconImageFileName);
  }

  @Test
  public void testEvaluateDashboardIcons_CacheReloadWithNoIconUpdates() throws IOException {
    String expectedIconImageFileName = "icon-1.png";
    byte[] expectedIconsZipFile = Files
        .readAllBytes(Paths.get(getClass().getResource("/LookerServiceTest/icon-1.zip").getPath()));
    byte[] expectedIconImageFile = Files
        .readAllBytes(Paths.get(getClass().getResource("/LookerServiceTest/" + expectedIconImageFileName).getPath()));
    when(hdsClientMock.get(InputStream.class, LOOKER_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(expectedIconsZipFile));
    lookerService.downloadAndCacheDashboardIcons();
    when(hdsClientMock.get(InputStream.class, LOOKER_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(expectedIconsZipFile));
    lookerService.downloadAndCacheDashboardIcons();

    assertDashboardIconImage(expectedIconImageFile, expectedIconImageFileName);
  }

  @Test
  public void testEvaluateDashboardIcons_CacheReloadWithIconUpdates() throws IOException {
    String firstIconImageFileName = "icon-1.png";
    String expectedIconImageFileName = "icon-2.png";
    File iconsDirectory = insightWork.getIerDashboardIconsDirectory();
    File firstIconImageFile = new File(iconsDirectory, firstIconImageFileName);
    byte[] firstIconZipFile = Files
        .readAllBytes(Paths.get(getClass().getResource("/LookerServiceTest/icon-1.zip").getPath()));
    byte[] expectedIconsZipFile = Files
        .readAllBytes(Paths.get(getClass().getResource("/LookerServiceTest/icon-2.zip").getPath()));
    byte[] expectedIconImageFile = Files
        .readAllBytes(Paths.get(getClass().getResource("/LookerServiceTest/" + expectedIconImageFileName).getPath()));
    when(hdsClientMock.get(InputStream.class, LOOKER_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(firstIconZipFile));
    lookerService.downloadAndCacheDashboardIcons();
    assertThat(firstIconImageFile.exists()).isTrue();

    when(hdsClientMock.get(InputStream.class, LOOKER_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(expectedIconsZipFile));
    lookerService.downloadAndCacheDashboardIcons();

    assertThat(firstIconImageFile.exists()).isFalse();
    assertDashboardIconImage(expectedIconImageFile, expectedIconImageFileName);
  }

  private void createSSOEmbedUrl_FeatureEnabled() {
    String expectedUrl = "looker.url.com";
    String expectedBaseUrl = "base.looker.com";
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenReturn(new SSOEmbedUrlDTO(expectedUrl));
    when(hdsClientMock.get(LookerConfigDTO.class, LOOKER_CONFIG_PATH))
        .thenReturn(new LookerConfigDTO(expectedBaseUrl));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));
    final Set<String> permissionsForUserPrincipalMock = mockGetPermissionsForUserPrincipal();
    final Set<String> applicationIdsForUserMock = mockGetApplicationIdsForUser();

    when(hdsClientMock.post(any(), anyString(), any())).thenReturn(new SSOEmbedUrlDTO(expectedUrl));
    when(mockMembershipMappingService.getPermissionsForUserPrincipal(any(), any()))
        .thenReturn(permissionsForUserPrincipalMock);
    when(mockMembershipMappingService.getApplicationIdsForUser(any(), any()))
        .thenReturn(applicationIdsForUserMock);
    SSOEmbedUrlDTO result = lookerService.createSSOEmbedUrl(new LookerDashboardDTO("test"));

    verify(hdsClientMock).post(eq(SSOEmbedUrlDTO.class), eq(LOOKER_SSO_EMBED_URL_PATH),
        lookerSSOEmbedUrlHdsRequestArgumentCaptor.capture());
    LookerSSOEmbedUrlHdsRequest actual = lookerSSOEmbedUrlHdsRequestArgumentCaptor.getValue();
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

  private static IerDashboardMetadataListDTO mockGetLookerDashboardMetadata() {
    return new IerDashboardMetadataListDTO(Arrays.asList(generateLookerDashboardMetadata(),
        generateLookerDashboardMetadata(), generateLookerDashboardMetadata()));
  }

  private static IerDashboardMetadataDTO generateLookerDashboardMetadata() {
    return new IerDashboardMetadataDTO(RandomStringUtils.random(8), RandomStringUtils.random(8),
        RandomStringUtils.random(8), Collections.singletonList(RandomStringUtils.random(8)),
        RandomStringUtils.random(8), RandomStringUtils.random(8), 1, false);
  }

  private void assertDashboardIconImage(byte[] expectedIconsImageBytes,
                                        String expectedIconImageFileName) throws IOException
  {
    File iconsDirectory = insightWork.getIerDashboardIconsDirectory();
    File actualIconImageFile = new File(iconsDirectory, expectedIconImageFileName);
    byte[] actualIconsImageFileBytes = Files.readAllBytes(actualIconImageFile.toPath());
    assertThat(actualIconImageFile.exists()).isTrue();
    assertThat(actualIconImageFile.isFile()).isTrue();
    assertThat(actualIconsImageFileBytes).containsExactly(expectedIconsImageBytes);
  }
}
