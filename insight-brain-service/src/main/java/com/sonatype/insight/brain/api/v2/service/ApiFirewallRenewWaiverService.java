/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.firewall.RenewWaiversResponseDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthorizedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for renewing Firewall policy waivers.
 * <p>
 * Renewal updates the waiver's expiry time and records the previous expiry time in renewal tracking columns.
 * Supports both single and bulk waiver renewals.
 *
 * @since 1.186
 */
@Named
@Singleton
public class ApiFirewallRenewWaiverService
{
  private static final Logger log = LoggerFactory.getLogger(ApiFirewallRenewWaiverService.class);

  private final PolicyWaiverDAO policyWaiverDAO;

  private final OwnerDAO ownerDAO;

  private final CurrentUser currentUser;

  @Inject
  public ApiFirewallRenewWaiverService(
      final PolicyWaiverDAO policyWaiverDAO,
      final OwnerDAO ownerDAO,
      final CurrentUser currentUser)
  {
    this.policyWaiverDAO = policyWaiverDAO;
    this.ownerDAO = ownerDAO;
    this.currentUser = currentUser;
  }

  public RenewWaiversResponseDTO renewWaivers(
      final List<String> waiverIds,
      final Date newExpiryTime,
      final String comment,
      final String reasonId)
  {
    validateRenewalRequest(newExpiryTime, waiverIds);

    RenewWaiversResponseDTO response = new RenewWaiversResponseDTO();
    response.renewed = 0;
    response.notFound = 0;
    response.errors = new ArrayList<>();

    for (String waiverId : waiverIds) {
      try {
        boolean renewed = renewSingleWaiver(waiverId, newExpiryTime, comment, reasonId);
        if (renewed) {
          response.renewed++;
        }
        else {
          response.notFound++;
        }
      }
      catch (UnauthorizedException e) {
        throw e;
      }
      catch (NotFoundException e) {
        response.notFound++;
      }
      catch (Exception e) {
        log.error("Failed to renew waiver {}: {}", waiverId, e.getMessage(), e);
        response.errors.add(String.format("Waiver %s: %s", waiverId, e.getMessage()));
      }
    }

    log.info("Renewed {} waivers, {} not found, {} errors for user {}",
        response.renewed, response.notFound, response.errors.size(), currentUser.getUsername());

    return response;
  }

  private boolean renewSingleWaiver(
      final String waiverId,
      final Date newExpiryTime,
      final String comment,
      final String reasonId)
  {
    validateWaiverId(waiverId);

    List<PolicyWaiver> waivers = policyWaiverDAO.getByIds(Set.of(waiverId));
    if (waivers.isEmpty()) {
      return false;
    }
    PolicyWaiver waiver = waivers.get(0);

    Owner owner = ownerDAO.getById(waiver.getOwnerId());
    if (owner == null) {
      throw new NotFoundException("Owner not found for waiver " + waiverId);
    }

    performRenewalWithAuthzCheck(owner.getType(), owner.getId(), waiver, newExpiryTime, comment, reasonId);
    return true;
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  void performRenewalWithAuthzCheck(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      final PolicyWaiver waiver,
      final Date newExpiryTime,
      final String comment,
      final String reasonId)
  {
    if (Objects.equals(waiver.getExpiryTime(), newExpiryTime)) {
      log.debug("Waiver {} expiry time unchanged, skipping renewal", waiver.getId());
      return;
    }

    Date renewalTime = new Date();
    String renewedBy = currentUser.getUsername();
    Date oldExpiryTime = waiver.getExpiryTime();

    waiver.setExpiryTime(newExpiryTime);
    waiver.setLastRenewalOldExpiryDate(oldExpiryTime);
    waiver.setLastRenewedBy(renewedBy);
    waiver.setLastRenewedAt(renewalTime);
    waiver.setLastRenewalComment(truncateComment(comment));
    waiver.setLastRenewalReasonId(reasonId);

    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      policyWaiverDAO.updateForRenewal(tx, waiver);
      tx.commit();
    }

    log.debug("Renewed waiver {} for user {}, old expiry: {}, new expiry: {}",
        waiver.getId(), renewedBy, oldExpiryTime, newExpiryTime);
  }

  private void validateRenewalRequest(final Date newExpiryTime, final List<String> waiverIds) {
    if (waiverIds == null || waiverIds.isEmpty()) {
      throw new BadRequestException("Waiver IDs list cannot be null or empty");
    }
    validateExpiryTime(newExpiryTime);
  }

  private void validateExpiryTime(final Date expiryTime) {
    if (Objects.nonNull(expiryTime) &&
        !expiryTime.toInstant().atZone(ZoneId.of("UTC")).toLocalDate().isAfter(LocalDate.now(ZoneId.of("UTC"))))
    {
      throw new BadRequestException("Expiration date must be in the future.");
    }
  }

  private void validateWaiverId(final String waiverId) {
    if (waiverId == null || waiverId.isBlank()) {
      throw new BadRequestException("Waiver ID cannot be null or blank");
    }
  }

  private String truncateComment(final String comment) {
    if (comment != null && comment.length() > 1000) {
      return comment.substring(0, 1000);
    }
    return comment;
  }
}
