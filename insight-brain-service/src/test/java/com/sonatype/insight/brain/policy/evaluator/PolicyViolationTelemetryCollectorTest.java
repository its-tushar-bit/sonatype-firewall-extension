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

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiSuggestedVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.*;
import static com.sonatype.insight.brain.telemetry.TelemetryUtils.REAL_APPLICATION_ID;
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
        TestablePolicyViolation.createDefaultViolationForComponent(commonsLang3)
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
        TestablePolicyViolation.createDefaultViolationForComponent(lodashv4)
            .openedHoursAgo(48)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByDowngrade")
            .markFixedByDowngrade();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in downgraded component
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        createWrappedComponent(lodashv3, true, false),
        null
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
        TestablePolicyViolation.createDefaultViolationForComponent(lodashv4)
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
        testablePolicyViolation.getAdditionalVersions(),
        null
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByRemoval() {
    // given a policy violation on lodash v4 that was fixed by downgrading
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultViolationForComponent(lodashv4)
            .openedHoursAgo(37)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByRemoval")
            .markFixedByRemoval();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in empty component list
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        null
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade() {
    doTestAddTelemetryForFixedViolation_FixedByUpgrade_AndRemediation(null);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade_GoldenPR() {
    doTestAddTelemetryForFixedViolation_FixedByUpgrade_AndRemediation(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade_Recommended() {
    doTestAddTelemetryForFixedViolation_FixedByUpgrade_AndRemediation(
        ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade_NextNonFailing() {
    doTestAddTelemetryForFixedViolation_FixedByUpgrade_AndRemediation(
        ApiVersionChangeOptionType.NEXT_NON_FAILING);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade_NextNonFailingWithDependencies() {
    doTestAddTelemetryForFixedViolation_FixedByUpgrade_AndRemediation(
        ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade_NextNoViolations() {
    doTestAddTelemetryForFixedViolation_FixedByUpgrade_AndRemediation(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedByUpgrade_NextNoViolationsWithDependencies() {
    doTestAddTelemetryForFixedViolation_FixedByUpgrade_AndRemediation(
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
  }

  private void doTestAddTelemetryForFixedViolation_FixedByUpgrade_AndRemediation(
      ApiVersionChangeOptionType remediationType)
  {
    Component wrongComponent = new Component(lodashv4);
    Component expectedComponent = new Component(lodashv5);

    // given a policy violation on lodash v4 that was fixed by downgrading
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultViolationForComponent(lodashv4)
            .withScmEnabled(true)
            .openedHoursAgo(72)
            .asDirectDependency(true)
            .withPolicyViolationId("fixedByUpgrade")
            .markFixedByUpgrade();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in an upgraded component
    ApiComponentRemediationValueDTO
        remediationObject = createRemediationObject(expectedComponent, wrongComponent, remediationType);
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        createWrappedComponent(lodashv5, true, false),
        remediationObject
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(
        telemetryData,
        TIME_TO_REMEDIATE_POLICY_VIOLATION,
        TIME_TO_CHANGE_VERSION_POLICY_VIOLATION
    );
    testablePolicyViolation.validateRemediationTelemetry(telemetryData, remediationType);
  }

  private ApiComponentRemediationValueDTO createRemediationObject(
      Component expectedComponent,
      Component wrongComponent,
      final ApiVersionChangeOptionType remediationType)
  {
    // Generate a remediation object
    ApiComponentRemediationValueDTO remediation = null; // = new ApiComponentRemediationValueDTO();
    if (remediationType != null) {
      remediation = new ApiComponentRemediationValueDTO();
      for (ApiVersionChangeOptionType rType : ApiVersionChangeOptionType.values()) {
        // If the remediation type is the expected one, use the expected component, otherwise use the wrong one
        // which has a different version that will not match when the telemetry is generated.
        Component useComponent = remediationType == rType ? expectedComponent : wrongComponent;
        switch (rType) {
          case RECOMMENDED_NON_BREAKING:
            remediation.suggestedVersionChange =
                new ApiSuggestedVersionChangeOptionDTO(rType, false, buildApiChangeAction(useComponent));
            break;
          case RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES:
            // This is the last option, so check if it is the expected one. Otherwise, the suggested might already
            // be set to the expected one.
            if (rType.equals(remediationType)) {
              remediation.suggestedVersionChange =
                  new ApiSuggestedVersionChangeOptionDTO(rType, true, buildApiChangeAction(useComponent));
            }
            break;
          default:
            remediation.versionChanges.add(new ApiVersionChangeOptionDTO(rType, buildApiChangeAction(useComponent)));
            break;
        }
      }
    }
    return remediation;
  }

  private ApiComponentChangeActionDTO buildApiChangeAction(final Component component) {
    ApiComponentChangeActionDTO action = new ApiComponentChangeActionDTO();
    ApiComponentDTOV2 apiComponent = new ApiComponentDTOV2();
    ApiComponentIdentifierDTOV2 apiComponentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(component.getComponentIdentifier());
    apiComponent.componentIdentifier = apiComponentIdentifier;
    action.setComponent(apiComponent);
    return action;
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedWithoutVersionChange() {
    // given a policy violation on lodash v5
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultViolationForComponent(lodashv5)
            .openedHoursAgo(480)
            .asDirectDependency(false)
            .withPolicyViolationId("fixedWithoutVersionChange")
            .markFixedByOtherMeans();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        null
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForFixedViolation_FixedWithoutVersionChange_licenseData() {
    // given a policy violation on lodash v5
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createMinimalViolationForComponent(lodashv5)
            .openedHoursAgo(480)
            .withScmEnabled(false)
            .withThreatCategory(PolicyThreatCategory.LICENSE)
            .withThreatLevel(10)
            .withConditionType(LicenseConditionType.ID)
            .asDirectDependency(false)
            .withPolicyViolationId("fixedWithoutVersionChange_license")
            .markFixedByOtherMeans();

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForFixedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponents(),
        null
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_REMEDIATE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForLegacyViolation() {
    // given a policy violation on lodash v3
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultViolationForComponent(lodashv3)
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
    // given a policy violation on lodash v3
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultViolationForComponent(urllib3)
            .openedHoursAgo(500)
            .asDirectDependency(true)
            .withPolicyViolationId("unwaivedViolation")
            .markUnwaived("oldWaiverIdForUrllib3");

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForUnwaivedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent(),
        testablePolicyViolation.getWaiverId()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_WAIVE_POLICY_VIOLATION);
  }

  @Test
  public void testAddTelemetryForWaivedViolation() {
    // given a policy violation and a waiver for it
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver(tempEntity.newPolicy().getId(), policyEvaluation.getApplicationId());

    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultViolationForComponent(commonsLang3)
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
        TestablePolicyViolation.createDefaultViolationForComponent(commonsLang3)
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
  public void testAddTelemetryForUnautoWaivedViolation() {
    // given a policy violation on lodash v3
    TestablePolicyViolation testablePolicyViolation =
        TestablePolicyViolation.createDefaultViolationForComponent(urllib3)
            .openedHoursAgo(500)
            .asDirectDependency(true)
            .withPolicyViolationId("unwaivedViolation")
            .markUnAutoWaived("oldAutoPolicyWaiverId");

    PolicyViolationTelemetryCollector telemetryCollector =
        createTelemetryCollector(testablePolicyViolation.isScmEnabled());

    // when - pass in same component version the violation is for
    telemetryCollector.addTelemetryForUnAutoWaivedViolation(
        testablePolicyViolation.getPolicyViolation(),
        testablePolicyViolation.getComponent(),
        testablePolicyViolation.getAutoPolicyWaiverId()
    );

    // then
    List<TelemetryData> telemetryData = telemetryCollector.getTelemetryData();
    testablePolicyViolation.validateTelemetryDataForPurposes(telemetryData, TIME_TO_WAIVE_POLICY_VIOLATION);
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

    private Component component;

    private String conditionType;

    private int count = 1;

    private String cveIdentifier;

    private Double cvssScore;

    private String fixReason;

    private Long fixTime;

    private boolean isScmEnabled;

    private String waiverId;
    
    private String autoPolicyWaiverId;

    private Long openTime;

    private String waiverExpiration;

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

    static TestablePolicyViolation createDefaultViolationForComponent(ComponentIdentifier componentIdentifier) {
      return createMinimalViolationForComponent(componentIdentifier)
          .withPolicyViolationId("conditionTypeViolation")
          .withThreatCategory(PolicyThreatCategory.SECURITY)
          .withThreatLevel(7)
          .withCveAndCvssScore("CVE-123", 7.5, 1);
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

    TestablePolicyViolation markUnwaived(String oldWaiverId) {
      this.waiverId = oldWaiverId;
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
    
    TestablePolicyViolation markUnAutoWaived(String oldAutoPolicyWaiverId) {
      this.autoPolicyWaiverId = oldAutoPolicyWaiverId;
      policyViolation.setWaiveTime(policyEvaluation.getTime());
      withCount(-1);
      return this;
    }
    
    TestablePolicyViolation markAutoWaived(AutoPolicyWaiver autoPolicyWaiver) {
      policyViolation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());
      policyViolation.setWaiveTime(policyEvaluation.getTime());
      autoPolicyWaiverId = autoPolicyWaiver.getId();
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
          ConditionGenerator.createConstraintFactsWithInjectedCondition(cveIdentifier, cvssScore, index));
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

    private String createPolicyId(String policyName) {
      return "ID_" + policyName;
    }

    /**
     * Ensure the remediation data is appropriately added to the telemetry. Other fields are already tested.
     */
    public void validateRemediationTelemetry(
        List<TelemetryData> telemetryDataList,
        final ApiVersionChangeOptionType remediationType)
    {
      for (int i = 0; i < telemetryDataList.size(); i++) {
        TelemetryData telemetryData = telemetryDataList.get(i);
        if (TIME_TO_CHANGE_VERSION_POLICY_VIOLATION.equals(telemetryData.getPurpose())) {
          Map<String, Object> attributes = telemetryData.getAttributes();
          if (null != remediationType) {
            assertThat(attributes).containsEntry(REMEDIATION_TYPE, remediationType.getNameForTelemetry());
          }
          else {
            assertThat(attributes).doesNotContainKey(REMEDIATION_TYPE);
          }
        }
      }
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
      validateMatchesOrNotExists(attributes, CVE_NUMBER, cveIdentifier);
      validateMatchesOrNotExists(attributes, CVSS_SCORE, cvssScore);
      assertThat(attributes).containsEntry(IS_SCM_ENABLED, isScmEnabled);
      assertThat(attributes).containsEntry(OPEN_TIME, getOpenTime());
      assertThat(attributes).containsEntry(POLICY_VIOLATION_ID, policyViolation.getId());
      assertThat(attributes).containsEntry(REAL_APPLICATION_ID, policyEvaluation.getApplicationId());
      assertThat(attributes).containsEntry(STAGE, policyEvaluation.getStageTypeId());
      assertThat(attributes).containsEntry(THREAT_LEVEL, policyViolation.getThreatLevel());
      assertThat(attributes).containsEntry(THREAT_CATEGORY, policyViolation.getThreatCategory().getName());
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
          validateMatchesOrNotExists(attributes, UNWAIVE_TIME, policyViolation.getWaiveTime().getTime());
          validateMatchesOrNotExists(attributes, WAIVE_TIME, policyViolation.getWaiveTime().getTime());
          validateMatchesOrNotExists(attributes, POLICY_WAIVER_ID, getWaiverId());
          validateMatchesOrNotExists(attributes, AUTO_POLICY_WAIVER_ID, getAutoPolicyWaiverId());
          validateMatchesOrNotExists(attributes, WAIVER_EXPIRATION, waiverExpiration);
          validateTimeAttribute(attributes, calculateExpectedUnwaiveTime());
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
     * @param cvIteration Iteration you want to insert the cv metadata
     * @return A list of constraint fact with the same amount of condition fact that contain the cv metadata
     */
    static List<ConstraintFact> createConstraintFactsWithInjectedCondition(
        String cveNumber,
        double cvssScore,
        int cvIteration)
    {
      List<ConstraintFact> constraintFacts = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        ConstraintFact constraintFact =
            new ConstraintFact("constraintId" + i, "constraintName" + i, "operatorName" + i);
        constraintFacts.add(constraintFact);
        for (int j = 0; j < 3; j++) {
          ConditionFact conditionFact;

          if (cvIteration == i && cvIteration == j) {
            conditionFact = createConditionFactWithCVMetadata(j, cveNumber, cvssScore);
          }
          else {
            conditionFact = createConditionFact(j);
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
          String.format("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"CVE-2013-7285\",\"severity\":%f}}", cvssScore);
      ConditionFact conditionFact =
          new ConditionFact(LicenseConditionType.ID, j, "summary", "reason", triggerReference);
      conditionFact.setTriggerJson(triggerJson);
      return conditionFact;
    }

    private static ConditionFact createConditionFact(int j) {
      return new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, j, "summary", "reason");
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

      return copy;
    }
  }
}
