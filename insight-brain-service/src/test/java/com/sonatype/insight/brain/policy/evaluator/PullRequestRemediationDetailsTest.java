/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestRemediationDetailsTest
    extends AbstractComponentTest
{
  @Inject
  private InsightConfig config;

  private static final String SCAN_ID = "5ea5363997ee2ba0c8730a5f785ae2c6";

  private Application app;

  @Before
  public void before() {
    config.setBaseUrl("http://localhost:1122");
    app = tempEntity.newApplicationWithParent("my-public-app-id", "MyTestApp", "Integrations");
    PullRequestRemediationDetails.clock = Clock
        .fixed(Instant.parse("2019-11-26T18:15:30Z"), ZoneId.of("America/Los_Angeles"));
  }

  @Test
  public void testSecurityVulnerabilityReport() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("org.jooq", "jooq", "3.11.2");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("foo", "bar", "baz");

    List<PolicyNotification> policyNotifications = createPolicyNotifications(componentIdentifier, componentIdentifier2);

    PullRequestRemediationDetails details =
        new PullRequestRemediationDetails(componentIdentifier, "3.11.3", "pullRequest", policyNotifications, app,
            SCAN_ID, Stage.ID_BUILD, lookup(BaseUrl.class).getConfigured());

    assertThat(details.getTitle()).isEqualTo("Bump jooq to 3.11.3");

    Path path = Paths.get(getClass().getResource("/PullRequestRemediationDetailsTest/VulnerabilityReport.md").toURI());
    String expectedContent = new String(Files.readAllBytes(path));
    assertThat(details.getContents()).isEqualTo(expectedContent);
  }

  @Test
  public void testSecurityVulnerabilityReport_npmComponent() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("@sonatype/foo", "1.0");

    List<PolicyNotification> policyNotifications = new ArrayList<>();

    PolicyNotification criticalPolicyNotification = new PolicyNotification(
        new PolicyFact("critical-id", "Security-Critical", 10),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact criticalComponentFact = new ComponentFact(componentIdentifier, "dummy-hash");
    ConstraintFact criticalConstraintFact = new ConstraintFact("constraint-id", "Critical risk CVSS score", "OR");
    criticalConstraintFact.addConditionFact(new ConditionFact("SecurityVulnerabilitySeverity", 0,
        "Security Vulnerability Severity >= 9",
        "Found security vulnerability CVE-2016-1000031 with severity 9.8.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "CVE-2016-1000031")));
    criticalComponentFact.addConstraintFact(criticalConstraintFact);
    criticalPolicyNotification.getPolicyFact().addComponentFact(criticalComponentFact);

    policyNotifications.add(criticalPolicyNotification);

    PullRequestRemediationDetails details =
        new PullRequestRemediationDetails(componentIdentifier, "1.1", "pullRequest", policyNotifications, app,
            SCAN_ID, Stage.ID_BUILD, lookup(BaseUrl.class).getConfigured());

    assertThat(details.getTitle()).isEqualTo("Bump @sonatype/foo to 1.1");

    Path path = Paths
        .get(getClass().getResource("/PullRequestRemediationDetailsTest/VulnerabilityReport_npm.md").toURI());
    String expectedContent = new String(Files.readAllBytes(path));
    assertThat(details.getContents()).isEqualTo(expectedContent);
  }

  @Test
  public void testSinglePolicyViolationPlurality() throws IOException {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("org.jooq", "jooq", "3.11.2");
    List<PolicyNotification> policyNotifications = ImmutableList.of(new PolicyNotification(
        new PolicyFact("critical-id", "Security-Critical", 10), new Notifications()));

    PullRequestRemediationDetails details =
        new PullRequestRemediationDetails(componentIdentifier, "3.11.3", "pullRequest", policyNotifications, app,
            SCAN_ID, Stage.ID_BUILD, lookup(BaseUrl.class).getConfigured());

    assertThat(details.getContents().replace("\r\n", "\n"))
        .startsWith("## :shield: Automated pull request to fix 1 Nexus IQ Policy Violation\n");
  }

  /**
   * A complex set of policy violations covering multiple components and policies. Intended as a superset of
   * possible scenarios we could encounter when synthesizing these into a simple aggregated display.
   */
  private List<PolicyNotification> createPolicyNotifications(final ComponentIdentifier componentIdentifier,
                                                             final ComponentIdentifier componentIdentifier2)
  {
    List<PolicyNotification> policyNotifications = new ArrayList<>();

    PolicyNotification criticalPolicyNotification = new PolicyNotification(
        new PolicyFact("critical-id", "Security-Critical", 10),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact criticalComponentFact = new ComponentFact(componentIdentifier, "dummy-hash");
    ConstraintFact criticalConstraintFact = new ConstraintFact("constraint-id", "Critical risk CVSS score", "OR");
    criticalConstraintFact.addConditionFact(new ConditionFact("SecurityVulnerabilitySeverity", 0,
        "Security Vulnerability Severity >= 9",
        "Found security vulnerability CVE-2016-1000031 with severity 9.8.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "CVE-2016-1000031")));
    criticalComponentFact.addConstraintFact(criticalConstraintFact);
    criticalPolicyNotification.getPolicyFact().addComponentFact(criticalComponentFact);

    //introduce a second fact for the same component, with differing CVE
    ComponentFact criticalComponentFact1 =
        new ComponentFact(criticalComponentFact.getComponentIdentifier(), criticalComponentFact.getHash());
    criticalComponentFact1.addConstraintFact(new ConstraintFact(criticalConstraintFact.getConstraintId(),
        criticalConstraintFact.getConstraintName(), criticalConstraintFact.getOperatorName(),
        new ConditionFact("SecurityVulnerabilitySeverity", 0, "Security Vulnerability Severity >= 9",
            "Found security vulnerability CVE-2016-1000032 with severity 9.8.",
            new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "CVE-2016-1000032"))));
    criticalPolicyNotification.getPolicyFact().addComponentFact(criticalComponentFact1);

    //introduce a different Component violating the same policy, should be omitted from output
    ComponentFact criticatComponentFact2 = new ComponentFact(componentIdentifier2, "dummy-hash");
    ConstraintFact criticalConstraintFact2 =
        new ConstraintFact("constraint-id", "Another Critical risk CVSS score", "OR");
    criticalConstraintFact2.addConditionFact(new ConditionFact("SecurityVulnerabilitySeverity", 0,
        "Security Vulnerability Severity >= 9",
        "Found security vulnerability SONATYPE-2019-01 with severity 9.8.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "SONATYPE-2019-01")));
    criticatComponentFact2.addConstraintFact(criticalConstraintFact2);
    criticalPolicyNotification.getPolicyFact().addComponentFact(criticatComponentFact2);

    policyNotifications.add(criticalPolicyNotification);

    PolicyNotification highPolicyNotification = new PolicyNotification(
        new PolicyFact("high-id", "Security-High", 9),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact highComponentFact = new ComponentFact(componentIdentifier, "dummy-high-hash");
    ConstraintFact highConstraintFact = new ConstraintFact("constraint-id", "High risk CVSS score", "OR");
    highConstraintFact.addConditionFact(new ConditionFact("SecurityVulnerabilitySeverity", 0,
        "Security Vulnerability Severity >= 7",
        "Found security vulnerability SONATYPE-2017-0312 with severity 8.5.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "SONATYPE-2017-0312")));
    highComponentFact.addConstraintFact(highConstraintFact);
    highPolicyNotification.getPolicyFact().addComponentFact(highComponentFact);
    policyNotifications.add(highPolicyNotification);

    PolicyNotification mediumPolicyNotification = new PolicyNotification(
        new PolicyFact("medium-id", "Security-Medium", 7),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact mediumComponentFact = new ComponentFact(componentIdentifier, "dummy-hash");
    ConstraintFact mediumConstraintFact = new ConstraintFact("med-constraint-id", "Medium risk CVSS score", "OR");
    mediumConstraintFact.addConditionFact(new ConditionFact("SecurityVulnerabilityMedium", 0,
        "Security Vulnerability Severity < 9",
        "Found security vulnerability CVE-2018-10237 with severity 5.9.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "CVE-2018-10237")));
    mediumComponentFact.addConstraintFact(mediumConstraintFact);
    mediumPolicyNotification.getPolicyFact().addComponentFact(mediumComponentFact);
    policyNotifications.add(mediumPolicyNotification);

    PolicyNotification lowPolicyNotification = new PolicyNotification(
        new PolicyFact("low-id", "Security-Low", 4),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact lowComponentFact = new ComponentFact(componentIdentifier, "dummy-low-hash");
    ConstraintFact lowConstraintFact = new ConstraintFact("constraint-id", "Low risk CVSS score", "OR");
    lowConstraintFact.addConditionFact(new ConditionFact("SecurityVulnerabilitySeverity", 0,
        "Security Vulnerability Severity <= 4",
        "Found security vulnerability sonatype-2017-1234 with severity 3.9.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "sonatype-2017-1234")));
    lowComponentFact.addConstraintFact(lowConstraintFact);
    lowPolicyNotification.getPolicyFact().addComponentFact(lowComponentFact);
    policyNotifications.add(lowPolicyNotification);
    return policyNotifications;
  }
}
