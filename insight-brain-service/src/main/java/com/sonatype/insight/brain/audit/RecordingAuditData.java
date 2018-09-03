/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

class RecordingAuditData
    extends AuditData
{
  private final boolean independent;

  private final Consumer<RecordingAuditData> recorder;

  private final List<RecordingAuditData> children = new ArrayList<>();

  private final long timestamp;

  private final RequestData requestData;

  private String username;

  private AuditEvent event;

  private int httpStatus;

  private String error;

  private Throwable exception;

  private Map<String, Object> data = new HashMap<>();

  RecordingAuditData(Consumer<RecordingAuditData> recorder, RequestData requestData) {
    independent = true;
    timestamp = System.currentTimeMillis();
    this.recorder = recorder;
    this.requestData = requestData;
  }

  private RecordingAuditData(RecordingAuditData parent, boolean independent) {
    this.independent = independent;
    timestamp = independent ? System.currentTimeMillis() : parent.timestamp;
    recorder = parent.recorder;
    requestData = parent.requestData;
    username = parent.username;
  }

  @Override
  protected <F> F continueAsync(Function<AuditData, F> taskSubmitter) {
    // should not be called directly on us but rather ProxyAuditData
    throw new UnsupportedOperationException();
  }

  @Override
  protected AuditData forSubEvent(AuditEvent event, boolean independent) {
    RecordingAuditData child = new RecordingAuditData(this, independent);
    child.setEvent(event);
    if (!independent) {
      children.add(child);
    }
    return child;
  }

  @Override
  public void commit() {
    if (independent) {
      recorder.accept(this);
    }
  }

  long getTimestamp() {
    return timestamp;
  }

  RequestData getRequestData() {
    return requestData;
  }

  String getUsername() {
    return username;
  }

  @Override
  public void setUsername(String username) {
    this.username = username;
  }

  AuditEvent getEvent() {
    return event;
  }

  @Override
  public void setEvent(AuditEvent event) {
    this.event = event;
  }

  String getError() {
    return error;
  }

  @Override
  public void setError(String error) {
    this.error = error;
  }

  Throwable getException() {
    return exception;
  }

  @Override
  public void setException(Throwable error) {
    this.exception = error;
  }

  int getHttpStatus() {
    return httpStatus;
  }

  @Override
  public void setHttpStatus(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  Map<String, Object> getData() {
    return data;
  }

  @Override
  public void addData(String key, Object value) {
    data.put(key, value);
  }

  List<RecordingAuditData> getChildren() {
    return children;
  }
}
