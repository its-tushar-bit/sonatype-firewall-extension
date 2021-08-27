/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { componentPropType } from '../../advancedLegalPropTypes';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';

export default function NoticeDetailsList(props) {
  const { component, noticeIndex, ownerType, ownerId, hash, stageTypeId, loading, error, $state } = props;

  const listLinkClass = (index) => classnames('nx-list__link', { selected: index === parseInt(noticeIndex) });

  const attributionStatus = (item) =>
    item.status === 'enabled' ? 'Included in attribution report' : 'Excluded from the report';

  const noticeTargetState = () =>
    stageTypeId ? 'legal.stageTypeComponentNoticeDetails.noticeDetails' : 'legal.componentNoticeDetails.noticeDetails';

  const noticeRef = React.useRef(new Map());

  const selectedNotice = parseInt(noticeIndex);

  const listItems =
    component && component.licenseLegalData
      ? component.licenseLegalData.noticeFiles.map((item, index) => (
          <li key={index} className="nx-list__item nx-list__item--link">
            <a
              href={$state.href(noticeTargetState(), {
                ownerType,
                ownerId,
                hash,
                noticeIndex: index,
              })}
              className={listLinkClass(index)}
            >
              <div
                className="nx-list__text nx-truncate-ellipsis"
                ref={(element) => noticeRef.current.set(index, element)}
              >
                {item.relPath ? item.relPath : 'Custom Notice'}
              </div>
              <div className="nx-list__subtext">{attributionStatus(item)}</div>
            </a>
          </li>
        ))
      : '';

  useEffect(() => {
    if (noticeRef && noticeRef.current && noticeRef.current.get(selectedNotice)) {
      noticeRef.current.get(selectedNotice).scrollIntoView({
        behavior: 'auto',
        block: 'center',
      });
    }
  });

  return loading || error ? null : (
    <aside className="nx-scrollable nx-viewport-sized__scrollable">
      <ul className="nx-list nx-list--clickable">{listItems}</ul>
    </aside>
  );
}

NoticeDetailsList.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  component: componentPropType,
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  hash: PropTypes.string,
  stageTypeId: PropTypes.string,
  $state: PropTypes.object.isRequired,
  noticeIndex: PropTypes.string.isRequired,
};
