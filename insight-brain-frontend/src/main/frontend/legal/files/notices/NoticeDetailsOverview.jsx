/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  availableScopesPropType,
  componentNoticeDetailsPropType,
  componentPropType,
} from '../../advancedLegalPropTypes';
import { timeAgo } from '../../../utilAngular/CommonServices';
import * as PropTypes from 'prop-types';
import { LegalFileOverviewHeader } from '../common/LegalFileOverviewHeader';

export default function NoticeDetailsOverview(props) {
  const { availableScopes, componentNoticeDetails, component, loading, error } = props;

  const licenseLegalData = component && component.licenseLegalData;

  const noticeModification = () => {
    if (licenseLegalData && licenseLegalData.componentNoticesLastUpdatedAt) {
      let age = timeAgo(licenseLegalData.componentNoticesLastUpdatedAt);
      return `${age.age} ${age.qualifier} by ${licenseLegalData.componentNoticesLastUpdatedByUsername}`;
    } else {
      return 'N/A';
    }
  };

  const scopeOwnerId =
    (component && component.licenseLegalData && component.licenseLegalData.componentNoticesScopeOwnerId) ||
    'ROOT_ORGANIZATION_ID';

  return loading || error
    ? null
    : LegalFileOverviewHeader(
        componentNoticeDetails.selectedNotice,
        availableScopes,
        noticeModification(),
        'Notice',
        scopeOwnerId
      );
}

NoticeDetailsOverview.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  availableScopes: availableScopesPropType,
  componentNoticeDetails: componentNoticeDetailsPropType,
  component: componentPropType,
};
