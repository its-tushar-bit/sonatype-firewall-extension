/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import RecentlyImportedSbomsTile from 'MainRoot/sbomManager/features/dashboard/recentlyImportedSbomsTile/RecentlyImportedSbomsTile';

describe('RecentlyImportedSbomsTile', () => {
  it('renders the correct title', () => {
    render(<RecentlyImportedSbomsTile />);

    expect(screen.getByRole('heading', { name: /Recently Imported SBOMs/i })).toBeVisible();
  });
});
