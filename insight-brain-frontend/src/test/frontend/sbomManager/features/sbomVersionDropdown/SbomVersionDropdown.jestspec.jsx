/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import SbomVersionDropdown from 'MainRoot/sbomManager/features/sbomVersionDropdown/SbomVersionDropdown';
import * as routerContext from 'MainRoot/react/RouterStateContext';

describe('SbomVersionDropdown', () => {
  const publicAppId = 'app123';
  const sbomVersion = '1.0-SNAPSHOT';
  const propsSingleSbom = {
    publicAppId: publicAppId,
    sbomVersions: [sbomVersion],
    currentSbomVersion: sbomVersion,
  };
  const propsMultSboms = {
    publicAppId: publicAppId,
    sbomVersions: ['1.0-SNAPSHOT', '1.1-SNAPSHOT', '1.2-SNAPSHOT'],
    currentSbomVersion: sbomVersion,
  };
  const renderDropdown = (props) => render(<SbomVersionDropdown {...props} />);

  describe('renders its dropdown label and links correctly when', () => {
    beforeEach(() => {
      jest.spyOn(routerContext, 'useRouterState').mockReturnValue({
        href: jest.fn((stateName, stateParams) => {
          if (stateName === 'sbomManager.management.view.bom') {
            return `/application/${stateParams.applicationPublicId}/bom/${stateParams.versionId}`;
          }
          return 'otherHref';
        }),
      });
    });

    it('there is only one SBOM version', async () => {
      renderDropdown(propsSingleSbom);

      const field = await screen.findByRole('button', { name: /Viewing:/i });
      expect(field).toHaveTextContent('Viewing: 1.0-SNAPSHOT');
      fireEvent.click(field);
      const emptyMessage = await screen.findByRole('button', { name: /No other SBOMs found/i });
      expect(emptyMessage).toBeVisible();
    });

    it('there are multiple SBOM versions', async () => {
      renderDropdown(propsMultSboms);

      const field = await screen.findByRole('button', { name: /Viewing:/i });
      expect(field).toHaveTextContent('Viewing: 1.0-SNAPSHOT');
      fireEvent.click(field);
      const link1 = await screen.findByRole('link', { name: /1.1-SNAPSHOT/i });
      expect(link1).toBeVisible();
      const link2 = await screen.findByRole('link', { name: /1.2-SNAPSHOT/i });
      expect(link2).toBeVisible();
    });
  });
});
