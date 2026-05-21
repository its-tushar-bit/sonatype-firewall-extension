/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiWaiverExpirationNotificationConfigDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MemberAttributeResolver;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.utils.TemplateUtils;
import com.sonatype.insight.brain.webhook.WaiverExpirationEvent;

import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends email notifications for waiver expiration events.
 *
 * <p>
 * Recipients are resolved from the waiver expiration notification config for the
 * waiver's owner org. Depending on {@code recipientType}:
 * <ul>
 * <li>{@code DIRECT} — emails configured directly in the config.</li>
 * <li>{@code ROLE} — members of each configured role at the waiver's owner scope,
 * resolved to email addresses via {@link MembershipMappingDAO} + {@link UserDirectory}.</li>
 * <li>{@code BOTH} — union of direct emails and role member emails.</li>
 * </ul>
 *
 * @since 1.179.0
 */
@Named
public class WaiverExpirationEmailer
{
  private static final Logger log = LoggerFactory.getLogger(WaiverExpirationEmailer.class);

  private static final String TEMPLATE_NAME = "waiver-expiration.ftl";

  private static final String RECIPIENT_TYPE_DIRECT = "DIRECT";

  private static final String RECIPIENT_TYPE_ROLE = "ROLE";

  private static final String RECIPIENT_TYPE_BOTH = "BOTH";

  private static final String STATUS_PREFIX_EXPIRING_IN = "EXPIRING_IN_";

  private static final String STATUS_SUFFIX_DAYS = "_DAYS";

  private static final String UNKNOWN_APPLICATION = "Unknown Application";

  // Template model keys
  private static final String MODEL_WAIVER_ID = "waiverId";

  private static final String MODEL_STATUS = "status";

  private static final String MODEL_DAYS_UNTIL_EXPIRY = "daysUntilExpiry";

  private static final String MODEL_COMPONENT_DISPLAY_NAME = "componentDisplayName";

  private static final String MODEL_COMPONENT_PACKAGE_URL = "componentPackageUrl";

  private static final String MODEL_POLICY_NAME = "policyName";

  private static final String MODEL_THREAT_LEVEL = "threatLevel";

  private static final String MODEL_APPLICATION_NAME = "applicationName";

  private static final String MODEL_CREATOR_USERNAME = "creatorUsername";

  private static final String MODEL_REPORT_URL = "reportUrl";

  private static final String MODEL_EXPIRATION_DATE = "expirationDate";

  private static final String AUDIT_RECIPIENT_EMAIL = "recipientEmail";

  private final InsightMail mail;

  private final AuditRecorder auditRecorder;

  private final MembershipMappingDAO membershipMappingDAO;

  private final UserDirectory userDirectory;

  @Inject
  public WaiverExpirationEmailer(
      final InsightMail mail,
      final AuditRecorder auditRecorder,
      final MembershipMappingDAO membershipMappingDAO,
      final UserDirectory userDirectory)
  {
    this.mail = mail;
    this.auditRecorder = auditRecorder;
    this.membershipMappingDAO = membershipMappingDAO;
    this.userDirectory = userDirectory;
  }

  /**
   * Sends waiver expiration notification emails for the given event.
   * The caller must supply the already-loaded effective config for the waiver's owner org
   * so this method does not need to fetch it again.
   *
   * @param event the waiver expiration event containing all required data
   * @param config the effective notification config for the waiver's owner, already loaded by the caller
   */
  public void send(final WaiverExpirationEvent event, final ApiWaiverExpirationNotificationConfigDTO config) {
    List<String> recipients = resolveRecipients(event, config);

    if (recipients.isEmpty()) {
      log.warn("Skipping waiver expiration email for waiver {} - no recipients configured",
          event.waiverId);
      return;
    }

    String subject = buildSubject(event);
    String body;
    try {
      body = buildBody(event);
    }
    catch (Exception e) {
      log.error("Failed to build email body for waiver {}", event.waiverId, e);
      return;
    }

    for (String recipientEmail : recipients) {
      try (AuditSession auditSession = auditRecorder.recordSystemEvent(
          AuditEvent.NOTIFY_WAIVER_EXPIRATION_EMAIL))
      {
        try {
          AuditData.get()
              .setData(MODEL_WAIVER_ID, event.waiverId)
              .setData(MODEL_STATUS, event.status)
              .setData(AUDIT_RECIPIENT_EMAIL, recipientEmail);

          log.debug("Sending waiver expiration email to {} for waiver {} (status={})",
              recipientEmail, event.waiverId, event.status);

          mail.sendHtml(recipientEmail, subject, body);

          log.info("Sent waiver expiration email to {} for waiver {}", recipientEmail, event.waiverId);
        }
        catch (Exception e) {
          log.error("Failed to send waiver expiration email for waiver {} to {}",
              event.waiverId, recipientEmail, e);
          AuditData.get().setException(e);
        }
      }
    }
  }

