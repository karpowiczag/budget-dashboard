export function StateScreen({ tone = "neutral", children }) {
  return <main className={`state ${tone === "error" ? "error" : ""}`}>{children}</main>;
}
