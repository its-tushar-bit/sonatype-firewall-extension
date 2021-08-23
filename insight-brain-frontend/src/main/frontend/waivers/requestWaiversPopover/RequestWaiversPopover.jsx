/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useRef } from 'react';
import PropTypes from 'prop-types';
import { faArrowToRight } from '@fortawesome/pro-solid-svg-icons';
import { NxInfoAlert, NxCodeSnippet, NxTextLink, NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';

import IqPopover from '../../react/IqPopover/IqPopover';
import { getRequestWaiverUrl } from '../../util/CLMLocation';
import { extractViolationDetails } from '../../util/violationDetailsUtil';
import { uriTemplate } from '../../util/urlUtil';

const RequestWaivers = ({ isShown, onClose, violationDetails }) => {
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
    isShown && (
      <IqPopover size="automatic" onClose={onClose} id="request-waivers">
        <IqPopover.Header className="legal-dashboard-filter-header">
          <div className="request-waivers-header__title">
            <h2 className="nx-h2 request-waivers-header__title-text">Request Waiver</h2>
            <NxButton onClick={onClose} variant="icon-only" title="Close" id="request-waivers-close-button">
              <NxFontAwesomeIcon icon={faArrowToRight} />
            </NxButton>
          </div>
        </IqPopover.Header>
        <NxInfoAlert>
          To request a waiver, please share the Policy Violation ID and sample curl command (found below) with the
          approver.{' '}
          <NxTextLink
            href="https://help.sonatype.com/iqserver/reporting/application-composition-report/waivers#Waivers-RequestingaWaivertobeAdded"
            external
          >
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
          className="visual-testing-ignore iq-request-waivers-page__violation-id"
          id="request-waivers-policy-violation-id"
        />
        <NxCodeSnippet
          label="Policy Violation Details Page"
          content={policyViolationUrl}
          className="visual-testing-ignore iq-request-waivers-page__page-url"
          onCopyUsingBtn={() => urlLinkEl.current.select()}
        />
        <NxTextLink newTab href={policyViolationUrl}>
          <input
            readOnly
            ref={urlLinkEl}
            value={policyViolationUrl}
            className="visual-testing-ignore iq-request-waivers-page__link-input"
          />
        </NxTextLink>
        <NxCodeSnippet label="Curl Example" content={curlExample} className="iq-request-waivers-page__curl" />
      </IqPopover>
    )
  );
};

RequestWaivers.propTypes = {
  isShown: PropTypes.bool,
  violationDetails: PropTypes.shape({
    policyViolationId: PropTypes.string,
    policyName: PropTypes.string,
    componentName: PropTypes.string,
    constraintName: PropTypes.string,
    reasons: PropTypes.arrayOf(
      PropTypes.shape({
        reason: PropTypes.string,
      })
    ),
  }),
  onClose: PropTypes.func.isRequired,
};

export default RequestWaivers;
