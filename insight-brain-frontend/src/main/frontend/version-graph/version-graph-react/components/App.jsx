/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { NxLoadWrapper, NxPageMain, NxInfoAlert } from '@sonatype/react-shared-components';

import {
  selectLoading as selectComponentsLoading,
  selectError as selectComponentsError,
  selectCurrentVersion,
} from '../slices/componentsSlice';
import { selectSelectedApplication } from '../slices/applicationsSlice';
import { selectError as selectGlobalError } from '../slices/globalSlice';
import { selectComponentDetails } from '../slices/componentDetailsSlice';
import VersionGraph from './VersionGraph';
import ComponentDetails from './ComponentDetails';
import ApplicationSelector from './ApplicationSelector';

import './App.scss';

/**
 * Main App component for the Version Graph panel
 */
export default function App() {
  const loading = useSelector(selectComponentsLoading);
  const globalError = useSelector(selectGlobalError);
  const componentsError = useSelector(selectComponentsError);
  const currentVersion = useSelector(selectCurrentVersion);
  const selectedApplication = useSelector(selectSelectedApplication);

  const refreshPage = () => {
    window.location.reload();
  };

  return (
    <NxPageMain>
      <ApplicationSelector />

      <NxLoadWrapper loading={loading} error={globalError ?? componentsError} retryHandler={refreshPage}>
        {!selectedApplication && <NxInfoAlert>Select an application</NxInfoAlert>}
        {selectedApplication && !currentVersion && <NxInfoAlert>Select a component to view details.</NxInfoAlert>}
        {currentVersion && selectedApplication && (
          <div className="iq-version-graph__graph-and-details">
            <VersionGraph />
            {<ComponentDetails />}
          </div>
        )}
      </NxLoadWrapper>
    </NxPageMain>
  );
}
