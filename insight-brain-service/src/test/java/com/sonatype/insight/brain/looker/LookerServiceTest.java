/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.looker;

import java.util.concurrent.ExecutionException;
import javax.ws.rs.InternalServerErrorException;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.hds.DefaultHdsClient;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.cache.LoadingCache;
import com.google.inject.Binder;
import com.google.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.looker.LookerService.DEFAULT_CONFIG_CACHE_KEY;
import static com.sonatype.insight.brain.looker.LookerService.LOOKER_CONFIG_PATH;
import static com.sonatype.insight.brain.looker.LookerService.LOOKER_SSO_EMBED_URL_PATH;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
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

  @Inject
  private LookerService lookerService;

  private LoadingCache<String, LookerConfigDTO> mockConfigCache = mock(LoadingCache.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(DefaultHdsClient.class).toInstance(hdsClientMock);
    binder.bind(CurrentUser.class).toInstance(currentUserMock);
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
  public void testCreateSSOEmbedUrl_FeatureEnabled() {
    String expectedUrl = "looker.url.com";
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenReturn(new SSOEmbedUrlDTO(expectedUrl));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));
    SSOEmbedUrlDTO result = lookerService.createSSOEmbedUrl(ROOT_ORGANIZATION_ID, new LookerDashboardDTO("test"));
    verify(hdsClientMock).post(eq(SSOEmbedUrlDTO.class), eq(LOOKER_SSO_EMBED_URL_PATH), any());
    assertThat(result).isNotNull();
    assertThat(result.url).isEqualTo(expectedUrl);
  }

  @Test
  public void testCreateSSOEmbedUrl_FeatureDisabled() {
    disableFeature();
    assertThatExceptionOfType(NotAuthorizedException.class).isThrownBy(
        () -> lookerService.createSSOEmbedUrl(ROOT_ORGANIZATION_ID, new LookerDashboardDTO("test")));
  }

  @Test
  public void testCreateSSOEmbedUrl_MissingDashboardKey() {
    assertThatThrownBy(() -> lookerService.createSSOEmbedUrl(ROOT_ORGANIZATION_ID, new LookerDashboardDTO(null)))
        .isInstanceOf(BadRequestException.class).hasMessage("Dashboard is null or empty");
  }

  @Test
  public void testCreateSSOEmbedUrl_HdsBadRequest() {
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenThrow(new BadRequestException("Bad request"));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));
    assertThatThrownBy(() -> lookerService.createSSOEmbedUrl(ROOT_ORGANIZATION_ID, new LookerDashboardDTO("test")))
        .isInstanceOf(BadRequestException.class).hasMessage("Bad request");
  }

  @Test
  public void testCreateSSOEmbedUrl_HdsNotFound() {
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenThrow(new NotFoundException("Not found"));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));
    assertThatThrownBy(() -> lookerService.createSSOEmbedUrl(ROOT_ORGANIZATION_ID, new LookerDashboardDTO("test")))
        .isInstanceOf(NotFoundException.class).hasMessage("Not found");
  }

  @Test
  public void testCreateSSOEmbedUrl_LookerError() {
    String hdsError = "Error with Looker";
    when(hdsClientMock.post(any(), anyString(), any()))
        .thenThrow(new ConflictException(hdsError));
    when(currentUserMock.getUserPrincipal())
        .thenReturn(new UserPrincipal("username", "displayName", "test"));
    assertThatThrownBy(() -> lookerService.createSSOEmbedUrl(ROOT_ORGANIZATION_ID, new LookerDashboardDTO("test")))
        .isInstanceOf(ConflictException.class)
        .hasMessage(hdsError);
  }

  @Test
  public void testGetLookerConfig() {
    when(hdsClientMock.get(LookerConfigDTO.class, LOOKER_CONFIG_PATH))
        .thenReturn(new LookerConfigDTO("lookerBaseUrl"));
    LookerConfigDTO config = lookerService.getLookerConfig();
    assertThat(config.baseUrl).isEqualTo("lookerBaseUrl");
  }

  @Test
  public void testGetLookerConfig_Error() throws Exception {
    lookerService = new LookerService(hdsClientMock, currentUserMock, mockConfigCache);
    when(mockConfigCache.get(DEFAULT_CONFIG_CACHE_KEY)).thenThrow(
        new ExecutionException(new RuntimeException("error")));

    assertThatExceptionOfType(InternalServerErrorException.class).isThrownBy(() -> lookerService.getLookerConfig())
        .withMessage("unable to load looker configuration from sonatype data services");
  }

  private void enableFeature() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .LOOKER_INTEGRATED_ENTERPRISE_REPORTING.setEnabled(true);
  }

  private void disableFeature() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .LOOKER_INTEGRATED_ENTERPRISE_REPORTING.setEnabled(false);
  }
}
