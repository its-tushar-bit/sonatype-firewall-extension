/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import com.sonatype.insight.brain.service.consumption.ConsumptionSourceClassifier.Source;
import com.sonatype.insight.telemetry.SonatypeUserAgentUtil;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Enclosed.class)
public class ConsumptionSourceClassifierTest
{
  @RunWith(Parameterized.class)
  public static class ClassifyTableTest
  {
    @Parameterized.Parameter(0)
    public String label;

    @Parameterized.Parameter(1)
    public String userAgent;

    @Parameterized.Parameter(2)
    public String path;

    @Parameterized.Parameter(3)
    public Source expected;

    @Test
    public void classifiesAsExpected() {
      Source actual = ConsumptionSourceClassifier.classify(userAgent, path);
      assertThat(actual)
          .as("UA=%s path=%s (%s)", userAgent, path, label)
          .isEqualTo(expected);
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> cases() {
      return Arrays.asList(new Object[][]{
        {"browser on /ui/", "Mozilla/5.0 (X11; Linux) AppleWebKit/537.36", "/ui/main.js", Source.UI},
        {"browser on /rest/", "Mozilla/5.0 (X11; Linux) AppleWebKit/537.36", "/rest/policy/list", Source.UI},
        {"browser on /api/", "Mozilla/5.0 (X11; Linux) AppleWebKit/537.36", "/api/v2/applications", Source.UI},

        {"Nexus_IQ_CLI on /api/",
          "Nexus_IQ_CLI/1.180 (OSS; Linux; 5.15.0; x86_64; 17.0.1)", "/api/v2/scan", Source.CLI},
        {"Sonatype_CLM_CLI on /api/ (rebranded JVM CLI, observed in IQ CLI 2.8.5)",
          "Sonatype_CLM_CLI/2.8.5-01 (Java 21.0.10; Mac OS X 26.3.1)",
          "/api/v2/scan", Source.CLI},
        {"Docker_Nexus_IQ_CLI on /api/ (Sonatype-published Docker wrapper)",
          "Docker_Nexus_IQ_CLI/1.180 (OSS; Linux; 5.15.0; x86_64; 17.0.1)",
          "/api/v2/scan", Source.CLI},
        {"GitLab_Nexus_IQ_CLI on /api/ (Sonatype-published GitLab template)",
          "GitLab_Nexus_IQ_CLI/1.180 (OSS; Linux; 5.15.0; x86_64; 17.0.1)",
          "/api/v2/scan", Source.CLI},
        {"Jenkins with embedded Nexus_IQ_CLI classifies by outer CI identifier",
          "Jenkins/2.400 Nexus_IQ_CLI/1.180 (OSS; Linux; 5.15.0; x86_64; 17.0.1)",
          "/api/v2/scan", Source.CI_CD},

        {"Nexus repo on /api/",
          "Nexus/3.9.0-01 (PRO; Mac OS X; 10.16; x86_64; 1.8.0_292)", "/api/v2/firewall", Source.REPO_MANAGER},
        {"FWFA on /api/",
          "Firewall_For_Jfrog_Artifactory/2.3-SNAPSHOT (; Linux; 5.10; amd64; 11; Jfrog Artifactory 7.37.15)",
          "/api/v2/firewall", Source.REPO_MANAGER},

        {"IntelliJ plugin",
          "Nexus_IQPlugin_IntelliJ/2.5 (IDEA; Windows 10; 10.0; amd64; 17.0.1)",
          "/api/v2/evaluate", Source.IDE},

        {"Jenkins alone", "Jenkins/2.400", "/api/v2/scan", Source.CI_CD},
        {"GitHub-Actions (CI wins over browser token)",
          "Mozilla/5.0 (compatible) GitHub-Actions/1.0", "/api/v2/scan", Source.CI_CD},
        {"bitbucket-pipelines", "bitbucket-pipelines/1.0", "/api/v2/scan", Source.CI_CD},
        {"drone", "Drone/2.0", "/api/v2/scan", Source.CI_CD},
        {"buildkite", "buildkite-agent/3.0", "/api/v2/scan", Source.CI_CD},
        {"codebuild", "AWS CodeBuild/1.0", "/api/v2/scan", Source.CI_CD},
        {"harness", "harness/1.0", "/api/v2/scan", Source.CI_CD},
        {"JENKINS mixed case", "JENKINS/2.400", "/api/v2/scan", Source.CI_CD},

        {"HeadlessChrome", "Mozilla/5.0 HeadlessChrome/120.0", "/api/v2/applications", Source.UNKNOWN},
        {"Playwright", "Mozilla/5.0 (X11) playwright/1.40", "/api/v2/applications", Source.UNKNOWN},
        {"Puppeteer", "puppeteer/21.0", "/rest/policy/list", Source.UNKNOWN},
        {"Selenium", "Selenium/4.15", "/api/v2/applications", Source.UNKNOWN},

        {"curl on /api/", "curl/8.1", "/api/v2/applications", Source.API},
        {"curl on /rest/", "curl/8.1", "/rest/v1/components", Source.API},
        {"python-requests on /api/", "python-requests/2.31", "/api/v2/applications", Source.API},
        {"Apache-HttpClient on /api/", "Apache-HttpClient/4.5 (Java/17)", "/api/v2/scan", Source.API},
        {"null UA on /api/", null, "/api/v2/applications", Source.API},
        {"null UA on /rest/", null, "/rest/policy/list", Source.API},
        {"empty UA on /api/", "", "/api/v2/applications", Source.API},

        {"Apache-HttpClient on /health", "Apache-HttpClient/4.5", "/health", Source.UNKNOWN},
        {"null UA on /health", null, "/health", Source.UNKNOWN},
        {"dash UA on /health", "-", "/health", Source.UNKNOWN},
        {"null UA and null path", null, null, Source.UNKNOWN},
      });
    }
  }

