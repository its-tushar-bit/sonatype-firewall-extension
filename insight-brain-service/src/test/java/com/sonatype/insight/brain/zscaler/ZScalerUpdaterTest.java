/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ZSCALER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ZScalerUpdaterTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(ZScalerUpdater.class);

  @Mock
  private ZScalerMaliciousUrlFetcher mockZScalerMaliciousUrlFetcher;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private ApiZScalerService mockApiZScalerService;

  @Mock
  private ProductLicense mockProductLicense;

  @Mock
  private Configuration mockConfiguration;

  @Mock
  private JobExecutionContext mockJobExecutionContext;

  @Mock
  private ZScalerClient mockZScalerClient;

  @Mock
  private HdsClient mockHdsClient;

  @Inject
  private ZScalerUpdater underTest;

  @Override
  public void configure(Binder binder) {
    binder.bind(ZScalerMaliciousUrlFetcher.class)
        .toInstance(mockZScalerMaliciousUrlFetcher);
    binder.bind(ZScalerClient.class).toInstance(mockZScalerClient);
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(ApiZScalerService.class).toInstance(mockApiZScalerService);
    binder.bind(ProductLicense.class).toInstance(mockProductLicense);
    binder.bind(Configuration.class).toInstance(mockConfiguration);
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Before
  @Override
  public void beforeTest() {
    super.beforeTest();
    tempEntity.newSystemConfigurationProperty(ZSCALER, "true");
  }

  @After
  @Override
  public void afterTest() {
    tempEntity.deleteSystemConfigurationProperty(ZSCALER);
  }

  @Test
  public void testRegister() {
    when(mockConfiguration.getZScalerUpdateTaskPeriod()).thenReturn(2);

    underTest.register();

    verify(mockTaskScheduler).schedulePeriodicTask(underTest, Duration.ofHours(2));
  }

  @Test
  public void testRegister_taskNotScheduled() {
    when(mockConfiguration.getZScalerUpdateTaskPeriod()).thenReturn(0);

    underTest.register();

    verify(mockTaskScheduler, never()).schedulePeriodicTask(any(), any());
  }

  @Test
  public void testExecute() throws JobExecutionException {
    ZScalerUpdater spyUnderTest = spy(underTest);

    spyUnderTest.execute(mockJobExecutionContext);

    verify(spyUnderTest).updateAllZScalerMaliciousCategoriesInternal();
  }

  @Test
  public void testExecute_error() throws JobExecutionException {
    ZScalerUpdater spyUnderTest = spy(underTest);

    doThrow(new RuntimeException("test error")).when(spyUnderTest).updateAllZScalerMaliciousCategoriesInternal();

    spyUnderTest.execute(mockJobExecutionContext);

    verify(spyUnderTest).updateAllZScalerMaliciousCategoriesInternal();
    assertThat(logOutput).atErrorLevel().contains("Error fetching zScaler malicious URLs");
  }

  @Test
  public void testUpdateAllzScalerMaliciousUrls_WithValidLicense() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);
    when(mockZScalerMaliciousUrlFetcher.fetchMaliciousUrls(any()))
        .thenReturn(new ByteArrayInputStream("{\"activeThreatUrls\": [\"randomurl.com\"]}".getBytes()));
    when(mockApiZScalerService.getConfiguredFormats()).thenReturn(
        List.of(ZScalerSupportedFormat.MAVEN, ZScalerSupportedFormat.PYPI, ZScalerSupportedFormat.NPM));

    underTest.updateAllZScalerMaliciousCategories();

    // Invocation for deleteCategory and then updateCategory
    verify(mockApiZScalerService, times(2)).updateCategory(eq(ZScalerSupportedFormat.MAVEN), any());
    verify(mockApiZScalerService, times(2)).updateCategory(eq(ZScalerSupportedFormat.NPM), any());
    verify(mockApiZScalerService, times(2)).updateCategory(eq(ZScalerSupportedFormat.PYPI), any());
    verify(mockApiZScalerService, times(1)).updateCategory(eq(ZScalerSupportedFormat.NUGET), any());
  }

  @Test
  public void testUpdateAllzScalerMaliciousUrls_updatesOnlyConfigured() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);
    when(mockZScalerMaliciousUrlFetcher.fetchMaliciousUrls(any()))
        .thenReturn(new ByteArrayInputStream("{\"activeThreatUrls\": [\"randomurl.com\"]}".getBytes()));
    when(mockApiZScalerService.getConfiguredFormats()).thenReturn(
        List.of(ZScalerSupportedFormat.PYPI, ZScalerSupportedFormat.NPM));

    underTest.updateAllZScalerMaliciousCategories();

    // Invocation for deleteCategory and then updateCategory
    verify(mockApiZScalerService, times(1)).updateCategory(eq(ZScalerSupportedFormat.MAVEN), any());
    verify(mockApiZScalerService, times(1)).updateCategory(eq(ZScalerSupportedFormat.NUGET), any());
    verify(mockApiZScalerService, times(2)).updateCategory(eq(ZScalerSupportedFormat.NPM), any());
    verify(mockApiZScalerService, times(2)).updateCategory(eq(ZScalerSupportedFormat.PYPI), any());
  }

  @Test
  public void testUpdateAllzScalerMaliciousUrls_WithInvalidLicense() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(false);

    underTest.updateAllZScalerMaliciousCategories();

    verify(mockApiZScalerService, never()).updateCategory(any(), any());
  }

  @Test
  public void testUpdate() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);
    when(mockZScalerMaliciousUrlFetcher.fetchMaliciousUrls(any()))
        .thenReturn(new ByteArrayInputStream("{\"activeThreatUrls\": [\"randomurl.com\"]}".getBytes()));

    underTest.updateCategory(ZScalerSupportedFormat.NPM);

    verify(mockApiZScalerService).updateCategory(eq(ZScalerSupportedFormat.NPM), eq(List.of("randomurl.com")));
  }

  @Test(expected = InvalidLicenseException.class)
  public void testUpdate_invalidLicense() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(false);

    underTest.updateCategory(ZScalerSupportedFormat.NPM);
  }

  @Test
  public void testUpdateCategory_handlesEmptyJson() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);
    when(mockZScalerMaliciousUrlFetcher.fetchMaliciousUrls(any()))
        .thenReturn(new ByteArrayInputStream("{}".getBytes()));

    underTest.updateCategory(ZScalerSupportedFormat.NPM);

    verify(mockApiZScalerService).updateCategory(eq(ZScalerSupportedFormat.NPM), eq(List.of()));
    verify(mockZScalerClient, never()).createCustomUrlCategory(any(), any(), any());
    verify(mockZScalerClient, never()).updateCustomUrlCategories(any(), any(), any(), any());
  }

  @Test
  public void testUpdateCategory_stripsHttpsPrefix() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);
    when(mockZScalerMaliciousUrlFetcher.fetchMaliciousUrls(any()))
        .thenReturn(new ByteArrayInputStream(
            "{\"activeThreatUrls\": [\"http://test1.com\", \"https://test2.com\"]}".getBytes()));

    underTest.updateCategory(ZScalerSupportedFormat.NPM);

    verify(mockApiZScalerService).updateCategory(eq(ZScalerSupportedFormat.NPM), eq(List.of("test1.com", "test2.com")));
  }

  @Test
  public void testDeleteAllZScalerMaliciousUrlCategories() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);

    underTest.deleteAllZScalerMaliciousUrlCategories();

    verify(mockApiZScalerService, times(1)).updateCategory(eq(ZScalerSupportedFormat.MAVEN),
        eq(List.of(
            "repo1.maven.org/maven2/org/sonatype/maven-policy-demo/1.1.0/maven-policy-demo-1.1.0.jar",
            "repo.maven.apache.org/maven2/org/sonatype/maven-policy-demo/1.1.0/maven-policy-demo-1.1.0.jar"
        )));
    verify(mockApiZScalerService, times(1)).updateCategory(eq(ZScalerSupportedFormat.NUGET),
        eq(List.of("placeholder.com/nuget")));
    verify(mockApiZScalerService, times(1)).updateCategory(eq(ZScalerSupportedFormat.NPM),
        eq(List.of("registry.npmjs.org/@sonatype/policy-demo/-/policy-demo-2.1.0.tgz")));
    verify(mockApiZScalerService, times(1)).updateCategory(eq(ZScalerSupportedFormat.PYPI),
        eq(List.of("files.pythonhosted.org/packages/a2/95/" +
            "d68eb18b5f334265097fc2872446c5dd4589bce3751035ab855bfe3e1e8a/python-policy-demo-1.1.0.tar.gz")));
  }

  @Test
  public void testDeleteAllZScalerMaliciousUrlCategories_WithInvalidLicense() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(false);

    underTest.deleteAllZScalerMaliciousUrlCategories();

    verify(mockApiZScalerService, never()).updateCategory(any(), any());
  }

  @Test
  public void testDelete() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);

    underTest.deleteZScalerMaliciousUrlCategory(ZScalerSupportedFormat.NPM);

    verify(mockApiZScalerService, times(1)).updateCategory(eq(ZScalerSupportedFormat.NPM),
        eq(List.of("registry.npmjs.org/@sonatype/policy-demo/-/policy-demo-2.1.0.tgz")));
    verify(mockApiZScalerService, never()).updateCategory(eq(ZScalerSupportedFormat.PYPI), any());
    verify(mockApiZScalerService, never()).updateCategory(eq(ZScalerSupportedFormat.MAVEN), any());
    verify(mockApiZScalerService, never()).updateCategory(eq(ZScalerSupportedFormat.NUGET), any());
  }

  @Test
  public void testDelete_invalidLicense() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(false);

    underTest.deleteZScalerMaliciousUrlCategory(ZScalerSupportedFormat.MAVEN);

    verify(mockApiZScalerService, never()).updateCategory(any(), any());
  }
}
