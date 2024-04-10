/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics.datadog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.micrometer.core.instrument.Tag;

public class DatadogTags
    implements Iterable<Tag>
{
  private final List<Tag> datadogTagsList = new ArrayList<>();

  @Override
  public Iterator<Tag> iterator() {
    return datadogTagsList.iterator();
  }

  public DatadogTags add(Object... tags) {
    if (tags.length % 2 != 0) {
      throw new IllegalArgumentException("incomplete key-value pair");
    }
    for (int i = 0; i < tags.length; i += 2) {
      String key = Objects.toString(tags[i], null);
      String value = Objects.toString(tags[i + 1], null);
      if (value != null) {
        this.datadogTagsList.add(Tag.of(key, value));
      }
    }
    return this;
  }
}
