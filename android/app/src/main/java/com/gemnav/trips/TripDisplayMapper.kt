package com.gemnav.trips

import android.content.Context
import com.gemnav.utils.PolylineDecoder
import java.text.DateFormat
import kotlin.math.roundToInt

object TripDisplayMapper {

    fun map(context: Context, summaries: List<TripSummary>): List<TripDisplayModel> {
        if (summaries.isEmpty()) return emptyList()

        val df: DateFormat = android.text.format.DateFormat.getMediumDateFormat(context)
        val tf: DateFormat = android.text.format.DateFormat.getTimeFormat(context)

        return summaries.map { s ->
            val startText = df.format(s.startTimestamp) + " " + tf.format(s.startTimestamp)
            val endText = if (s.endTimestamp != null)
                df.format(s.endTimestamp) + " " + tf.format(s.endTimestamp)
            else
                "In Progress"

            val km = (s.distanceMeters / 1000.0)
            val distanceText = "${(km * 10).roundToInt() / 10.0} km"

            TripDisplayModel(
                id = s.id,
                startTimeText = startText,
                endTimeText = endText,
                distanceText = distanceText,
                path = PolylineDecoder.decode(s.encodedPath)
            )
        }
    }
}
