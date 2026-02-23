/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { UIView } from '@uirouter/react';
import { useSelector } from 'react-redux';
import DashboardHeaderContainer from './DashboardHeaderContainer';

export default function DashboardResultsContainer() {
  const filterLoading = useSelector((state) => state.dashboardFilter.loading);
  const loadFilterError = useSelector((state) => state.dashboardFilter.loadError);

  const isFilterLoaded = !filterLoading && !loadFilterError;

  return (
    <>
      <DashboardHeaderContainer />
      {isFilterLoaded && <UIView />}
    </>
  );
}
