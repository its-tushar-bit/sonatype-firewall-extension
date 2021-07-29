/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { join } from 'ramda';

import { formatTimeAgoUpToDay } from '../../util/dateUtils';

export default function Overview({ componentInformation }) {
  const {
    componentIdentifier,
    displayName,
    createTime,
    matchState,
    identificationSource,
    componentCategories = [],
    pathnames,
  } = componentInformation;

  const isUnknown = !matchState || matchState === 'unknown';
  const format = isUnknown ? '' : componentIdentifier.format;
  const catalogedDateAgo = createTime ? formatTimeAgoUpToDay(createTime) : '';
  const joinedComponentCategories = join(
    ',',
    componentCategories.map((category) => category.path)
  );

  const generalInfoSectionContent = (
    <dl className="iq-inline-definition-list">
      <div className="iq-inline-definition-list__item">
        <dt>Type:</dt>
        <dd>{format}</dd>
      </div>

      {displayName.parts.map(
        ({ field, value }) =>
          field && (
            <div className="iq-inline-definition-list__item" key={`${field}${value}`}>
              <dt>{`${field}:`}</dt>
              <dd>{value}</dd>
            </div>
          )
      )}
    </dl>
  );

  const identificationInfoSectionContent = (
    <dl className="iq-inline-definition-list">
      <div className="iq-inline-definition-list__item">
        <dt>Cataloged:</dt>
        <dd>{catalogedDateAgo}</dd>
      </div>
      <div className="iq-inline-definition-list__item">
        <dt>Match State:</dt>
        <dd>{matchState}</dd>
      </div>
      <div className="iq-inline-definition-list__item">
        <dt>Identification Source:</dt>
        <dd>{identificationSource}</dd>
      </div>
      <div className="iq-inline-definition-list__item">
        <dt>Category:</dt>
        <dd>{joinedComponentCategories}</dd>
      </div>
    </dl>
  );

  const occurrencesSectionContent = (
    <dl className="iq-inline-definition-list">
      <div className="iq-inline-definition-list__item">
        <dt>File Matches:</dt>
        <dd>{pathnames.length}</dd>
      </div>
    </dl>
  );

  return (
    <section className="nx-tile iq-component-information-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Component Information</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <div className="nx-grid-row">
          <section className="nx-grid-col nx-grid-col--33">
            <header className="nx-grid-header">
              <h3 className="nx-h3 nx-grid-header__title">General Info</h3>
            </header>
            {generalInfoSectionContent}
          </section>
          <section className="nx-grid-col nx-grid-col--33">
            <header className="nx-grid-header">
              <h3 className="nx-h3 nx-grid-header__title">Identification Info</h3>
            </header>
            {identificationInfoSectionContent}
          </section>
          <section className="nx-grid-col nx-grid-col--33">
            <header className="nx-grid-header">
              <h3 className="nx-h3 nx-grid-header__title">Occurrences</h3>
            </header>
            {occurrencesSectionContent}
          </section>
        </div>
      </div>
    </section>
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
  }),
};
