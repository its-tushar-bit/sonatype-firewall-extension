/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxFormSelect, NxLoadWrapper } from '@sonatype/react-shared-components';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectFirewallPolicyViolations } from 'MainRoot/firewall/firewallSelectors';
import {
  selectFirewallAvailableWaiverScopes,
  selectFirewallLoadingWaiverScopes,
  selectFirewallWaiverScopesError,
  selectFirewallSelectedWaiverScope,
  selectFirewallBulkWaiverSelectedViolations,
} from '../../firewallBulkWaiverSelectors';
import { loadFirewallWaiverScopes } from '../../firewallBulkWaiverActions';
import { loadComponentPolicyViolations } from 'MainRoot/firewall/firewallActions';
import { actions } from '../../firewallBulkWaiverSlice';

function FirewallScopeDropdown({ id }) {
  const dispatch = useDispatch();
  const routerParams = useSelector(selectRouterCurrentParams);
  const { repositoryId } = routerParams;
  const availableScopes = useSelector(selectFirewallAvailableWaiverScopes);
  const loading = useSelector(selectFirewallLoadingWaiverScopes);
  const error = useSelector(selectFirewallWaiverScopesError);
  const selectedScope = useSelector(selectFirewallSelectedWaiverScope);
  const selectedViolations = useSelector(selectFirewallBulkWaiverSelectedViolations);
  const policyViolations = useSelector(selectFirewallPolicyViolations);
  const [policyId, setPolicyId] = useState(null);

  const firstViolation = selectedViolations?.[0];

  useEffect(() => {
    if (firstViolation && repositoryId && firstViolation.pathname) {
      dispatch(loadComponentPolicyViolations(firstViolation.pathname, repositoryId));
    } else if (!firstViolation && selectedViolations) {
      dispatch(actions.setWaiverScopesError('No violations selected'));
    }
  }, [dispatch, firstViolation?.pathname, repositoryId, selectedViolations]);

  useEffect(() => {
    if (policyViolations && policyViolations.length > 0) {
      const extractedPolicyId = policyViolations[0]?.policyId;
      setPolicyId(extractedPolicyId);
    }
  }, [policyViolations]);

  useEffect(() => {
    if (repositoryId && policyId) {
      dispatch(loadFirewallWaiverScopes(repositoryId, policyId));
    }
  }, [dispatch, repositoryId, policyId]);

  const handleScopeChange = (value) => {
    const selectedId = value;
    const scope = availableScopes.find((s) => (s.id || s.ownerId) === selectedId);
    dispatch(actions.setSelectedWaiverScope(scope));
  };

  const getOptionText = (scope) => {
    const type =
      scope.ownerType === 'application'
        ? 'Application'
        : scope.ownerType === 'organization'
        ? 'Organization'
        : 'Repository';
    return `${type}: ${scope.ownerName}`;
  };

  const retryHandler = () => {
    if (repositoryId && policyId) {
      dispatch(loadFirewallWaiverScopes(repositoryId, policyId));
    }
  };

  return (
    <NxLoadWrapper loading={loading} error={error} retryHandler={retryHandler}>
      <NxFormSelect
        aria-label="select scope"
        id={id}
        onChange={handleScopeChange}
        value={selectedScope?.id || selectedScope?.ownerId || ''}
        validatable={true}
      >
        <option value="" disabled>
          Select Scope
        </option>
        {availableScopes &&
          availableScopes.map((scope) => (
            <option key={scope.id || scope.ownerId} value={scope.id || scope.ownerId}>
              {getOptionText(scope)}
            </option>
          ))}
      </NxFormSelect>
    </NxLoadWrapper>
  );
}

FirewallScopeDropdown.propTypes = {
  id: PropTypes.string,
};

export default FirewallScopeDropdown;
