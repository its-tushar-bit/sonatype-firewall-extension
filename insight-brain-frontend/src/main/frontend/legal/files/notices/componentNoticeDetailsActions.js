/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { payloadParamActionCreator } from '../../../util/reduxUtil';
import { loadAvailableScopes, loadComponent, loadComponentByComponentIdentifier } from '../../advancedLegalActions';

export const NOTICE_DETAILS_SELECTED_NOTICE = 'NOTICE_DETAILS_SELECTED_NOTICE';

export const selectedNoticeDetail = payloadParamActionCreator(NOTICE_DETAILS_SELECTED_NOTICE);

export function loadComponentAndNoticeDetails(ownerType, ownerId, hash, noticeIndex, componentIdentifier) {
  return (dispatch, getState) => {
    const component = getState().advancedLegal.component.component;
    if (!component) {
      if (componentIdentifier) {
        return dispatch(
          loadComponentByComponentIdentifier(componentIdentifier, {
            orgOrApp: ownerType,
            ownerId,
          })
        ).then(() => {
          dispatchSelectedNotice(dispatch, getState(), noticeIndex);
        });
      } else {
        dispatch(loadAvailableScopes(ownerType, ownerId));
        return dispatch(loadComponent(ownerType, ownerId, hash)).then(() =>
          dispatchSelectedNotice(dispatch, getState(), noticeIndex)
        );
      }
    } else {
      return dispatchSelectedNotice(dispatch, getState(), noticeIndex);
    }
  };
}

export function refreshNoticeFilesDetails() {
  return (dispatch, getState) => {
    const currentParams = getState().router && getState().router.currentParams;
    return (
      currentParams &&
      currentParams.noticeIndex &&
      dispatchSelectedNotice(dispatch, getState(), currentParams.noticeIndex)
    );
  };
}

function dispatchSelectedNotice(dispatch, state, noticeIndex) {
  const { notice } = extractRoutingParameters(state, noticeIndex);
  dispatch(
    selectedNoticeDetail({
      noticeIndex,
      notice,
    })
  );
}

function extractRoutingParameters(state, requestedNoticeIndex) {
  const advancedLegalState = state.advancedLegal;
  const routerParams = state.router.currentParams;
  const noticeIndex = requestedNoticeIndex || state.componentNoticeDetails.noticeIndex;
  const component = advancedLegalState.component.component;
  const notice = component?.licenseLegalData.noticeFiles[noticeIndex];

  const ownerType = routerParams.ownerType;
  const ownerPublicId = routerParams.ownerId;
  return { notice, component, ownerType, ownerPublicId };
}
