import React from "react";
import { createRoot } from "react-dom/client";
import App from "./app/App.jsx";
import { QueryProvider } from "./app/providers/QueryProvider.jsx";

createRoot(document.getElementById("root")).render(
  <QueryProvider>
    <App />
  </QueryProvider>
);
