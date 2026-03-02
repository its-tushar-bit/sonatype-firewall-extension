/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const HELP_URL = 'http://links.sonatype.com/products/sbom-manager-learn-more';

export const BATCH_SIZE = 50;

// Auto-expand threshold: total node count including all descendants
// Small SBOMs (≤ threshold) are automatically expanded on load
// Large SBOMs (> threshold) remain collapsed to maintain performance
export const AUTO_EXPAND_THRESHOLD = 1000;

export const MIN_SEARCH_LENGTH = 2;

export const MAX_SEARCH_LENGTH = 100;

export const SEARCH_DEBOUNCE_TIMEOUT_MS = 300;

export const MAX_SEARCH_DEPTH = 32;

// Maximum length for truncating display names in tree nodes
export const MAX_TITLE_LENGTH = 50;
