/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.consumption;

import java.time.Instant;
import java.time.LocalDate;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a consumption event recorded when known components are looked up.
 *
 * @since 1.204
 */
@Entity
@Table(name = "consumption_events")
public class ConsumptionEvent
    implements HasStringId
{
  private static final Logger log = LoggerFactory.getLogger(ConsumptionEvent.class);

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "org_id", nullable = false)
  private String orgId;

  @Column(name = "app_id")
  private String appId;

  @Column(name = "scan_id")
  private String scanId;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "event_timestamp", nullable = false)
  private Instant eventTimestamp;

  @Column(name = "component_count", nullable = false)
  private int componentCount = 1;

  @Column(name = "activity_type", nullable = false)
  private String activityType;

  @Column(name = "source", nullable = false)
  private String source;

  @Column(name = "tier", nullable = false)
  private String tier;

  @Column(name = "billing_month", nullable = false)
  private LocalDate billingMonth;

  public ConsumptionEvent() {
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getOrgId() {
    return orgId;
  }

  public void setOrgId(final String orgId) {
    this.orgId = orgId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(final String appId) {
    this.appId = appId;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(final String scanId) {
    this.scanId = scanId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(final String userId) {
    this.userId = userId;
  }

  public Instant getEventTimestamp() {
    return eventTimestamp;
  }

  public void setEventTimestamp(final Instant eventTimestamp) {
    this.eventTimestamp = eventTimestamp;
  }

  public int getComponentCount() {
    return componentCount;
  }

  public void setComponentCount(final int componentCount) {
    if (componentCount <= 0) {
      throw new IllegalArgumentException("componentCount must be > 0, got: " + componentCount);
    }
    this.componentCount = componentCount;
  }

  public ActivityType getActivityType() {
    if (activityType == null) {
      return ActivityType.OTHERS;
    }
    try {
      return ActivityType.valueOf(activityType);
    }
    catch (IllegalArgumentException e) {
      log.warn("Unknown ActivityType '{}' for ConsumptionEvent id={}; returning OTHERS", activityType, id);
      return ActivityType.OTHERS;
    }
  }

  public void setActivityType(final ActivityType activityType) {
    if (activityType == null) {
      throw new IllegalArgumentException("activityType must not be null");
    }
    if (activityType == ActivityType.OTHERS) {
      throw new IllegalArgumentException("ActivityType.OTHERS is a read-side sentinel; must not be persisted");
    }
    this.activityType = activityType.name();
  }

  public String getActivityTypeRaw() {
    return activityType;
  }

  public void setActivityTypeRaw(final String activityType) {
    this.activityType = activityType;
  }

  public String getSource() {
    return source;
  }

  public void setSource(final String source) {
    this.source = source;
  }

  public String getTier() {
    return tier;
  }

  public void setTier(final String tier) {
    this.tier = tier;
  }

  public LocalDate getBillingMonth() {
    return billingMonth;
  }

  public void setBillingMonth(final LocalDate billingMonth) {
    this.billingMonth = billingMonth;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
  {
    private final ConsumptionEvent event = new ConsumptionEvent();

    public Builder orgId(final String orgId) {
      event.setOrgId(orgId);
      return this;
    }

    public Builder appId(final String appId) {
      event.setAppId(appId);
      return this;
    }

    public Builder scanId(final String scanId) {
      event.setScanId(scanId);
      return this;
    }

    public Builder userId(final String userId) {
      event.setUserId(userId);
      return this;
    }

    public Builder eventTimestamp(final Instant eventTimestamp) {
      event.setEventTimestamp(eventTimestamp);
      return this;
    }

    public Builder componentCount(final int componentCount) {
      event.setComponentCount(componentCount);
      return this;
    }

    public Builder activityType(final ActivityType activityType) {
      event.setActivityType(activityType);
      return this;
    }

    public Builder source(final String source) {
      event.setSource(source);
      return this;
    }

    public Builder tier(final String tier) {
      event.setTier(tier);
      return this;
    }

    public Builder billingMonth(final LocalDate billingMonth) {
      event.setBillingMonth(billingMonth);
      return this;
    }

    public ConsumptionEvent build() {
      return event;
    }
  }
}
