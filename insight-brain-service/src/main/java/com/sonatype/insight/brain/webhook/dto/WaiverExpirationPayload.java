/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import java.time.ZoneId;
import java.util.Date;

import com.sonatype.insight.brain.webhook.WaiverExpirationEvent;

/**
 * Webhook payload for waiver expiration events.
 *
 * @since 1.178.0
 */
public class WaiverExpirationPayload
    extends WebhookPayload
{
  /**
   * Event type identifier
   */
  public String eventType;

  /**
   * Application information
   */
  public ApplicationInfo application;

  /**
   * Component information
   */
  public ComponentInfo component;

  /**
   * Policy information
   */
  public PolicyInfo policy;

  /**
   * Waiver information
   */
  public WaiverInfo waiver;

  /**
   * Link to the IQ Server report showing the waiver details
   */
  public String reportUrl;

  /**
   * Status of the waiver expiration:
   * - "EXPIRING_IN_7_DAYS": Waiver will expire in 7 days (advance warning)
   * - "EXPIRING_IN_24_HOURS": Waiver will expire in 24 hours (final warning)
   */
  public String status;

  /**
   * Nested class for application information
   */
  public static class ApplicationInfo
  {
    public String id;

    public String name;

    public String publicId;

    public ApplicationInfo() {
    }

    public ApplicationInfo(String id, String name, String publicId) {
      this.id = id;
      this.name = name;
      this.publicId = publicId;
    }
  }

  /**
   * Nested class for component information
   */
  public static class ComponentInfo
  {
    public String packageUrl;

    public String format;

    public String displayName;

    public ComponentInfo() {
    }

    public ComponentInfo(String packageUrl, String format, String displayName) {
      this.packageUrl = packageUrl;
      this.format = format;
      this.displayName = displayName;
    }
  }

  /**
   * Nested class for policy information
   */
  public static class PolicyInfo
  {
    public String id;

    public String name;

    public Integer threatLevel;

    public PolicyInfo() {
    }

    public PolicyInfo(String id, String name, Integer threatLevel) {
      this.id = id;
      this.name = name;
      this.threatLevel = threatLevel;
    }
  }

  /**
   * Nested class for waiver information
   */
  public static class WaiverInfo
  {
    public String id;

    public Date expirationDate;

    public String comment;

    public String creatorUsername;

    public String creatorEmail;

    public WaiverInfo() {
    }

    public WaiverInfo(String id, Date expirationDate, String comment, String creatorUsername, String creatorEmail) {
      this.id = id;
      this.expirationDate = expirationDate;
      this.comment = comment;
      this.creatorUsername = creatorUsername;
      this.creatorEmail = creatorEmail;
    }
  }

  /**
   * Default constructor for Jackson serialization
   */
  public WaiverExpirationPayload() {
  }

  /**
   * Conversion constructor from WaiverExpirationEvent
   *
   * @param event the internal event to convert
   */
  public WaiverExpirationPayload(WaiverExpirationEvent event) {
    this.timestamp =
        event.timestamp != null ? Date.from(event.timestamp.atZone(ZoneId.systemDefault()).toInstant()) : null;
    this.initiator = event.initiator != null ? event.initiator : "SYSTEM";
    this.eventType = "iq:waiverExpiration";

    // Application info
    this.application = new ApplicationInfo(
        event.applicationId,
        event.applicationName,
        event.applicationPublicId);

    // Component info
    this.component = new ComponentInfo(
        event.componentPackageUrl,
        event.componentFormat,
        event.componentDisplayName);

    // Policy info
    this.policy = new PolicyInfo(
        event.policyId,
        event.policyName,
        event.threatLevel);

    // Waiver info
    this.waiver = new WaiverInfo(
        event.waiverId,
        event.expirationDate != null
            ? Date.from(event.expirationDate.atZone(ZoneId.systemDefault()).toInstant())
            : null,
        event.comment,
        event.creatorUsername,
        event.creatorEmail);

    this.reportUrl = event.iqReportUrl;
    this.status = event.status;
  }
}
