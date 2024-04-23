/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import SbomReleaseStatusTile from 'MainRoot/sbomManager/features/dashboard/sbomReleaseStatusTile/SbomReleaseStatusTile';

describe('SbomReleaseStatusTile', () => {
  it('renders the correct title', () => {
    render(<SbomReleaseStatusTile />);

    expect(screen.getByRole('heading', { name: /SBOM Release Status/i })).toBeVisible();
  });
});
