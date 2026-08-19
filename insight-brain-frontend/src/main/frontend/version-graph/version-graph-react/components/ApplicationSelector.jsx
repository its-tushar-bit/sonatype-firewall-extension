/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxFormGroup,
  NxFormSelect,
  NxInfoAlert,
  NxLoadError,
  NxLoadingSpinner,
} from '@sonatype/react-shared-components';
import {
  fetchApplications,
  setApplication,
  selectApplications,
  selectLoading,
  selectError,
  selectSelectedApplication,
} from '../slices/applicationsSlice';

import './ApplicationSelector.scss';

/**
 * Allows selecting an application to view component details for
 */
export default function ApplicationSelector() {
  const dispatch = useDispatch();
  const applications = useSelector(selectApplications);
  const selectedAppId = useSelector(selectSelectedApplication)?.publicId;
  const loading = useSelector(selectLoading);
  const error = useSelector(selectError);

  useEffect(() => {
    load();
  }, [dispatch]);

  const handleChange = (value) => {
    if (value) {
      dispatch(setApplication(value));
    }
  };

  const load = () => {
    dispatch(fetchApplications());
  };

  return (
    <div className="iq-version-graph-app-selector">
      {loading ? (
        <NxLoadingSpinner>Loading applications…</NxLoadingSpinner>
      ) : error ? (
        <NxLoadError error={error} onRetry={load} />
      ) : applications.length > 0 ? (
        <NxFormGroup label="Application">
          <NxFormSelect
            value={selectedAppId || ''}
            onChange={handleChange}
            className="iq-version-graph-app-selector__form-select nx-form-select--long"
          >
            <option value="" disabled>
              Select an application
            </option>
            {applications.map((app) => (
              <option key={app.publicId} value={app.publicId}>
                {app.name} ({app.publicId})
              </option>
            ))}
          </NxFormSelect>
        </NxFormGroup>
      ) : (
        <NxInfoAlert>No applications found. Please ensure you have access to applications in IQ Server.</NxInfoAlert>
      )}
    </div>
  );
}
