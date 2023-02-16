/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { memo } from 'react';
import { useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';

import { NxOverflowTooltip, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faTerminal } from '@fortawesome/pro-solid-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectOwnerById } from './ownerSideNavSelectors';

const Application = memo(({ applicationPublicId, ...otherProps }) => {
  const uiRouterState = useRouterState();
  const application = useSelector((state) => selectOwnerById(state, applicationPublicId));
  const applicationUrl = uiRouterState.href('management.view.application', { applicationPublicId });

  return (
    <a href={applicationUrl} {...otherProps}>
      <NxOverflowTooltip>
        <div className="iq-owner-name">
          <NxFontAwesomeIcon icon={faTerminal} />
          <span>{application?.name}</span>
        </div>
      </NxOverflowTooltip>
    </a>
  );
});

Application.propTypes = {
  applicationPublicId: PropTypes.string,
};

export default Application;
