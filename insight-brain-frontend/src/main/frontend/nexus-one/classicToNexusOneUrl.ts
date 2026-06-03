/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { toNexusOneEquivalent } from 'MainRoot/nosc/routing/classicPreviewMap';

/**
 * Maps a classic in-hash path (e.g. {@code /dashboard/violations}) to the equivalent
 * Nexus One in-hash path in the nexus-one bundle.
 */
export function classicToNexusOneUrl(classicHashPath: string): string | null {
  return toNexusOneEquivalent(classicHashPath);
}