  public static class SonatypeParserAdjudication
  {
    @Test
    public void nexusRepo_v39_pro() {
      assertProductLowerCaseEquals(
          "Nexus/3.9.0-01 (PRO; Mac OS X; 10.16; x86_64; 1.8.0_292)", "nexus");
    }

    @Test
    public void fwfa() {
      assertProductLowerCaseEquals(
          "Firewall_For_Jfrog_Artifactory/2.3-SNAPSHOT (; Linux; 5.10; amd64; 11; Jfrog Artifactory 7.37.15)",
          "firewall_for_jfrog_artifactory");
    }

    @Test
    public void iqCli() {
      assertProductLowerCaseEquals(
          "Nexus_IQ_CLI/1.180 (OSS; Linux; 5.15.0; x86_64; 17.0.1)", "nexus_iq_cli");
    }

    @Test
    public void sonatypeClmCli_observedFromIqCli285() {
      assertProductLowerCaseEquals(
          "Sonatype_CLM_CLI/2.8.5-01 (Java 21.0.10; Mac OS X 26.3.1)", "sonatype_clm_cli");
    }

    @Test
    public void dockerNexusIqCli() {
      assertProductLowerCaseEquals(
          "Docker_Nexus_IQ_CLI/1.180 (OSS; Linux; 5.15.0; x86_64; 17.0.1)", "docker_nexus_iq_cli");
    }

    @Test
    public void gitlabNexusIqCli() {
      assertProductLowerCaseEquals(
          "GitLab_Nexus_IQ_CLI/1.180 (OSS; Linux; 5.15.0; x86_64; 17.0.1)", "gitlab_nexus_iq_cli");
    }

    @Test
    public void idePlugin_intellij_startsWithExpectedPrefix() {
      assertProductLowerCaseStartsWith(
          "Nexus_IQPlugin_IntelliJ/2.5 (IDEA; Windows 10; 10.0; amd64; 17.0.1)", "nexus_iqplugin");
    }

    private static void assertProductLowerCaseEquals(String userAgent, String expectedLc) {
      SonatypeUserAgentUtil.UserAgent parsed = SonatypeUserAgentUtil.parse(userAgent);
      assertThat(parsed).as("parse(%s) must be non-null", userAgent).isNotNull();
      assertThat(parsed.product).as("parse(%s).product must be non-null", userAgent).isNotNull();
      assertThat(parsed.product.toLowerCase(Locale.ROOT)).isEqualTo(expectedLc);
    }

    private static void assertProductLowerCaseStartsWith(String userAgent, String expectedLcPrefix) {
      SonatypeUserAgentUtil.UserAgent parsed = SonatypeUserAgentUtil.parse(userAgent);
      assertThat(parsed).as("parse(%s) must be non-null", userAgent).isNotNull();
      assertThat(parsed.product).as("parse(%s).product must be non-null", userAgent).isNotNull();
      assertThat(parsed.product.toLowerCase(Locale.ROOT)).startsWith(expectedLcPrefix);
    }
  }
}
