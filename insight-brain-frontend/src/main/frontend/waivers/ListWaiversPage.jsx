/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import { faPlus } from '@fortawesome/free-solid-svg-icons';

import { NxBackButton, NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import LoadWrapper from '../react/LoadWrapper';
import MaximizedContainer from '../react/MaximizedContainer';
import ViolationExclamation from '../react/ViolationExclamation';
import { extractViolationDetails } from '../util/violationDetailsUtil';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfoTile';
import ListWaiversTable, { waiverType } from './ListWaiversTable';

export default function ListWaiversPage(props) {
  const {
    activeWaivers,
    expiredWaivers,
    loadViolation,
    violationId,
    loading,
    violationDetails,
    violationDetailsError,
    $state
  } = props;

  useEffect(() => {
    if (violationId) {
      loadViolation(violationId);
    }
  }, [violationId]);

  const redirectToAddWaiverPage = () => $state.go('addWaiver', { violationId });
  const violationDetailsHref = $state.href($state.get('sidebarView.violation'), { 'id': violationId });

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
        <LoadWrapper loading={ loading || !violationDetails } error={ violationDetailsError }>
          <NxBackButton targetPageTitle="Violation Details" href={ violationDetailsHref } />
          <div className="nx-page-title">
            <h1 className="nx-h1">Waivers for Violation</h1>
            <div className="list-waivers--threat-indicator">
              { threatLevelCategory && <ViolationExclamation threatLevelCategory={ threatLevelCategory } /> }
              <span className={ policyClassnames }>{ policyName }</span>
            </div>
          </div>
          <div className="nx-tile">
            <div className="nx-tile-header nx-tile-header--hrule">
              <h2 className="nx-h2">Violation Details</h2>
            </div>
            <div className="nx-tile-content list-waivers--details">
              <div className="list-waivers--constraint">
                <h3 className="nx-label iq-read-only">Constraint Name</h3>
                <div className="iq-read-only-data">{ constraintName }</div>
              </div>
              <div className="list-waivers--conditions">
                <h3 className="nx-label iq-read-only">Conditions</h3>
                <div className="iq-read-only-data">
                  {reasons && reasons.map((reason, index) =>
                    <span key={index}>{reason}</span>
                  )}
                </div>
              </div>
              <div className="list-waivers--component-name">
                <h3 className="nx-label iq-read-only">Component Name</h3>
                <div className="iq-read-only-data">{ componentName }</div>
              </div>
            </div>
          </div>
          <div className="nx-tile">
            <div className="nx-tile-header">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2">Applicable Waivers</h2>
              </div>
              <div className="nx-tile__actions">
                <NxButton className="nx-btn--tertiary" onClick={ redirectToAddWaiverPage }>
                  <NxFontAwesomeIcon icon={ faPlus }/>
                  <span>Add Waiver</span>
                </NxButton>
              </div>
            </div>
            <div className="nx-tile-content">
              <ListWaiversTable { ...({ activeWaivers, expiredWaivers, violationDetails }) }/>
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
  violationDetailsError: PropTypes.any,
  loading: PropTypes.bool,
  loadViolation: PropTypes.func.isRequired,
  $state: PropTypes.shape({
    go: PropTypes.func.isRequired
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
  })
};
