/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { omit, pick } from 'ramda';
import { createAsyncThunk } from '@reduxjs/toolkit';
import { getOrganizationsUrl, getApplicationsUrl } from '../util/CLMLocation';
import { actions as organizationActions } from './organizationsSlice';
import { actions as applicationsActions } from './applicationsSlice';

const REDUCER_NAME = 'ownerEditor';

const updateOwner = createAsyncThunk(
  `${REDUCER_NAME}/updateApplication`,
  ({ ownerToSave, isApp }, { dispatch, rejectWithValue }) => {
    const url = isApp ? getApplicationsUrl() : getOrganizationsUrl();
    const payload = isApp
      ? pick(['id', 'name', 'publicId', 'organizationId', 'contactInternalName'], ownerToSave)
      : omit(['isNew'], ownerToSave);

    const isNew = !!ownerToSave.isNew;

    return axios[isNew ? 'post' : 'put'](url, payload)
      .then(({ data }) => {
        const updatedOwner = { isNew, [isApp ? 'application' : 'organization']: data };

        isApp
          ? dispatch(applicationsActions.updateApplication(updatedOwner))
          : dispatch(organizationActions.updateOrganization(updatedOwner));

        return updatedOwner;
      })
      .catch(rejectWithValue);
  }
);

export const actions = {
  updateOwner,
};
