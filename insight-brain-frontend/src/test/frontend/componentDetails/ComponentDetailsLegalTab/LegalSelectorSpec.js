/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectComponentDetailsLegalSlice,
  selectShowEditLicensesPopover,
} from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LegalSelectors';

describe('ComponentDetailsLegal Selectors', () => {
  let mockState, componentDetailsLegal;
  beforeEach(() => {
    componentDetailsLegal = {
      showEditLicensesPopover: false,
    };
    mockState = {
      componentDetailsLegal,
    };
  });

  describe('selectComponentDetailsLegalSlice', () => {
    it('selects componentDetailsLegalSlice', () => {
      const expectedSelection = componentDetailsLegal;

      const actualSelection = selectComponentDetailsLegalSlice(mockState);

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
