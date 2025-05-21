/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.PresentReachableComponentVulnerabilities;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetryBuilder;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.hasPolicyViolationByComponentIdentifier;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.APPLICATION_ID;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.AUTO_POLICY_WAIVER_ID;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.CALL_FLOW_EVALUATION_SUCCESSFUL;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.CALL_FLOW_HAS_REACHABLE_INFORMATION_FOR_COMPONENT;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.COMPONENT_DOWNGRADE;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.COMPONENT_IDENTIFIER;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.COMPONENT_NAME;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.COMPONENT_NAMESPACE;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.COMPONENT_UPGRADE;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.COMPONENT_VERSION;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.CONDITION_TYPE;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.COUNT;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.DIRECT_DEPENDENCY;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.ECOSYSTEM;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.FIX_BY_VERSION_CHANGE;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.FIX_TIME;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.INNERSOURCE_DEPENDENCY;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.IS_SCM_ENABLED;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.LEGACY_VIOLATION_TIME;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.NEW_POLICY_VIOLATION_ID;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.OPEN_TIME;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.POLICY_VIOLATION_ID;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.POLICY_WAIVER_ID;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.REACHABILITY_STATUS;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.STAGE;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.THREAT_CATEGORY;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.THREAT_LEVEL;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.TIME;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.UNWAIVE_TIME;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.WAIVER_EXPIRATION;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.WAIVE_TIME;
import static com.sonatype.insight.brain.telemetry.TelemetryUtils.REAL_APPLICATION_ID;
import static com.sonatype.insight.purl.PackageUrlIdentifier.fromComponentIdentifier;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.CALLFLOW_EVALUATION_COMPONENT_COUNTS;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.CONDITION_TYPE_VIOLATION;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_CHANGE_VERSION_POLICY_VIOLATION;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_LEGACY_VIOLATION;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationTelemetryCollectorTest
    extends AbstractComponentTest
{
  private static final String TEST_APP_ID = "testApp";

  private static final String TEST_STAGE = "testStage";

  private static final PolicyEvaluation policyEvaluation =
      new PolicyEvaluation(TEST_APP_ID, TEST_STAGE, "scanId123", CurrentUser.SYSTEM, ScanTriggerType.CLI);

  static {
    policyEvaluation.setTime(new Date());
  }

  private static final ComponentIdentifier commonsLang3 = ComponentIdentifier.createMavenCoordinates(
      "org.apache.commons", "commons-lang3", "3.8.1");

  private static final ComponentIdentifier lodashv3 = ComponentIdentifier.createNpmCoordinates("lodash", "3.0.4");

  private static final ComponentIdentifier lodashv4 = ComponentIdentifier.createNpmCoordinates("lodash", "4.17.15");

  private static final ComponentIdentifier lodashv5 = ComponentIdentifier.createNpmCoordinates("lodash", "5.1.0");

  private static final ComponentIdentifier urllib3 = ComponentIdentifier.createPypiCoordinates(
      "urllib3", "1.25.7", null, "py");

  private static final String TEST_POLICY = "testPolicy";

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private TelemetryUtils telemetryUtils;

  @Test
  public void testAddTelemetryForConditionTypeViolation() {
    // given a new policy violation and a telemetry collector
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .withConditionType(HygieneRatingConditionType.ID);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when
    telemetryCollector.addTelemetryForConditionTypeViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getConditionType(),
        testablePolicyViolation.getComponents()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, CONDITION_TYPE_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByDowngrade() {
    // given a policy violation on lodash v4 that was fixed by downgrading
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .openedHoursAgo(48)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByDowngrade")
            .markFixedByDowngrade();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in downgraded component
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        createWrappedComponent(lodashv3, true, false)
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(
        telemetryData,
        TIME_TO_REMEDIATE_POLICY_VIOLATION,
        TIME_TO_CHANGE_VERSION_POLICY_VIOLATION
    );
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByOtherMeans() {
    // given a policy violation on lodash v4 that was fixed by downgrading
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .openedHoursAgo(2)
            .asDirectDependency(true)
            .asInnerSourceDependency(true)
            .withPolicyViolationId("fixedByOtherMeans")
            .withAdditionalComponentVersion(lodashv3, true, true)
            .withAdditionalComponentVersion(lodashv5, true, true)
            .markFixedByOtherMeans();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in multiple additional versions
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getAdditionalVersions()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByRemoval() {
    // given a policy violation on lodash v4 that was fixed by downgrading
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .openedHoursAgo(37)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByRemoval")
            .markFixedByRemoval();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in empty component list
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade() {
    // given a policy violation on lodash v4 that was fixed by downgrading
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv4)
            .withScmEnabled(true)
            .openedHoursAgo(72)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByUpgrade")
            .markFixedByUpgrade();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in an upgraded component
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        createWrappedComponent(lodashv5, true, false)
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(
        telemetryData,
        TIME_TO_REMEDIATE_POLICY_VIOLATION,
        TIME_TO_CHANGE_VERSION_POLICY_VIOLATION
    );
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedWithoutVersionChange() {
    // given a policy violation on lodash v5
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv5)
            .openedHoursAgo(480)
            .asDirectDependency(false)
            .withPolicyViolationId("fixedWithoutVersionChange")
            .markFixedByOtherMeans();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedWithoutVersionChange_licenseData() {
    // given a policy violation on lodash v5
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultLicenseViolationForComponent(lodashv5)
            .openedHoursAgo(480)
            .withScmEnabled(false)
            .withThreatLevel(10)
            .withConditionType(LicenseThreatGroupConditionType.ID)
            .asDirectDependency(false)
            .withPolicyViolationId("fixedWithoutVersionChange_license")
            .markFixedByOtherMeans();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForLegacyViolation() {
    // given a policy violation on lodash v3
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv3)
            .openedHoursAgo(480)
            .asDirectDependency(false)
            .withPolicyViolationId("legacyViolation")
            .markFixedAsLegacy();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForLegacyViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_LEGACY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForUnwaivedViolation() {
    // given a policy violation that was unwaived and the new open violation created for it
    var policyWaiver = tempEntity.newWaiver(tempEntity.newPolicy().getId(), policyEvaluation.getApplicationId());

    var replacementPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(0)
        .asDirectDependency(true)
        .withPolicyViolationId("newViolationAfterUnwaived")
        .withConditionType(SecurityVulnerabilitySeverityConditionType.ID);

    var unwaivedPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(500)
        .asDirectDependency(true)
        .withPolicyViolationId("unwaivedViolation")
        .markWaived(policyWaiver)
        .markUnwaived(policyWaiver.getId(), replacementPolicyViolation.getPolicyViolation());

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(unwaivedPolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForUnwaivedViolation(
        unwaivedPolicyViolation.getPolicyViolation(),
        replacementPolicyViolation.getPolicyViolation(),
        unwaivedPolicyViolation.getComponent()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    unwaivedPolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_WAIVE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForWaivedViolation() {
    // given a policy violation and a waiver for it
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(tempEntity.newPolicy().getId(), policyEvaluation.getApplicationId());

    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(5)
            .asDirectDependency(true)
            .withPolicyViolationId("waivedViolation")
            .markWaived(policyWaiver);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForWaivedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_WAIVE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForAutoWaivedViolation() {
    // given a policy violation and a waiver for it
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(policyEvaluation.getApplicationId());

    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(commonsLang3)
            .openedHoursAgo(5)
            .asDirectDependency(true)
            .withPolicyViolationId("waivedViolation")
            .markAutoWaived(autoPolicyWaiver);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForAutoWaivedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_WAIVE_POLICY_VIOLATION);

  }

  @Test
  public void testAddTelemetryForUnwaivedViolation_wasAutoWaived() {
    // given an uwaived policy violation and the new open violation that replaces it
    var autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(policyEvaluation.getApplicationId());

    var replacementPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(0)
        .asDirectDependency(true)
        .withPolicyViolationId("newViolationAfterUnwaived")
        .withConditionType(SecurityVulnerabilitySeverityConditionType.ID);

    var unwaivedPolicyViolation = TestablePolicyViolation.createDefaultSecurityViolationForComponent(urllib3)
        .openedHoursAgo(500)
        .asDirectDependency(true)
        .withPolicyViolationId("unwaivedViolation")
        .markAutoWaived(autoPolicyWaiver)
        .markUnAutoWaived(autoPolicyWaiver.getId(), replacementPolicyViolation.getPolicyViolation());

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(unwaivedPolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForUnwaivedViolation(
        unwaivedPolicyViolation.getPolicyViolation(),
        replacementPolicyViolation.getPolicyViolation(),
        unwaivedPolicyViolation.getComponent()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    unwaivedPolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_WAIVE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForReachableViolation_When_ViolationIsNotReachable() {
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultSecurityViolationForComponent(lodashv3);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    telemetryCollector.addTelemetryForReachableViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent(),
        null
    );

    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, CALLFLOW_EVALUATION_COMPONENT_COUNTS);
  }

  @Test
  public void testAddTelemetryForReachableViolation_When_ViolationIsReachable() {
    TestablePolicyViolation testablePolicyViolation = TestablePolicyViolation
        .createDefaultSecurityViolationForComponent(lodashv3);

    PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities = new PurlIdentifiersWithVulnerabilities(
        null,
        null,
        Map.of(
            fromComponentIdentifier(testablePolicyViolation.policyViolation.getComponentIdentifier()),
            new PresentReachableComponentVulnerabilities(Set.of("CVE-1234"))
        ));

    testablePolicyViolation.withPurlIdentifiersWithVulnerabilities(purlIdentifiersWithVulnerabilities);

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    telemetryCollector.addTelemetryForReachableViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent(),
        purlIdentifiersWithVulnerabilities
    );

    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, CALLFLOW_EVALUATION_COMPONENT_COUNTS);
  }

  private PolicyViolationTelemetryCollector createTelemetryCollector(boolean isScmEnabled) {
    PolicyViolationTelemetryCollector telemetryCollector =
        new PolicyViolationTelemetryCollector(policyWaiverDAO, telemetryUtils, isScmEnabled);
    telemetryCollector.setTimeOfPolicyEvaluation(policyEvaluation.getTime());
    return telemetryCollector;
  }

  private List<Component> createWrappedComponent(
      ComponentIdentifier componentIdentifier,
      boolean isDirectDependency,
      boolean isInnersource)
  {
    Component component = new Component(componentIdentifier);
    component.setDirectDependency(isDirectDependency);
    component.setInnerSourceData(isInnersource ? Collections.singleton(new InnerSourceData()) : null);
    return List.of(component);
  }

  /**
   * Helper class to create the policy violation and component data needed for the tests and the validations against
   * that data.
   */
  private static class TestablePolicyViolation
  {
    private final List<Component> additionalVersions = new ArrayList<>();

    private final List<Component> components = new ArrayList<>();

    private final PolicyViolation policyViolation;

    private PolicyViolation replacementPolicyViolation;

    private Component component;

    private String conditionType;

    private int count = 1;

    private String cveIdentifier;

    private Double cvssScore;

    private boolean expectUnwaived;

    private String fixReason;

    private Long fixTime;

    private boolean isScmEnabled;

    private String licenseThreatGroup;

    private String waiverId;

    private String autoPolicyWaiverId;

    private Long openTime;

    private String waiverExpiration;

    private PurlIdentifiersWithVulnerabilities reachablePurlIdentifiersWithVulnerabilities;

    TestablePolicyViolation(ComponentIdentifier componentIdentifier, String policyName) {
      this.component = new Component(componentIdentifier);
      this.components.add(component);
      this.policyViolation = new PolicyViolation();
      policyViolation.setComponentIdentifier(componentIdentifier);
      policyViolation.setPolicyName(policyName);
      policyViolation.setPolicyId(createPolicyId(policyName));
      policyViolation.setApplicationId(policyEvaluation.getApplicationId());
      policyViolation.setStageTypeId(policyEvaluation.getStageTypeId());
      policyViolation.setOpenTime(policyEvaluation.getTime());
    }

    static TestablePolicyViolation createDefaultSecurityViolationForComponent(ComponentIdentifier componentIdentifier) {
      return createMinimalViolationForComponent(componentIdentifier)
          .withPolicyViolationId("conditionTypeViolation")
          .withThreatCategory(PolicyThreatCategory.SECURITY)
          .withThreatLevel(7)
          .withCveAndCvssScore("CVE-123", 7.5, 1);
    }

    static TestablePolicyViolation createDefaultLicenseViolationForComponent(ComponentIdentifier componentIdentifier) {
      return createMinimalViolationForComponent(componentIdentifier)
          .withPolicyViolationId("conditionTypeViolation")
          .withThreatCategory(PolicyThreatCategory.LICENSE)
          .withLicenseThreatGroup("GPL")
          .withThreatLevel(8);
    }

    static TestablePolicyViolation createMinimalViolationForComponent(ComponentIdentifier componentIdentifier) {
      return new TestablePolicyViolation(componentIdentifier, TEST_POLICY);
    }

    private long calculateExpectedFixTime() {
      return calculateTimeDifference(getOpenTime(), policyViolation.getFixTime());
    }

    private long calculateExpectedLegacyTime() {
      return calculateTimeDifference(getOpenTime(), policyViolation.getLegacyViolationTime());
    }

    private long calculateExpectedUnwaiveTime() {
      return calculateTimeDifference(getOpenTime(), policyViolation.getWaiveTime());
    }

    private long calculateTimeDifference(long startTime, Date endTime) {
      if (null == endTime) {
        endTime = policyEvaluation.getTime();
      }
      return endTime.getTime() - startTime;
    }

    List<Component> getAdditionalVersions() {
      return additionalVersions;
    }

    Component getComponent() {
      return component;
    }

    List<Component> getComponents() {
      return components;
    }

    String getConditionType() {
      return conditionType;
    }

    String getWaiverId() {
      return waiverId;
    }

    String getAutoPolicyWaiverId() {
      return autoPolicyWaiverId;
    }

    long getOpenTime() {
      return null != openTime ? openTime : policyEvaluation.getTime().getTime();
    }

    PolicyViolation getPolicyViolation() {
      return PolicyViolationTestUtils.copyPolicyViolation(policyViolation);
    }

    boolean isScmEnabled() {
      return isScmEnabled;
    }

    private long msForHours(int hours) {
      return 1000L * 60 * 60 * hours;
    }

    TestablePolicyViolation asDirectDependency(boolean isDirectDependency) {
      component.setDirectDependency(isDirectDependency);
      return this;
    }

    TestablePolicyViolation asInnerSourceDependency(boolean isInnerSource) {
      component.setInnerSourceData(isInnerSource ? Collections.singleton(new InnerSourceData()) : null);
      return this;
    }

    TestablePolicyViolation markFixedAsLegacy() {
      this.fixTime = policyEvaluation.getTime().getTime();
      return this;
    }

    TestablePolicyViolation markFixedByDowngrade() {
      this.fixTime = policyEvaluation.getTime().getTime();
      this.fixReason = COMPONENT_DOWNGRADE;
      return this;
    }

    TestablePolicyViolation markFixedByOtherMeans() {
      this.fixTime = policyEvaluation.getTime().getTime();
      return this;
    }

    TestablePolicyViolation markFixedByRemoval() {
      this.fixTime = policyEvaluation.getTime().getTime();
      component = null;
      this.components.clear();
      return this;
    }

    TestablePolicyViolation markFixedByUpgrade() {
      this.fixTime = policyEvaluation.getTime().getTime();
      this.fixReason = COMPONENT_UPGRADE;
      return this;
    }

    TestablePolicyViolation markUnwaived(String oldWaiverId, PolicyViolation replacementPolicyViolation) {
      this.expectUnwaived = true;
      this.waiverId = oldWaiverId;
      this.replacementPolicyViolation = replacementPolicyViolation;
      policyViolation.setPolicyWaiverId(oldWaiverId);
      policyViolation.setWaiveTime(policyEvaluation.getTime());
      withCount(-1);
      return this;
    }

    TestablePolicyViolation markWaived(PolicyWaiver policyWaiver) {
      policyViolation.setPolicyWaiverId(policyWaiver.getId());
      policyViolation.setWaiveTime(policyEvaluation.getTime());
      waiverId = policyWaiver.getId();
      waiverExpiration = "never";
      return this;
    }

    TestablePolicyViolation markUnAutoWaived(String oldAutoPolicyWaiverId, PolicyViolation replacementPolicyViolation) {
      this.expectUnwaived = true;
      this.autoPolicyWaiverId = oldAutoPolicyWaiverId;
      this.replacementPolicyViolation = replacementPolicyViolation;
      policyViolation.setWaiveTime(policyEvaluation.getTime());
      policyViolation.setAutoPolicyWaiverId(oldAutoPolicyWaiverId);
      withCount(-1);
      return this;
    }

    TestablePolicyViolation markAutoWaived(AutoPolicyWaiver autoPolicyWaiver) {
      policyViolation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());
      policyViolation.setWaiveTime(policyEvaluation.getTime());
      autoPolicyWaiverId = autoPolicyWaiver.getId();
      waiverExpiration = "never";
      return this;
    }

    TestablePolicyViolation openedHoursAgo(int hours) {
      this.openTime = policyEvaluation.getTime().getTime() - msForHours(hours);
      policyViolation.setOpenTime(new Date(openTime));
      return this;
    }

    TestablePolicyViolation withAdditionalComponentVersion(
        ComponentIdentifier componentIdentifier,
        boolean isDirect,
        boolean isInnerSource)
    {
      Component anotherVersion = new Component(componentIdentifier);
      anotherVersion.setDirectDependency(isDirect);
      anotherVersion.setInnerSourceData(isInnerSource ? Collections.singleton(new InnerSourceData()) : null);
      additionalVersions.add(anotherVersion);

      return this;
    }

    TestablePolicyViolation withConditionType(String conditionType) {
      this.conditionType = conditionType;
      return this;
    }

    TestablePolicyViolation withCount(int count) {
      this.count = count;
      return this;
    }

    TestablePolicyViolation withCveAndCvssScore(String cveIdentifier, double cvssScore, int index) {
      this.cveIdentifier = cveIdentifier;
      this.cvssScore = cvssScore;

      policyViolation.setConstraintFacts(
          ConditionGenerator.creatConstraintFactsWithInjectedSecurityCondition(cveIdentifier, cvssScore, index));
      return this;
    }

    TestablePolicyViolation withLicenseThreatGroup(String licenseThreatGroup) {
      this.licenseThreatGroup = licenseThreatGroup;
      policyViolation.setConstraintFacts(
          ConditionGenerator.createConstraintFactsWithInjectedLicenseCondition(licenseThreatGroup, 1));
      return this;
    }

    TestablePolicyViolation withPolicyViolationId(String policyViolationId) {
      policyViolation.setId(policyViolationId);
      return this;
    }

    TestablePolicyViolation withScmEnabled(boolean scmEnabled) {
      this.isScmEnabled = scmEnabled;
      return this;
    }

    TestablePolicyViolation withThreatLevel(int threatLevel) {
      policyViolation.setThreatLevel(threatLevel);
      return this;
    }

    TestablePolicyViolation withThreatCategory(PolicyThreatCategory threatCategory) {
      policyViolation.setThreatCategory(threatCategory);
      return this;
    }

    TestablePolicyViolation withPurlIdentifiersWithVulnerabilities(
        final PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities)
    {
      this.reachablePurlIdentifiersWithVulnerabilities = purlIdentifiersWithVulnerabilities;
      return this;
    }

    private String createPolicyId(String policyName) {
      return "ID_" + policyName;
    }

    void validateTelemetryDataForPurposes(List<TelemetryData> telemetryDataList, TelemetryPurpose... purposeCodes) {
      assertThat(telemetryDataList).hasSize(purposeCodes.length);
      for (int i = 0; i < telemetryDataList.size(); i++) {
        TelemetryData telemetryData = telemetryDataList.get(i);
        TelemetryPurpose purpose = purposeCodes[i];
        assertThat(telemetryData.getPurpose()).isEqualTo(purpose);

        validateCommonAttributes(telemetryData);
        validateComponentInfo(telemetryData.getAttributes());
        validateDependencyInfo(telemetryData.getAttributes());
        validatePurposeSpecificAttributes(telemetryData.getAttributes(), purpose);
      }
    }

    void validateCommonAttributes(TelemetryData telemetryData) {
      Map<String, Object> attributes = telemetryData.getAttributes();

      // the application ID is obfuscated
      assertThat(attributes).doesNotContainEntry(APPLICATION_ID, policyEvaluation.getApplicationId());

      assertThat(attributes).containsEntry(COUNT, count);
      validateMatchesOrNotExists(attributes, PolicyViolationTelemetryBuilder.CVE_NUMBER, cveIdentifier);
      validateMatchesOrNotExists(attributes, PolicyViolationTelemetryBuilder.CVSS_SCORE, cvssScore);
      validateMatchesOrNotExists(attributes, PolicyViolationTelemetryBuilder.LICENSE_THREAT_GROUP_ATTRIBUTE,
          licenseThreatGroup);
      assertThat(attributes).containsEntry(IS_SCM_ENABLED, isScmEnabled);
      assertThat(attributes).containsEntry(OPEN_TIME, getOpenTime());
      assertThat(attributes).containsEntry(POLICY_VIOLATION_ID, policyViolation.getId());
      assertThat(attributes).containsEntry(REAL_APPLICATION_ID, policyEvaluation.getApplicationId());
      assertThat(attributes).containsEntry(STAGE, policyEvaluation.getStageTypeId());
      assertThat(attributes).containsEntry(THREAT_LEVEL, policyViolation.getThreatLevel());
      assertThat(attributes).containsEntry(THREAT_CATEGORY, policyViolation.getThreatCategory().getName());

      ReachabilityStatus reachabilityStatus = policyViolation.getReachabilityStatus();
      assertThat(attributes)
          .containsEntry(REACHABILITY_STATUS, reachabilityStatus == null ? null : reachabilityStatus.getName());
    }

    void validateComponentInfo(Map<String, Object> attributes) {
      assertThat(attributes).containsEntry(ECOSYSTEM, policyViolation.getComponentIdentifier().getFormat());
      assertThat(attributes).containsEntry(COMPONENT_IDENTIFIER, policyViolation.getComponentIdentifier().toString());

      PackageUrlIdentifier packageUrlIdentifier =
          PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier());

      assertThat(attributes).containsEntry(COMPONENT_NAMESPACE, packageUrlIdentifier.getNamespace());
      assertThat(attributes).containsEntry(COMPONENT_NAME, packageUrlIdentifier.getName());
      assertThat(attributes).containsEntry(COMPONENT_VERSION, packageUrlIdentifier.getVersion());
    }

    void validateDependencyInfo(Map<String, Object> attributes) {
      if (null != component) {
        validateMatchesOrNotExists(attributes, DIRECT_DEPENDENCY, component.getDirectDependency());
        assertThat(attributes).containsEntry(INNERSOURCE_DEPENDENCY, component.getInnerSourceData() != null);
      }
    }

    void validateMatchesOrNotExists(Map<String, Object> attributes, String key, Object value) {
      if (null != value) {
        assertThat(attributes).containsEntry(key, value);
      }
      else {
        assertThat(attributes).doesNotContainKey(key);
      }
    }

    void validatePurposeSpecificAttributes(Map<String, Object> attributes, TelemetryPurpose purpose) {
      switch (purpose) {
        case CONDITION_TYPE_VIOLATION:
          validateMatchesOrNotExists(attributes, CONDITION_TYPE, conditionType);
          validateTimeAttribute(attributes, 0L);
          break;

        case TIME_TO_CHANGE_VERSION_POLICY_VIOLATION:
          validateMatchesOrNotExists(attributes, FIX_TIME, fixTime);
          validateMatchesOrNotExists(attributes, FIX_BY_VERSION_CHANGE, fixReason);
          validateTimeAttribute(attributes, calculateExpectedFixTime());
          break;

        case TIME_TO_LEGACY_VIOLATION:
          validateMatchesOrNotExists(attributes, LEGACY_VIOLATION_TIME, fixTime);
          validateTimeAttribute(attributes, calculateExpectedLegacyTime());
          break;

        case TIME_TO_REMEDIATE_POLICY_VIOLATION:
          validateMatchesOrNotExists(attributes, FIX_TIME, fixTime);
          validateTimeAttribute(attributes, calculateExpectedFixTime());
          break;

        case TIME_TO_WAIVE_POLICY_VIOLATION:
          validateMatchesOrNotExists(attributes, WAIVE_TIME, policyViolation.getWaiveTime().getTime());
          validateMatchesOrNotExists(attributes, POLICY_WAIVER_ID, getWaiverId());
          validateMatchesOrNotExists(attributes, AUTO_POLICY_WAIVER_ID, getAutoPolicyWaiverId());
          validateMatchesOrNotExists(attributes, WAIVER_EXPIRATION, waiverExpiration);
          if (expectUnwaived) {
            validateMatchesOrNotExists(attributes, UNWAIVE_TIME, policyViolation.getWaiveTime().getTime());
            validateMatchesOrNotExists(attributes, NEW_POLICY_VIOLATION_ID, replacementPolicyViolation.getId());
          }
          validateTimeAttribute(attributes, calculateExpectedUnwaiveTime());
          break;

        case CALLFLOW_EVALUATION_COMPONENT_COUNTS:
          boolean evaluationSuccessful = reachablePurlIdentifiersWithVulnerabilities != null;
          validateMatchesOrNotExists(attributes, CALL_FLOW_EVALUATION_SUCCESSFUL, evaluationSuccessful);
          validateMatchesOrNotExists(attributes, CALL_FLOW_HAS_REACHABLE_INFORMATION_FOR_COMPONENT,
              hasPolicyViolationByComponentIdentifier(policyViolation, reachablePurlIdentifiersWithVulnerabilities));
          break;

        default:
          throw new IllegalArgumentException("Unexpected purpose: " + purpose);
      }
    }

    void validateTimeAttribute(Map<String, Object> attributes, long expectedTime) {
      assertThat(attributes).containsEntry(TIME, expectedTime);
    }
  }

  /**
   * Helper class to generate the constraint facts with the condition facts needed for the tests.
   */
  private class ConditionGenerator
  {
    static List<ConstraintFact> createConstraintFactsWithInjectedLicenseCondition(
        String licenseThreatGroup,
        int cvIteration)
    {
      return createConstraintFactsWithInjectedCondition(licenseThreatGroup, null, null, cvIteration);
    }

    static List<ConstraintFact> creatConstraintFactsWithInjectedSecurityCondition(
        String cveNumber,
        Double cvssScore,
        int cvIteration)
    {
      return createConstraintFactsWithInjectedCondition(null, cveNumber, cvssScore, cvIteration);
    }

    /**
     * A useful constraint fact must have at least 1 condition facts because that's where
     * the test data is. Several are created because in real word scenarios there may be multiple constraint facts
     * with one condition fact nested with needed data.
     * The number of constraint and condition facts is fixed.
     * Only one condition fact with the cv metadata is instantiated in the whole list of
     * constraint facts, it is possible to choose where.
     * Hardcoded values are not important for these tests.
     *
     * @param cveNumber   CVE id to inject in the condition fact
     * @param cvssScore   Score to inject in the condition fact
     * @param licenseThreatGroup use for license thread conditions;  mutually exclusive with the cve params above
     * @param cvIteration Iteration you want to insert the cv metadata
     * @return A list of constraint fact with the same amount of condition fact that contain the cv metadata
     */
    private static List<ConstraintFact> createConstraintFactsWithInjectedCondition(
        String licenseThreatGroup,
        String cveNumber,
        Double cvssScore,
        int cvIteration)
    {
      List<ConstraintFact> constraintFacts = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        ConstraintFact constraintFact =
            new ConstraintFact("constraintId" + i, "constraintName" + i, "operatorName" + i);
        constraintFacts.add(constraintFact);
        for (int j = 0; j < 3; j++) {
          ConditionFact conditionFact;

          if (cveNumber != null && cvssScore != null && cvIteration == i && cvIteration == j) {
            conditionFact = createConditionFactWithCVMetadata(j, cveNumber, cvssScore);
          }
          else if (StringUtils.isNotBlank(licenseThreatGroup)  && cvIteration == i && cvIteration == j) {
            conditionFact = createConditionFactForLicenseThreat(j, licenseThreatGroup);
          }
          else {
            conditionFact = createConditionFact(j, SecurityVulnerabilitySeverityConditionType.ID);
          }

          constraintFact.addConditionFact(conditionFact);
        }
      }
      return constraintFacts;
    }

    private static ConditionFact createConditionFactWithCVMetadata(int j, String cveNumber, double cvssScore) {
      TriggerReference triggerReference =
          new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID, cveNumber);
      String triggerJson =
          String.format("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"%s\",\"severity\":%f}}", cveNumber, cvssScore);
      ConditionFact conditionFact =
          new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, j, "summary", "reason", triggerReference);
      conditionFact.setTriggerJson(triggerJson);
      return conditionFact;
    }

    private static ConditionFact createConditionFactForLicenseThreat(int j, String licenseThreatGroup) {
      var triggerJson = String.format("{\"conditionIndex\":0,\"trigger\":{\"id\":\"%s\"}}", "threatGroupId");
      var summary = String.format("License Threat Group is '%s'", licenseThreatGroup);
      ConditionFact conditionFact =
          new ConditionFact(LicenseThreatGroupConditionType.ID, j, summary, "reason", null);
      conditionFact.setTriggerJson(triggerJson);
      return conditionFact;
    }

    private static ConditionFact createConditionFact(int j, String conditionType) {
      return new ConditionFact(conditionType, j, "summary", "reason");
    }
  }

  /**
   * Helper class used to isolate the policy violation data to ensure that the telemetry collector can't
   * inadvertently modify the reference data.
   */
  private class PolicyViolationTestUtils
  {
    static PolicyViolation copyPolicyViolation(PolicyViolation original) {
      PolicyViolation copy = new PolicyViolation();

      copy.setApplicationId(original.getApplicationId());
      copy.setAutoPolicyWaiverId(original.getAutoPolicyWaiverId());
      copy.setComponentIdentifier(original.getComponentIdentifier());
      copy.setConstraintFacts(null != original.getConstraintFacts() ?
          new ArrayList<>(original.getConstraintFacts()) : new ArrayList<>());
      copy.setFilename(original.getFilename());
      copy.setFixTime(original.getFixTime());
      copy.setHash(original.getHash());
      copy.setId(original.getId());
      copy.setLegacyViolationApplied(original.isLegacyViolationApplied());
      copy.setLegacyViolationTime(original.getLegacyViolationTime());
      copy.setOpenTime(original.getOpenTime());
      copy.setPolicyId(original.getPolicyId());
      copy.setPolicyName(original.getPolicyName());
      copy.setPolicyWaiverId(original.getPolicyWaiverId());
      copy.setAutoPolicyWaiverId(original.getAutoPolicyWaiverId());
      copy.setReachabilityStatus(original.getReachabilityStatus());
      copy.setSeenByMonitoringEvaluation(original.isSeenByMonitoringEvaluation());
      copy.setSeenByPrimaryEvaluation(original.isSeenByPrimaryEvaluation());
      copy.setStageTypeId(original.getStageTypeId());
      copy.setThreatCategory(original.getThreatCategory());
      copy.setThreatLevel(original.getThreatLevel());
      copy.setWaiveTime(original.getWaiveTime());
      copy.setReachabilityStatus(original.getReachabilityStatus());

      return copy;
    }
  }
}
