/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { find, propEq, filter, curryN, isEmpty } from 'ramda';
import { faPlus, faTag } from '@fortawesome/free-solid-svg-icons';
import {
  NxH2,
  NxH3,
  NxTile,
  NxButton,
  NxFontAwesomeIcon,
  NxList,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { angularToRscColorMap } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectApplicableLabels,
  selectLabelsLoading,
  selectLabelsLoadError,
} from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/labelsSlice';
import { selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectSelectedOwner } from '../../orgsAndPoliciesSelectors';

export default function LabelsTile() {
  const dispatch = useDispatch();
  const uiStateRouter = useRouterState();

  const router = useSelector(selectRouterSlice());
  const selectedOwner = useSelector(selectSelectedOwner);

  const loading = useSelector(selectLabelsLoading);
  const loadError = useSelector(selectLabelsLoadError);
  const ownerName = useSelector(selectSelectedOwnerName);
  const applicableLabels = useSelector(selectApplicableLabels);

  const doLoad = () => dispatch(actions.loadApplicableLabels());
  const goToCreateLabel = () => dispatch(actions.goToCreateLabel());

  const editLabelHref = (labelId) => {
    const { to, params } = deriveEditRoute(router, 'label', { labelId });
    return uiStateRouter.href(to, params);
  };

  useEffect(() => {
    doLoad();
  }, [selectedOwner]);

  const renderInherited = (inherited) => {
    return inherited?.map((owner) => {
      if (!isEmpty(owner.labels)) {
        return (
          <NxTile.Subsection key={owner.ownerId}>
            <NxTile.SubsectionHeader>
              <NxH3>Inherited from {owner.ownerName}</NxH3>
            </NxTile.SubsectionHeader>
            <NxList>{owner?.labels?.map(renderListItem(false))}</NxList>
          </NxTile.Subsection>
        );
      }
    });
  };

  const renderListItem = curryN(2, (isLink, label) => {
    const ListItem = isLink ? NxList.LinkItem : NxList.Item;
    const additionalProps = isLink ? { href: editLabelHref(label.id) } : {};
    return (
      <ListItem key={label.id} {...additionalProps}>
        <NxList.Text>
          <NxFontAwesomeIcon
            icon={faTag}
            className={
              angularToRscColorMap[label.color] ? `nx-selectable-color--${angularToRscColorMap[label.color]}` : ''
            }
          />
          <span>{label.label}</span>
        </NxList.Text>
        {label.description && <NxList.Subtext>{label.description}</NxList.Subtext>}
      </ListItem>
    );
  });

  const renderLists = (labels) => {
    const local = find(propEq('inherited', false), labels ?? []);
    const inherited = filter(propEq('inherited', true), labels ?? []);
    return (
      <>
        <NxTile.Subsection>
          <NxTile.SubsectionHeader>
            <NxH3>Local</NxH3>
          </NxTile.SubsectionHeader>
          <NxList emptyMessage="No local component labels defined">{local?.labels.map(renderListItem(true))}</NxList>
        </NxTile.Subsection>
        {renderInherited(inherited)}
      </>
    );
  };

  return (
    <NxTile id="owner-pill-comp-labels">
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.Headings>
            <NxTile.HeaderTitle>
              <NxH2>Component Labels</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderSubtitle>available to {ownerName} policies</NxTile.HeaderSubtitle>
          </NxTile.Headings>
          <NxTile.HeaderActions>
            <NxButton variant="tertiary" id="add-label-button" onClick={goToCreateLabel}>
              <NxFontAwesomeIcon icon={faPlus} />
              <span>Add a Label</span>
            </NxButton>
          </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content>{renderLists(applicableLabels)}</NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
