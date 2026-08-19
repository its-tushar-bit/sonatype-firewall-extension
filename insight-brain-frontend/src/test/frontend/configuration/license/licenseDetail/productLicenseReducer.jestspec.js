/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  PRODUCT_LICENSE_LOAD_REQUESTED,
  PRODUCT_LICENSE_LOAD_FAILED,
  PRODUCT_LICENSE_LOAD_FULFILLED,
  PRODUCT_LICENSE_INVALID,
  PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED,
  PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED,
  PRODUCT_LICENSE_SUBMIT_MASK_TIMER_DONE,
  PRODUCT_LICENSE_UPDATE_LICENSE_FAILED,
  PRODUCT_LICENSE_CLEAR_UPDATE_LICENSE_ERROR,
  PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED,
  PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED,
  PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL,
  PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE,
} from '../../../../../main/frontend/configuration/license/productLicenseActions';

import reduce, {
  initialState as actualInitialState,
} from '../../../../../main/frontend/configuration/license/productLicenseReducer';

describe('ProductLicenseReducer', () => {
  let initialState;
  const otherObject = {};

  beforeEach(() => {
    initialState = { ...actualInitialState };
  });

  describe(`${PRODUCT_LICENSE_LOAD_REQUESTED} action`, () => {
    it('returns initialState', () => {
      const action = { type: PRODUCT_LICENSE_LOAD_REQUESTED };
      const newState = reduce(initialState, action);
      expect(newState).toEqual(initialState);
    });
  });

  describe(`${PRODUCT_LICENSE_LOAD_FULFILLED} action`, () => {
    let newState;
    const payload = {},
      action = { type: PRODUCT_LICENSE_LOAD_FULFILLED, payload };

    beforeEach(() => {
      const state = {
        otherObject,
        license: {},
        loading: true,
        loadError: 'some error',
        installed: false,
      };
      newState = reduce(state, action);
    });

    it('does not modify other properties', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('fills the license prop with the value in payload', () => {
      expect(newState.license).toEqual(payload);
    });

    it('sets false to loading prop', () => {
      expect(newState.loading).toBe(false);
    });

    it('sets null to loadError', () => {
      expect(newState.loadError).toBeNull();
    });

    it('sets true to installed prop', () => {
      expect(newState.installed).toBe(true);
    });
  });

  describe(`${PRODUCT_LICENSE_LOAD_FAILED} action`, () => {
    let newState;
    const payload = 'some error',
      action = { type: PRODUCT_LICENSE_LOAD_FAILED, payload };

    beforeEach(() => {
      const state = {
        loading: true,
        loadError: null,
        otherObject,
      };
      newState = reduce(state, action);
    });

    it('does not modify unrelated properties', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets false to loading prop', () => {
      expect(newState.loading).toBe(false);
    });

    it('fills loadError prop with the value in payload', () => {
      expect(newState.loadError).toBe(payload);
    });
  });

  describe(`${PRODUCT_LICENSE_INVALID} action`, () => {
    let newState;

    beforeEach(() => {
      const state = {
        loading: true,
        loadError: {},
        license: {},
        otherObject,
        installed: true,
      };
      const action = {
        type: PRODUCT_LICENSE_INVALID,
      };

      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets false to loading', () => {
      expect(newState.loading).toBe(false);
    });

    it('sets null to license prop', () => {
      expect(newState.license).toBeNull();
    });

    it('sets null to loadingError prop', () => {
      expect(newState.loadError).toBeNull();
    });

    it('sets false to installed prop', () => {
      expect(newState.installed).toBe(false);
    });
  });

  describe(`${PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED} action`, () => {
    let newState;

    beforeEach(() => {
      const state = {
        otherObject,
        submitMaskState: null,
      };

      const action = {
        type: PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED,
      };

      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets false to submitMaskState', () => {
      expect(newState.submitMaskState).toBe(false);
    });
  });

  describe(`${PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED} action`, () => {
    let newState;

    beforeEach(() => {
      const state = {
        otherObject,
        updateLicenseError: 'some error',
        submitMaskState: null,
        installed: false,
      };

      const action = {
        type: PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED,
      };

      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets null to updateLicenseError', () => {
      expect(newState.updateLicenseError).toBeNull();
    });

    it('sets true to submitMaskState', () => {
      expect(newState.submitMaskState).toBe(true);
    });

    it('sets true to submitMaskState', () => {
      expect(newState.installed).toBe(true);
    });
  });

  describe(`${PRODUCT_LICENSE_SUBMIT_MASK_TIMER_DONE} action`, () => {
    let newState;

    beforeEach(() => {
      const state = {
        otherObject,
        submitMaskState: true,
      };

      const action = {
        type: PRODUCT_LICENSE_SUBMIT_MASK_TIMER_DONE,
      };

      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets null to submitMaskState', () => {
      expect(newState.submitMaskState).toBeNull();
    });
  });

  describe(`${PRODUCT_LICENSE_UPDATE_LICENSE_FAILED} actions`, () => {
    let newState;
    const payload = 'some error happened';

    beforeEach(() => {
      const state = {
        otherObject,
        submitMaskState: true,
        updateLicenseError: null,
      };

      const action = {
        type: PRODUCT_LICENSE_UPDATE_LICENSE_FAILED,
        payload,
      };

      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets null to submitMaskState', () => {
      expect(newState.submitMaskState).toBeNull();
    });

    it('sets null to updateLicenseError', () => {
      expect(newState.updateLicenseError).toBe(payload);
    });
  });

  describe(`${PRODUCT_LICENSE_CLEAR_UPDATE_LICENSE_ERROR} action`, () => {
    let newState;

    beforeEach(() => {
      const state = {
        otherObject,
        updateLicenseError: 'some error happened',
      };

      const action = {
        type: PRODUCT_LICENSE_CLEAR_UPDATE_LICENSE_ERROR,
      };

      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets null to updateLicenseError', () => {
      expect(newState.updateLicenseError).toBeNull();
    });
  });

  describe(`${PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED} action`, () => {
    let newState;

    beforeEach(() => {
      const state = {
        otherObject,
        uninstallMaskState: null,
      };
      const action = {
        type: PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED,
      };
      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets false to uninstallMaskState', () => {
      expect(newState.uninstallMaskState).toBe(false);
    });
  });

  describe(`${PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED} action`, () => {
    let newState;

    beforeEach(() => {
      const state = {
        otherObject,
        uninstallMaskState: false,
        uninstallError: 'some error',
        installed: true,
      };

      const action = {
        type: PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED,
      };

      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets true to uninstallMaskState', () => {
      expect(newState.uninstallMaskState).toBe(true);
    });

    it('sets null to uninstallError', () => {
      expect(newState.uninstallError).toBeNull();
    });

    it('sets false to installed prop', () => {
      expect(newState.installed).toBe(false);
    });
  });

  describe(`${PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL} action`, () => {
    let newState;
    const payload = 'some error happened';

    beforeEach(() => {
      const state = {
        otherObject,
        uninstallError: null,
        uninstallMaskState: false,
      };

      const action = {
        type: PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL,
        payload,
      };

      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets the payload to uninstallError', () => {
      expect(newState.uninstallError).toBe(payload);
    });

    it('sets null to uninstallMaskState', () => {
      expect(newState.uninstallMaskState).toBeNull();
    });
  });

  describe(`${PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE} action`, () => {
    let newState;

    beforeEach(() => {
      const state = {
        otherObject,
        uninstallMaskState: true,
        loading: false,
      };

      const action = {
        type: PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE,
      };

      newState = reduce(state, action);
    });

    it('checks for immutability', () => {
      expect(newState.otherObject).toBe(otherObject);
    });

    it('sets null to uninstallMaskState', () => {
      expect(newState.uninstallMaskState).toBeNull();
    });

    it('sets true to loading', () => {
      expect(newState.loading).toBe(true);
    });
  });
});
