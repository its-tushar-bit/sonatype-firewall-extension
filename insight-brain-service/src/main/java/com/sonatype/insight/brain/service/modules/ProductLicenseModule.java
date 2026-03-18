/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import java.util.List;

import com.sonatype.insight.brain.product.license.CLMFeature;
import com.sonatype.insight.brain.product.license.FirewallFeature;
import com.sonatype.insight.brain.product.license.FirewallReleaseIntegrityLicenseListener;
import com.sonatype.insight.brain.product.license.LicensedConditionTypesListener;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseScheduler;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.license.model.CLMLicenseBuilder;
import com.sonatype.insight.license.model.CLMProductDetails;
import org.sonatype.licensing.PreferencesFactory;
import org.sonatype.licensing.ProductDetails;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.product.LicenseBuilder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

/**
 * Guice module providing explicit bindings for sonatype-licensing components. This replaces Sisu's automatic @Named
 * component discovery.
 */
public class ProductLicenseModule
    extends AbstractModule
{
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void configure() {
    bind(CLMProductDetails.class);
    bind(ProductDetails.class).to(CLMProductDetails.class);

    var features = MapBinder.newMapBinder(binder(), String.class, Feature.class);
    features.addBinding(CLMFeature.ID).to(CLMFeature.class);
    features.addBinding(FirewallFeature.ID).to(FirewallFeature.class);

    // Create multibinder for ProductLicenseListener - these will be registered with CLMLicenseManager
    var listeners = Multibinder.newSetBinder(binder(), ProductLicenseListener.class);
    listeners.addBinding().to(AutomaticQuarantineReleaseScheduler.class);
    listeners.addBinding().to(QuartzJobStoreTX.class);
    listeners.addBinding().to(LicensedConditionTypesListener.class);
    listeners.addBinding().to(FirewallReleaseIntegrityLicenseListener.class);
    listeners.addBinding().to(com.sonatype.insight.brain.telemetry.PendoCache.class);
    listeners.addBinding().to(com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler.class);
    listeners.addBinding().to(com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory.class);
  }

  @Provides
  @Singleton
  public LicenseBuilder clmLicenseBuilder(
      CLMProductDetails clmProductDetails,
      PreferencesFactory preferencesFactory)
  {
    return new CLMLicenseBuilder(clmProductDetails, preferencesFactory);
  }

  /**
   * Provides a list of LicenseChangeListeners by discovering all ProductLicenseListener implementations and wrapping
   * them in adapters. This replaces Sisu's automatic discovery mechanism.
   */
  @Provides
  @Singleton
  public List<org.sonatype.licensing.product.LicenseChangeListener> provideLicenseChangeListeners(
      AutomaticQuarantineReleaseScheduler automaticQuarantineReleaseScheduler,
      QuartzJobStoreTX quartzJobStoreTX,
      LicensedConditionTypesListener licensedConditionTypesListener,
      FirewallReleaseIntegrityLicenseListener firewallReleaseIntegrityLicenseListener,
      com.sonatype.insight.brain.telemetry.PendoCache pendoCache,
      com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler policyMonitorScheduler,
      com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {

    // Create adapters that bridge from external LicenseChangeListener to internal ProductLicenseListener
    return java.util.Arrays.asList(
        new ProductLicenseListenerAdapter(automaticQuarantineReleaseScheduler),
        new ProductLicenseListenerAdapter(quartzJobStoreTX),
        new ProductLicenseListenerAdapter(licensedConditionTypesListener),
        new ProductLicenseListenerAdapter(firewallReleaseIntegrityLicenseListener),
        new ProductLicenseListenerAdapter(pendoCache),
        new ProductLicenseListenerAdapter(policyMonitorScheduler),
        new ProductLicenseListenerAdapter(policyViolationLoggerFactory));
  }

  /**
   * Adapter that bridges the external org.sonatype.licensing.product.LicenseChangeListener interface to our internal
   * ProductLicenseListener interface.
   */
  private static class ProductLicenseListenerAdapter
      implements org.sonatype.licensing.product.LicenseChangeListener
  {
    private final com.sonatype.insight.brain.product.license.ProductLicenseListener delegate;

    ProductLicenseListenerAdapter(com.sonatype.insight.brain.product.license.ProductLicenseListener delegate) {
      this.delegate = delegate;
    }

    @Override
    public void licenseChanged(org.sonatype.licensing.product.ProductLicenseKey key, boolean installed) {
      // The external licensing library notifies us when the license changes
      // We delegate to our internal listener which doesn't need the key/installed parameters
      delegate.productLicenseChanged();
    }
  }
}
