/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/waivedComponentUpgradesSlice';

const SAVE_UPGRADE_STAGE_REQUESTED = 'waivedComponentUpgrades/saveUpgradeStage/pending';
const SAVE_UPGRADE_STAGE_FULFILLED = 'waivedComponentUpgrades/saveUpgradeStage/fulfilled';
const SAVE_UPGRADE_STAGE_FAILED = 'waivedComponentUpgrades/saveUpgradeStage/rejected';

const LOAD_UPGRADE_STAGE_REQUESTED = 'waivedComponentUpgrades/loadUpgradeStage/pending';
const LOAD_UPGRADE_STAGE_FULFILLED = 'waivedComponentUpgrades/loadUpgradeStage/fulfilled';
const LOAD_UPGRADE_STAGE_FAILED = 'waivedComponentUpgrades/loadUpgradeStage/rejected';

describe('waivedComponentUpgrades reducers', () => {
  describe('waivedComponentUpgrades/saveUpgradeStage/pending', () => {
    it('resets submitMaskState, submitError properties', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitError: 'error',
      });

      const { submitMaskState, submitError } = reducer(state, {
        type: SAVE_UPGRADE_STAGE_REQUESTED,
      });

      expect(submitMaskState).toBeFalse();
      expect(submitError).toBeNull();
    });
  });

  describe('waivedComponentUpgrades/saveUpgradeStage/fulfilled', () => {
    it('resets submitMaskState, submitError, and isDirty properties', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitError: 'error',
        isDirty: true,
      });

      const { submitMaskState, submitError, isDirty, configuredStage } = reducer(state, {
        type: SAVE_UPGRADE_STAGE_FULFILLED,
        payload: {
          waivedComponentUpgradeStageTypeId: 'develop',
        },
      });

      expect(isDirty).toBeFalse();
      expect(submitMaskState).toBeTrue();
      expect(submitError).toBeNull();
      expect(configuredStage).toEqual('develop');
    });
  });

  describe('waivedComponentUpgrades/saveUpgradeStage/rejected', () => {
    it('sets submitMaskState, submitError properties', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitError: null,
      });

      const { submitMaskState, submitError } = reducer(state, {
        type: SAVE_UPGRADE_STAGE_FAILED,
        payload: 'error',
      });

      expect(submitMaskState).toBeNull();
      expect(submitError).toBe('error');
    });
  });

  describe('waivedComponentUpgrades/loadUpgradeStage/pending', () => {
    it('resets loadError and set loading properties', () => {
      const state = Object.freeze({
        loading: null,
        loadError: 'error',
      });

      const { loadError, loading } = reducer(state, {
        type: LOAD_UPGRADE_STAGE_REQUESTED,
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('waivedComponentUpgrades/loadUpgradeStage/fulfilled', () => {
    it('resets loading and set configuredStage properties', () => {
      const state = Object.freeze({
        loading: true,
        configuredStage: null,
      });

      const { loading, configuredStage } = reducer(state, {
        type: LOAD_UPGRADE_STAGE_FULFILLED,
        payload: {
          stage: 'develop',
        },
      });

      expect(loading).toBeFalse();
      expect(configuredStage).toEqual('develop');
    });
  });

  describe('waivedComponentUpgrades/loadUpgradeStage/rejected', () => {
    it('sets loading, configuredStage and loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        configuredStage: null,
        loadError: null,
      });

      const { loading, configuredStage, loadError } = reducer(state, {
        type: LOAD_UPGRADE_STAGE_FAILED,
        payload: 'error',
      });

      expect(configuredStage).toBeNull();
      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });
});
