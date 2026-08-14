/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent.ImportStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlOrganizationImportEventDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlOrganizationImportEventDAO dao;

  private Organization org;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createSourceControlOrganizationImportEventDAO();
    org = tempEntity.newOrganization();
  }

  @Test
  public void testInsert() {
    SourceControlOrganizationImportEvent event = new SourceControlOrganizationImportEvent();
    event.setOrganizationId(org.getId());
    String scmHostUrl = "https://myscm.org/myorg";
    event.setScmHostUrl(scmHostUrl);
    Date startTime = new Date();
    event.setStartTime(startTime);

    dao.insert(event);
    assertThat(event.getId()).isNotNull();

    SourceControlOrganizationImportEvent savedEvent = dao.getById(event.getId());
    assertThat(savedEvent.getOrganizationId()).isEqualTo(org.getId());
    assertThat(savedEvent.getScmHostUrl()).isEqualTo(scmHostUrl);
    assertThat(savedEvent.getImportLimit()).isEqualTo(-1);
    assertThat(savedEvent.getDesiredSubOrganizationCount()).isZero();
    assertThat(savedEvent.getImportStatus()).isEqualTo(ImportStatus.IN_PROGRESS);
    assertThat(savedEvent.getImportSuccessCount()).isZero();
    assertThat(savedEvent.getImportFailureCount()).isZero();
    assertThat(savedEvent.getStartTime()).isEqualTo(startTime);
    assertThat(savedEvent.getImportErrors()).isNull();
  }

  @Test
  public void testUpdate() throws Exception {
    SourceControlOrganizationImportEvent event = tempEntity.newSourceControlOrganizationImportEvent();

    event.setScmHostUrl("https://uodatedhost/org");
    event.setImportLimit(30000);
    event.setDesiredSubOrganizationCount(1000);
    event.setImportErrors("error");
    Date lastUpdatedTime = new Date();
    event.setLastUpdatedTime(lastUpdatedTime);
    event.setImportStatus(ImportStatus.COMPLETE);
    event.setImportSuccessCount(29000);
    event.setImportFailureCount(1000);

    dao.update(event);

    SourceControlOrganizationImportEvent savedEvent = dao.getById(event.getId());
    assertThat(savedEvent.getScmHostUrl()).isEqualTo("https://uodatedhost/org");
    assertThat(savedEvent.getImportLimit()).isEqualTo(30000);
    assertThat(savedEvent.getDesiredSubOrganizationCount()).isEqualTo(1000);
    assertThat(savedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(savedEvent.getImportSuccessCount()).isEqualTo(29000);
    assertThat(savedEvent.getImportFailureCount()).isEqualTo(1000);
    assertThat(savedEvent.getLastUpdatedTime()).isEqualTo(lastUpdatedTime);
    assertThat(savedEvent.getImportErrors()).isEqualTo("error");
  }

  @Test
  public void testDelete() throws Exception {
    SourceControlOrganizationImportEvent event = tempEntity.newSourceControlOrganizationImportEvent();
    dao.delete(event);

    assertThat(dao.getById(event.getId())).isNull();
  }

  @Test
  public void testGetByOrganizationAndEventId() {
    String orgId = tempEntity.newOrganization().getId();
    SourceControlOrganizationImportEvent event =
        tempEntity.newSourceControlOrganizationImportEvent(orgId, "scmUrl", -1, 5);

    SourceControlOrganizationImportEvent retrievedEvent = dao.getByOrganizationAndEventId(orgId, event.getId());
    assertThat(retrievedEvent).isNotNull();
    assertThat(retrievedEvent.getImportStatus()).isEqualTo(event.getImportStatus());
    assertThat(retrievedEvent.getImportLimit()).isEqualTo(event.getImportLimit());
    assertThat(retrievedEvent.getImportSuccessCount()).isEqualTo(event.getImportSuccessCount());
    assertThat(retrievedEvent.getImportFailureCount()).isEqualTo(event.getImportFailureCount());
    assertThat(retrievedEvent.getImportErrors()).isEqualTo(event.getImportErrors());
  }

  @Test
  public void testGetByOrganizationAndEventId_MismatchingIds() {
    String org1Id = tempEntity.newOrganization().getId();
    SourceControlOrganizationImportEvent event1 =
        tempEntity.newSourceControlOrganizationImportEvent(org1Id, "scmUrl", -1, 5);
    String org2Id = tempEntity.newOrganization().getId();
    SourceControlOrganizationImportEvent event2 =
        tempEntity.newSourceControlOrganizationImportEvent(org2Id, "scmUrl", -1, 5);

    SourceControlOrganizationImportEvent retrievedEvent = dao.getByOrganizationAndEventId(org2Id, event1.getId());
    assertThat(retrievedEvent).isNull();

    retrievedEvent = dao.getByOrganizationAndEventId(org1Id, event2.getId());
    assertThat(retrievedEvent).isNull();
  }

  @Test
  public void testGetByOrganizationId() {
    String org1Id = tempEntity.newOrganization().getId();
    String org2Id = tempEntity.newOrganization().getId();
    tempEntity.newSourceControlOrganizationImportEvent(org1Id, "scmUrl1", -1, 1);
    tempEntity.newSourceControlOrganizationImportEvent(org1Id, "scmUrl2", -1, 2);
    tempEntity.newSourceControlOrganizationImportEvent(org2Id, "scmUrl3", -1, 3);

    List<SourceControlOrganizationImportEvent> events = dao.getByOrganizationId(org1Id);
    assertThat(events).hasSize(2);
    assertThat(events.stream().map(SourceControlOrganizationImportEvent::getOrganizationId)).containsOnly(org1Id);

    events = dao.getByOrganizationId(org2Id);
    assertThat(events).hasSize(1);
    assertThat(events.stream().map(SourceControlOrganizationImportEvent::getOrganizationId)).containsOnly(org2Id);
  }
}
