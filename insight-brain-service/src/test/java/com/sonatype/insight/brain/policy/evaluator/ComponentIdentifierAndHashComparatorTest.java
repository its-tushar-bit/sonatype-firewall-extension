/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.ComponentIdentifierAndHashComparable;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.policy.evaluator.ComponentIdentifierAndHashComparator.COMPARATOR;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIdentifierAndHashComparatorTest
{
  @Test
  public void testCompare_Equal() {
    ComponentIdentifierAndHashComparableImpl comparable1 = new ComponentIdentifierAndHashComparableImpl(null, null);
    ComponentIdentifierAndHashComparableImpl comparable2 = new ComponentIdentifierAndHashComparableImpl(null, null);
    assertThat(COMPARATOR.compare(comparable1, comparable2)).isEqualTo(0);
    assertThat(COMPARATOR.compare(comparable2, comparable1)).isEqualTo(0);

    comparable1 = new ComponentIdentifierAndHashComparableImpl("hash", null);
    comparable2 = new ComponentIdentifierAndHashComparableImpl("hash", null);
    assertThat(COMPARATOR.compare(comparable1, comparable2)).isEqualTo(0);
    assertThat(COMPARATOR.compare(comparable2, comparable1)).isEqualTo(0);

    comparable1 = new ComponentIdentifierAndHashComparableImpl(null,
        ComponentIdentifier.createNpmCoordinates("packageId", "version"));
    comparable2 = new ComponentIdentifierAndHashComparableImpl(null,
        ComponentIdentifier.createNpmCoordinates("packageId", "version"));
    assertThat(COMPARATOR.compare(comparable1, comparable2)).isEqualTo(0);
    assertThat(COMPARATOR.compare(comparable2, comparable1)).isEqualTo(0);

    comparable1 = new ComponentIdentifierAndHashComparableImpl("hash",
        ComponentIdentifier.createNpmCoordinates("packageId", "version"));
    comparable2 = new ComponentIdentifierAndHashComparableImpl("hash",
        ComponentIdentifier.createNpmCoordinates("packageId", "version"));
    assertThat(COMPARATOR.compare(comparable1, comparable2)).isEqualTo(0);
    assertThat(COMPARATOR.compare(comparable2, comparable1)).isEqualTo(0);
  }

  @Test
  public void testCompare_Different() {
    ComponentIdentifierAndHashComparableImpl comparable1 = new ComponentIdentifierAndHashComparableImpl("hash1", null);
    ComponentIdentifierAndHashComparableImpl comparable2 = new ComponentIdentifierAndHashComparableImpl(null, null);
    assertThat(COMPARATOR.compare(comparable1, comparable2)).isEqualTo(-1);
    assertThat(COMPARATOR.compare(comparable2, comparable1)).isEqualTo(1);

    comparable1 = new ComponentIdentifierAndHashComparableImpl("hash1", null);
    comparable2 = new ComponentIdentifierAndHashComparableImpl("hash2", null);
    assertThat(COMPARATOR.compare(comparable1, comparable2)).isEqualTo(-1);
    assertThat(COMPARATOR.compare(comparable2, comparable1)).isEqualTo(1);

    comparable1 = new ComponentIdentifierAndHashComparableImpl(null,
        ComponentIdentifier.createNpmCoordinates("packageId", "version1"));
    comparable2 = new ComponentIdentifierAndHashComparableImpl(null, null);
    assertThat(COMPARATOR.compare(comparable1, comparable2)).isEqualTo(-1);
    assertThat(COMPARATOR.compare(comparable2, comparable1)).isEqualTo(1);

    comparable1 = new ComponentIdentifierAndHashComparableImpl(null,
        ComponentIdentifier.createNpmCoordinates("packageId", "version1"));
    comparable2 = new ComponentIdentifierAndHashComparableImpl("hash", null);
    assertThat(COMPARATOR.compare(comparable1, comparable2)).isEqualTo(1);
    assertThat(COMPARATOR.compare(comparable2, comparable1)).isEqualTo(-1);

    comparable1 = new ComponentIdentifierAndHashComparableImpl(null,
        ComponentIdentifier.createNpmCoordinates("packageId", "version1"));
    comparable2 = new ComponentIdentifierAndHashComparableImpl(null,
        ComponentIdentifier.createNpmCoordinates("packageId", "version2"));
    assertThat(COMPARATOR.compare(comparable1, comparable2)).isEqualTo(-1);
    assertThat(COMPARATOR.compare(comparable2, comparable1)).isEqualTo(1);
  }

  private static class ComponentIdentifierAndHashComparableImpl
      implements ComponentIdentifierAndHashComparable
  {
    private final String hash;

    private final ComponentIdentifier componentIdentifier;

    public ComponentIdentifierAndHashComparableImpl(String hash, ComponentIdentifier componentIdentifier) {
      this.hash = hash;
      this.componentIdentifier = componentIdentifier;
    }

    @Override
    public String getHash() {
      return hash;
    }

    @Override
    public ComponentIdentifier getComponentIdentifier() {
      return componentIdentifier;
    }
  }
}
