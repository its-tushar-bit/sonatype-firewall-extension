/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { join, path, isNil } from 'ramda';

import { capitalize } from 'MainRoot/util/jsUtil';

import { NxButton, NxTextLink, NxWarningAlert, useToggle, NxTile, NxReadOnly } from '@sonatype/react-shared-components';
import ComponentCoordinatesPopover from 'MainRoot/componentDetails/overview/ComponentCoordinatesPopover/ComponentCoordinatesPopover';
import { selectFirewallComponentDetailsPage } from 'MainRoot/firewall/firewallSelectors';
import { actions } from 'MainRoot/componentDetails/overview/overviewSlice';
import { useSelector, useDispatch } from 'react-redux';

export default function FirewallOverviewComponentInformation() {
  const { versionExplorerData, componentDetails } = useSelector(selectFirewallComponentDetailsPage);
  const dispatch = useDispatch();
  const {
    componentIdentifier,
    displayName,
    matchState,
    identificationSource,
    componentCategories,
    website,
  } = componentDetails;

  const repositorySourceMessage = path(['sourceResponse', 'sourceMessage'], versionExplorerData);
  const [isRepositorySourceAlertOpen, dismissRepositorySourceAlert] = useToggle(true);
  const isUnknown = !matchState || matchState === 'unknown';
  const format = isUnknown ? '' : componentIdentifier.format;
  const joinedComponentCategories = join(
    ',',
    (componentCategories ?? []).map((category) => category.path)
  );

  const repositorySourceAlert = !isNil(repositorySourceMessage) && isRepositorySourceAlertOpen && (
    <NxWarningAlert
      id="inner-source-repository-source-alert"
      className="inner-source-repository-source-alert"
      onClose={dismissRepositorySourceAlert}
    >
      {repositorySourceMessage}
    </NxWarningAlert>
  );

  const identificationInfoSectionContent = (
    <NxTile.Content>
      <NxReadOnly className="iq-firewall-identification-info-definition-list nx-read-only--grid">
        <NxReadOnly.Item>
          <NxReadOnly.Label>Match State</NxReadOnly.Label>
          <NxReadOnly.Data>{capitalize(matchState)}</NxReadOnly.Data>
        </NxReadOnly.Item>
        <NxReadOnly.Item>
          <NxReadOnly.Label>Identification Source</NxReadOnly.Label>
          <NxReadOnly.Data>{identificationSource}</NxReadOnly.Data>
        </NxReadOnly.Item>
        <NxReadOnly.Item className="iq-firewall-identification-info-definition-list__website">
          <NxReadOnly.Label>Website</NxReadOnly.Label>
          <NxReadOnly.Data>
            {website && (
              <NxTextLink external href={website} className="iq-identification-info-definition-list__website-link">
                Visit Project Website
              </NxTextLink>
            )}
          </NxReadOnly.Data>
        </NxReadOnly.Item>
        <NxReadOnly.Item>
          <NxReadOnly.Label>Category</NxReadOnly.Label>
          <NxReadOnly.Data>{joinedComponentCategories || (isUnknown ? '' : 'Other')}</NxReadOnly.Data>
        </NxReadOnly.Item>
      </NxReadOnly>
    </NxTile.Content>
  );

  return (
    <>
      {repositorySourceAlert}
      <NxTile id="firewall-overview-component-information-tile" className="iq-component-information-tile">
        {!isUnknown && <ComponentCoordinatesPopover displayName={displayName} componentFormat={format} />}
        <NxTile.Header>
          <div className="nx-tile-header__title">
            <h2 className="nx-h2">Component Information</h2>
          </div>
          {!isUnknown && (
            <div className="nx-tile__actions">
              <NxButton
                className="component-coordinates-button"
                variant="tertiary"
                onClick={() => dispatch(actions.toggleShowComponentCoordinatesPopover())}
              >
                View Coordinates
              </NxButton>
            </div>
          )}
        </NxTile.Header>
        {identificationInfoSectionContent}
      </NxTile>
    </>
  );
}
