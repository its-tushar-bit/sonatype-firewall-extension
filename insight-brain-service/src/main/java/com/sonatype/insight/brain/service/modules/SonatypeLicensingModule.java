/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import java.util.List;
import java.util.Map;
import jakarta.annotation.Nullable;

import com.sonatype.insight.brain.product.license.DatabasePreferencesFactory;
import org.sonatype.licensing.LicenseKeyRequest;
import org.sonatype.licensing.PreferencesFactory;
import org.sonatype.licensing.ProductDetails;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.feature.FeatureValidator;
import org.sonatype.licensing.internal.DefaultFeatureValidator;
import org.sonatype.licensing.product.LicenseBuilder;
import org.sonatype.licensing.product.LicenseChangeListener;
import org.sonatype.licensing.product.LicenseChangeNotifier;
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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Bind classes from the inner source sonatype-licensing library that SCA doesn't directly control.
 */
public class SonatypeLicensingModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(PreferencesFactory.class).to(DatabasePreferencesFactory.class);
  }

  @Provides
  @Singleton
  public LicenseChangeNotifier licenseChangeNotifier(
      final List<LicenseChangeListener> listeners,
      final DefaultLicenseFeatureVerifier licenseFeatureVerifier,
      final LicenseFingerprintStrategy licenseFingerprintStrategy)
  {
    return new LicenseChangeNotifierImpl(listeners, licenseFeatureVerifier, licenseFingerprintStrategy);
  }

  @Provides
  @Singleton
  public FeatureValidator licenseFeatureValidator() {
    return new DefaultFeatureValidator();
  }

  @Provides
  @Singleton
  public DefaultLicenseFeatureVerifier licenseFeatureVerifier(final FeatureValidator licenseFeatureValidator) {
    return new DefaultLicenseFeatureVerifier(licenseFeatureValidator);
  }

  @Provides
  @Singleton
  public LicenseKeyRequest licenseKeyRequest(final ProductDetails nexusProductDetails) {
    return new DefaultLicenseKeyRequest(nexusProductDetails);
  }

  @Provides
  @Singleton
  public ProductLicenseManager productLicenseManager(
      final TrialLicenseManager trialLicenseManager,
      final LicenseBuilder licenseBuilder,
      final LicenseChangeNotifier licenseChangeNotifier)
  {
    return new DefaultProductLicenseManager(trialLicenseManager, licenseBuilder, licenseChangeNotifier);
  }

  @Provides
  @Singleton
  public TrialLicenseManager trialLicenseManager(
      final Map<String, Feature> features,
      final FeatureValidator featureValidator)
  {
    return new DefaultTrialLicenseManager(() -> new DefaultLicenseKey(features), featureValidator);
  }

  @Provides
  @Singleton
  public LicenseContent licenseContent(
      final LicenseBuilder licenseBuilder,
      final ProductLicenseManager productLicenseManager,
      final PreferencesFactory preferencesFactory)
  {
    return new LicenseContent(licenseBuilder, productLicenseManager, preferencesFactory);
  }

  @Provides
  @Singleton
  public LicenseFingerprintStrategy licenseFingerprintStrategy() {
    return new LicenseFingerprintStrategyImpl();
  }

  @Provides
  @Singleton
  public LicenseFingerprinter licenseFingerprinter(
      final LicenseFingerprintStrategy licenseFingerprintStrategy,
      @Nullable final LicenseContent licenseContent)
  {
    return new LicenseFingerprinter(licenseFingerprintStrategy, licenseContent);
  }
}
