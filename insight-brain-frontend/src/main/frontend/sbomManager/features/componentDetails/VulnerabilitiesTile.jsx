/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useMemo } from 'react';
import * as PropTypes from 'prop-types';
import {
  allThreatLevelNumbers,
  NxButton,
  NxErrorStatusIndicator,
  NxFontAwesomeIcon,
  NxH2,
  NxIntermediateStatusIndicator,
  NxNegativeStatusIndicator,
  NxPositiveStatusIndicator,
  NxTable,
  NxTextLink,
  NxThreatIndicator,
  NxTile,
} from '@sonatype/react-shared-components';
import { faCheckCircle, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import {
  always,
  ascend,
  assoc,
  cond,
  descend,
  equals,
  find,
  isNil,
  map,
  prop,
  propSatisfies,
  sortWith,
  T,
  toUpper,
  when,
} from 'ramda';

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { SORT_BY_FIELDS, SORT_DIRECTION } from './componentDetailsSlice';

import './VulnerabilitiesTile.scss';

const transformJustification = (justification) =>
  justification ? justification.replace(/_/g, ' ').replace(/^\w/, toUpper) : '';

export const isVulnerabilityAnnotated = (vulnerabilityRow, vulnerabilityValidAnalysisStates) =>
  vulnerabilityValidAnalysisStates.map((entry) => entry.key).indexOf(vulnerabilityRow?.analysisStatus) > -1;

const sortVulnerabilities = (vulnerabilites, { sortBy, sortDirection }) => {
  const sortConfig = cond([
    [equals(SORT_DIRECTION.ASC), always([ascend(prop(sortBy))])],
    [equals(SORT_DIRECTION.DESC), always([descend(prop(sortBy))])],
    [T, always([always(0)])],
  ])(sortDirection);
  return sortWith(sortConfig)(vulnerabilites);
};

const augmentVulnerabilitiesAnalysisStatusUnannotated = map(
  when(propSatisfies(isNil, 'analysisStatus'), assoc('analysisStatus', 'unannotated'))
);

export default function VulnerabilitiesTile(props) {
  const {
    tableUniqueIdentifier = '',
    isDisclosedVulnerabilities = true,
    vulnerabilities,
    openVulnerabilityDetailsModal,
    openVexAnnotationModal,
    analysisStatusesOptions,
    sortConfiguration,
    toggleSortDirection,
  } = props;

  const isEmpty = isNilOrEmpty(vulnerabilities);

  const sortedVulnerabilities = useMemo(
    () =>
      isEmpty
        ? []
        : sortVulnerabilities(augmentVulnerabilitiesAnalysisStatusUnannotated(vulnerabilities), sortConfiguration),
    [vulnerabilities, sortConfiguration, isEmpty]
  );

  const analysisStatusIndicator = (status) => {
    switch (status) {
      case 'resolved':
        return <NxPositiveStatusIndicator>Resolved</NxPositiveStatusIndicator>;
      case 'resolved_with_pedigree':
        return <NxPositiveStatusIndicator>Resolved with Pedigree</NxPositiveStatusIndicator>;
      case 'exploitable':
        return <NxErrorStatusIndicator>Exploitable</NxErrorStatusIndicator>;
      case 'in_triage':
        return (
          <NxNegativeStatusIndicator className="sbom-manager-cdp-vulnerabilities-tile__intriage-status">
            In Triage
          </NxNegativeStatusIndicator>
        );
      case 'false_positive':
        return <NxNegativeStatusIndicator>False Positive</NxNegativeStatusIndicator>;
      case 'not_affected':
        return <NxIntermediateStatusIndicator>Not Affected</NxIntermediateStatusIndicator>;
      default:
        return <span>Unannotated</span>;
    }
  };

  const isRowAnnotated = (vulnRow, states) => isVulnerabilityAnnotated(vulnRow, states);

  const tableBodyRows = !isEmpty
    ? sortedVulnerabilities.map((vulnerability) => (
        <NxTable.Row key={vulnerability.issue}>
          <NxTable.Cell>
            <NxThreatIndicator
              policyThreatLevel={find(equals(Math.floor(vulnerability.cvssScore)))(allThreatLevelNumbers)}
              presentational
            />
            <span>{vulnerability.cvssScore}</span>
          </NxTable.Cell>

          <NxTable.Cell>
            <NxTextLink id="sbom-component-details-link" onClick={() => openVulnerabilityDetailsModal(vulnerability)}>
              {vulnerability.issue}
            </NxTextLink>
          </NxTable.Cell>

          {isDisclosedVulnerabilities && (
            <NxTable.Cell>
              <div>
                <NxFontAwesomeIcon
                  className={vulnerability.verified ? 'sbom-verified-icon' : 'sbom-unverified-icon'}
                  icon={vulnerability.verified ? faCheckCircle : faExclamationTriangle}
                />
                <span>{vulnerability.verified ? 'Sonatype Verified' : 'Unverified'}</span>
              </div>
            </NxTable.Cell>
          )}

          <NxTable.Cell>{analysisStatusIndicator(vulnerability.analysisStatus)}</NxTable.Cell>

          <NxTable.Cell>
            <span>{transformJustification(vulnerability.justification)}</span>
          </NxTable.Cell>

          <NxTable.Cell>
            <NxButton
              onClick={() =>
                openVexAnnotationModal({
                  ...vulnerability,
                  isRowAnnotated: isRowAnnotated(vulnerability, analysisStatusesOptions),
                })
              }
            >
              {isRowAnnotated(vulnerability, analysisStatusesOptions) ? 'Edit' : 'Add'}
            </NxButton>
          </NxTable.Cell>
        </NxTable.Row>
      ))
    : null;

  const sortableConfigCreator = (sortBy) => ({
    isSortable: !isEmpty,
    sortDir: sortConfiguration.sortBy === sortBy ? sortConfiguration.sortDirection : SORT_DIRECTION.UNSORTED,
    onClick: () => {
      if (!isEmpty) {
        toggleSortDirection(sortBy);
      }
    },
  });

  const identifierSeparator = isNilOrEmpty(tableUniqueIdentifier) ? '' : '__';
  const tableTileId = `sbom-manager-cdp-vulnerabilities-tile${identifierSeparator}${tableUniqueIdentifier}`;

  return (
    <NxTile id={tableTileId} className="sbom-manager-cdp-vulnerabilities-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>
            {isDisclosedVulnerabilities
              ? 'Disclosed Vulnerabilities'
              : 'Additional Sonatype Identified Vulnerabilities'}
          </NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content className="sbom-manager-cdp-vulnerabilities-tile__content">
        <span>
          {isDisclosedVulnerabilities
            ? 'Existing vulnerabilities disclosed by the originator of this SBOM.'
            : 'Additional vulnerabilities in this SBOM, detected by Sonatype vulnerability detection system.'}
        </span>

        <NxTable>
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell {...sortableConfigCreator(SORT_BY_FIELDS.cvssScore)}>CVSS Score</NxTable.Cell>
              <NxTable.Cell>Issue</NxTable.Cell>
              {isDisclosedVulnerabilities && <NxTable.Cell>Verified Status</NxTable.Cell>}
              <NxTable.Cell {...sortableConfigCreator(SORT_BY_FIELDS.analysisStatus)}>Analysis Status</NxTable.Cell>
              <NxTable.Cell>Justification</NxTable.Cell>
              <NxTable.Cell>Action</NxTable.Cell>
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body emptyMessage="No vulnerabilities found">{tableBodyRows}</NxTable.Body>
        </NxTable>
      </NxTile.Content>
    </NxTile>
  );
}

VulnerabilitiesTile.propTypes = {
  tableUniqueIdentifier: PropTypes.string,
  isDisclosedVulnerabilities: PropTypes.bool,
  vulnerabilities: PropTypes.array,
  openVulnerabilityDetailsModal: PropTypes.func,
  openVexAnnotationModal: PropTypes.func,
  analysisStatusesOptions: PropTypes.array.isRequired,
  sortConfiguration: PropTypes.shape({
    sortBy: PropTypes.string.isRequired,
    sortDirection: PropTypes.string,
  }).isRequired,
  toggleSortDirection: PropTypes.func.isRequired,
};
