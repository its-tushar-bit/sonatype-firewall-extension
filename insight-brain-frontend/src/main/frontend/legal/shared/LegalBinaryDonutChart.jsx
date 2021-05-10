/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { NxBinaryDonutChart, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faCheckCircle } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

export default function LegalBinaryDonutChart(props) {
  const { percent, className } = props;
  const useCheck = percent && percent >= 100;
  const styleName = classnames(className, useCheck ? 'legal-binary-donut-check' : 'legal-binary-donut-chart');
  const chartProps = { className: styleName, percent };

  return useCheck ? (
    <NxFontAwesomeIcon icon={faCheckCircle} className={styleName} />
  ) : (
    <NxBinaryDonutChart {...chartProps} />
  );
}

LegalBinaryDonutChart.propTypes = {
  percent: PropTypes.number.isRequired,
  className: PropTypes.string,
};
