/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;
import com.sonatype.insight.test.productlicense.ProductLicenseSigner;
import jakarta.inject.Inject;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.sonatype.licensing.LicensingException;
import org.sonatype.licensing.PreferencesFactory;
import org.sonatype.licensing.product.LicenseBuilder;
import org.sonatype.licensing.product.LicenseChangeNotifier;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.internal.DefaultProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseContent;
import org.sonatype.licensing.product.util.LicenseFingerprintStrategy;
import org.sonatype.licensing.product.util.LicenseFingerprinter;
import org.sonatype.licensing.trial.TrialLicenseManager;

/**
 * Most CLMLicenseManager functionality is tested in CLMLicenseManagerTest against a mocked
 * underlying ProductLicenseManager (a TestProductLicenseManager instance). In order to test that the license
 * actually gets read from and written to the correct place, we need a separate test that does not mock
 * the underlying ProductLicenseManager
 */
public class CLMLicenseManagerDatabaseTest
    extends BrainInjectedTest
{
  private static final String UNEXPIRED_LICENSE_FINGERPRINT = "662ea4dcae50eccbd92df98bc2a10217d3264bea";

  private static final String EXPIRED_LICENSE_FINGERPRINT = "238de98fcee541b7c6b311dedfa6799de2524181";

  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Inject
  private ApiConfigurationService apiConfigurationService;

  @Inject
  private ProductLicenseSigner productLicenseSigner;

  @Inject
  private ProductLicense productLicense;

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Mock
  private TaskScheduler taskScheduler;

  @Before
  public void resetHdsServer() {
    hdsMockServer.reset();
    apiConfigurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HDS_URL,
        hdsMockServer.getHttpUrl());
    apiConfigurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);
  }

  @Before
  public void resetProductLicense() {
    productLicense.clear();
  }

  @Override
  protected List<BeanFieldOverride> getBeanFieldOverrides() {
    TrialLicenseManager trialLicenseManager = getApplicationContext().getBean(TrialLicenseManager.class);
    LicenseBuilder licenseBuilder = getApplicationContext().getBean(LicenseBuilder.class);
    LicenseChangeNotifier licenseChangeNotifier = getApplicationContext().getBean(LicenseChangeNotifier.class);
    PreferencesFactory preferencesFactory = getApplicationContext().getBean(PreferencesFactory.class);
    LicenseFingerprintStrategy licenseFingerprintStrategy =
        getApplicationContext().getBean(LicenseFingerprintStrategy.class);
    ProductLicenseDAO productLicenseDAO = getApplicationContext().getBean(ProductLicenseDAO.class);

    ProductLicenseManager realProductLicenseManager =
        new DefaultProductLicenseManager(trialLicenseManager, licenseBuilder, licenseChangeNotifier);
    LicenseContent realLicenseContent =
        new LicenseContent(licenseBuilder, realProductLicenseManager, preferencesFactory);
    LicenseFingerprinter realLicenseFingerprinter =
        new LicenseFingerprinter(licenseFingerprintStrategy, realLicenseContent);
    ProductLicenseDetailsCache realProductLicenseDetailsCache = new ProductLicenseDetailsCache(productLicenseDAO);

    return List.of(
        beanFieldOverride(CLMLicenseManager.class, "licenseManager", realProductLicenseManager),
        beanFieldOverride(CLMLicenseManager.class, "licenseContent", realLicenseContent),
        beanFieldOverride(CLMLicenseManager.class, "licenseFingerprinter", realLicenseFingerprinter),
        beanFieldOverride(CLMLicenseManager.class, "productLicenseDetailsCache", realProductLicenseDetailsCache));
  }

  private URL getTestLicenseUrl(String fingerprint) {
    return getClass().getResource("/productlicense/" + fingerprint + ".lic");
  }

  private void setupInstalledLicense(String fingerprint) throws Exception {
    tempEntity.setProductLicense(Paths.get(getTestLicenseUrl(fingerprint).toURI()));
    mockHdsProductLicenseDetails(fingerprint);
  }

  private void mockHdsProductLicenseDetails(String fingerprint) {
    SignedProductLicenseDetailsDTO licenseDetails = new SignedProductLicenseDetailsDTO();
    licenseDetails.version = 1;
    licenseDetails.features = new TreeSet<>();
    licenseDetails.stageIds = new TreeSet<>();
    licenseDetails.maxApplications = 100;
    if (licenseDetails.signature == null) {
      productLicenseSigner.sign(licenseDetails, fingerprint);
    }
    hdsMockServer.respondWith(licenseDetails).atUri("/rest/productLicense/v1").withoutLicense();
  }

  @Test
  public void testLoadLicense_noLicenseInstalled() {
    clmLicenseManager.loadLicense();
    assertThat(productLicense.isValid()).isFalse();
  }

  @Test
  public void testLoadLicense_validLicenseInDatabase() throws Exception {
    String fingerprint = UNEXPIRED_LICENSE_FINGERPRINT;
    setupInstalledLicense(fingerprint);
    assertThat(productLicense.isValid()).isFalse();

    clmLicenseManager.loadLicense();
    assertThat(productLicense.isValid()).isTrue();
    assertThat(productLicense.getFingerprint()).isEqualTo(fingerprint);
    assertThat(productLicense.getExpirationTimestamp())
        .isEqualTo(Instant.parse("2051-01-01T00:00:00Z").toEpochMilli());
  }

  @Test
  public void testLoadLicense_expiredLicenseInDatabase() throws Exception {
    String fingerprint = EXPIRED_LICENSE_FINGERPRINT;
    setupInstalledLicense(fingerprint);
    assertThat(productLicense.isValid()).isFalse();

    clmLicenseManager.loadLicense();
    assertThat(productLicense.isValid()).isFalse();
    assertThat(productLicense.getFingerprint()).isEqualTo(null);
    assertThat(productLicense.getExpirationTimestamp()).isEqualTo(0);
  }

  @Test
  public void testInstallLicense_validLicense() throws Exception {
    String fingerprint = UNEXPIRED_LICENSE_FINGERPRINT;
    mockHdsProductLicenseDetails(fingerprint);

    assertThat(productLicense.isValid()).isFalse();

    clmLicenseManager.installLicense(getTestLicenseUrl(fingerprint).openStream());

    assertThat(productLicense.isValid()).isTrue();
    assertThat(productLicense.getFingerprint()).isEqualTo(fingerprint);
    assertThat(productLicense.getExpirationTimestamp())
        .isEqualTo(Instant.parse("2051-01-01T00:00:00Z").toEpochMilli());
  }

  @Test
  public void testInstallLicense_expiredLicense() throws Exception {
    String fingerprint = EXPIRED_LICENSE_FINGERPRINT;
    mockHdsProductLicenseDetails(fingerprint);

    assertThatThrownBy(
        () -> clmLicenseManager.installLicense(getTestLicenseUrl(fingerprint).openStream()))
            .isInstanceOf(LicensingException.class);

    assertThat(productLicense.isValid()).isFalse();
    assertThat(productLicense.getFingerprint()).isEqualTo(null);
    assertThat(productLicense.getExpirationTimestamp()).isEqualTo(0);
  }
}
