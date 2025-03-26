/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.autowaivers;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;

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
  private static final Logger log = LoggerFactory.getLogger(AutoPolicyWaiverUtil.class);

  public static void validateAutoWaiversFeatureEnabled() {
    if (!SystemConfigurationPropertyFeature.AUTO_WAIVERS.isEnabled()) {
      throw new UnauthorizedException("Auto Policy Waivers feature is not enabled");
    }
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
              apiAutoPolicyWaiver2.pathForward
          ));
        }

        if (allScopesEnabled(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2)) {
          log.debug(
              "Equal Auto Policy Waiver found for reachability: '{}' and pathForward '{}'",
              apiAutoPolicyWaiver2.reachability,
              apiAutoPolicyWaiver2.pathForward
          );
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
   * @param ownerId              - {@link String}
   * @param apiAutoPolicyWaivers - List of {@link ApiAutoPolicyWaiverDTO}
   * @param autoPolicyWaivers    - List of {@link AutoPolicyWaiver}
   * @return true if any of the {@link ApiAutoPolicyWaiverDTO}s are equal by owner and scope, compared to the given
   * {@link AutoPolicyWaiver}, false otherwise.
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
                apiAutoPolicyWaiver.pathForward
            ));
          }

          if (allScopesEnabled(apiAutoPolicyWaiver, autoPolicyWaiver)) {
            log.debug(
                "Auto Policy Waiver with id: {} is equal by owner id: {} and reachability: '{}' and pathForward '{}'",
                autoPolicyWaiver.getId(),
                ownerId,
                apiAutoPolicyWaiver.reachability,
                apiAutoPolicyWaiver.pathForward
            );
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
    return TRUE.equals(apiAutoPolicyWaiver1.reachability) && TRUE.equals(apiAutoPolicyWaiver2.reachability) &&
        TRUE.equals(apiAutoPolicyWaiver1.pathForward) && TRUE.equals(apiAutoPolicyWaiver2.pathForward);
  }

  private static boolean allScopesDisabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1,
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2)
  {
    return FALSE.equals(apiAutoPolicyWaiver1.reachability) && FALSE.equals(apiAutoPolicyWaiver2.reachability) &&
        FALSE.equals(apiAutoPolicyWaiver1.pathForward) && FALSE.equals(apiAutoPolicyWaiver2.pathForward);
  }

  private static boolean isOnlyReachabilityEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1,
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2)
  {
    return TRUE.equals(apiAutoPolicyWaiver1.reachability) && TRUE.equals(apiAutoPolicyWaiver2.reachability) &&
        FALSE.equals(apiAutoPolicyWaiver1.pathForward) && FALSE.equals(apiAutoPolicyWaiver2.pathForward);
  }

  private static boolean isOnlyPathForwardEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1,
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2)
  {
    return FALSE.equals(apiAutoPolicyWaiver1.reachability) && FALSE.equals(apiAutoPolicyWaiver2.reachability) &&
        TRUE.equals(apiAutoPolicyWaiver1.pathForward) && TRUE.equals(apiAutoPolicyWaiver2.pathForward);
  }

  private static boolean isOnlyReachabilityEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver,
      final AutoPolicyWaiver autoPolicyWaiver)
  {
    return TRUE.equals(apiAutoPolicyWaiver.reachability) && autoPolicyWaiver.hasReachability() &&
        FALSE.equals(apiAutoPolicyWaiver.pathForward) && !autoPolicyWaiver.hasPathForward();
  }

  private static boolean isOnlyPathForwardEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver,
      final AutoPolicyWaiver autoPolicyWaiver)
  {
    return FALSE.equals(apiAutoPolicyWaiver.reachability) && !autoPolicyWaiver.hasReachability() &&
        TRUE.equals(apiAutoPolicyWaiver.pathForward) && autoPolicyWaiver.hasPathForward();
  }

  private static boolean allScopesEnabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver,
      final AutoPolicyWaiver autoPolicyWaiver)
  {
    return TRUE.equals(apiAutoPolicyWaiver.reachability) && autoPolicyWaiver.hasReachability() &&
        TRUE.equals(apiAutoPolicyWaiver.pathForward) && autoPolicyWaiver.hasPathForward();
  }

  private static boolean allScopesDisabled(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver,
      final AutoPolicyWaiver autoPolicyWaiver)
  {
    return FALSE.equals(apiAutoPolicyWaiver.reachability) && !autoPolicyWaiver.hasReachability() &&
        FALSE.equals(apiAutoPolicyWaiver.pathForward) && !autoPolicyWaiver.hasPathForward();
  }
}
