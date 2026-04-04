package com.xaymaca.sit.ui.nav

sealed class Screen(val route: String) {
    object Network : Screen("network")
    object AddContact : Screen("add_contact")
    data class ContactDetail(val id: Long = 0L) : Screen("contact_detail/{contactId}") {
        companion object {
            const val ROUTE = "contact_detail/{contactId}"
            fun createRoute(id: Long) = "contact_detail/$id"
        }
    }
    object GroupList : Screen("groups")
    data class GroupDetail(val id: Long = 0L) : Screen("group_detail/{groupId}") {
        companion object {
            const val ROUTE = "group_detail/{groupId}"
            fun createRoute(id: Long) = "group_detail/$id"
        }
    }
    object Tickle : Screen("tickle")
    data class TickleEdit(val id: Long = -1L) : Screen("tickle_edit/{tickleId}?contactId={contactId}") {
        companion object {
            const val ROUTE = "tickle_edit/{tickleId}?contactId={contactId}"
            fun createRoute(id: Long = -1L) = "tickle_edit/$id"
            fun createRouteWithContact(contactId: Long) = "tickle_edit/-1?contactId=$contactId"
        }
    }
    object Compose : Screen("compose")
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
    object Import : Screen("import")
    object TemplateList : Screen("template_list")
    data class TemplateEdit(val id: Long = -1L) : Screen("template_edit/{templateId}") {
        companion object {
            const val ROUTE = "template_edit/{templateId}"
            fun createRoute(id: Long = -1L) = "template_edit/$id"
        }
    }
}
