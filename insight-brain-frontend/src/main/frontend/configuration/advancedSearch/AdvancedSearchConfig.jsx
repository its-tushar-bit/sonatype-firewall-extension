/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';
import LoadError from '../../react/LoadError';
import {
  NxPageMain,
  NxTile,
  NxStatefulForm,
  NxButton,
  NxList,
  NxP,
  NxCheckbox,
  NxFieldset,
  NxFontAwesomeIcon,
  NxTextLink,
} from '@sonatype/react-shared-components';

import { faSyncAlt } from '@fortawesome/free-solid-svg-icons';

const authError = `It appears you do not have permission to access this page.
    If you believe this to be incorrect please contact your administrator.`;

export default function AdvancedSearchConfig(props) {
  // Actions
  const { load, save, setIsEnabled, resetForm, reIndex } = props;

  // View State
  const {
    loading,
    submitMaskState,
    submitMaskMessage,
    loadError: loadErrorProp,
    isAuthorized,
    saveError,
    reIndexError,
    pollError,
    isDirty,
  } = props;

  // Form State
  const { isEnabled, lastIndexTime, isFullIndexTriggered } = props;

  const loadError = isAuthorized ? loadErrorProp : authError;

  useEffect(() => {
    load();
  }, []);

  function onSubmit() {
    save();
  }

  function reIndexHandler(evt) {
    evt.preventDefault();
    reIndex();
  }

  const reIndexButtonAndMessage = (
    <>
      {isFullIndexTriggered && (
        <span className="iq-advanced-search-config-reindexing-notification">
          <NxFontAwesomeIcon icon={faSyncAlt} spin={true} />
          <span>Reindexing is in progress. Leaving this page will not interrupt this process.</span>
        </span>
      )}
      <NxButton
        id="advanced-search-config-re-index-button"
        onClick={reIndexHandler}
        disabled={!isEnabled || isFullIndexTriggered}
      >
        Re-Index
      </NxButton>
      <NxButton id="advanced-search-config-cancel" onClick={resetForm} disabled={!isDirty}>
        Cancel
      </NxButton>
    </>
  );

  return (
    <NxPageMain id="advanced-search-config-page-container">
      <LoadWrapper loading={loading} error={loadError} retryHandler={load}>
        <NxTile id="advanced-search-config" className="nx-tile iq-advanced-search-config-tile">
          <NxStatefulForm
            submitBtnText="Save"
            onSubmit={onSubmit}
            submitError={saveError ? 'Error' : null}
            submitErrorTitleMessage="An error occurred while saving the configuration."
            submitMaskState={submitMaskState}
            submitMaskMessage={submitMaskMessage}
            additionalFooterBtns={reIndexButtonAndMessage}
            validationErrors={!isDirty ? 'There are no changes to save. Please update the checkbox.' : null}
          >
            <NxTile.Header>
              <NxTile.HeaderTitle>
                <h2 className="nx-h2">Advanced Search Configuration</h2>
              </NxTile.HeaderTitle>
            </NxTile.Header>
            <NxTile.Content>
              <NxP>
                Advanced Search gives you robust search options to help you find exactly what you are looking for.
                Search terms give you the ability to scope your search to specific types of information relating to the
                following categories:
              </NxP>
              <NxList bulleted>
                <NxList.Item>
                  <NxList.Text>Organizations</NxList.Text>
                </NxList.Item>
                <NxList.Item>
                  <NxList.Text>Applications</NxList.Text>
                </NxList.Item>
                <NxList.Item>
                  <NxList.Text>Application Categories</NxList.Text>
                </NxList.Item>
                <NxList.Item>
                  <NxList.Text>Component Labels</NxList.Text>
                </NxList.Item>
                <NxList.Item>
                  <NxList.Text>Policies</NxList.Text>
                </NxList.Item>
                <NxList.Item>
                  <NxList.Text>Security Vulnerabilities</NxList.Text>
                </NxList.Item>
              </NxList>
              <NxP>
                You can combine multiple search terms to craft an even more targeted search. For more information on how
                to use this feature,{' '}
                <NxTextLink external href="https://links.sonatype.com/products/nxiq/doc/advanced-search">
                  check out the documentation
                </NxTextLink>
                .
              </NxP>
              <NxFieldset label="Advanced Search Status">
                <NxCheckbox
                  id="advanced-search-config-is-enabled-checkbox"
                  isChecked={isEnabled}
                  onChange={setIsEnabled}
                >
                  Enabled
                </NxCheckbox>
                <NxP>
                  Note: It is recommended that you manually re-index after enabling this feature in order for Advanced
                  Search to index your historical data.
                </NxP>
              </NxFieldset>
              <NxTile.Subsection>
                <NxTile.SubsectionHeader>
                  <h3 className="nx-h3">Indexing</h3>
                </NxTile.SubsectionHeader>
                <NxP>
                  To ensure search results are as accurate as possible, Advanced Search automatically re-indexes when
                  any changes are made to relevant IQ Server application data. Automatic indexing only applies to data
                  changes made whilst the feature is enabled. In order for Advanced Search to index historical data, you
                  must run a manual index. Re-indexing may impact the performance of IQ Server while it is running, so
                  it is recommended to do this during a time of low usage. If you would like to manually re-index, you
                  can do so below.
                </NxP>
                <NxP>
                  <span>Last Indexed: </span>
                  <span id="advanced-search-last-index-time">
                    {lastIndexTime ? new Date(lastIndexTime).toLocaleString() : ''}
                  </span>
                </NxP>
              </NxTile.Subsection>
              {reIndexError && (
                <LoadError
                  titleMessage="An error occurred while in triggering the re-index operation."
                  error={reIndexError}
                  retryHandler={reIndex}
                />
              )}
              {pollError && (
                <LoadError titleMessage="An error occurred while checking indexing status." error={pollError} />
              )}
            </NxTile.Content>
          </NxStatefulForm>
        </NxTile>
      </LoadWrapper>
    </NxPageMain>
  );
}

AdvancedSearchConfig.propTypes = {
  isEnabled: PropTypes.bool.isRequired,
  setIsEnabled: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired,
  load: PropTypes.func.isRequired,
  save: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.object,
  saveError: PropTypes.object,
  reIndexError: PropTypes.object,
  pollError: PropTypes.object,
  submitMaskState: PropTypes.bool,
  submitMaskMessage: PropTypes.string,
  reIndex: PropTypes.func.isRequired,
  isFullIndexTriggered: PropTypes.bool.isRequired,
  lastIndexTime: PropTypes.number,
  isAuthorized: PropTypes.bool.isRequired,
};
