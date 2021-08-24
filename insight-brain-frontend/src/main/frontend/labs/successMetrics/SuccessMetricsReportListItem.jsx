/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faAngleRight } from '@fortawesome/free-solid-svg-icons';
import { useRouterState } from '../../react/RouterStateContext';

const SuccessMetricsReportListItem = ({ reportId, reportName }) => {
  const uiRouterState = useRouterState();

  return (
    <li className="nx-list__item nx-list__item--link" tabIndex={0}>
      <a
        className="nx-list__link"
        href={uiRouterState.href('labs.successMetricsReport', { successMetricsReportId: reportId })}
      >
        <span className="nx-list__text">{reportName}</span>
        <NxFontAwesomeIcon icon={faAngleRight} className="nx-chevron" />
      </a>
    </li>
  );
};

SuccessMetricsReportListItem.propTypes = {
  reportId: PropTypes.string,
  reportName: PropTypes.string,
};

export default SuccessMetricsReportListItem;
