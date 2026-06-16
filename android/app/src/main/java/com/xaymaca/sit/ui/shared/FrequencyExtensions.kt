package com.xaymaca.sit.ui.shared

import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.TickleFrequency

val TickleFrequency.displayNameResId: Int
    get() = when (this) {
        TickleFrequency.ONE_TIME -> R.string.frequency_one_time
        TickleFrequency.DAILY -> R.string.frequency_daily
        TickleFrequency.WEEKLY -> R.string.frequency_weekly
        TickleFrequency.BIWEEKLY -> R.string.frequency_biweekly
        TickleFrequency.MONTHLY -> R.string.frequency_monthly
        TickleFrequency.BIMONTHLY -> R.string.frequency_bimonthly
        TickleFrequency.QUARTERLY -> R.string.frequency_quarterly
        TickleFrequency.ANNUAL -> R.string.frequency_annual
        TickleFrequency.CUSTOM -> R.string.frequency_custom
    }
