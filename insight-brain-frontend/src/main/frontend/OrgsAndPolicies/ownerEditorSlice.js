/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { omit, pick } from 'ramda';
import { createAsyncThunk } from '@reduxjs/toolkit';
import { getApplicationsUrl, getNLevelOrgUrl, getOrganizationsUrl } from '../util/CLMLocation';
import { actions as organizationActions } from './organizationsSlice';
import { actions as applicationsActions } from './applicationsSlice';

const REDUCER_NAME = 'ownerActions';

const updateOwner = createAsyncThunk(
  `${REDUCER_NAME}/updateApplication`,
  ({ ownerToSave, isApp }, { dispatch, rejectWithValue }) => {
    const isNew = !!ownerToSave.isNew;

    const url = isApp ? getApplicationsUrl() : isNew ? getNLevelOrgUrl() : getOrganizationsUrl();
    const payload = isApp
      ? pick(['id', 'name', 'publicId', 'organizationId', 'contactInternalName'], ownerToSave)
      : omit(['isNew'], ownerToSave);

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
