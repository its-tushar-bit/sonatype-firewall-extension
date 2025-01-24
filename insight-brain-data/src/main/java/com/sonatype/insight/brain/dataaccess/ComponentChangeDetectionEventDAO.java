/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;

/**
 * This is an in-memory representation of the component change detection configuration.
 * It is in-memory as a temporary measure to unblock the remaining of NEXUS-45271 and to establish the correct schema.
 */
@Named
@Singleton
public class ComponentChangeDetectionEventDAO
{
  private static final List<ComponentChangeDetectionEvent> table = new ArrayList<>();

  @Inject
  public ComponentChangeDetectionEventDAO() {
    // Empty
  }

  public void addEvent(ComponentChangeDetectionEvent event) {
    table.add(event);
  }

  public List<ComponentChangeDetectionEvent> getAll() {
    return table;
  }

  public void deleteEntriesOlderThan(DateTime time) {
    table.removeIf(event -> event.addedTime.isBefore(time));
  }

  public record ComponentChangeDetectionEvent(String purl, String data, DateTime addedTime)
  {
    public ComponentChangeDetectionEvent(String purl, String data) {
      this(purl, data, DateTime.now());
    }
  }
}
