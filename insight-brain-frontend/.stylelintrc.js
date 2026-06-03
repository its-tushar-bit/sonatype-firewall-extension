module.exports = {
  customSyntax: 'postcss-scss',
  plugins: ['@stylistic/stylelint-plugin'],
  rules: {
    '@stylistic/no-eol-whitespace': [true, { severity: 'warning' }],
  },
};
