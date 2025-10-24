/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getPermissionContextTestUrl, getGlobalPermissionTestUrl } from '../util/CLMContextLocation';
import { getProductFeaturesUrl } from './CLMLocation';

export async function isContextAuthorized(permissions, ownerType, ownerId) {
  const response = await axios.put(getPermissionContextTestUrl(ownerType, ownerId), permissions);
  return permissions.length === response.data.length;
}

export async function isAuthorized(permissions) {
  const response = await axios.put(getGlobalPermissionTestUrl(), permissions);
  return permissions.length === response.data.length;
}

export async function getValidPermissions(permissions) {
  const response = await axios.put(getGlobalPermissionTestUrl(), permissions);
  return response.data;
}

export async function isAutomationFeatureEnabled() {
  try {
    const response = await axios.get(getProductFeaturesUrl());
    return response.data.includes('automation');
  } catch (error) {
    return false;
  }
}

export async function isFeatureEnabled(featureName) {
  try {
    const response = await axios.get(getProductFeaturesUrl());
    return response.data.includes(featureName);
  } catch (error) {
    return false;
  }
}
