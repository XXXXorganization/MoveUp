import js from "@eslint/js";
import globals from "globals";
import tseslint from "typescript-eslint";
import { defineConfig } from "eslint/config";

export default defineConfig([
  // 1. 忽略不需要的目录
  {
    ignores: ["dist/**", "coverage/**", "node_modules/**"],
  },

  // 2. 所有源文件（非测试）
  {
    files: ["**/*.{js,mjs,cjs,ts,mts,cts}"],
    ...js.configs.recommended,
    languageOptions: {
      globals: {
        ...globals.node,
      },
    },
  },

  // 3. TypeScript 推荐规则
  ...tseslint.configs.recommended,

  // 4. 覆盖：测试文件
  {
    files: ["**/*.test.{js,ts}", "**/*.spec.{js,ts}", "tests/**/*.{js,ts}"],
    languageOptions: {
      globals: {
        ...globals.jest,
      },
    },
    rules: {
      "@typescript-eslint/no-require-imports": "off",
      "@typescript-eslint/no-explicit-any": "off",
      "@typescript-eslint/no-unused-vars": "off",
    },
  },

  // 5. 覆盖：传统 CommonJS .js 文件（如 connect.js, knexfile.js 等）
  {
    files: ["**/*.js", "**/*.cjs"],
    rules: {
      "@typescript-eslint/no-require-imports": "off",
    },
  },
  {
    files: ["src/**/*.ts"],
    rules: {
      "@typescript-eslint/no-unused-vars": ["error", { "argsIgnorePattern": "^_" }],
    },
  },
  {
    files: ["src/modules/**/*.ts"],   // 或更精确： ["src/modules/**/model.ts", "src/modules/**/service.ts"]
    rules: {
      "@typescript-eslint/no-explicit-any": "off",
    },
  }
]);