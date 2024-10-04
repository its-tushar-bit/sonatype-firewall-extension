/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { omit } from 'ramda';
import {
  NxDivider,
  NxFieldset,
  NxFontAwesomeIcon,
  NxH2,
  NxInfoAlert,
  NxLoadWrapper,
  NxP,
  NxRadio,
  NxTable,
  NxTooltip,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { faExclamationCircle, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';

import { actions as policyActions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { selectActionStageTypes, selectActionStagesLoadError } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectIsActionOverrideEnabled,
  selectCurrentPolicyActions,
  selectActionsOverridesForCurrentPolicy,
  selectIsInherited,
  selectOverrideActionsFlag,
  selectShouldShowQuarantineWarning,
  selectIsActionsInheritOverrideEnabled,
  selectIsActionsTableEnabled,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import {
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsRepositoriesRelated } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function PolicyActionsEditor() {
  const dispatch = useDispatch();
  const loadActionStages = () => dispatch(stagesActions.loadActionStages());
  const setOverrideParentActions = () => dispatch(policyActions.setOverrideParentActions());
  const unsetOverrideParentActions = (ownerId) => dispatch(policyActions.unSetOverrideParentActions(ownerId));
  const setActions = (actions) => dispatch(policyActions.setActions(actions));
  const setActionsOverride = (ownerId, actionsOverride) =>
    dispatch(policyActions.setActionsOverride({ ownerId, actionsOverride }));

  const ownerId = useSelector(selectSelectedOwnerId);
  const actionStages = useSelector(selectActionStageTypes);
  const isActionOverrideEnabled = useSelector(selectIsActionOverrideEnabled);
  const actionsOverrides = useSelector(selectActionsOverridesForCurrentPolicy);
  const currentPolicyActions = useSelector(selectCurrentPolicyActions);
  const isInherited = useSelector(selectIsInherited);
  const isEnforcementSupported = useSelector(selectIsEnforcementSupported);
  const isFirewallSupported = useSelector(selectIsFirewallSupported);
  const isOverrideParentActionsEnabled = useSelector(selectOverrideActionsFlag);
  const actionStagesLoadError = useSelector(selectActionStagesLoadError);
  const shouldShowQuarantineWarning = useSelector(selectShouldShowQuarantineWarning);
  const actions = actionsOverrides || currentPolicyActions;
  const tableGridTemplateStyles = {
    gridTemplateColumns: `minmax(90px, 1fr) repeat(${actionStages?.length}, min-content) minmax(48px, min-content) 60px`,
  };
  const isActionsInheritOverrideEnabled = useSelector(selectIsActionsInheritOverrideEnabled);
  const isActionsTableEnabled = useSelector(selectIsActionsTableEnabled);

  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const isDisabled = (stageTypeId) => {
    if (isRepositoriesRelated) {
      if (!isInherited) {
        return stageTypeId !== 'proxy';
      }
      return !isActionsTableEnabled || stageTypeId !== 'proxy';
    }
    const isEnforcementSupportedForStage = isEnforcementSupported || (isFirewallSupported && stageTypeId === 'proxy');
    return !isActionsTableEnabled || !isEnforcementSupportedForStage;
  };

  const ACTIONS = [
    { label: 'No Action', value: null },
    { label: 'Warn', value: 'warn', icon: faExclamationTriangle },
    { label: 'Fail', value: 'fail', icon: faExclamationCircle },
  ];

  const getActionFor = (stageTypeId) => actions[stageTypeId] || null;

  const onOverrideParentActionsChange = (enableOverrides) => {
    if (enableOverrides) {
      setOverrideParentActions();
      setActionsOverride(ownerId, actions ?? {});
    } else {
      unsetOverrideParentActions(ownerId);
    }
  };

  const onActionsChange = (stageTypeId, value) => {
    const updatedActions = value ? { ...actions, [stageTypeId]: value } : omit([stageTypeId], actions);

    if (isActionOverrideEnabled) {
      setActionsOverride(ownerId, updatedActions);
    } else {
      setActions(updatedActions);
    }
  };

  const getRadioTooltipMessage = (stage) =>
    isRepositoriesRelated && stage.stageTypeId !== 'proxy' ? 'Actions are only supported at Proxy stage' : '';

  return (
    <NxLoadWrapper loading={!actionStages} error={actionStagesLoadError} retryHandler={loadActionStages}>
      <div id="policy-edit-actions">
        <NxH2>Actions</NxH2>

        {isInherited && (
          <div id="edit-policy-actions-override">
            <NxP>
              {isActionOverrideEnabled
                ? 'Action overrides have been enabled for this policy. Modifying actions will only affect this level.'
                : 'Action overrides have been disabled for this policy.'}
            </NxP>

            <NxFieldset id="editor-policy-inherit" label="Override Status" isRequired={true}>
              <NxRadio
                id="edit-policy-actions-override-inherit"
                name="overrideParentActionsStatus"
                value={null}
                disabled={!isActionsInheritOverrideEnabled}
                isChecked={!isOverrideParentActionsEnabled}
                onChange={() => onOverrideParentActionsChange(false)}
              >
                Inherit parent actions
              </NxRadio>

              <NxRadio
                id="edit-policy-actions-override-override"
                name="overrideParentActionsStatus"
                value="allowOverrideParentActionsStatus"
                disabled={!isActionsInheritOverrideEnabled}
                isChecked={isOverrideParentActionsEnabled}
                onChange={() => onOverrideParentActionsChange(true)}
              >
                Override parent actions
              </NxRadio>
            </NxFieldset>
          </div>
        )}

        {!isEnforcementSupported && (
          <NxInfoAlert id="actions-disabled-message">
            {isFirewallSupported ? (
              <span>Only Proxy Actions are supported with your Firewall product license.</span>
            ) : (
              <span>Actions are not supported by your product license.</span>
            )}
          </NxInfoAlert>
        )}

        <NxTable
          id="edit-policy-actions-table"
          className="iq-policy-editor-table"
          aria-label="Edit policy actions table"
          style={tableGridTemplateStyles}
        >
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell />
              {actionStages?.map((stage) => (
                <NxTable.Cell key={stage.stageTypeId} className={stage.shortName?.toLowerCase()}>
                  {stage.shortName}
                </NxTable.Cell>
              ))}
              <NxTable.Cell className="placeholder continuous-monitoring" />
              <NxTable.Cell className="placeholder delete-action" />
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body>
            {ACTIONS.map((action) => (
              <NxTable.Row key={`row-${action.value}`}>
                <NxTable.Cell>
                  {action.icon && <NxFontAwesomeIcon icon={action.icon} className={`iq-icon--${action.value}`} />}
                  <span>{action.label}</span>
                </NxTable.Cell>
                {actionStages?.map((stage) => (
                  <NxTable.Cell key={`${action.value}-${stage.stageTypeId}`} className={stage.shortName?.toLowerCase()}>
                    <NxTooltip title={getRadioTooltipMessage(stage)}>
                      <NxRadio
                        name={`action-for-${stage.stageTypeId}`}
                        value={action.value}
                        disabled={isDisabled(stage.stageTypeId)}
                        onChange={() => onActionsChange(stage.stageTypeId, action.value)}
                        isChecked={getActionFor(stage.stageTypeId) === action.value}
                      />
                    </NxTooltip>
                  </NxTable.Cell>
                ))}
                <NxTable.Cell className="placeholder continuous-monitoring" />
                <NxTable.Cell className="placeholder delete-action" />
              </NxTable.Row>
            ))}
          </NxTable.Body>
        </NxTable>
        {shouldShowQuarantineWarning && (
          <NxWarningAlert id="quarantine-warning-message">
            This will quarantine all new components that violate this policy going forward and may cause build failures.
          </NxWarningAlert>
        )}
        <NxDivider />
      </div>
    </NxLoadWrapper>
  );
}
