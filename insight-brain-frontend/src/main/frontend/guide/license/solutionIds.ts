/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { SolutionId } from '../layout/ProductSwitcher/productMetadata';

/**
 * Canonical {@link SolutionId} for the Sonatype AI Developer product (frontend id retained from
 * its earlier "Guide" name). {@code fetchLicensedSolutions} canonicalizes {@link
 * AI_DEVELOPER_SOLUTION_ID} onto this id so the license gate, provider, and product switcher all
 * key off one value and render "AI Developer" branding.
 */
export const GUIDE_SOLUTION_ID: SolutionId = 'guide';

/**
 * Solution id emitted by /api/v2/solutions/licensed for the AI Developer SKU (GUIDE-3124).
 * Canonicalized onto {@link GUIDE_SOLUTION_ID} at the fetch boundary (see {@code
 * fetchLicensedSolutions}); not used as a typed {@link SolutionId} beyond that boundary.
 */
export const AI_DEVELOPER_SOLUTION_ID: string = 'aiDeveloper';
