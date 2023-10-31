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
  NxCollapsibleItems,
} from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { angularToRscColorMap } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectEntityId, selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectApplicableLabels,
  selectLabelsLoading,
  selectLabelsLoadError,
  selectInheritedLabelsOpen,
} from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/labelsSlice';
import { selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectHasEditIqPermission } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';

export default function LabelsTile() {
  const dispatch = useDispatch();
  const uiStateRouter = useRouterState();

  const router = useSelector(selectRouterSlice());

  const loading = useSelector(selectLabelsLoading);
  const loadError = useSelector(selectLabelsLoadError);
  const ownerName = useSelector(selectSelectedOwnerName);
  const applicableLabels = useSelector(selectApplicableLabels);
  const inheritedLabelsOpen = useSelector(selectInheritedLabelsOpen);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);
  const entityId = useSelector(selectEntityId);

  const doLoad = () => dispatch(actions.loadApplicableLabels());
  const goToCreateLabel = () => dispatch(actions.goToCreateLabel());

  const editLabelHref = (labelId) => {
    const { to, params } = deriveEditRoute(router, 'label', { labelId });
    return uiStateRouter.href(to, params);
  };

  const toggleInheritedLabelsOpen = (ownerId) => dispatch(actions.toggleInheritedLabelsOpen(ownerId));

  useEffect(() => {
    doLoad();
  }, [entityId]);

  const renderInherited = (inherited) => {
    return inherited?.map((owner) => {
      if (!isEmpty(owner.labels)) {
        return (
          <NxTile.Subsection key={owner.ownerId}>
            <NxCollapsibleItems
              onToggleCollapse={() => toggleInheritedLabelsOpen(owner.ownerId)}
              isOpen={inheritedLabelsOpen[owner.ownerId]}
              triggerContent={
                <>
                  <h3 className="nx-h3 component-labels-header">Inherited from {owner.ownerName}</h3>
                  {!inheritedLabelsOpen[owner.ownerId] ? renderLabelIcons(owner?.labels) : null}
                </>
              }
            >
              <dl id={'component-labels-for-' + owner.ownerId}>{owner?.labels?.map(renderCollapsibleListItem)}</dl>
            </NxCollapsibleItems>
          </NxTile.Subsection>
        );
      }
    });
  };

  const renderCollapsibleListItem = (label) => {
    return (
      <NxCollapsibleItems.Child key={label.id}>
        <div className="component-labels-element">
          <dt className="component-labels-icon-and-label">
            {renderLabelIcon(label)}
            <span className="component-labels-label">{label.label}</span>
          </dt>
          <dd className="component-labels-description">{label.description}</dd>
        </div>
      </NxCollapsibleItems.Child>
    );
  };

  const renderLabelIcons = (labels) => {
    return <span className="component-labels-header">{labels?.map(renderLabelIcon)}</span>;
  };

  const renderLabelIcon = (label) => {
    return (
      <NxFontAwesomeIcon
        key={label.id}
        icon={faTag}
        className={angularToRscColorMap[label.color] ? `nx-selectable-color--${angularToRscColorMap[label.color]}` : ''}
      />
    );
  };

  const renderListItem = curryN(2, (isLink, label) => {
    const ListItem = isLink ? NxList.LinkItem : NxList.Item;
    const additionalProps = isLink ? { href: editLabelHref(label.id) } : {};
    return (
      <ListItem key={label.id} {...additionalProps}>
        <NxList.Text>
          {renderLabelIcon(label)}
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
            <NxH3>Local to {ownerName}</NxH3>
          </NxTile.SubsectionHeader>
          <NxList emptyMessage="No local component labels defined">
            {local?.labels.map(renderListItem(hasEditIqPermission))}
          </NxList>
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
