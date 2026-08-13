/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.comparison;

import java.io.UncheckedIOException;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ComponentH2Test
public class AutoPolicyWaiverViolationConditionFactComparatorTest
    extends AbstractComponentH2Test
{
  private final AutoPolicyWaiverViolationConditionFactComparator conditionFactComparator =
      new AutoPolicyWaiverViolationConditionFactComparator();

  private ConditionFact conditionFact1;

  private ConditionFact conditionFact2;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();

    conditionFact1 = new ConditionFact();
    conditionFact2 = new ConditionFact();
  }

  @Test
  public void testCompare_Against_Null() {
    assertThat(conditionFactComparator.compare(null, null)).isEqualTo(0);
    assertThat(conditionFactComparator.compare(null, conditionFact2)).isEqualTo(1);
    assertThat(conditionFactComparator.compare(conditionFact1, null)).isEqualTo(-1);
  }

  @Test
  public void testCompare_By_TriggerJson() {
    // required to test trigger json
    conditionFact1.setConditionIndex(0);
    conditionFact2.setConditionIndex(0);

    conditionFact1.setTriggerJson("triggerJson");
    conditionFact2.setTriggerJson("triggerJson");
    assertThatThrownBy(() -> conditionFactComparator.compare(conditionFact1, conditionFact2))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining("Unrecognized token 'triggerJson'");

    conditionFact1.setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"refId\":\"CVE-2024-43398\",\"severity\":8.7}}");
    conditionFact2.setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"refId\":\"CVE-2024-43398\",\"severity\":8.7}}");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isEqualTo(0);

    conditionFact1.setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"refId\":\"CVE-2024-43398\",\"severity\":8.8}}");
    conditionFact2.setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"refId\":\"CVE-2024-43398\",\"severity\":8.7}}");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isGreaterThanOrEqualTo(1);

    conditionFact1.setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"refId\":\"CVE-2024-43398\",\"severity\":8.7}}");
    conditionFact2.setTriggerJson("{\"conditionIndex\":0,\"trigger\":{\"refId\":\"CVE-2024-43398\",\"severity\":8.8}}");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isLessThanOrEqualTo(-1);
  }

  @Test
  public void testCompare_By_TriggerReference() {
    TriggerReference triggerReference1 = new TriggerReference();
    conditionFact1.setReference(triggerReference1);

    TriggerReference triggerReference2 = new TriggerReference();
    conditionFact2.setReference(triggerReference2);

    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isEqualTo(0);

    triggerReference1.setType(Type.SECURITY_VULNERABILITY_REFID);
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isLessThanOrEqualTo(-1);

    triggerReference2.setType(Type.SAST_FINDING_ID);
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isLessThanOrEqualTo(-1);

    triggerReference1.setType(Type.SAST_FINDING_ID);
    triggerReference2.setType(Type.SECURITY_VULNERABILITY_REFID);
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isGreaterThanOrEqualTo(1);

    triggerReference1.setType(Type.SECURITY_VULNERABILITY_REFID);
    triggerReference2.setType(Type.SECURITY_VULNERABILITY_REFID);

    triggerReference1.setValue("CVE-1111");
    triggerReference2.setValue("CVE-1111");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isEqualTo(0);

    triggerReference1.setValue("CVE-0000");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isLessThanOrEqualTo(-1);

    triggerReference1.setValue("CVE-9999");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void testCompare_By_ConditionTypeId() {
    conditionFact1.setConditionTypeId("abcd");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isGreaterThanOrEqualTo(-1);

    conditionFact2.setConditionTypeId("abcd");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isEqualTo(0);

    conditionFact2.setConditionTypeId("aaaa");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isGreaterThanOrEqualTo(1);

    conditionFact2.setConditionTypeId("zzzz");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isLessThanOrEqualTo(-1);
  }

  @Test
  public void testCompare_By_ConditionIndex() {
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isEqualTo(0);

    conditionFact1.setConditionIndex(1);
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isGreaterThanOrEqualTo(-1);

    conditionFact2.setConditionIndex(1);
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isEqualTo(0);

    conditionFact2.setConditionIndex(2);
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isLessThanOrEqualTo(-1);

    conditionFact1.setConditionIndex(3);
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void testCompare_By_Reason() {
    conditionFact1 = new ConditionFact("", 0, "", "vulnerability CVE-1111 with severity >= 9 (severity = 9.8)");
    conditionFact2 = new ConditionFact("", 0, "", "vulnerability CVE-1111 with severity >= 9 (severity = 9.8)");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isEqualTo(0);

    conditionFact1 = new ConditionFact("", 0, "", "vulnerability CVE-0000 with severity >= 9 (severity = 9.8)");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isLessThanOrEqualTo(-1);

    conditionFact1 = new ConditionFact("", 0, "", "vulnerability CVE-9999 with severity >= 9 (severity = 9.8)");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void testCompare_By_Summary() {
    conditionFact1 = new ConditionFact("", 0, "Security Vulnerability Severity >= 7", "");
    conditionFact2 = new ConditionFact("", 0, "Security Vulnerability Severity >= 7", "");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isEqualTo(0);

    conditionFact1 = new ConditionFact("", 0, "Security Vulnerability Severity >= 4", "");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isLessThanOrEqualTo(-1);

    conditionFact2 = new ConditionFact("", 0, "Security Vulnerability Severity >= 3", "");
    assertThat(conditionFactComparator.compare(conditionFact1, conditionFact2)).isGreaterThanOrEqualTo(1);
  }
}
