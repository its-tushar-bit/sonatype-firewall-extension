/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Objects;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;

class ProxyAuditData
    extends AuditData
{
  private AuditData auditData;

  public ProxyAuditData(AuditData auditData) {
    this.auditData = auditData;
  }

  @VisibleForTesting
  AuditData getAuditData() {
    return auditData;
  }

  @Override
  protected <F> F continueAsync(Function<AuditData, F> taskSubmitter) {
    F future = taskSubmitter.apply(new ProxyAuditData(auditData));
    auditData = NoopAuditData.INSTANCE;
    return future;
  }

  @Override
  protected AuditData forSubEvent(AuditEvent event, boolean independent, boolean system) {
    Objects.requireNonNull(event);
    return new ProxyAuditData(auditData.forSubEvent(event, independent, system));
  }

  @Override
  public void commit() {
    auditData.commit();
    auditData = NoopAuditData.INSTANCE;
  }

  @Override
  public void commitSubEvents() {
    auditData.commitSubEvents();
  }

  @Override
  public void setUsername(String username) {
    auditData.setUsername(username);
  }

  @Override
  public AuditEvent getEvent() {
    return auditData.getEvent();
  }

  @Override
  public void setEvent(AuditEvent event) {
    auditData.setEvent(event);
  }

  @Override
  public void setError(String error) {
    auditData.setError(error);
  }

  @Override
  public void setException(Throwable error) {
    auditData.setException(error);
  }

  @Override
  public void setHttpStatus(int httpStatus) {
    auditData.setHttpStatus(httpStatus);
  }

  @Override
  public AuditData setData(String key, Object value) {
    auditData.setData(key, value);
    return this;
  }
}
