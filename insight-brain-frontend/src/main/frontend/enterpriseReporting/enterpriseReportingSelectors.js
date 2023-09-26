/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const enterpriseReportingSlice = prop('enterpriseReporting');
export const selectEmbedUrlData = createSelector(enterpriseReportingSlice, prop('embedUrlData'));
export const selectLoading = createSelector(enterpriseReportingSlice, prop('loading'));
export const selectError = createSelector(enterpriseReportingSlice, prop('loadError'));
