/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SamlUserTest
{
  @Test
  public void testSetGetGroups() {
    SamlUser samlUser = new SamlUser();

    samlUser.setGroups(null);
    assertThat(samlUser.getGroups()).isEmpty();

    Set<String> groups = new LinkedHashSet<>();
    samlUser.setGroups(groups);
    assertThat(samlUser.getGroups()).isEmpty();

    groups = new LinkedHashSet<>(Collections.singletonList("group1"));
    samlUser.setGroups(groups);
    assertThat(samlUser.getGroups()).isEqualTo(groups);

    groups = new LinkedHashSet<>(Arrays.asList("group1", "group2"));
    samlUser.setGroups(groups);
    assertThat(samlUser.getGroups()).isEqualTo(groups);

    groups = new LinkedHashSet<>(Arrays.asList("group1", "group2", "group3"));
    samlUser.setGroups(groups);
    assertThat(samlUser.getGroups()).isEqualTo(groups);

    groups = new LinkedHashSet<>(Arrays.asList("aa=foo,bb=bar,cc=baz", "group2", "group3"));
    samlUser.setGroups(groups);
    assertThat(samlUser.getGroups()).isEqualTo(groups);
  }

  @Test
  public void testCalculateDisplayName_Null() {
    SamlUser samlUser = new SamlUser();

    assertThat(samlUser.calculateDisplayName()).isNull();
  }

  @Test
  public void testCalculateDisplayName_OnlyUsername() {
    SamlUser samlUser = new SamlUser();
    samlUser.setUsername("username");

    assertThat(samlUser.calculateDisplayName()).isEqualTo(samlUser.getUsername());
  }

  @Test
  public void testCalculateDisplayName_FirstName() {
    SamlUser samlUser = new SamlUser();
    samlUser.setUsername("username");

    samlUser.setFirstName("");
    assertThat(samlUser.calculateDisplayName()).isEqualTo(samlUser.getUsername());

    samlUser.setFirstName("Bob");
    assertThat(samlUser.calculateDisplayName()).isEqualTo("Bob");
  }

  @Test
  public void testCalculateDisplayName_LastName() {
    SamlUser samlUser = new SamlUser();
    samlUser.setUsername("username");

    samlUser.setLastName("");
    assertThat(samlUser.calculateDisplayName()).isEqualTo(samlUser.getUsername());

    samlUser.setLastName("Smith");
    assertThat(samlUser.calculateDisplayName()).isEqualTo("Smith");
  }

  @Test
  public void testCalculateDisplayName_FirstNameAndLastName() {
    SamlUser samlUser = new SamlUser();
    samlUser.setUsername("username");

    samlUser.setFirstName("");
    samlUser.setLastName("");
    assertThat(samlUser.calculateDisplayName()).isEqualTo(samlUser.getUsername());

    samlUser.setFirstName("Bob");
    samlUser.setLastName("Smith");
    assertThat(samlUser.calculateDisplayName()).isEqualTo("Bob Smith");
  }
}
