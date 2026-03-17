package com.xaymaca.sit.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "contact_group_cross_ref",
    primaryKeys = ["contactId", "groupId"],
    indices = [Index("groupId")]
)
data class ContactGroupCrossRef(
    val contactId: Long,
    val groupId: Long
)
