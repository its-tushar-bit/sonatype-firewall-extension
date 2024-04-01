/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectBillOfMaterialsPage,
  selectInternalApplicationId,
  selectInternalApplicationIdIsLoading,
  selectInternalApplicationIdError,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSelectors.js';

let mockState;

describe('sbomTileSelectors returns the correct state for the following selector:', () => {
  beforeEach(() => {
    mockState = {
      billOfMaterialsPage: {
        loading: false,
        errorInternalAppId: null,
        internalAppId: null,
        publicAppId: null,
      },
    };
  });

  it('selectBillOfMaterialsPage', () => {
    expect(selectBillOfMaterialsPage(mockState)).toEqual(mockState.billOfMaterialsPage);
  });

  it('selectInternalApplicationId', () => {
    expect(selectInternalApplicationId(mockState)).toEqual(mockState.billOfMaterialsPage.internalAppId);
  });

  it('selectInternalApplicationIdIsLoading', () => {
    expect(selectInternalApplicationIdIsLoading(mockState)).toEqual(mockState.billOfMaterialsPage.loading);
  });

  it('selectInternalApplicationIdError', () => {
    expect(selectInternalApplicationIdError(mockState)).toEqual(mockState.billOfMaterialsPage.errorInternalAppId);
  });
});
