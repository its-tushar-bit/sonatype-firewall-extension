/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { selectOwnerProperties } from '../orgsAndPoliciesSelectors';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import { copyToClipboard } from 'MainRoot/util/jsUtil';

const REDUCER_NAME = `${OWNER_ACTIONS}/copyIdToClipboard`;

const copyIdToClipboardAction = createAsyncThunk(REDUCER_NAME, async (_, { getState, dispatch }) => {
  const { ownerId } = selectOwnerProperties(getState());
  try {
    // the clipboard object is the modern API, but it is only available in secure contexts (ie https or localhost)
    if (window.navigator.clipboard) {
      await navigator.clipboard.writeText(ownerId);
    } else {
      // document.execCommand works outside of https, but is deprecated and might be deactivated some day
      copyToClipboard(ownerId);
    }

    dispatch(toastActions.addToast({ type: 'success', message: 'Copied!' }));
  } catch (error) {
    dispatch(toastActions.addToast({ type: 'error', message: 'An error occured.' }));
  }
});

export default copyIdToClipboardAction;
