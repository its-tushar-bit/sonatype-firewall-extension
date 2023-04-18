/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { isEmpty } from 'ramda';
import {
  NxStatefulForm,
  NxH1,
  NxH3,
  NxTile,
  NxTextInput,
  NxFormGroup,
  NxList,
  NxButton,
  NxButtonBar,
  NxFontAwesomeIcon,
  NxFormSelect,
  NxPageTitle,
  NxFormRow,
} from '@sonatype/react-shared-components';
import { faTrashAlt, faPlus } from '@fortawesome/free-solid-svg-icons';
import ProprietaryOtherConfigsComponent from './ProprietaryOtherConfigsComponent';
import { actions, matcherTypes } from '../proprietarySlice';

import {
  selectLoadError,
  selectSubmitError,
  selectIsDirty,
  selectIsLoading,
  selectLocalMatchers,
  selectProprietarySubmitMaskState,
  selectMatcherType,
  selectMatcherValue,
  selectProprietaryOtherConfigs,
} from '../proprietarySelectors';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

export default function ProprietaryComponentConfiguration() {
  const dispatch = useDispatch();
  const loadError = useSelector(selectLoadError);
  const submitError = useSelector(selectSubmitError);
  const loading = useSelector(selectIsLoading);
  const isDirty = useSelector(selectIsDirty);
  const submitMaskState = useSelector(selectProprietarySubmitMaskState);
  const matcherType = useSelector(selectMatcherType);
  const localMatchers = useSelector(selectLocalMatchers);
  const matcherValue = useSelector(selectMatcherValue);
  const proprietaryOtherConfigs = useSelector(selectProprietaryOtherConfigs);

  const saveProprietaryConfig = () => dispatch(actions.saveProprietaryConfig());
  const doLoad = () => dispatch(actions.loadProprietaryConfig());

  useEffect(() => {
    doLoad();
  }, []);

  const onChangeMatcher = (evt) => {
    const { selectedIndex, options } = evt.currentTarget;
    dispatch(actions.setMatcherType(options[selectedIndex].value));
  };

  const onChange = (val) => dispatch(actions.setMatcherValue(val));

  const addMatcher = () => {
    dispatch(
      actions.addMatcher({
        type: matcherType,
        matcher: matcherValue.value,
      })
    );
    dispatch(actions.resetMatcher());
  };

  const removeMatcher = (type, matcher) => {
    dispatch(
      actions.removeMatcher({
        type,
        matcher,
      })
    );
  };

  return (
    <>
      <NxPageTitle>
        <NxH1>Proprietary Component Configuration</NxH1>

        <NxPageTitle.Description>
          These entries are provided to Sonatype IQ scanners and are used to help identify internal components. If a
          specified package or file is contained within a component, it will be considered proprietary.
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxTile>
        <NxTile.Content>
          <NxH3 id="local-proprietary-component-matchers-title">Local</NxH3>
          <NxList
            emptyMessage="No matchers configured"
            className="local-proprietary-component-matchers"
            aria-labelledby="local-proprietary-component-matchers-title"
          >
            {localMatchers.map(({ type, matcher }) => (
              <NxList.Item key={`${type}${matcher}`}>
                <NxList.Text>{matcher}</NxList.Text>
                <NxList.Subtext>{type === matcherTypes.REGEX ? 'RegEx' : type}</NxList.Subtext>
                <NxList.Actions>
                  <NxButton title="Delete" variant="icon-only" onClick={() => removeMatcher(type, matcher)}>
                    <NxFontAwesomeIcon icon={faTrashAlt} />
                  </NxButton>
                </NxList.Actions>
              </NxList.Item>
            ))}
          </NxList>
          <NxStatefulForm
            onSubmit={saveProprietaryConfig}
            submitBtnText="Update"
            submitMaskMessage="Saving…"
            submitMaskState={submitMaskState}
            doLoad={doLoad}
            loadError={loadError}
            loading={loading}
            validationErrors={!isDirty ? MSG_NO_CHANGES_TO_SAVE : undefined}
            submitError={submitError}
          >
            <NxFormRow>
              <NxFormGroup label="Value" isRequired>
                <NxTextInput onChange={onChange} {...matcherValue} validatable />
              </NxFormGroup>

              <NxFormGroup label="Matcher Type" isRequired>
                <NxFormSelect onChange={onChangeMatcher} value={matcherType}>
                  <option value={matcherTypes.PACKAGE}>{matcherTypes.PACKAGE}</option>
                  <option value={matcherTypes.REGEX}>{matcherTypes.REGEX}</option>
                </NxFormSelect>
              </NxFormGroup>

              <NxButtonBar>
                <NxButton
                  type="button"
                  onClick={addMatcher}
                  variant="tertiary"
                  disabled={matcherValue.value === '' || matcherValue.validationErrors !== null}
                >
                  <NxFontAwesomeIcon icon={faPlus} />
                  <span>Add</span>
                </NxButton>
              </NxButtonBar>
            </NxFormRow>

            {!isEmpty(proprietaryOtherConfigs) &&
              proprietaryOtherConfigs.map((innerEl) => (
                <ProprietaryOtherConfigsComponent key={innerEl.ownerId} innerEl={innerEl} />
              ))}
          </NxStatefulForm>
        </NxTile.Content>
      </NxTile>
    </>
  );
}
