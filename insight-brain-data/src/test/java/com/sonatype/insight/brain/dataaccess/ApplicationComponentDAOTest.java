/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;

import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

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
}
