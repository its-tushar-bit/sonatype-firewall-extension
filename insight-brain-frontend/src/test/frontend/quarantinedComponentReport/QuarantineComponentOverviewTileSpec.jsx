/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import QuarantineComponentOverviewTile from 'MainRoot/quarantinedComponentReport/componentOverviewTile/QuarantineComponentOverviewTile';
import { render, screen } from 'TestRoot/SpecUtil';
import React from 'react';

describe('QuarantineComponentOverviewTile', () => {
  let minimalProps, renderComponent;

  beforeEach(function () {
    minimalProps = {
      componentOverview: {
        componentDisplayName: 'a : b : 1',
        componentHash: 'someHash',
        matchState: 'someMatchState',
        pathname: 'somePathname',
        isQuarantined: true,
        quarantinedPolicyViolationsCount: 3,
        quarantinedDate: '2022-01-23T21:29:13.162+0000',
        repositoryId: 'someRepositoryId',
        repositoryName: 'maven-central',
      },
    };

    renderComponent = (additionalProps = {}) =>
      render(<QuarantineComponentOverviewTile {...minimalProps} {...additionalProps} />);
  });

  describe('renders a threat level indicator based on the quarantine status', () => {
    it('renders a threat level indicator as critical when the quarantine status is true', function () {
      renderComponent();
      const span = screen.getByText('Quarantined');
      const parent = span.parentElement;
      expect(parent).not.toBeNull();
      expect(parent.children[0].classList.toString()).toContain('nx-threat-indicator--critical');
    });

    it('renders a threat level indicator as none when the quarantine status is not true', function () {
      renderComponent({
        componentOverview: {
          ...minimalProps.componentOverview,
          isQuarantined: false,
        },
      });
      const span = screen.getByText('Unquarantined');
      const parent = span.parentElement;
      expect(parent).not.toBeNull();
      expect(parent.children[0].classList.toString()).toContain('nx-threat-indicator--none');
    });
  });
});
