/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { toClassicEquivalent } from 'MainRoot/nosc/routing/classicPreviewMap';

/**
 * Maps a Nexus One in-hash path to the equivalent classic in-hash path.
 */
export function nexusOneToClassicUrl(nexusOneHashPath: string): string | null {
  return toClassicEquivalent(nexusOneHashPath);
}
