package com.xaymaca.sit.ui.shared

import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.TickleFrequency

val TickleFrequency.displayNameResId: Int
    get() = when (this) {
        TickleFrequency.DAILY -> R.string.frequency_daily
        TickleFrequency.WEEKLY -> R.string.frequency_weekly
        TickleFrequency.BIWEEKLY -> R.string.frequency_biweekly
        TickleFrequency.MONTHLY -> R.string.frequency_monthly
        TickleFrequency.BIMONTHLY -> R.string.frequency_bimonthly
        TickleFrequency.QUARTERLY -> R.string.frequency_quarterly
        TickleFrequency.CUSTOM -> R.string.frequency_custom
    }
