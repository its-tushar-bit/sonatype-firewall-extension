/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { join, map, prop } from 'ramda';

import { fetchComponentDetails } from '../api/componentDetailsApi';
import PolicyViolationsSection from './PolicyViolationsSection';
import LicenseAnalysisSection from './LicenseAnalysisSection';
import SecurityIssuesSection from './SecurityIssuesSection';
import ErrorDisplay from './ErrorDisplay';
import { NxH2, NxLoadingSpinner, NxPageMain } from '@sonatype/react-shared-components';

/**
 * Main component for displaying component details in the viewdetails page
 */
export default function App() {
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadData = async () => {
    const queryParams = new URLSearchParams(window.location.search);

    setIsLoading(true);
    setError(null);

    try {
      const componentData = await fetchComponentDetails(urlSearchParamsToObject(queryParams));
      setData(componentData);
    } catch (err) {
      setError(err);
    } finally {
      setIsLoading(false);
    }
  };

  // Load data on initial render
  useEffect(() => {
    loadData();
  }, []);

  let contents;
  if (isLoading && !data) {
    contents = <NxLoadingSpinner>Loading component data…</NxLoadingSpinner>;
  } else if (error) {
    contents = <ErrorDisplay error={error} onRetry={loadData} />;
  } else if (!data) {
    contents = null;
  } else {
    contents = (
      <>
        <NxH2>
          Component Details for {join('', map(prop('value'), data.displayName.parts))} in the context of IQ Application{' '}
          {data.appName}
        </NxH2>

        <PolicyViolationsSection policyAlerts={data.policyAlerts} />

        <LicenseAnalysisSection
          matchState={data.matchState}
          identificationSource={data.identificationSource}
          licenseThreatLevel={data.licenseThreatLevel}
          licenseThreatGroupNames={data.licenseThreatGroupNames}
          declaredLicenses={data.declaredLicenses}
          observedLicenses={data.observedLicenses}
          overriddenLicenses={data.overriddenLicenses}
        />

        <SecurityIssuesSection
          matchState={data.matchState}
          identificationSource={data.identificationSource}
          securityVulnerabilities={data.securityVulnerabilities}
        />
      </>
    );
  }

  return (
    <div className="nx-page-content">
      <NxPageMain>{contents}</NxPageMain>
    </div>
  );
}

App.propTypes = {
  clmHeaders: PropTypes.object,
};

function urlSearchParamsToObject(params) {
  const obj = {};
  for (const [key, value] of params.entries()) {
    obj[key] = value;
  }

  return obj;
}
