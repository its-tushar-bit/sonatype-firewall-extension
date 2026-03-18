/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.autowaivers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;

import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AutoPolicyWaiverUtil
{
  private AutoPolicyWaiverUtil() {
    // no-op
  }

  private static final Logger log = LoggerFactory.getLogger(AutoPolicyWaiverUtil.class);

  public static void validateAutoWaiversFeatureEnabled() {
    if (!SystemConfigurationPropertyFeature.AUTO_WAIVERS.isEnabled()) {
      throw new UnauthorizedException("Auto Policy Waivers feature is not enabled");
    }
  }

  public static List<AutoPolicyWaiver> getApplicableAutoPolicyWaivers(final List<AutoPolicyWaiver> autoPolicyWaivers) {
    if (autoPolicyWaivers.isEmpty()) {
      return Collections.emptyList();
    }

    // The below order of insertion is important as it determines the order in which the waivers will be evaluated
    // when called by ScanPolicyEvaluator. Do not change the order of insertion.
    final List<AutoPolicyWaiver> applicableAutoWaivers = new ArrayList<>();
    final Optional<AutoPolicyWaiver> autoPolicyWaiverWithReachabilityAndNPF = autoPolicyWaivers.stream()
        .filter(AutoPolicyWaiverUtil::hasReachabilityAndNPF)
        .findFirst();
    autoPolicyWaiverWithReachabilityAndNPF.ifPresent(applicableAutoWaivers::add);
    final Optional<AutoPolicyWaiver> autoPolicyWaiverWithReachability = autoPolicyWaivers.stream()
        .filter(AutoPolicyWaiverUtil::hasReachability)
        .findFirst();
    autoPolicyWaiverWithReachability.ifPresent(applicableAutoWaivers::add);
    final Optional<AutoPolicyWaiver> autoPolicyWaiverWithPathForward = autoPolicyWaivers.stream()
        .filter(AutoPolicyWaiverUtil::hasPathForward)
        .findFirst();
    autoPolicyWaiverWithPathForward.ifPresent(applicableAutoWaivers::add);

    return applicableAutoWaivers;
  }

  private static boolean hasReachabilityAndNPF(final AutoPolicyWaiver autoPolicyWaiver) {
    return autoPolicyWaiver.hasReachability() && autoPolicyWaiver.hasPathForward();
  }

  private static boolean hasReachability(final AutoPolicyWaiver autoPolicyWaiver) {
    return autoPolicyWaiver.hasReachability() && !autoPolicyWaiver.hasPathForward();
  }

  private static boolean hasPathForward(final AutoPolicyWaiver autoPolicyWaiver) {
    return autoPolicyWaiver.hasPathForward() && !autoPolicyWaiver.hasReachability();
  }

  /**
   * Check if any of the given {@link ApiAutoPolicyWaiverDTO}s in the {@link List} are equal by scopes.
   *
   * @param apiAutoPolicyWaivers List of {@link ApiAutoPolicyWaiverDTO}
   * @return true if any of the {@link ApiAutoPolicyWaiverDTO}s are equal by scope, false otherwise.
   */
  public static boolean anyEqualByScope(final List<ApiAutoPolicyWaiverDTO> apiAutoPolicyWaivers) {
    if (isEmpty(apiAutoPolicyWaivers)) {
      return false;
    }

    for (ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1 : apiAutoPolicyWaivers) {
      for (ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2 : apiAutoPolicyWaivers) {
        // don't compare the same auto policy waiver
        if (apiAutoPolicyWaiver1 == apiAutoPolicyWaiver2) {
          continue;
        }

        // assure that the reachability and pathForward are never both false
        if (allScopesDisabled(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2)) {
          throw new IllegalStateException(format(
              "Equal Auto Policy Waiver found for reachability: '%s' and pathForward '%s' " +
                  "but are not allowed to be both false.",
              apiAutoPolicyWaiver2.reachability,
              apiAutoPolicyWaiver2.pathForward));
        }

        if (allScopesEnabled(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2)) {
          log.debug(
              "Equal Auto Policy Waiver found for reachability: '{}' and pathForward '{}'",
              apiAutoPolicyWaiver2.reachability,
              apiAutoPolicyWaiver2.pathForward);
          return true;
        }

        if (isOnlyReachabilityEnabled(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2)) {
          log.debug("Equal Auto Policy Waiver found for reachability: '{}'", apiAutoPolicyWaiver1.reachability);
          return true;
        }

        if (isOnlyPathForwardEnabled(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2)) {
          log.debug("Equal Auto Policy Waiver found for pathForward '{}'", apiAutoPolicyWaiver1.pathForward);
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Check if any of the given {@link ApiAutoPolicyWaiverDTO}s in the {@link List} are equal by owner and scope compared
   * to the given {@link AutoPolicyWaiver} {@link List}.
   *
   * @param ownerId - {@link String}
   * @param apiAutoPolicyWaivers - List of {@link ApiAutoPolicyWaiverDTO}
   * @param autoPolicyWaivers - List of {@link AutoPolicyWaiver}
   * @return true if any of the {@link ApiAutoPolicyWaiverDTO}s are equal by owner and scope, compared to the given
   *         {@link AutoPolicyWaiver}, false otherwise.
   */
  public static boolean anyEqualByOwnerAndScope(
      final String ownerId,
      final List<ApiAutoPolicyWaiverDTO> apiAutoPolicyWaivers,
      final List<AutoPolicyWaiver> autoPolicyWaivers)
  {
    if (isBlank(ownerId) || isEmpty(apiAutoPolicyWaivers)) {
      return false;
    }

    for (ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver : apiAutoPolicyWaivers) {
      for (AutoPolicyWaiver autoPolicyWaiver : autoPolicyWaivers) {
        // does the owner match, then continue.
        if (ownerId.equals(autoPolicyWaiver.getOwnerId())) {

          // assure that the reachability and pathForward are never both false
          if (allScopesDisabled(apiAutoPolicyWaiver, autoPolicyWaiver)) {
            throw new IllegalStateException(format(
                "Auto Policy Waiver with id: %s is equal by owner id: %s and reachability: '%s' and pathForward '%s' " +
                    "but are not allowed to be both false.",
                autoPolicyWaiver.getId(),
                ownerId,
                apiAutoPolicyWaiver.reachability,
                apiAutoPolicyWaiver.pathForward));
          }

          if (allScopesEnabled(apiAutoPolicyWaiver, autoPolicyWaiver)) {
            log.debug(
                "Auto Policy Waiver with id: {} is equal by owner id: {} and reachability: '{}' and pathForward '{}'",
                autoPolicyWaiver.getId(),
                ownerId,
                apiAutoPolicyWaiver.reachability,
                apiAutoPolicyWaiver.pathForward);
            return true;
          }

          if (isOnlyReachabilityEnabled(apiAutoPolicyWaiver, autoPolicyWaiver)) {
            log.debug("Auto Policy Waiver with id: {} is equal by owner id: {} and reachability: '{}'",
                autoPolicyWaiver.getId(), ownerId, apiAutoPolicyWaiver.reachability);
            return true;
          }

          if (isOnlyPathForwardEnabled(apiAutoPolicyWaiver, autoPolicyWaiver)) {
            log.debug("Auto Policy Waiver with id: {} is equal by owner id: {} and pathForward '{}'",
                autoPolicyWaiver.getId(), ownerId, apiAutoPolicyWaiver.pathForward);
            return true;
          }
        }
      }
    }

    log.debug("No Auto Policy Waiver was found to be equal by owner id: {} and " +
        "scope for reachability and pathForward", ownerId);

    return false;
  }

  private static boolean allScopesEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1,
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2)
  {
    return checkSettingEnabled(apiAutoPolicyWaiver1.reachability) &&
        checkSettingEnabled(apiAutoPolicyWaiver2.reachability) &&
        checkSettingEnabled(apiAutoPolicyWaiver1.pathForward) &&
        checkSettingEnabled(apiAutoPolicyWaiver2.pathForward);
  }

  private static boolean allScopesDisabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1,
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2)
  {
    return checkSettingDisabled(apiAutoPolicyWaiver1.reachability) &&
        checkSettingDisabled(apiAutoPolicyWaiver2.reachability) &&
        checkSettingDisabled(apiAutoPolicyWaiver1.pathForward) &&
        checkSettingDisabled(apiAutoPolicyWaiver2.pathForward);
  }

  private static boolean isOnlyReachabilityEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1,
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2)
  {
    return checkSettingEnabled(apiAutoPolicyWaiver1.reachability) &&
        checkSettingEnabled(apiAutoPolicyWaiver2.reachability) &&
        checkSettingDisabled(apiAutoPolicyWaiver1.pathForward) &&
        checkSettingDisabled(apiAutoPolicyWaiver2.pathForward);
  }

  private static boolean isOnlyPathForwardEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1,
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2)
  {
    return checkSettingDisabled(apiAutoPolicyWaiver1.reachability) &&
        checkSettingDisabled(apiAutoPolicyWaiver2.reachability) &&
        checkSettingEnabled(apiAutoPolicyWaiver1.pathForward) &&
        checkSettingEnabled(apiAutoPolicyWaiver2.pathForward);
  }

  private static boolean isOnlyReachabilityEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver,
      final AutoPolicyWaiver autoPolicyWaiver)
  {
    return checkSettingEnabled(apiAutoPolicyWaiver.reachability) && autoPolicyWaiver.hasReachability() &&
        checkSettingDisabled(apiAutoPolicyWaiver.pathForward) && !autoPolicyWaiver.hasPathForward();
  }

  private static boolean isOnlyPathForwardEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver,
      final AutoPolicyWaiver autoPolicyWaiver)
  {
    return checkSettingDisabled(apiAutoPolicyWaiver.reachability) && !autoPolicyWaiver.hasReachability() &&
        checkSettingEnabled(apiAutoPolicyWaiver.pathForward) && autoPolicyWaiver.hasPathForward();
  }

  private static boolean allScopesEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver,
      final AutoPolicyWaiver autoPolicyWaiver)
  {
    return checkSettingEnabled(apiAutoPolicyWaiver.reachability) && autoPolicyWaiver.hasReachability() &&
        checkSettingEnabled(apiAutoPolicyWaiver.pathForward) && autoPolicyWaiver.hasPathForward();
  }

  private static boolean allScopesDisabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver,
      final AutoPolicyWaiver autoPolicyWaiver)
  {
    return checkSettingDisabled(apiAutoPolicyWaiver.reachability) && !autoPolicyWaiver.hasReachability() &&
        checkSettingDisabled(apiAutoPolicyWaiver.pathForward) && !autoPolicyWaiver.hasPathForward();
  }

  public static boolean checkSettingDisabled(Boolean autoWaiverSetting) {
    return autoWaiverSetting == null || FALSE.equals(autoWaiverSetting);
  }

  private static boolean checkSettingEnabled(Boolean autoWaiverSetting) {
    return TRUE.equals(autoWaiverSetting);
  }
}
