/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxFilterInput,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { isNilOrEmpty } from '../../util/jsUtil';
import RawLicenseDisplay from './RawLicenseDisplay';

const rawComponentNameSettings = {
  key: 'derivedComponentName',
  sortingOrder: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
};

const rawLicenseKeySettings = {
  key: 'licenseSortKey',
  sortingOrder: ['licenseSortKey', 'derivedComponentName', 'securityCode', '-cvssScore'],
};

const rawSecurityCodeSettings = {
  key: 'securityCode',
  sortingOrder: ['securityCode', 'derivedComponentName', 'licenseSortKey', '-cvssScore'],
};

const rawCvssScoreSettings = {
  key: 'cvssScore',
  sortingOrder: ['-cvssScore', 'derivedComponentName', 'licenseSortKey', 'securityCode'],
};

const getCvssNumericFilters = (cvssScore) => {
  let cvssMinNumericFilter = '';
  let cvssMaxNumericFilter = '';

  if (!isNilOrEmpty(cvssScore)) {
    cvssMinNumericFilter = cvssScore[0] || '';
    cvssMaxNumericFilter = cvssScore[1] || '';
  }

  return { cvssMinNumericFilter, cvssMaxNumericFilter };
};

function createRow(entry, openModal) {
  const { derivedComponentName, license, securityCode, cvssScore, key } = entry;
  return (
    <NxTableRow key={key}>
      <NxTableCell>{derivedComponentName}</NxTableCell>
      <NxTableCell>{license && <RawLicenseDisplay license={license} />}</NxTableCell>
      <NxTableCell>
        <NxTextLink onClick={() => openModal(entry)}>{securityCode}</NxTextLink>
      </NxTableCell>
      <NxTableCell>{cvssScore}</NxTableCell>
    </NxTableRow>
  );
}

export default function ApplicationReportRawDataTable(props) {
  const {
    pendingLoadsSize,
    loadError,
    loadReportRawData,
    displayedEntries,
    derivedComponentNameSubstringFilter = '',
    licenseSortKeySubstringFilter = '',
    securityCodeSubstringFilter = '',
    cvssScore,
    rawSortConfiguration,
    // actions
    setRawDataStringFieldFilter,
    setRawDataNumericMaxFilter,
    setRawDataNumericMinFilter,
    setRawSortingParameters,
    openModal,
  } = props;

  const { cvssMinNumericFilter, cvssMaxNumericFilter } = getCvssNumericFilters(cvssScore);

  function getActiveDirection(settings) {
    const isSelectedCvssScore =
      rawSortConfiguration && rawSortConfiguration.key !== settings.key && settings.key === 'cvssScore';
    const isCurrentHasBeenSelected =
      rawSortConfiguration && rawSortConfiguration.key === settings.key && rawSortConfiguration.dir === 'asc';
    if (isCurrentHasBeenSelected || isSelectedCvssScore) {
      return 'desc';
    }

    return 'asc';
  }

  function requestSort(settings) {
    const { sortingOrder } = settings;
    const [first] = sortingOrder;
    const direction = getActiveDirection(settings);

    if (direction === 'desc' && !first.startsWith('-')) {
      sortingOrder[0] = '-'.concat(first);
    }
    if (direction === 'asc' && first.startsWith('-')) {
      sortingOrder[0] = first.substring(1);
    }

    setRawSortingParameters(settings.key, sortingOrder, direction);
  }

  const getDirection = (key) => (rawSortConfiguration.key === key ? rawSortConfiguration.dir : null);

  function handleCvssScore(value, isMax) {
    const regExp = /^(10|\d((\.)\d?)?)$/;
    if (!value || regExp.test(value)) {
      isMax ? setRawDataNumericMaxFilter('cvssScore', value) : setRawDataNumericMinFilter('cvssScore', value);
    }
  }

  const rows = (displayedEntries || []).map((raw) => createRow(raw, openModal));

  return (
    <NxTable id="raw-data-report-results">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell
            isSortable
            id="raw-data-component-column-header"
            sortDir={getDirection('derivedComponentName')}
            onClick={() => requestSort(rawComponentNameSettings)}
          >
            Component
          </NxTableCell>
          <NxTableCell
            isSortable
            id="raw-data-license-column-header"
            sortDir={getDirection('licenseSortKey')}
            onClick={() => requestSort(rawLicenseKeySettings)}
          >
            License
          </NxTableCell>
          <NxTableCell
            isSortable
            id="raw-data-security-column-header"
            sortDir={getDirection('securityCode')}
            onClick={() => requestSort(rawSecurityCodeSettings)}
          >
            Security Issue
          </NxTableCell>
          <NxTableCell
            isSortable
            id="raw-data-cvss-column-header"
            sortDir={getDirection('cvssScore')}
            onClick={() => requestSort(rawCvssScoreSettings)}
          >
            CVSS Score
          </NxTableCell>
        </NxTableRow>
        <NxTableRow isFilterHeader>
          <NxTableCell>
            <NxFilterInput
              id="raw-data-component-filter"
              placeholder="Component"
              onChange={(value) => setRawDataStringFieldFilter('derivedComponentName', value)}
              value={derivedComponentNameSubstringFilter}
            />
          </NxTableCell>
          <NxTableCell>
            <NxFilterInput
              id="raw-data-license-filter"
              placeholder="License"
              onChange={(value) => setRawDataStringFieldFilter('licenseSortKey', value)}
              value={licenseSortKeySubstringFilter}
            />
          </NxTableCell>
          <NxTableCell>
            <NxFilterInput
              id="raw-data-security-filter"
              placeholder="Security Issue"
              onChange={(value) => setRawDataStringFieldFilter('securityCode', value)}
              value={securityCodeSubstringFilter}
            />
          </NxTableCell>
          <NxTableCell className="iq-cell--report-raw-data-cvss">
            <NxFilterInput
              id="raw-data-cvss-min-filter"
              placeholder="Min"
              maxLength="3"
              onChange={(value) => handleCvssScore(value, false)}
              value={cvssMinNumericFilter}
            />
            <NxFilterInput
              id="raw-data-cvss-max-filter"
              placeholder="Max"
              maxLength="3"
              onChange={(value) => handleCvssScore(value, true)}
              value={cvssMaxNumericFilter}
            />
          </NxTableCell>
        </NxTableRow>
      </NxTableHead>
      <NxTableBody
        emptyMessage="No Data Available"
        isLoading={!!pendingLoadsSize}
        error={loadError}
        retryHandler={loadReportRawData}
      >
        {rows}
      </NxTableBody>
    </NxTable>
  );
}

export const tablePropTypes = {
  pendingLoadsSize: PropTypes.number,
  loadError: PropTypes.string,
  loadReportRawData: PropTypes.func.isRequired,
  displayedEntries: PropTypes.arrayOf(PropTypes.object),
  derivedComponentNameSubstringFilter: PropTypes.string,
  licenseSortKeySubstringFilter: PropTypes.string,
  securityCodeSubstringFilter: PropTypes.string,
  cvssScore: PropTypes.arrayOf(PropTypes.string),
  rawSortConfiguration: PropTypes.shape({
    key: PropTypes.string,
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string,
  }).isRequired,
  setRawDataStringFieldFilter: PropTypes.func,
  setRawDataNumericMaxFilter: PropTypes.func,
  setRawDataNumericMinFilter: PropTypes.func,
  setRawSortingParameters: PropTypes.func,
  openModal: PropTypes.func,
};

ApplicationReportRawDataTable.propTypes = {
  ...tablePropTypes,
};
