/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Collections;
import java.util.HashMap;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;

import org.junit.Before;

import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.convertToHdsUrl;
import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.newComponentDetails;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;

public abstract class AbstractComponentInfoResourceAuditTest
    extends AbstractAuditTest
{
  protected static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("g1",
      "a1", "v1", "", "jar");

  protected static final String COMPONENT_HASH = "hash";

  protected Application application;

  protected MultiLicenseDAO multiLicenseDAO;

  @Before
  public void before() {
    multiLicenseDAO = lookup(MultiLicenseDAO.class);
    application = tempEntity.newApplicationWithParent();
  }

  protected AuditDTO assertAuditComponentInfo(Owner owner, ComponentIdentifier componentIdentifier) {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertOwnerData(auditDTO, owner);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    return auditDTO;
  }

  protected AuditDTO assertAuditComponentInfo(Owner owner, ComponentIdentifier componentIdentifier, String hash) {
    AuditDTO auditDTO = assertAuditComponentInfo(owner, componentIdentifier);
    assertCustomData(auditDTO, "componentHash", hash);
    return auditDTO;
  }

  protected void setupHdsResponseForComponent(final HttpRequest httpRequest) {
    ComponentDetails hdsComponentDetails = newComponentDetails(COMPONENT_IDENTIFIER, multiLicenseDAO);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Collections.singletonList(hdsComponentDetails));
    hdsRespondWith(hdsComponentDetailsList).atUri(convertToHdsUrl(httpRequest.getUrl()));
    hdsRespondWith(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>())).atUri("rest/component/dependencies");
    hdsRespondWith(new VersionScoringService[]{}).atUri(HDS_BULK_SCORE_VERSIONING_PATH);
  }
}
