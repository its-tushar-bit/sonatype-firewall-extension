/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiWaiverExpirationNotificationConfigDTO;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.webhook.WaiverExpirationEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WaiverExpirationEmailer.
 *
 * @since 1.179.0
 */
@RunWith(MockitoJUnitRunner.class)
public class WaiverExpirationEmailerTest
{
  @Mock
  private InsightMail mail;

  @Mock
  private AuditRecorder auditRecorder;

  @Mock
  private MembershipMappingDAO membershipMappingDAO;

  @Mock
  private UserDirectory userDirectory;

  private WaiverExpirationEmailer emailer;

  @Before
  public void setUp() {
    emailer = new WaiverExpirationEmailer(mail, auditRecorder, membershipMappingDAO, userDirectory);
  }

  // --- resolveRecipients ---

  @Test
  public void testResolveRecipients_direct_returnsConfiguredEmails() {
    WaiverExpirationEvent event = eventWithOwner("owner-1");
    ApiWaiverExpirationNotificationConfigDTO config = directConfig(List.of("alice@example.com", "bob@example.com"));

    List<String> recipients = emailer.resolveRecipients(event, config);

    assertThat(recipients).containsExactly("alice@example.com", "bob@example.com");
  }

  @Test
  public void testResolveRecipients_nullApplicationId_returnsEmpty() {
    WaiverExpirationEvent event = new WaiverExpirationEvent();
    event.waiverId = "waiver-1";
    event.applicationId = null;
    ApiWaiverExpirationNotificationConfigDTO config = directConfig(List.of("alice@example.com"));

    List<String> recipients = emailer.resolveRecipients(event, config);

    assertThat(recipients).isEmpty();
  }

  @Test
  public void testResolveRecipients_nullConfig_returnsEmpty() {
    WaiverExpirationEvent event = eventWithOwner("owner-1");

    List<String> recipients = emailer.resolveRecipients(event, null);

    assertThat(recipients).isEmpty();
  }

  @Test
  public void testResolveRecipients_direct_deduplicatesEmails() {
    WaiverExpirationEvent event = eventWithOwner("owner-1");
    ApiWaiverExpirationNotificationConfigDTO config =
        directConfig(List.of("alice@example.com", "alice@example.com", "bob@example.com"));

    List<String> recipients = emailer.resolveRecipients(event, config);

    assertThat(recipients).containsExactly("alice@example.com", "bob@example.com");
  }

  @Test
  public void testResolveRecipients_direct_filtersBlankEmails() {
    WaiverExpirationEvent event = eventWithOwner("owner-1");
    ApiWaiverExpirationNotificationConfigDTO config =
        directConfig(List.of("alice@example.com", "", "  "));

    List<String> recipients = emailer.resolveRecipients(event, config);

    assertThat(recipients).containsExactly("alice@example.com");
  }

  @Test
  public void testResolveRecipients_role_emptyMembership_returnsEmpty() {
    WaiverExpirationEvent event = eventWithOwner("owner-1");
    ApiWaiverExpirationNotificationConfigDTO config = roleConfig(List.of("role-1"));

    when(membershipMappingDAO.getByContextIdAndRoleId("owner-1", "role-1"))
        .thenReturn(Collections.emptyList());

    List<String> recipients = emailer.resolveRecipients(event, config);

    assertThat(recipients).isEmpty();
  }

  @Test
  public void testResolveRecipients_both_directEmailsIncluded() {
    // BOTH type: at minimum the direct emails must be included
    WaiverExpirationEvent event = eventWithOwner("owner-1");
    ApiWaiverExpirationNotificationConfigDTO config = new ApiWaiverExpirationNotificationConfigDTO();
    config.setRecipientType("BOTH");
    config.setDirectEmails(List.of("direct@example.com"));
    config.setRoleIds(List.of("role-1"));

    when(membershipMappingDAO.getByContextIdAndRoleId("owner-1", "role-1"))
        .thenReturn(Collections.emptyList());

    List<String> recipients = emailer.resolveRecipients(event, config);

    assertThat(recipients).contains("direct@example.com");
  }

  // --- parseDaysFromStatus (tested via buildSubject behaviour indirectly, and directly via send subject) ---

  @Test
  public void testResolveRecipients_unknownRecipientType_returnsEmpty() {
    WaiverExpirationEvent event = eventWithOwner("owner-1");
    ApiWaiverExpirationNotificationConfigDTO config = new ApiWaiverExpirationNotificationConfigDTO();
    config.setRecipientType("UNKNOWN");
    config.setDirectEmails(List.of("alice@example.com"));

    List<String> recipients = emailer.resolveRecipients(event, config);

    assertThat(recipients).isEmpty();
  }

  // --- helpers ---

  private WaiverExpirationEvent eventWithOwner(String ownerId) {
    WaiverExpirationEvent event = new WaiverExpirationEvent();
    event.waiverId = "waiver-1";
    event.applicationId = ownerId;
    event.status = "EXPIRING_IN_24_HOURS";
    return event;
  }

  private ApiWaiverExpirationNotificationConfigDTO directConfig(List<String> emails) {
    ApiWaiverExpirationNotificationConfigDTO config = new ApiWaiverExpirationNotificationConfigDTO();
    config.setRecipientType("DIRECT");
    config.setDirectEmails(emails);
    return config;
  }

  private ApiWaiverExpirationNotificationConfigDTO roleConfig(List<String> roleIds) {
    ApiWaiverExpirationNotificationConfigDTO config = new ApiWaiverExpirationNotificationConfigDTO();
    config.setRecipientType("ROLE");
    config.setRoleIds(roleIds);
    return config;
  }
}
