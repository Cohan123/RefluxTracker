package com.example.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.analysis.DailyBurden
import com.example.analysis.DataQuality
import com.example.analysis.TimeOfDayBucket
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Renders the doctor report as a DIN-A4 PDF using Android's built-in
 * [PdfDocument]. No external library and no network access are required.
 *
 * The layout is paginated: every section checks the remaining space and starts
 * a new page when necessary. The disclaimer is printed on every page. Missing
 * values are written as "nicht erfasst"/"nicht berechenbar"; no value is
 * estimated or invented.
 */
object DoctorReportPdf {

    const val DISCLAIMER =
        "Dieser Bericht dokumentiert subjektive Symptome und beobachtete statistische " +
            "Zusammenhänge. Er stellt keine medizinische Diagnose dar."

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val CONTENT_BOTTOM = PAGE_HEIGHT - 64f

    fun write(context: Context, data: DoctorReportData): File {
        val document = PdfDocument()
        val writer = ReportWriter(document, data)
        writer.render()

        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(Date(data.generatedAt))
        val file = File(directory, "Arztbericht_$date.pdf")
        try {
            FileOutputStream(file).use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }
        return file
    }

    private class ReportWriter(
        private val document: PdfDocument,
        private val data: DoctorReportData
    ) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 0f

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 32, 48)
            textSize = 18f
            isFakeBoldText = true
        }
        private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(75, 85, 99)
            textSize = 9.5f
        }
        private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 66, 102)
            textSize = 13f
            isFakeBoldText = true
        }
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 41, 55)
            textSize = 9.5f
        }
        private val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 41, 55)
            textSize = 9.5f
            isFakeBoldText = true
        }
        private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 100, 115)
            textSize = 8f
        }
        private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 66, 102)
            textSize = 10f
            isFakeBoldText = true
        }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(205, 213, 224)
            strokeWidth = 0.8f
        }
        private val chartLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 66, 102)
            strokeWidth = 1.8f
            style = Paint.Style.STROKE
        }
        private val trendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(139, 92, 44)
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(7f, 5f), 0f)
        }
        private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 231, 238)
            strokeWidth = 0.6f
        }
        private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 118, 130)
            textSize = 7.2f
        }

        fun render() {
            newPage()
            drawFirstPageHeader()
            drawSymptomsSection()
            drawMedicationSection()
            drawCourseSection()
            drawAssociationsSection()
            drawGerdQSection()
            finishPage()
        }

        private fun newPage() {
            finishPage()
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(info)
            canvas = page!!.canvas
            y = MARGIN
            if (pageNumber > 1) {
                val c = canvas!!
                c.drawText("GERD / Reflux – Symptom- und Verlaufsbericht", MARGIN, 26f, smallPaint)
                c.drawText("Seite $pageNumber", PAGE_WIDTH - MARGIN - 42f, 26f, smallPaint)
                c.drawLine(MARGIN, 32f, PAGE_WIDTH - MARGIN, 32f, linePaint)
                y = 46f
            }
            drawFooter()
        }

        private fun finishPage() {
            page?.let { document.finishPage(it) }
            page = null
            canvas = null
        }

        private fun drawFooter() {
            val c = canvas ?: return
            val footerTop = PAGE_HEIGHT - 50f
            c.drawLine(MARGIN, footerTop, PAGE_WIDTH - MARGIN, footerTop, linePaint)
            val lines = wrap(DISCLAIMER, footerPaint, CONTENT_WIDTH - 60f)
            var fy = footerTop + 10f
            lines.forEach { line ->
                c.drawText(line, MARGIN, fy, footerPaint)
                fy += 9f
            }
            c.drawText("Seite $pageNumber", PAGE_WIDTH - MARGIN - 34f, PAGE_HEIGHT - 22f, footerPaint)
        }

        private fun drawFirstPageHeader() {
            val c = canvas ?: return
            c.drawText("GERD / Reflux – Symptom- und Verlaufsbericht", MARGIN, y + 12f, titlePaint)
            y += 24f
            val name = data.userName
            if (name != null) {
                c.drawText("Name: $name", MARGIN, y + 8f, bodyPaint)
                y += 14f
            }
            c.drawText(
                "Auswertungszeitraum: ${date(data.periodStart)} – ${date(data.periodEnd)} " +
                    "(${data.timeWindowDays} Tage)",
                MARGIN,
                y + 8f,
                bodyPaint
            )
            y += 14f
            c.drawText(
                "Erstellt am: ${dateTime(data.generatedAt)}",
                MARGIN,
                y + 8f,
                smallPaint
            )
            y += 18f
            c.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 16f
        }

        private fun drawSymptomsSection() {
            heading("A. Symptome")
            val s = data.symptoms
            keyValue("Symptome gesamt", s.totalSymptoms.toString())
            keyValue("Tage mit Sodbrennen", s.daysWithHeartburn.toString())
            keyValue("Nächte mit Reflux (22–6 Uhr)", s.nightsWithReflux.toString())
            keyValue(
                "Mittlere Intensität",
                s.averageIntensity?.let { formatOne(it) + "/10" } ?: "nicht erfassbar"
            )
            keyValue(
                "Symptomfreie Tage",
                if (s.documentedDays > 0) {
                    "${s.symptomFreeDays} von ${s.documentedDays} dokumentierten Tagen"
                } else {
                    "nicht erfassbar"
                }
            )
            keyValue(
                "Häufigste Beschwerdezeit",
                if (s.totalSymptoms > 0) {
                    String.format(
                        Locale.GERMAN,
                        "%02d:00–%02d:00 Uhr",
                        s.peakStartHour,
                        s.peakEndHourExclusive
                    )
                } else {
                    "keine Symptome erfasst"
                }
            )
            y += 4f
            drawTimeOfDayChart(data.timeOfDayBuckets)
            y += 10f
        }

        private fun drawMedicationSection() {
            heading("B. Therapie und Medikamente")
            val m = data.medication
            if (m.documentedIntakes == 0) {
                paragraph(
                    "Es sind keine strukturierten Medikamenteneinnahmen dokumentiert. " +
                        "Dosierung, geplante Einnahmehäufigkeit und Einnahmetreue können " +
                        "deshalb nicht berechnet werden."
                )
            } else {
                keyValue("Dokumentierte Einnahmen", "${m.documentedIntakes} an ${m.distinctDays} Tagen")
                if (m.names.isNotEmpty()) {
                    keyValue("Dokumentierte Präparate", m.names.joinToString(", "))
                }
                if (m.firstIntakeAt != null && m.lastIntakeAt != null) {
                    keyValue(
                        "Dokumentierter Zeitraum",
                        "${date(m.firstIntakeAt)} – ${date(m.lastIntakeAt)}"
                    )
                }
                if (m.intakeAtTimes.isNotEmpty()) {
                    val shown = m.intakeAtTimes.take(8).joinToString(", ") { dateTime(it) }
                    val suffix = if (m.intakeAtTimes.size > 8) {
                        " … und ${m.intakeAtTimes.size - 8} weitere"
                    } else {
                        ""
                    }
                    paragraphSmall("Dokumentierte Einnahmezeiten: $shown$suffix")
                }
                keyValue(
                    "Dokumentierte Häufigkeit",
                    m.documentedIntakesPerWeek?.let {
                        formatOne(it) + " pro Woche (aus Einträgen, kein Einnahmeplan)"
                    } ?: "nicht berechenbar (zu kurzer Zeitraum)"
                )
                keyValue("Dosierung", "nicht erfasst")
                keyValue("Geplante Einnahmehäufigkeit", "nicht erfasst")
                keyValue("Einnahmetreue", "nicht berechenbar (kein Einnahmeplan dokumentiert)")
            }
            y += 10f
        }

        private fun drawCourseSection() {
            heading("C. Verlauf")
            val trend = data.course.trend
            val first = trend.firstHalfAverageBurden
            val second = trend.secondHalfAverageBurden
            if (first != null && second != null && trend.firstHalfDocumentedDays > 0 &&
                trend.secondHalfDocumentedDays > 0
            ) {
                keyValue(
                    "Ø GERD-Burden 1. Hälfte",
                    "${formatOne(first)} (${trend.firstHalfDocumentedDays} dokumentierte Tage)"
                )
                keyValue(
                    "Ø GERD-Burden 2. Hälfte",
                    "${formatOne(second)} (${trend.secondHalfDocumentedDays} dokumentierte Tage)"
                )
                val change = data.course.changePercent
                val arrow = when {
                    change == null -> "→"
                    change <= -1.0 -> "↓"
                    change >= 1.0 -> "↑"
                    else -> "→"
                }
                keyValue(
                    "Veränderung",
                    change?.let {
                        String.format(
                            Locale.GERMAN,
                            "%s %+.0f %% (%+.1f Punkte)",
                            arrow,
                            it,
                            data.course.changeAbsolute ?: 0.0
                        )
                    } ?: "absolut ${formatSignedOne(data.course.changeAbsolute)} Punkte",
                    highlight = true
                )
                paragraphSmall(
                    "Vergleich der ersten und zweiten Hälfte des Auswertungszeitraums. " +
                        "Mittelwert des täglichen GERD-Burden über dokumentierte Tage; " +
                        "Tage ohne Einträge sind ausgeschlossen. Negativ bedeutet Besserung."
                )
            } else {
                paragraph(
                    "Der Verlauf ist nicht berechenbar, weil in einer Hälfte des Zeitraums " +
                        "keine dokumentierten Tage liegen. Es werden keine Werte geschätzt."
                )
            }
            y += 4f
            drawBurdenChart(data.dailyBurden, trend)
            y += 10f
        }

        private fun drawAssociationsSection() {
            heading("D. Beobachtete Zusammenhänge")
            val associations = data.associations
            if (associations.isEmpty()) {
                paragraph(
                    "Es liegen keine ausreichend belastbaren Faktorkombinationen vor " +
                        "(mindestens 3 Mahlzeiten mit und ohne Faktor). Fehlende Daten werden " +
                        "nicht als positiver oder negativer Befund gewertet."
                )
            } else {
                paragraphSmall(
                    "Vergleich der Symptomwahrscheinlichkeit je Mahlzeit mit und ohne Faktor " +
                        "(30 Min. bis 4 Std. nach der Mahlzeit). Die relative Veränderung wird " +
                        "nur bei ausreichender Datenbasis ausgewiesen."
                )
                y += 4f
                drawAssociationTable(associations)
            }

            if (data.insights.isNotEmpty()) {
                y += 8f
                ensureSpace(16f)
                canvas!!.drawText("Aus den Daten abgeleitete Hinweise", MARGIN, y, bodyBoldPaint)
                y += 12f
                data.insights.forEach { insight ->
                    drawBullet(insight)
                }
            }
            y += 10f
        }

        private fun drawGerdQSection() {
            heading("E. GerdQ")
            val gerdQ = data.gerdQ
            if (gerdQ == null) {
                paragraph(
                    "Keine GerdQ-Daten vorhanden. Es werden keine Start-, aktuellen oder " +
                        "Veränderungswerte geschätzt."
                )
            } else {
                keyValue(
                    "Startwert",
                    "${gerdQ.startValue} (${date(gerdQ.startDate)})"
                )
                keyValue(
                    "Aktueller Wert",
                    "${gerdQ.currentValue} (${date(gerdQ.currentDate)})"
                )
                keyValue("Veränderung", "${gerdQ.currentValue - gerdQ.startValue}")
            }
            y += 8f
        }

        private fun drawAssociationTable(associations: List<ReportAssociation>) {
            val columns = floatArrayOf(
                MARGIN,
                MARGIN + 178f,
                MARGIN + 258f,
                MARGIN + 392f
            )
            ensureSpace(16f)
            canvas!!.drawText("Faktor", columns[0], y, bodyBoldPaint)
            canvas!!.drawText("Relativ", columns[1], y, bodyBoldPaint)
            canvas!!.drawText("Mit / Ohne", columns[2], y, bodyBoldPaint)
            canvas!!.drawText("Datenlage", columns[3], y, bodyBoldPaint)
            y += 6f
            canvas!!.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 11f

            associations.forEach { association ->
                val labelLines = wrap(association.label, bodyPaint, 168f)
                val rowHeight = maxOf(14f, labelLines.size * 10f + 4f)
                ensureSpace(rowHeight)
                val rowTop = y - 7f
                labelLines.forEachIndexed { index, line ->
                    canvas!!.drawText(line, columns[0], y + index * 10f, bodyPaint)
                }
                val relative = association.relativeChangePercent
                canvas!!.drawText(
                    relative?.let { String.format(Locale.GERMAN, "%+.0f %%", it) } ?: "–",
                    columns[1],
                    y,
                    bodyBoldPaint
                )
                canvas!!.drawText(
                    String.format(
                        Locale.GERMAN,
                        "%.0f %% / %.0f %%",
                        association.probabilityWith * 100,
                        association.probabilityWithout * 100
                    ),
                    columns[2],
                    y,
                    bodyPaint
                )
                canvas!!.drawText(
                    "${association.dataQuality.label} (${association.exposedMeals}/${association.unexposedMeals})",
                    columns[3],
                    y,
                    smallPaint
                )
                y += rowHeight
                canvas!!.drawLine(MARGIN, y - 4f, PAGE_WIDTH - MARGIN, y - 4f, linePaint)
            }
            paragraphSmall(
                "Relative Veränderung = (Wahrscheinlichkeit mit Faktor − ohne Faktor) / ohne " +
                    "Faktor. Rein beobachteter Zusammenhang, keine Kausalität."
            )
        }

        private fun drawBurdenChart(days: List<DailyBurden>, trend: com.example.analysis.BurdenTrend) {
            if (days.isEmpty()) return
            val chartHeight = 130f
            ensureSpace(chartHeight + 30f)
            val c = canvas!!
            val left = MARGIN + 24f
            val top = y + 8f
            val width = CONTENT_WIDTH - 24f
            val height = chartHeight

            c.drawText("Täglicher GERD-Burden (0–100)", MARGIN, y, bodyBoldPaint)
            y += 12f

            for (value in listOf(0, 50, 100)) {
                val py = top + height - (value / 100f) * height
                c.drawLine(left, py, left + width, py, gridPaint)
                c.drawText(value.toString(), MARGIN, py + 3f, smallPaint)
            }

            fun x(index: Int): Float =
                if (days.size <= 1) left + width / 2f
                else left + width * index / (days.size - 1).toFloat()

            fun yFor(value: Double): Float =
                top + height - (value.coerceIn(0.0, 100.0) / 100.0 * height).toFloat()

            var previous: FloatArray? = null
            days.forEachIndexed { index, day ->
                val burden = day.burden
                val px = x(index)
                if (burden == null) {
                    c.drawCircle(px, top + height, 1f, smallPaint)
                    previous = null
                    return@forEachIndexed
                }
                val py = yFor(burden)
                previous?.let { (pxPrev, pyPrev) ->
                    c.drawLine(pxPrev, pyPrev, px, py, chartLinePaint)
                }
                if (day.isSpike) {
                    val spikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(176, 42, 42)
                    }
                    c.drawCircle(px, py, 2.8f, spikePaint)
                } else {
                    c.drawCircle(px, py, 1.7f, chartLinePaint)
                }
                previous = floatArrayOf(px, py)
            }

            val fitStart = trend.fittedStartBurden
            val fitEnd = trend.fittedEndBurden
            val fitStartDay = trend.fittedStartDayStartMillis
            val fitEndDay = trend.fittedEndDayStartMillis
            if (fitStart != null && fitEnd != null && fitStartDay != null && fitEndDay != null) {
                val startIndex = days.indexOfFirst { it.dayStartMillis == fitStartDay }
                val endIndex = days.indexOfFirst { it.dayStartMillis == fitEndDay }
                if (startIndex >= 0 && endIndex >= 0) {
                    c.drawLine(
                        x(startIndex),
                        yFor(fitStart),
                        x(endIndex),
                        yFor(fitEnd),
                        trendPaint
                    )
                }
            }

            c.drawText(date(days.first().dayStartMillis), left, top + height + 10f, smallPaint)
            val lastLabel = date(days.last().dayStartMillis)
            c.drawText(
                lastLabel,
                left + width - smallPaint.measureText(lastLabel),
                top + height + 10f,
                smallPaint
            )
            c.drawText(
                "Punkt = Tag mit Einträgen · rot = Spitze · Lücke = keine Einträge · " +
                    "gestrichelt = lineare Tendenz",
                MARGIN,
                top + height + 22f,
                smallPaint
            )
            y = top + height + 30f
        }

        private fun drawTimeOfDayChart(buckets: List<TimeOfDayBucket>) {
            val chartHeight = 92f
            ensureSpace(chartHeight + 26f)
            val c = canvas!!
            c.drawText("Symptome nach Tageszeit", MARGIN, y, bodyBoldPaint)
            y += 12f
            val cellWidth = CONTENT_WIDTH / 6f
            val cellHeight = 17f
            val maxWeight = buckets.maxOfOrNull { it.intensitySum } ?: 0
            for (row in 0 until 4) {
                for (column in 0 until 6) {
                    val hour = row * 6 + column
                    val bucket = buckets.getOrNull(hour) ?: TimeOfDayBucket(hour, 0, null, 0, 0)
                    val alpha = if (bucket.symptomCount == 0 || maxWeight == 0) {
                        20
                    } else {
                        (40 + 180.0 * bucket.intensitySum / maxWeight).roundToInt().coerceIn(40, 220)
                    }
                    val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(alpha, 18, 66, 102)
                    }
                    val left = MARGIN + column * cellWidth
                    val top = y + row * cellHeight
                    c.drawRect(
                        RectF(left, top, left + cellWidth - 3f, top + cellHeight - 3f),
                        rectPaint
                    )
                    val labelColor = if (alpha > 130) Color.WHITE else Color.rgb(31, 41, 55)
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = labelColor
                        textSize = 7.5f
                    }
                    c.drawText(
                        String.format(Locale.GERMAN, "%02d", hour),
                        left + 3f,
                        top + 11f,
                        labelPaint
                    )
                    c.drawText(
                        bucket.symptomCount.toString(),
                        left + cellWidth - 13f,
                        top + 11f,
                        labelPaint
                    )
                }
            }
            y += 4 * cellHeight + 4f
            c.drawText(
                "Farbe = Häufigkeit und Intensität der erfassten Symptome",
                MARGIN,
                y + 4f,
                smallPaint
            )
            y += 12f
        }

        private fun heading(text: String) {
            ensureSpace(28f)
            y += 6f
            canvas!!.drawText(text, MARGIN, y, sectionPaint)
            y += 5f
            canvas!!.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 13f
        }

        private fun keyValue(label: String, value: String, highlight: Boolean = false) {
            ensureSpace(13f)
            val c = canvas!!
            c.drawText(label, MARGIN, y, smallPaint)
            c.drawText(value, MARGIN + 178f, y, if (highlight) accentPaint else bodyPaint)
            y += 13f
        }

        private fun paragraph(text: String) {
            paragraphSmall(text, bodyPaint)
        }

        private fun paragraphSmall(text: String, paint: Paint = smallPaint) {
            val lines = wrap(text, paint, CONTENT_WIDTH)
            lines.forEach { line ->
                ensureSpace(11f)
                canvas!!.drawText(line, MARGIN, y, paint)
                y += 11f
            }
        }

        private fun drawBullet(text: String) {
            val lines = wrap(text, smallPaint, CONTENT_WIDTH - 10f)
            ensureSpace(maxOf(11f, lines.size * 10f))
            lines.forEachIndexed { index, line ->
                canvas!!.drawText(
                    if (index == 0) "• $line" else "   $line",
                    MARGIN,
                    y,
                    smallPaint
                )
                y += 10f
            }
        }

        private fun ensureSpace(needed: Float) {
            if (y + needed > CONTENT_BOTTOM) {
                newPage()
            }
        }

        private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (text.isBlank()) return emptyList()
            val result = mutableListOf<String>()
            for (paragraph in text.split('\n')) {
                if (paragraph.isBlank()) {
                    result += ""
                    continue
                }
                val words = paragraph.trim().split(Regex("\\s+"))
                var current = StringBuilder()
                for (word in words) {
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        current = StringBuilder(candidate)
                    } else {
                        if (current.isNotEmpty()) result += current.toString()
                        current = StringBuilder(word)
                    }
                }
                if (current.isNotEmpty()) result += current.toString()
            }
            return result
        }

        private fun date(timestamp: Long): String =
            SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(Date(timestamp))

        private fun dateTime(timestamp: Long): String =
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN).format(Date(timestamp))

        private fun formatOne(value: Double): String =
            String.format(Locale.GERMAN, "%.1f", value)

        private fun formatSignedOne(value: Double?): String =
            value?.let { String.format(Locale.GERMAN, "%+.1f", it) } ?: "nicht berechenbar"
    }
}
