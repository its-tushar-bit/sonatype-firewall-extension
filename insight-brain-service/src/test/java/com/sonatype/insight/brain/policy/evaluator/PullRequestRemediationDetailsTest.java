/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestRemediationDetailsTest extends AbstractComponentTest
{
  @Inject
  private InsightConfig config;

  @Before
  public void before() {
    config.setBaseUrl("http://localhost:1122");
  }

  @Test
  public void testSecurityVulnerabilityReport() throws Exception {
    Application app = tempEntity.newApplicationWithParent("my-public-app-id", "MyTestApp", "Integrations");
    String scanId = "5ea5363997ee2ba0c8730a5f785ae2c6";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("org.jooq", "jooq", "3.11.2");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("foo", "bar", "baz");

    List<PolicyNotification> policyNotifications = new ArrayList<>();

    PolicyNotification criticalPolicy = new PolicyNotification(
        new PolicyFact("critical-id", "Security-Critical", 10),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact criticalFact = new ComponentFact(componentIdentifier, "dummy-hash");
    ConstraintFact criticalConstraintFact = new ConstraintFact("constraint-id", "Critical risk CVSS score", "OR");
    criticalConstraintFact.addConditionFact(new ConditionFact("SecurityVulnerabilitySeverity", 0,
        "Security Vulnerability Severity >= 9",
        "Found security vulnerability CVE-2016-1000031 with severity 9.8.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "CVE-2016-1000031")));
    criticalFact.addConstraintFact(criticalConstraintFact);
    criticalPolicy.getPolicyFact().addComponentFact(criticalFact);

    //introduce a second fact for the same component, with differing CVE
    criticalPolicy.getPolicyFact().addComponentFact(
        criticalFact.with(criticalConstraintFact.with(new ConditionFact("SecurityVulnerabilitySeverity", 0,
            "Security Vulnerability Severity >= 9",
            "Found security vulnerability CVE-2016-1000032 with severity 9.8.",
            new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "CVE-2016-1000032")))));

    //introduce a different Component violating the same policy, should be omitted from output
    ComponentFact criticatFact2 = new ComponentFact(componentIdentifier2, "dummy-hash");
    ConstraintFact criticalConstraintFact2 =
        new ConstraintFact("constraint-id", "Another Critical risk CVSS score", "OR");
    criticatFact2.addConstraintFact(criticalConstraintFact2);
    criticalConstraintFact2.addConditionFact(new ConditionFact("SecurityVulnerabilitySeverity", 0,
        "Security Vulnerability Severity >= 9",
        "Found security vulnerability SONATYPE-2019-01 with severity 9.8.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "SONATYPE-2019-01")));
    criticalPolicy.getPolicyFact().addComponentFact(criticatFact2);
    
    policyNotifications.add(criticalPolicy);

    PolicyNotification highPolicy = new PolicyNotification(
        new PolicyFact("high-id", "Security-High", 9),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact highFact = new ComponentFact(componentIdentifier, "dummy-high-hash");
    ConstraintFact highConstraintFact = new ConstraintFact("constraint-id", "High risk CVSS score", "OR");
    highConstraintFact.addConditionFact(new ConditionFact("SecurityVulnerabilitySeverity", 0,
        "Security Vulnerability Severity >= 7",
        "Found security vulnerability SONATYPE-2017-0312 with severity 8.5.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "SONATYPE-2017-0312")));
    highFact.addConstraintFact(highConstraintFact);
    highPolicy.getPolicyFact().addComponentFact(highFact);
    policyNotifications.add(highPolicy);

    PolicyNotification mediumPolicy = new PolicyNotification(
        new PolicyFact("medium-id", "Security-Medium", 7),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact mediumFact = new ComponentFact(componentIdentifier, "dummy-hash");
    ConstraintFact mediumConstraintFact = new ConstraintFact("med-constraint-id", "Medium risk CVSS score", "OR");
    mediumConstraintFact.addConditionFact(new ConditionFact("SecurityVulnerabilityMedium", 0,
        "Security Vulnerability Severity < 9",
        "Found security vulnerability CVE-2018-10237 with severity 5.9.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "CVE-2018-10237")));
    mediumFact.addConstraintFact(mediumConstraintFact);
    mediumPolicy.getPolicyFact().addComponentFact(mediumFact);
    policyNotifications.add(mediumPolicy);

    PolicyNotification lowPolicy = new PolicyNotification(
        new PolicyFact("low-id", "Security-Low", 4),
        new Notifications(new UserNotification("tester@foo.com")));
    ComponentFact lowFact = new ComponentFact(componentIdentifier, "dummy-low-hash");
    ConstraintFact lowConstraintFact = new ConstraintFact("constraint-id", "Low risk CVSS score", "OR");
    lowConstraintFact.addConditionFact(new ConditionFact("SecurityVulnerabilitySeverity", 0,
        "Security Vulnerability Severity <= 4",
        "Found security vulnerability sonatype-2017-1234 with severity 3.9.",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "sonatype-2017-1234")));
    lowFact.addConstraintFact(lowConstraintFact);
    lowPolicy.getPolicyFact().addComponentFact(lowFact);
    policyNotifications.add(lowPolicy);

    ApiVersionChangeOptionDTO versionChangeOptionDTO = new ApiVersionChangeOptionDTO();
    ApiComponentChangeActionDTO changeActionDTO = new ApiComponentChangeActionDTO();
    versionChangeOptionDTO.setData(changeActionDTO);
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    changeActionDTO.setComponent(componentDTOV2);
    // upgrade version
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("org.jooq", "jooq", "3.11.3"));
    ApiComponentRemediationDTO remediationDTO = new ApiComponentRemediationDTO();
    remediationDTO.remediation.versionChanges = Arrays.asList(versionChangeOptionDTO);
    
    PullRequestRemediationDetails details =
        new PullRequestRemediationDetails(componentIdentifier, "3.11.3", "pullRequest", policyNotifications, app,
            scanId, new Stage(Stage.ID_BUILD), lookup(BaseUrl.class));

    assertThat(details.getTitle()).isEqualTo("Bump org.jooq:jooq:3.11.2 to 3.11.3");

    Path path = Paths.get(getClass().getResource("/PullRequestRemediationDetailsTest/VulnerabilityReport.md").toURI());
    String expectedContent = new String(Files.readAllBytes(path));
    assertThat(details.getContents()).isEqualTo(expectedContent);
  }
}
