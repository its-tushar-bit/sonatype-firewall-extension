/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop, map } from 'ramda';
import moment from 'moment';

import { selectSelectedComponent } from '../../applicationReport/applicationReportSelectors';

export const DATE_FORMAT = 'YYYY-MM-DD';

export const selectComponentDetailsClaimSlice = prop('componentDetailsClaim');

export const selectSelectedComponentHash = createSelector(selectSelectedComponent, prop('hash'));

export const selectClaimServerData = createSelector(selectComponentDetailsClaimSlice, prop('serverData'));
export const selectClaimId = createSelector(selectClaimServerData, prop('id'));
export const selectClaimInputFieldsData = createSelector(selectComponentDetailsClaimSlice, prop('inputFields'));
export const selectClaimInputFieldsValues = createSelector(
  selectClaimInputFieldsData,
  map((field) => field.trimmedValue)
);

export const selectClaimRequestData = createSelector(
  selectClaimInputFieldsValues,
  selectSelectedComponentHash,
  ({ comment, createTime, artifactId, classifier, extension, groupId, version }, hash) => ({
    hash,
    comment,
    createTime: createTime ? moment(createTime, DATE_FORMAT).valueOf() : null,
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId,
        classifier,
        extension,
        groupId,
        version,
      },
    },
  })
);
