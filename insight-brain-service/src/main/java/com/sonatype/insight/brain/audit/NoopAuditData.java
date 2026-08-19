/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.function.Function;

class NoopAuditData
    extends AuditData
{
  public static final AuditData INSTANCE = new NoopAuditData();

  private NoopAuditData() {
    // hide constructor in favor of singleton
  }

  @Override
  protected <F> F continueAsync(Function<AuditData, F> taskSubmitter) {
    return taskSubmitter.apply(this);
  }

  @Override
  protected AuditData forSubEvent(AuditEvent event, boolean independent, boolean system) {
    return this;
  }

  @Override
  public void commit() {
  }

  @Override
  public void commitSubEvents() {
  }

  @Override
  public void setUsername(String username) {
  }

  @Override
  public AuditEvent getEvent() {
    return null;
  }

  @Override
  public void setEvent(AuditEvent event) {
  }

  @Override
  public void setError(String error) {
  }

  @Override
  public void setException(Throwable error) {
  }

  @Override
  public void setHttpStatus(int httpStatus) {
  }

  @Override
  public AuditData setData(String key, Object value) {
    return this;
  }
}
