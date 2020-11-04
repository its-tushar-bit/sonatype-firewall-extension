/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import { faPlus } from '@fortawesome/free-solid-svg-icons';

import { NxBackButton, NxButton, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import LoadWrapper from '../react/LoadWrapper';
import MaximizedContainer from '../react/MaximizedContainer';
import ViolationExclamation from '../react/ViolationExclamation';
import { extractViolationDetails } from '../util/violationDetailsUtil';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfoTile';
import ListWaiversTable, { waiverType } from './ListWaiversTable';
import DeleteWaiverModalContainer from './deleteWaiverModal/DeleteWaiverModalContainer';

export default function ListWaiversPage(props) {
  const {
    activeWaivers,
    expiredWaivers,
    loadManageWaiversData,
    violationId,
    loading,
    violationDetails,
    loadError,
    hasPermissionForAppWaivers,
    waiverToDelete,
    setWaiverToDelete,
    $state
  } = props;

  function load() {
    if (violationId) {
      loadManageWaiversData(violationId);
    }
  }

  useEffect(load, [violationId]);

  const redirectToAddWaiverPage = () => hasPermissionForAppWaivers && $state.go('addWaiver', { violationId });
  const violationDetailsHref = $state.href(
      $state.get('sidebarView.violation'),
      {
        id: violationId,
        type: $state.params.type,
        sidebarReference: $state.params.sidebarReference
      });

  const {
    componentName,
    constraintName,
    policyName,
    reasons,
    threatLevelCategory
  } = extractViolationDetails(violationDetails);

  const policyClassnames = classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`);

  return (
    <MaximizedContainer id="list-waivers-page" className="nx-page-content">
      <div className="nx-page-main list-waivers-page">
        { waiverToDelete && <DeleteWaiverModalContainer/> }
        <LoadWrapper loading={ loading || !violationDetails } error={ loadError } retryHandler={load}>
          <NxBackButton targetPageTitle="Violation Details" href={ violationDetailsHref } />
          <div className="nx-page-title">
            <h1 className="nx-h1">Waivers for Violation</h1>
            <div className="list-waivers__threat-indicator">
              { threatLevelCategory && <ViolationExclamation threatLevelCategory={ threatLevelCategory } /> }
              <span className={ policyClassnames }>{ policyName }</span>
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
                <div id="list-waivers-constraint-name" className="iq-read-only-data">{ constraintName }</div>
              </div>
              <div className="nx-form-group iq-read-only">
                <label className="nx-label">
                  <span className="nx-label__text">Conditions</span>
                </label>
                <div id="list-waivers-conditions" className="iq-read-only-data iq-read-only-data--vertical">
                  {reasons && reasons.map((reason, index) =>
                    <span key={index}>{reason}</span>
                  )}
                </div>
              </div>
              <div className="nx-form-group iq-read-only">
                <label className="nx-label">
                  <span className="nx-label__text">Component Name</span>
                </label>
                <div id="list-waivers-component-name" className="iq-read-only-data">{ componentName }</div>
              </div>
            </div>
          </div>
          <div className="nx-tile">
            <div className="nx-tile-header">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2">Applicable Waivers</h2>
              </div>
              <div className="nx-tile__actions">
                <NxTooltip id="add-waiver-btn-tooltip"
                           title={ hasPermissionForAppWaivers ? '' : 'Insufficient permissions to Add Waiver' }>
                  <NxButton className={ classnames({disabled: !hasPermissionForAppWaivers}) }
                            variant="tertiary"
                            onClick={ redirectToAddWaiverPage }
                            id="add-waiver-btn">
                    <NxFontAwesomeIcon icon={ faPlus }/>
                    <span>Add Waiver</span>
                  </NxButton>
                </NxTooltip>
              </div>
            </div>
            <div className="nx-tile-content">
              <ListWaiversTable { ...({ activeWaivers, expiredWaivers, violationDetails, setWaiverToDelete }) }/>
            </div>
          </div>
        </LoadWrapper>
      </div>
    </MaximizedContainer>
  );
}

ListWaiversPage.propTypes = {
  activeWaivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  expiredWaivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  loadError: PropTypes.any,
  loading: PropTypes.bool,
  loadManageWaiversData: PropTypes.func.isRequired,
  waiverToDelete: PropTypes.shape(waiverType),
  setWaiverToDelete: PropTypes.func.isRequired,
  $state: PropTypes.shape({
    go: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired,
    get: PropTypes.func.isRequired,
    params: PropTypes.shape({
      type: PropTypes.string,
      sidebarReference: PropTypes.string
    })
  }),
  violationId: PropTypes.string,
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
    displayName: PropTypes.shape({
      parts: PropTypes.arrayOf(PropTypes.object)
    }),
    filename: PropTypes.string,
    policyViolationId: PropTypes.string.isRequired
  }),
  hasPermissionForAppWaivers: PropTypes.bool
};
