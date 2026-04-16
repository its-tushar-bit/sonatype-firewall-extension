/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.time.LocalDateTime;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;

/**
 * Event fired when a waiver expires in IQ Server.
 * This event is consumed by the webhook system to notify external systems about waiver expirations.
 *
 * @since 1.178.0
 */
public class WaiverExpirationEvent
    extends WebhookEvent
{
  /**
   * Timestamp when the waiver expired
   */
  public LocalDateTime timestamp;

  /**
   * Unique identifier for the waiver that expired
   */
  public String waiverId;

  /**
   * Date/time when the waiver expired
   */
  public LocalDateTime expirationDate;

  /**
   * Comment/justification provided when the waiver was created
   */
  public String comment;

  /**
   * Username of the person who created the waiver
   */
  public String creatorUsername;

  /**
   * Email of the person who created the waiver
   */
  public String creatorEmail;

  /**
   * Package URL (PURL) of the component associated with the waiver
   */
  public String componentPackageUrl;

  /**
   * Format of the component (e.g., maven, npm, pypi)
   */
  public String componentFormat;

  /**
   * Display name of the component
   */
  public String componentDisplayName;

  /**
   * Policy ID that was waived
   */
  public String policyId;

  /**
   * Name of the policy that was waived
   */
  public String policyName;

  /**
   * Threat level of the policy (0-10)
   */
  public Integer threatLevel;

  /**
   * Application ID where the waiver was granted
   */
  public String applicationId;

  /**
   * Application public ID
   */
  public String applicationPublicId;

  /**
   * Application name
   */
  public String applicationName;

  /**
   * Link to the IQ Server report showing the waiver details
   */
  public String iqReportUrl;

  /**
   * Status of the waiver expiration:
   * - "EXPIRING_IN_7_DAYS": Waiver will expire in 7 days (advance warning)
   * - "EXPIRING_IN_24_HOURS": Waiver will expire in 24 hours (final warning)
   */
  public String status;

  @Override
  public String toString() {
    String jsonifiedFields = String.format(
        "{waiverId='%s',expirationDate='%s',componentPackageUrl='%s',policyId='%s',applicationPublicId='%s'}",
        waiverId,
        expirationDate != null ? expirationDate.toString() : "null",
        componentPackageUrl,
        policyId,
        applicationPublicId);
    return getClass().getName() + jsonifiedFields;
  }
}
