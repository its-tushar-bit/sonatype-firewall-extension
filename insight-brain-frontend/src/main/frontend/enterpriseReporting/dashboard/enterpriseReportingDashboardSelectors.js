/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const enterpriseReportingDashboardSlice = prop('enterpriseReportingDashboard');
export const selectLoading = createSelector(enterpriseReportingDashboardSlice, prop('loading'));
export const selectError = createSelector(enterpriseReportingDashboardSlice, prop('loadError'));
export const selectSelectedDashboard = createSelector(enterpriseReportingDashboardSlice, prop('selectedDashboard'));
export const selectBaseUrl = (state) => enterpriseReportingDashboardSlice(state)?.baseUrl;
