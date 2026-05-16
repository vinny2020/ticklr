package com.xaymaca.sit.data.repository

import com.xaymaca.sit.data.dao.MessageTemplateDao
import com.xaymaca.sit.data.model.MessageTemplate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageTemplateRepository @Inject constructor(
    private val messageTemplateDao: MessageTemplateDao
) {
    fun getAllTemplates(): Flow<List<MessageTemplate>> = messageTemplateDao.getAll()

    suspend fun getTemplateById(id: Long): MessageTemplate? = messageTemplateDao.getById(id)

    suspend fun count(): Int = messageTemplateDao.count()

    suspend fun insertTemplate(template: MessageTemplate): Long =
        messageTemplateDao.insert(template)

    suspend fun updateTemplate(template: MessageTemplate) = messageTemplateDao.update(template)

    suspend fun deleteTemplate(template: MessageTemplate) = messageTemplateDao.delete(template)
}
