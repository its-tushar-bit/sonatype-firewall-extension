/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.CLMFeature;
import com.sonatype.insight.brain.product.license.FirewallFeature;
import com.sonatype.insight.brain.product.license.FirewallReleaseIntegrityLicenseListener;
import com.sonatype.insight.brain.product.license.LicensedConditionTypesListener;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseScheduler;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.telemetry.PendoCache;
import com.sonatype.insight.license.model.CLMLicenseBuilder;
import com.sonatype.insight.license.model.CLMProductDetails;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonatype.licensing.LicenseKeyRequest;
import org.sonatype.licensing.PreferencesFactory;
import org.sonatype.licensing.ProductDetails;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.feature.FeatureValidator;
import org.sonatype.licensing.internal.DefaultFeatureValidator;
import org.sonatype.licensing.product.LicenseBuilder;
import org.sonatype.licensing.product.LicenseChangeListener;
import org.sonatype.licensing.product.LicenseChangeNotifier;
import org.sonatype.licensing.product.ProductLicenseKey;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.internal.DefaultLicenseFeatureVerifier;
import org.sonatype.licensing.product.internal.DefaultLicenseKey;
import org.sonatype.licensing.product.internal.DefaultLicenseKeyRequest;
import org.sonatype.licensing.product.internal.DefaultProductLicenseManager;
import org.sonatype.licensing.product.internal.LicenseChangeNotifierImpl;
import org.sonatype.licensing.product.util.LicenseContent;
import org.sonatype.licensing.product.util.LicenseFingerprintStrategy;
import org.sonatype.licensing.product.util.LicenseFingerprintStrategyImpl;
import org.sonatype.licensing.product.util.LicenseFingerprinter;
import org.sonatype.licensing.trial.TrialLicenseManager;
import org.sonatype.licensing.trial.internal.DefaultTrialLicenseManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for licensing beans.
 * <p>
 * This configuration provides the sonatype-licensing beans used by the Spring application.
 */
@Configuration
public class LicensingConfiguration
{

  // ==== ProductLicenseModule beans ====

  @Bean
  public CLMProductDetails clmProductDetails() {
    return new CLMProductDetails();
  }

  @Bean
  @Primary
  public ProductDetails productDetails(CLMProductDetails clmProductDetails) {
    return clmProductDetails;
  }

  @Bean
  public Map<String, Feature> featureMap(CLMFeature clmFeature, FirewallFeature firewallFeature) {
    Map<String, Feature> map = new HashMap<>();
    map.put(CLMFeature.ID, clmFeature);
    map.put(FirewallFeature.ID, firewallFeature);
    return map;
  }

  @Bean
  public LicenseBuilder clmLicenseBuilder(
      CLMProductDetails clmProductDetails,
      PreferencesFactory preferencesFactory)
  {
    return new CLMLicenseBuilder(clmProductDetails, preferencesFactory);
  }

  @Bean
  public List<LicenseChangeListener> licenseChangeListeners(
      AutomaticQuarantineReleaseScheduler automaticQuarantineReleaseScheduler,
      QuartzJobStoreTX quartzJobStoreTX,
      LicensedConditionTypesListener licensedConditionTypesListener,
      FirewallReleaseIntegrityLicenseListener firewallReleaseIntegrityLicenseListener,
      PendoCache pendoCache,
      PolicyMonitorScheduler policyMonitorScheduler,
      PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    return Arrays.asList(
        new ProductLicenseListenerAdapter(automaticQuarantineReleaseScheduler),
        new ProductLicenseListenerAdapter(quartzJobStoreTX),
        new ProductLicenseListenerAdapter(licensedConditionTypesListener),
        new ProductLicenseListenerAdapter(firewallReleaseIntegrityLicenseListener),
        new ProductLicenseListenerAdapter(pendoCache),
        new ProductLicenseListenerAdapter(policyMonitorScheduler),
        new ProductLicenseListenerAdapter(policyViolationLoggerFactory));
  }

  // ==== SonatypeLicensingModule beans ====
  // Note: DatabasePreferencesFactory already implements PreferencesFactory and is @Singleton,
  // so it can be injected directly as PreferencesFactory without a separate bean

  @Bean
  public LicenseChangeNotifier licenseChangeNotifier(
      List<LicenseChangeListener> listeners,
      DefaultLicenseFeatureVerifier licenseFeatureVerifier,
      LicenseFingerprintStrategy licenseFingerprintStrategy)
  {
    return new LicenseChangeNotifierImpl(listeners, licenseFeatureVerifier, licenseFingerprintStrategy);
  }

  @Bean
  public FeatureValidator licenseFeatureValidator() {
    return new DefaultFeatureValidator();
  }

  @Bean
  public DefaultLicenseFeatureVerifier licenseFeatureVerifier(FeatureValidator licenseFeatureValidator) {
    return new DefaultLicenseFeatureVerifier(licenseFeatureValidator);
  }

  @Bean
  public LicenseKeyRequest licenseKeyRequest(ProductDetails nexusProductDetails) {
    return new DefaultLicenseKeyRequest(nexusProductDetails);
  }

  @Bean
  public ProductLicenseManager productLicenseManager(
      TrialLicenseManager trialLicenseManager,
      LicenseBuilder licenseBuilder,
      LicenseChangeNotifier licenseChangeNotifier)
  {
    return new DefaultProductLicenseManager(trialLicenseManager, licenseBuilder, licenseChangeNotifier);
  }

  @Bean
  public TrialLicenseManager trialLicenseManager(
      Map<String, Feature> features,
      FeatureValidator featureValidator)
  {
    return new DefaultTrialLicenseManager(() -> new DefaultLicenseKey(features), featureValidator);
  }

  @Bean
  public LicenseContent licenseContent(
      LicenseBuilder licenseBuilder,
      ProductLicenseManager productLicenseManager,
      PreferencesFactory preferencesFactory)
  {
    return new LicenseContent(licenseBuilder, productLicenseManager, preferencesFactory);
  }

  @Bean
  public LicenseFingerprintStrategy licenseFingerprintStrategy() {
    return new LicenseFingerprintStrategyImpl();
  }

  @Bean
  public LicenseFingerprinter licenseFingerprinter(
      LicenseFingerprintStrategy licenseFingerprintStrategy,
      LicenseContent licenseContent)
  {
    return new LicenseFingerprinter(licenseFingerprintStrategy, licenseContent);
  }

  /**
   * Adapter that bridges the external org.sonatype.licensing.product.LicenseChangeListener interface to our internal
   * ProductLicenseListener interface.
   */
  private static class ProductLicenseListenerAdapter
      implements LicenseChangeListener
  {
    private final ProductLicenseListener delegate;

    ProductLicenseListenerAdapter(ProductLicenseListener delegate) {
      this.delegate = delegate;
    }

    @Override
    public void licenseChanged(ProductLicenseKey key, boolean installed) {
      // The external licensing library notifies us when the license changes
      // We delegate to our internal listener which doesn't need the key/installed parameters
      delegate.productLicenseChanged();
    }
  }
}
