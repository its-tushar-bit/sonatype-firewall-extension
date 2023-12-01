/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

module.exports = {
  roots: ['<rootDir>/src/main/frontend', '<rootDir>/src/test/frontend'],
  transformIgnorePatterns: [
    '/node_modules/(?!(pretty-bytes|@react-hook|@sonatype|@nivo|d3-color|d3-interpolate|d3-scale-chromatic)/)',
  ],
  transform: {
    '\\.[jt]sx?$': [
      'babel-jest',
      {
        presets: [['@babel/preset-env', { targets: { node: '16.16.0' } }], '@babel/preset-react'],
      },
    ],
  },
  testRegex: 'src/test/frontend/.*\\.jestspec\\.jsx?',
  testEnvironment: 'jsdom',
  moduleFileExtensions: ['js', 'jsx', 'json', 'node'],
  setupFilesAfterEnv: ['<rootDir>/src/test/frontend/setupJest.js'],
  moduleNameMapper: {
    '\\.s?css$': '<rootDir>/src/test/frontend/__mocks__/styleMock.js',
    '\\.(png|svg)$': '<rootDir>/src/test/frontend/__mocks__/imgMock.js',
    '\\.(html)$': '<rootDir>/src/test/frontend/__mocks__/htmlMock.js',
    '^MainRoot/(.*)': '<rootDir>/src/main/frontend/$1',
    '^TestRoot/(.*)': '<rootDir>/src/test/frontend/$1',
  },
  reporters: [
    'default',
    [
      'jest-junit',
      {
        outputFile: '<rootDir>/target/jest-reports/jest-junit.xml',
      },
    ],
  ],
};
