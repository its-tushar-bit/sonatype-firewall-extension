/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.HashHelper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test of {@link HashComponentIdentifier} model.
 */
public class HashComponentIdentifierTest
{
  @Test
  public void testLongHashTruncatedWhenObjectCreated() {
    String longHash = "123456789012345678901";
    assertThat(longHash.length()).isGreaterThan(20);
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(longHash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    assertThat(hashComponentIdentifier.getHash()).isEqualTo(longHash.substring(0, HashHelper.MAX_LENGTH));
  }

  @Test
  public void testLongHashTruncatedWhenHashSet() {
    String longHash = "123456789012345678901";
    assertThat(longHash.length()).isGreaterThan(20);
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier();
    hashComponentIdentifier.setHash(longHash);
    assertThat(hashComponentIdentifier.getHash()).isEqualTo(longHash.substring(0, HashHelper.MAX_LENGTH));
  }
}
