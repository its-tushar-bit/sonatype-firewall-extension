/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import LearnMoreSbomManager from 'MainRoot/sbomManager/features/LearnMoreSbomManager';

describe('LearnMoreSbomManager', () => {
  it('renders page content', () => {
    render(<LearnMoreSbomManager />);
    expect(screen.getByText('SBOM Manager is currently not enabled for your organization.')).toBeVisible();
    expect(screen.getByText('Learn more about SBOM Manager.')).toBeVisible();
  });
});
