/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ApiNamespaceConfusionResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testApiNamespaceConfusionResourceAddNamespace() throws Exception {
    HttpResponse response = restRequest().path(ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_PATH)
        .parameter("maven2")
        .body("[ \"org.sonatype\" ]")
        .post();

    assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ADD_PROPRIETARY_COMPONENT_NAMES, null);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_ROOT);
    assertCustomData(auditDTO, "repositoryPublicId", "nsc_maven2");
    assertCustomData(auditDTO, "addedPatternCount", 1);
  }

  @Test
  public void testApiNamespaceConfusionResourceDeleteNamespace() throws Exception {
    HttpResponse response = restRequest().path(ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_PATH)
        .parameter("maven2")
        .delete();

    assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_PROPRIETARY_COMPONENT_NAMES, null);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_ROOT);
    assertCustomData(auditDTO, "repositoryPublicId", "nsc_maven2");
  }
}
