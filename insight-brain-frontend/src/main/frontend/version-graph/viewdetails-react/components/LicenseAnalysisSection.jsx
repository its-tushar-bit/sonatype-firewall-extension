/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxTable, NxThreatIndicator } from '@sonatype/react-shared-components';

import './LicenseAnalysisSection.scss';

/**
 * Component for displaying license analysis information
 */
export default function LicenseAnalysisSection({
  matchState,
  identificationSource,
  licenseThreatLevel,
  licenseThreatGroupNames,
  declaredLicenses,
  observedLicenses,
  overriddenLicenses,
}) {
  const showOverridden = overriddenLicenses && overriddenLicenses.length > 0;
  let body;

  // if the component is unknown or claimed, show a message
  if (matchState === 'unknown') {
    body = <NxTable.Body emptyMessage="The component is unknown; license data is not available." />;
  } else if (identificationSource === 'Manual') {
    body = <NxTable.Body emptyMessage="The component is claimed; license data is not available." />;
  } else {
    body = (
      <NxTable.Body emptyMessage="None">
        <NxTable.Row>
          <NxTable.Cell>
            <div className="iq-viewdetails-ltg-cell">
              <NxThreatIndicator
                className="iq-viewdetails-ltg-cell__threat-indicator"
                policyThreatLevel={licenseThreatLevel}
              />
              <ul className="iq-viewdetails-ltg-cell__list">
                {(licenseThreatGroupNames ?? []).map((name) => (
                  <li key={name}>{name}</li>
                ))}
              </ul>
            </div>
          </NxTable.Cell>
          {showOverridden && (
            <NxTable.Cell>
              <ul className="iq-viewdetails-license-list">
                {(overriddenLicenses ?? []).map((name) => (
                  <li key={name}>{name}</li>
                ))}
              </ul>
            </NxTable.Cell>
          )}
          <NxTable.Cell>
            <ul className="iq-viewdetails-license-list">
              {(declaredLicenses ?? []).map((name) => (
                <li key={name}>{name}</li>
              ))}
            </ul>
          </NxTable.Cell>
          <NxTable.Cell>
            <ul className="iq-viewdetails-license-list">
              {(observedLicenses ?? []).map((name) => (
                <li key={name}>{name}</li>
              ))}
            </ul>
          </NxTable.Cell>
        </NxTable.Row>
      </NxTable.Body>
    );
  }

  return (
    <NxTable caption="License Analysis">
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>Threat Level</NxTable.Cell>
          {showOverridden && <NxTable.Cell>Overridden License</NxTable.Cell>}
          <NxTable.Cell>Declared License(s)</NxTable.Cell>
          <NxTable.Cell>Observed License(s)</NxTable.Cell>
        </NxTable.Row>
      </NxTable.Head>
      {body}
    </NxTable>
  );
}

LicenseAnalysisSection.propTypes = {
  matchState: PropTypes.string,
  identificationSource: PropTypes.string,
  licenseThreatLevel: PropTypes.number,
  licenseThreatGroupNames: PropTypes.arrayOf(PropTypes.string),
  declaredLicenses: PropTypes.arrayOf(PropTypes.string),
  observedLicenses: PropTypes.arrayOf(PropTypes.string),
  overriddenLicenses: PropTypes.arrayOf(PropTypes.string),
};
