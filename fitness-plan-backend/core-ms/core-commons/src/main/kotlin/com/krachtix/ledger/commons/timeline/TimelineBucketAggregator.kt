package com.krachtix.commons.timeline

import com.krachtix.commons.customer.DailyCountDto
import com.krachtix.commons.customer.DailyVolumeDto
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TimelineDataPoint(
    val label: String,
    val count: Long,
    val value: BigDecimal
)

data class TimelineResult(
    val intervalLabel: String,
    val data: List<TimelineDataPoint>
)

object TimelineBucketAggregator {

    private const val BUCKET_COUNT = 20
    private val HOURS_4 = Duration.ofHours(4)
    private val DAYS_2 = Duration.ofDays(2)
    private val DAYS_14 = Duration.ofDays(14)

    private val HOURLY_FORMAT = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH)
    private val DAILY_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val MONTHLY_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

    fun aggregateVolumesIntoBuckets(
        startDate: Instant,
        endDate: Instant,
        rawData: List<DailyVolumeDto>
    ): TimelineResult {
        val buckets = createBuckets(startDate, endDate)
        val intervalLabel = determineIntervalLabel(buckets)

        val dataPoints = buckets.mapIndexed { index, bucket ->
            val matching = filterMatching(rawData, bucket, isLast = index == buckets.lastIndex) { it.date }
            TimelineDataPoint(
                label = formatBucketLabel(bucket.start, bucket.end, intervalLabel),
                count = matching.sumOf { it.count },
                value = matching.fold(BigDecimal.ZERO) { acc, dto -> acc + dto.value }
            )
        }

        return TimelineResult(intervalLabel = intervalLabel, data = dataPoints)
    }

    fun aggregateCountsIntoBuckets(
        startDate: Instant,
        endDate: Instant,
        rawData: List<DailyCountDto>
    ): TimelineResult {
        val buckets = createBuckets(startDate, endDate)
        val intervalLabel = determineIntervalLabel(buckets)

        val dataPoints = buckets.mapIndexed { index, bucket ->
            val matching = filterMatching(rawData, bucket, isLast = index == buckets.lastIndex) { it.date }
            TimelineDataPoint(
                label = formatBucketLabel(bucket.start, bucket.end, intervalLabel),
                count = matching.sumOf { it.count },
                value = BigDecimal.ZERO
            )
        }

        return TimelineResult(intervalLabel = intervalLabel, data = dataPoints)
    }

    private data class Bucket(val start: Instant, val end: Instant)

    private fun createBuckets(startDate: Instant, endDate: Instant): List<Bucket> {
        val totalDuration = Duration.between(startDate, endDate)
        val bucketSize = Duration.ofMillis(totalDuration.toMillis() / BUCKET_COUNT)

        return (0 until BUCKET_COUNT).map { i ->
            val bucketStart = startDate.plus(bucketSize.multipliedBy(i.toLong()))
            val bucketEnd = when (i) {
                BUCKET_COUNT - 1 -> endDate
                else -> startDate.plus(bucketSize.multipliedBy((i + 1).toLong()))
            }
            Bucket(start = bucketStart, end = bucketEnd)
        }
    }

    private fun <T> filterMatching(
        rawData: List<T>,
        bucket: Bucket,
        isLast: Boolean,
        dateExtractor: (T) -> java.time.LocalDate
    ): List<T> = rawData.filter { item ->
        val itemInstant = dateExtractor(item).atStartOfDay().toInstant(ZoneOffset.UTC)
        when {
            isLast -> itemInstant >= bucket.start && itemInstant <= bucket.end
            else -> itemInstant >= bucket.start && itemInstant < bucket.end
        }
    }

    private fun determineIntervalLabel(buckets: List<Bucket>): String {
        val bucketSize = Duration.between(buckets.first().start, buckets[1].start)
        return when {
            bucketSize < HOURS_4 -> "Hourly"
            bucketSize < DAYS_2 -> "Daily"
            bucketSize < DAYS_14 -> "Weekly"
            else -> "Monthly"
        }
    }

    private fun formatBucketLabel(start: Instant, end: Instant, intervalLabel: String): String {
        val startZoned = start.atZone(ZoneOffset.UTC)
        return when (intervalLabel) {
            "Hourly" -> startZoned.format(HOURLY_FORMAT)
            "Daily" -> startZoned.format(DAILY_FORMAT)
            "Weekly" -> {
                val endZoned = end.atZone(ZoneOffset.UTC)
                "${startZoned.format(DAILY_FORMAT)} \u2013 ${endZoned.format(DAILY_FORMAT)}"
            }
            else -> startZoned.format(MONTHLY_FORMAT)
        }
    }
}
