/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import java.util.Collections;

import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ProprietaryConfigTest
{
  @Test
  public void testSetPackages() {
    ProprietaryConfig config = new ProprietaryConfig("ownerId", null /* packages */, null /* regexes */);
    assertThat(config.getPackages(), hasSize(0));
    assertThat(config.getPackagesJson(), is(nullValue()));
    assertThat(config.getRegexes(), hasSize(0));
    assertThat(config.getRegexesJson(), is(nullValue()));

    config = new ProprietaryConfig("ownerId", Collections.<String> emptyList() /* packages */, null /* regexes */);
    assertThat(config.getPackages(), hasSize(0));
    assertThat(config.getPackagesJson(), is(nullValue()));
    assertThat(config.getRegexes(), hasSize(0));
    assertThat(config.getRegexesJson(), is(nullValue()));

    config = new ProprietaryConfig("ownerId", Collections.singletonList("foo") /* packages */, null /* regexes */);
    assertThat(config.getPackages(), hasSize(1));
    assertThat(config.getPackages().get(0), is("foo"));
    assertThat(config.getPackagesJson(), is("[\"foo\"]"));
    assertThat(config.getRegexes(), hasSize(0));
    assertThat(config.getRegexesJson(), is(nullValue()));

    config.setPackages(null);
    assertThat(config.getPackages(), hasSize(0));
    assertThat(config.getPackagesJson(), is(nullValue()));
    assertThat(config.getRegexes(), hasSize(0));
    assertThat(config.getRegexesJson(), is(nullValue()));

    config.setPackages(Collections.<String> emptyList());
    assertThat(config.getPackages(), hasSize(0));
    assertThat(config.getPackagesJson(), is(nullValue()));
    assertThat(config.getRegexes(), hasSize(0));
    assertThat(config.getRegexesJson(), is(nullValue()));

    config.setPackages(Collections.singletonList("bar"));
    assertThat(config.getPackages(), hasSize(1));
    assertThat(config.getPackages().get(0), is("bar"));
    assertThat(config.getPackagesJson(), is("[\"bar\"]"));
    assertThat(config.getRegexes(), hasSize(0));
    assertThat(config.getRegexesJson(), is(nullValue()));
  }

  @Test
  public void testSetRegexes() {
    ProprietaryConfig config = new ProprietaryConfig("ownerId", null /* packages */, null /* regexes */);
    assertThat(config.getPackages(), hasSize(0));
    assertThat(config.getPackagesJson(), is(nullValue()));
    assertThat(config.getRegexes(), hasSize(0));
    assertThat(config.getRegexesJson(), is(nullValue()));

    config = new ProprietaryConfig("ownerId", null /* packages */, Collections.<String> emptyList() /* regexes */);
    assertThat(config.getPackages(), hasSize(0));
    assertThat(config.getPackagesJson(), is(nullValue()));
    assertThat(config.getRegexes(), hasSize(0));
    assertThat(config.getRegexesJson(), is(nullValue()));

    config = new ProprietaryConfig("ownerId", null /* packages */, Collections.singletonList("foo") /* regexes */);
    assertThat(config.getPackages(), hasSize(0));
    assertThat(config.getPackagesJson(), is(nullValue()));
    assertThat(config.getRegexes(), hasSize(1));
    assertThat(config.getRegexes().get(0), is("foo"));
    assertThat(config.getRegexesJson(), is("[\"foo\"]"));

    config.setRegexes(null);
    assertThat(config.getPackages(), hasSize(0));
    assertThat(config.getPackagesJson(), is(nullValue()));
    assertThat(config.getRegexes(), hasSize(0));
    assertThat(config.getRegexesJson(), is(nullValue()));

    config.setRegexes(Collections.<String> emptyList());
    assertThat(config.getPackages(), hasSize(0));
    assertThat(config.getPackagesJson(), is(nullValue()));
    assertThat(config.getRegexes(), hasSize(0));
    assertThat(config.getRegexesJson(), is(nullValue()));

    config.setRegexes(Collections.singletonList("bar"));
    assertThat(config.getPackages(), hasSize(0));
    assertThat(config.getPackagesJson(), is(nullValue()));
    assertThat(config.getRegexes(), hasSize(1));
    assertThat(config.getRegexes().get(0), is("bar"));
    assertThat(config.getRegexesJson(), is("[\"bar\"]"));
  }
}
