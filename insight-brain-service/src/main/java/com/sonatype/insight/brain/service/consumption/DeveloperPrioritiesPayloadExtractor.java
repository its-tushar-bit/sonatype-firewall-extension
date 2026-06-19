/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.hds.AffectedComponentDTO;
import com.sonatype.insight.brain.hds.AffectedComponentList;

import jakarta.annotation.Nullable;

/**
 * Extracts entity ids for {@code DEVELOPER_PRIORITIES} consumption events from
 * the response of {@code /rest/vulnerability/affected}. Each entity id is the
 * pair {@code "{refId}|{format}:{namespace}:{name}:{version}"} where
 * {@code refId} is a CVE reference attributed to the component.
 *
 * <p>
 * One entity id is produced per {@code (refId, coordinates)} pair. Components
 * with no {@code refIds} are skipped. The pipe character separates the CVE ref
 * from the coordinate tuple to avoid collision with the colon-delimited segments
 * used inside the coordinate (and the colon-delimited outer key separator in
 * {@link IdempotencyKeyGenerator}).
 *
 * @since 1.205 (CLM-40771)
 */
public final class DeveloperPrioritiesPayloadExtractor
{
  private static final String COORDINATE_SEPARATOR = ":";

  private static final String REF_SEPARATOR = "|";

  private DeveloperPrioritiesPayloadExtractor() {
  }

  public static List<String> extract(@Nullable final AffectedComponentList payload) {
    if (payload == null || payload.getComponents() == null || payload.getComponents().isEmpty()) {
      return Collections.emptyList();
    }
    Set<String> dedup = new LinkedHashSet<>();
    for (AffectedComponentDTO component : payload.getComponents()) {
      if (component == null || component.refIds() == null) {
        continue;
      }
      String coordinates = formatCoordinates(component);
      for (String refId : component.refIds()) {
        if (refId == null || refId.isEmpty()) {
          continue;
        }
        dedup.add(refId + REF_SEPARATOR + coordinates);
      }
    }
    return List.copyOf(dedup);
  }

  private static String formatCoordinates(final AffectedComponentDTO component) {
    return String.join(
        COORDINATE_SEPARATOR,
        nullToEmpty(component.format()),
        nullToEmpty(component.namespace()),
        nullToEmpty(component.name()),
        nullToEmpty(component.version()));
  }

  private static String nullToEmpty(@Nullable final String s) {
    return s == null ? "" : s;
  }
}
