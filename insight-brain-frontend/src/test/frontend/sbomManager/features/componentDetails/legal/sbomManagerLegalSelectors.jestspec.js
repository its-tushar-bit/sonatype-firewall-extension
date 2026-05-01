/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectSbomManagerComponentDetailsLoadingState,
  selectSbomManagerLicenseDetectionsTileDataSlice,
} from 'MainRoot/sbomManager/features/componentDetails/legal/sbomManagerLegalSelectors';
import { initialState as licenseDetectionsTileInitialState } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';

const baseState = {
  sbomComponentDetailsPage: {
    loading: false,
    loadError: null,
    componentDetails: null,
    componentDetailsPaginationData: null,
  },
  componentDetailsLicenseDetectionsTile: {
    ...licenseDetectionsTileInitialState,
  },
  productFeatures: {
    productFeatures: {},
  },
  router: {
    currentParams: { componentHash: 'abc123' },
  },
};

describe('sbomManagerLegalSelectors', () => {
  describe('selectSbomManagerComponentDetailsLoadingState', () => {
    it('testSelectSbomManagerComponentDetailsLoadingState_ReturnsLoadingAndLoadError', () => {
      const state = {
        ...baseState,
        sbomComponentDetailsPage: {
          loading: true,
          loadError: 'some error',
        },
      };

      const result = selectSbomManagerComponentDetailsLoadingState(state);

      expect(result).toEqual({ loading: true, loadError: 'some error' });
    });

    it('testSelectSbomManagerComponentDetailsLoadingState_DefaultsToNotLoading', () => {
      const result = selectSbomManagerComponentDetailsLoadingState(baseState);

      expect(result).toEqual({ loading: false, loadError: null });
    });
  });

  describe('selectSbomManagerLicenseDetectionsTileDataSlice', () => {
    it('testSelectSbomManagerLicenseDetectionsTileDataSlice_MapsIsLoadingComponentDetails', () => {
      const state = {
        ...baseState,
        sbomComponentDetailsPage: {
          loading: true,
          loadError: null,
        },
      };

      const result = selectSbomManagerLicenseDetectionsTileDataSlice(state);

      expect(result.isLoadingComponentDetails).toBe(true);
    });

    it('testSelectSbomManagerLicenseDetectionsTileDataSlice_MapsComponentDetailsLoadError', () => {
      const errorMessage = 'Failed to load component details';
      const state = {
        ...baseState,
        sbomComponentDetailsPage: {
          loading: false,
          loadError: errorMessage,
        },
      };

      const result = selectSbomManagerLicenseDetectionsTileDataSlice(state);

      expect(result.componentDetailsLoadError).toBe(errorMessage);
    });

    it('testSelectSbomManagerLicenseDetectionsTileDataSlice_UsesDeclaredLicensesFromTileData', () => {
      const declaredLicenses = [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
          licenses: [{ license: { licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }, threatLevel: 0 }],
        },
      ];
      const state = {
        ...baseState,
        sbomComponentDetailsPage: {
          loading: false,
          loadError: null,
          componentDetailsPaginationData: null,
        },
        componentDetailsLicenseDetectionsTile: {
          ...licenseDetectionsTileInitialState,
          declaredLicenses,
        },
        productFeatures: {
          productFeatures: {
            'advanced-legal-pack': true,
            'alp-for-sbom-manager': true,
          },
        },
      };

      const result = selectSbomManagerLicenseDetectionsTileDataSlice(state);

      expect(result.declaredLicenses).toBe(declaredLicenses);
      expect(result.isAdvancedLegalPackSupported).toBe(true);
    });

    it('testSelectSbomManagerLicenseDetectionsTileDataSlice_DeclaredLicensesNotAffectedByOverride', () => {
      const declaredLicenses = [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
          licenses: [{ license: { licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }, threatLevel: 0 }],
        },
      ];
      const overriddenEffective = [
        {
          licenseId: 'Beerware',
          licenseName: 'Beerware',
          licenses: [{ license: { licenseId: 'Beerware', licenseName: 'Beerware' }, threatLevel: 7 }],
        },
      ];
      const state = {
        ...baseState,
        sbomComponentDetailsPage: {
          loading: false,
          loadError: null,
          componentDetailsPaginationData: null,
        },
        componentDetailsLicenseDetectionsTile: {
          ...licenseDetectionsTileInitialState,
          declaredLicenses,
          effectiveLicenses: overriddenEffective,
          licenseOverride: [
            {
              ownerType: 'application',
              ownerId: 'app-id',
              licenseOverride: { status: 'OVERRIDDEN', licenseIds: ['Beerware'] },
            },
          ],
        },
      };

      const result = selectSbomManagerLicenseDetectionsTileDataSlice(state);

      expect(result.declaredLicenses).toBe(declaredLicenses);
      expect(result.effectiveLicenses).toBe(overriddenEffective);
    });

    it('testSelectSbomManagerLicenseDetectionsTileDataSlice_EffectiveLicensesUsesApiDataWhenNoOverride', () => {
      const declaredLicenses = [{ licenseId: 'Apache-2.0', licenseName: 'Apache-2.0', licenses: [] }];
      const apiEffectiveLicenses = [
        { licenseId: 'Apache-2.0', licenseName: 'Apache-2.0', licenses: [] },
        { licenseId: 'BSD', licenseName: 'BSD', licenses: [] },
      ];
      const state = {
        ...baseState,
        sbomComponentDetailsPage: {
          loading: false,
          loadError: null,
          componentDetailsPaginationData: null,
        },
        componentDetailsLicenseDetectionsTile: {
          ...licenseDetectionsTileInitialState,
          declaredLicenses,
          effectiveLicenses: apiEffectiveLicenses,
          licenseOverride: [{ ownerType: 'application', ownerId: 'app-id', licenseOverride: null }],
        },
      };

      const result = selectSbomManagerLicenseDetectionsTileDataSlice(state);

      expect(result.effectiveLicenses).toBe(apiEffectiveLicenses);
    });

    it('testSelectSbomManagerLicenseDetectionsTileDataSlice_EffectiveLicensesFallsBackToDeclaredWhenApiReturnsNull', () => {
      const declaredLicenses = [{ licenseId: 'Apache-2.0', licenseName: 'Apache-2.0', licenses: [] }];
      const state = {
        ...baseState,
        sbomComponentDetailsPage: {
          loading: false,
          loadError: null,
          componentDetailsPaginationData: null,
        },
        componentDetailsLicenseDetectionsTile: {
          ...licenseDetectionsTileInitialState,
          declaredLicenses,
          effectiveLicenses: null,
        },
      };

      const result = selectSbomManagerLicenseDetectionsTileDataSlice(state);

      expect(result.effectiveLicenses).toBe(declaredLicenses);
    });

    it('testSelectSbomManagerLicenseDetectionsTileDataSlice_EffectiveLicensesUsesApiDataWhenOverridden', () => {
      const declaredLicenses = [{ licenseId: 'Apache-2.0', licenseName: 'Apache-2.0', licenses: [] }];
      const overriddenLicenses = [{ licenseId: 'MIT', licenseName: 'MIT', licenses: [] }];
      const state = {
        ...baseState,
        sbomComponentDetailsPage: {
          loading: false,
          loadError: null,
          componentDetailsPaginationData: null,
        },
        componentDetailsLicenseDetectionsTile: {
          ...licenseDetectionsTileInitialState,
          declaredLicenses,
          licenseOverride: [
            {
              ownerType: 'application',
              ownerId: 'app-id',
              licenseOverride: { status: 'OVERRIDDEN', licenseIds: ['MIT'] },
            },
          ],
          effectiveLicenses: overriddenLicenses,
        },
      };

      const result = selectSbomManagerLicenseDetectionsTileDataSlice(state);

      expect(result.effectiveLicenses).toBe(overriddenLicenses);
      expect(result.declaredLicenses[0].licenseId).toBe('Apache-2.0');
    });
  });
});
