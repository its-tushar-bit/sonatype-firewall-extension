/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApplicationComponentDAOTest
    extends AbstractDbDAOTest
{
  private ApplicationComponentDAO dao = new ApplicationComponentDAO();

  private AggregateFileDAO aggregateFileDAO = new AggregateFileDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    Date now = new Date();
    ApplicationComponent appComponent = new ApplicationComponent(application.getId(), BuildStageType.ID, now, "hash",
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
    ApplicationComponent appComponentToUpdate = appComponent;
    assertThatThrownBy(() -> {
      dao.update(appComponentToUpdate);
    }).isInstanceOf(UnsupportedOperationException.class);

    // Delete
    dao.delete(appComponent);

    // Get
    appComponent = dao.getById(appComponent.getId());
    assertThat(appComponent).isNull();
  }

  private void assertApplicationComponent(String applicationId,
                                          String stageTypeId,
                                          Date time,
                                          String hash,
                                          ComponentIdentifier componentIdentifier,
                                          String matchStateId,
                                          String identificationSourceId,
                                          boolean proprietary,
                                          String pathnames,
                                          ApplicationComponent actual)
  {
    assertThat(actual.getApplicationId()).isEqualTo(applicationId);
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
  public void testGetByApplicationIdAndStageTypeIdAndHash() {
    String app1 = application.getId();
    String app2 = tempEntity.newApplication(organization.getId()).getId();
    ApplicationComponent component1 = tempEntity.newApplicationComponent(app1, BuildStageType.ID, "hash-1",
        MatchState.EXACT, false);
    tempEntity.newApplicationComponent(app1, ReleaseStageType.ID, "hash-1", MatchState.EXACT, true);
    tempEntity.newApplicationComponent(app1, BuildStageType.ID, "hash-3",
        ComponentIdentifier.createMavenCoordinates("Group2", "Artifact2", "Version2"), null, MatchState.EXACT, true,
        new Date());
    tempEntity.newApplicationComponent(app1, BuildStageType.ID, "hash-2", MatchState.EXACT, true);
    tempEntity.newApplicationComponent(app2, BuildStageType.ID, "hash-1", MatchState.EXACT, false);

    ApplicationComponent retrievedComponent = dao.getByApplicationIdAndStageTypeIdAndHash(app1, BuildStageType.ID,
        "hash-1");
    assertApplicationComponent(component1, retrievedComponent);
  }

  @Test
  public void testGetByApplicationIdsAndStageTypeIdsSince_AppFiltering() {
    String appId1 = application.getId();
    String appId2 = tempEntity.newApplication(organization.getId()).getId();
    Set<String> largeIdList = new HashSet<>();

    // make a collection of over 2000 ids.
    largeIdList.add(appId1);
    largeIdList.add(appId2);
    for (int i = 0; i < 2000; i++) {
      largeIdList.add(new Integer(i).toString());
    }

    Date date = new Date();
    String componentId1 = tempEntity.newApplicationComponent(appId1, ReleaseStageType.ID, "hash-1",
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
            ComponentIdentifier.createMavenCoordinates("g", "a", "4"), null, MatchState.EXACT, false, date).getId();

    Set<String> stageTypeIds = Collections.singleton(ReleaseStageType.ID);
    List<ApplicationComponent> components = dao.getByApplicationIdsAndStageTypeIdsSince(null, stageTypeIds, date);
    assertThat(components).isEmpty();
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.emptySet(), stageTypeIds, date);
    assertThat(components).isEmpty();
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton("missing"), stageTypeIds, date);
    assertThat(components).isEmpty();
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds, date);
    assertThat(components).extracting(ApplicationComponent::getId).containsExactly(componentId1, componentId2);
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId2), stageTypeIds, date);
    assertThat(components).extracting(ApplicationComponent::getId).containsExactly(componentId3);
    components = dao.getByApplicationIdsAndStageTypeIdsSince(new HashSet<>(Arrays.asList(appId1, appId2)), stageTypeIds,
        date);
    assertThat(components).extracting(ApplicationComponent::getId).containsExactly(componentId1, componentId2,
        componentId3);
    components = dao.getByApplicationIdsAndStageTypeIdsSince(largeIdList, stageTypeIds, date);
    assertThat(components).extracting(ApplicationComponent::getId).containsExactly(componentId1, componentId2,
        componentId3);
  }

  @Test
  public void testGetByApplicationIdsAndStageTypeIdsSince_StageFiltering() {
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
            ComponentIdentifier.createMavenCoordinates("g", "a", "4"), null, MatchState.EXACT, false, date).getId();

    Set<String> appIds = Collections.singleton(application.getId());
    List<ApplicationComponent> components = dao.getByApplicationIdsAndStageTypeIdsSince(appIds, null, date);
    assertThat(components).isEmpty();
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.emptySet(), Collections.emptySet(), date);
    assertThat(components).isEmpty();
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton("missing"),
        Collections.singleton("missing"), date);
    assertThat(components).isEmpty();
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1),
        Collections.singleton(BuildStageType.ID), date);
    assertThat(components).extracting(ApplicationComponent::getId).containsExactly(componentId1);
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1),
        Collections.singleton(ReleaseStageType.ID), date);
    assertThat(components).extracting(ApplicationComponent::getId).containsExactly(componentId2);
    components = dao.getByApplicationIdsAndStageTypeIdsSince(new HashSet<>(Arrays.asList(appId1, appId2)),
        new HashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID)), date);
    assertThat(components).extracting(ApplicationComponent::getId).containsExactly(componentId1, componentId2,
        componentId3);
  }

  @Test
  public void testGetByApplicationIdsAndStageTypeIdsSince_DateFiltering() {
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
    List<ApplicationComponent> components = dao.getByApplicationIdsAndStageTypeIdsSince(appIds, stageTypeIds, null);
    assertThat(components).isEmpty();
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds,
        new Date(date.getTime() + 3000));
    assertThat(components).isEmpty();
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds, date);
    assertThat(components).extracting(ApplicationComponent::getId).containsExactly(componentId1, componentId2);
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds,
        new DateTime(date).minusDays(1).toDate());
    assertThat(components).extracting(ApplicationComponent::getId).containsExactly(componentId1, componentId2);
  }

  @Test
  public void testGetByApplicationIdsAndStageTypeIdsSince_MultipleStages() {
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
    List<ApplicationComponent> components = dao.getByApplicationIdsAndStageTypeIdsSince(appIds, stageIds, date);
    assertThat(components).extracting(ApplicationComponent::getId).containsExactly(componentId1, componentId2,
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
    ApplicationComponent applicationComponent1 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    ApplicationComponent applicationComponent2 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash2", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    tempEntity.newAggregateFile(applicationComponent1.getId(), "hash3", null);
    tempEntity.newAggregateFile(applicationComponent1.getId(), "hash4",
        Sets.newLinkedHashSet(Arrays.asList("pathname1", "pathname2")));
    AggregateFile aggregateFile3 = tempEntity.newAggregateFile(applicationComponent2.getId(), "hash5", null);
    AggregateFile aggregateFile4 = tempEntity.newAggregateFile(applicationComponent2.getId(), "hash6",
        Sets.newLinkedHashSet(Arrays.asList("pathname3", "pathname4")));

    dao.delete(applicationComponent1);

    assertThat(aggregateFileDAO.getByApplicationComponentId(applicationComponent1.getId())).isEmpty();
    assertThat(aggregateFileDAO.getByApplicationComponentId(applicationComponent2.getId()))
        .usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(aggregateFile3, aggregateFile4);
  }

  public void assertApplicationComponent(ApplicationComponent expected, ApplicationComponent actual) {
    assertThat(actual).isNotNull();
    assertThat(actual.getApplicationId()).isEqualTo(expected.getApplicationId());
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
