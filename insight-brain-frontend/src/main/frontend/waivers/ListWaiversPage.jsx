/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, Fragment } from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import { NxButton, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';

import LoadWrapper from '../react/LoadWrapper';
import ViolationExclamation from '../react/ViolationExclamation';
import { extractViolationDetails } from '../util/violationDetailsUtil';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfoTile';
import ListWaiversTable from './ListWaiversTable';
import { waiverType } from '../util/waiverUtils';
import DeleteWaiverModalContainer from './deleteWaiverModal/DeleteWaiverModalContainer';
import ListWaiversBackButton from './ListWaiversBackButton';
import RequestWaiversPopover from './requestWaiversPopover/RequestWaiversPopoverContainer';

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
    isRequestWaiverPopoverShown,
    setIsRequestWaiverPopoverShown,
    ...backButtonProps
  } = props;

  const { violationId } = backButtonProps;

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

  const redirectToAddWaiverPage = () => hasPermissionForAppWaivers && stateGo('addWaiver', { violationId });

  const policyClassnames = classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`);

  return (
    <Fragment>
      {waiverToDelete && <DeleteWaiverModalContainer />}
      {isRequestWaiverPopoverShown && <RequestWaiversPopover onClose={() => setIsRequestWaiverPopoverShown(false)} />}
      <div id="list-waivers-page" className="nx-page-main list-waivers-page">
        <LoadWrapper
          loading={loadingManageWaiversData || !violationDetails}
          error={loadManageWaiversDataError}
          retryHandler={load}
        >
          <ListWaiversBackButton {...backButtonProps} />
          <div className="nx-page-title">
            <h1 className="nx-h1">Waivers for Violation</h1>
            <div className="list-waivers__threat-indicator">
              {threatLevelCategory && <ViolationExclamation threatLevelCategory={threatLevelCategory} />}
              <span className={policyClassnames}>{policyName}</span>
            </div>
          </div>
          <div className="nx-tile">
            <div className="nx-tile-header nx-tile-header--hrule">
              <h2 className="nx-h2">Violation Details</h2>
            </div>
            <div className="nx-tile-content">
              <div className="nx-form-group iq-read-only">
                <label className="nx-label">
                  <span className="nx-label__text">Constraint Name</span>
                </label>
                <div id="list-waivers-constraint-name" className="iq-read-only-data">
                  {constraintName}
                </div>
              </div>
              <div className="nx-form-group iq-read-only">
                <label className="nx-label">
                  <span className="nx-label__text">Conditions</span>
                </label>
                <div id="list-waivers-conditions" className="iq-read-only-data iq-read-only-data--vertical">
                  {reasons && reasons.map((reason, index) => <span key={index}>{reason}</span>)}
                </div>
              </div>
              <div className="nx-form-group iq-read-only">
                <label className="nx-label">
                  <span className="nx-label__text">Component Name</span>
                </label>
                <div id="list-waivers-component-name" className="iq-read-only-data">
                  {componentName}
                </div>
              </div>
            </div>
          </div>
          <div className="nx-tile">
            <div className="nx-tile-header">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2">Applicable Waivers</h2>
              </div>
              <div className="nx-tile__actions">
                <NxButton
                  variant="tertiary"
                  onClick={() => setIsRequestWaiverPopoverShown(true)}
                  id="request-waiver-btn"
                >
                  <span>Request Waiver</span>
                </NxButton>
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
              </div>
            </div>
            <div className="nx-tile-content">
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
            </div>
          </div>
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
  isRequestWaiverPopoverShown: PropTypes.bool,
  loadApplicableWaiversError: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
  loadingApplicableWaivers: PropTypes.bool,
  loadManageWaiversData: PropTypes.func.isRequired,
  waiverToDelete: PropTypes.shape(waiverType),
  setIsRequestWaiverPopoverShown: PropTypes.func.isRequired,
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
