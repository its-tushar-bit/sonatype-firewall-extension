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
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ApplicationComponentDAOTest
    extends AbstractDbDAOTest
{
  private ApplicationComponentDAO dao = new ApplicationComponentDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    Date now = new Date();
    ApplicationComponent appComponent = new ApplicationComponent(applicationId, BuildStageType.ID, now, "hash",
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), true /* proprietary */, null /* pathnames */);
    dao.insert(appComponent);
    assertThat(appComponent.getId(), notNullValue());

    // Get
    appComponent = dao.getById(appComponent.getId());
    assertThat(appComponent, notNullValue());
    assertApplicationComponent(applicationId, BuildStageType.ID, now, "hash",
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), true /* proprietary */, null /* pathnames */, appComponent);

    // Update
    try {
      dao.update(appComponent);
      fail("Expected UnsupportedOperationException");
    }
    catch (UnsupportedOperationException expected) {
    }

    // Delete
    dao.delete(appComponent);

    // Get
    appComponent = dao.getById(appComponent.getId());
    assertThat(appComponent, nullValue());
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
    assertThat(actual.getApplicationId(), is(applicationId));
    assertThat(actual.getStageTypeId(), is(stageTypeId));
    assertThat(actual.getHash(), is(hash));
    assertThat(actual.getTime(), is(time));
    assertThat(actual.getComponentIdentifier(), is(componentIdentifier));
    assertThat(actual.getMatchStateId(), is(matchStateId));
    assertThat(actual.getIdentificationSourceId(), is(identificationSourceId));
    assertThat(actual.isProprietary(), is(proprietary));
    assertThat(actual.getPathnamesString(), is(pathnames));
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
    assertThat(components, hasSize(0));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.<String> emptySet(), stageTypeIds, date);
    assertThat(components, hasSize(0));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton("missing"), stageTypeIds, date);
    assertThat(components, hasSize(0));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds, date);
    assertThat(components, hasSize(2));
    assertThat(components.get(0).getId(), is(componentId1));
    assertThat(components.get(1).getId(), is(componentId2));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId2), stageTypeIds, date);
    assertThat(components, hasSize(1));
    assertThat(components.get(0).getId(), is(componentId3));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(new HashSet<>(Arrays.asList(appId1, appId2)), stageTypeIds,
        date);
    assertThat(components, hasSize(3));
    assertThat(components.get(0).getId(), is(componentId1));
    assertThat(components.get(1).getId(), is(componentId2));
    assertThat(components.get(2).getId(), is(componentId3));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(largeIdList, stageTypeIds, date);
    assertThat(components, hasSize(3));
    assertThat(components.get(0).getId(), is(componentId1));
    assertThat(components.get(1).getId(), is(componentId2));
    assertThat(components.get(2).getId(), is(componentId3));
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
    assertThat(components, hasSize(0));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.<String> emptySet(),
        Collections.<String> emptySet(), date);
    assertThat(components, hasSize(0));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton("missing"),
        Collections.singleton("missing"), date);
    assertThat(components, hasSize(0));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1),
        Collections.singleton(BuildStageType.ID), date);
    assertThat(components, hasSize(1));
    assertThat(components.get(0).getId(), is(componentId1));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1),
        Collections.singleton(ReleaseStageType.ID), date);
    assertThat(components, hasSize(1));
    assertThat(components.get(0).getId(), is(componentId2));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(new HashSet<>(Arrays.asList(appId1, appId2)),
        new HashSet<>(Arrays.asList(BuildStageType.ID, ReleaseStageType.ID)), date);
    assertThat(components, hasSize(3));
    assertThat(components.get(0).getId(), is(componentId1));
    assertThat(components.get(1).getId(), is(componentId2));
    assertThat(components.get(2).getId(), is(componentId3));
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
    assertThat(components, hasSize(0));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds,
        new Date(date.getTime() + 3000));
    assertThat(components, hasSize(0));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds, date);
    assertThat(components, hasSize(2));
    assertThat(components.get(0).getId(), is(componentId1));
    assertThat(components.get(1).getId(), is(componentId2));
    components = dao.getByApplicationIdsAndStageTypeIdsSince(Collections.singleton(appId1), stageTypeIds,
        new DateTime(date).minusDays(1).toDate());
    assertThat(components, hasSize(2));
    assertThat(components.get(0).getId(), is(componentId1));
    assertThat(components.get(1).getId(), is(componentId2));
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
    assertThat(components, hasSize(3));
    assertThat(components.get(0).getId(), is(componentId1));
    assertThat(components.get(1).getId(), is(componentId2));
    assertThat(components.get(2).getId(), is(componentId3));
  }

  public void assertApplicationComponent(ApplicationComponent expected, ApplicationComponent actual) {
    assertThat(actual, notNullValue());
    assertThat(actual.getApplicationId(), is(expected.getApplicationId()));
    assertThat(actual.getHash(), is(expected.getHash()));
    assertThat(actual.getId(), is(expected.getId()));
    assertThat(actual.getIdentificationSourceId(), is(expected.getIdentificationSourceId()));
    assertThat(actual.getStageTypeId(), is(expected.getStageTypeId()));
    assertThat(actual.getMatchStateId(), is(expected.getMatchStateId()));
    assertThat(actual.isProprietary(), is(expected.isProprietary()));
    assertThat(actual.getPathnames(), is(expected.getPathnames()));
    assertThat(actual.getPathnamesString(), is(expected.getPathnamesString()));
  }
}
