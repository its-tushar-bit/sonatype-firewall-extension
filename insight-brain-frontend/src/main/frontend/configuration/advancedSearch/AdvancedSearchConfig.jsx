/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';
import NxExternalLink from '../../react/NxExternalLink';
import LoadError from '../../react/LoadError';
import { NxButton, NxCheckbox, NxFieldset, NxFontAwesomeIcon, NxSubmitMask } from '@sonatype/react-shared-components';

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
    isDirty,
    loadError: loadErrorProp,
    saveError,
    reIndexError,
    pollError,
    isAuthorized,
  } = props;

  // Form State
  const { isEnabled, lastIndexTime, isFullIndexTriggered } = props;

  const loadError = isAuthorized ? loadErrorProp : authError;

  useEffect(() => {
    load();
  }, []);

  function onSubmit(evt) {
    evt.preventDefault();
    save();
  }

  function reIndexHandler(evt) {
    evt.preventDefault();
    reIndex();
  }

  return (
    <main id="advanced-search-config-page-container" className="nx-page-main">
      <LoadWrapper loading={loading} error={loadError} retryHandler={load}>
        <section id="advanced-search-config" className="nx-tile iq-advanced-search-config-tile">
          <form className="nx-form" onSubmit={onSubmit}>
            <header className="nx-tile-header">
              <div className="iq-tile-header__title">
                <h2 className="nx-h2">Advanced Search Configuration</h2>
              </div>
            </header>
            <div className="nx-tile-content">
              <p className="nx-p">
                Advanced Search gives you robust search options to help you find exactly what you are looking for.
                Search terms give you the ability to scope your search to specific types of information relating to the
                following categories:
              </p>
              <ul className="nx-list nx-list--bulleted">
                <li className="nx-list__item">
                  <span className="nx-list__text">Organizations</span>
                </li>
                <li className="nx-list__item">
                  <span className="nx-list__text">Applications</span>
                </li>
                <li className="nx-list__item">
                  <span className="nx-list__text">Application Categories</span>
                </li>
                <li className="nx-list__item">
                  <span className="nx-list__text">Component Labels</span>
                </li>
                <li className="nx-list__item">
                  <span className="nx-list__text">Policies</span>
                </li>
                <li className="nx-list__item">
                  <span className="nx-list__text">Security Vulnerabilities</span>
                </li>
              </ul>
              <p className="nx-p">
                You can combine multiple search terms to craft an even more targeted search. For more information on how
                to use this feature,{' '}
                <NxExternalLink href="https://links.sonatype.com/products/nxiq/doc/advanced-search">
                  check out the documentation
                </NxExternalLink>
                .
              </p>
              {submitMaskState !== null && <NxSubmitMask success={submitMaskState} message={submitMaskMessage} />}
              <NxFieldset label="Advanced Search Status" isRequired>
                <NxCheckbox
                  id="advanced-search-config-is-enabled-checkbox"
                  isChecked={isEnabled}
                  onChange={setIsEnabled}
                >
                  Enabled
                </NxCheckbox>
                <p className="nx-p">
                  Note: It is recommended that you manually re-index after enabling this feature in order for Advanced
                  Search to index your historical data.
                </p>
              </NxFieldset>
              <section className="nx-tile-subsection">
                <header className="nx-tile-subsection__header">
                  <h3 className="nx-h3">Indexing</h3>
                </header>
                <p className="nx-p">
                  To ensure search results are as accurate as possible, Advanced Search automatically re-indexes when
                  any changes are made to relevant IQ Server application data. Automatic indexing only applies to data
                  changes made whilst the feature is enabled. In order for Advanced Search to index historical data, you
                  must run a manual index. Re-indexing may impact the performance of IQ Server while it is running, so
                  it is recommended to do this during a time of low usage. If you would like to manually re-index, you
                  can do so below.
                </p>
                <p className="nx-p">
                  <span>Last Indexed: </span>
                  <span id="advanced-search-last-index-time">
                    {lastIndexTime ? new Date(lastIndexTime).toLocaleString() : ''}
                  </span>
                </p>
              </section>
              <footer className="nx-footer">
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
                {saveError && (
                  <LoadError
                    titleMessage="An error occurred while saving the configuration."
                    error={saveError}
                    retryHandler={onSubmit}
                  />
                )}
                <div className="nx-btn-bar">
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
                  <NxButton type="button" id="advanced-search-config-cancel" onClick={resetForm} disabled={!isDirty}>
                    Cancel
                  </NxButton>
                  <NxButton type="submit" id="advanced-search-config-save" variant="primary" disabled={!isDirty}>
                    Save
                  </NxButton>
                </div>
              </footer>
            </div>
          </form>
        </section>
      </LoadWrapper>
    </main>
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
