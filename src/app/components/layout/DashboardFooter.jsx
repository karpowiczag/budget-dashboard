import { ChartNoAxesCombined } from "lucide-react";

export function DashboardFooter() {
  return (
    <footer>
      <ChartNoAxesCombined size={16} />
      Dane pochodzą z backendu Spring Boot i są przeliczane po każdym imporcie CSV.
    </footer>
  );
}
