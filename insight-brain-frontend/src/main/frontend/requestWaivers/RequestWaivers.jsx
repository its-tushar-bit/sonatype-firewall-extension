/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { map, chain, prop } from 'ramda';
import { NxInfoAlert, NxCodeSnippet, NxLoadWrapper, NxTextLink } from '@sonatype/react-shared-components';
import { getRequestWaiverUrl } from '../util/CLMLocation';
import BackButton from '../react/BackButton';
import { useRouterState } from '../react/RouterStateContext';

const RequestWaivers = ({ loadError, isLoading, policyViolation, loadComponentDetails }) => {
  const uiRouterState = useRouterState();

  useEffect(() => {
    loadComponentDetails();
  }, []);

  const { policyViolationId = '', policyName = '', constraints = [], derivedComponentName = '' } =
    policyViolation || {};

  const curlExample = `curl -X POST -u user:pass -H "Content-Type: text/plain; charset=UTF-8" ${getRequestWaiverUrl(
    policyViolationId
  )} --data-binary 'waiver comment (optional)'`;

  const conditionReasons = map(prop('conditionReason'), chain(prop('conditions'), constraints));

  const constraintNameElements = constraints.map(({ constraintName, constraintId }) => (
    <dd key={constraintId} className="nx-read-only__data">
      {constraintName}
    </dd>
  ));

  const conditionsElements = conditionReasons.map((conditionReason) => (
    <dd key={conditionReason} className="nx-read-only__data">
      {conditionReason}
    </dd>
  ));

  return (
    <main className="nx-page-main" id="request-waivers-page">
      <BackButton stateName="applicationReport.policy" $state={uiRouterState} />
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={loadComponentDetails}>
        <header className="nx-page-title">
          <h1 className="nx-h1">Request Waiver</h1>
        </header>
        <section className="nx-tile">
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Waiver information</h2>
            </div>
          </header>
          <NxInfoAlert>
            To request a waiver, please share the Policy Violation ID and sample curl command (found below) with the
            approver.{' '}
            <NxTextLink
              href="https://help.sonatype.com/iqserver/reporting/application-composition-report/waivers#Waivers-RequestingaWaivertobeAdded"
              external
            >
              Learn about automating waiver requests.
            </NxTextLink>{' '}
            You can also share the link to the violation details page where a waiver can be added.
          </NxInfoAlert>
          <div className="nx-tile-content">
            <dl className="nx-read-only">
              <dt className="nx-read-only__label">Component</dt>
              <dd className="nx-read-only__data">{derivedComponentName}</dd>
              <dt className="nx-read-only__label">Policy</dt>
              <dd className="nx-read-only__data">{policyName}</dd>
              <dt className="nx-read-only__label">Constraint Name</dt>
              {constraintNameElements}
              <dt className="nx-read-only__label">Conditions</dt>
              {conditionsElements}
            </dl>
            <NxCodeSnippet
              label="Policy Violation ID"
              content={policyViolationId}
              className="iq-request-waivers-page__clipboard"
            />
            <NxCodeSnippet label="Curl Example" content={curlExample} />
          </div>
        </section>
      </NxLoadWrapper>
    </main>
  );
};

RequestWaivers.propTypes = {
  loadError: PropTypes.string,
  isLoading: PropTypes.bool,
  policyViolation: PropTypes.shape({
    policyViolationId: PropTypes.string,
    policyName: PropTypes.string,
    derivedComponentName: PropTypes.string,
    constraints: PropTypes.arrayOf(
      PropTypes.shape({
        constraintId: PropTypes.string,
        constraintName: PropTypes.string,
        conditions: PropTypes.arrayOf(
          PropTypes.shape({
            conditionReason: PropTypes.string,
          })
        ),
      })
    ),
  }),
  loadComponentDetails: PropTypes.func.isRequired,
};

export default RequestWaivers;
