/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { faCube, faSitemap, faTerminal } from '@fortawesome/free-solid-svg-icons';
import { NxOverflowTooltip, NxFontAwesomeIcon } from '@sonatype/react-shared-components';

import { getComponentName } from '../util/componentNameUtils';

export default function ViolationDetailsSubtitle(props) {
  return (
    <div className="nx-tile-header__subtitle">
      <NxOverflowTooltip>
        <span className="iq-violation-details__subtitle-part nx-truncate-ellipsis">
          <span className="iq-inverse-icon-wrapper">
            <NxFontAwesomeIcon icon={faSitemap} inverse />
          </span>
          <span>{props.organizationName}</span>
        </span>
      </NxOverflowTooltip>
      <NxOverflowTooltip>
        <span className="iq-violation-details__subtitle-part nx-truncate-ellipsis">
          <span className="iq-inverse-icon-wrapper">
            <NxFontAwesomeIcon icon={faTerminal} inverse />
          </span>
          <span>{props.applicationName}</span>
        </span>
      </NxOverflowTooltip>
      <NxOverflowTooltip>
        <span className="iq-violation-details__subtitle-part nx-truncate-ellipsis">
          <NxFontAwesomeIcon icon={faCube} />
          <span>{getComponentName(props)}</span>
        </span>
      </NxOverflowTooltip>
    </div>
  );
}

ViolationDetailsSubtitle.propTypes = {
  organizationName: PropTypes.string.isRequired,
  applicationName: PropTypes.string.isRequired,
  displayName: PropTypes.object,
  filename: PropTypes.string,
  filenames: PropTypes.arrayOf(PropTypes.string),
};
