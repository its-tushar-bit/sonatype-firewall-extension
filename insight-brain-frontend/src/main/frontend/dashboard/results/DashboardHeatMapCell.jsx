/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTableCell } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function DashboardHeatMapCell(props) {
  const { children, threatScore, colorStyler, chevron } = props,
    score = threatScore !== undefined ? threatScore : children;

  const textColorClass = chevron
    ? ''
    : score === 0
    ? 'grey-text'
    : colorStyler && colorStyler.isWhiteText(score)
    ? 'white-text'
    : '';

  const backgroundColor = colorStyler ? colorStyler.getColor(score) : '';

  return (
    <NxTableCell
      className={`nx-cell--num iq-cell--heatmap ${textColorClass}`}
      style={{ backgroundColor: backgroundColor }}
      chevron={chevron}
    >
      {chevron ? null : score}
    </NxTableCell>
  );
}

export const heatMapColorStylerPropTypes = PropTypes.shape({
  isWhiteText: PropTypes.func.isRequired,
  getColor: PropTypes.func.isRequired,
});

DashboardHeatMapCell.propTypes = {
  children: PropTypes.number,
  threatScore: PropTypes.number,
  colorStyler: heatMapColorStylerPropTypes,
  chevron: PropTypes.bool,
};
