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
  selectCurrentPolicyViolationGrandfatheringAllowed,
  selectHasEditIqPermission,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';
import { selectIsGrandfatheringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsRepositoriesRelated } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function EditPolicySummary() {
  const dispatch = useDispatch();

  const name = useSelector(selectCurrentPolicyName);

  const threatLevel = useSelector(selectCurrentPolicyThreatLevel);
  const isInherited = useSelector(selectIsInherited);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const readOnly = isRepositoriesRelated ? false : isInherited || !hasEditIqPermission;
  const isGrandfatheringSupported = useSelector(selectIsGrandfatheringSupported);
  const policyViolationGrandfatheringAllowed = useSelector(selectCurrentPolicyViolationGrandfatheringAllowed);

  const setPolicyName = (val) => dispatch(actions.setPolicyName(val));

  const setThreatLevel = (val) => dispatch(actions.setThreatLevel(val));

  const togglePolicyViolationGrandfatheringAllowed = (val) =>
    dispatch(actions.togglePolicyViolationGrandfatheringAllowed(val));

  return (
    <div id="policy-edit-summary">
      <NxH2>Summary</NxH2>
      <NxFormRow>
        <NxFormGroup label="Policy Name" isRequired={true}>
          <NxTextInput
            {...name}
            onChange={setPolicyName}
            validatable={!name.isPristine || !name.value}
            disabled={isRepositoriesRelated ? false : readOnly}
            id="editor-policy-name"
            name="policy"
            autoFocus
          />
        </NxFormGroup>
        <ThreatDropdownSelector
          className="edit-policy-threat-dropdown"
          threatLevel={threatLevel}
          onSelectThreatLevel={setThreatLevel}
          disabled={isRepositoriesRelated ? false : readOnly}
          id="editor-policy-threat-level"
        />
      </NxFormRow>
      {!isGrandfatheringSupported && (
        <NxStatefulInfoAlert id="grandfathering-disabled-message">
          Policy Violation Grandfathering is not supported by your license
        </NxStatefulInfoAlert>
      )}
      {!isRepositoriesRelated && (
        <NxFieldset label="Policy Violation Grandfathering">
          <NxCheckbox
            id="editor-policy-violation-grandfathering"
            onChange={togglePolicyViolationGrandfatheringAllowed}
            isChecked={!!policyViolationGrandfatheringAllowed}
            disabled={isRepositoriesRelated ? false : readOnly || !isGrandfatheringSupported}
          >
            Allow this policy to be grandfathered
          </NxCheckbox>
        </NxFieldset>
      )}
      <NxDivider />
    </div>
  );
}
