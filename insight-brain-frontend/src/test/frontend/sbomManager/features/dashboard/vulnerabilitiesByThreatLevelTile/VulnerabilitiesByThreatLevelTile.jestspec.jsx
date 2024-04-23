/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import VulnerabilitiesByThreatLevelTile from 'MainRoot/sbomManager/features/dashboard/vulnerabilitiesByThreatLevelTile/VulnerabilitiesByThreatLevelTile';

describe('VulnerabilitiesByThreatLevelTile', () => {
  it('renders the correct title', () => {
    render(<VulnerabilitiesByThreatLevelTile />);

    expect(screen.getByRole('heading', { name: /Vulnerabilities by Threat Level/i })).toBeVisible();
    expect(screen.getByRole('heading', { name: /(all time)/i })).toBeVisible();
  });
});
