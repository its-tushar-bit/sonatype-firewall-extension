/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';

import {
  NxCheckbox,
  NxDivider,
  NxFieldset,
  NxFormGroup,
  NxFormRow,
  NxH2,
  NxStatefulInfoAlert,
  NxTextInput,
} from '@sonatype/react-shared-components';

import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import {
  selectCurrentPolicyName,
  selectCurrentPolicyThreatLevel,
  selectIsInherited,
  selectCurrentLegacyViolationAllowed,
  selectHasEditIqPermission,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';
import { selectIsLegacyViolationSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsRepositoriesRelated, selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function EditPolicySummary() {
  const dispatch = useDispatch();

  const name = useSelector(selectCurrentPolicyName);

  const threatLevel = useSelector(selectCurrentPolicyThreatLevel);
  const isInherited = useSelector(selectIsInherited);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const isSbomManager = useSelector(selectIsSbomManager);
  const readOnly = isRepositoriesRelated && !isInherited ? false : isInherited || !hasEditIqPermission || isSbomManager;
  const isLegacyViolationSupported = useSelector(selectIsLegacyViolationSupported);
  const legacyViolationAllowed = useSelector(selectCurrentLegacyViolationAllowed);

  const setPolicyName = (val) => dispatch(actions.setPolicyName(val));

  const setThreatLevel = (val) => dispatch(actions.setThreatLevel(val));

  const toggleLegacyViolationAllowed = (val) => dispatch(actions.toggleLegacyViolationAllowed(val));

  return (
    <div id="policy-edit-summary">
      <NxH2>Summary</NxH2>
      <NxFormRow>
        <NxFormGroup label="Policy Name" isRequired={true}>
          <NxTextInput
            {...name}
            onChange={setPolicyName}
            validatable={!name.isPristine || !name.value}
            disabled={isRepositoriesRelated && !isInherited ? false : readOnly}
            id="editor-policy-name"
            name="policy"
            autoFocus
          />
        </NxFormGroup>
        <ThreatDropdownSelector
          className="edit-policy-threat-dropdown"
          threatLevel={threatLevel}
          onSelectThreatLevel={setThreatLevel}
          disabled={isRepositoriesRelated && !isInherited ? false : readOnly}
          id="editor-policy-threat-level"
        />
      </NxFormRow>
      {!isSbomManager && !isLegacyViolationSupported && (
        <NxStatefulInfoAlert id="legacy-violation-disabled-message">
          Legacy Violations are not supported by your license
        </NxStatefulInfoAlert>
      )}
      {!isSbomManager && !isRepositoriesRelated && (
        <NxFieldset
          label="Legacy Violations"
          sublabel="Eligible violations will be reported but will not trigger actions"
        >
          <NxCheckbox
            id="editor-legacy-violation-checkbox"
            onChange={toggleLegacyViolationAllowed}
            isChecked={!!legacyViolationAllowed}
            disabled={isRepositoriesRelated ? false : readOnly || !isLegacyViolationSupported}
          >
            Allow violations of this policy to be granted legacy status
          </NxCheckbox>
        </NxFieldset>
      )}
      <NxDivider />
    </div>
  );
}
