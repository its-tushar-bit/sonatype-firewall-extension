/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.logging.Level;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Named
@Singleton
public class MultiTenantAuditRecorder
    extends AuditRecorder
{
  @Inject
  public MultiTenantAuditRecorder(ErrorResponseGenerator errorResponseGenerator) {
    super(errorResponseGenerator);
  }

  @Override
  ObjectNode toObjectNode(RecordingAuditData recordingAuditData, String error) {
    return AUDIT_OBJECT_MAPPER.valueToTree(new MultiTenantAuditDTO(recordingAuditData, error));
  }

  private static class MultiTenantAuditDTO
      extends AuditDTO
  {
    public final MdcTenant mdc;

    public final String level;

    public final String logType;

    public final String message;

    public MultiTenantAuditDTO(RecordingAuditData recordingAuditData, String error) {
      super(recordingAuditData, error);

      this.mdc = new MdcTenant(TenantThreadLocal.getTenant().tenantSlug);
      this.level = Level.INFO.getName();
      this.logType = "AuditLog";
      this.message = String.format("Audit event[Domain=%s, Type=%s]", domain, type);
    }

    private static class MdcTenant
    {
      public final String tenant;

      public MdcTenant(String tenant) {
        this.tenant = tenant;
      }
    }
  }
}
