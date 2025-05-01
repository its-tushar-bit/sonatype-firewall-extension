/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.InputStream;
import java.time.Duration;
import javax.inject.Inject;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ZSCALER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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

  @Inject
  private ZScalerUpdater underTest;

  @Override
  public void configure(Binder binder) {
    binder.bind(ZScalerMaliciousUrlFetcher.class).toInstance(mockZScalerMaliciousUrlFetcher);
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(ApiZScalerService.class).toInstance(mockApiZScalerService);
    binder.bind(ProductLicense.class).toInstance(mockProductLicense);
    binder.bind(Configuration.class).toInstance(mockConfiguration);
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

    verify(spyUnderTest).updateAllzScalerMaliciousUrls();
  }

  @Test
  public void testExecute_error() throws JobExecutionException {
    ZScalerUpdater spyUnderTest = spy(underTest);

    doThrow(new RuntimeException("test error")).when(spyUnderTest).updateAllzScalerMaliciousUrls();

    spyUnderTest.execute(mockJobExecutionContext);

    verify(spyUnderTest).updateAllzScalerMaliciousUrls();
    assertThat(logOutput).atErrorLevel().contains("Error fetching zScaler malicious URLs");
  }

  @Test
  public void testUpdateAllzScalerMaliciousUrls_WithValidLicense() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);

    underTest.updateAllzScalerMaliciousUrls();

    verify(mockApiZScalerService).updateCategories(eq(ZScalerFormat.MAVEN2), any());
    verify(mockApiZScalerService).updateCategories(eq(ZScalerFormat.NPM), any());
    verify(mockApiZScalerService).updateCategories(eq(ZScalerFormat.PYPI), any());
  }

  @Test
  public void testUpdateAllzScalerMaliciousUrls_WithInvalidLicense() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(false);

    underTest.updateAllzScalerMaliciousUrls();

    verify(mockApiZScalerService, never()).updateCategories(any(), any());
  }

  @Test
  public void testUpdate() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);

    underTest.updateInternal(ZScalerFormat.NPM);

    ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(mockApiZScalerService).updateCategories(eq(ZScalerFormat.NPM), inputStreamCaptor.capture());

    assertNotNull(inputStreamCaptor.getValue());
  }

  @Test(expected = InvalidLicenseException.class)
  public void testUpdate_invalidLicense() {
    when(mockProductLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(false);

    underTest.updateInternal(ZScalerFormat.NPM);
  }
}
