/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.hash.Hashing;
import jakarta.annotation.Nullable;

/**
 * Derives a stable entity id for HDS calls that submit a set of PackageUrlIdentifiers
 * in the POST body (rest/component/dependencies). Sorts the PURLs by their string form,
 * joins with '|', and returns the SHA-256 truncated to 16 hex chars (64 bits).
 *
 * <p>
 * Two engagements that query the exact same set of PURLs produce the same id;
 * different sets produce different ids. Combined with userId/scanId/sessionHash in the
 * 5-segment key, this guarantees per-session dedup for repeat Component Details page
 * opens (the same page → same alt-version set → same id), while honouring distinct
 * actions like "Compare Versions" which alter the set.
 *
 * <p>
 * Returns null when the body is null, not a Collection, empty, or contains non-PURL
 * elements — those cases fall through to a NULL-keyed event (no dedup), matching the
 * generator's existing null-on-missing-segment contract.
 *
 * @since 1.205 (CLM-40771 follow-up)
 */
public final class PackageUrlIdentifierSetBodyExtractor
{
  /** Truncated SHA-256 length — same convention as sessionIdHash in IdempotencyKeyGenerator. */
  private static final int HASH_LENGTH = 16;

  private PackageUrlIdentifierSetBodyExtractor() {
  }

  @Nullable
  public static String extract(@Nullable Object body) {
    if (!(body instanceof Collection<?> collection) || collection.isEmpty()) {
      return null;
    }
    String joined = collection.stream()
        .filter(PackageUrlIdentifier.class::isInstance)
        .map(p -> ((PackageUrlIdentifier) p).getPackageUrl())
        .filter(Objects::nonNull)
        .sorted()
        .distinct()
        .collect(Collectors.joining("|"));
    if (joined.isEmpty()) {
      return null;
    }
    return Hashing.sha256()
        .hashString(joined, StandardCharsets.UTF_8)
        .toString()
        .substring(0, HASH_LENGTH);
  }
}
