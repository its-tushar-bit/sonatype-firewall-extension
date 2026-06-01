/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Maps a classic in-hash path (e.g. {@code /dashboard}) to the equivalent Nexus One
 * in-hash path. Phase 1 returns {@code null} for all routes — Epic 2+ registers mappings.
 */
export function classicToNexusOneUrl(classicHashPath: string): string | null {
  // Epic 2+ registers per-route mappings; Phase 1 returns null for all paths.
  return null;
}
