/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { path, pathOr, prop } from 'ramda';

import { selectOrgsAndPoliciesSlice } from '../orgsAndPoliciesSelectors';

export const selectImportSbomModalSlice = createSelector(
  selectOrgsAndPoliciesSlice,
  path(['ownerActions', 'importSbomModal'])
);

export const selectIsModalOpen = createSelector(selectImportSbomModalSlice, prop('isModalOpen'));

export const selectSelectedFile = createSelector(
  selectImportSbomModalSlice,
  pathOr(null, ['fileInputState', 'files', 0])
);

export const selectSelectedFilename = createSelector(selectSelectedFile, (file) => file?.name);

export const selectUploadValidationErrors = createSelector(selectSelectedFile, (file) =>
  file ? null : 'Please select a file to upload.'
);
