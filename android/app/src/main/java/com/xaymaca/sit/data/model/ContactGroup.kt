package com.xaymaca.sit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_groups")
data class ContactGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val emoji: String = "👥",
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Stable identifier for one of the canonical warm-redesign categories
     * (`family`, `friends`, `work`, `milestones`, `community`). Null for
     * user-created groups. See WarmCategory.id.
     */
    val categoryId: String? = null,
)
