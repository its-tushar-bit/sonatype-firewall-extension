/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.firewall.RenewWaiversResponseDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import com.sonatype.insight.brain.security.SecurityAspectControl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiFirewallRenewWaiverServiceTest
{
  private static final String WAIVER_ID_1 = "waiver-1";

  private static final String WAIVER_ID_2 = "waiver-2";

  private static final String USERNAME = "testuser";

  private static final String COMMENT = "Test renewal comment";

  private static final String REASON_ID = "reason-123";

  private static final String INTERNAL_OWNER_ID = "org-internal-1";

  @Mock
  private PolicyWaiverDAO policyWaiverDAO;

  @Mock
  private OwnerDAO ownerDAO;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private TransactionContext transactionContext;

  @Mock
  private PolicyWaiver waiver1;

  @Mock
  private PolicyWaiver waiver2;

  @Mock
  private Owner mockOwner;

  private ApiFirewallRenewWaiverService service;

  @Before
  public void setUp() {
    SecurityAspectControl.disableEnforcement();
    service = new ApiFirewallRenewWaiverService(policyWaiverDAO, ownerDAO, currentUser);

    when(currentUser.getUsername()).thenReturn(USERNAME);
    when(policyWaiverDAO.createTransactionContext()).thenReturn(transactionContext);
    doNothing().when(transactionContext).begin();
    doNothing().when(transactionContext).commit();
    doNothing().when(transactionContext).close();

    when(mockOwner.getId()).thenReturn(INTERNAL_OWNER_ID);
    when(mockOwner.getType()).thenReturn(OwnerType.REPOSITORY);
  }

  @After
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
  }

  private void mockWaiverFound(PolicyWaiver waiver, String waiverId) {
    when(waiver.getOwnerId()).thenReturn(INTERNAL_OWNER_ID);
    when(policyWaiverDAO.getByIds(Set.of(waiverId))).thenReturn(List.of(waiver));
    when(ownerDAO.getById(INTERNAL_OWNER_ID)).thenReturn(mockOwner);
  }

  @Test
  public void testRenewWaivers_SuccessfulRenewal() {
    Date newExpiryTime = new Date(System.currentTimeMillis() + 86400000L);
    Date oldExpiryTime = new Date();

    when(waiver1.getExpiryTime()).thenReturn(oldExpiryTime);
    mockWaiverFound(waiver1, WAIVER_ID_1);

    RenewWaiversResponseDTO response = service.renewWaivers(
        List.of(WAIVER_ID_1),
        newExpiryTime,
        COMMENT,
        REASON_ID);

    assertThat(response.renewed).isEqualTo(1);
    assertThat(response.notFound).isEqualTo(0);
    assertThat(response.errors).isEmpty();

    verify(waiver1).setExpiryTime(newExpiryTime);
    verify(waiver1).setLastRenewalOldExpiryDate(oldExpiryTime);
    verify(waiver1).setLastRenewedBy(USERNAME);
    verify(waiver1).setLastRenewalComment(COMMENT);
    verify(waiver1).setLastRenewalReasonId(REASON_ID);

    verify(transactionContext).begin();
    verify(transactionContext).commit();
  }

  @Test
  public void testRenewWaivers_NullExpiryTime_NeverExpire() {
    Date oldExpiryTime = new Date();

    when(waiver1.getExpiryTime()).thenReturn(oldExpiryTime);
    mockWaiverFound(waiver1, WAIVER_ID_1);

    RenewWaiversResponseDTO response = service.renewWaivers(
        List.of(WAIVER_ID_1),
        null,
        COMMENT,
        REASON_ID);

    assertThat(response.renewed).isEqualTo(1);
    assertThat(response.notFound).isEqualTo(0);
    assertThat(response.errors).isEmpty();

    verify(waiver1).setExpiryTime(null);
    verify(waiver1).setLastRenewalOldExpiryDate(oldExpiryTime);
  }

  @Test
  public void testRenewWaivers_WaiverNotFound() {
    Date newExpiryTime = new Date(System.currentTimeMillis() + 86400000L);
    when(policyWaiverDAO.getByIds(Set.of(WAIVER_ID_1))).thenReturn(Collections.emptyList());

    RenewWaiversResponseDTO response = service.renewWaivers(
        List.of(WAIVER_ID_1),
        newExpiryTime,
        COMMENT,
        REASON_ID);

    assertThat(response.renewed).isEqualTo(0);
    assertThat(response.notFound).isEqualTo(1);
    assertThat(response.errors).isEmpty();

    verify(policyWaiverDAO, never()).updateForRenewal(any(), any());
  }

  @Test
  public void testRenewWaivers_EmptyWaiverIds_ThrowsBadRequest() {
    Date newExpiryTime = new Date(System.currentTimeMillis() + 86400000L);

    assertThatThrownBy(() -> service.renewWaivers(
        Collections.emptyList(),
        newExpiryTime,
        COMMENT,
        REASON_ID))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Waiver IDs list cannot be null or empty");
  }

  @Test
  public void testRenewWaivers_NullWaiverIds_ThrowsBadRequest() {
    Date newExpiryTime = new Date(System.currentTimeMillis() + 86400000L);

    assertThatThrownBy(() -> service.renewWaivers(
        null,
        newExpiryTime,
        COMMENT,
        REASON_ID))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Waiver IDs list cannot be null or empty");
  }

  @Test
  public void testRenewWaivers_CommentTruncation() {
    Date newExpiryTime = new Date(System.currentTimeMillis() + 86400000L);
    Date oldExpiryTime = new Date();

    StringBuilder longComment = new StringBuilder();
    for (int i = 0; i < 110; i++) {
      longComment.append("comment1234567890");
    }
    String longCommentStr = longComment.toString();

    when(waiver1.getExpiryTime()).thenReturn(oldExpiryTime);
    mockWaiverFound(waiver1, WAIVER_ID_1);

    service.renewWaivers(List.of(WAIVER_ID_1), newExpiryTime, longCommentStr, REASON_ID);

    verify(waiver1).setLastRenewalComment(longCommentStr.substring(0, 1000));
  }

  @Test
  public void testRenewWaivers_ExceptionDuringUpdate_CaughtAndLogged() {
    Date newExpiryTime = new Date(System.currentTimeMillis() + 86400000L);
    Date oldExpiryTime = new Date();

    when(waiver1.getExpiryTime()).thenReturn(oldExpiryTime);
    mockWaiverFound(waiver1, WAIVER_ID_1);

    RuntimeException dbException = new RuntimeException("Database connection lost");
    doThrow(dbException).when(policyWaiverDAO).updateForRenewal(eq(transactionContext), any(PolicyWaiver.class));

    RenewWaiversResponseDTO response = service.renewWaivers(
        List.of(WAIVER_ID_1),
        newExpiryTime,
        COMMENT,
        REASON_ID);

    assertThat(response.renewed).isEqualTo(0);
    assertThat(response.notFound).isEqualTo(0);
    assertThat(response.errors).hasSize(1);
    assertThat(response.errors.get(0)).contains(WAIVER_ID_1);
    assertThat(response.errors.get(0)).contains("Database connection lost");
  }

  @Test
  public void testRenewWaivers_MixedResults() {
    Date newExpiryTime = new Date(System.currentTimeMillis() + 86400000L);
    Date oldExpiryTime = new Date();

    when(waiver1.getExpiryTime()).thenReturn(oldExpiryTime);
    mockWaiverFound(waiver1, WAIVER_ID_1);

    when(policyWaiverDAO.getByIds(Set.of(WAIVER_ID_2))).thenReturn(Collections.emptyList());

    RenewWaiversResponseDTO response = service.renewWaivers(
        List.of(WAIVER_ID_1, WAIVER_ID_2),
        newExpiryTime,
        COMMENT,
        REASON_ID);

    assertThat(response.renewed).isEqualTo(1);
    assertThat(response.notFound).isEqualTo(1);
    assertThat(response.errors).isEmpty();
  }

  @Test
  public void testRenewWaivers_SameExpiryTime_NoOp() {
    Date expiryTime = new Date(System.currentTimeMillis() + 86_400_000L);

    when(waiver1.getExpiryTime()).thenReturn(expiryTime);
    mockWaiverFound(waiver1, WAIVER_ID_1);

    RenewWaiversResponseDTO response = service.renewWaivers(
        List.of(WAIVER_ID_1),
        expiryTime,
        COMMENT,
        REASON_ID);

    assertThat(response.renewed).isEqualTo(1);
    assertThat(response.notFound).isEqualTo(0);

    verify(policyWaiverDAO, never()).updateForRenewal(any(), any());
  }

  @Test
  public void testRenewWaivers_PastExpiryTime_ThrowsBadRequest() {
    Date pastDate = new Date(System.currentTimeMillis() - 86400000L);

    assertThatThrownBy(() -> service.renewWaivers(
        List.of(WAIVER_ID_1),
        pastDate,
        COMMENT,
        REASON_ID))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Expiration date must be in the future");
  }

  @Test
  public void testRenewWaivers_FutureExpiryTime_Valid() {
    Date futureDate = new Date(System.currentTimeMillis() + 86400000L * 30);
    Date oldExpiryTime = new Date();

    when(waiver1.getExpiryTime()).thenReturn(oldExpiryTime);
    mockWaiverFound(waiver1, WAIVER_ID_1);

    RenewWaiversResponseDTO response = service.renewWaivers(
        List.of(WAIVER_ID_1),
        futureDate,
        COMMENT,
        REASON_ID);

    assertThat(response.renewed).isEqualTo(1);
    assertThat(response.notFound).isEqualTo(0);
    assertThat(response.errors).isEmpty();
  }
}
