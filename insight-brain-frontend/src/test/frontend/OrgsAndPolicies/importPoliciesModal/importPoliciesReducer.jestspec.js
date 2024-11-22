/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/importPoliciesModal/importPoliciesSlice';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { nxFileUploadStateHelpers } from '@sonatype/react-shared-components';

const { initialState: rscInitialFileUploadState } = nxFileUploadStateHelpers;

describe('importPolicies reducer', () => {
  it('sets submitMaskState to true on /fulfilled', () => {
    const state = Object.freeze({
      submitMaskState: false,
      submitError: 'not null',
    });

    const newState = reducer(state, {
      type: `${OWNER_ACTIONS}/importPolicies/fulfilled`,
    });

    expect(newState).toEqual({
      submitMaskState: true,
      submitError: null,
    });
  });

  it('sets ownerFile on /selectFile', () => {
    const state = Object.freeze({
      ownerFile: rscInitialFileUploadState(null),
    });

    const testJSONFile = [
      {
        name: 'referencePolicies.json',
        type: 'application/json',
      },
    ];

    const newState = reducer(state, {
      type: `${OWNER_ACTIONS}/importPolicies/selectFile`,
      payload: { ...testJSONFile },
    });

    expect(newState.ownerFile).toEqual({
      isPristine: false,
      files: { 0: { name: 'referencePolicies.json', type: 'application/json' } },
    });
  });

  it('resets state on ownerActions/importPolicies/closeModal', () => {
    const state = Object.freeze({
      submitMaskState: false,
      submitError: 'not null',
    });

    const newState = reducer(state, {
      type: `${OWNER_ACTIONS}/importPolicies/closeModal`,
    });

    expect(newState).toEqual({
      submitMaskState: null,
      submitError: null,
      isModalOpen: false,
      ownerFile: { isPristine: true, files: null },
    });
  });

  it('sets error on import policies is failed', () => {
    const state = Object.freeze({
      submitMaskState: false,
      submitError: 'not null',
    });

    const newState = reducer(state, {
      type: `${OWNER_ACTIONS}/importPolicies/rejected`,
      payload: 'Some error',
    });

    expect(newState).toEqual({
      submitMaskState: null,
      submitError: 'Some error',
    });
  });
});
