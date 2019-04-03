var isProd = process.env.NODE_ENV;
module.exports = {
  "parserOptions": {
    "ecmaVersion": 6,
    "sourceType": "module",
    "ecmaFeatures": {
      "experimentalObjectRestSpread": true
    }
  },
  "env": {
    "browser": true,
    "node": true,
    "jasmine": true
  },
  "globals": {
    "angular": false,
    "$": false,
    "clmBuildTimestamp": false,
    "Fuse": false,
    "AngularStateUtils":  false,
    "inject": false,
    "Set": false,
    "jQuery": false,
    "SpecUtil": false,
    "JiraServiceMockData": false,
    "ResourceUtils": false,
    "StoreMockData": false,
    "SystemNoticeMockData": false,
    "StoreUtils": false,
    "PolicyTileMockData": false,
    "ProprietaryMockData": false,
    "MockData": false,
    "SidebarResourceMockData": false,
    "LabelMockData": false,
    "AccessMockData": false,
    "WebhookMockData": false,
    "ApplicationMockData": false
  },
  "extends": "eslint:recommended",
  "rules": {
    "camelcase": [
      "error",
      {
        "properties": "never"
      }
    ],
    "curly": [
      "error",
      "all"
    ],
    "eqeqeq": ["error", "always", {"null": "ignore"}],
    "wrap-iife": 2,
    "indent": [
      "error",
      2,
      {
        "SwitchCase": 1,
        "MemberExpression": 2,
        "ObjectExpression": 1,
        "VariableDeclarator": 2,
        "FunctionDeclaration": {"parameters": "first"},
        "FunctionExpression": {"parameters": "first"},
        "CallExpression": {"arguments": 2}
      }
    ],
    "new-cap": "off",
    "no-caller": "error",
    "quotes": [
      "error",
      "single"
    ],
    "max-len": [ "error", { "code": 120 } ],
    "no-undef": "error",
    "no-unused-vars": "error",
    "strict": "error",
    "no-invalid-this": "off",
    "no-mixed-spaces-and-tabs": "error",
    "no-multiple-empty-lines": ["error", { "max": 1}],
    "no-multi-spaces": "error",
    "no-nested-ternary": "off",
    "padded-blocks": "off",
    "key-spacing": "error",
    "space-unary-ops": [
      "error",
      {
        "words": false,
        "nonwords": false
      }
    ],
    "comma-spacing": [
      "error",
      {
        "before": false,
        "after": true
      }
    ],
    "semi-spacing": [
      "error",
      {
        "before": false,
        "after": true
      }
    ],
    "no-spaced-func": "error",
    "space-before-function-paren": [
      "error",
      {
        "anonymous": "ignore",
        "named": "never"
      }
    ],
    "comma-dangle": [
      "error",
      "never"
    ],
    "no-trailing-spaces": "error",
    "eol-last": "error",
    "semi": [
      "error",
      "always"
    ],
    "space-infix-ops": "error",
    "keyword-spacing": [
      "error",
      {}
    ],
    "space-before-blocks": [
      "error",
      "always"
    ],
    "vars-on-top": "off",
    "space-in-parens": [
      "error",
      "never"
    ],
    "no-console": isProd ? ["error", { "allow": ["warn", "error"] }] : 'off',
    "no-debugger": isProd ? 'error' : 'off',
    "array-bracket-spacing": ["error", "never"],
    "object-property-newline": ["error", { "allowMultiplePropertiesPerLine": true }],
    "brace-style": ["error", "stroustrup", { "allowSingleLine": true }]
  }
}
