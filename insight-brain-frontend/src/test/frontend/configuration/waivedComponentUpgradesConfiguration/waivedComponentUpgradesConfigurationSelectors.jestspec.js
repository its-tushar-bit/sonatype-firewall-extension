/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectFormState,
  selectIsDirty,
  selectLoadError,
  selectLoading,
  selectSubmitMaskState,
  selectUpdateError,
  selectWaivedComponentUpgradesConfigurationSlice,
} from 'MainRoot/configuration/waivedComponentUpgradesConfiguration/waivedComponentUpgradesConfigurationSelectors';

describe('waivedComponentUpgradesConfigurationSelectors', () => {
  describe('selectFormState', () => {
    it('is composed from the following selector', () => {
      expect(selectFormState.dependencies).toEqual([selectWaivedComponentUpgradesConfigurationSlice]);
    });

    it('selects formState', () => {
      const waivedComponentUpgradesConfigurationSlice = { formState: { key: 'some form state' } };

      const actualSelection = selectFormState.resultFunc(waivedComponentUpgradesConfigurationSlice);

      expect(actualSelection).toEqual({ key: 'some form state' });
    });
  });

  describe('selectLoading', () => {
    it('is composed from the following selector', () => {
      expect(selectLoading.dependencies).toEqual([selectWaivedComponentUpgradesConfigurationSlice]);
    });

    it('selects loading', () => {
      const waivedComponentUpgradesConfigurationSlice = { loading: true };

      const actualSelection = selectLoading.resultFunc(waivedComponentUpgradesConfigurationSlice);

      expect(actualSelection).toBe(true);
    });
  });

  describe('selectSubmitMaskState', () => {
    it('is composed from the following selector', () => {
      expect(selectSubmitMaskState.dependencies).toEqual([selectWaivedComponentUpgradesConfigurationSlice]);
    });

    it('selects submitMaskState', () => {
      const waivedComponentUpgradesConfigurationSlice = { submitMaskState: false };

      const actualSelection = selectSubmitMaskState.resultFunc(waivedComponentUpgradesConfigurationSlice);

      expect(actualSelection).toBe(false);
    });
  });

  describe('selectIsDirty', () => {
    it('is composed from the following selector', () => {
      expect(selectIsDirty.dependencies).toEqual([selectWaivedComponentUpgradesConfigurationSlice]);
    });

    it('selects isDirty', () => {
      const waivedComponentUpgradesConfigurationSlice = { isDirty: null };

      const actualSelection = selectIsDirty.resultFunc(waivedComponentUpgradesConfigurationSlice);

      expect(actualSelection).toBeNull();
    });
  });

  describe('selectLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectLoadError.dependencies).toEqual([selectWaivedComponentUpgradesConfigurationSlice]);
    });

    it('selects loadError', () => {
      const waivedComponentUpgradesConfigurationSlice = { loadError: 'some load error' };

      const actualSelection = selectLoadError.resultFunc(waivedComponentUpgradesConfigurationSlice);

      expect(actualSelection).toBe('some load error');
    });
  });

  describe('selectUpdateError', () => {
    it('is composed from the following selector', () => {
      expect(selectUpdateError.dependencies).toEqual([selectWaivedComponentUpgradesConfigurationSlice]);
    });

    it('selects updateError', () => {
      const waivedComponentUpgradesConfigurationSlice = { updateError: 'some update error' };

      const actualSelection = selectUpdateError.resultFunc(waivedComponentUpgradesConfigurationSlice);

      expect(actualSelection).toBe('some update error');
    });
  });
});
