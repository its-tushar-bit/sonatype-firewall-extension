/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useRef } from 'react';
import PropTypes from 'prop-types';
import { NxInfoAlert, NxCodeSnippet, NxTextLink } from '@sonatype/react-shared-components';

import IqPopover from '../../react/IqPopover/IqPopover';
import { getRequestWaiverUrl } from '../../util/CLMLocation';
import { extractViolationDetails, violationDetailsPropTypes } from '../../util/violationDetailsUtil';
import { uriTemplate } from '../../util/urlUtil';

const RequestWaiversPopover = ({ onClose, violationDetails }) => {
  const { policyViolationId, policyName, constraintName, componentName, reasons = [] } = extractViolationDetails(
    violationDetails
  );

  const curlExample = `curl -X POST -u user:pass -H "Content-Type: text/plain; charset=UTF-8" ${getRequestWaiverUrl(
    policyViolationId
  )} --data-binary 'waiver comment (optional)'`;

  const reasonsElements = reasons.map((reason) => (
    <dd key={reason} className="nx-read-only__data">
      {reason}
    </dd>
  ));

  const policyViolationUrl = uriTemplate`/assets/#/violation/${policyViolationId}`;

  const urlLinkEl = useRef();

  return (
    <IqPopover size="large" onClose={onClose} id="request-waivers">
      <IqPopover.Header
        className="request-waiver-header"
        headerTitle="Request Waiver"
        buttonId="request-waivers-close-button"
        onClose={onClose}
      />
      <NxInfoAlert>
        To request a waiver, please share the Policy Violation ID and sample curl command (found below) with the
        approver.{' '}
        <NxTextLink href="http://links.sonatype.com/products/nxiq/doc/request-waiver" external>
          Learn about automating waiver requests.
        </NxTextLink>
      </NxInfoAlert>
      <dl className="nx-read-only">
        <dt className="nx-read-only__label">Component</dt>
        <dd className="nx-read-only__data">{componentName}</dd>
        <dt className="nx-read-only__label">Policy</dt>
        <dd className="nx-read-only__data">{policyName}</dd>
        <dt className="nx-read-only__label">Constraint Name</dt>
        <dd className="nx-read-only__data">{constraintName}</dd>
        <dt className="nx-read-only__label">Conditions</dt>
        {reasonsElements}
      </dl>
      <NxCodeSnippet
        label="Policy Violation ID"
        content={policyViolationId}
        className="visual-testing-ignore iq-request-waivers-popover__violation-id"
        id="request-waivers-policy-violation-id"
      />
      <NxCodeSnippet
        label="Policy Violation Details Page"
        content={policyViolationUrl}
        className="visual-testing-ignore iq-request-waivers-popover__page-url"
        onCopyUsingBtn={() => urlLinkEl.current.select()}
      />
      <NxTextLink newTab href={policyViolationUrl}>
        <input
          readOnly
          ref={urlLinkEl}
          value={policyViolationUrl}
          className="visual-testing-ignore iq-request-waivers-popover__link-input"
        />
      </NxTextLink>
      <NxCodeSnippet
        label="Curl Example"
        content={curlExample}
        className="visual-testing-ignore iq-request-waivers-popover__curl"
      />
    </IqPopover>
  );
};

RequestWaiversPopover.propTypes = {
  violationDetails: violationDetailsPropTypes,
  onClose: PropTypes.func.isRequired,
};

export default RequestWaiversPopover;
