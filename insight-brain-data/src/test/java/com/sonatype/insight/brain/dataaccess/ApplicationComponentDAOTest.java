/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.junit.Test;

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
        "groupId", "artifactId", "version", MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(),
        true /* proprietary */, null /* pathnames */);
    dao.insert(appComponent);
    assertThat(appComponent.getId(), notNullValue());

    // Get
    appComponent = dao.getById(appComponent.getId());
    assertThat(appComponent, notNullValue());
    assertApplicationComponent(applicationId, BuildStageType.ID, now, "hash", "groupId", "artifactId", "version",
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), true /* proprietary */, null /* pathnames */,
        appComponent);

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

  private void assertApplicationComponent(String applicationId, String stageTypeId, Date time, String hash,
      String groupId, String artifactId, String version, String matchStateId, String identificationSourceId,
      boolean proprietary, String pathnames, ApplicationComponent actual)
  {
    assertThat(actual.getApplicationId(), is(applicationId));
    assertThat(actual.getStageTypeId(), is(stageTypeId));
    assertThat(actual.getHash(), is(hash));
    assertThat(actual.getTime(), is(time));
    assertThat(actual.getGroupId(), is(groupId));
    assertThat(actual.getArtifactId(), is(artifactId));
    assertThat(actual.getVersion(), is(version));
    assertThat(actual.getMatchStateId(), is(matchStateId));
    assertThat(actual.getIdentificationSourceId(), is(identificationSourceId));
    assertThat(actual.isProprietary(), is(proprietary));
    assertThat(actual.getPathnamesString(), is(pathnames));
  }

  @Test
  public void testGetUniqueCountByApplicationIdsAndStageTypeIds_AppFiltering() {
    String app1 = application.getId();
    String app2 = tempEntity.newApplication(organization.getId()).getId();

    tempEntity.newApplicationComponent(app1, BuildStageType.ID, "hash-1", "g", "a", "1");
    tempEntity.newApplicationComponent(app1, BuildStageType.ID, "hash-2", "g", "a", "2");
    tempEntity.newApplicationComponent(app2, BuildStageType.ID, "hash-1", "g", "a", "1.1");

    Collection<String> stageTypeIds = Arrays.asList(BuildStageType.ID);
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(null, stageTypeIds), is(0));
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(Arrays.<String> asList(), stageTypeIds), is(0));
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(Arrays.asList("missing"), stageTypeIds), is(0));
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(Arrays.asList(app1), stageTypeIds), is(2));
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(Arrays.asList(app2), stageTypeIds), is(1));
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(Arrays.asList(app1, app2), stageTypeIds), is(2));
  }

  @Test
  public void testGetUniqueCountByApplicationIdsAndStageTypeIds_StageFiltering() {
    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "hash-1", "g", "a", "1");
    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "hash-2", "g", "a", "2");
    tempEntity.newApplicationComponent(application.getId(), ReleaseStageType.ID, "hash-1", "g", "a", "1.1");

    Collection<String> appIds = Arrays.asList(application.getId());
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(appIds, null), is(0));
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(appIds, Arrays.<String> asList()), is(0));
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(appIds, Arrays.asList("missing")), is(0));
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(appIds, Arrays.asList(BuildStageType.ID)), is(2));
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(appIds, Arrays.asList(ReleaseStageType.ID)), is(1));
    assertThat(dao.getUniqueCountByApplicationIdsAndStageTypeIds(appIds,
        Arrays.asList(BuildStageType.ID, ReleaseStageType.ID)), is(2));
  }
}
