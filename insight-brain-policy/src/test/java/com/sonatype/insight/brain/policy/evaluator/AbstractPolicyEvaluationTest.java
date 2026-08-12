/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.collect.ImmutableMap;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ConditionTypesTestHelper;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRule;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.DroolsGenerator;
import com.sonatype.insight.brain.utils.FirewallForContainerImagesHelper;
import com.sonatype.insight.brain.validation.DefaultSourceControlSshValidator;
import com.sonatype.insight.brain.validation.SourceControlSshValidator;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.lqa.LqaFormat;
import com.sonatype.insight.test.SpringInjectedTest;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = AbstractPolicyEvaluationTest.PolicyEvaluationTestConfiguration.class)
public abstract class AbstractPolicyEvaluationTest
    extends SpringInjectedTest
{
  @Rule(order = 1)
  public DatabaseRule databaseRule = DatabaseRule.getInstance(AbstractPolicyEvaluationTest.class);

  protected DAOFactory daoFactory;

  @Rule(order = 2)
  public TemporaryEntity tempEntity = new TemporaryEntity(databaseRule);

  protected ComponentPolicyEvaluator componentPolicyEvaluator;

  protected LabelDAO labelDAO;

  @Before
  public void setUp() throws Exception {
    daoFactory = new TestDAOFactory(databaseRule);

    SystemConfigurationPropertyFeature.injectDependencies(daoFactory.createSystemConfigurationPropertyDAO());

    // Re-inject classes that have static dependencies
    ConditionTypesTestHelper.initConditionTypes(daoFactory);
    ConditionTypesTestHelper.initConditionValueTypes(daoFactory);

    // Manually create policy evaluation components (they depend on initialized data stores)
    FirewallForContainerImagesHelper firewallHelper = new FirewallForContainerImagesHelper(
        daoFactory.createOrganizationDAO(),
        daoFactory.createRepositoryDAO(),
        daoFactory.createOwnerDAO());
    componentPolicyEvaluator = new ComponentPolicyEvaluator(
        daoFactory.createPolicyWaiverDAO(),
        daoFactory.createOwnerDAO(),
        daoFactory.createPolicyDAO(),
        firewallHelper);
    labelDAO = daoFactory.createLabelDAO();
  }

  /**
   * Test configuration that provides minimal beans for policy evaluation tests.
   * Note: ComponentPolicyEvaluator and LabelDAO are created manually in setUp()
   * because they depend on DatabaseRule data stores which are initialized by JUnit @Rule.
   */
  @TestConfiguration
  static class PolicyEvaluationTestConfiguration
  {
    @Bean
    @Singleton
    @Named
    public SourceControlSshValidator sourceControlSshValidator() {
      return new DefaultSourceControlSshValidator();
    }
  }

  protected List<PolicyAlert> evaluate(Policy policy, List<Component> components) {
    return evaluate(new Stage(BuildStageType.ID), policy, components);
  }

  protected List<PolicyAlert> evaluate(Stage stage, Policy policy, List<Component> components) {
    DroolsGenerator.generate(policy, labelDAO);
    return componentPolicyEvaluator.evaluate(null /* applicationId */, stage, Collections.singletonList(policy),
        components).getActiveAlerts();
  }

  protected Constraint createConstraint(
      String constraintId,
      String constraintName,
      String conditionTypeId,
      String operator,
      String value)
  {
    Condition condition = new Condition(conditionTypeId, operator, value);
    Constraint constraint = new Constraint(constraintId, constraintName, LogicalOperator.AND);
    constraint.addCondition(condition);
    return constraint;
  }

  public static void assertFactCounts(
      int expectedConstraintFactCount,
      int expectedComponentFactCount,
      PolicyAlert actualPolicyAlert)
  {
    List<ComponentFact> componentFacts = actualPolicyAlert.getTrigger().getComponentFacts();
    assertThat(componentFacts).hasSize(expectedComponentFactCount);

    int actualConstraintFactCount = 0;
    Set<String> observeredConstraints = new HashSet<>();
    for (ComponentFact componentFact : componentFacts) {
      for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
        if (observeredConstraints.add(constraintFact.getConstraintId())) {
          actualConstraintFactCount++;
        }
      }
    }
    assertThat(actualConstraintFactCount).as("Incorrect number of constraint facts")
        .isEqualTo(expectedConstraintFactCount);
  }

  private static List<ConditionFact> findConditionFactsInPolicyAlerts(
      Component expectedComponent,
      Policy expectedPolicy,
      Constraint expectedConstraint,
      String expectedActionTypeId,
      String expectedConditionTypeId,
      List<PolicyAlert> actual)
  {
    List<ConditionFact> result = new ArrayList<>();

    for (PolicyAlert actualPolicyAlert : actual) {
      PolicyFact policyFact = actualPolicyAlert.getTrigger();
      if (expectedPolicy.getId().equals(policyFact.getPolicyId())
          && expectedPolicy.getName().equals(policyFact.getPolicyName())
          && policyAlertContainsAction(actualPolicyAlert, expectedActionTypeId))
      {
        for (ComponentFact componentFact : policyFact.getComponentFacts()) {
          if (Objects.equals(expectedComponent.getComponentIdentifier(), componentFact.getComponentIdentifier())
              && Objects.equals(expectedComponent.getHash(), componentFact.getHash()))
          {
            for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
              if (expectedConstraint.getId().equals(constraintFact.getConstraintId())
                  && expectedConstraint.getName().equals(constraintFact.getConstraintName()))
              {
                for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
                  if (expectedConditionTypeId.equals(conditionFact.getConditionTypeId())) {
                    result.add(conditionFact);
                  }
                }
              }
            }
          }
        }
      }
    }

    return result;
  }

  public static List<ConditionFact> assertContainsPolicyAlert(
      Component expectedComponent,
      Policy expectedPolicy,
      Constraint expectedConstraint,
      String expectedActionTypeId,
      String expectedConditionTypeId,
      List<PolicyAlert> actual)
  {
    List<ConditionFact> conditionFacts = findConditionFactsInPolicyAlerts(expectedComponent, expectedPolicy,
        expectedConstraint, expectedActionTypeId, expectedConditionTypeId, actual);
    if (conditionFacts.isEmpty()) {
      fail("Cannot find expected policy alert in:" + toString(actual));
    }

    return conditionFacts;
  }

  public static List<ConditionFact> assertContainsPolicyAlert(
      Component expectedComponent,
      Policy expectedPolicy,
      Constraint expectedConstraint,
      String expectedActionTypeId,
      String expectedConditionTypeId,
      ConditionTrigger expectedConditionTrigger,
      List<PolicyAlert> actual)
  {
    List<ConditionFact> conditionFacts = findConditionFactsInPolicyAlerts(expectedComponent, expectedPolicy,
        expectedConstraint, expectedActionTypeId, expectedConditionTypeId, actual);
    if (conditionFacts.isEmpty()) {
      fail("Cannot find expected policy alert in:" + toString(actual));
    }

    for (ConditionFact conditionFact : conditionFacts) {
      if (conditionFact.getTriggerJson().equals(JsonUtils.writeUnformatted(expectedConditionTrigger))) {
        return conditionFacts;
      }
    }
    fail("Cannot find expected policy alert with condition trigger in:" + toString(actual));
    return null; // unreachable, only needed to avoid warnings
  }

  public static void assertNotContainsPolicyAlert(
      Component expectedComponent,
      Policy expectedPolicy,
      Constraint expectedConstraint,
      String expectedActionTypeId,
      String expectedConditionTypeId,
      List<PolicyAlert> actual)
  {
    List<ConditionFact> conditionFacts = findConditionFactsInPolicyAlerts(expectedComponent, expectedPolicy,
        expectedConstraint, expectedActionTypeId, expectedConditionTypeId, actual);
    if (!conditionFacts.isEmpty()) {
      fail("Found unexpected policy alert in:" + toString(actual));
    }
  }

  private static String toString(List<PolicyAlert> policyAlerts) {
    StringBuilder result = new StringBuilder();
    for (PolicyAlert policyAlert : policyAlerts) {
      result.append(policyAlert.getTrigger().toString());
    }
    return result.toString();
  }

  private static boolean policyAlertContainsAction(PolicyAlert actualPolicyAlert, String actionTypeId) {
    for (Action action : actualPolicyAlert.getActions()) {
      if (actionTypeId.equals(action.getActionTypeId())) {
        return true;
      }
    }
    return false;
  }

  public static Component forCoordinatesPackageUrl(String format, String... coord) {
    ComponentIdentifier componentIdentifier;
    switch (format) {
      case ComponentIdentifier.FORMAT_MAVEN:
        if (coord.length == 5) {
          // this method takes maven coordinates in the order GAVCE, but we have them as GAVEC, so swap the last two
          componentIdentifier =
              ComponentIdentifier.createMavenCoordinates(coord[0], coord[1], coord[2], coord[4], coord[3]);
        }
        else {
          componentIdentifier = ComponentIdentifier.createMavenCoordinates(coord[0], coord[1], coord[2]);
        }
        break;
      case ComponentIdentifier.FORMAT_ANAME:
        componentIdentifier = ComponentIdentifier.createAnameCoordinates(coord[1], coord[4], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_PYPI:
        componentIdentifier = ComponentIdentifier.createPypiCoordinates(coord[1], coord[2], coord[4], coord[3]);
        break;
      case ComponentIdentifier.FORMAT_GOLANG:
        componentIdentifier = ComponentIdentifier.createGolangCoordinates(coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_NPM:
        componentIdentifier = ComponentIdentifier.createNpmCoordinates(coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_NUGET:
        componentIdentifier = ComponentIdentifier.createNugetCoordinates(coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_RPM:
        componentIdentifier = ComponentIdentifier.createRpmCoordinates(coord[1], coord[2], coord[4]);
        break;
      case ComponentIdentifier.FORMAT_RUBYGEMS:
        componentIdentifier = ComponentIdentifier.createRubyGemsCoordinates(coord[1], coord[2], coord[4]);
        break;
      case ComponentIdentifier.FORMAT_SWIFT:
        componentIdentifier = ComponentIdentifier.createSwiftCoordinates(coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_COCOAPODS:
        componentIdentifier = ComponentIdentifier.createCocoapodsCoordinates(coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_PECOFF:
        componentIdentifier = ComponentIdentifier.createPecoffCoordinates(coord[0], coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_TERRAFORM:
        componentIdentifier = ComponentIdentifier.createTerraformCoordinates(coord[0], coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_CONTAINER:
        componentIdentifier = ComponentIdentifier.createContainerCoordinates(coord[0], coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_CONAN:
        componentIdentifier = ComponentIdentifier.createConanCoordinates(coord[1], coord[2], coord[0], coord[3]);
        break;
      case ComponentIdentifier.FORMAT_CARGO:
        componentIdentifier = ComponentIdentifier.createCargoCoordinates(coord[1], coord[2], coord[3]);
        break;
      case ComponentIdentifier.FORMAT_CRAN:
        componentIdentifier = ComponentIdentifier.createCranCoordinates(coord[1], coord[2], coord[3]);
        break;
      case ComponentIdentifier.FORMAT_CONDA:
        componentIdentifier = createCondaIdentifier(coord);
        break;
      case ComponentIdentifier.FORMAT_COMPOSER:
        componentIdentifier = ComponentIdentifier.createComposerCoordinates(coord[0], coord[1], coord[2]);
        break;
      default:
        componentIdentifier = createLqaComponentIdentifier(format, coord);
    }
    Component component = new Component(componentIdentifier);
    component.setMatchState(MatchState.EXACT);
    return component;
  }

  private static ComponentIdentifier createCondaIdentifier(String... coord) {
    String[] classifierData = coord[4].split("=");
    return ComponentIdentifier.createCondaCoordinates(coord[1], coord[2], coord[0], classifierData[0],
        null, coord[3]);
  }

  private static ComponentIdentifier createLqaComponentIdentifier(String format, String... coord) {
    LqaFormat lqaFormat = LqaFormat.getByLqaFormat(format);
    if (lqaFormat != null) {
      Map<String, String> coords;
      if (lqaFormat == LqaFormat.DEBIAN) {
        coords = ImmutableMap.of("namespace", coord[0], "name", coord[1], "version", coord[2]);
        return new ComponentIdentifier(format, coords);
      }
      return createGenericComponentIdentifier(format, coord);
    }
    return createGenericComponentIdentifier(format, coord);
  }

  private static ComponentIdentifier createGenericComponentIdentifier(String format, String... coord) {
    Map<String, String> coordinates = new LinkedHashMap<>();
    coordinates.put("namespace", coord[0]);
    coordinates.put("name", coord[1]);
    coordinates.put("version", coord[2]);
    coordinates.put("type", coord[4]);
    coordinates.put("qualifier", coord[4]);
    return new ComponentIdentifier(format, coordinates);
  }
}
