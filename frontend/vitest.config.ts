import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            "@": fileURLToPath(new URL("./src", import.meta.url)),
        },
    },
    test: {
        environment: "jsdom",
        include: ["src/**/*.test.{ts,tsx}", "src/**/*.spec.{ts,tsx}"],
        setupFiles: ["./src/test/setupTests.ts"],
        coverage: {
            provider: "v8",
            reporter: ["text", "lcov"],
            reportsDirectory: "coverage",
            include: ["src/**/*.{ts,tsx}"],
            exclude: [
                "src/**/*.test.{ts,tsx}",
                "src/**/*.spec.{ts,tsx}",
                "src/test/**",
                "src/main.tsx",
                "src/vite-env.d.ts",
            ],
        },
    },
});
