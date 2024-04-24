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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_CURRENT_VERSION_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH;
import static com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService.ENTERPRISE_REPORTING_SSO_EMBED_URL_PATH;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.Assertions.assertThat;
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

public class EnterpriseReportingServiceTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private CurrentUser mockCurrentUser;

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
  private ArgumentCaptor<SSOEmbedUrlRequest> ssoEmbedUrlRequestArgumentCaptor;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    binder.bind(CurrentUser.class).toInstance(mockCurrentUser);
    binder.bind(UserDAO.class).toInstance(mockUserDAO);
    binder.bind(SamlUserDAO.class).toInstance(mockSamlUserDAO);
    binder.bind(MembershipMappingService.class).toInstance(mockMembershipMappingService);
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    super.configure(binder);
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureEnabled_InternalRealm() {
    when(mockCurrentUser.getUserPrincipal()).thenReturn(new UserPrincipal("username", "displayName", InternalRealm.ID));
    when(mockUserDAO.getByUsernameNotNull("username")).thenReturn(
        new User("username", "password", "firstName", "lastName", "email"));
    createSSOEmbedUrl_FeatureEnabled(InternalRealm.ID, "firstName", "lastName");
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureEnabled_SamlRealm() {
    when(mockCurrentUser.getUserPrincipal()).thenReturn(new UserPrincipal("username", "displayName", SamlRealm.ID));
    when(mockSamlUserDAO.getByUsernameNotNull("username")).thenReturn(
        new SamlUser("username", "firstName", "lastName", "email", Collections.emptySet()));
    createSSOEmbedUrl_FeatureEnabled(SamlRealm.ID, "firstName", "lastName");
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureEnabled_OtherRealm() {
    when(mockCurrentUser.getUserPrincipal()).thenReturn(new UserPrincipal("username", "displayName", "other"));
    createSSOEmbedUrl_FeatureEnabled("other", "displayName", "");
  }

  @Test
  public void testCreateSSOEmbedUrl_MissingDashboardKey() {
    assertThatThrownBy(() -> enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO(null)))
        .isInstanceOf(BadRequestException.class).hasMessage("Dashboard is null or empty");
  }

  @Test
  public void testCreateSSOEmbedUrl_HdsBadRequest() {
    when(mockHdsClient.post(any(), anyString(), any()))
        .thenThrow(new BadRequestException("Bad request"));
    when(mockCurrentUser.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatThrownBy(() -> enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("test")))
        .isInstanceOf(BadRequestException.class).hasMessage("Bad request");
  }

  @Test
  public void testCreateSSOEmbedUrl_HdsNotFound() {
    when(mockHdsClient.post(any(), anyString(), any()))
        .thenThrow(new NotFoundException("Not found"));
    when(mockCurrentUser.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatThrownBy(() -> enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("test")))
        .isInstanceOf(NotFoundException.class).hasMessage("Not found");
  }

  @Test
  public void testCreateSSOEmbedUrl_LookerError() {
    String hdsError = "Error with Looker";
    when(mockHdsClient.post(any(), anyString(), any()))
        .thenThrow(new ConflictException(hdsError));
    when(mockCurrentUser.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));

    assertThatThrownBy(() -> enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("test")))
        .isInstanceOf(ConflictException.class)
        .hasMessage(hdsError);
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
    testAs(tenant1, t1 -> {
      assertThat(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).isEqualTo(expectedTenant1BaseUrl);
    });
    Tenant tenant2 = testAsNewTenant(testName, t1 -> {
      when(mockHdsClient.get(EnterpriseReportingConfigDTO.class,
          EnterpriseReportingService.ENTERPRISE_REPORTING_CONFIG_PATH))
          .thenReturn(new EnterpriseReportingConfigDTO(expectedTenant2BaseUrl));
    });
    testAs(tenant2, t1 -> {
      assertThat(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).isEqualTo(expectedTenant2BaseUrl);
    });
    testAs(tenant1, t1 -> {
      assertThat(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl()).isEqualTo(expectedTenant1BaseUrl);
    });
    testAs(tenant2, t1 -> {
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

    verify(mockHdsClient, times(1)).get(DashboardsVersionDTO.class, ENTERPRISE_REPORTING_CURRENT_VERSION_PATH);
    verify(mockHdsClient, times(1)).get(DashboardMetadataListDTO.class, ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH);
    verify(mockHdsClient, never()).get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH);

    verifyScheduledTaskVersionCache(1);
    Mockito.clearInvocations(mockHdsClient);
    Mockito.clearInvocations(mockTaskScheduler);

    assertThat(enterpriseReportingService.getDashboardMetadata().dashboardMetadata)
        .hasSameElementsAs(expected.dashboardMetadata);

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
        .getResource("/EnterpriseReportingServiceTest/" + firstIconImageFileName).toURI()));
    byte[] firstIconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip").toURI()));
    DashboardMetadataDTO firstDashboardMetadataDTO = new DashboardMetadataDTO(
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        Collections.singletonList(RandomStringUtils.random(8)),
        RandomStringUtils.random(8),
        firstIconImageFileName,
        1,
        false
    );
    DashboardMetadataListDTO firstDashboardMetadataListDTO =
        new DashboardMetadataListDTO(Collections.singletonList(firstDashboardMetadataDTO));
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));
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
        .getResource("/EnterpriseReportingServiceTest/" + firstIconImageFileName).toURI()));
    byte[] iconsZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icons.zip").toURI()));
    DashboardMetadataDTO firstDashboardMetadataDTO = new DashboardMetadataDTO(
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        Collections.singletonList(RandomStringUtils.random(8)),
        RandomStringUtils.random(8),
        firstIconImageFileName,
        1,
        false
    );
    String secondIconImageFileName = "icon-2.png";
    byte[] secondIconBytes = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + secondIconImageFileName).toURI()));
    DashboardMetadataDTO secondDashboardMetadataDTO = new DashboardMetadataDTO(
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        Collections.singletonList(RandomStringUtils.random(8)),
        RandomStringUtils.random(8),
        secondIconImageFileName,
        1,
        false
    );
    DashboardMetadataListDTO dashboardMetadataListDTO =
        new DashboardMetadataListDTO(Arrays.asList(firstDashboardMetadataDTO, secondDashboardMetadataDTO));
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));
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
        .getResource("/EnterpriseReportingServiceTest/" + firstIconImageFileName).toURI()));
    byte[] firstIconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip").toURI()));
    DashboardMetadataDTO firstDashboardMetadataDTO = new DashboardMetadataDTO(
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        Collections.singletonList(RandomStringUtils.random(8)),
        RandomStringUtils.random(8),
        firstIconImageFileName,
        1,
        false
    );
    DashboardMetadataListDTO firstDashboardMetadataListDTO =
        new DashboardMetadataListDTO(Collections.singletonList(firstDashboardMetadataDTO));
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));
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
        .getResource("/EnterpriseReportingServiceTest/" + firstIconImageFileName).toURI()));
    byte[] firstIconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip").toURI()));
    DashboardMetadataDTO firstDashboardMetadataDTO = new DashboardMetadataDTO(
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        Collections.singletonList(RandomStringUtils.random(8)),
        RandomStringUtils.random(8),
        firstIconImageFileName,
        1,
        false
    );
    DashboardMetadataListDTO firstDashboardMetadataListDTO =
        new DashboardMetadataListDTO(Collections.singletonList(firstDashboardMetadataDTO));

    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(firstDashboardMetadataListDTO);
    when(mockHdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(firstIconZipFile));

    assertThat(enterpriseReportingService.getIcon(firstIconImageFileName)).isEqualTo(firstIconBytes);

    assertDashboardIconImage(firstIconBytes, firstIconImageFileName);

    String secondIconImageFileName = "icon-2.png";
    byte[] secondIconBytes = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/" + secondIconImageFileName).toURI()));
    byte[] secondIconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-2.zip").toURI()));
    DashboardMetadataDTO secondDashboardMetadataDTO = new DashboardMetadataDTO(
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        RandomStringUtils.random(8),
        Collections.singletonList(RandomStringUtils.random(8)),
        RandomStringUtils.random(8),
        secondIconImageFileName,
        1,
        false
    );
    DashboardMetadataListDTO secondDashboardMetadataListDTO =
        new DashboardMetadataListDTO(Collections.singletonList(secondDashboardMetadataDTO));

    enterpriseReportingService.currentDashboardsVersionSupplier.reset();
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(2));
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(secondDashboardMetadataListDTO);
    when(mockHdsClient.get(InputStream.class, ENTERPRISE_REPORTING_DASHBOARD_ICONS_PATH))
        .thenReturn(new ByteArrayInputStream(secondIconZipFile));

    assertThat(enterpriseReportingService.getIcon(secondIconImageFileName)).isEqualTo(secondIconBytes);

    assertDashboardIconImage(secondIconBytes, secondIconImageFileName);
  }

  private void createSSOEmbedUrl_FeatureEnabled(
      String realmId,
      String expectedUserFirstName,
      String expectedUserLastName)
  {
    String expectedUrl = "looker.url.com";
    String expectedBaseUrl = "base.looker.com";
    String expectedUsernameAndRealm = "username@" + realmId;
    when(mockHdsClient.post(any(), anyString(), any()))
        .thenReturn(new SSOEmbedUrlDTO(expectedUrl));
    when(mockHdsClient.get(EnterpriseReportingConfigDTO.class, ENTERPRISE_REPORTING_CONFIG_PATH))
        .thenReturn(new EnterpriseReportingConfigDTO(expectedBaseUrl));
    final Set<String> permissionsForUserPrincipalMock = mockGetPermissionsForUserPrincipal();
    final Set<String> applicationIdsForUserMock = mockGetApplicationIdsForUser();

    when(mockHdsClient.post(any(), anyString(), any())).thenReturn(new SSOEmbedUrlDTO(expectedUrl));
    when(mockMembershipMappingService.getPermissionsForUserPrincipal(any(), any()))
        .thenReturn(permissionsForUserPrincipalMock);
    when(mockMembershipMappingService.getApplicationIdsForUser(any(), any()))
        .thenReturn(applicationIdsForUserMock);
    SSOEmbedUrlDTO result = enterpriseReportingService.createSSOEmbedUrl(new DashboardRequestDTO("test"));

    verify(mockHdsClient).post(eq(SSOEmbedUrlDTO.class), eq(ENTERPRISE_REPORTING_SSO_EMBED_URL_PATH),
        ssoEmbedUrlRequestArgumentCaptor.capture());
    SSOEmbedUrlRequest actual = ssoEmbedUrlRequestArgumentCaptor.getValue();
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

  private static DashboardMetadataListDTO mockGetLookerDashboardMetadata() {
    return new DashboardMetadataListDTO(Arrays.asList(generateLookerDashboardMetadata(),
        generateLookerDashboardMetadata(), generateLookerDashboardMetadata()));
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
  public void testGetIcon_valid_onlyRequestedOnce() throws Exception {
    String iconName = "icon-1.png";
    DashboardMetadataListDTO dashboardMetadataListDTO = new DashboardMetadataListDTO(Collections.singletonList(
        new DashboardMetadataDTO(RandomStringUtils.random(8), RandomStringUtils.random(8), RandomStringUtils.random(8),
            Collections.singletonList(RandomStringUtils.random(8)), RandomStringUtils.random(8), iconName, 1, false)));
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(dashboardMetadataListDTO);
    byte[] iconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip").toURI()));
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
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(
        new DashboardMetadataListDTO(Collections.emptyList()));

    assertThatThrownBy(() -> enterpriseReportingService.getIcon("rolling-recap1.svg"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testGetIcon_badRequest() throws Exception {
    String iconName = "icon-1.png";
    DashboardMetadataListDTO dashboardMetadataListDTO = new DashboardMetadataListDTO(Collections.singletonList(
        new DashboardMetadataDTO(RandomStringUtils.random(8), RandomStringUtils.random(8), RandomStringUtils.random(8),
            Collections.singletonList(RandomStringUtils.random(8)), RandomStringUtils.random(8), iconName, 1, false)));
    when(mockHdsClient.get(DashboardsVersionDTO.class,
        ENTERPRISE_REPORTING_CURRENT_VERSION_PATH)).thenReturn(new DashboardsVersionDTO(1));
    when(mockHdsClient.get(DashboardMetadataListDTO.class,
        ENTERPRISE_REPORTING_DASHBOARDS_METADATA_PATH)).thenReturn(dashboardMetadataListDTO);
    byte[] iconZipFile = Files.readAllBytes(Paths.get(getClass()
        .getResource("/EnterpriseReportingServiceTest/icon-1.zip").toURI()));
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

  private void verifyScheduledTaskVersionCache(Integer latestVersion) {
    Map<String, String> expectedParameters = new HashMap<>();
    expectedParameters.put(EnterpriseReportingService.TASK_PARAM_CURRENT_VERSION, latestVersion.toString());
    verify(mockTaskScheduler)
        .scheduleOneTimeTaskForAllOtherNodes(enterpriseReportingService, expectedParameters);
  }
}
