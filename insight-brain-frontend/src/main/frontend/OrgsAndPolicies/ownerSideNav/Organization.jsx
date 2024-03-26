/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { memo } from 'react';
import { useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';

import { NxOverflowTooltip, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faSitemap } from '@fortawesome/pro-solid-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectOwnerById } from './ownerSideNavSelectors';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';

const Organization = memo(({ organizationId, displayParentNameInTooltip = false, ...otherProps }) => {
  const uiRouterState = useRouterState();
  const organization = useSelector((state) => selectOwnerById(state, organizationId));
  const parentOrganization = useSelector((state) => selectOwnerById(state, organization?.parentOrganizationId));
  const isSbomManager = useSelector(selectIsSbomManager);
  const organizationUrl = uiRouterState.href(`${isSbomManager ? 'sbomManager.' : ''}management.view.organization`, {
    organizationId,
  });

  const organizationTooltip = (
    <>
      {
        //ParentOrgName is optional and should only be displayed when filtering/searching
        displayParentNameInTooltip && parentOrganization?.name ? <span>Parent: {parentOrganization?.name}</span> : null
      }
      <div className="iq-child-iq-children-counter">
        <span>Sub-Orgs:</span>
        <span>{organization?.subOrgs}</span>
      </div>
      <div className="iq-child-iq-children-counter">
        <span>Total Apps:</span>
        <span>{organization?.totalApps}</span>
      </div>
    </>
  );

  return (
    <a href={organizationUrl} {...otherProps}>
      <NxOverflowTooltip>
        <div className="iq-owner-name">
          <NxFontAwesomeIcon icon={faSitemap} />
          <span>{organization.name}</span>
        </div>
      </NxOverflowTooltip>
      <NxTooltip key={organizationId} title={organizationTooltip} placement="right">
        <div className="iq-children-counter">
          <span>({organization.subOrgs + organization.totalApps})</span>
        </div>
      </NxTooltip>
    </a>
  );
});

Organization.propTypes = {
  organizationId: PropTypes.string,
  displayParentNameInTooltip: PropTypes.bool,
};

export default Organization;
