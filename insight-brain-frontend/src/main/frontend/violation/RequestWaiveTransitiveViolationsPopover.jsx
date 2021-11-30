/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { IqPopover } from '../react/IqPopover';
import * as PropTypes from 'prop-types';
import { NxCodeSnippet, NxInfoAlert, NxTextLink } from '@sonatype/react-shared-components';
import { availableScopesPropType, componentTransitivePolicyViolationsPropType } from './transitiveViolationsPropTypes';
import { getWaiveTransitiveViolationsUrl } from '../util/CLMLocation';
import TransitiveViolationsSummary from './TransitiveViolationsSummary';

export default function RequestWaiveTransitiveViolationsPopover(props) {
  const {
    scanId,
    hash,
    availableScopes,
    componentTransitivePolicyViolations,
    toggleRequestWaiveTransitiveViolations,
  } = props;

  const curlExample =
    'curl -u admin:admin123 -X POST ' + getWaiveTransitiveViolationsUrl(availableScopes.data[0].publicId, scanId, hash);

  return (
    <IqPopover
      id="request-waive-transitive-violations-popover"
      size="large"
      onClose={toggleRequestWaiveTransitiveViolations}
    >
      <IqPopover.Header
        className="transitive-violations-popover-header"
        headerTitle="Request Waivers for Transitive Violations"
        buttonId="request-waive-transitive-violations-popover-toggle"
        onClose={toggleRequestWaiveTransitiveViolations}
      />
      <NxInfoAlert>
        To request transitive violation waivers, please share the application id, report id, parent component hash, and
        sample curl command (found below) with the approver.{' '}
        <NxTextLink external href="http://links.sonatype.com/products/nxiq/doc/request-waiver">
          Learn about automating waiver requests.
        </NxTextLink>
      </NxInfoAlert>
      <TransitiveViolationsSummary
        threatCounts={componentTransitivePolicyViolations.threatCounts}
        threatCountsTotal={componentTransitivePolicyViolations.threatCountsTotal}
        componentCount={componentTransitivePolicyViolations.componentCount}
      />
      <NxCodeSnippet
        id="request-waive-transitive-violations-application-public-id"
        label="Application ID"
        content={availableScopes.data[0].publicId}
        inputProps={{ rows: 1 }}
      />
      <NxCodeSnippet
        id="request-waive-transitive-violations-report-id"
        label="Report ID"
        content={scanId}
        inputProps={{ rows: 1 }}
      />
      <NxCodeSnippet
        id="request-waive-transitive-violations-component-hash"
        label="Parent Component Hash"
        content={hash}
        inputProps={{ rows: 1 }}
      />
      <NxCodeSnippet
        id="request-waive-transitive-violations-curl-example"
        label="cURL Example"
        content={curlExample}
        inputProps={{ rows: 4 }}
      />
    </IqPopover>
  );
}

RequestWaiveTransitiveViolationsPopover.propTypes = {
  scanId: PropTypes.string,
  hash: PropTypes.string,
  availableScopes: availableScopesPropType.isRequired,
  componentTransitivePolicyViolations: componentTransitivePolicyViolationsPropType.isRequired,
  toggleRequestWaiveTransitiveViolations: PropTypes.func.isRequired,
};
