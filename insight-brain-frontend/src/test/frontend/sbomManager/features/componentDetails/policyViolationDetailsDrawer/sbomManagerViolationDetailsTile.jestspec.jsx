/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';

import SbomManagerViolationDetailsTile from 'MainRoot/sbomManager/features/componentDetails/policyViolationDetailsDrawer/SbomManagerViolationDetailsTile';

describe('SbomManagerViolationDetailsTile', () => {
  const renderComponent = (preloadedState) => render(<SbomManagerViolationDetailsTile />, { preloadedState });
  const checkColumnExistanceAndValue = (column, value) => {
    const columnValue = screen.getByRole('definition', { name: column });
    expect(columnValue).toBeInTheDocument();
    expect(columnValue).toHaveTextContent(value);
  };

  it('renders correctly', async () => {
    const initialState = Object.freeze({
      sbomComponentDetailsPage: {
        policyViolationDetailsDrawer: {
          violationDetails: {
            policyThreatCategory: 'SECURITY',
            threatLevel: 10,
          },
        },
      },
    });
    renderComponent(initialState);

    checkColumnExistanceAndValue('Threat Level', '10');
    checkColumnExistanceAndValue('Policy Type', 'Security');
  });
});
