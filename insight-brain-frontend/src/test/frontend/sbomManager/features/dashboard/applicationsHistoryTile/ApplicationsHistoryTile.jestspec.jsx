/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ApplicationsHistoryTile from 'MainRoot/sbomManager/features/dashboard/applicationsHistoryTile/ApplicationsHistoryTile';

describe('ApplicationsHistoryTile', () => {
  it('renders the correct title', () => {
    render(<ApplicationsHistoryTile />);

    expect(screen.getByRole('heading', { name: /Applications History/i })).toBeVisible();
  });
});
