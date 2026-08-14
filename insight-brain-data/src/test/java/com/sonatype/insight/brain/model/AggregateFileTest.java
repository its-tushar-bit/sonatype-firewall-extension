/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AggregateFileTest
    extends AbstractDataTest
{
  @Test
  public void testCreate() {
    String ownerComponentId = TemporaryEntity.uuid();
    assertThat(new AggregateFile(ownerComponentId, null, null).getOwnerComponentId())
        .isEqualTo(ownerComponentId);
    String hash = "hash";
    assertThat(new AggregateFile(null, hash, null).getHash()).isEqualTo(hash);
    assertThat(createWithPathnames(null).getPathnames()).isEmpty();
    assertThat(createWithPathnames(null).getPathnamesString()).isNull();
    assertThat(createWithPathnames(Collections.emptySet()).getPathnames()).isEmpty();
    assertThat(createWithPathnames(Collections.emptySet()).getPathnamesString()).isNull();
    Set<String> pathnames = Sets.newLinkedHashSet(Arrays.asList("pathname1", "pathname2"));
    assertThat(createWithPathnames(pathnames).getPathnames()).isEqualTo(pathnames);
    assertThat(createWithPathnames(pathnames).getPathnamesString()).isEqualTo(String.join("\n", pathnames));
  }

  private AggregateFile createWithPathnames(Set<String> pathnames) {
    return new AggregateFile(null, null, pathnames);
  }
}
