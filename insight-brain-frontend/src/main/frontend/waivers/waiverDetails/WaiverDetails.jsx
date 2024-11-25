/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxTile,
  NxH2,
  NxLoadWrapper,
  NxReadOnly,
  NxButton,
  NxP,
  NxPositiveStatusIndicator,
  NxBlockquote,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import {
  formatWaiverDetails,
  isWaiverAllVersionsOrExact,
  shouldShowUpgradeIndicator,
  waiverMatcherStrategy,
} from 'MainRoot/util/waiverUtils';
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
import DeleteWaiverModalContainer from 'MainRoot/waivers/deleteWaiverModal/DeleteWaiverModalContainer';
import { setWaiverToDelete } from 'MainRoot/waivers/waiverActions';
import { faSitemap, faTerminal } from '@fortawesome/pro-solid-svg-icons';

export default function WaiverDetails() {
  const isLoading = useSelector(selectWaiverDetailsLoading);
  const loadError = useSelector(selectWaiverDetailsError);
  const details = useSelector(selectWaiverDetails);

  const {
    policyName,
    constraintName,
    reasons,
    expiration,
    comment,
    creatorName,
    dateCreated,
    vulnerabilityId,
    component,
    componentUpgradeAvailable,
    reasonText,
  } = formatWaiverDetails(details);

  const waiver = useSelector(selectWaiverDetails);
  const waiverToDelete = useSelector(selectWaiverToDelete);
  const isApplication = waiver?.scopeOwnerType === 'application';

  const scopeIcon = isApplication ? <NxFontAwesomeIcon icon={faTerminal} /> : <NxFontAwesomeIcon icon={faSitemap} />;

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
    return (
      <NxReadOnly.Data className="iq-waiver-details__constrain-conditions">
        <ul>
          {reasons.map((reason, index) => (
            <li key={index}>
              <span>{reason}</span>
            </li>
          ))}
        </ul>
      </NxReadOnly.Data>
    );
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
      return <NxPositiveStatusIndicator>Upgrade Available</NxPositiveStatusIndicator>;
    }
  };

  const getComponentVersion = () => {
    switch (component?.matcherStrategy) {
      case waiverMatcherStrategy.ALL_VERSIONS:
        return 'All Versions';
      case waiverMatcherStrategy.ALL_COMPONENTS:
        return '--';
      case waiverMatcherStrategy.EXACT_COMPONENT:
        return component?.componentIdentifier?.coordinates?.version;
      default:
        return 'All Versions';
    }
  };

  useEffect(() => {
    getDetails();
    return () => dispatch(closeVulnerabilityDetailsModal());
  }, []);

  return (
    <NxTile className="nx-viewport-sized" id="waiver-details-page" data-testid="waiver-details-page">
      <NxTile.Header className="iq-waiver-details-header-container">
        <NxTile.HeaderTitle>
          <NxH2 id="iq-waiver-details-header">Waiver Detail View</NxH2>
        </NxTile.HeaderTitle>
        <NxTile.HeaderActions className="iq-waiver-details__delete-waiver">
          <NxButton variant="tertiary" onClick={handleDeleteWaiverButtonClick}>
            Delete Waiver
          </NxButton>
        </NxTile.HeaderActions>
      </NxTile.Header>
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={getDetails}>
        <div className="iq-waiver-details-content">
          {/* Policy */}
          <NxReadOnly className="iq-waiver-details__policy">
            <NxReadOnly.Label>Policy</NxReadOnly.Label>
            <NxReadOnly.Data>{policyName}</NxReadOnly.Data>
          </NxReadOnly>
          {/* Policy Constraint */}
          <NxReadOnly className="iq-waiver-details__constraint">
            <NxReadOnly.Label>Policy Constraint</NxReadOnly.Label>
            <NxReadOnly.Data>
              <strong>{constraintName}</strong> is in violation for:
            </NxReadOnly.Data>
            {reasons && renderConditions()}
          </NxReadOnly>
          {vulnerabilityId && (
            <>
              <NxButton
                className="iq-waiver-details__vulnerability-details-button"
                onClick={onVulnerabilityDetailsClick}
                variant="tertiary"
              >
                Vulnerability Details
              </NxButton>
              <VulnerabilityDetailsModalContainer />
            </>
          )}
          <NxReadOnly className="nx-read-only--grid">
            {/* Scope */}
            <NxReadOnly.Item className="iq-waiver-details__scope">
              <NxReadOnly.Label>Scope</NxReadOnly.Label>
              <NxReadOnly.Data>
                {scopeIcon}
                {waiver?.scopeOwnerName}
              </NxReadOnly.Data>
            </NxReadOnly.Item>
            {/* Waiver Expiration */}
            <NxReadOnly.Item className="iq-waiver-details__expiration">
              <NxReadOnly.Label>Waiver Expiration</NxReadOnly.Label>
              <NxReadOnly.Data>{expiration}</NxReadOnly.Data>
            </NxReadOnly.Item>
            {/* Components */}
            <NxReadOnly.Item className="iq-waiver-details__components">
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
                    </div>
                  </>
                ) : (
                  '--'
                )}
              </NxReadOnly.Data>
            </NxReadOnly.Item>
            {/* Version */}
            <NxReadOnly.Item className="iq-waiver-details__version">
              <NxReadOnly.Label>Version</NxReadOnly.Label>
              <NxReadOnly.Data data-testid="waiver-details-version">
                {getComponentVersion()} {renderUpgradeAvailableIndicator()}
              </NxReadOnly.Data>
            </NxReadOnly.Item>
            {/* Reason */}
            <NxReadOnly.Item className="iq-waiver-details__reason">
              <NxReadOnly.Label>Reason</NxReadOnly.Label>
              <NxReadOnly.Data>{reasonText}</NxReadOnly.Data>
            </NxReadOnly.Item>
            {/* Date Created */}
            <NxReadOnly.Item className="iq-waiver-details__date-created">
              <NxReadOnly.Label>Date Created</NxReadOnly.Label>
              <NxReadOnly.Data>{dateCreated}</NxReadOnly.Data>
            </NxReadOnly.Item>
          </NxReadOnly>

          {/* Comments */}
          <NxReadOnly className="iq-waiver-details__comments">
            <NxReadOnly.Label>Comments</NxReadOnly.Label>
            <NxReadOnly.Data>
              <NxBlockquote>{comment}</NxBlockquote>
            </NxReadOnly.Data>
          </NxReadOnly>
          {/* Created By */}
          <NxReadOnly className="iq-waiver-details__created-by">
            <NxReadOnly.Label>Created By</NxReadOnly.Label>
            <NxReadOnly.Data>{creatorName}</NxReadOnly.Data>
          </NxReadOnly>
        </div>
        {waiverToDelete && <DeleteWaiverModalContainer />}
      </NxLoadWrapper>
    </NxTile>
  );
}
