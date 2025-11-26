/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectUserTokensConfigurationSlice,
  selectFormState,
  selectLoading,
  selectSubmitMaskState,
  selectIsDirty,
  selectLoadError,
  selectUpdateError,
} from 'MainRoot/configuration/userTokensConfiguration/userTokensConfigurationSelectors';

describe('userTokensConfigurationSelectors', () => {
  const mockState = {
    userTokensConfiguration: {
      loading: false,
      loadError: null,
      updateError: 'Test error',
      isDirty: true,
      submitMaskState: false,
      formState: {
        userTokensEnabled: true,
        expirationEnabled: true,
        expirationDays: { value: '60', isPristine: false, validationErrors: null },
      },
      serverData: {
        userTokensEnabled: true,
        expirationEnabled: false,
        expirationDays: 30,
      },
    },
  };

  it('selectUserTokensConfigurationSlice should return the slice', () => {
    expect(selectUserTokensConfigurationSlice(mockState)).toEqual(mockState.userTokensConfiguration);
  });

  it('selectFormState should return the form state', () => {
    expect(selectFormState(mockState)).toEqual(mockState.userTokensConfiguration.formState);
  });

  it('selectLoading should return loading state', () => {
    expect(selectLoading(mockState)).toBe(false);
  });

  it('selectSubmitMaskState should return submitMaskState', () => {
    expect(selectSubmitMaskState(mockState)).toBe(false);
  });

  it('selectIsDirty should return isDirty', () => {
    expect(selectIsDirty(mockState)).toBe(true);
  });

  it('selectLoadError should return loadError', () => {
    expect(selectLoadError(mockState)).toBeNull();
  });

  it('selectUpdateError should return updateError', () => {
    expect(selectUpdateError(mockState)).toBe('Test error');
  });
});
