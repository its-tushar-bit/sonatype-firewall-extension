/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useMemo } from 'react';
import * as PropTypes from 'prop-types';
import {
  allThreatLevelNumbers,
  NxFontAwesomeIcon,
  NxH2,
  NxIconDropdown,
  NxTable,
  NxTextLink,
  NxThreatIndicator,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faCheckCircle, faEllipsisV, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
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
  when,
} from 'ramda';

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { actions, SORT_BY_FIELDS, SORT_DIRECTION } from './componentDetailsSlice';

import './VulnerabilitiesTile.scss';
import { useDispatch, useSelector } from 'react-redux';
import { selectIssueForActions } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';
import { analysisStatusIndicator, transformJustification } from './componentDetailsUtils';
import cx from 'classnames';
import { pickFirstVexResponse } from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationDrawer';

export const isVexFieldAnnotated = (vexFieldAnnotation, vulnerabilityValidVexFieldStates) => {
  return (
    vulnerabilityValidVexFieldStates.map((entry) => entry.key).indexOf(pickFirstVexResponse(vexFieldAnnotation)) > -1
  );
};

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
    justificationsOptions,
    responsesOptions,
    sortConfiguration,
    toggleSortDirection,
    onDeleteOptionClick,
    onCopyOptionClick,
  } = props;

  const dispatch = useDispatch();
  const selectedIssueForActions = useSelector(selectIssueForActions);

  const isActionsOpen = (issue) => {
    return !isNilOrEmpty(selectedIssueForActions) && selectedIssueForActions === issue;
  };

  const onActionsToggleCollapse = (issue) => dispatch(actions.setSelectedIssueForActions(issue));

  const isEmpty = isNilOrEmpty(vulnerabilities);

  const sortedVulnerabilities = useMemo(
    () =>
      isEmpty
        ? []
        : sortVulnerabilities(augmentVulnerabilitiesAnalysisStatusUnannotated(vulnerabilities), sortConfiguration),
    [vulnerabilities, sortConfiguration, isEmpty]
  );

  const isRowAnnotated = (vulnRow, states) => isVexFieldAnnotated(vulnRow?.analysisStatus, states);

  const isJustificationSet = (vulnRow, states) => isVexFieldAnnotated(vulnRow?.justification, states);

  const isResponseSet = (vulnRow, states) => isVexFieldAnnotated(vulnRow?.response, states);

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

          <NxTable.Cell>
            {analysisStatusIndicator(vulnerability.analysisStatus)}{' '}
            {vulnerability.latestPreviousAnnotation && (
              <NxTooltip
                title={'Annotated in previous version (' + vulnerability.latestPreviousAnnotation.sbomVersion + ')'}
              >
                {analysisStatusIndicator(vulnerability.latestPreviousAnnotation.analysisStatus, true)}
              </NxTooltip>
            )}
          </NxTable.Cell>

          <NxTable.Cell>
            <NxTooltip
              title={
                vulnerability.latestPreviousAnnotation
                  ? 'Annotated in previous version (' + vulnerability.latestPreviousAnnotation.sbomVersion + ')'
                  : ''
              }
            >
              <span
                className={
                  vulnerability.latestPreviousAnnotation && 'sbom-manager-cdp-vulnerabilities-tile__justification-copy'
                }
              >
                {transformJustification(
                  vulnerability.latestPreviousAnnotation
                    ? vulnerability.latestPreviousAnnotation.justification
                    : transformJustification(vulnerability.justification)
                )}
              </span>
            </NxTooltip>
          </NxTable.Cell>

          <NxTable.Cell>
            <NxIconDropdown
              isOpen={isActionsOpen(vulnerability.issue)}
              onToggleCollapse={() => onActionsToggleCollapse(vulnerability.issue)}
              icon={faEllipsisV}
              title={'Options'}
              aria-label={vulnerability.issue + '-actions'}
            >
              <button
                onClick={() =>
                  openVexAnnotationModal({
                    ...vulnerability,
                    isRowAnnotated: isRowAnnotated(vulnerability, analysisStatusesOptions),
                    isJustificationSet: isJustificationSet(vulnerability, justificationsOptions),
                    isResponseSet: isResponseSet(vulnerability, responsesOptions),
                  })
                }
                className={cx(
                  'nx-dropdown-button',
                  isRowAnnotated(vulnerability, analysisStatusesOptions) ? 'edit-annotation' : 'add-annotation'
                )}
              >
                {isRowAnnotated(vulnerability, analysisStatusesOptions) ? 'Edit Annotation' : 'Add Annotation'}
              </button>
              {vulnerability.latestPreviousAnnotation && (
                <button onClick={() => onCopyOptionClick(vulnerability)} className="nx-dropdown-button copy-annotation">
                  Copy Annotation
                </button>
              )}
              {isRowAnnotated(vulnerability, analysisStatusesOptions) && (
                <button
                  onClick={() => onDeleteOptionClick(vulnerability)}
                  className="nx-dropdown-button delete-annotation"
                >
                  Delete Annotation
                </button>
              )}
            </NxIconDropdown>
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
              <NxTable.Cell {...sortableConfigCreator(SORT_BY_FIELDS.analysisStatus)}>Analysis State</NxTable.Cell>
              <NxTable.Cell>Justification</NxTable.Cell>
              <NxTable.Cell>Actions</NxTable.Cell>
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
  justificationsOptions: PropTypes.array.isRequired,
  responsesOptions: PropTypes.array.isRequired,
  sortConfiguration: PropTypes.shape({
    sortBy: PropTypes.string.isRequired,
    sortDirection: PropTypes.string,
  }).isRequired,
  toggleSortDirection: PropTypes.func.isRequired,
  onDeleteOptionClick: PropTypes.func,
  onCopyOptionClick: PropTypes.func,
};