  /**
   * Resolves the list of recipient email addresses for the given event using the
   * already-loaded effective config. The config is supplied by the caller to avoid
   * a redundant fetch.
   *
   * @param event the waiver expiration event
   * @param config the effective notification config for the waiver's owner (may be null)
   * @return deduplicated list of recipient email addresses; empty if none configured
   */
  List<String> resolveRecipients(
      final WaiverExpirationEvent event,
      final ApiWaiverExpirationNotificationConfigDTO config)
  {
    if (event.applicationId == null) {
      log.warn("Cannot resolve recipients for waiver {} - applicationId is null", event.waiverId);
      return Collections.emptyList();
    }

    if (config == null) {
      return Collections.emptyList();
    }

    // Use LinkedHashSet to deduplicate while preserving insertion order
    Set<String> emails = new LinkedHashSet<>();

    String recipientType = config.getRecipientType();

    if (RECIPIENT_TYPE_DIRECT.equals(recipientType) || RECIPIENT_TYPE_BOTH.equals(recipientType)) {
      List<String> directEmails = config.getDirectEmails();
      if (directEmails != null) {
        for (String email : directEmails) {
          if (email != null && !email.trim().isEmpty()) {
            emails.add(email.trim());
          }
        }
      }
    }

    if (RECIPIENT_TYPE_ROLE.equals(recipientType) || RECIPIENT_TYPE_BOTH.equals(recipientType)) {
      List<String> roleIds = config.getRoleIds();
      if (roleIds != null) {
        List<String> roleEmails = resolveRoleEmails(event.applicationId, roleIds, event.waiverId);
        emails.addAll(roleEmails);
      }
    }

    return new ArrayList<>(emails);
  }

  /**
   * Resolves email addresses for members of the given roles at the specified owner scope.
   */
  private List<String> resolveRoleEmails(
      final String ownerId,
      final List<String> roleIds,
      final String waiverId)
  {
    List<Member> members = new ArrayList<>();

    for (String roleId : roleIds) {
      if (roleId == null || roleId.isEmpty()) {
        continue;
      }
      try {
        List<MembershipMapping> mappings = membershipMappingDAO.getByContextIdAndRoleId(ownerId, roleId);
        for (MembershipMapping mapping : mappings) {
          if (mapping.getMemberName() != null && mapping.getMemberType() != null) {
            members.add(new Member(mapping.getMemberType(), mapping.getMemberName(), null));
          }
        }
      }
      catch (Exception e) {
        log.error("Failed to load membership mappings for owner {} role {} (waiver {})",
            ownerId, roleId, waiverId, e);
      }
    }

    if (members.isEmpty()) {
      return Collections.emptyList();
    }

    // Resolve attributes (email, displayName) for all members
    MemberAttributeResolver resolver = new MemberAttributeResolver(userDirectory);
    resolver.resolve(members);

    List<String> emails = new ArrayList<>();
    for (Member member : members) {
      if (member.getType() == MemberType.USER) {
        String email = member.getEmail();
        if (email != null && !email.trim().isEmpty()) {
          emails.add(email.trim());
        }
        else {
          log.debug("No email resolved for user {} (waiver {})", member.getInternalName(), waiverId);
        }
      }
    }
    return emails;
  }

  private String buildSubject(final WaiverExpirationEvent event) {
    String componentName = event.componentDisplayName != null
        ? event.componentDisplayName
        : event.componentPackageUrl;
    String appName = event.applicationName != null ? event.applicationName : UNKNOWN_APPLICATION;

    // status is EXPIRING_IN_<N>_DAYS — extract N for a human-readable subject
    int days = parseDaysFromStatus(event.status);
    if (days == 1) {
      return String.format("Waiver Expiring in 24 Hours: %s in %s", componentName, appName);
    }
    return String.format("Waiver Expiring in %d Days: %s in %s", days, componentName, appName);
  }

  /**
   * Parses the day count from a status string of the form {@code EXPIRING_IN_<N>_DAYS}.
   * Returns 1 as a safe default if parsing fails.
   */
  private int parseDaysFromStatus(final String status) {
    if (status != null && status.startsWith(STATUS_PREFIX_EXPIRING_IN) && status.endsWith(STATUS_SUFFIX_DAYS)) {
      try {
        return Integer.parseInt(
            status.substring(STATUS_PREFIX_EXPIRING_IN.length(), status.length() - STATUS_SUFFIX_DAYS.length()));
      }
      catch (NumberFormatException ignored) {
        // fall through to default
      }
    }
    return 1;
  }

  private String buildBody(final WaiverExpirationEvent event) throws IOException {
    Map<String, Object> model = buildTemplateModel(event);
    Template template = TemplateUtils.createFreemarkerConfig().getTemplate(TEMPLATE_NAME);
    return TemplateUtils.render(template, model);
  }

  private Map<String, Object> buildTemplateModel(final WaiverExpirationEvent event) {
    Map<String, Object> model = new HashMap<>();

    model.put(MODEL_WAIVER_ID, event.waiverId);
    model.put(MODEL_STATUS, event.status);
    model.put(MODEL_DAYS_UNTIL_EXPIRY, parseDaysFromStatus(event.status));
    model.put(MODEL_COMPONENT_DISPLAY_NAME,
        event.componentDisplayName != null ? event.componentDisplayName : event.componentPackageUrl);
    model.put(MODEL_COMPONENT_PACKAGE_URL, event.componentPackageUrl);
    model.put(MODEL_POLICY_NAME, event.policyName);
    model.put(MODEL_THREAT_LEVEL, event.threatLevel);
    model.put(MODEL_APPLICATION_NAME,
        event.applicationName != null ? event.applicationName : UNKNOWN_APPLICATION);
    model.put(MODEL_CREATOR_USERNAME, event.creatorUsername);
    model.put(MODEL_REPORT_URL, event.iqReportUrl);

    model.put(MODEL_EXPIRATION_DATE,
        new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH).format(
            Date.from(event.expirationDate.atZone(java.time.ZoneId.systemDefault()).toInstant())));

    return model;
  }
}
