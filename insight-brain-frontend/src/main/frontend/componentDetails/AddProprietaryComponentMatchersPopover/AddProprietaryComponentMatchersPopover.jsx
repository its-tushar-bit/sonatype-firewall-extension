/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxCheckbox,
  NxFieldset,
  NxStatefulForm,
  NxFormGroup,
  NxInfoAlert,
  NxTextInput,
  NxTextLink,
} from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';
import { includes, indexOf, remove } from 'ramda';
import React, { useEffect, useState } from 'react';

import IqPopover from 'MainRoot/react/IqPopover/IqPopover';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function AddProprietaryComponentMatchersPopover({
  onClose,
  showPopover,
  pathnames,
  appInfo,
  addProprietaryMatchers,
  submitError,
  submitMaskState,
  data,
  resetSubmitError,
  setComponentMatchersData,
}) {
  const uiRouterState = useRouterState();

  const [isPristine, setIsPristine] = useState(true);

  const isFormValid = !!data.paths?.length || !!data.regex;

  const isPathnameChecked = (pathname) => {
    return includes(pathname, data.paths || []);
  };

  const togglePathname = (pathname) => {
    const index = indexOf(pathname, data.paths ?? []);
    if (index === -1) {
      setComponentMatchersData({ ...data, paths: [...data.paths, pathname] });
    } else {
      setComponentMatchersData({ ...data, paths: remove(index, 1, data.paths) });
    }
  };
  const submitForm = () => {
    const dataToSend = { paths: data.paths };
    if (data?.regex) dataToSend.regex = data.regex;
    addProprietaryMatchers(dataToSend);
  };

  const clearForm = () => {
    setComponentMatchersData({ paths: pathnames, regex: '' });
    setIsPristine(true);
    resetSubmitError();
  };

  const closePopover = () => {
    onClose();
  };

  useEffect(() => {
    if (showPopover) clearForm();
  }, [showPopover]);

  return (
    showPopover && (
      <IqPopover size="large" onClose={closePopover} id="component-details-add-proprietary-component-matchers-popover">
        <IqPopover.Header
          id="add-proprietary-component-matchers-header"
          buttonId="add-proprietary-component-matchers-btn"
          onClose={closePopover}
          headerTitle="Add Proprietary Component Matchers"
        />
        <NxInfoAlert>
          The following matchers will be added to the{' '}
          <NxTextLink
            href={uiRouterState.href('management.edit.application.proprietary-config-policy', {
              applicationPublicId: appInfo.applicationId,
            })}
            newTab
          >
            {appInfo.applicationName} Configuration
          </NxTextLink>{' '}
          (duplicates will be ignored). The new matchers will be in effect for the next application analysis.
        </NxInfoAlert>
        <NxStatefulForm
          onSubmit={submitForm}
          submitError={submitError}
          submitMaskState={submitMaskState}
          onCancel={closePopover}
          submitBtnText="Add"
          validationErrors={isFormValid ? undefined : 'Unable to add: Fields with invalid or missing data.'}
        >
          <NxFieldset label="Matchers" isRequired>
            {pathnames.map((pathname) => (
              <NxCheckbox
                isChecked={isPathnameChecked(pathname)}
                key={pathname}
                onChange={() => togglePathname(pathname)}
              >
                {pathname}
              </NxCheckbox>
            ))}
          </NxFieldset>
          <NxFormGroup
            label="Regular Expression"
            sublabel="The Regular Expression will be added in addition to any matchers checked above"
          >
            <NxTextInput
              value={data.regex}
              isPristine={isPristine}
              id="add-proprietary-component-matchers-regex"
              onChange={(value) => {
                setComponentMatchersData({ ...data, regex: value });
                setIsPristine(false);
              }}
            />
          </NxFormGroup>
        </NxStatefulForm>
      </IqPopover>
    )
  );
}

AddProprietaryComponentMatchersPopover.propTypes = {
  onClose: PropTypes.func.isRequired,
  showPopover: PropTypes.bool,
  pathnames: PropTypes.arrayOf(PropTypes.string),
  appInfo: PropTypes.shape({ applicationName: PropTypes.string, applicationId: PropTypes.string }),
  addProprietaryMatchers: PropTypes.func.isRequired,
  submitError: PropTypes.string,
  submitMaskState: PropTypes.bool,
  resetSubmitError: PropTypes.func.isRequired,
  setComponentMatchersData: PropTypes.func.isRequired,
  data: PropTypes.shape({ paths: PropTypes.arrayOf(PropTypes.string), regex: PropTypes.string }),
};
