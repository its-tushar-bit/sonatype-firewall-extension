/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  isOverriddenOrSelected,
  renderObservedLicenses,
} from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LegalTabUtils';
import { render, screen } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('LegalTabUtils', () => {
  describe('isOverriddenOrSelected', () => {
    it('returns false if status is not Overridden or Selected', () => {
      expect(isOverriddenOrSelected('OPEN')).toBe(false);
      expect(isOverriddenOrSelected('ACKNOWLEDGED')).toBe(false);
      expect(isOverriddenOrSelected('CONFIRMED')).toBe(false);
      expect(isOverriddenOrSelected(null)).toBe(false);
    });

    it('returns true if status is `Selected`', () => {
      expect(isOverriddenOrSelected('SELECTED')).toBe(true);
    });

    it('returns true if status is `Overridden`', () => {
      expect(isOverriddenOrSelected('OVERRIDDEN')).toBe(true);
    });
  });

  describe('renderObservedLicenses', () => {
    const multiLicenses = [
      {
        licenses: [
          {
            license: {
              licenseId: 'test-license',
              licenseName: 'test-license',
            },
          },
        ],
      },
    ];

    it('renders the alert to enable the ALP Observed Licenses Detection', () => {
      render(renderObservedLicenses(null, false, true));
      expect(
        screen.getByText('Enable the Observed License Detection feature in the Advanced Legal Pack (ALP) add-on.')
      ).toBeInTheDocument();
    });

    it('renders the alert to get ALP when it is not enabled and the format supports Observed Licenses', () => {
      render(renderObservedLicenses(null, false, false, false, true));
      expect(screen.getByText('Get Advanced Legal Pack (ALP) to view Observed Licenses.')).toBeInTheDocument();
    });

    it('renders the Observed Licenses when ALP is disabled and the component format is not supported', () => {
      render(renderObservedLicenses(multiLicenses, false, false, false, false));
      expect(screen.getByText('test-license')).toBeInTheDocument();
    });

    it('renders the Observed Licenses when the ALP Observed License Detection is enabled', () => {
      render(renderObservedLicenses(multiLicenses, false, false, true, true));
      expect(screen.getByText('test-license')).toBeInTheDocument();
    });
  });
});
