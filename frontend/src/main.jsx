import React from "react";
import { createRoot } from "react-dom/client";
import App from "./app/App.jsx";
import { QueryProvider } from "./app/providers/QueryProvider.jsx";
import { applyTheme, getInitialTheme } from "./app/theme.js";

// Set the theme before first paint to avoid a flash of the wrong palette.
applyTheme(getInitialTheme());

createRoot(document.getElementById("root")).render(
  <QueryProvider>
    <App />
  </QueryProvider>
);
