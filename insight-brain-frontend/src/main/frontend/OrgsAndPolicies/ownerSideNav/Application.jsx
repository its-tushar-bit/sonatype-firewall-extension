/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { memo } from 'react';
import { useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';

import { NxOverflowTooltip, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faTerminal } from '@fortawesome/pro-solid-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectOwnerById } from './ownerSideNavSelectors';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';

const Application = memo(({ applicationPublicId, isFilteredResult, ...otherProps }) => {
  const uiRouterState = useRouterState();
  const application = useSelector((state) => selectOwnerById(state, applicationPublicId));
  const parentOrg = useSelector((state) => selectOwnerById(state, application?.organizationId));
  const isSbomManager = useSelector(selectIsSbomManager);
  const applicationUrl = uiRouterState.href(`${isSbomManager ? 'sbomManager.' : ''}management.view.application`, {
    applicationPublicId,
  });
  const applicationTooltip = (
    <>
      <span>{application?.name}</span>
      {isFilteredResult && <div className="iq-application-tooltip-parent">Parent: {parentOrg?.name}</div>}
    </>
  );

  const TooltipToUse = isFilteredResult ? NxTooltip : NxOverflowTooltip;
  return (
    <a href={applicationUrl} {...otherProps}>
      <TooltipToUse title={applicationTooltip}>
        <div className="iq-owner-name">
          <NxFontAwesomeIcon icon={faTerminal} />
          <span>{application?.name}</span>
        </div>
      </TooltipToUse>
    </a>
  );
});

Application.propTypes = {
  applicationPublicId: PropTypes.string,
  isFilteredResult: PropTypes.bool,
};

export default Application;
