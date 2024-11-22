/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectComponentDetailsLicenseDetectionsTileSlice,
  selectShowEditLicensesPopover,
  selectLicenseDetectionsTileDataSlice,
  selectEditLicensesForm,
  selectEditLicensesFormIsDirty,
  selectIsUnsavedChangesModalActive,
} from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSelectors';

describe('LicenseDetectionsTile Selectors', () => {
  let mockState, componentDetailsLicenseDetectionsTile, LicenseDetectionsTileData, editLicensesForm;
  beforeEach(() => {
    editLicensesForm = {
      status: 'ACKNOWLEDGED',
      isDirty: false,
      submitError: null,
      submitMaskState: null,
    };

    LicenseDetectionsTileData = {
      licenseOverride: null,
      declaredLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      effectiveLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      observedLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      selectableLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      allLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      hiddenObservedLicenses: true,
      supportAlpObservedLicenses: true,
      loading: true,
      loadError: 'error',
      isAdvancedLegalPackSupported: true,
    };
    componentDetailsLicenseDetectionsTile = {
      ...LicenseDetectionsTileData,
      editLicensesForm,
      showEditLicensesPopover: false,
      isAdvancedLegalPackSupported: true,
    };

    mockState = {
      componentDetailsLicenseDetectionsTile,
      productFeatures: {
        productFeatures: {
          'advanced-legal-pack': true,
        },
      },
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

  describe('selectEditLicensesForm', () => {
    it('selects editLicensesForm', () => {
      const expectedSelection = editLicensesForm;

      const actualSelection = selectEditLicensesForm(mockState);

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectEditLicensesFormIsDirty', () => {
    it('selects editLicensesForm.isDirty', () => {
      const expectedSelection = editLicensesForm.isDirty;
      const actualSelection = selectEditLicensesFormIsDirty(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectIsUnsavedChangesModalActive', () => {
    it('selects editLicensesForm.showUnsavedChangesModal', () => {
      const expectedSelection = editLicensesForm.showUnsavedChangesModal;
      const actualSelection = selectIsUnsavedChangesModalActive(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });
});
