/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;

/**
 * This is an in-memory representation of the component change detection configuration.
 * It is in-memory as a temporary measure to unblock the remaining of NEXUS-45271 and to establish the correct schema.
 */
@Named
@Singleton
public class ComponentChangeDetectionConfigurationDAO
{
  private final List<ComponentChangeConfiguration> table = new ArrayList<>();

  public List<ComponentChangeConfiguration> addComponents(
      final long maxComponents,
      final List<ComponentChangeConfiguration> items)
  {
    // SQL equivalent: INSERT INTO table (purl, hash, addedTime) VALUES (purl, hash, addedTime)
    // First insert the items in DateTime order
    table.addAll(items);
    table.sort(Comparator.comparing(ComponentChangeConfiguration::addedTime));

    if (table.size() > maxComponents) {
      int removeCount = table.size() - (int) maxComponents;

      // Remove the oldest components to make room for the new ones
      // SQL equivalent: DELETE FROM table WHERE addedTime IN
      // (SELECT addedTime FROM table ORDER BY addedTime LIMIT removeCount)
      List<ComponentChangeConfiguration> removeComponents = table.subList(0, removeCount).stream().toList();
      table.subList(0, removeCount).clear();
      return removeComponents;
    }

    return List.of();
  }

  public void updateHashOfPurl(String purl, String hash) {
    for (int i = 0; i < table.size(); i++) {
      ComponentChangeConfiguration item = table.get(i);
      if (item.purl().equals(purl)) {
        table.set(i, new ComponentChangeConfiguration(item.purl(), hash, item.addedTime()));
        break;
      }
    }
  }

  public Long getCount() {
    return (long) table.size();
  }

  public List<ComponentChangeConfiguration> getComponents(final int page, final int pageSize) {
    // An example of how to accomplish pagination
    // src/main/java/com/sonatype/insight/brain/dataaccess/AbstractSqlDAO.java#L200
    if (page <= 0 || pageSize <= 0) {
      throw new IllegalArgumentException("Page and pageSize must be greater than 0");
    }

    int pageStart = (page - 1) * pageSize;
    int pageEnd = Math.min(page * pageSize, table.size());

    return table.subList(pageStart, pageEnd);
  }

  public record ComponentChangeConfiguration(Integer id, String purl, String componentHash,
                                             String comparisonHash, DateTime addedTime)
  {
    public ComponentChangeConfiguration(String purl, String comparisonHash, DateTime addedTime) {
      this(null, purl, null, comparisonHash, addedTime);
    }
  }
}
