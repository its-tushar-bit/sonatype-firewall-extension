/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import { faFile, faNetworkWired, faTerminal } from '@fortawesome/free-solid-svg-icons';
import { NxOverflowTooltip, NxFontAwesomeIcon } from '@sonatype/react-shared-components';

import { formatDate } from '../../../util/dateUtils';

const ComponentDetailsSbomInfo = ({ applicationName, organizationName, sbomCreationTime, className, ...restProps }) => {
  return (
    <div {...restProps} className={cx('component-details-header__reportinfo', className)}>
      {organizationName && (
        <NxOverflowTooltip>
          <span className="component-details-header__reportinfo-item nx-truncate-ellipsis">
            <NxFontAwesomeIcon className="component-details-header__reportinfo-icon" icon={faNetworkWired} />
            <span>{organizationName}</span>
          </span>
        </NxOverflowTooltip>
      )}
      {applicationName && (
        <NxOverflowTooltip>
          <span className="component-details-header__reportinfo-item nx-truncate-ellipsis">
            <NxFontAwesomeIcon className="component-details-header__reportinfo-icon" icon={faTerminal} />
            <span>{applicationName}</span>
          </span>
        </NxOverflowTooltip>
      )}
      {sbomCreationTime && (
        <NxOverflowTooltip>
          <span className="component-details-header__reportinfo-item nx-truncate-ellipsis">
            <NxFontAwesomeIcon className="component-details-header__reportinfo-icon" icon={faFile} />
            <span className="visual-testing-ignore">{`BOM ${formatDate(
              sbomCreationTime,
              'YYYY-MM-DD HH:mm:ss'
            )}`}</span>
          </span>
        </NxOverflowTooltip>
      )}
    </div>
  );
};

ComponentDetailsSbomInfo.propTypes = {
  className: PropTypes.string,
  applicationName: PropTypes.string,
  organizationName: PropTypes.string,
  sbomCreationTime: PropTypes.number,
};

export default ComponentDetailsSbomInfo;
