/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

export default function ComponentOverviewTile(props) {
  const {
    licenseLegalMetadata
  } = props;

  let obligationsCount = 0;
  licenseLegalMetadata.forEach(metadata => {
    obligationsCount += metadata.obligations.length;
  });

  return (
    <section id="component-overview-tile" className="nx-tile">
      <div className="nx-tile-content nx-grid-row nx-form-group iq-read-only">
        <dl className="nx-grid-col nx-grid-col--33">
          <dt>Review Status</dt>
          <dd className="iq-read-only-data">Open</dd>
        </dl>
        <dl className="nx-grid-col nx-grid-col--33">
          <dt>Obligations</dt>
          <dd className="iq-read-only-data obligations-count">{obligationsCount}</dd>
        </dl>
        <dl className="nx-grid-col nx-grid-col--33">
          <dt>Licenses</dt>
          <dd className="iq-read-only-data license-names">
            {licenseLegalMetadata.map(license => license.licenseName).join(', ')}
          </dd>
        </dl>
      </div>
    </section>
  );
}

ComponentOverviewTile.propTypes =
  PropTypes.arrayOf(
      PropTypes.shape({
        licenseName: PropTypes.string.isRequired,
        obligations: PropTypes.array.isRequired
      }).isRequired
  ).isRequired;
