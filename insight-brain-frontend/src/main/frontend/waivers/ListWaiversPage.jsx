/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, Fragment } from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxH1,
  NxH2,
  NxPageTitle,
  NxReadOnly,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';

import LoadWrapper from '../react/LoadWrapper';
import ViolationExclamation from '../react/ViolationExclamation';
import { extractViolationDetails } from '../util/violationDetailsUtil';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfoTile';
import ListWaiversTable from './ListWaiversTable';
import { waiverType } from '../util/waiverUtils';
import DeleteWaiverModalContainer from './deleteWaiverModal/DeleteWaiverModalContainer';
import ListWaiversBackButton from './ListWaiversBackButton';

export default function ListWaiversPage(props) {
  const {
    activeWaivers,
    expiredWaivers,
    loadManageWaiversData,
    loadingManageWaiversData,
    loadingApplicableWaivers,
    violationDetails,
    loadManageWaiversDataError,
    loadApplicableWaiversError,
    hasPermissionForAppWaivers,
    waiverToDelete,
    setWaiverToDelete,
    loadApplicableWaivers,
    stateGo,
    isCurrentRouteName,
    ...backButtonProps
  } = props;

  const {
    violationId,
    repositoryPolicyId,
    componentIdentifier,
    hash,
    matchState,
    proprietary,
    identificationSource,
    pathname,
    isFirewall,
    isFirewallOrRepositoryComponent,
    tabId,
  } = backButtonProps;

  function load() {
    if (violationId) {
      loadManageWaiversData(violationId);
    }
  }

  function reloadApplicableWaivers() {
    loadApplicableWaivers(violationId);
  }

  useEffect(load, [violationId]);

  const { componentName, constraintName, policyName, reasons, threatLevelCategory } = extractViolationDetails(
    violationDetails
  );

  const redirectToAddWaiverPage = () => {
    if (isFirewallOrRepositoryComponent) {
      return (
        hasPermissionForAppWaivers &&
        stateGo(`${isFirewall ? 'firewall' : 'repository'}.addWaiver`, {
          repositoryId: repositoryPolicyId,
          componentIdentifier,
          componentHash: hash,
          matchState,
          violationId,
          proprietary,
          identificationSource,
          pathname,
          tabId,
        })
      );
    }
    return hasPermissionForAppWaivers && stateGo('addWaiver', { violationId });
  };

  const policyClassnames = classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`);

  return (
    <Fragment>
      {waiverToDelete && <DeleteWaiverModalContainer />}
      <div id="list-waivers-page" className="nx-page-main list-waivers-page">
        <LoadWrapper
          loading={loadingManageWaiversData || !violationDetails}
          error={loadManageWaiversDataError}
          retryHandler={load}
        >
          <ListWaiversBackButton {...backButtonProps} />
          <NxPageTitle>
            <NxH1>Waivers for Violation</NxH1>
            {threatLevelCategory && <ViolationExclamation threatLevelCategory={threatLevelCategory} />}
            <span className={policyClassnames}>{policyName}</span>
          </NxPageTitle>
          <NxTile id="list-waivers-details">
            <NxTile.Header>
              <NxTile.HeaderTitle>
                <NxH2>Violation Details</NxH2>
              </NxTile.HeaderTitle>
            </NxTile.Header>
            <NxTile.Content>
              {/* Constraints */}
              <NxReadOnly className="list-waivers_constraints">
                <NxReadOnly.Label>Constraint Name</NxReadOnly.Label>
                <NxReadOnly.Data id="list-waivers-constraint-name">{constraintName}</NxReadOnly.Data>
              </NxReadOnly>
              {/* Conditions  */}
              <NxReadOnly className="list-waivers_conditions">
                <NxReadOnly.Label>Conditions</NxReadOnly.Label>
                {reasons &&
                  reasons.map((reason, index) => (
                    <NxReadOnly.Data className="list-waivers-condition" key={index}>
                      {reason}
                    </NxReadOnly.Data>
                  ))}
                <NxReadOnly.Data></NxReadOnly.Data>
              </NxReadOnly>
              {/* Component  */}
              <NxReadOnly className="list-waivers_component">
                <NxReadOnly.Label>Component Name</NxReadOnly.Label>
                <NxReadOnly.Data id="list-waivers-component-name">{componentName}</NxReadOnly.Data>
              </NxReadOnly>
            </NxTile.Content>
          </NxTile>
          <NxTile id="list-waivers-applicable">
            <NxTile.Header>
              <NxTile.HeaderTitle>
                <NxH2>Applicable Waivers</NxH2>
              </NxTile.HeaderTitle>
              <NxTile.HeaderActions>
                {!isCurrentRouteName && (
                  <NxButton
                    variant="tertiary"
                    onClick={() => stateGo('requestWaiver', { violationId })}
                    id="request-waiver-btn"
                  >
                    <span>Request Waiver</span>
                  </NxButton>
                )}
                <NxTooltip
                  id="add-waiver-btn-tooltip"
                  title={hasPermissionForAppWaivers ? '' : 'Insufficient permissions to Add Waiver'}
                >
                  <NxButton
                    className={classnames({
                      disabled: !hasPermissionForAppWaivers,
                    })}
                    variant="tertiary"
                    onClick={redirectToAddWaiverPage}
                    id="add-waiver-btn"
                  >
                    <NxFontAwesomeIcon icon={faPlus} />
                    <span>Add Waiver</span>
                  </NxButton>
                </NxTooltip>
              </NxTile.HeaderActions>
            </NxTile.Header>

            <NxTile.Content>
              <ListWaiversTable
                {...{
                  activeWaivers,
                  expiredWaivers,
                  violationDetails,
                  setWaiverToDelete,
                  loadingApplicableWaivers,
                  loadApplicableWaiversError,
                  reloadApplicableWaivers,
                }}
              />
            </NxTile.Content>
          </NxTile>
        </LoadWrapper>
      </div>
    </Fragment>
  );
}

ListWaiversPage.propTypes = {
  ...ListWaiversBackButton.propTypes,
  activeWaivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  expiredWaivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  loadingManageWaiversData: PropTypes.bool,
  loadManageWaiversDataError: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
  loadApplicableWaiversError: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
  loadingApplicableWaivers: PropTypes.bool,
  loadManageWaiversData: PropTypes.func.isRequired,
  waiverToDelete: PropTypes.shape(waiverType),
  setWaiverToDelete: PropTypes.func.isRequired,
  loadApplicableWaivers: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
    displayName: PropTypes.shape({
      parts: PropTypes.arrayOf(PropTypes.object),
    }),
    filename: PropTypes.string,
    policyViolationId: PropTypes.string.isRequired,
  }),
  hasPermissionForAppWaivers: PropTypes.bool,
};
