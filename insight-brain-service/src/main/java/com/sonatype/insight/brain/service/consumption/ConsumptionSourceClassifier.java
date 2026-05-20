/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.Locale;
import java.util.Set;

import com.sonatype.insight.telemetry.SonatypeUserAgentUtil;

import org.apache.commons.lang3.StringUtils;

/**
 * Classifies an inbound HTTP request into a consumption-source bucket from User-Agent and path.
 *
 * @since 1.204
 */
public final class ConsumptionSourceClassifier
{
  /** Consumption-source buckets; {@link #token()} is the string persisted to {@code consumption_events.source}. */
  public enum Source
  {
    UI("UI"),
    CLI("CLI"),
    CI_CD("CI_CD"),
    IDE("IDE"),
    REPO_MANAGER("REPO_MANAGER"),
    API("API"),
    CONTINUOUS_MONITOR("CONTINUOUS_MONITOR"),
    UNKNOWN("UNKNOWN");

    private final String token;

    Source(String token) {
      this.token = token;
    }

    public String token() {
      return token;
    }
  }

  static final Set<String> SONATYPE_CLI_PRODUCTS = Set.of(
      "nexus_iq_cli",
      "iq-cli",
      "cli",
      "sonatype_clm_cli",
      "docker_nexus_iq_cli",
      "gitlab_nexus_iq_cli");

  static final Set<String> SONATYPE_REPO_MANAGER_PRODUCTS =
      Set.of("nexus", "firewall_for_jfrog_artifactory", "nexus_repository_firewall");

  static final String SONATYPE_IDE_PRODUCT_PREFIX = "nexus_iqplugin";

  static final Set<String> CI_CD_TOKENS = Set.of(
      "jenkins", "bamboo", "teamcity", "circleci", "travis",
      "github-actions", "gitlab-ci", "azure-devops",
      "bitbucket-pipelines", "drone", "buildkite",
      "codebuild", "cloud-build", "harness");

  static final Set<String> HEADLESS_BROWSER_TOKENS = Set.of(
      "headlesschrome", "headless chrome", "playwright",
      "puppeteer", "selenium", "phantomjs", "webdriver");

  static final Set<String> API_PATH_PREFIXES = Set.of("/api/", "/rest/");

  private ConsumptionSourceClassifier() {
  }

  /** Classifies the request into a {@link Source}; never returns null. */
  public static Source classify(String userAgent, String path) {
    Source sonatypeBucket = classifyBySonatypeProduct(userAgent);
    if (sonatypeBucket != null) {
      return sonatypeBucket;
    }

    String ualc = userAgent == null ? null : userAgent.toLowerCase(Locale.ROOT);

    if (ualc != null && containsAny(ualc, CI_CD_TOKENS)) {
      return Source.CI_CD;
    }
    if (ualc != null && containsAny(ualc, HEADLESS_BROWSER_TOKENS)) {
      return Source.UNKNOWN;
    }
    if (ualc != null && (ualc.contains("mozilla") || ualc.contains("webkit"))) {
      return Source.UI;
    }
    if (path != null) {
      for (String prefix : API_PATH_PREFIXES) {
        if (path.startsWith(prefix)) {
          return Source.API;
        }
      }
    }
    return Source.UNKNOWN;
  }

  private static Source classifyBySonatypeProduct(String userAgent) {
    if (StringUtils.isBlank(userAgent)) {
      return null;
    }
    SonatypeUserAgentUtil.UserAgent parsed = SonatypeUserAgentUtil.parse(userAgent);
    if (parsed == null || StringUtils.isBlank(parsed.product)) {
      return null;
    }
    String productLc = parsed.product.toLowerCase(Locale.ROOT);
    if (productLc.startsWith(SONATYPE_IDE_PRODUCT_PREFIX)) {
      return Source.IDE;
    }
    if (SONATYPE_CLI_PRODUCTS.contains(productLc)) {
      return Source.CLI;
    }
    if (SONATYPE_REPO_MANAGER_PRODUCTS.contains(productLc)) {
      return Source.REPO_MANAGER;
    }
    return null;
  }

  private static boolean containsAny(String haystack, Set<String> needles) {
    for (String needle : needles) {
      if (haystack.contains(needle)) {
        return true;
      }
    }
    return false;
  }
}
