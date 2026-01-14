/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';

export default function React2ShellSummaryTiles({ metrics }) {
  if (!metrics) {
    return null;
  }

  const tiles = [
    {
      title: 'Affected Applications',
      value: metrics.affectedApplications,
    },
    {
      title: 'Affected Components',
      value: metrics.affectedComponents,
    },
    {
      title: 'Violating Components',
      value: metrics.violatingComponents,
    },
    {
      title: 'Active Waivers',
      value: metrics.activeWaivers,
    },
  ];

  return (
    <div className="iq-react2shell-summary-tiles">
      {tiles.map((tile, index) => (
        <div key={index} className="iq-react2shell-summary-tiles__tile">
          <div className="iq-react2shell-summary-tiles__value">{tile.value}</div>
          <div className="iq-react2shell-summary-tiles__title">{tile.title}</div>
        </div>
      ))}
    </div>
  );
}

React2ShellSummaryTiles.propTypes = {
  metrics: PropTypes.shape({
    affectedApplications: PropTypes.number.isRequired,
    affectedComponents: PropTypes.number.isRequired,
    violatingComponents: PropTypes.number.isRequired,
    activeWaivers: PropTypes.number.isRequired,
  }),
};
