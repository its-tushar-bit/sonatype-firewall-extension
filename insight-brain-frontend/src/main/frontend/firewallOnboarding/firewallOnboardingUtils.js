/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const stepsIds = { SELECT: 'select', PROTECT: 'protect' };
const select = { id: stepsIds.SELECT, index: 0, name: 'Select', title: 'Select proxy repositories' };
const protect = { id: stepsIds.PROTECT, index: 1, name: 'Protect', title: 'Inspect and complete onboarding' };

export const steps = [select, protect];

export const next = (step) => steps[step.index + 1];
export const prev = (step) => steps[step.index - 1];
