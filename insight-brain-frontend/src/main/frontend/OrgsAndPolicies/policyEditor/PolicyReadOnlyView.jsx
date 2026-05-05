/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import {
  NxReadOnly,
  NxTile,
  NxH2,
  NxList,
  NxTable,
  NxDivider,
  NxFontAwesomeIcon,
  NxThreatIndicator,
  categoryByPolicyThreatLevel,
} from '@sonatype/react-shared-components';
import { faExclamationCircle, faExclamationTriangle, faCheckCircle } from '@fortawesome/pro-solid-svg-icons';
import { capitalize } from 'MainRoot/util/jsUtil';
import { isNil } from 'ramda';
import { conditionString } from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';
import { selectConditionTypesMap } from 'MainRoot/OrgsAndPolicies/constraintSelectors';
import EnterprisePopover from 'MainRoot/shared/enterpriseTier/EnterprisePopover';
import './_PolicyReadOnlyView.scss';

export default function PolicyReadOnlyView({
  policy,
  showActionsAndNotifications = true,
  showSummary = true,
  showInheritance = true,
  showConstraints = true,
  showConstraintsPopover = false,
  onSwitchToCustomMode,
}) {
  const conditionTypesMap = useSelector(selectConditionTypesMap);

  if (!policy) {
    return null;
  }

  const renderSummarySection = () => {
    const policyName = typeof policy.name === 'string' ? policy.name : policy.name?.value || '--';
    const threatLevel = policy.threatLevel;
    const hasThreatLevel =
      threatLevel !== undefined && threatLevel !== null && categoryByPolicyThreatLevel[threatLevel] !== undefined;
    const legacyViolations = policy.legacyViolations;
    const isLegacyEnabled = legacyViolations !== undefined && legacyViolations !== null && legacyViolations;

    return (
      <div className="iq-policy-readonly-view__section">
        <NxH2>Summary</NxH2>
        <div className="iq-policy-readonly-view__summary-row">
          <NxReadOnly>
            <NxReadOnly.Label>Policy Name</NxReadOnly.Label>
            <NxReadOnly.Data data-testid="policy-name">{policyName}</NxReadOnly.Data>
          </NxReadOnly>
          {hasThreatLevel && (
            <NxReadOnly>
              <NxReadOnly.Label>Threat Level</NxReadOnly.Label>
              <NxReadOnly.Data className="iq-policy-readonly-view__threat-level" data-testid="policy-threat-level">
                <NxThreatIndicator policyThreatLevel={threatLevel} />
                <span>
                  {threatLevel} - {capitalize(categoryByPolicyThreatLevel[threatLevel])}
                </span>
              </NxReadOnly.Data>
            </NxReadOnly>
          )}
        </div>
        <NxReadOnly>
          <NxReadOnly.Label>Legacy Violations</NxReadOnly.Label>
          <NxReadOnly.Data
            className="iq-policy-readonly-view__legacy-violations"
            data-testid="policy-legacy-violations"
          >
            {isLegacyEnabled
              ? 'Violations of this policy can be granted legacy status. Eligible violations will be reported but will not trigger actions.'
              : 'Violations of this policy cannot be granted legacy status.'}
          </NxReadOnly.Data>
        </NxReadOnly>
      </div>
    );
  };

  const renderInheritanceSection = () => {
    const hasInheritanceData =
      policy.inherited ||
      policy.policyActionsOverrideAllowed !== undefined ||
      policy.policyNotificationsOverrideAllowed !== undefined;

    return (
      <div className="iq-policy-readonly-view__section">
        <NxH2>Inheritance</NxH2>
        <div className="iq-policy-readonly-view__inheritance" data-testid="policy-inheritance">
          {policy.inherited ? (
            <div className="iq-policy-readonly-view__inheritance-text">
              <div>
                <strong>This Policy is Inherited From:</strong>
              </div>
              <div>{policy.inherited.ownerName || '--'}</div>
            </div>
          ) : (
            <div className="iq-policy-readonly-view__inheritance-text">
              <div>
                <strong>This Policy Inherits to:</strong>
              </div>
              <div>All Applications and Repositories</div>
            </div>
          )}

          {(policy.policyActionsOverrideAllowed !== undefined ||
            policy.policyNotificationsOverrideAllowed !== undefined) && (
            <div className="iq-policy-readonly-view__inheritance-overrides">
              <strong>Inheritance Overrides:</strong>
              <ul className="iq-policy-readonly-view__overrides-list">
                {policy.policyActionsOverrideAllowed !== undefined && (
                  <li data-testid="policy-actions-override-allowed">
                    Actions Override: {policy.policyActionsOverrideAllowed ? 'Allowed' : 'Not Allowed'}
                  </li>
                )}
                {policy.policyNotificationsOverrideAllowed !== undefined && (
                  <li data-testid="policy-notifications-override-allowed">
                    Notifications Override: {policy.policyNotificationsOverrideAllowed ? 'Allowed' : 'Not Allowed'}
                  </li>
                )}
              </ul>
            </div>
          )}
        </div>
      </div>
    );
  };

  const renderConstraintsSection = () => {
    const constraints = policy.constraints || [];

    const constraintsContent = (
      <div className="iq-policy-readonly-view__section">
        <NxH2>Constraints</NxH2>
        {constraints.length === 0 ? (
          <div data-testid="policy-constraints-empty">No constraints defined</div>
        ) : (
          <NxList data-testid="policy-constraints">
            {constraints.map((constraint, index) => {
              const constraintName =
                typeof constraint.name === 'string'
                  ? constraint.name
                  : constraint.name?.value || `Constraint ${index + 1}`;
              const constraintSubheader =
                constraint.conditions && constraint.conditions.length > 1
                  ? `${constraint.operator === 'OR' ? 'any' : 'all'} of the following are true:`
                  : 'the following is true:';

              return (
                <NxList.Item key={constraint.id || index} data-testid="readonly-constraint">
                  <NxList.Text>{constraintName}</NxList.Text>
                  <NxList.Subtext>
                    is in violation if {constraintSubheader}
                    <NxList bulleted>
                      {!isNil(conditionTypesMap) &&
                        (constraint.conditions || []).map((condition) => (
                          <NxList.Item key={condition.conditionTypeId}>
                            {conditionString(condition, conditionTypesMap)}
                          </NxList.Item>
                        ))}
                    </NxList>
                  </NxList.Subtext>
                </NxList.Item>
              );
            })}
          </NxList>
        )}
      </div>
    );

    if (showConstraintsPopover) {
      return (
        <EnterprisePopover
          featureId="constraints"
          highlightText="Customize policy constraints"
          content="to match your organization's risk tolerance and enforce standards more precisely."
          linkText="Go to enterprise custom policy"
          onLinkClick={onSwitchToCustomMode}
        >
          {constraintsContent}
        </EnterprisePopover>
      );
    }

    return constraintsContent;
  };

  const renderNotificationsSection = () => {
    const notifications = policy.notifications;

    const renderRecipientBadge = (recipient) => {
      const recipientName =
        recipient.displayName || recipient.emailAddress || recipient.roleName || 'Unknown recipient';
      const isEmail = recipient.emailAddress || (recipientName && recipientName.includes('@'));

      if (isEmail) {
        return <span className="iq-policy-readonly-view__notification-badge">{recipientName}</span>;
      }
      return <span>{recipientName}</span>;
    };

    const formatNotificationText = (recipient) => {
      const stages = recipient.stageIds && recipient.stageIds.length > 0 ? recipient.stageIds.join(', ') : 'all stages';

      return (
        <>
          {renderRecipientBadge(recipient)} will be notified every time this policy is triggered at {stages}
        </>
      );
    };

    return (
      <div className="iq-policy-readonly-view__section">
        <NxH2>Notifications</NxH2>
        {!notifications ||
        ((!notifications.userNotifications || notifications.userNotifications.length === 0) &&
          !notifications.recipients) ? (
          <div data-testid="policy-notifications-empty">No notifications configured</div>
        ) : (
          <div className="iq-policy-readonly-view__notifications" data-testid="policy-notifications">
            <ul className="iq-policy-readonly-view__notifications-list">
              {(notifications.userNotifications || notifications.recipients || []).map((recipient, index) => (
                <li key={index} className="iq-policy-readonly-view__notification-item">
                  {formatNotificationText(recipient)}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    );
  };

  const ACTIONS = [
    { label: 'No Action', value: null },
    { label: 'Warn', value: 'warn', icon: faExclamationTriangle },
    { label: 'Fail', value: 'fail', icon: faExclamationCircle },
  ];

  const renderActionsSection = () => {
    const actions = policy.actions || {};
    const stages = Object.keys(actions).sort();

    const getActionDisplay = (actionValue) => {
      const action = ACTIONS.find((a) => a.value === actionValue);
      if (!action) return 'No Action';
      return (
        <div className="iq-policy-readonly-view__action-display">
          {action.icon && (
            <NxFontAwesomeIcon
              icon={action.icon}
              className={`iq-policy-readonly-view__action-icon iq-policy-readonly-view__action-icon--${action.value}`}
            />
          )}
          <span>{action.label}</span>
        </div>
      );
    };

    return (
      <div className="iq-policy-readonly-view__section">
        <NxH2>Actions</NxH2>
        {stages.length === 0 ? (
          <div data-testid="policy-actions-empty">No actions configured</div>
        ) : (
          <NxTable className="iq-policy-readonly-view__actions-table" data-testid="policy-actions">
            <NxTable.Head>
              <NxTable.Row>
                {stages.map((stage) => (
                  <NxTable.Cell key={stage} className="iq-policy-readonly-view__action-stage">
                    {capitalize(stage)}
                  </NxTable.Cell>
                ))}
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body>
              <NxTable.Row>
                {stages.map((stage) => (
                  <NxTable.Cell key={stage} className="iq-policy-readonly-view__action-cell">
                    {getActionDisplay(actions[stage])}
                  </NxTable.Cell>
                ))}
              </NxTable.Row>
            </NxTable.Body>
          </NxTable>
        )}
      </div>
    );
  };

  return (
    <div className="iq-policy-readonly-view" data-testid="policy-readonly-view">
      {showSummary && renderSummarySection()}
      {showInheritance && renderInheritanceSection()}
      {showConstraints && renderConstraintsSection()}
      {showActionsAndNotifications && renderActionsSection()}
      {showActionsAndNotifications && renderNotificationsSection()}
    </div>
  );
}

PolicyReadOnlyView.propTypes = {
  policy: PropTypes.shape({
    name: PropTypes.oneOfType([PropTypes.string, PropTypes.shape({ value: PropTypes.string })]),
    threatLevel: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    stage: PropTypes.string,
    inherited: PropTypes.shape({
      ownerName: PropTypes.string,
      ownerType: PropTypes.string,
    }),
    policyActionsOverrideAllowed: PropTypes.bool,
    policyNotificationsOverrideAllowed: PropTypes.bool,
    legacyViolations: PropTypes.bool,
    constraints: PropTypes.arrayOf(
      PropTypes.shape({
        id: PropTypes.string,
        name: PropTypes.oneOfType([PropTypes.string, PropTypes.shape({ value: PropTypes.string })]),
        operator: PropTypes.string,
        conditions: PropTypes.arrayOf(
          PropTypes.shape({
            conditionTypeId: PropTypes.string,
            operator: PropTypes.string,
            value: PropTypes.any,
          })
        ),
      })
    ),
    notifications: PropTypes.shape({
      userNotifications: PropTypes.array,
      recipients: PropTypes.array,
    }),
    actions: PropTypes.object,
  }),
  showActionsAndNotifications: PropTypes.bool,
  showSummary: PropTypes.bool,
  showInheritance: PropTypes.bool,
  showConstraints: PropTypes.bool,
  showConstraintsPopover: PropTypes.bool,
  onSwitchToCustomMode: PropTypes.func,
};
