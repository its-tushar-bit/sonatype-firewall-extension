/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import HighPriorityVulnerabilitiesTile from 'MainRoot/sbomManager/features/dashboard/highPriorityVulnerabilitiesTile/HighPriorityVulnerabilitiesTile';

describe('HighPriorityVulnerabilitiesTile', () => {
  it('renders the correct title', () => {
    render(<HighPriorityVulnerabilitiesTile />);

    expect(screen.getByRole('heading', { name: /High Priority Vulnerabilities/i })).toBeVisible();
  });
});
