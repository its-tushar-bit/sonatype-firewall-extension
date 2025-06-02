/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZscalerFormatDAO;
import com.sonatype.insight.brain.dataaccess.zscaler.ZScalerMetricsDAO;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.zscaler.ApiZScalerService.ApiZScalerQuotaDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.test.LogOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.cache.Cache;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiZScalerServiceTest
{
  private ZScalerConfiguration config;

  private List<String> urls;

  private String url = "http://example-url";

  private List<ZscalerFormat> zscalerFormats;

  @Mock
  private ZScalerClient client;

  @Rule
  public LogOutput logOutput = new LogOutput(ApiZScalerService.class);

  @Mock
  private ZScalerConfigurationDAO configurationDAO;

  @Mock
  private ZscalerFormatDAO zscalerFormatDAO;

  @Mock
  private ZScalerMetricsDAO metricsDAO;

  @Mock
  private PasswordHandler passwordHandler;

  @Mock
  private Cache<String, ZScalerQuota> cache;

  @Spy
  @InjectMocks
  private ApiZScalerService underTest;

  @Before
  public void setUp() throws Exception {
    config = new ZScalerConfiguration();
    config.setUsername("user");
    config.setPassword("password");
    config.setApikey("abcdefghijkl");
    config.setHostname("host");

    urls = List.of(url);

    underTest = Mockito.spy(new ApiZScalerService(configurationDAO, zscalerFormatDAO,
        metricsDAO, passwordHandler, client, cache));
  }

  @Test
  public void testUpdateCategory() throws Exception {
    when(configurationDAO.get()).thenReturn(config);
    when(client.getZScalerQuota(anyString())).thenReturn(new ZScalerQuota(0, 100));

    underTest.updateCategory(ZScalerSupportedFormat.NPM, urls);

    verify(client).getCustomUrlCategories(anyString());
    verify(client).createCustomUrlCategory(anyString(), anyString(), anyList());
    verify(configurationDAO).get();
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_noConfiguration() throws Exception {
    when(configurationDAO.get()).thenReturn(null);

    assertThrows("No zScaler configuration found", BadRequestException.class,
        () -> underTest.updateCategory(ZScalerSupportedFormat.NPM, urls));

    assertThat(logOutput).atWarnLevel().contains("No zScaler configuration found");
  }

  @Test
  public void testUpdateCategory_categoryExistsAndUpdatedSuccessfully() {
    ZScalerCategory existingCategory = new ZScalerCategory();
    existingCategory.setId("npm-category");
    existingCategory.setConfiguredName("sonatype-npm-shadow-download-defense");
    existingCategory.setCustomCategory(true);
    existingCategory.setUrls(List.of("https://npmjs.org/npm/"));

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(100, 200);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(List.of(existingCategory));

    underTest.updateCategory(ZScalerSupportedFormat.NPM, urls);

    verify(client).getCustomUrlCategories(config.getHostname());
    verify(client).updateCustomUrlCategories(eq(config.getHostname()), eq("sonatype-npm-shadow-download-defense"),
        eq("npm-category"), eq(List.of(url)));
    verify(client, never()).createCustomUrlCategory(anyString(), anyString(), anyList());
    assertThat(logOutput).atInfoLevel()
        .contains("sonatype-npm-shadow-download-defense category with id npm-category already exists, updating it");
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_categoryDoesNotExistAndCreatedSuccessfully() {
    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(100, 200);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(Collections.emptyList());

    underTest.updateCategory(ZScalerSupportedFormat.NPM, urls);

    verify(client).getCustomUrlCategories(config.getHostname());
    verify(client, never()).updateCustomUrlCategories(anyString(), anyString(), anyString(), anyList());
    verify(client).createCustomUrlCategory(eq(config.getHostname()), eq("sonatype-npm-shadow-download-defense"),
        anyList());
    assertThat(logOutput).atInfoLevel()
        .contains("sonatype-npm-shadow-download-defense category does not exist, creating it");
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_whenQuotaWouldBeExceeded() throws Exception {
    ZScalerCategory existingCategory = new ZScalerCategory();
    existingCategory.setId("npm-category");
    existingCategory.setConfiguredName("sonatype-npm-shadow-download-defense");
    existingCategory.setCustomCategory(true);
    existingCategory.setCustomUrlsCount(1);
    existingCategory.setUrls(List.of("https://npmjs.org/npm/"));

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(100, 0);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(List.of(existingCategory));

    underTest.updateCategory(ZScalerSupportedFormat.NPM,
        List.of("http://example-url1", "http://example-url2", "http://example-url3"));

    verify(client).updateCustomUrlCategories(eq(config.getHostname()), eq(existingCategory.getConfiguredName()),
        eq(existingCategory.getId()), eq(List.of("http://example-url1")));
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_whenQuotaWouldNotBeExceeded() throws Exception {
    ZScalerCategory existingCategory = new ZScalerCategory();
    existingCategory.setId("npm-category");
    existingCategory.setConfiguredName("sonatype-npm-shadow-download-defense");
    existingCategory.setCustomCategory(true);
    existingCategory.setCustomUrlsCount(1);
    existingCategory.setUrls(List.of("https://npmjs.org/npm/"));

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(80, 20);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(List.of(existingCategory));

    underTest.updateCategory(ZScalerSupportedFormat.NPM,
        List.of("http://example-url1", "http://example-url2", "http://example-url3"));

    verify(client).updateCustomUrlCategories(eq(config.getHostname()), eq(existingCategory.getConfiguredName()),
        eq(existingCategory.getId()), eq(List.of("http://example-url1", "http://example-url2", "http://example-url3")));
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_whenQuotaExceededMax() throws Exception {
    ZScalerCategory existingCategory = new ZScalerCategory();
    existingCategory.setId("npm-category");
    existingCategory.setConfiguredName("sonatype-npm-shadow-download-defense");
    existingCategory.setCustomCategory(true);
    existingCategory.setCustomUrlsCount(0);
    existingCategory.setUrls(List.of("https://npmjs.org/npm/"));

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(100, 0);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(List.of(existingCategory));

    underTest.updateCategory(ZScalerSupportedFormat.NPM,
        List.of("http://example-url1", "http://example-url2", "http://example-url3"));

    verify(client, never()).updateCustomUrlCategories(anyString(), anyString(), anyString(), anyList());
    verify(metricsDAO, never()).set(any());
  }

  @Test
  public void testUpdateCategory_whenQuotaIsNotCached() {
    when(configurationDAO.get()).thenReturn(config);
    when(cache.getIfPresent(anyString())).thenReturn(null);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(new ZScalerQuota(99, 1));

    ApiZScalerQuotaDTO response = underTest.getQuota();

    verify(client).getZScalerQuota(config.getHostname());
    assertThat(response).isNotNull();
    assertThat(response.totalAllowedUrls()).isEqualTo(100);
    assertThat(response.remainingUrls()).isEqualTo(1);
    assertThat(response.status()).isEqualTo("under");
  }

  @Test
  public void testUpdateCategory_whenQuotaIsCached() {
    when(configurationDAO.get()).thenReturn(config);
    when(cache.getIfPresent(anyString())).thenReturn(new ZScalerQuota(200, 2));

    ApiZScalerQuotaDTO response = underTest.getQuota();

    verify(client, never()).getZScalerQuota(config.getHostname());
    assertThat(response).isNotNull();
    assertThat(response.totalAllowedUrls()).isEqualTo(202);
    assertThat(response.remainingUrls()).isEqualTo(2);
    assertThat(response.status()).isEqualTo("under");
  }

  @Test
  public void testGetQuota_overStatus() {
    when(configurationDAO.get()).thenReturn(config);
    when(cache.getIfPresent(anyString())).thenReturn(new ZScalerQuota(100, 0));

    ApiZScalerQuotaDTO response = underTest.getQuota();

    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo("over");
  }

  @Test
  public void testGetQuota_underStatus() {
    when(configurationDAO.get()).thenReturn(config);
    when(cache.getIfPresent(anyString())).thenReturn(new ZScalerQuota(99, 1));

    ApiZScalerQuotaDTO response = underTest.getQuota();

    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo("under");
  }

  @Test
  public void getConfiguredFormatsReturnsEmptyListWhenFormatsAreNull() {
    zscalerFormats = new ArrayList<>();
    zscalerFormats.add(new ZscalerFormat("maven", false));
    zscalerFormats.add(new ZscalerFormat("npm", false));
    zscalerFormats.add(new ZscalerFormat("pypi",false));
    zscalerFormats.add(new ZscalerFormat("nuget",false));
    when(zscalerFormatDAO.getAll()).thenReturn(zscalerFormats);
    when(configurationDAO.get()).thenReturn(config);

    List<ZScalerSupportedFormat> result = underTest.getConfiguredFormats();

    assertThat(result).isEmpty();
  }

  @Test
  public void getConfiguredFormatsReturnsSpecifiedFormat() {
    zscalerFormats = new ArrayList<>();
    zscalerFormats.add(new ZscalerFormat("maven", false));
    zscalerFormats.add(new ZscalerFormat("npm", true));
    zscalerFormats.add(new ZscalerFormat("pypi",true));
    zscalerFormats.add(new ZscalerFormat("nuget",false));
    when(zscalerFormatDAO.getAll()).thenReturn(zscalerFormats);
    when(configurationDAO.get()).thenReturn(config);

    List<ZScalerSupportedFormat> result = underTest.getConfiguredFormats();

    assertThat(result).containsExactly(ZScalerSupportedFormat.NPM, ZScalerSupportedFormat.PYPI);
  }

  @Test
  public void getConfiguredFormatsThrowsExceptionWhenConfigurationIsNull() {
    when(configurationDAO.get()).thenReturn(null);

    assertThrows("No zScaler configuration found", BadRequestException.class,
        () -> underTest.getConfiguredFormats());

    assertThat(logOutput).atWarnLevel().contains("No zScaler configuration found");
  }
}
