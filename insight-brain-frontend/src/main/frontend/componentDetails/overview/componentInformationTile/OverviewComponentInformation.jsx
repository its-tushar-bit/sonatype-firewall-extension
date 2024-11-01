/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { join, path, isNil } from 'ramda';

import { capitalize } from 'MainRoot/util/jsUtil';

import OccurrencesPopoverContainer from '../occurrencesPopover/OccurrencesPopoverContainer';
import InnerSourceProducerAlertContainer from '../InnerSourceProducerAlert/InnerSourceProducerAlertContainer';
import InnerSourceProducerReportModalContainer from '../InnerSourceProducerReportModal/InnerSourceProducerReportModalContainer';
import InnerSourceProducerPermissionsModalContainer from '../InnerSourceProducerPermissionsModal/InnerSourceProducerPermissionsModalContainer';
import { NxButton, NxTextLink, NxWarningAlert, useToggle } from '@sonatype/react-shared-components';
import ComponentCoordinatesPopover from '../ComponentCoordinatesPopover/ComponentCoordinatesPopover';
import { componentInformationPropType } from 'MainRoot/componentDetails/overview/overviewTypes';

export default function OverviewComponentInformation({
  componentInformation,
  toggleShowOccurrencesPopover,
  similarMatches,
  versionExplorerData,
  toggleShowSimilarMatches,
  loadInnerSourceProducerData,
  toggleShowComponentCoordinatesPopover,
}) {
  const {
    componentIdentifier,
    displayName,
    packageUrl,
    matchState,
    identificationSource,
    componentCategories = [],
    pathnames = [],
    website,
  } = componentInformation;

  useEffect(() => {
    if (loadInnerSourceProducerData) loadInnerSourceProducerData();
  }, []);

  const repositorySourceMessage = path(['sourceResponse', 'sourceMessage'], versionExplorerData);
  const [isRepositorySourceAlertOpen, dismissRepositorySourceAlert] = useToggle(true);
  const isUnknown = !matchState || matchState === 'unknown';
  const format = isUnknown ? '' : componentIdentifier.format;
  const joinedComponentCategories = join(
    ',',
    componentCategories.map((category) => category.path)
  );

  const viewSimilarMatchesLink = similarMatches?.length > 0 && (
    <span>
      {/* required space before link */ ' '}
      <NxTextLink
        className="iq-identification-info-definition-list__similar-matches-link"
        onClick={toggleShowSimilarMatches}
        tabIndex="0"
        onKeyDown={(evt) => {
          if (evt.key === 'Enter') {
            toggleShowSimilarMatches();
          }
        }}
      >
        (View Similar Matches)
      </NxTextLink>
    </span>
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
    <dl className="nx-read-only nx-read-only--grid iq-identification-info-definition-list">
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Match State</dt>
        <dd className="nx-read-only__data">
          {capitalize(matchState)}
          {viewSimilarMatchesLink}
        </dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Identification Source</dt>
        <dd className="nx-read-only__data">{identificationSource}</dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Occurrences</dt>
        <dd className="nx-read-only__data">
          <NxTextLink
            className="iq-identification-info-definition-list__occurrences-link"
            onClick={toggleShowOccurrencesPopover}
            tabIndex="0"
            onKeyDown={(evt) => {
              if (evt.key === 'Enter') {
                toggleShowOccurrencesPopover();
              }
            }}
          >
            {pathnames.length + (pathnames.length > 1 ? ' Files' : ' File')}
          </NxTextLink>
        </dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Website</dt>
        <dd className="nx-read-only__data">
          {website && (
            <NxTextLink external href={website} className="iq-identification-info-definition-list__website-link">
              Visit Project Website
            </NxTextLink>
          )}
        </dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Category</dt>
        <dd className="nx-read-only__data">{joinedComponentCategories || (isUnknown ? '' : 'Other')}</dd>
      </div>
    </dl>
  );

  return (
    <>
      <InnerSourceProducerAlertContainer />
      {repositorySourceAlert}
      <section id="overview-component-information-tile" className="nx-tile iq-component-information-tile">
        <OccurrencesPopoverContainer occurrences={pathnames} />
        <InnerSourceProducerReportModalContainer />
        <InnerSourceProducerPermissionsModalContainer />
        {!isUnknown && (
          <ComponentCoordinatesPopover displayName={displayName} componentFormat={format} packageUrl={packageUrl} />
        )}
        <header className="nx-tile-header">
          <div className="nx-tile-header__title">
            <h2 className="nx-h2">Component Information</h2>
          </div>
          {!isUnknown && (
            <div className="nx-tile__actions">
              <NxButton
                className="component-coordinates-button"
                variant="tertiary"
                onClick={toggleShowComponentCoordinatesPopover}
              >
                View Coordinates
              </NxButton>
            </div>
          )}
        </header>
        <div className="nx-tile-content">{identificationInfoSectionContent}</div>
      </section>
    </>
  );
}

OverviewComponentInformation.propTypes = {
  loadInnerSourceProducerData: PropTypes.func,
  componentInformation: componentInformationPropType,
  toggleShowOccurrencesPopover: PropTypes.func,
  toggleShowComponentCoordinatesPopover: PropTypes.func.isRequired,
  toggleShowSimilarMatches: PropTypes.func,
  similarMatches: PropTypes.array,
};
