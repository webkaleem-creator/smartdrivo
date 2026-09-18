package com.example.data

import com.example.model.OrderStatus
import com.example.model.Platform
import com.example.model.RideCandidate
import com.example.model.VehicleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class RideDiagnosticReport(
    val id: String,
    val platform: Platform,
    val vehicleType: VehicleType,
    val detectedAt: Long,
    val dateStr: String,
    val timeStr: String,
    val fare: Float,
    val baseFare: Float,
    val tipAmount: Float,
    val pickupDistKm: Float,
    val dropDistKm: Float,
    val pickupAddress: String,
    val dropAddress: String,
    val status: OrderStatus,
    val matchedRule: String,
    val matchedRuleCode: String,
    val buttonFound: Boolean,
    val buttonDetails: String,
    val clickMethod: String,
    val finalAction: String,
    val error: String? = null,
    val insertLatencyMs: Long = 0L,
    val decisionLatencyMs: Long = 0L,
    val actionLatencyMs: Long = 0L,
    val totalLatencyMs: Long = 0L,
    val duplicateEventsCount: Int = 0
)

object RideDiagnosticsManager {

    private val _recentReports = MutableStateFlow<List<RideDiagnosticReport>>(emptyList())
    val recentReports: StateFlow<List<RideDiagnosticReport>> = _recentReports.asStateFlow()

    private val reportMap = LinkedHashMap<String, RideDiagnosticReport>()
    private val lock = Any()

    fun recordDetection(
        id: String,
        candidate: RideCandidate,
        insertLatencyMs: Long = 0L
    ) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val (dateStr, timeStr) = formatDateTime(now)
            val existing = reportMap[id]
            val report = if (existing != null) {
                existing.copy(
                    duplicateEventsCount = existing.duplicateEventsCount + 1,
                    fare = if ((candidate.fare ?: 0f) > 0f) (candidate.fare ?: 0f) else existing.fare,
                    pickupDistKm = if ((candidate.pickupDistKm ?: 0f) > 0f) (candidate.pickupDistKm ?: 0f) else existing.pickupDistKm,
                    dropDistKm = if ((candidate.dropDistKm ?: 0f) > 0f) (candidate.dropDistKm ?: 0f) else existing.dropDistKm,
                    pickupAddress = candidate.pickupAddress?.ifBlank { null } ?: existing.pickupAddress,
                    dropAddress = candidate.dropAddress?.ifBlank { null } ?: existing.dropAddress
                )
            } else {
                RideDiagnosticReport(
                    id = id,
                    platform = candidate.platform,
                    vehicleType = candidate.vehicleType,
                    detectedAt = if (candidate.detectionTimeMs > 0L) candidate.detectionTimeMs else now,
                    dateStr = dateStr,
                    timeStr = timeStr,
                    fare = candidate.fare ?: 0f,
                    baseFare = candidate.baseFare ?: (candidate.fare ?: 0f),
                    tipAmount = candidate.tipAmount ?: 0f,
                    pickupDistKm = candidate.pickupDistKm ?: 0f,
                    dropDistKm = candidate.dropDistKm ?: 0f,
                    pickupAddress = candidate.pickupAddress ?: "Extracting pickup...",
                    dropAddress = candidate.dropAddress ?: "Extracting drop...",
                    status = OrderStatus.PROCESSING,
                    matchedRule = "Evaluating ride filters...",
                    matchedRuleCode = "PROCESSING",
                    buttonFound = false,
                    buttonDetails = "Scanning accessibility tree...",
                    clickMethod = "Pending evaluation",
                    finalAction = "Evaluating ride filters...",
                    insertLatencyMs = insertLatencyMs
                )
            }
            reportMap[id] = report
            trimAndEmit()
        }
    }

    fun recordDecision(
        id: String,
        status: OrderStatus,
        ruleCode: String,
        exactRule: String,
        decisionLatencyMs: Long
    ) {
        synchronized(lock) {
            val existing = reportMap[id] ?: return
            val report = existing.copy(
                status = status,
                matchedRuleCode = ruleCode,
                matchedRule = exactRule,
                decisionLatencyMs = decisionLatencyMs,
                totalLatencyMs = if (status != OrderStatus.ACCEPTED) decisionLatencyMs else existing.totalLatencyMs,
                finalAction = when (status) {
                    OrderStatus.REJECTED -> "REJECTED: $exactRule"
                    OrderStatus.IGNORED -> "IGNORED: $exactRule"
                    OrderStatus.ACCEPTED -> "FILTERS PASSED: $exactRule"
                    else -> exactRule
                }
            )
            reportMap[id] = report
            trimAndEmit()
        }
    }

    fun recordAction(
        id: String,
        status: OrderStatus,
        buttonFound: Boolean,
        buttonDetails: String,
        clickMethod: String,
        finalAction: String,
        error: String? = null,
        actionLatencyMs: Long = 0L,
        totalLatencyMs: Long = 0L
    ) {
        synchronized(lock) {
            val existing = reportMap[id] ?: return
            val report = existing.copy(
                status = status,
                buttonFound = buttonFound,
                buttonDetails = buttonDetails,
                clickMethod = clickMethod,
                finalAction = finalAction,
                error = error,
                actionLatencyMs = actionLatencyMs,
                totalLatencyMs = totalLatencyMs
            )
            reportMap[id] = report
            trimAndEmit()
        }
    }

    fun clearAll() {
        synchronized(lock) {
            reportMap.clear()
            _recentReports.value = emptyList()
        }
    }

    private fun trimAndEmit() {
        val list = reportMap.values.toList().sortedByDescending { it.detectedAt }
        val trimmed = if (list.size > 50) list.take(50) else list
        _recentReports.value = trimmed
    }

    private fun formatDateTime(timestamp: Long): Pair<String, String> {
        val date = Date(timestamp)
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        return Pair(sdfDate.format(date), sdfTime.format(date))
    }
}
