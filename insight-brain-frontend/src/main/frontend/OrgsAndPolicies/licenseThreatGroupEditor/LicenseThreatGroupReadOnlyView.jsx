/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useMemo } from 'react';
import PropTypes from 'prop-types';
import {
  NxReadOnly,
  NxH2,
  NxThreatIndicator,
  NxTable,
  NxFilterInput,
  categoryByPolicyThreatLevel,
} from '@sonatype/react-shared-components';
import { capitalize } from 'MainRoot/util/jsUtil';
import './_LicenseThreatGroupReadOnlyView.scss';

export default function LicenseThreatGroupReadOnlyView({ licenseThreatGroup, allLicenses }) {
  const [searchFilter, setSearchFilter] = useState('');

  if (!licenseThreatGroup) {
    return null;
  }

  // Calculate selected licenses once
  const licenseIds = licenseThreatGroup.licenseIds || [];
  const selectedLicenses = useMemo(() => {
    return licenseIds
      .map((id) => allLicenses?.find((license) => license.id === id || license.licenseId === id))
      .filter(Boolean);
  }, [licenseIds, allLicenses]);

  // Filter licenses based on search
  const filteredLicenses = useMemo(() => {
    if (!searchFilter.trim()) {
      return selectedLicenses;
    }

    const lowerSearchFilter = searchFilter.toLowerCase();
    return selectedLicenses.filter((license) => {
      const licenseName = (license.displayName || license.licenseName || '').toLowerCase();
      return licenseName.includes(lowerSearchFilter);
    });
  }, [selectedLicenses, searchFilter]);

  const renderDetailsSection = () => {
    // Handle both string and RSC field object formats
    const groupName =
      typeof licenseThreatGroup.name === 'string'
        ? licenseThreatGroup.name || '--'
        : licenseThreatGroup.name?.trimmedValue || licenseThreatGroup.name?.value || '--';

    const threatLevel = licenseThreatGroup.threatLevel;
    const hasThreatLevel =
      threatLevel !== undefined && threatLevel !== null && categoryByPolicyThreatLevel[threatLevel] !== undefined;

    return (
      <div className="iq-license-threat-group-readonly-view__section">
        <NxH2>Group Details</NxH2>
        <div className="iq-license-threat-group-readonly-view__summary-row">
          <NxReadOnly>
            <NxReadOnly.Label>Group Name</NxReadOnly.Label>
            <NxReadOnly.Data data-testid="ltg-name">{groupName}</NxReadOnly.Data>
          </NxReadOnly>
          {hasThreatLevel && (
            <NxReadOnly>
              <NxReadOnly.Label>Threat Level</NxReadOnly.Label>
              <NxReadOnly.Data
                className="iq-license-threat-group-readonly-view__threat-level"
                data-testid="ltg-threat-level"
              >
                <NxThreatIndicator policyThreatLevel={threatLevel} />
                <span>
                  {threatLevel} - {capitalize(categoryByPolicyThreatLevel[threatLevel])}
                </span>
              </NxReadOnly.Data>
            </NxReadOnly>
          )}
        </div>
      </div>
    );
  };

  const renderLicensesSection = () => {
    const emptyMessage = searchFilter.trim() ? `No licenses match "${searchFilter}"` : 'No licenses assigned';

    return (
      <div className="iq-license-threat-group-readonly-view__section">
        <NxH2>Included Licenses</NxH2>
        {selectedLicenses.length > 0 && (
          <NxFilterInput
            className="iq-license-threat-group-readonly-view__search"
            placeholder="Filter licenses"
            value={searchFilter}
            onChange={setSearchFilter}
          />
        )}
        <NxTable className="iq-license-threat-group-readonly-view__licenses-table" data-testid="ltg-licenses-table">
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell>License Name</NxTable.Cell>
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body emptyMessage={emptyMessage}>
            {filteredLicenses.map((license) => (
              <NxTable.Row key={license.id || license.licenseId}>
                <NxTable.Cell>{license.displayName || license.licenseName}</NxTable.Cell>
              </NxTable.Row>
            ))}
          </NxTable.Body>
        </NxTable>
      </div>
    );
  };

  return (
    <div className="iq-license-threat-group-readonly-view" data-testid="license-threat-group-readonly-view">
      {renderDetailsSection()}
      {renderLicensesSection()}
    </div>
  );
}

LicenseThreatGroupReadOnlyView.propTypes = {
  licenseThreatGroup: PropTypes.shape({
    name: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.shape({
        value: PropTypes.string,
        trimmedValue: PropTypes.string,
      }),
    ]),
    threatLevel: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    licenseIds: PropTypes.arrayOf(PropTypes.string),
  }),
  allLicenses: PropTypes.arrayOf(
    PropTypes.shape({
      licenseId: PropTypes.string,
      licenseName: PropTypes.string,
    })
  ),
};
