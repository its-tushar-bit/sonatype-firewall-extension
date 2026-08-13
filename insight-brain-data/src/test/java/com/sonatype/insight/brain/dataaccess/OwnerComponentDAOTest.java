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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.OwnerComponentLicense;
import com.sonatype.insight.brain.model.ApplicationComponentRisk;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;

import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OwnerComponentDAOTest
    extends AbstractDbDAOTest
{
  private OwnerComponentDAO dao;

  private AggregateFileDAO aggregateFileDAO;

  private OwnerComponentLicenseDAO applicationComponentLicenseDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createOwnerComponentDAO();
    aggregateFileDAO = daoFactory.createAggregateFileDAO();
    applicationComponentLicenseDAO = daoFactory.createOwnerComponentLicenseDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Date now = new Date();
    OwnerComponent appComponent = new OwnerComponent(application.getId(), BuildStageType.ID, now, "hash",
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), true /* proprietary */, null /* pathnames */);
    dao.insert(appComponent);
    assertThat(appComponent.getId()).isNotNull();

    // Get
    appComponent = dao.getById(appComponent.getId());
    assertThat(appComponent).isNotNull();
    assertApplicationComponent(application.getId(), BuildStageType.ID, now, "hash",
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), true /* proprietary */, null /* pathnames */, appComponent);

    // Update
    OwnerComponent appComponentToUpdate = appComponent;
    assertThatThrownBy(() -> dao.update(appComponentToUpdate)).isInstanceOf(UnsupportedOperationException.class);

    // Delete
    dao.delete(appComponent);

    // Get
    appComponent = dao.getById(appComponent.getId());
    assertThat(appComponent).isNull();
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
  public void testGetByOwnerIdAndStageTypeIdAndHash() {
    String app1 = application.getId();
    String app2 = tempEntity.newApplication(organization.getId()).getId();
    OwnerComponent component1 = tempEntity.newApplicationComponent(app1, BuildStageType.ID, "hash-1",
        MatchState.EXACT, false);
    tempEntity.newApplicationComponent(app1, ReleaseStageType.ID, "hash-1", MatchState.EXACT, true);
    tempEntity.newApplicationComponent(app1, BuildStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("Group2", "Artifact2", "Version2"), null, MatchState.EXACT, true,
        new Date());
    tempEntity.newApplicationComponent(app1, BuildStageType.ID, "hash-2", MatchState.EXACT, true);
    tempEntity.newApplicationComponent(app2, BuildStageType.ID, "hash-1", MatchState.EXACT, false);

    OwnerComponent retrievedComponent = dao.getByOwnerIdAndStageTypeIdAndHash(app1, BuildStageType.ID,
        "hash-1");
    assertApplicationComponent(component1, retrievedComponent);
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeId() {
    String app1 = application.getId();
    String app2 = tempEntity.newApplication(organization.getId()).getId();

    tempEntity.newApplicationComponent(app1, BuildStageType.ID, "hash-1", MatchState.EXACT, false);
    tempEntity.newApplicationComponent(app1, BuildStageType.ID, "hash-2", MatchState.EXACT, false);
    // wrong stage for app1 must be excluded
    tempEntity.newApplicationComponent(app1, ReleaseStageType.ID, "hash-3", MatchState.EXACT, false);
    tempEntity.newApplicationComponent(app2, BuildStageType.ID, "hash-4", MatchState.EXACT, false);

    List<OwnerComponent> components =
        dao.getByOwnerIdsAndStageTypeId(Sets.newHashSet(app1, app2), BuildStageType.ID);
    assertThat(components).extracting(OwnerComponent::getHash)
        .containsExactlyInAnyOrder("hash-1", "hash-2", "hash-4");

    assertThat(dao.getByOwnerIdsAndStageTypeId(Collections.emptySet(), BuildStageType.ID)).isEmpty();
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsSince_AppFiltering_H2() {
    testGetByOwnerIdsAndStageTypeIdsSince_AppFiltering(true);
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIds_AppFiltering_H2() {
    testGetByOwnerIdsAndStageTypeIds_AppFiltering(true, null);
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
  public void testGetByOwnerIdsAndStageTypeIdsSince_StageFiltering() {
    String appId1 = application.getId();
    String appId2 = tempEntity.newApplication(organization.getId()).getId();

    Date date = new Date();
    String componentId1 = tempEntity.newApplicationComponent(appId1, BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 1000)).getId();
    String componentId2 = tempEntity.newApplicationComponent(appId1, ReleaseStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 2000)).getId();
    String componentId3 = tempEntity.newApplicationComponent(appId2, ReleaseStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("g", "a", "3"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 3000)).getId();
    tempEntity
        .newApplicationComponent(tempEntity.newApplication(organization.getId()).getId(), ReleaseStageType.ID, "hash-4",
            ComponentIdentifier.createMavenCoordinates("g", "a", "4"), null, MatchState.EXACT, false, date)
        .getId();

    Set<String> appIds = Collections.singleton(application.getId());
    List<OwnerComponent> components = dao.getByOwnerIdsAndStageTypeIdsSince(appIds, null, date);
    assertThat(components).isEmpty();
    components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.emptySet(), Collections.emptySet(), date);
    assertThat(components).isEmpty();
    components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.singleton("missing"),
        Collections.singleton("missing"), date);
    assertThat(components).isEmpty();
    components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.singleton(appId1),
        Collections.singleton(BuildStageType.ID), date);
    assertThat(components).extracting(OwnerComponent::getId).containsExactly(componentId1);
    components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.singleton(appId1),
        Collections.singleton(ReleaseStageType.ID), date);
    assertThat(components).extracting(OwnerComponent::getId).containsExactly(componentId2);
    components = dao.getByOwnerIdsAndStageTypeIdsSince(new HashSet<>(Arrays.asList(appId1, appId2)),
        new HashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID)), date);
    assertThat(components).extracting(OwnerComponent::getId)
        .containsExactly(componentId1, componentId2,
            componentId3);
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIds_StageFiltering() {
    String appId1 = application.getId();
    String appId2 = tempEntity.newApplication(organization.getId()).getId();

    Date date = new Date();
    String componentId1 = tempEntity.newApplicationComponent(appId1, BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 1000)).getId();
    String componentId2 = tempEntity.newApplicationComponent(appId1, ReleaseStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 2000)).getId();
    String componentId3 = tempEntity.newApplicationComponent(appId2, ReleaseStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("g", "a", "3"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 3000)).getId();
    tempEntity.newApplicationComponent(tempEntity.newApplication(organization.getId()).getId(), ReleaseStageType.ID,
        "hash-4", ComponentIdentifier.createMavenCoordinates("g", "a", "4"), null, MatchState.EXACT, false, date)
        .getId();

    Set<String> appIds = Collections.singleton(application.getId());
    List<OwnerComponent> components = dao.getByOwnerIdsAndStageTypeIds(appIds, null);
    assertThat(components).isEmpty();
    components = dao.getByOwnerIdsAndStageTypeIds(Collections.emptySet(), Collections.emptySet());
    assertThat(components).isEmpty();
    components =
        dao.getByOwnerIdsAndStageTypeIds(Collections.singleton("missing"), Collections.singleton("missing"));
    assertThat(components).isEmpty();
    components =
        dao.getByOwnerIdsAndStageTypeIds(Collections.singleton(appId1), Collections.singleton(BuildStageType.ID));
    assertThat(components).extracting(OwnerComponent::getId).containsExactly(componentId1);
    components = dao.getByOwnerIdsAndStageTypeIds(Collections.singleton(appId1),
        Collections.singleton(ReleaseStageType.ID));
    assertThat(components).extracting(OwnerComponent::getId).containsExactly(componentId2);
    components = dao.getByOwnerIdsAndStageTypeIds(new HashSet<>(Arrays.asList(appId1, appId2)),
        new HashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID)));
    assertThat(components).extracting(OwnerComponent::getId)
        .containsExactlyInAnyOrder(componentId1, componentId2,
            componentId3);
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsSince_DateFiltering() {
    String appId1 = application.getId();

    Date date = new Date();
    String componentId1 = tempEntity.newApplicationComponent(appId1, ReleaseStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 1000)).getId();
    String componentId2 = tempEntity.newApplicationComponent(appId1, ReleaseStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 2000)).getId();

    Set<String> stageTypeIds = Collections.singleton(ReleaseStageType.ID);
    Set<String> appIds = Collections.singleton(application.getId());
    List<OwnerComponent> components = dao.getByOwnerIdsAndStageTypeIdsSince(appIds, stageTypeIds, null);
    assertThat(components).isEmpty();
    components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds,
        new Date(date.getTime() + 3000));
    assertThat(components).isEmpty();
    components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds, date);
    assertThat(components).extracting(OwnerComponent::getId).containsExactly(componentId1, componentId2);
    components = dao.getByOwnerIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds,
        new DateTime(date).minusDays(1).toDate());
    assertThat(components).extracting(OwnerComponent::getId).containsExactly(componentId1, componentId2);
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsSince_MultipleStages() {
    String appId1 = application.getId();
    String appId2 = tempEntity.newApplication(organization.getId()).getId();

    Date date = new Date();

    String componentId1 = tempEntity.newApplicationComponent(appId1, BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 1000)).getId();
    String componentId2 = tempEntity.newApplicationComponent(appId1, ReleaseStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 2000)).getId();
    String componentId3 = tempEntity.newApplicationComponent(appId2, BuildStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("g", "a", "3"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 3000)).getId();

    Set<String> appIds = new HashSet<>(Arrays.asList(appId1, appId2));
    Set<String> stageIds = new HashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID));
    List<OwnerComponent> components = dao.getByOwnerIdsAndStageTypeIdsSince(appIds, stageIds, date);
    assertThat(components).extracting(OwnerComponent::getId)
        .containsExactly(componentId1, componentId2,
            componentId3);
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIds_MultipleStages() {
    String appId1 = application.getId();
    String appId2 = tempEntity.newApplication(organization.getId()).getId();

    Date date = new Date();

    String componentId1 = tempEntity.newApplicationComponent(appId1, BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 1000)).getId();
    String componentId2 = tempEntity.newApplicationComponent(appId1, ReleaseStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 2000)).getId();
    String componentId3 = tempEntity.newApplicationComponent(appId2, BuildStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("g", "a", "3"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 3000)).getId();

    Set<String> appIds = new HashSet<>(Arrays.asList(appId1, appId2));
    Set<String> stageIds = new HashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID));
    List<OwnerComponent> components = dao.getByOwnerIdsAndStageTypeIds(appIds, stageIds);
    assertThat(components).extracting(OwnerComponent::getId)
        .containsExactlyInAnyOrder(componentId1, componentId2,
            componentId3);
  }

  @Test
  public void testGetCount() {
    String appId1 = application.getId();
    Date date = new Date();

    tempEntity.newApplicationComponent(appId1, BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 1000)).getId();
    tempEntity.newApplicationComponent(appId1, ReleaseStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"), null, MatchState.EXACT, false,
        new Date(date.getTime() + 2000)).getId();

    assertThat(dao.getCount()).isEqualTo(2);
  }

  @Test
  public void testCascadeDeleteToAggregateFiles() {
    OwnerComponent applicationComponent1 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    OwnerComponent applicationComponent2 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash2", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    tempEntity.newAggregateFile(applicationComponent1.getId(), "hash3", null);
    tempEntity.newAggregateFile(applicationComponent1.getId(), "hash4",
        Sets.newLinkedHashSet(Arrays.asList("pathname1", "pathname2")));
    AggregateFile aggregateFile3 = tempEntity.newAggregateFile(applicationComponent2.getId(), "hash5", null);
    AggregateFile aggregateFile4 = tempEntity.newAggregateFile(applicationComponent2.getId(), "hash6",
        Sets.newLinkedHashSet(Arrays.asList("pathname3", "pathname4")));

    dao.delete(applicationComponent1);

    assertThat(aggregateFileDAO.getByOwnerComponentId(applicationComponent1.getId())).isEmpty();
    assertThat(aggregateFileDAO.getByOwnerComponentId(applicationComponent2.getId()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(aggregateFile3, aggregateFile4);
  }

  @Test
  public void testCascadeDeleteToOwnerComponentLicense() {
    OwnerComponent applicationComponent1 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    OwnerComponent applicationComponent2 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));

    tempEntity.newApplicationComponentLicense(applicationComponent1.getId(), "license-1");
    tempEntity.newApplicationComponentLicense(applicationComponent1.getId(), "license-2");

    OwnerComponentLicense applicationComponentLicense3 =
        tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-3");
    OwnerComponentLicense applicationComponentLicense4 =
        tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-4");

    dao.delete(applicationComponent1);

    assertThat(applicationComponentLicenseDAO.getByOwnerComponentId(applicationComponent1.getId())).isEmpty();
    assertThat(applicationComponentLicenseDAO.getByOwnerComponentId(applicationComponent2.getId()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(applicationComponentLicense3, applicationComponentLicense4);
  }

  @Test
  public void testGetApplicationIdsAndStageTypeIdsByReviewStatus() {
    OwnerComponent applicationComponent1 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    tempEntity.newComponentObligation(applicationComponent1.getComponentIdentifier(),
        applicationComponent1.getOwnerId(), "obligation1", "comment1", ObligationStatus.FULFILLED, "hash1");

    Application otherApplication = tempEntity.newApplication(organization.getId());

    OwnerComponent applicationComponent2 = tempEntity.newApplicationComponent(otherApplication.getId(),
        BuildStageType.ID, "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newComponentObligation(applicationComponent2.getComponentIdentifier(),
        applicationComponent2.getOwnerId(), "obligation2", "comment2", ObligationStatus.IGNORED, "hash2");

    OwnerComponent applicationComponent3 = tempEntity.newApplicationComponent(application.getId(),
        DevelopStageType.ID, "hash3", ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newComponentObligation(applicationComponent3.getComponentIdentifier(),
        applicationComponent3.getOwnerId(), "obligation3", "comment3", ObligationStatus.OPEN, "hash3");

    OwnerComponent applicationComponent4 = tempEntity.newApplicationComponent(otherApplication.getId(),
        DevelopStageType.ID, "hash4", ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newComponentObligation(applicationComponent4.getComponentIdentifier(),
        applicationComponent4.getOwnerId(), "obligation4", "comment4", ObligationStatus.FLAGGED, "hash4");

    Application applicationNewParent = tempEntity.newApplicationWithParent();

    OwnerComponent applicationComponent5 = tempEntity.newApplicationComponent(applicationNewParent.getId(),
        BuildStageType.ID, "hash5", ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"));
    tempEntity.newComponentObligation(applicationComponent5.getComponentIdentifier(),
        applicationNewParent.getOrganizationId(),
        "obligation5", "comment5", ObligationStatus.FULFILLED, "hash5");

    Application applicationForRoot = tempEntity.newApplicationWithParent();

    OwnerComponent applicationComponent6 = tempEntity.newApplicationComponent(applicationForRoot.getId(),
        DevelopStageType.ID, "hash6", ComponentIdentifier.createMavenCoordinates("g6", "a6", "v6"));
    tempEntity.newComponentObligation(applicationComponent6.getComponentIdentifier(), Organization.ROOT_ORGANIZATION_ID,
        "obligation6", "comment6", ObligationStatus.IGNORED, "hash6");

    Application oneMoreApplication = tempEntity.newApplication(organization.getId());
    OwnerComponent applicationComponent7 = tempEntity.newApplicationComponent(oneMoreApplication.getId(),
        BuildStageType.ID, "hash7", ComponentIdentifier.createMavenCoordinates("g7", "a7", "v7"));
    tempEntity.newComponentObligation(applicationComponent7.getComponentIdentifier(),
        applicationComponent7.getOwnerId(), "obligation7", "comment7", ObligationStatus.IGNORED, "hash7");

    OwnerComponent applicationComponent8 = tempEntity.newApplicationComponent(application.getId(),
        ReleaseStageType.ID, "hash8", ComponentIdentifier.createMavenCoordinates("g8", "a8", "v8"));
    tempEntity.newComponentObligation(applicationComponent7.getComponentIdentifier(),
        applicationComponent8.getOwnerId(), "obligation8", "comment8", ObligationStatus.FULFILLED, "hash8");

    Application applicationWithoutReview = tempEntity.newApplication(organization.getId());
    tempEntity.newApplicationComponent(applicationWithoutReview.getId(), DevelopStageType.ID, "hash9",
        ComponentIdentifier.createMavenCoordinates("g9", "a9", "v9"));

    List<Object[]> result = dao.getOwnerIdsAndStageTypeIdsByReviewStatus(
        Sets.newHashSet(application.getId(), otherApplication.getId(), applicationNewParent.getId(),
            applicationForRoot.getId(), applicationWithoutReview.getId()),
        Sets.newHashSet(BuildStageType.ID, DevelopStageType.ID),
        true);

    assertThat(result).isNotEmpty()
        .containsExactlyInAnyOrder(
            new Object[]{application.getId(), BuildStageType.ID},
            new Object[]{otherApplication.getId(), BuildStageType.ID},
            new Object[]{application.getId(), DevelopStageType.ID},
            new Object[]{otherApplication.getId(), DevelopStageType.ID},
            new Object[]{applicationNewParent.getId(), BuildStageType.ID},
            new Object[]{applicationForRoot.getId(), DevelopStageType.ID});

    result = dao.getOwnerIdsAndStageTypeIdsByReviewStatus(
        Sets.newHashSet(application.getId(), otherApplication.getId(), applicationNewParent.getId(),
            applicationForRoot.getId(), applicationWithoutReview.getId()),
        Sets.newHashSet(BuildStageType.ID, DevelopStageType.ID),
        false);

    assertThat(result).isNotEmpty()
        .containsExactlyInAnyOrder(
            new Object[]{applicationWithoutReview.getId(), DevelopStageType.ID});
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    OwnerComponent applicationComponent1 =
        tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash1", componentIdentifier);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash1", componentIdentifier);
    OwnerComponent applicationComponent3 =
        tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash1", componentIdentifier);
    OwnerComponent applicationComponent4 =
        tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash2", componentIdentifier);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash3",
        componentIdentifier.createAlternativeVersion("v2"));

    assertThat(dao.getByOwnerIdAndComponentIdentifier(app1.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(applicationComponent1, applicationComponent3, applicationComponent4);
  }

  @Test
  public void testLastByComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app = tempEntity.newApplicationWithParent();

    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash1", componentIdentifier, null,
        MatchState.EXACT, IdentificationSource.SONATYPE, false, new Date(1));
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, "hash1", componentIdentifier, null,
        MatchState.EXACT, IdentificationSource.SONATYPE, false, new Date(100));
    OwnerComponent applicationComponent3 =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash2", componentIdentifier, null,
            MatchState.EXACT, IdentificationSource.SONATYPE, false, new Date(1000));

    OwnerComponent appComponent = dao.getLastByComponentIdentifier(componentIdentifier);

    assertApplicationComponent(applicationComponent3, appComponent);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier_Empty() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app = tempEntity.newApplicationWithParent();

    assertThat(dao.getByOwnerIdAndComponentIdentifier(app.getId(), componentIdentifier)).isEmpty();
  }

  @Test
  public void testGetComponentsRiskFiltered_H2DatabaseNotSupported() {
    assertThatThrownBy(() -> dao.getComponentsRiskFiltered(Set.of(application.getId()), Collections.emptySet(),
        Collections.emptySet(), new AbstractMap.SimpleEntry<>(0, 10), Collections.emptySet(),
        "score DESC", 0, 100))
            .hasMessage("This operation is only supported for PostgreSQL databases")
            .isInstanceOf(UnsupportedOperationException.class);
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

  @Test
  public void testGetMapByOwnerIdsAndStageTypeIdsAndHashes() {
    Date now = new Date();
    OwnerComponent comp1 = new OwnerComponent(application.getId(), BuildStageType.ID, now, "hash1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), true, null);
    OwnerComponent comp2 = new OwnerComponent(application.getId(), BuildStageType.ID, now, "hash2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    OwnerComponent comp3 = new OwnerComponent(application.getId(), ReleaseStageType.ID, now, "hash3",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    dao.insert(comp1);
    dao.insert(comp2);
    dao.insert(comp3);

    Set<String> appIds = Set.of(application.getId());
    Set<String> stageTypeIds = Set.of(BuildStageType.ID, ReleaseStageType.ID);

    Map<OwnerComponentDAO.OwnerComponentKey, OwnerComponent> results =
        dao.getMapByOwnerIdsAndStageTypeIdsAndHashes(appIds, stageTypeIds, Set.of("hash1", "hash3"));
    assertThat(results).hasSize(2);
    assertThat(results.values()).extracting(OwnerComponent::getHash).containsExactlyInAnyOrder("hash1", "hash3");

    results = dao.getMapByOwnerIdsAndStageTypeIdsAndHashes(appIds, stageTypeIds, Set.of("nonexistent"));
    assertThat(results).isEmpty();

    results = dao.getMapByOwnerIdsAndStageTypeIdsAndHashes(appIds, stageTypeIds, Collections.emptySet());
    assertThat(results).isEmpty();

    results = dao.getMapByOwnerIdsAndStageTypeIdsAndHashes(Collections.emptySet(), stageTypeIds, Set.of("hash1"));
    assertThat(results).isEmpty();

    results = dao.getMapByOwnerIdsAndStageTypeIdsAndHashes(appIds, Collections.emptySet(), Set.of("hash1"));
    assertThat(results).isEmpty();

    // Multi-app: same hash exists in two different applications
    String app2Id = tempEntity.newApplication(organization.getId()).getId();
    OwnerComponent comp4 = new OwnerComponent(app2Id, BuildStageType.ID, now, "hash1",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    dao.insert(comp4);

    results = dao.getMapByOwnerIdsAndStageTypeIdsAndHashes(
        Set.of(application.getId(), app2Id), stageTypeIds, Set.of("hash1"));
    assertThat(results).hasSize(2);
    assertThat(results.values()).extracting(OwnerComponent::getOwnerId)
        .containsExactlyInAnyOrder(application.getId(), app2Id);
    assertThat(results.values()).extracting(OwnerComponent::getHash).containsOnly("hash1");

    // Only one app requested — should only get that app's component
    results = dao.getMapByOwnerIdsAndStageTypeIdsAndHashes(
        Set.of(app2Id), stageTypeIds, Set.of("hash1"));
    assertThat(results).hasSize(1);
    OwnerComponentDAO.OwnerComponentKey key = results.keySet().iterator().next();
    assertThat(key.ownerId()).isEqualTo(app2Id);
    assertThat(key.hash()).isEqualTo("hash1");
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

  @Test
  public void findDistinctOwnersByHashPaged_excludesOrphansAndRespectsOwnerScope() {
    Date newer = new Date(2_000L);
    Date older = new Date(1_000L);
    // varchar(20) hash; keep unique across parallel forks.
    String hash = ("u1" + TemporaryEntity.uuid()).substring(0, 20);
    dao.insert(new OwnerComponent(application.getId(), BuildStageType.ID, older, hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null));
    dao.insert(new OwnerComponent(application.getId(), ReleaseStageType.ID, newer, hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null));

    String app2Id = tempEntity.newApplication(organization.getId()).getId();
    dao.insert(new OwnerComponent(app2Id, BuildStageType.ID, older, hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null));

    // Orphan owner_component (no application row) must not inflate total or pages.
    // Delete before TemporaryEntity teardown — MAIN runs with -DdetectTestEntityLeaks.
    OwnerComponent orphan = new OwnerComponent("missing-app-" + TemporaryEntity.uuid(), BuildStageType.ID, newer,
        hash, ComponentIdentifier.createMavenCoordinates("g", "a", "1"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    dao.insert(orphan);
    try {
      OwnerComponentDAO.PagedOwnersByHash unrestricted =
          dao.findDistinctOwnersByHashPaged(hash, null, 0, 25);
      assertThat(unrestricted.total()).isEqualTo(2L);
      assertThat(unrestricted.rows()).extracting(OwnerComponentDAO.ComponentOwnerUsageRow::ownerId)
          .containsExactly(application.getId(), app2Id);
      // Compare epoch millis — Postgres returns Timestamp; Timestamp.equals(Date) is false.
      assertThat(unrestricted.rows().get(0).lastSeenTime().getTime()).isEqualTo(newer.getTime());

      OwnerComponentDAO.PagedOwnersByHash scoped =
          dao.findDistinctOwnersByHashPaged(hash, Set.of(app2Id), 0, 25);
      assertThat(scoped.total()).isEqualTo(1L);
      assertThat(scoped.rows()).extracting(OwnerComponentDAO.ComponentOwnerUsageRow::ownerId)
          .containsExactly(app2Id);

      assertThat(dao.findDistinctOwnersByHashPaged(hash, Set.of(), 0, 25).total()).isZero();

      Map<String, List<String>> stages =
          dao.getStageTypeIdsByOwnerIdForHash(hash, List.of(application.getId()));
      assertThat(stages.get(application.getId()))
          .containsExactlyInAnyOrder(BuildStageType.ID, ReleaseStageType.ID);
    }
    finally {
      dao.delete(orphan);
    }
  }

  @Test
  public void findDistinctOwnersByHashPaged_embeddedLargeScopeFiltersInMemory() {
    Date now = new Date();
    String hash = ("u3" + TemporaryEntity.uuid()).substring(0, 20);
    dao.insert(new OwnerComponent(application.getId(), BuildStageType.ID, now, hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null));

    // Above H2 complex-query IN threshold: must take the embedded hash-seek + in-memory filter path.
    Set<String> largeScope = new HashSet<>();
    largeScope.add(application.getId());
    for (int i = 0; i < 400; i++) {
      largeScope.add("missing-owner-" + i);
    }

    OwnerComponentDAO.PagedOwnersByHash paged =
        dao.findDistinctOwnersByHashPaged(hash, largeScope, 0, 25);
    assertThat(paged.total()).isEqualTo(1L);
    assertThat(paged.rows()).extracting(OwnerComponentDAO.ComponentOwnerUsageRow::ownerId)
        .containsExactly(application.getId());

    OwnerComponentDAO.PagedOrganizationsByHash orgs =
        dao.findDistinctOrganizationsByHashPaged(hash, largeScope, 0, 25);
    assertThat(orgs.total()).isEqualTo(1L);
    assertThat(orgs.rows()).extracting(OwnerComponentDAO.ComponentOrganizationUsageRow::organizationId)
        .containsExactly(organization.getId());
  }

  @Test
  public void findDistinctOrganizationsByHashPaged_pagesAndScopes() {
    Date now = new Date();
    String hash = ("u2" + TemporaryEntity.uuid()).substring(0, 20);
    dao.insert(new OwnerComponent(application.getId(), BuildStageType.ID, now, hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null));

    Organization org2 = tempEntity.newOrganization(getClass().getSimpleName() + "_org2_" + TemporaryEntity.uuid());
    String appInOrg2 = tempEntity.newApplication(org2.getId()).getId();
    dao.insert(new OwnerComponent(appInOrg2, BuildStageType.ID, new Date(now.getTime() + 1_000), hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null));

    OwnerComponentDAO.PagedOrganizationsByHash unrestricted =
        dao.findDistinctOrganizationsByHashPaged(hash, null, 0, 25);
    assertThat(unrestricted.total()).isEqualTo(2L);
    assertThat(unrestricted.rows()).extracting(OwnerComponentDAO.ComponentOrganizationUsageRow::organizationId)
        .containsExactly(org2.getId(), organization.getId());
    assertThat(unrestricted.rows().get(0).applicationCount()).isEqualTo(1L);

    OwnerComponentDAO.PagedOrganizationsByHash scoped =
        dao.findDistinctOrganizationsByHashPaged(hash, Set.of(application.getId()), 0, 25);
    assertThat(scoped.total()).isEqualTo(1L);
    assertThat(scoped.rows()).extracting(OwnerComponentDAO.ComponentOrganizationUsageRow::organizationId)
        .containsExactly(organization.getId());
  }

  @Test
  public void findReportsByHashAndOwnerPaged_returnsLatestNonBlankReportsByStage() {
    String hash = ("u4" + TemporaryEntity.uuid()).substring(0, 20);
    String otherHash = ("u5" + TemporaryEntity.uuid()).substring(0, 20);

    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(application.getId(), ReleaseStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(application.getId(), DevelopStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(application.getId(), SourceStageType.ID, otherHash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "build-old", new Date(1_000L));
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "build-new", new Date(2_000L));
    tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "release-new", new Date(3_000L));
    tempEntity.newPolicyEvaluation(application.getId(), DevelopStageType.ID, " ", new Date(4_000L));
    tempEntity.newPolicyEvaluation(application.getId(), SourceStageType.ID, "other-hash", new Date(5_000L));

    OwnerComponentDAO.PagedReportsByHashAndOwner firstPage =
        dao.findReportsByHashAndOwnerPaged(hash, application.getId(), 0, 1);
    assertThat(firstPage.total()).isEqualTo(2L);
    assertThat(firstPage.rows()).hasSize(1);
    assertThat(firstPage.rows().get(0).reportId()).isEqualTo("release-new");
    assertThat(firstPage.rows().get(0).stageTypeId()).isEqualTo(ReleaseStageType.ID);
    assertThat(firstPage.rows().get(0).evaluationTime().getTime()).isEqualTo(3_000L);

    OwnerComponentDAO.PagedReportsByHashAndOwner secondPage =
        dao.findReportsByHashAndOwnerPaged(hash, application.getId(), 1, 1);
    assertThat(secondPage.total()).isEqualTo(2L);
    assertThat(secondPage.rows()).hasSize(1);
    assertThat(secondPage.rows().get(0).reportId()).isEqualTo("build-new");
    assertThat(secondPage.rows().get(0).stageTypeId()).isEqualTo(BuildStageType.ID);
    assertThat(secondPage.rows().get(0).evaluationTime().getTime()).isEqualTo(2_000L);
  }

  @Test
  public void findReportsByHashAndOwnerPaged_blankHashOrOwnerReturnsEmpty() {
    OwnerComponentDAO.PagedReportsByHashAndOwner blankHash =
        dao.findReportsByHashAndOwnerPaged(" ", application.getId(), 0, 25);
    assertThat(blankHash.total()).isEqualTo(0L);
    assertThat(blankHash.rows()).isEmpty();

    OwnerComponentDAO.PagedReportsByHashAndOwner blankOwner =
        dao.findReportsByHashAndOwnerPaged("abc123", " ", 0, 25);
    assertThat(blankOwner.total()).isEqualTo(0L);
    assertThat(blankOwner.rows()).isEmpty();
  }

  @Test
  public void getCountsByOwnerIdsAndStageTypeIds_countsPerOwnerAndStage() {
    // Two owners (HRC-shaped: use unique synthetic ids), each with owner_component rows at two stages.
    String hrcA = TemporaryEntity.uuid();
    String hrcB = TemporaryEntity.uuid();
    seedOwnerComponent(hrcA, "build", "hAb1");
    seedOwnerComponent(hrcA, "build", "hAb2");
    seedOwnerComponent(hrcA, "build", "hAb3");
    seedOwnerComponent(hrcA, "release", "hAr1");
    seedOwnerComponent(hrcB, "build", "hBb1");
    try {
      Map<String, Integer> counts = dao.getCountsByOwnerIdsAndStageTypeIds(
          List.of(hrcA, hrcB, "no-such"), List.of("build", "release"));

      assertThat(counts).containsEntry(hrcA + "|build", 3);
      assertThat(counts).containsEntry(hrcA + "|release", 1);
      assertThat(counts).containsEntry(hrcB + "|build", 1);
      assertThat(counts).doesNotContainKey("no-such|build");
    }
    finally {
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        dao.deleteByOwnerIds(tx, List.of(hrcA, hrcB));
        tx.commit();
      }
    }
  }

  @Test
  public void getCountsByOwnerIdsAndStageTypeIds_emptyOrNullInputReturnsEmptyMap() {
    assertThat(dao.getCountsByOwnerIdsAndStageTypeIds(List.of(), List.of("build"))).isEmpty();
    assertThat(dao.getCountsByOwnerIdsAndStageTypeIds(List.of("owner"), List.of())).isEmpty();
    assertThat(dao.getCountsByOwnerIdsAndStageTypeIds(null, null)).isEmpty();
  }

  private void seedOwnerComponent(String ownerId, String stageTypeId, String hash) {
    OwnerComponent oc = new OwnerComponent(ownerId, stageTypeId, new Date(), hash,
        new ComponentIdentifier("maven", Map.of("groupId", "g", "artifactId", "a", "version", hash)),
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), false, List.of());
    dao.insert(oc);
  }
}
