/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.ApplicationComponentRisk;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link OwnerComponentDAOTest} (CLM-45228). The H2/unit coverage stays
 * in that origin class; the {@code @PostgresTest} coverage lives here so this module keeps a single (Postgres)
 * DatabaseRule fixture type per JVM.
 */
@PostgresTest
public class OwnerComponentDAOPgTest
    extends AbstractDbDAOTest
{
  private OwnerComponentDAO dao;

  private AggregateFileDAO aggregateFileDAO;

  private OwnerComponentLicenseDAO applicationComponentLicenseDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createOwnerComponentDAO();
    aggregateFileDAO = daoFactory.createAggregateFileDAO();
    applicationComponentLicenseDAO = daoFactory.createOwnerComponentLicenseDAO();
  }

  private void assertApplicationComponent(
      String applicationId,
      String stageTypeId,
      Date time,
      String hash,
      ComponentIdentifier componentIdentifier,
      String matchStateId,
      String identificationSourceId,
      boolean proprietary,
      String pathnames,
      OwnerComponent actual)
  {
    assertThat(actual.getOwnerId()).isEqualTo(applicationId);
    assertThat(actual.getStageTypeId()).isEqualTo(stageTypeId);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getTime()).isEqualTo(time);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getMatchStateId()).isEqualTo(matchStateId);
    assertThat(actual.getIdentificationSourceId()).isEqualTo(identificationSourceId);
    assertThat(actual.isProprietary()).isEqualTo(proprietary);
    assertThat(actual.getPathnamesString()).isEqualTo(pathnames);
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsSince_AppFiltering_Postgres() {
    testGetByOwnerIdsAndStageTypeIdsSince_AppFiltering(false);
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIds_AppFiltering_Postgres() {
    testGetByOwnerIdsAndStageTypeIds_AppFiltering(false, null);
  }

  private void testGetByOwnerIdsAndStageTypeIdsSince_AppFiltering(boolean isDatabaseEmbedded) {
    testGetByOwnerIdsAndStageTypeIds_AppFiltering(isDatabaseEmbedded, new Date());
  }

  private void testGetByOwnerIdsAndStageTypeIds_AppFiltering(boolean isDatabaseEmbedded, Date date) {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    String appId1 = application.getId();
    String appId2 = tempEntity.newApplication(organization.getId()).getId();
    Set<String> largeIdList = new HashSet<>();

    int threshold = isDatabaseEmbedded
        ? OwnerComponentDAO.H2_IN_OPERATOR_THRESHOLD
        : OwnerComponentDAO.POSTGRES_IN_OPERATOR_THRESHOLD;
    // make a collection of over 2000 ids.
    largeIdList.add(appId1);
    largeIdList.add(appId2);
    for (int i = 0; i < threshold; i++) {
      largeIdList.add(Integer.toString(i));
    }

    Date currentDate = date != null ? date : new Date();
    String componentId1 = tempEntity.newApplicationComponent(appId1, ReleaseStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), null, MatchState.EXACT, false,
        new Date(currentDate.getTime() + 1000)).getId();
    String componentId2 = tempEntity.newApplicationComponent(appId1, ReleaseStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"), null, MatchState.EXACT, false,
        new Date(currentDate.getTime() + 2000)).getId();
    String componentId3 = tempEntity.newApplicationComponent(appId2, ReleaseStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("g", "a", "3"), null, MatchState.EXACT, false,
        new Date(currentDate.getTime() + 3000)).getId();
    tempEntity
        .newApplicationComponent(tempEntity.newApplication(organization.getId()).getId(), ReleaseStageType.ID, "hash-4",
            ComponentIdentifier.createMavenCoordinates("g", "a", "4"), null, MatchState.EXACT, false, currentDate)
        .getId();

    Set<String> stageTypeIds = Collections.singleton(ReleaseStageType.ID);

    if (date != null) {
      List<OwnerComponent> components = dao.getByOwnerIdsAndStageTypeIdsSince(null, stageTypeIds, date);
      assertThat(components).isEmpty();
      components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.emptySet(), stageTypeIds, date);
      assertThat(components).isEmpty();
      components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.singleton("missing"), stageTypeIds, date);
      assertThat(components).isEmpty();
      components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds, date);
      assertThat(components).extracting(OwnerComponent::getId).containsExactly(componentId1, componentId2);
      components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.singleton(appId2), stageTypeIds, date);
      assertThat(components).extracting(OwnerComponent::getId).containsExactly(componentId3);
      components =
          dao.getByOwnerIdsAndStageTypeIdsSince(new HashSet<>(Arrays.asList(appId1, appId2)), stageTypeIds, date);
      assertThat(components).extracting(OwnerComponent::getId)
          .containsExactly(componentId1, componentId2,
              componentId3);
      components = dao.getByOwnerIdsAndStageTypeIdsSince(largeIdList, stageTypeIds, date);
      assertThat(components).extracting(OwnerComponent::getId)
          .containsExactly(componentId1, componentId2,
              componentId3);
    }
    else {
      List<OwnerComponent> components = dao.getByOwnerIdsAndStageTypeIds(null, stageTypeIds);
      assertThat(components).isEmpty();
      components = dao.getByOwnerIdsAndStageTypeIds(Collections.emptySet(), stageTypeIds);
      assertThat(components).isEmpty();
      components = dao.getByOwnerIdsAndStageTypeIds(Collections.singleton("missing"), stageTypeIds);
      assertThat(components).isEmpty();
      components = dao.getByOwnerIdsAndStageTypeIds(Collections.singleton(appId1), stageTypeIds);
      assertThat(components).extracting(OwnerComponent::getId)
          .containsExactlyInAnyOrder(componentId1,
              componentId2);
      components = dao.getByOwnerIdsAndStageTypeIds(Collections.singleton(appId2), stageTypeIds);
      assertThat(components).extracting(OwnerComponent::getId).containsExactly(componentId3);
      components = dao.getByOwnerIdsAndStageTypeIds(new HashSet<>(Arrays.asList(appId1, appId2)), stageTypeIds);
      assertThat(components).extracting(OwnerComponent::getId)
          .containsExactlyInAnyOrder(componentId1,
              componentId2, componentId3);
      components = dao.getByOwnerIdsAndStageTypeIds(largeIdList, stageTypeIds);
      assertThat(components).extracting(OwnerComponent::getId)
          .containsExactlyInAnyOrder(componentId1,
              componentId2, componentId3);
    }
  }

  @Test
  public void testGetComponentsRiskFiltered_DefaultFilters() {
    long time = System.currentTimeMillis() - 1000;

    Policy app1Policy = tempEntity.newPolicy(application.getId(), "app owned policy", 5);
    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "test scan app1 id", new Date(time));

    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));

    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, "Group1",
        "Artifact1", "Version1", "hash", "ConstraintFact1");

    List<ApplicationComponentRisk> result = dao.getComponentsRiskFiltered(Set.of(application.getId()),
        Set.of(BuildStageType.ID, SourceStageType.ID, ReleaseStageType.ID), Collections.emptySet(),
        new AbstractMap.SimpleEntry<>(0, 10), Collections.emptySet(),
        "score DESC", 0, 100);

    ApplicationComponentRisk expected = new ApplicationComponentRisk("hash", null, "maven", null, 1, 5, 0, 5, 0, 0);

    assertApplicationComponentRisk(List.of(expected), result);
  }

  @Test
  public void testGetComponentsRiskFiltered_ApplicationFilter() {
    long time = System.currentTimeMillis() - 1000;

    Application application1 = tempEntity.newApplication(organization.getId());
    Application application2 = tempEntity.newApplication(organization.getId());
    Application application3 = tempEntity.newApplication(organization.getId());

    Policy orgPolicy1 = tempEntity.newPolicy(organization.getId(), "critical policy", 10);
    Policy orgPolicy2 = tempEntity.newPolicy(organization.getId(), "severe policy", 5);
    Policy orgPolicy3 = tempEntity.newPolicy(organization.getId(), "moderate policy", 2);

    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID,
        "test scan app1 id", new Date(time));
    PolicyEvaluation app2PolicyEvaluation = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "test scan app2 id", new Date(time));
    PolicyEvaluation app3PolicyEvaluation = tempEntity.newPolicyEvaluation(application3.getId(), BuildStageType.ID,
        "test scan app3 id", new Date(time));

    tempEntity.newApplicationComponent(application1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("a", "b", "1"));
    tempEntity.newApplicationComponent(application1.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("c", "d", "1"));
    tempEntity.newApplicationComponent(application2.getId(), BuildStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("e", "f", "1"));
    tempEntity.newApplicationComponent(application2.getId(), BuildStageType.ID, "hash-4",
        ComponentIdentifier.createMavenCoordinates("g", "h", "1"));
    tempEntity.newApplicationComponent(application3.getId(), BuildStageType.ID, "hash-5",
        ComponentIdentifier.createMavenCoordinates("i", "j", "1"));

    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy1, "Group1",
        "Artifact1", "Version1", "hash", "ConstraintFact1");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy1, "Group2",
        "Artifact2", "Version2", "hash", "ConstraintFact2");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy2, "Group3",
        "Artifact3", "Version3", "hash", "ConstraintFact3");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy3, "Group4",
        "Artifact4", "Version4", "hash", "ConstraintFact4");
    tempEntity.newPolicyViolation(app3PolicyEvaluation, orgPolicy2, "Group5",
        "Artifact5", "Version5", "hash", "ConstraintFact5");
    tempEntity.newPolicyViolation(app3PolicyEvaluation, orgPolicy3, "Group6",
        "Artifact6", "Version6", "hash", "ConstraintFact6");

    List<ApplicationComponentRisk> result = dao.getComponentsRiskFiltered(
        Set.of(application1.getId(), application2.getId()), Set.of(BuildStageType.ID, SourceStageType.ID,
            ReleaseStageType.ID),
        Collections.emptySet(), new AbstractMap.SimpleEntry<>(0, 10), Collections.emptySet(),
        "score DESC", 0, 100);

    List<ApplicationComponentRisk> expected = List.of(
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 10, 10, 0, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 10, 10, 0, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 5, 0, 5, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 2, 0, 0, 2, 0));

    assertApplicationComponentRisk(expected, result);
  }

  @Test
  public void testGetComponentsRiskFiltered_StageFilter() {
    long time = System.currentTimeMillis() - 1000;

    Application application1 = tempEntity.newApplication(organization.getId());
    Application application2 = tempEntity.newApplication(organization.getId());
    Application application3 = tempEntity.newApplication(organization.getId());

    Policy orgPolicy1 = tempEntity.newPolicy(organization.getId(), "critical policy", 10);
    Policy orgPolicy2 = tempEntity.newPolicy(organization.getId(), "severe policy", 5);
    Policy orgPolicy3 = tempEntity.newPolicy(organization.getId(), "moderate policy", 2);

    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID,
        "test scan app1 id", new Date(time));
    PolicyEvaluation app2PolicyEvaluation = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "test scan app2 id", new Date(time));
    PolicyEvaluation app3PolicyEvaluation = tempEntity.newPolicyEvaluation(application3.getId(), SourceStageType.ID,
        "test scan app3 id", new Date(time));

    tempEntity.newApplicationComponent(application1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("a", "b", "1"));
    tempEntity.newApplicationComponent(application1.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("c", "d", "1"));
    tempEntity.newApplicationComponent(application2.getId(), BuildStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("e", "f", "1"));
    tempEntity.newApplicationComponent(application2.getId(), BuildStageType.ID, "hash-4",
        ComponentIdentifier.createMavenCoordinates("g", "h", "1"));
    tempEntity.newApplicationComponent(application3.getId(), SourceStageType.ID, "hash-5",
        ComponentIdentifier.createMavenCoordinates("i", "j", "1"));

    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy1, "Group1",
        "Artifact1", "Version1", "hash", "ConstraintFact1");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy1, "Group2",
        "Artifact2", "Version2", "hash", "ConstraintFact2");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy2, "Group3",
        "Artifact3", "Version3", "hash", "ConstraintFact3");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy3, "Group4",
        "Artifact4", "Version4", "hash", "ConstraintFact4");
    tempEntity.newPolicyViolation(app3PolicyEvaluation, orgPolicy2, "Group5",
        "Artifact5", "Version5", "hash", "ConstraintFact5");
    tempEntity.newPolicyViolation(app3PolicyEvaluation, orgPolicy3, "Group6",
        "Artifact6", "Version6", "hash", "ConstraintFact6");

    List<ApplicationComponentRisk> result = dao.getComponentsRiskFiltered(
        Set.of(application1.getId(), application2.getId(), application3.getId()), Set.of(BuildStageType.ID,
            SourceStageType.ID, ReleaseStageType.ID),
        Collections.emptySet(), new AbstractMap.SimpleEntry<>(0, 10),
        Collections.emptySet(),
        "score DESC", 0, 100);

    List<ApplicationComponentRisk> expected = List.of(
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 10, 10, 0, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 10, 10, 0, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 5, 0, 5, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 5, 0, 5, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 2, 0, 0, 2, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 2, 0, 0, 2, 0));

    assertApplicationComponentRisk(expected, result);
  }

  @Test
  public void testGetComponentsRiskFiltered_TheatCategoryFilter() {
    long time = System.currentTimeMillis() - 1000;

    Application application1 = tempEntity.newApplication(organization.getId());
    Application application2 = tempEntity.newApplication(organization.getId());
    Application application3 = tempEntity.newApplication(organization.getId());

    Policy orgPolicy1 = tempEntity.newPolicy(organization.getId(), "critical policy", 10);
    Policy orgPolicy2 = tempEntity.newPolicy(organization.getId(), "severe policy", 5);
    Policy orgPolicy3 = tempEntity.newPolicy(organization.getId(), "moderate policy", 2);

    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID,
        "test scan app1 id", new Date(time));
    PolicyEvaluation app2PolicyEvaluation = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "test scan app2 id", new Date(time));
    PolicyEvaluation app3PolicyEvaluation = tempEntity.newPolicyEvaluation(application3.getId(), SourceStageType.ID,
        "test scan app3 id", new Date(time));

    tempEntity.newApplicationComponent(application1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("a", "b", "1"));
    tempEntity.newApplicationComponent(application1.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("c", "d", "1"));
    tempEntity.newApplicationComponent(application2.getId(), BuildStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("e", "f", "1"));
    tempEntity.newApplicationComponent(application2.getId(), BuildStageType.ID, "hash-4",
        ComponentIdentifier.createMavenCoordinates("g", "h", "1"));
    tempEntity.newApplicationComponent(application3.getId(), SourceStageType.ID, "hash-5",
        ComponentIdentifier.createMavenCoordinates("i", "j", "1"));

    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy1, "Group1",
        "Artifact1", "Version1", "hash", "ConstraintFact1");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy1, "Group2",
        "Artifact2", "Version2", "hash", "ConstraintFact2");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy2, "Group3",
        "Artifact3", "Version3", "hash", "ConstraintFact3");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy3, "Group4",
        "Artifact4", "Version4", "hash", "ConstraintFact4");
    tempEntity.newPolicyViolation(app3PolicyEvaluation, orgPolicy2, "Group5",
        "Artifact5", "Version5", "hash", "ConstraintFact5");
    tempEntity.newPolicyViolation(app3PolicyEvaluation, orgPolicy3, "Group6",
        "Artifact6", "Version6", "hash", "ConstraintFact6");

    List<ApplicationComponentRisk> result = dao.getComponentsRiskFiltered(
        Set.of(application1.getId(), application2.getId(), application3.getId()),
        Set.of(BuildStageType.ID, SourceStageType.ID,
            ReleaseStageType.ID),
        Set.of(PolicyThreatCategory.SECURITY.getId()),
        new AbstractMap.SimpleEntry<>(0, 10), Collections.emptySet(), "score DESC", 0, 100);

    List<ApplicationComponentRisk> expected = List.of(
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 10, 10, 0, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 10, 10, 0, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 5, 0, 5, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 5, 0, 5, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 2, 0, 0, 2, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 2, 0, 0, 2, 0));

    assertApplicationComponentRisk(expected, result);

    result = dao.getComponentsRiskFiltered(
        Set.of(application1.getId(), application2.getId(), application3.getId()),
        Set.of(BuildStageType.ID, SourceStageType.ID,
            ReleaseStageType.ID),
        Set.of(PolicyThreatCategory.LICENSE.getId()),
        new AbstractMap.SimpleEntry<>(0, 10), Collections.emptySet(), "score DESC", 0, 100);

    assertThat(result).hasSize(0);
  }

  @Test
  public void testGetComponentsRiskFiltered_ThreatLevelFilter() {
    long time = System.currentTimeMillis() - 1000;

    Application application1 = tempEntity.newApplication(organization.getId());
    Application application2 = tempEntity.newApplication(organization.getId());
    Application application3 = tempEntity.newApplication(organization.getId());

    Policy orgPolicy1 = tempEntity.newPolicy(organization.getId(), "critical policy", 10);
    Policy orgPolicy2 = tempEntity.newPolicy(organization.getId(), "severe policy", 5);
    Policy orgPolicy3 = tempEntity.newPolicy(organization.getId(), "moderate policy", 2);

    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID,
        "test scan app1 id", new Date(time));
    PolicyEvaluation app2PolicyEvaluation = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "test scan app2 id", new Date(time));
    PolicyEvaluation app3PolicyEvaluation = tempEntity.newPolicyEvaluation(application3.getId(), SourceStageType.ID,
        "test scan app3 id", new Date(time));

    tempEntity.newApplicationComponent(application1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("a", "b", "1"));
    tempEntity.newApplicationComponent(application1.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("c", "d", "1"));
    tempEntity.newApplicationComponent(application2.getId(), BuildStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("e", "f", "1"));
    tempEntity.newApplicationComponent(application2.getId(), BuildStageType.ID, "hash-4",
        ComponentIdentifier.createMavenCoordinates("g", "h", "1"));
    tempEntity.newApplicationComponent(application3.getId(), SourceStageType.ID, "hash-5",
        ComponentIdentifier.createMavenCoordinates("i", "j", "1"));

    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy1, "Group1",
        "Artifact1", "Version1", "hash", "ConstraintFact1");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy1, "Group2",
        "Artifact2", "Version2", "hash", "ConstraintFact2");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy2, "Group3",
        "Artifact3", "Version3", "hash", "ConstraintFact3");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy3, "Group4",
        "Artifact4", "Version4", "hash", "ConstraintFact4");
    tempEntity.newPolicyViolation(app3PolicyEvaluation, orgPolicy2, "Group5",
        "Artifact5", "Version5", "hash", "ConstraintFact5");
    tempEntity.newPolicyViolation(app3PolicyEvaluation, orgPolicy3, "Group6",
        "Artifact6", "Version6", "hash", "ConstraintFact6");

    List<ApplicationComponentRisk> result = dao.getComponentsRiskFiltered(
        Set.of(application1.getId(), application2.getId(), application3.getId()),
        Set.of(BuildStageType.ID, SourceStageType.ID,
            ReleaseStageType.ID),
        Collections.emptySet(), new AbstractMap.SimpleEntry<>(6, 10), Collections.emptySet(),
        "score DESC", 0, 100);

    List<ApplicationComponentRisk> expected = List.of(
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 10, 10, 0, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 10, 10, 0, 0, 0));

    assertApplicationComponentRisk(expected, result);

    result = dao.getComponentsRiskFiltered(
        Set.of(application1.getId(), application2.getId(), application3.getId()),
        Set.of(BuildStageType.ID, SourceStageType.ID,
            ReleaseStageType.ID),
        Collections.emptySet(), new AbstractMap.SimpleEntry<>(0, 5), Collections.emptySet(),
        "score DESC", 0, 100);

    expected = List.of(
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 5, 0, 5, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 5, 0, 5, 0, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 2, 0, 0, 2, 0),
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 2, 0, 0, 2, 0));

    assertApplicationComponentRisk(expected, result);
  }

  @Test
  public void testGetComponentsRiskFiltered_ViolationStateFilter() {
    long time = System.currentTimeMillis() - 1000;

    Application application1 = tempEntity.newApplication(organization.getId());
    Application application2 = tempEntity.newApplication(organization.getId());
    Application application3 = tempEntity.newApplication(organization.getId());

    Policy orgPolicy1 = tempEntity.newPolicy(organization.getId(), "critical policy", 10);
    Policy orgPolicy2 = tempEntity.newPolicy(organization.getId(), "severe policy", 5);
    Policy orgPolicy3 = tempEntity.newPolicy(organization.getId(), "moderate policy", 2);
    Policy orgPolicy4 = tempEntity.newPolicy(organization.getId(), "legacy policy", 3);

    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID,
        "test scan app1 id", new Date(time));
    PolicyEvaluation app2PolicyEvaluation = tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "test scan app2 id", new Date(time));
    PolicyEvaluation app3PolicyEvaluation = tempEntity.newPolicyEvaluation(application3.getId(), BuildStageType.ID,
        "test scan app3 id", new Date(time));

    tempEntity.newApplicationComponent(application1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("a", "b", "1"));
    tempEntity.newApplicationComponent(application1.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("c", "d", "1"));
    tempEntity.newApplicationComponent(application2.getId(), BuildStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("e", "f", "1"));
    tempEntity.newApplicationComponent(application2.getId(), BuildStageType.ID, "hash-4",
        ComponentIdentifier.createMavenCoordinates("g", "h", "1"));
    tempEntity.newApplicationComponent(application3.getId(), BuildStageType.ID, "hash-5",
        ComponentIdentifier.createMavenCoordinates("i", "j", "1"));

    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy1, "Group1",
        "Artifact1", "Version1", "hash-1", "ConstraintFact1");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy1, "Group2",
        "Artifact2", "Version2", "hash-2", "ConstraintFact2");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy2, "Group3",
        "Artifact3", "Version3", "hash-3", "ConstraintFact3");
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy3, "Group4",
        "Artifact4", "Version4", "hash-4", "ConstraintFact4");
    tempEntity.newPolicyViolation(app3PolicyEvaluation, orgPolicy2, "Group5",
        "Artifact5", "Version5", "hash-5", "ConstraintFact5");
    PolicyWaiver waiver = tempEntity.newWaiver(orgPolicy3.getId(), application3.getId());
    tempEntity.newWaivedPolicyViolation(app3PolicyEvaluation, orgPolicy3, waiver);
    tempEntity.newLegacyPolicyViolation(app3PolicyEvaluation, orgPolicy4);

    List<ApplicationComponentRisk> result = dao.getComponentsRiskFiltered(
        Set.of(application1.getId(), application2.getId(), application3.getId()),
        Set.of(BuildStageType.ID), Collections.emptySet(),
        new AbstractMap.SimpleEntry<>(0, 10), Set.of("WAIVED"), "score DESC", 0, 100);

    List<ApplicationComponentRisk> expected = List.of(
        new ApplicationComponentRisk("hash", null, "maven", null, 1, 2, 0, 0, 2, 0));

    assertApplicationComponentRisk(expected, result);

    result = dao.getComponentsRiskFiltered(
        Set.of(application1.getId(), application2.getId(), application3.getId()),
        Set.of(BuildStageType.ID), Collections.emptySet(),
        new AbstractMap.SimpleEntry<>(0, 10), Set.of("OPEN"), "score DESC", 0, 100);

    expected = List.of(
        new ApplicationComponentRisk("hash-1", null, "maven", null, 1, 10, 10, 0, 0, 0),
        new ApplicationComponentRisk("hash-2", null, "maven", null, 1, 10, 10, 0, 0, 0),
        new ApplicationComponentRisk("hash-3", null, "maven", null, 1, 5, 0, 5, 0, 0),
        new ApplicationComponentRisk("hash-5", null, "maven", null, 1, 5, 0, 5, 0, 0),
        new ApplicationComponentRisk("hash-4", null, "maven", null, 1, 2, 0, 0, 2, 0));

    assertApplicationComponentRisk(expected, result);

    result = dao.getComponentsRiskFiltered(
        Set.of(application1.getId(), application2.getId(), application3.getId()),
        Set.of(BuildStageType.ID), Collections.emptySet(),
        new AbstractMap.SimpleEntry<>(0, 10), Set.of("LEGACY_VIOLATION"), "score DESC", 0, 100);

    expected = List.of(
        new ApplicationComponentRisk(result.get(0).hash(), "unknown.jar", "npm", null, 1, 3, 0, 0, 3, 0));

    assertApplicationComponentRisk(expected, result);
  }

  public void assertApplicationComponentRisk(
      List<ApplicationComponentRisk> expected,
      List<ApplicationComponentRisk> actual)
  {
    assertThat(actual).isNotNull();
    assertThat(actual).hasSize(expected.size());
    assertThat(actual).usingElementComparator((a, b) -> {
      if (Objects.equals(a.hash(), b.hash()) &&
          Objects.equals(a.componentIdFormat(), b.componentIdFormat()) &&
          Objects.equals(a.affectedApplications(), b.affectedApplications()) &&
          Objects.equals(a.score(), b.score()) &&
          Objects.equals(a.scoreCritical(), b.scoreCritical()) &&
          Objects.equals(a.scoreSevere(), b.scoreSevere()) &&
          Objects.equals(a.scoreModerate(), b.scoreModerate()) &&
          Objects.equals(a.scoreLow(), b.scoreLow()))
      {
        return 0;
      }

      return -1;
    })
        .containsExactlyInAnyOrderElementsOf(expected);
  }

  public void assertApplicationComponent(OwnerComponent expected, OwnerComponent actual) {
    assertThat(actual).isNotNull();
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
    assertThat(actual.getHash()).isEqualTo(expected.getHash());
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getIdentificationSourceId()).isEqualTo(expected.getIdentificationSourceId());
    assertThat(actual.getStageTypeId()).isEqualTo(expected.getStageTypeId());
    assertThat(actual.getMatchStateId()).isEqualTo(expected.getMatchStateId());
    assertThat(actual.isProprietary()).isEqualTo(expected.isProprietary());
    assertThat(actual.getPathnames()).isEqualTo(expected.getPathnames());
    assertThat(actual.getPathnamesString()).isEqualTo(expected.getPathnamesString());
  }
}
