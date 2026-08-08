/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import mavenIcon from 'MainRoot/firewall/iqProxy/logos/maven2.svg';
import npmIcon from 'MainRoot/firewall/iqProxy/logos/npm.svg';
import pypiIcon from 'MainRoot/firewall/iqProxy/logos/pypi.svg';
import nugetIcon from 'MainRoot/firewall/iqProxy/logos/nuget.svg';

export const FORMAT_OPTIONS = [
  { value: 'maven2', label: 'Maven', icon: mavenIcon },
  { value: 'npm', label: 'npm', icon: npmIcon },
  { value: 'pypi', label: 'PyPI', icon: pypiIcon },
  { value: 'nuget', label: 'NuGet', icon: nugetIcon },
];

export const DEFAULT_UPSTREAM_URLS = {
  maven2: 'https://repo1.maven.org/maven2/',
  npm: 'https://registry.npmjs.org',
  pypi: 'https://pypi.org',
  nuget: 'https://api.nuget.org/v3/index.json',
};

export const PCCS_ELIGIBLE_FORMATS = new Set(['npm', 'pypi']);
export const PACKAGE_HOST_URL_REQUIRED_FORMATS = new Set(['pypi']);

const formatByValue = FORMAT_OPTIONS.reduce((acc, opt) => ({ ...acc, [opt.value]: opt }), {});

export const getFormatLabel = (value) => formatByValue[value]?.label ?? value;
export const getFormatIcon = (value) => formatByValue[value]?.icon ?? null;
export const isPccsEligible = (format) => PCCS_ELIGIBLE_FORMATS.has(format);
export const isPackageHostUrlRequired = (format) => PACKAGE_HOST_URL_REQUIRED_FORMATS.has(format);
