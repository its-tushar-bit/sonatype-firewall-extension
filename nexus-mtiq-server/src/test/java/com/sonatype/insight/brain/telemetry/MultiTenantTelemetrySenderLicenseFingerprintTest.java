/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.MultiTenantProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicensingModel;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.telemetry.TelemetryReceiptService.TelemetryReceipt;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonatype.licensing.product.ProductLicenseKey;

@RunWith(MockitoJUnitRunner.Silent.class)
@Category(SlowTest.class)
public class MultiTenantTelemetrySenderLicenseFingerprintTest
    extends AbstractMultiTenantTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Mock
  private InsightProxy proxy;

  @Mock
  private Configuration configuration;

  @Mock
  private TelemetryReceiptService mockTelemetryReceiptService;

  @Mock
  private VersionService versionService;

  @Mock
  private TelemetryId telemetryId;

  @Mock
  private TenantUtil tenantUtil;

  private TestProductLicenseManager testProductLicenseManager;

  private ProductLicense productLicense;

  private TelemetrySender telemetrySender;

  @Before
  public void before() throws Exception {
    hdsMockServer.reset();

    // set the server URL into the HdsClient
    doAnswer(invocation -> {
      HttpClientUtils.Configuration config = invocation.getArgument(0);
      config.setServerUrl(hdsMockServer.getHttpUrl());
      return null;
    }).when(proxy).contextualize(any());

    // Enable multi-tenant mode so TenantAwareOneTimeRunnable properly sets tenant context
    doReturn(true).when(tenantUtil).isMultiTenant();

    testProductLicenseManager = new TestProductLicenseManager();
    productLicense = new MultiTenantProductLicense(mock(DeveloperEnablementService.class));
    HdsClient hdsClient = new HdsClient(proxy, productLicense, configuration, versionService, telemetryId, null, null);
    telemetrySender =
        new TelemetrySender(hdsClient, versionService, telemetryId, tenantUtil, mockTelemetryReceiptService);

    telemetrySender.start();
  }

  @After
  public void after() {
    telemetrySender.stop();
  }

  @Test
  public void testFingerprintUsedInHdsClientIsCorrectForATenant() throws InterruptedException {
    testTenant("foo");
    testTenant("bar");
    testTenant("baz");
  }

  private void testTenant(final String tenantName) throws InterruptedException {
    // special test fingerprint we will assert later
    String testFingerprint = tenantName + "-fingerprint";

    TenantTestHelper.testAsNewTenant(tenantName, tenant -> {
      ProductLicenseKey productLicenseKey =
          testProductLicenseManager.getLicenseDetails(new ByteArrayInputStream(new byte[1]));
      productLicense.set(productLicenseKey, testFingerprint,
          new HashSet<>(
              Arrays.asList(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION, ProductLicenseDetails.PRODUCT_FIREWALL,
                  ProductLicenseDetails.PRODUCT_FIREWALL_FOR_ARTIFACTORY,
                  ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD)),
          EnumSet.allOf(LicensedFeature.class), new HashSet<>(StageTypes.getAll()),
          Collections.singleton(ProductLicensingModel.LEGACY),
          100, 50, 45, 50);

      // send some fake telemetry
      TelemetryData telemetryDataSend = new TelemetryData(TelemetryPurpose.DATABASE);
      telemetryDataSend.put("test-key", "test-value");
      doReturn(new TelemetryReceipt(List.of(telemetryDataSend)))
          .when(mockTelemetryReceiptService)
          .onTelemetrySubmitted(anyList());
      telemetrySender.send(telemetryDataSend);
    });

    // wait a bit for the inner Telemetry thread to finish processing
    Thread.sleep(500);

    // assert the fingerprint matches the header used in the HdsClient call
    Map<String, String> headers = hdsMockServer.getCapturedRequestHttpHeaders(TelemetrySender.RESOURCE_PATH);
    assertThat(headers).containsEntry("X-CLM-Token", testFingerprint);

    hdsMockServer.reset();
  }

  /**
   * Credit is a self-hosted-only feature. MultiTenantProductLicense's credit methods are no-ops.
   */
  @Test
  public void testMultiTenantProductLicense_creditMethodsAreNoOps() {
    MultiTenantProductLicense mtLicense = new MultiTenantProductLicense(mock(DeveloperEnablementService.class));

    // setCreditAmount is a no-op - should not throw
    mtLicense.setCreditAmount(new java.math.BigDecimal("1000"));

    // getCreditAmount always returns null
    assertThat(mtLicense.getCreditAmount()).isNull();
  }
}
