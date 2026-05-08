import { Download } from "lucide-react";
import { useEffect, useRef } from "react";
import {
  BarChart,
  FunnelChart,
  GaugeChart,
  HeatmapChart,
  LineChart,
  PieChart,
  RadarChart,
  SankeyChart,
  ScatterChart,
  SunburstChart,
  TreemapChart,
} from "echarts/charts";
import {
  CalendarComponent,
  DataZoomComponent,
  DatasetComponent,
  GridComponent,
  LegendComponent,
  MarkLineComponent,
  RadarComponent,
  TitleComponent,
  ToolboxComponent,
  TooltipComponent,
  TransformComponent,
  VisualMapComponent,
} from "echarts/components";
import * as echarts from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";

echarts.use([
  BarChart,
  CalendarComponent,
  CanvasRenderer,
  DataZoomComponent,
  DatasetComponent,
  FunnelChart,
  GaugeChart,
  GridComponent,
  HeatmapChart,
  LegendComponent,
  LineChart,
  MarkLineComponent,
  PieChart,
  RadarChart,
  RadarComponent,
  SankeyChart,
  ScatterChart,
  SunburstChart,
  TitleComponent,
  ToolboxComponent,
  TooltipComponent,
  TransformComponent,
  TreemapChart,
  VisualMapComponent,
]);

export function EChart({
  className = "chart",
  empty,
  emptyMessage = "Brak danych dla wykresu.",
  exportName,
  onClick,
  option,
}) {
  const nodeRef = useRef(null);
  const chartRef = useRef(null);
  const onClickRef = useRef(onClick);

  useEffect(() => {
    onClickRef.current = onClick;
  }, [onClick]);

  useEffect(() => {
    if (!nodeRef.current || empty) return undefined;
    const chart = echarts.init(nodeRef.current, null, { renderer: "canvas" });
    chartRef.current = chart;

    const handleClick = (params) => onClickRef.current?.(params?.data || params);
    chart.on("click", handleClick);

    let resizeObserver;
    if (typeof ResizeObserver !== "undefined") {
      resizeObserver = new ResizeObserver(() => chart.resize());
      resizeObserver.observe(nodeRef.current);
    } else {
      window.addEventListener("resize", chart.resize);
    }

    return () => {
      chart.off("click", handleClick);
      resizeObserver?.disconnect();
      window.removeEventListener("resize", chart.resize);
      chart.dispose();
      chartRef.current = null;
    };
  }, [empty]);

  useEffect(() => {
    if (!chartRef.current || empty) return;
    chartRef.current.setOption({ animation: false, ...option }, true);
  }, [empty, option]);

  function exportPng() {
    const chart = chartRef.current;
    if (!chart) return;
    const url = chart.getDataURL({
      backgroundColor: "#ffffff",
      pixelRatio: 2,
      type: "png",
    });
    const link = document.createElement("a");
    link.href = url;
    link.download = `${exportName || "wykres"}.png`;
    link.click();
  }

  if (empty) {
    return <div className="chartEmpty">{emptyMessage}</div>;
  }

  return (
    <div className="chartShell">
      {exportName && (
        <button type="button" className="chartExport" onClick={exportPng} title="Eksportuj wykres PNG">
          <Download size={14} /> PNG
        </button>
      )}
      <div ref={nodeRef} className={className} />
    </div>
  );
}
