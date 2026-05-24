import pluginVue from "eslint-plugin-vue";
import typescriptEslint from "@typescript-eslint/parser";
import typescriptEslintPlugin from "@typescript-eslint/eslint-plugin";
import prettier from "eslint-plugin-prettier";

export default [
  {
    files: ["**/*.{js,mjs,cjs,ts}"],
    languageOptions: {
      parser: typescriptEslint,
      parserOptions: {
        ecmaVersion: "latest",
        sourceType: "module",
        ecmaFeatures: {
          jsx: true,
        },
      },
    },
    plugins: {
      vue: pluginVue,
      "@typescript-eslint": typescriptEslintPlugin,
      prettier,
    },
    rules: {
      ...pluginVue.configs["flat/recommended"].rules,
      ...typescriptEslintPlugin.configs.recommended.rules,
      "prettier/prettier": "error",
    },
  },
  {
    files: ["**/*.vue"],
    languageOptions: {
      parser: pluginVue.configs.base.parser,
      parserOptions: {
        parser: typescriptEslint,
        ecmaVersion: "latest",
        sourceType: "module",
      },
    },
  },
];