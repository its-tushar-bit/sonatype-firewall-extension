/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { selectSourceControlConfigurationSlice } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import { NxList, NxFontAwesomeIcon, NxTextLink } from '@sonatype/react-shared-components';
import { faCheckCircle, faExclamationCircle, faQuestionCircle } from '@fortawesome/pro-solid-svg-icons';

const Icon = ({ valid, missing }) => (
  <NxFontAwesomeIcon
    className={`iq-source-control-check iq-source-control-check-${valid ? 'ok' : missing ? 'nok' : 'error'}`}
    icon={valid ? faCheckCircle : missing ? faQuestionCircle : faExclamationCircle}
  />
);

const ListItem = ({ data, text }) => (
  <NxList.Item>
    <Icon valid={data?.valid} missing={!data} />
    <NxList.Text>{text}</NxList.Text>
    {data?.message && <NxList.Subtext>{data?.message}</NxList.Subtext>}
  </NxList.Item>
);

const TestConfigurationResults = () => {
  const {
    sourceControl,
    scmConfigValidation: { result, error, loading },
  } = useSelector(selectSourceControlConfigurationSlice);

  const isSshEnabled = (sourceControl) => {
    return sourceControl?.sshEnabled?.isInherited
      ? sourceControl?.sshEnabled?.parentValue
      : sourceControl?.sshEnabled?.value;
  };

  const showRepositoryResult = () => {
    const isValidPublicRepo = result?.repoPublic?.valid;
    const isValidPrivateRepo = result?.repoPrivate?.valid;
    return isValidPrivateRepo || !isValidPublicRepo;
  };

  const showTable = result || loading || error;

  return showTable ? (
    <section id="scm-config-results" className="nx-tile-subsection">
      <header className="nx-tile-subsection__header">
        <hgroup className="nx-tile-header__headings">
          <div className="nx-tile-header__title">
            <h3 id="scm-config-results-title">Configuration Test Results</h3>
          </div>
          <h4 className="iq-source-control-configuration-test-subtitle">
            Learn more about SCM Configuration test results{' '}
            <NxTextLink external href="https://links.sonatype.com/products/nxiq/doc/scm-test-configuration">
              here
            </NxTextLink>
            .
          </h4>
        </hgroup>
      </header>
      <NxList isLoading={loading} error={error}>
        <ListItem data={result?.configurationComplete} text="Configuration complete" />
        {showRepositoryResult() && <ListItem data={result?.repoPrivate} text="Private repository" />}
        <ListItem data={result?.tokenPermissions} text="Sufficient token permissions" />
        {isSshEnabled(sourceControl) && <ListItem data={result?.sshConfiguration} text="SSH configuration complete" />}
      </NxList>
    </section>
  ) : null;
};

Icon.propTypes = {
  valid: PropTypes.bool,
  missing: PropTypes.bool,
};

ListItem.propTypes = {
  data: PropTypes.shape({
    valid: PropTypes.bool,
    message: PropTypes.string,
  }),
  text: PropTypes.string.isRequired,
};

export default TestConfigurationResults;
