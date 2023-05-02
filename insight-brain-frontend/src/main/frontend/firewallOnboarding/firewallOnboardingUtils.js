/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const select = { id: 'select', index: 0, name: 'Select' };
const protect = { id: 'protect', index: 1, name: 'Protect' };

export const steps = [select, protect];

export const next = (step) => steps[step.index + 1];
export const prev = (step) => steps[step.index - 1];
