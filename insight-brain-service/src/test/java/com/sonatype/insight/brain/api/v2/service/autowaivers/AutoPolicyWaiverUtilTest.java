/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.autowaivers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;

import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.dto.ApiPolicyOwnerType.ORGANIZATION;
import static com.sonatype.insight.brain.api.v2.service.autowaivers.AutoPolicyWaiverUtil.anyEqualByOwnerAndScope;
import static com.sonatype.insight.brain.api.v2.service.autowaivers.AutoPolicyWaiverUtil.anyEqualByScope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AutoPolicyWaiverUtilTest
{
  private static final String OWNER_ID = "ownerId";

  private static final int THREAT_LEVEL = 7;

  private static final String CREATOR_ID = "creatorId";

  private static final String CREATOR_NAME = "creatorName";

  private static final Date NOW = new Date(System.currentTimeMillis());

  @Test
  public void testGetApplicableAutoPolicyWaivers_ReturnsFirstOfEachType() {
    final AutoPolicyWaiver waiverNRAndNPF1 = createAutoWaiver(true, true);
    final AutoPolicyWaiver waiverNRAndNPF2 = createAutoWaiver(true, true);
    final AutoPolicyWaiver waiverNRAndNPF3 = createAutoWaiver(true, true);

    final AutoPolicyWaiver waiverNR1 = createAutoWaiver(true, false);
    final AutoPolicyWaiver waiverNR2 = createAutoWaiver(true, false);
    final AutoPolicyWaiver waiverNR3 = createAutoWaiver(true, false);

    final AutoPolicyWaiver waiverNPF1 = createAutoWaiver(false, true);
    final AutoPolicyWaiver waiverNPF2 = createAutoWaiver(false, true);
    final AutoPolicyWaiver waiverNPF3 = createAutoWaiver(false, true);

    final List<AutoPolicyWaiver> autoWaivers = List.of(
        waiverNRAndNPF1,
        waiverNRAndNPF2,
        waiverNR1,
        waiverNRAndNPF3,
        waiverNPF1,
        waiverNR2,
        waiverNR3,
        waiverNPF2,
        waiverNPF3);

    final List<AutoPolicyWaiver> applicableAutoWaivers =
        AutoPolicyWaiverUtil.getApplicableAutoPolicyWaivers(autoWaivers);
    assertThat(applicableAutoWaivers)
        .hasSize(3)
        .containsExactly(waiverNRAndNPF1, waiverNR1, waiverNPF1);
  }

  @Test
  public void testGetApplicableAutoPolicyWaivers_ReturnsEmptyResult_WhenInputIsEmpty() {
    assertThat(AutoPolicyWaiverUtil.getApplicableAutoPolicyWaivers(List.of())).isEmpty();
  }

  @Test
  public void testAnyEqualByScope_WithNullAndEmptyValues_ForApiAutoPolicyWaivers() {
    assertThat(anyEqualByScope(null)).isFalse();
    assertThat(anyEqualByScope(List.of())).isFalse();
    assertThat(anyEqualByScope(List.of(new ApiAutoPolicyWaiverDTO()))).isFalse();
  }

  @Test
  public void testAnyEqualByScope_WithSameOwnerAndMixedScopes_ForApiAutoPolicyWaivers() {
    String ownerId = "1234";

    List<ApiAutoPolicyWaiverDTO> apiAutoPolicyWaivers = new ArrayList<>();

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1 = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiver1.ownerId = ownerId;
    apiAutoPolicyWaiver1.ownerType = ORGANIZATION.name();
    apiAutoPolicyWaivers.add(apiAutoPolicyWaiver1);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2 = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiver2.ownerId = ownerId;
    apiAutoPolicyWaiver2.ownerType = ORGANIZATION.name();
    apiAutoPolicyWaivers.add(apiAutoPolicyWaiver2);

    apiAutoPolicyWaiver1.reachability = true;
    apiAutoPolicyWaiver1.pathForward = true;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = true;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isTrue();

    apiAutoPolicyWaiver1.reachability = false;
    apiAutoPolicyWaiver1.pathForward = true;
    apiAutoPolicyWaiver2.reachability = false;
    apiAutoPolicyWaiver2.pathForward = true;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isTrue();

    apiAutoPolicyWaiver1.reachability = true;
    apiAutoPolicyWaiver1.pathForward = false;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = false;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isTrue();

    apiAutoPolicyWaiver1.reachability = false;
    apiAutoPolicyWaiver1.pathForward = true;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = true;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isFalse();

    apiAutoPolicyWaiver1.reachability = true;
    apiAutoPolicyWaiver1.pathForward = false;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = true;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isFalse();

    apiAutoPolicyWaiver1.reachability = true;
    apiAutoPolicyWaiver1.pathForward = true;
    apiAutoPolicyWaiver2.reachability = false;
    apiAutoPolicyWaiver2.pathForward = true;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isFalse();

    apiAutoPolicyWaiver1.reachability = true;
    apiAutoPolicyWaiver1.pathForward = true;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = false;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isFalse();

    apiAutoPolicyWaiver1.reachability = false;
    apiAutoPolicyWaiver1.pathForward = false;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = true;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isFalse();

    apiAutoPolicyWaiver1.reachability = false;
    apiAutoPolicyWaiver1.pathForward = false;
    apiAutoPolicyWaiver2.reachability = false;
    apiAutoPolicyWaiver2.pathForward = true;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isFalse();

    apiAutoPolicyWaiver1.reachability = true;
    apiAutoPolicyWaiver1.pathForward = false;
    apiAutoPolicyWaiver2.reachability = false;
    apiAutoPolicyWaiver2.pathForward = false;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isFalse();

    apiAutoPolicyWaiver1.reachability = false;
    apiAutoPolicyWaiver1.pathForward = true;
    apiAutoPolicyWaiver2.reachability = false;
    apiAutoPolicyWaiver2.pathForward = false;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isFalse();

    apiAutoPolicyWaiver1.reachability = false;
    apiAutoPolicyWaiver1.pathForward = false;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = false;

    assertThat(
        anyEqualByScope(apiAutoPolicyWaivers)).isFalse();

    apiAutoPolicyWaiver1.reachability = false;
    apiAutoPolicyWaiver1.pathForward = false;
    apiAutoPolicyWaiver2.reachability = false;
    apiAutoPolicyWaiver2.pathForward = false;

    assertThatThrownBy(() -> anyEqualByScope(apiAutoPolicyWaivers)).isInstanceOf(IllegalStateException.class)
        .hasMessage("Equal Auto Policy Waiver found for reachability: 'false' and " +
            "pathForward 'false' but are not allowed to be both false.");
  }

  @Test
  public void testAnyEqualByScope_WithNullAndEmptyValues_ForApiAutoPolicyWaiversAndAutoPolicyWaivers() {
    assertThat(anyEqualByOwnerAndScope(null, null, null)).isFalse();
    assertThat(anyEqualByOwnerAndScope(null, null, List.of())).isFalse();
    assertThat(anyEqualByOwnerAndScope(null, List.of(), List.of())).isFalse();
    assertThat(anyEqualByOwnerAndScope(null, List.of(new ApiAutoPolicyWaiverDTO()), List.of())).isFalse();
    assertThat(anyEqualByOwnerAndScope("", List.of(), List.of(new AutoPolicyWaiver()))).isFalse();
    assertThat(anyEqualByOwnerAndScope("",
        List.of(new ApiAutoPolicyWaiverDTO()), List.of(new AutoPolicyWaiver()))).isFalse();
    assertThat(
        anyEqualByOwnerAndScope(
            "1234",
            List.of(new ApiAutoPolicyWaiverDTO()),
            List.of(new AutoPolicyWaiver()))).isFalse();
  }

  @Test
  public void testAnyEqualByScope_WithNullScopes_ForApiAutoPolicyWaiverAndAutoPolicyWaiver() {
    String ownerId = "1234";

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiverDTO.ownerId = ownerId;
    apiAutoPolicyWaiverDTO.ownerType = ORGANIZATION.name();

    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver();
    autoPolicyWaiver.setOwnerId(ownerId);

    assertThatThrownBy(
        () -> anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Auto Policy Waiver with id: null is equal by owner id: 1234 and reachability: 'null' and " +
                "pathForward 'null' but are not allowed to be both false.");

    apiAutoPolicyWaiverDTO.reachability = true;

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    apiAutoPolicyWaiverDTO.pathForward = true;

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    autoPolicyWaiver.setReachability(true);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    autoPolicyWaiver.setPathForward(true);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isTrue();
  }

  @Test
  public void testAnyEqualByScope_WithSameScopeAndMixedOwners_ForApiAutoPolicyWaiverAndAutoPolicyWaiver() {
    String ownerId = "1234";

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiverDTO.ownerId = ownerId;
    apiAutoPolicyWaiverDTO.ownerType = ORGANIZATION.name();

    AutoPolicyWaiver autoPolicyWaiver = createAutoWaiver(true, true);
    autoPolicyWaiver.setOwnerId("5678");

    apiAutoPolicyWaiverDTO.reachability = true;
    apiAutoPolicyWaiverDTO.pathForward = true;

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();
  }

  @Test
  public void testAnyEqualByScope_WithSameOwnerAndMixedScopes_ForApiAutoPolicyWaiverAndAutoPolicyWaiver() {
    String ownerId = "1234";

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiverDTO.ownerId = ownerId;
    apiAutoPolicyWaiverDTO.ownerType = ORGANIZATION.name();

    AutoPolicyWaiver autoPolicyWaiver = createAutoWaiver(true, true);
    autoPolicyWaiver.setOwnerId(ownerId);

    apiAutoPolicyWaiverDTO.reachability = true;
    apiAutoPolicyWaiverDTO.pathForward = true;

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isTrue();

    apiAutoPolicyWaiverDTO.reachability = false;
    apiAutoPolicyWaiverDTO.pathForward = true;
    autoPolicyWaiver.setReachability(false);
    autoPolicyWaiver.setPathForward(true);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isTrue();

    apiAutoPolicyWaiverDTO.reachability = true;
    apiAutoPolicyWaiverDTO.pathForward = false;
    autoPolicyWaiver.setReachability(true);
    autoPolicyWaiver.setPathForward(false);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isTrue();

    apiAutoPolicyWaiverDTO.reachability = false;
    apiAutoPolicyWaiverDTO.pathForward = true;
    autoPolicyWaiver.setReachability(true);
    autoPolicyWaiver.setPathForward(true);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    apiAutoPolicyWaiverDTO.reachability = true;
    apiAutoPolicyWaiverDTO.pathForward = false;
    autoPolicyWaiver.setReachability(true);
    autoPolicyWaiver.setPathForward(true);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    apiAutoPolicyWaiverDTO.reachability = true;
    apiAutoPolicyWaiverDTO.pathForward = true;
    autoPolicyWaiver.setReachability(false);
    autoPolicyWaiver.setPathForward(true);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    apiAutoPolicyWaiverDTO.reachability = true;
    apiAutoPolicyWaiverDTO.pathForward = true;
    autoPolicyWaiver.setReachability(true);
    autoPolicyWaiver.setPathForward(false);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    apiAutoPolicyWaiverDTO.reachability = false;
    apiAutoPolicyWaiverDTO.pathForward = false;
    autoPolicyWaiver.setReachability(true);
    autoPolicyWaiver.setPathForward(true);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    apiAutoPolicyWaiverDTO.reachability = false;
    apiAutoPolicyWaiverDTO.pathForward = false;
    autoPolicyWaiver.setReachability(false);
    autoPolicyWaiver.setPathForward(true);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    apiAutoPolicyWaiverDTO.reachability = true;
    apiAutoPolicyWaiverDTO.pathForward = false;
    autoPolicyWaiver.setReachability(false);
    autoPolicyWaiver.setPathForward(false);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    apiAutoPolicyWaiverDTO.reachability = false;
    apiAutoPolicyWaiverDTO.pathForward = true;
    autoPolicyWaiver.setReachability(false);
    autoPolicyWaiver.setPathForward(false);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    apiAutoPolicyWaiverDTO.reachability = false;
    apiAutoPolicyWaiverDTO.pathForward = false;
    autoPolicyWaiver.setReachability(true);
    autoPolicyWaiver.setPathForward(false);

    assertThat(
        anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver))).isFalse();

    apiAutoPolicyWaiverDTO.reachability = false;
    apiAutoPolicyWaiverDTO.pathForward = false;
    autoPolicyWaiver.setReachability(false);
    autoPolicyWaiver.setPathForward(false);

    assertThatThrownBy(
        () -> anyEqualByOwnerAndScope(ownerId, List.of(apiAutoPolicyWaiverDTO), List.of(autoPolicyWaiver)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Auto Policy Waiver with id: null is equal by owner id: 1234 and reachability: 'false' and " +
                "pathForward 'false' but are not allowed to be both false.");

  }

  private static AutoPolicyWaiver createAutoWaiver(final boolean hasNotReachable, final boolean hasNoPathForward) {
    return new AutoPolicyWaiver(OWNER_ID, THREAT_LEVEL, hasNotReachable, hasNoPathForward, CREATOR_ID, CREATOR_NAME,
        NOW);
  }
}
