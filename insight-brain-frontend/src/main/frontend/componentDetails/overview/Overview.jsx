/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { join } from 'ramda';

import { formatTimeAgoUpToDay } from '../../util/dateUtils';
import { RiskRemediation } from './riskRemediation/RiskRemediation';

export default function Overview({ componentInformation, ancestors, routeName }) {
  const {
    componentIdentifier,
    displayName,
    createTime,
    matchState,
    identificationSource,
    componentCategories = [],
    pathnames,
    directDependency,
  } = componentInformation;

  const isUnknown = !matchState || matchState === 'unknown';
  const format = isUnknown ? '' : componentIdentifier.format;
  const catalogedDateAgo = createTime ? formatTimeAgoUpToDay(createTime) : '';
  const joinedComponentCategories = join(
    ',',
    componentCategories.map((category) => category.path)
  );

  const generalInfoSectionContent = (
    <dl className="nx-read-only nx-read-only--grid iq-general-info-definition-list">
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Type</dt>
        <dd className="nx-read-only__data">{format}</dd>
      </div>

      {displayName.parts.map(
        ({ field, value }) =>
          field && (
            <div className="nx-read-only__item" key={`${field}${value}`}>
              <dt className="nx-read-only__label">{`${field}`}</dt>
              <dd className="nx-read-only__data">{value}</dd>
            </div>
          )
      )}
    </dl>
  );

  const identificationInfoSectionContent = (
    <dl className="nx-read-only nx-read-only--grid iq-identification-info-definition-list">
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Cataloged</dt>
        <dd className="nx-read-only__data">{catalogedDateAgo}</dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Match State</dt>
        <dd className="nx-read-only__data">{matchState}</dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Occurrences</dt>
        <dd className="nx-read-only__data">{pathnames.length} File Matches</dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Identification Source</dt>
        <dd className="nx-read-only__data">{identificationSource}</dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Category</dt>
        <dd className="nx-read-only__data">{joinedComponentCategories}</dd>
      </div>
    </dl>
  );

  const overviewComponentInformationTile = (
    <section id="overview-component-information-tile" className="nx-tile iq-component-information-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Component Information</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <div className="nx-grid-row">
          <section className="nx-grid-col iq-component-data-col">
            <header className="nx-grid-header">
              <h3 className="nx-h3 nx-grid-header__title">General Info</h3>
            </header>
            {generalInfoSectionContent}
          </section>
          <section className="nx-grid-col iq-component-data-col">
            <header className="nx-grid-header">
              <h3 className="nx-h3 nx-grid-header__title">Identification Info</h3>
            </header>
            {identificationInfoSectionContent}
          </section>
        </div>
      </div>
    </section>
  );

  return (
    <div>
      {overviewComponentInformationTile}
      <RiskRemediation directDependency={directDependency} ancestors={ancestors} routeName={routeName} />
    </div>
  );
}

Overview.propTypes = {
  componentInformation: PropTypes.shape({
    componentIdentifier: PropTypes.shape({
      format: PropTypes.string,
    }),
    displayName: PropTypes.shape({
      parts: PropTypes.array,
    }).isRequired,
    createTime: PropTypes.number,
    matchState: PropTypes.string.isRequired,
    identificationSource: PropTypes.string,
    componentCategories: PropTypes.arrayOf(
      PropTypes.shape({
        path: PropTypes.string.isRequired,
      })
    ),
    pathnames: PropTypes.arrayOf(PropTypes.string).isRequired,
    directDependency: PropTypes.bool.isRequired,
  }),
  routeName: PropTypes.string.isRequired,
  ancestors: PropTypes.arrayOf(
    PropTypes.shape({
      hash: PropTypes.string.isRequired,
      derivedComponentName: PropTypes.string.isRequired,
      componentIdentifier: PropTypes.shape({
        format: PropTypes.string.isRequired,
        coordinates: PropTypes.shape({
          artifactId: PropTypes.string.isRequired,
          classifier: PropTypes.string,
          extension: PropTypes.string.isRequired,
          groupId: PropTypes.string.isRequired,
          version: PropTypes.string.isRequired,
        }),
      }),
    })
  ),
};
