/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as R from 'ramda';
import { createSelector } from '@reduxjs/toolkit';
import { hasValidationErrors } from '@sonatype/react-shared-components';

import { ROI_VALIDATABLE_FIELDS } from './editRoiConfigurationPageSlice';

export const selectEditRoiConfigurationPageSlice = R.prop('editRoiConfigurationPage');

export const selectHasValidationErrors = createSelector(selectEditRoiConfigurationPageSlice, (state) =>
  R.any(
    (field) => hasValidationErrors(R.path(['configuration', field, 'input', 'validationErrors'], state)),
    ROI_VALIDATABLE_FIELDS
  )
);
