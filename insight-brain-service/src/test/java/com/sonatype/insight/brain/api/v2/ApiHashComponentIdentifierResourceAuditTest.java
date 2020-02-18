/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifierDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ApiHashComponentIdentifierResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CLAIM_PATH_V2);
  }

  @Test
  public void testSet() throws Exception {
    ApiHashComponentIdentifierDTO givenDTO = new ApiHashComponentIdentifierDTO(
        new HashComponentIdentifier("hash", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e")));
    ComponentIdentifier componentIdentifier =
        givenDTO.componentIdentifier == null ? null : givenDTO.componentIdentifier.toComponentIdentifier();
    mockComponentSummary(componentIdentifier, ComponentSummary.create(false));

    restRequest().body(givenDTO).post();

    HashComponentIdentifier storedHashComponentIdentifier = new HashComponentIdentifierDAO().getByHash(givenDTO.hash);
    tempEntity.register(storedHashComponentIdentifier);

    assertHashComponentIdentifierData(assertAuditLog(AuditEvent.SET_COMPONENT_IDENTITY, null),
        givenDTO.toHashComponentIdentifier());
  }

  @Test
  public void testSet_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).body(new ApiHashComponentIdentifierDTO()).post();

    assertAuditLog(AuditEvent.SET_COMPONENT_IDENTITY, "unauthorized");
  }

  @Test
  public void testDelete() throws Exception {
    HashComponentIdentifier hashComponentIdentifier =
        tempEntity.newClaimedComponent("hash", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));

    restRequest().path(hashComponentIdentifier.getHash()).delete();

    assertHashComponentIdentifierData(assertAuditLog(AuditEvent.UNSET_COMPONENT_IDENTITY, null),
        hashComponentIdentifier);
  }

  @Test
  public void testDelete_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).path("hash").delete();

    assertAuditLog(AuditEvent.UNSET_COMPONENT_IDENTITY, "unauthorized");
  }

  private void assertHashComponentIdentifierData(AuditDTO auditDTO, HashComponentIdentifier hashComponentIdentifier) {
    assertCustomData(auditDTO, "componentHash", hashComponentIdentifier.getHash());
    assertCustomObject(auditDTO, "componentIdentifier", hashComponentIdentifier.getComponentIdentifier());
    assertCustomData(auditDTO, "comment", hashComponentIdentifier.getComment());
  }
}
