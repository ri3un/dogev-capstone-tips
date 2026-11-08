package com.gaebalhagae.report.service;

import com.gaebalhagae.record.dto.RecordResponses.WeightPoint;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/** ⑦ 체중 꺾은선 그래프를 PNG data URI 로. 글꼴이 없는 서버에서도 깨지지 않게 글자는 숫자·날짜만 씁니다 */
@Slf4j
public final class WeightChart {

    private WeightChart() {
    }

    public static String dataUri(List<WeightPoint> points) {
        if (points.size() < 2) {
            return null;
        }
        try {
            TimeSeries series = new TimeSeries("kg");
            for (WeightPoint p : points) {
                series.addOrUpdate(new Day(p.date().getDayOfMonth(), p.date().getMonthValue(), p.date().getYear()),
                        p.weightKg());
            }
            JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, "kg",
                    new TimeSeriesCollection(series), false, false, false);
            chart.setBackgroundPaint(Color.WHITE);
            XYPlot plot = chart.getXYPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeGridlinePaint(new Color(0xDDDDDD));
            plot.setDomainGridlinePaint(new Color(0xDDDDDD));
            ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(false);
            ((DateAxis) plot.getDomainAxis()).setDateFormatOverride(new SimpleDateFormat("MM-dd"));
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
            renderer.setSeriesPaint(0, new Color(0xE8833A));
            renderer.setSeriesStroke(0, new BasicStroke(2.5f));
            plot.setRenderer(renderer);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(out, chart, 640, 260);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception | Error e) {
            // 서버에 글꼴(fontconfig)이 없으면 그래프만 빼고 표는 그대로 보여 줍니다
            log.warn("체중 그래프를 그리지 못했습니다: {}", e.toString());
            return null;
        }
    }
}
