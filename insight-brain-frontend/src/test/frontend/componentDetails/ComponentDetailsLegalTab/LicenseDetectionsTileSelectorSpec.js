/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectComponentDetailsLicenseDetectionsTileSlice,
  selectShowEditLicensesPopover,
  selectLicenseDetectionsTileDataSlice,
} from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSelectors';

describe('LicenseDetectionsTile Selectors', () => {
  let mockState, componentDetailsLicenseDetectionsTile, LicenseDetectionsTileData;
  beforeEach(() => {
    LicenseDetectionsTileData = {
      licenseOverride: null,
      declaredlicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      effectiveLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      observedlicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      selectableLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      allLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      loading: true,
      loadError: 'error',
    };
    componentDetailsLicenseDetectionsTile = {
      ...LicenseDetectionsTileData,
      showEditLicensesPopover: false,
    };
    mockState = {
      componentDetailsLicenseDetectionsTile,
    };
  });

  describe('selectComponentDetailsLicenseDetectionsTileSlice', () => {
    it('selects componentDetailsLicenseDetectionsTileSlice', () => {
      const expectedSelection = componentDetailsLicenseDetectionsTile;

      const actualSelection = selectComponentDetailsLicenseDetectionsTileSlice(mockState);

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectLicenseDetectionsTileDataSlice', () => {
    it('selects licenseDetectionsTileDataSlice', () => {
      const expectedSelection = LicenseDetectionsTileData;

      const actualSelection = selectLicenseDetectionsTileDataSlice(mockState);

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectShowEditLicensesPopover', () => {
    it('selects showEditLicensesPopover', () => {
      const expectedSelection = false;

      const actualSelection = selectShowEditLicensesPopover(mockState);

      expect(actualSelection).toBe(expectedSelection);
    });
  });
});
