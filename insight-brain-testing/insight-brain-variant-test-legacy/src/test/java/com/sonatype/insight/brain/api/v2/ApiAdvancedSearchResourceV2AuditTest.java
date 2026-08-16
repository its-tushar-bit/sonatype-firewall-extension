/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.variant.LegacyServerTest;

import org.junit.jupiter.api.Test;

@LegacyServerTest
public class ApiAdvancedSearchResourceV2AuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ADVANCED_SEARCH_RESOURCE_PATH_V2);
  }

  @Test
  public void testSearchIndex() throws Exception {
    getCLMServer().getInstance(IndexService.class).createSearchIndex();
    String query = "organizationId:" + Organization.ROOT_ORGANIZATION_ID;
    restRequest().query("query", query).query("pageSize", "123").get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.PERFORM_ADVANCED_SEARCH, null);
    assertCustomData(auditDTO, "searchQuery", query);
    assertCustomData(auditDTO, "searchPageSize", 123);
    assertCustomData(auditDTO, "searchPageIndex", 0);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }
}
