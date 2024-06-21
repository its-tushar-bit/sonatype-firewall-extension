/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
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
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { faCheckCircle, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import './VulnerabilitiesTile.scss';
import * as PropTypes from 'prop-types';

export default function VulnerabilitiesTile(props) {
  const {
    tableUniqueIdentifier = '',
    isDisclosedVulnerabilities = true,
    vulnerabilities,
    openVulnerabilityDetailsModal,
    openVexAnnotationModal,
    analysisStatusesOptions,
  } = props;

  const determineTableTitle = () => {
    let title;
    if (isDisclosedVulnerabilities) {
      title = 'Disclosed Vulnerabilities';
    } else {
      title = 'Additional Sonatype Identified Vulnerabilities';
    }

    return title;
  };

  const determineTableSubtitle = () => {
    let subtitle;
    if (isDisclosedVulnerabilities) {
      subtitle = 'Existing vulnerabilities disclosed by the originator of this SBOM.';
    } else {
      subtitle = 'Additional vulnerabilities in this SBOM, detected by Sonatype vulnerability detection system.';
    }

    return <span>{subtitle}</span>;
  };

  const determineVerifiedUnverifiedStatus = (status) => {
    if (status) {
      return (
        <div>
          <NxFontAwesomeIcon className={'sbom-verified-icon'} icon={faCheckCircle} />
          <span>Sonatype Verified</span>
        </div>
      );
    } else {
      return (
        <div>
          <NxFontAwesomeIcon className={'sbom-unverified-icon'} icon={faExclamationTriangle} />
          <span>Unverified</span>
        </div>
      );
    }
  };

  const determineAnalysisStatus = (status) => {
    let statusIndicator;
    switch (status) {
      case 'resolved':
        statusIndicator = <NxPositiveStatusIndicator>Resolved</NxPositiveStatusIndicator>;
        break;
      case 'resolved_with_pedigree':
        statusIndicator = <NxPositiveStatusIndicator>Resolved with Pedigree</NxPositiveStatusIndicator>;
        break;
      case 'exploitable':
        statusIndicator = <NxErrorStatusIndicator>Exploitable</NxErrorStatusIndicator>;
        break;
      case 'in_triage':
        statusIndicator = (
          <NxNegativeStatusIndicator className="sbom-manager-cdp-vulnerabilities-tile__intriage-status">
            In Triage
          </NxNegativeStatusIndicator>
        );
        break;
      case 'false_positive':
        statusIndicator = <NxNegativeStatusIndicator>False Positive</NxNegativeStatusIndicator>;
        break;
      case 'not_affected':
        statusIndicator = <NxIntermediateStatusIndicator>Not Affected</NxIntermediateStatusIndicator>;
        break;
      default:
        statusIndicator = <span>Unannotated</span>;
    }

    return statusIndicator;
  };

  const isRowAnnotated = (vulnRow, states) => isVulnerabilityAnnotated(vulnRow, states);

  const translateJustification = (justification) => {
    return justification ? justification.replace(/_/g, ' ').replace(/^\w/, (c) => c.toUpperCase()) : '';
  };

  const generateTableBodyRows = () => {
    if (!isNilOrEmpty(vulnerabilities)) {
      return (
        <>
          {vulnerabilities.map((vulnerability) => (
            <NxTable.Row key={vulnerability.issue}>
              <NxTable.Cell>
                <NxThreatIndicator
                  policyThreatLevel={allThreatLevelNumbers.find((n) => n === Math.floor(vulnerability.cvssScore))}
                  presentational
                />
                <span>{vulnerability.cvssScore}</span>
              </NxTable.Cell>
              <NxTable.Cell>
                <NxTextLink
                  id="sbom-component-details-link"
                  onClick={() => openVulnerabilityDetailsModal(vulnerability)}
                >
                  {vulnerability.issue}
                </NxTextLink>
              </NxTable.Cell>
              {isDisclosedVulnerabilities && (
                <NxTable.Cell>{determineVerifiedUnverifiedStatus(vulnerability.verified)}</NxTable.Cell>
              )}
              <NxTable.Cell>{determineAnalysisStatus(vulnerability.analysisStatus)}</NxTable.Cell>
              <NxTable.Cell>
                <span>{translateJustification(vulnerability.justification)}</span>
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
          ))}
        </>
      );
    }
  };

  const identifierSeparator = isNilOrEmpty(tableUniqueIdentifier) ? '' : '__';
  const tableTileId = `sbom-manager-cdp-vulnerabilities-tile${identifierSeparator}${tableUniqueIdentifier}`;

  return (
    <NxTile id={tableTileId} className="sbom-manager-cdp-vulnerabilities-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>{determineTableTitle()}</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content className="sbom-manager-cdp-vulnerabilities-tile__content">
        {determineTableSubtitle()}

        <NxTable>
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell>CVSS SCORE</NxTable.Cell>
              <NxTable.Cell>ISSUE</NxTable.Cell>
              {isDisclosedVulnerabilities && <NxTable.Cell>VERIFIED STATUS</NxTable.Cell>}
              <NxTable.Cell>ANALYSIS STATUS</NxTable.Cell>
              <NxTable.Cell>JUSTIFICATION</NxTable.Cell>
              <NxTable.Cell>ACTION</NxTable.Cell>
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body emptyMessage="No vulnerabilities found">{generateTableBodyRows()}</NxTable.Body>
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
};

export const isVulnerabilityAnnotated = (vulnerabilityRow, vulnerabilityValidAnalysisStates) =>
  vulnerabilityValidAnalysisStates.map((entry) => entry.key).indexOf(vulnerabilityRow?.analysisStatus) > -1;
