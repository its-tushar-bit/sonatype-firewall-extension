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

  @Mock
  private ZScalerPermissionValidator permissionValidator;

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

  @Mock
  private com.sonatype.insight.brain.service.Configuration configuration;

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

    // Default: return null so the service uses the default value (25000)
    when(configuration.getZScalerMaxUrlsPerCategory()).thenReturn(null);

    ZScalerCategory mockCategory = new ZScalerCategory();
    mockCategory.setId("mock-category-id");
    when(client.createCustomUrlCategory(anyString(), anyString(), anyList()))
        .thenReturn(ZScalerOperationResult.success(200, mockCategory));
    when(client.updateCustomUrlCategories(anyString(), anyString(), anyString(), anyList()))
        .thenReturn(ZScalerOperationResult.success(200));

    underTest = Mockito.spy(new ApiZScalerService(configurationDAO, zscalerFormatDAO,
        metricsDAO, passwordHandler, client, permissionValidator, configuration, cache));
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
    existingCategory.setConfiguredName("sonatype-npm-0-shadow-download-defense");
    existingCategory.setCustomCategory(true);
    existingCategory.setCustomUrlsCount(1);
    existingCategory.setUrls(List.of("https://npmjs.org/npm/"));

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(100, 200);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(List.of(existingCategory));

    underTest.updateCategory(ZScalerSupportedFormat.NPM, urls);

    verify(client).getCustomUrlCategories(config.getHostname());
    verify(client).updateCustomUrlCategories(eq(config.getHostname()), eq("sonatype-npm-0-shadow-download-defense"),
        eq("npm-category"), eq(List.of(url)));
    verify(client, never()).createCustomUrlCategory(anyString(), anyString(), anyList());
    assertThat(logOutput).atInfoLevel()
        .contains("Category sonatype-npm-0-shadow-download-defense with id npm-category already exists, " +
            "updating with 1 URLs");
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
    verify(client).createCustomUrlCategory(eq(config.getHostname()), eq("sonatype-npm-0-shadow-download-defense"),
        anyList());
    assertThat(logOutput).atInfoLevel()
        .contains("Category sonatype-npm-0-shadow-download-defense does not exist, creating with 1 URLs");
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_whenQuotaWouldBeExceeded() throws Exception {
    ZScalerCategory existingCategory = new ZScalerCategory();
    existingCategory.setId("npm-category");
    existingCategory.setConfiguredName("sonatype-npm-0-shadow-download-defense");
    existingCategory.setCustomCategory(true);
    existingCategory.setCustomUrlsCount(1);
    existingCategory.setUrls(List.of("https://npmjs.org/npm/"));

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(100, 0);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(List.of(existingCategory));

    underTest.updateCategory(ZScalerSupportedFormat.NPM,
        List.of("http://example-url1", "http://example-url2", "http://example-url3"));

    verify(client).updateCustomUrlCategories(eq(config.getHostname()), eq("sonatype-npm-0-shadow-download-defense"),
        eq(existingCategory.getId()), eq(List.of("http://example-url1")));
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_whenQuotaWouldNotBeExceeded() throws Exception {
    ZScalerCategory existingCategory = new ZScalerCategory();
    existingCategory.setId("npm-category");
    existingCategory.setConfiguredName("sonatype-npm-0-shadow-download-defense");
    existingCategory.setCustomCategory(true);
    existingCategory.setCustomUrlsCount(1);
    existingCategory.setUrls(List.of("https://npmjs.org/npm/"));

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(80, 20);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(List.of(existingCategory));

    underTest.updateCategory(ZScalerSupportedFormat.NPM,
        List.of("http://example-url1", "http://example-url2", "http://example-url3"));

    verify(client).updateCustomUrlCategories(eq(config.getHostname()), eq("sonatype-npm-0-shadow-download-defense"),
        eq(existingCategory.getId()), eq(List.of("http://example-url1", "http://example-url2", "http://example-url3")));
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_whenQuotaExceededMax() throws Exception {
    ZScalerCategory existingCategory = new ZScalerCategory();
    existingCategory.setId("npm-category");
    existingCategory.setConfiguredName("sonatype-npm-0-shadow-download-defense");
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
    zscalerFormats.add(new ZscalerFormat("pypi", false));
    zscalerFormats.add(new ZscalerFormat("nuget", false));
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
    zscalerFormats.add(new ZscalerFormat("pypi", true));
    zscalerFormats.add(new ZscalerFormat("nuget", false));
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

  @Test
  public void testUpdateCategory_multipleCategories_whenUrlsExceedLimit() {
    // Create a list of 50001 URLs (more than 25000 per category)
    List<String> manyUrls = new ArrayList<>();
    for (int i = 0; i < 50001; i++) {
      manyUrls.add("http://example-url-" + i);
    }

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(0, 100000);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(Collections.emptyList());

    underTest.updateCategory(ZScalerSupportedFormat.MAVEN, manyUrls);

    verify(client).getCustomUrlCategories(config.getHostname());
    // Should create 3 categories: 2 with 25000 each, 1 with 1 URL
    verify(client).createCustomUrlCategory(eq(config.getHostname()), eq("sonatype-maven-0-shadow-download-defense"),
        anyList());
    verify(client).createCustomUrlCategory(eq(config.getHostname()), eq("sonatype-maven-1-shadow-download-defense"),
        anyList());
    verify(client).createCustomUrlCategory(eq(config.getHostname()), eq("sonatype-maven-2-shadow-download-defense"),
        anyList());
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_cleanupUnusedCategories() {
    // Start with 3 categories
    ZScalerCategory category0 = new ZScalerCategory();
    category0.setId("maven-category-0");
    category0.setConfiguredName("sonatype-maven-0-shadow-download-defense");
    category0.setCustomCategory(true);
    category0.setCustomUrlsCount(25000);

    ZScalerCategory category1 = new ZScalerCategory();
    category1.setId("maven-category-1");
    category1.setConfiguredName("sonatype-maven-1-shadow-download-defense");
    category1.setCustomCategory(true);
    category1.setCustomUrlsCount(25000);

    ZScalerCategory category2 = new ZScalerCategory();
    category2.setId("maven-category-2");
    category2.setConfiguredName("sonatype-maven-2-shadow-download-defense");
    category2.setCustomCategory(true);
    category2.setCustomUrlsCount(5000);

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(55000, 100000);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(List.of(category0, category1, category2));

    // Now update with only 10000 URLs (should only need 1 category)
    List<String> urls = new ArrayList<>();
    for (int i = 0; i < 10000; i++) {
      urls.add("http://example-url-" + i);
    }

    underTest.updateCategory(ZScalerSupportedFormat.MAVEN, urls);

    // Should update category 0
    verify(client).updateCustomUrlCategories(eq(config.getHostname()), eq("sonatype-maven-0-shadow-download-defense"),
        eq("maven-category-0"), anyList());
    // Should delete categories 1 and 2
    verify(client).deleteCustomUrlCategory(config.getHostname(), "maven-category-1");
    verify(client).deleteCustomUrlCategory(config.getHostname(), "maven-category-2");
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_migratesLegacyCategory() {
    // Create a legacy category (without index)
    ZScalerCategory legacyCategory = new ZScalerCategory();
    legacyCategory.setId("npm-legacy-category");
    legacyCategory.setConfiguredName("sonatype-npm-shadow-download-defense");
    legacyCategory.setCustomCategory(true);
    legacyCategory.setCustomUrlsCount(5000);

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(0, 100000);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(List.of(legacyCategory));

    underTest.updateCategory(ZScalerSupportedFormat.NPM, urls);

    // Should delete the legacy category first
    verify(client).deleteCustomUrlCategory(config.getHostname(), "npm-legacy-category");
    // Then create a new indexed category
    verify(client).createCustomUrlCategory(eq(config.getHostname()), eq("sonatype-npm-0-shadow-download-defense"),
        anyList());
    verify(metricsDAO).set(any());
    assertThat(logOutput).atInfoLevel()
        .contains("Deleting legacy category sonatype-npm-shadow-download-defense with id npm-legacy-category");
  }

  @Test
  public void testUpdateCategory_handlesMixedLegacyAndIndexedCategories() {
    // Create both legacy and indexed categories
    ZScalerCategory legacyCategory = new ZScalerCategory();
    legacyCategory.setId("maven-legacy-category");
    legacyCategory.setConfiguredName("sonatype-maven-shadow-download-defense");
    legacyCategory.setCustomCategory(true);
    legacyCategory.setCustomUrlsCount(10000);

    ZScalerCategory indexedCategory = new ZScalerCategory();
    indexedCategory.setId("maven-category-0");
    indexedCategory.setConfiguredName("sonatype-maven-0-shadow-download-defense");
    indexedCategory.setCustomCategory(true);
    indexedCategory.setCustomUrlsCount(5000);

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(15000, 100000);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(List.of(legacyCategory, indexedCategory));

    // Create a list with 10000 URLs
    List<String> manyUrls = new ArrayList<>();
    for (int i = 0; i < 10000; i++) {
      manyUrls.add("http://example-url-" + i);
    }

    underTest.updateCategory(ZScalerSupportedFormat.MAVEN, manyUrls);

    // Should delete the legacy category
    verify(client).deleteCustomUrlCategory(config.getHostname(), "maven-legacy-category");
    // Should update the indexed category (only considers indexed categories for URL count)
    verify(client).updateCustomUrlCategories(eq(config.getHostname()), eq("sonatype-maven-0-shadow-download-defense"),
        eq("maven-category-0"), anyList());
    verify(metricsDAO).set(any());
  }

  @Test
  public void testUpdateCategory_withCustomMaxUrlsPerCategory() {
    // Configure custom max URLs per category
    when(configuration.getZScalerMaxUrlsPerCategory()).thenReturn(10000);

    // Create a list of 25000 URLs (would be 1 category with default 25000, but 3 with custom 10000)
    List<String> manyUrls = new ArrayList<>();
    for (int i = 0; i < 25000; i++) {
      manyUrls.add("http://example-url-" + i);
    }

    when(configurationDAO.get()).thenReturn(config);
    ZScalerQuota quota = new ZScalerQuota(0, 100000);
    when(client.getZScalerQuota(config.getHostname())).thenReturn(quota);
    when(client.getCustomUrlCategories(config.getHostname())).thenReturn(Collections.emptyList());

    underTest.updateCategory(ZScalerSupportedFormat.MAVEN, manyUrls);

    verify(client).getCustomUrlCategories(config.getHostname());
    // Should create 3 categories with custom limit: 10000, 10000, 5000
    verify(client).createCustomUrlCategory(eq(config.getHostname()), eq("sonatype-maven-0-shadow-download-defense"),
        anyList());
    verify(client).createCustomUrlCategory(eq(config.getHostname()), eq("sonatype-maven-1-shadow-download-defense"),
        anyList());
    verify(client).createCustomUrlCategory(eq(config.getHostname()), eq("sonatype-maven-2-shadow-download-defense"),
        anyList());
    verify(metricsDAO).set(any());
  }
}
