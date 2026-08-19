/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license.entitlement;

import java.util.Map;

import com.sonatype.insight.license.model.LicensedFeature;

/**
 * Registry of upsell metadata for tier-gated features.
 * Centralizes all marketing copy for entitlement 402 responses.
 */
public final class EntitlementUpsellRegistry
{
  private static final String CTA_URL = "https://www.sonatype.com/products/request-demo";

  private static final Map<LicensedFeature, UpsellInfo> REGISTRY = Map.ofEntries(
      Map.entry(LicensedFeature.CUSTOM_POLICIES,
          new UpsellInfo(
              "Custom policies are not available in your current plan.",
              "Upgrade to Lifecycle Enterprise to define policies tailored to your organization's risk tolerance and enforce standards more precisely.",
              "https://help.sonatype.com/en/policy-constraints.html",
              CTA_URL)),

      Map.entry(LicensedFeature.CUSTOM_APPLICATION_CATEGORIES,
          new UpsellInfo(
              "Custom application categories are not available in your current plan.",
              "Upgrade to Lifecycle Enterprise to group applications by risk and environment and apply policies where they matter most.",
              "https://help.sonatype.com/en/application-categories.html",
              CTA_URL)),

      Map.entry(LicensedFeature.CUSTOM_COMPONENT_LABELS,
          new UpsellInfo(
              "Custom component labels are not available in your current plan.",
              "Upgrade to Lifecycle Enterprise to organize components and prioritize remediation based on your workflows.",
              "https://help.sonatype.com/en/component-labels.html",
              CTA_URL)),

      Map.entry(LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS,
          new UpsellInfo(
              "Custom license threat groups are not available in your current plan.",
              "Upgrade to Lifecycle Enterprise to align license risk with your legal standards and reduce manual review effort.",
              "https://help.sonatype.com/en/license-threat-groups.html",
              CTA_URL)),

      Map.entry(LicensedFeature.AUTO_WAIVER_MANAGEMENT,
          new UpsellInfo(
              "Auto waivers are not available in your current plan.",
              "Upgrade to Lifecycle Enterprise to automatically manage known issues and reduce manual triage while maintaining control over risk.",
              "https://help.sonatype.com/en/automated-waivers.html",
              CTA_URL)),

      Map.entry(LicensedFeature.WAIVER_REQUEST_WORKFLOW,
          new UpsellInfo(
              "Waiver request workflow is not available in your current plan.",
              "Upgrade to Lifecycle Enterprise to enable teams to request waivers directly and reduce approval bottlenecks.",
              "https://help.sonatype.com/en/requested-waivers.html",
              CTA_URL)),

      Map.entry(LicensedFeature.BULK_WAIVERS,
          new UpsellInfo(
              "Bulk waivers are not available in your current plan.",
              "Upgrade to Lifecycle Enterprise to resolve multiple violations in a single action instead of waiving them one by one.",
              "https://help.sonatype.com/en/bulk-waivers.html",
              CTA_URL)));

  private static final UpsellInfo DEFAULT_UPSELL = new UpsellInfo(
      "This feature is not available in your current plan.",
      "Upgrade to Lifecycle Enterprise for access to advanced capabilities.",
      null,
      CTA_URL);

  private EntitlementUpsellRegistry() {
    // utility class
  }

  public static UpsellInfo getUpsellInfo(LicensedFeature feature) {
    return REGISTRY.getOrDefault(feature, DEFAULT_UPSELL);
  }
}
