/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { join } from 'ramda';

import { capitalize } from 'MainRoot/util/jsUtil';

import OccurrencesPopoverContainer from '../occurrencesPopover/OccurrencesPopoverContainer';
import InnerSourceProducerAlertContainer from '../InnerSourceProducerAlert/InnerSourceProducerAlertContainer';
import InnerSourceProducerReportModalContainer from '../InnerSourceProducerReportModal/InnerSourceProducerReportModalContainer';
import InnerSourceProducerPermissionsModalContainer from '../InnerSourceProducerPermissionsModal/InnerSourceProducerPermissionsModalContainer';
import { NxButton, NxTextLink } from '@sonatype/react-shared-components';
import ComponentCoordinatesPopover from '../ComponentCoordinatesPopover/ComponentCoordinatesPopover';

export default function OverviewComponentInformation({
  componentInformation,
  toggleShowOccurrencesPopover,
  similarMatches,
  toggleShowSimilarMatches,
  loadInnerSourceProducerData,
  toggleShowComponentCoordinatesPopover,
}) {
  const {
    componentIdentifier,
    displayName,
    matchState,
    identificationSource,
    componentCategories = [],
    pathnames = [],
    website,
  } = componentInformation;

  useEffect(() => {
    loadInnerSourceProducerData();
  }, []);

  const isUnknown = !matchState || matchState === 'unknown';
  const format = isUnknown ? '' : componentIdentifier.format;
  const joinedComponentCategories = join(
    ',',
    componentCategories.map((category) => category.path)
  );

  const viewSimilarMatchesLink = !!similarMatches.length && (
    <span>
      {/* required space before link */ ' '}
      <a className="iq-identification-info-definition-list__similar-matches-link" onClick={toggleShowSimilarMatches}>
        (View Similar Matches)
      </a>
    </span>
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
          <a
            className="iq-identification-info-definition-list__occurrences-link"
            onClick={toggleShowOccurrencesPopover}
          >
            {pathnames.length + (pathnames.length > 1 ? ' Files' : ' File')}
          </a>
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
      <section id="overview-component-information-tile" className="nx-tile iq-component-information-tile">
        <OccurrencesPopoverContainer occurrences={pathnames} />
        <InnerSourceProducerReportModalContainer />
        <InnerSourceProducerPermissionsModalContainer />
        {!isUnknown && <ComponentCoordinatesPopover displayName={displayName} componentFormat={format} />}
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
  loadInnerSourceProducerData: PropTypes.func.isRequired,
  componentInformation: PropTypes.shape({
    componentIdentifier: PropTypes.shape({
      format: PropTypes.string,
    }),
    displayName: PropTypes.shape({
      parts: PropTypes.array,
    }).isRequired,
    matchState: PropTypes.string.isRequired,
    identificationSource: PropTypes.string,
    componentCategories: PropTypes.arrayOf(
      PropTypes.shape({
        path: PropTypes.string.isRequired,
      })
    ),
    pathnames: PropTypes.arrayOf(PropTypes.string).isRequired,
    dependencyInfo: PropTypes.shape({
      isDirectDependency: PropTypes.bool.isRequired,
    }),
    website: PropTypes.string,
  }),
  toggleShowOccurrencesPopover: PropTypes.func.isRequired,
  toggleShowComponentCoordinatesPopover: PropTypes.func.isRequired,
  toggleShowSimilarMatches: PropTypes.func.isRequired,
  similarMatches: PropTypes.array,
};
