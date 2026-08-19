/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.System.currentTimeMillis;

public class RandomGenerator
{
  private static final Logger log = LoggerFactory.getLogger(RandomGenerator.class);

  private final long seed;

  private final Random random;

  private final String name;

  public RandomGenerator(final Class<?> klazz) {
    this(klazz.getSimpleName());
  }

  public RandomGenerator(final String name) {
    this(name, currentTimeMillis());
  }

  public RandomGenerator(final String name, final long seed) {
    this.name = name;
    this.seed = seed;
    random = new Random(seed);
    log.debug("Created random generator '{}' with seed: {}", name, seed);
  }

  public long getSeed() {
    return seed;
  }

  public String getName() {
    return name;
  }

  public int randomInt(final int fromInclusive, final int toExclusive) {
    checkArgument(fromInclusive < toExclusive, "The from parameter must be less than the to parameter");
    return random.nextInt(toExclusive - fromInclusive) + fromInclusive;
  }

  public <T> T randomElement(final List<T> items) {
    final int idx = random.nextInt(items.size());
    return items.get(idx);
  }

  public boolean randomBoolean() {
    return random.nextInt(2) == 1;
  }

  public <E extends Enum<E>> E randomEnum(final Class<E> enumClass) {
    final List<E> enumList = ImmutableList.copyOf(EnumSet.allOf(enumClass));
    return randomElement(enumList);
  }
}
