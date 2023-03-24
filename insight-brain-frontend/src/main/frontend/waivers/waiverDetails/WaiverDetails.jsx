/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxTile, NxH2, NxLoadWrapper, NxTextLink, NxReadOnly, NxButton, NxP } from '@sonatype/react-shared-components';
import { formatWaiverDetails, isWaiverAllVersionsOrExact, shouldShowUpgradeIndicator } from 'MainRoot/util/waiverUtils';
import {
  selectWaiverDetails,
  selectWaiverDetailsLoading,
  selectWaiverDetailsError,
  selectWaiverToDelete,
} from './waiverDetailsSelectors';
import { actions } from './waiverDetailsSlice';
import {
  openVulnerabilityDetailsModal,
  closeVulnerabilityDetailsModal,
} from 'MainRoot/vulnerabilityDetails/vulnerabilityDetailsModalActions';
import VulnerabilityDetailsModalContainer from 'MainRoot/vulnerabilityDetails/VulnerabilityDetailsModalContainer';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import UpgradeAvailableIndicator from 'MainRoot/react/upgradeAvailableIndicator/UpgradeAvailableIndicator';
import DeleteWaiverModalContainer from 'MainRoot/waivers/deleteWaiverModal/DeleteWaiverModalContainer';
import { setWaiverToDelete } from 'MainRoot/waivers/waiverActions';

export default function waiverDetails() {
  const isLoading = useSelector(selectWaiverDetailsLoading);
  const loadError = useSelector(selectWaiverDetailsError);
  const details = useSelector(selectWaiverDetails);

  const {
    policyName,
    constraintName,
    reasons,
    waiverScope,
    expiration,
    comment,
    creatorName,
    dateCreated,
    vulnerabilityId,
    component,
    componentUpgradeAvailable,
  } = formatWaiverDetails(details);

  const waiver = useSelector(selectWaiverDetails);
  const waiverToDelete = useSelector(selectWaiverToDelete);

  const componentMatchStrategy = () => component.matcherStrategy;

  const dispatch = useDispatch();
  const getDetails = () => dispatch(actions.loadWaiver());

  const handleDeleteWaiverButtonClick = () => {
    dispatch(setWaiverToDelete(waiver));
  };

  const onVulnerabilityDetailsClick = () => {
    dispatch(
      openVulnerabilityDetailsModal({
        vulnerabilityId: vulnerabilityId,
      })
    );
  };

  const renderConditions = () => {
    return reasons.map((reason, index) => (
      <NxReadOnly.Data key={index}>
        <span>{reason}</span>
      </NxReadOnly.Data>
    ));
  };

  // Only show disclaimer if the component has a displayName. It should never
  // be displayed if the component is "Unknown"
  const renderDisclaimer = () => {
    if (component.displayName) {
      return (
        <NxP className="iq-waiver-details__disclaimer">
          &#42;Indicates the component name when the waiver was created
        </NxP>
      );
    }
  };

  // Only show upgrade indicator if component is exact version. It should never
  // be displayed for ALL_COMPONENTS or ALL_VERSIONS matches, or unknown components.
  const renderUpgradeAvailableIndicator = () => {
    if (shouldShowUpgradeIndicator(componentUpgradeAvailable, component)) {
      return <UpgradeAvailableIndicator isAbbreviated={false} />;
    }
  };

  useEffect(() => {
    getDetails();
    return () => dispatch(closeVulnerabilityDetailsModal());
  }, []);

  return (
    <NxTile className="nx-viewport-sized" id="waiver-details-page">
      <NxTile.Header className="iq-waiver-details-header-container">
        <NxTile.HeaderTitle>
          <NxH2 id="iq-waiver-details-header">Waiver Detail View</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={getDetails}>
        <div className="iq-waiver-details-content">
          {/* Policy */}
          <NxReadOnly className="iq-waiver-details__policy">
            <NxReadOnly.Label>Policy</NxReadOnly.Label>
            <NxReadOnly.Data>{policyName}</NxReadOnly.Data>
          </NxReadOnly>
          {/* Policy */}
          <NxReadOnly className="iq-waiver-details__constraint">
            <NxReadOnly.Label>Constraint Name</NxReadOnly.Label>
            <NxReadOnly.Data>{constraintName}</NxReadOnly.Data>
          </NxReadOnly>
          {/* Conditions */}
          <NxReadOnly className="iq-waiver-details__conditions">
            <NxReadOnly.Label>Conditions</NxReadOnly.Label>
            {reasons && renderConditions()}
          </NxReadOnly>
          {/* Vulnerability Details */}
          {vulnerabilityId && (
            <div className="iq-waiver-details__vulnerability-details-link">
              <NxTextLink onClick={onVulnerabilityDetailsClick}>See Security Vulnerability Details</NxTextLink>
              <VulnerabilityDetailsModalContainer />
            </div>
          )}
          {/* Scope */}
          <NxReadOnly className="iq-waiver-details__scope">
            <NxReadOnly.Label>Scope</NxReadOnly.Label>
            <NxReadOnly.Data>{waiverScope}</NxReadOnly.Data>
          </NxReadOnly>
          {/* Components */}
          <NxReadOnly className="iq-waiver-details__components">
            <NxReadOnly.Label>Components</NxReadOnly.Label>
            <NxReadOnly.Data>
              {component && isWaiverAllVersionsOrExact(component) ? (
                <>
                  {renderDisclaimer()}
                  <div className="component-name-and-upgrade-indicator">
                    <ComponentDisplay
                      component={component}
                      truncate={true}
                      matcherStrategy={componentMatchStrategy()}
                    />
                    {renderUpgradeAvailableIndicator()}
                  </div>
                </>
              ) : (
                'All components'
              )}
            </NxReadOnly.Data>
          </NxReadOnly>
          {/* Waiver Expiration */}
          <NxReadOnly className="iq-waiver-details__expiration">
            <NxReadOnly.Label>Waiver Expiration</NxReadOnly.Label>
            <NxReadOnly.Data>{expiration}</NxReadOnly.Data>
          </NxReadOnly>
          {/* Comments */}
          <NxReadOnly className="iq-waiver-details__comments">
            <NxReadOnly.Label>Comments</NxReadOnly.Label>
            <NxReadOnly.Data>{comment}</NxReadOnly.Data>
          </NxReadOnly>
          {/* Created By */}
          <NxReadOnly className="iq-waiver-details__created-by">
            <NxReadOnly.Label>Created By</NxReadOnly.Label>
            <NxReadOnly.Data>{creatorName}</NxReadOnly.Data>
          </NxReadOnly>
          {/* Date Created */}
          <NxReadOnly className="iq-waiver-details__date-created">
            <NxReadOnly.Label>Date Created</NxReadOnly.Label>
            <NxReadOnly.Data>{dateCreated}</NxReadOnly.Data>
          </NxReadOnly>
          <div className="iq-waiver-details__delete-waiver">
            <NxButton variant="tertiary" onClick={handleDeleteWaiverButtonClick}>
              Delete Waiver
            </NxButton>
          </div>
          {waiverToDelete && <DeleteWaiverModalContainer />}
        </div>
      </NxLoadWrapper>
    </NxTile>
  );
}
