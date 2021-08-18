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
import java.util.concurrent.TimeUnit;

import javax.persistence.Query;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.postgres.PostgresServer;

import com.google.common.collect.Sets;
import org.awaitility.Awaitility;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ApplicationComponentDAOTest
    extends AbstractDbDAOTest
{
  private ApplicationComponentDAO dao = new ApplicationComponentDAO();

  private AggregateFileDAO aggregateFileDAO = new AggregateFileDAO();

  private ApplicationComponentLicenseDAO applicationComponentLicenseDAO = new ApplicationComponentLicenseDAO();

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
  public void testGetByApplicationIdsAndStageTypeIdsSince_AppFiltering_H2() {
    testGetByApplicationIdsAndStageTypeIdsSince_AppFiltering(true);
  }

  @Test
  public void testGetByApplicationIdsAndStageTypeIdsSince_AppFiltering_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testGetByApplicationIdsAndStageTypeIdsSince_AppFiltering(false);
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  public void testGetByApplicationIdsAndStageTypeIdsSince_AppFiltering(boolean isDatabaseEmbedded) {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    String appId1 = application.getId();
    String appId2 = tempEntity.newApplication(organization.getId()).getId();
    Set<String> largeIdList = new HashSet<>();

    int threshold = isDatabaseEmbedded ?
        ApplicationComponentDAO.H2_IN_OPERATOR_THRESHOLD : ApplicationComponentDAO.POSTGRES_IN_OPERATOR_THRESHOLD;
    // make a collection of over 2000 ids.
    largeIdList.add(appId1);
    largeIdList.add(appId2);
    for (int i = 0; i < threshold; i++) {
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

  @Test
  public void testCascadeDeleteToApplicationComponentLicense() {
    ApplicationComponent applicationComponent1 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    ApplicationComponent applicationComponent2 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));

    tempEntity.newApplicationComponentLicense(applicationComponent1.getId(), "license-1");
    tempEntity.newApplicationComponentLicense(applicationComponent1.getId(), "license-2");

    ApplicationComponentLicense applicationComponentLicense3 =
        tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-3");
    ApplicationComponentLicense applicationComponentLicense4 =
        tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-4");

    dao.delete(applicationComponent1);

    assertThat(applicationComponentLicenseDAO.getByApplicationComponentId(applicationComponent1.getId())).isEmpty();
    assertThat(applicationComponentLicenseDAO.getByApplicationComponentId(applicationComponent2.getId()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(applicationComponentLicense3, applicationComponentLicense4);
  }

  @Test
  public void testGetApplicationIdsAndStageTypeIdsByLicenses() {
    ApplicationComponent applicationComponent1 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    tempEntity.newApplicationComponentLicense(applicationComponent1.getId(), "Apache-1.0");

    Application otherApplication = tempEntity.newApplication(organization.getId());

    ApplicationComponent applicationComponent2 = tempEntity.newApplicationComponent(otherApplication.getId(),
        BuildStageType.ID, "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "Apache-1.0");

    ApplicationComponent applicationComponent3 = tempEntity.newApplicationComponent(application.getId(),
        DevelopStageType.ID, "hash3", ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newApplicationComponentLicense(applicationComponent3.getId(), "Apache-2.0");

    ApplicationComponent applicationComponent4 = tempEntity.newApplicationComponent(otherApplication.getId(),
        DevelopStageType.ID, "hash4", ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newApplicationComponentLicense(applicationComponent4.getId(), "Apache-2.0");

    ApplicationComponent applicationComponent5 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash5", ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"));
    tempEntity.newApplicationComponentLicense(applicationComponent5.getId(), "MIT");
    tempEntity.newLicenseOverride(application.getId(), applicationComponent5.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, Sets.newHashSet("Apache-1.0"));

    Application otherApplicationForOrg = tempEntity.newApplicationWithParent();

    ApplicationComponent applicationComponent6 = tempEntity.newApplicationComponent(otherApplicationForOrg.getId(),
        DevelopStageType.ID, "hash6", ComponentIdentifier.createMavenCoordinates("g6", "a6", "v6"));
    tempEntity.newApplicationComponentLicense(applicationComponent6.getId(), "MIT");
    tempEntity.newLicenseOverride(otherApplicationForOrg.getOrganizationId(),
        applicationComponent6.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, Sets.newHashSet("Apache-1.0"));

    Application otherApplicationForRootOrg = tempEntity.newApplication(organization.getId());

    ApplicationComponent applicationComponent7 = tempEntity.newApplicationComponent(otherApplicationForRootOrg.getId(),
        BuildStageType.ID, "hash7", ComponentIdentifier.createMavenCoordinates("g7", "a7", "v7"));
    tempEntity.newApplicationComponentLicense(applicationComponent7.getId(), "MIT");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID, applicationComponent7.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, Sets.newHashSet("Apache-2.0"));

    Application oneMoreApplication = tempEntity.newApplication(organization.getId());
    ApplicationComponent applicationComponent8 = tempEntity.newApplicationComponent(oneMoreApplication.getId(),
        BuildStageType.ID, "hash8", ComponentIdentifier.createMavenCoordinates("g8", "a8", "v8"));
    tempEntity.newApplicationComponentLicense(applicationComponent8.getId(), "Apache-1.0");

    ApplicationComponent applicationComponent9 = tempEntity.newApplicationComponent(application.getId(),
        ReleaseStageType.ID, "hash9", ComponentIdentifier.createMavenCoordinates("g9", "a9", "v9"));
    tempEntity.newApplicationComponentLicense(applicationComponent9.getId(), "Apache-2.0");

    ApplicationComponent applicationComponent10 = tempEntity.newApplicationComponent(otherApplication.getId(),
        BuildStageType.ID, "hash10", ComponentIdentifier.createMavenCoordinates("g10", "a10", "v10"));
    tempEntity.newApplicationComponentLicense(applicationComponent10.getId(), "MIT");

    List<Object[]> result =
        dao.getApplicationIdsAndStageTypeIdsByLicenses(
            Sets.newHashSet(application.getId(), otherApplication.getId(), otherApplicationForOrg.getId(),
                otherApplicationForRootOrg.getId()),
            Sets.newHashSet(BuildStageType.ID, DevelopStageType.ID), Sets.newHashSet("Apache-1.0", "Apache-2.0"));

    assertThat(result)
        .isNotEmpty()
        .containsExactlyInAnyOrder(
            new Object[]{application.getId(), BuildStageType.ID},
            new Object[]{otherApplication.getId(), BuildStageType.ID},
            new Object[]{application.getId(), DevelopStageType.ID},
            new Object[]{otherApplication.getId(), DevelopStageType.ID},
            new Object[]{otherApplicationForOrg.getId(), DevelopStageType.ID},
            new Object[]{otherApplicationForRootOrg.getId(), BuildStageType.ID});
  }

  @Test
  public void testBuildPositionalParameters() {
    List<String> list = Arrays.asList("a");
    assertThat(dao.buildPositionalParameters(list, 1)).isEqualTo("(?1)");

    list = Arrays.asList("a", "b");
    assertThat(dao.buildPositionalParameters(list, 1)).isEqualTo("(?1,?2)");

    list = Arrays.asList("a", "b", "c");
    assertThat(dao.buildPositionalParameters(list, 1)).isEqualTo("(?1,?2,?3)");

    list = Arrays.asList("d");
    assertThat(dao.buildPositionalParameters(list, 4)).isEqualTo("(?4)");

    list = Arrays.asList("d", "e", "f");
    assertThat(dao.buildPositionalParameters(list, 4)).isEqualTo("(?4,?5,?6)");

    list = Arrays.asList("x", "y", "z");
    assertThat(dao.buildPositionalParameters(list, 24)).isEqualTo("(?24,?25,?26)");
  }

  @Test
  public void testAddPositionalParameters() {
    List<String> list = Arrays.asList("a");
    Query query = mock(Query.class);
    dao.addPositionalParameters(query, list, 1);
    verify(query).setParameter(1, "a");
    verifyNoMoreInteractions(query);

    list = Arrays.asList("a", "b");
    query = mock(Query.class);
    dao.addPositionalParameters(query, list, 1);
    verify(query).setParameter(1, "a");
    verify(query).setParameter(2, "b");
    verifyNoMoreInteractions(query);

    list = Arrays.asList("a", "b", "c");
    query = mock(Query.class);
    dao.addPositionalParameters(query, list, 1);
    verify(query).setParameter(1, "a");
    verify(query).setParameter(2, "b");
    verify(query).setParameter(3, "c");
    verifyNoMoreInteractions(query);

    list = Arrays.asList("d");
    query = mock(Query.class);
    dao.addPositionalParameters(query, list, 4);
    verify(query).setParameter(4, "d");
    verifyNoMoreInteractions(query);

    list = Arrays.asList("d", "e", "f");
    query = mock(Query.class);
    dao.addPositionalParameters(query, list, 4);
    verify(query).setParameter(4, "d");
    verify(query).setParameter(5, "e");
    verify(query).setParameter(6, "f");
    verifyNoMoreInteractions(query);

    list = Arrays.asList("x", "y", "z");
    query = mock(Query.class);
    dao.addPositionalParameters(query, list, 24);
    verify(query).setParameter(24, "x");
    verify(query).setParameter(25, "y");
    verify(query).setParameter(26, "z");
    verifyNoMoreInteractions(query);
  }

  @Test
  public void testGetApplicationIdsAndStageTypeIdsByReviewStatus() {
    ApplicationComponent applicationComponent1 = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    tempEntity.newComponentObligation(applicationComponent1.getComponentIdentifier(),
        applicationComponent1.getApplicationId(), "obligation1", "comment1", ObligationStatus.FULFILLED, "hash1");

    Application otherApplication = tempEntity.newApplication(organization.getId());

    ApplicationComponent applicationComponent2 = tempEntity.newApplicationComponent(otherApplication.getId(),
        BuildStageType.ID, "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newComponentObligation(applicationComponent2.getComponentIdentifier(),
        applicationComponent2.getApplicationId(), "obligation2", "comment2", ObligationStatus.IGNORED, "hash2");

    ApplicationComponent applicationComponent3 = tempEntity.newApplicationComponent(application.getId(),
        DevelopStageType.ID, "hash3", ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newComponentObligation(applicationComponent3.getComponentIdentifier(),
        applicationComponent3.getApplicationId(), "obligation3", "comment3", ObligationStatus.OPEN, "hash3");

    ApplicationComponent applicationComponent4 = tempEntity.newApplicationComponent(otherApplication.getId(),
        DevelopStageType.ID, "hash4", ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newComponentObligation(applicationComponent4.getComponentIdentifier(),
        applicationComponent4.getApplicationId(), "obligation4", "comment4", ObligationStatus.FLAGGED, "hash4");

    Application applicationNewParent = tempEntity.newApplicationWithParent();

    ApplicationComponent applicationComponent5 = tempEntity.newApplicationComponent(applicationNewParent.getId(),
        BuildStageType.ID, "hash5", ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"));
    tempEntity.newComponentObligation(applicationComponent5.getComponentIdentifier(),
        applicationNewParent.getOrganizationId(),
        "obligation5", "comment5", ObligationStatus.FULFILLED, "hash5");

    Application applicationForRoot = tempEntity.newApplicationWithParent();

    ApplicationComponent applicationComponent6 = tempEntity.newApplicationComponent(applicationForRoot.getId(),
        DevelopStageType.ID, "hash6", ComponentIdentifier.createMavenCoordinates("g6", "a6", "v6"));
    tempEntity.newComponentObligation(applicationComponent6.getComponentIdentifier(), Organization.ROOT_ORGANIZATION_ID,
        "obligation6", "comment6", ObligationStatus.IGNORED, "hash6");

    Application oneMoreApplication = tempEntity.newApplication(organization.getId());
    ApplicationComponent applicationComponent7 = tempEntity.newApplicationComponent(oneMoreApplication.getId(),
        BuildStageType.ID, "hash7", ComponentIdentifier.createMavenCoordinates("g7", "a7", "v7"));
    tempEntity.newComponentObligation(applicationComponent7.getComponentIdentifier(),
        applicationComponent7.getApplicationId(), "obligation7", "comment7", ObligationStatus.IGNORED, "hash7");

    ApplicationComponent applicationComponent8 = tempEntity.newApplicationComponent(application.getId(),
        ReleaseStageType.ID, "hash8", ComponentIdentifier.createMavenCoordinates("g8", "a8", "v8"));
    tempEntity.newComponentObligation(applicationComponent7.getComponentIdentifier(),
        applicationComponent8.getApplicationId(), "obligation8", "comment8", ObligationStatus.FULFILLED, "hash8");

    Application applicationWithoutReview = tempEntity.newApplication(organization.getId());
    tempEntity.newApplicationComponent(applicationWithoutReview.getId(), DevelopStageType.ID, "hash9",
        ComponentIdentifier.createMavenCoordinates("g9", "a9", "v9"));

    List<Object[]> result = dao.getApplicationIdsAndStageTypeIdsByReviewStatus(
        Sets.newHashSet(application.getId(), otherApplication.getId(), applicationNewParent.getId(),
            applicationForRoot.getId(), applicationWithoutReview.getId()),
        Sets.newHashSet(BuildStageType.ID, DevelopStageType.ID),
        true);

    assertThat(result).isNotEmpty().containsExactlyInAnyOrder(
        new Object[]{application.getId(), BuildStageType.ID},
        new Object[]{otherApplication.getId(), BuildStageType.ID},
        new Object[]{application.getId(), DevelopStageType.ID},
        new Object[]{otherApplication.getId(), DevelopStageType.ID},
        new Object[]{applicationNewParent.getId(), BuildStageType.ID},
        new Object[]{applicationForRoot.getId(), DevelopStageType.ID});

    result = dao.getApplicationIdsAndStageTypeIdsByReviewStatus(
        Sets.newHashSet(application.getId(), otherApplication.getId(), applicationNewParent.getId(),
            applicationForRoot.getId(), applicationWithoutReview.getId()),
        Sets.newHashSet(BuildStageType.ID, DevelopStageType.ID),
        false);

    assertThat(result).isNotEmpty().containsExactlyInAnyOrder(
        new Object[]{applicationWithoutReview.getId(), DevelopStageType.ID});
  }

  @Test
  public void testGetByApplicationIdAndComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    ApplicationComponent applicationComponent1 =
        tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash1", componentIdentifier);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash1", componentIdentifier);
    ApplicationComponent applicationComponent3 =
        tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash1", componentIdentifier);
    ApplicationComponent applicationComponent4 =
        tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash2", componentIdentifier);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash3",
        componentIdentifier.createAlternativeVersion("v2"));

    assertThat(dao.getByApplicationIdAndComponentIdentifier(app1.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(applicationComponent1, applicationComponent3, applicationComponent4);
  }

  @Test
  public void testLastByComponentIdentifier() throws InterruptedException {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app = tempEntity.newApplicationWithParent();

    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash1", componentIdentifier);
    Awaitility.await().timeout(10, TimeUnit.MILLISECONDS);
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, "hash1", componentIdentifier);
    Awaitility.await().timeout(10, TimeUnit.MILLISECONDS);
    ApplicationComponent applicationComponent3 =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash2", componentIdentifier);

    ApplicationComponent appComponent = dao.getLastByComponentIdentifier(componentIdentifier);

    assertApplicationComponent(applicationComponent3, appComponent);
  }

  @Test
  public void testGetByApplicationIdAndComponentIdentifier_Empty() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app = tempEntity.newApplicationWithParent();

    assertThat(dao.getByApplicationIdAndComponentIdentifier(app.getId(), componentIdentifier)).isEmpty();
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
