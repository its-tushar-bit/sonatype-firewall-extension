/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { omit, pick } from 'ramda';
import { createAsyncThunk } from '@reduxjs/toolkit';
import { getApplicationsUrl, getNLevelOrgUrl, getOrganizationsUrl, getRepositoryManagerUrl } from '../util/CLMLocation';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';

const REDUCER_NAME = 'ownerActions';

const updateOwner = createAsyncThunk(
  `${REDUCER_NAME}/updateOwner`,
  ({ ownerToSave, ownerType }, { dispatch, rejectWithValue }) => {
    const isNew = !!ownerToSave.isNew;

    function getUpdateUrl() {
      if (ownerType === 'application') {
        return getApplicationsUrl();
      }
      if (ownerType === 'organization') {
        return isNew ? getNLevelOrgUrl() : getOrganizationsUrl();
      }
      if (ownerType === 'repository_manager') {
        return getRepositoryManagerUrl(ownerToSave.id, ownerToSave.name);
      }
    }

    function getPayload() {
      if (ownerType === 'application') {
        return pick(['id', 'name', 'publicId', 'organizationId', 'contactInternalName'], ownerToSave);
      }
      if (ownerType === 'organization') {
        return omit(['isNew'], ownerToSave);
      }
      if (ownerType === 'repository_manager') {
        return null;
      }
    }

    function getMethod() {
      if (ownerType === 'application' || ownerType === 'organization') {
        return isNew ? 'post' : 'put';
      }
      if (ownerType === 'repository_manager') {
        return 'put';
      }
    }

    return axios[getMethod()](getUpdateUrl(), getPayload())
      .then(async ({ data }) => {
        const updatedOwner = { isNew, [ownerType]: data };
        await dispatch(rootActions.loadSelectedOwner(true));

        return updatedOwner;
      })
      .catch(rejectWithValue);
  }
);

export const actions = {
  updateOwner,
};
