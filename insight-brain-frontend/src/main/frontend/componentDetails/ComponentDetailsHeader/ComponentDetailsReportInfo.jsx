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

import { formatDate } from '../../util/dateUtils';

export const ComponentDetailsReportInfo = ({
  applicationName,
  organizationName,
  reportTime,
  reportTitle,
  ...props
}) => {
  if (!applicationName && !organizationName && !reportTime && !reportTitle) {
    return null;
  }

  return (
    <div {...props} className={cx('component-details-header__reportinfo', props.className)}>
      {!!organizationName && (
        <NxOverflowTooltip>
          <span className="component-details-header__reportinfo-item nx-truncate-ellipsis">
            <NxFontAwesomeIcon className="component-details-header__reportinfo-icon" icon={faNetworkWired} />
            <span>{organizationName}</span>
          </span>
        </NxOverflowTooltip>
      )}
      {!!applicationName && (
        <NxOverflowTooltip>
          <span className="component-details-header__reportinfo-item nx-truncate-ellipsis">
            <NxFontAwesomeIcon className="component-details-header__reportinfo-icon" icon={faTerminal} />
            <span>{applicationName}</span>
          </span>
        </NxOverflowTooltip>
      )}
      {!!reportTitle && reportTime && (
        <NxOverflowTooltip>
          <span className="component-details-header__reportinfo-item nx-truncate-ellipsis">
            <NxFontAwesomeIcon className="component-details-header__reportinfo-icon" icon={faFile} />
            <span className="visual-testing-ignore">{`${reportTitle} ${formatDate(
              reportTime,
              'YYYY-MM-DD HH:mm:ss'
            )}`}</span>
          </span>
        </NxOverflowTooltip>
      )}
    </div>
  );
};

ComponentDetailsReportInfo.propTypes = {
  className: PropTypes.string,
  applicationName: PropTypes.string,
  organizationName: PropTypes.string,
  reportTime: PropTypes.number,
  reportTitle: PropTypes.string,
};

export default ComponentDetailsReportInfo;
