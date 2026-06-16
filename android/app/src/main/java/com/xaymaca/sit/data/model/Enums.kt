package com.xaymaca.sit.data.model

enum class TickleFrequency(val label: String) {
    ONE_TIME("One time"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BIWEEKLY("Every 2 Weeks"),
    MONTHLY("Monthly"),
    BIMONTHLY("Every 2 Months"),
    QUARTERLY("Quarterly"),
    ANNUAL("Annual"),
    CUSTOM("Custom")
}

enum class TickleStatus(val label: String) {
    ACTIVE("Active"),
    SNOOZED("Snoozed"),
    COMPLETED("Completed")
}

enum class ImportSource(val label: String) {
    MANUAL("Manual"),
    IOS_CONTACTS("Phone Contacts"),
    LINKEDIN("LinkedIn")
}
