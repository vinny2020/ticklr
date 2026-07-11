package com.xaymaca.sit.ui.nav

sealed class Screen(val route: String) {
    object Network : Screen("network") {
        const val ARG_FOCUS_CONTACT_ID = "focusContactId"

        /**
         * TIC-96: optional query param carrying a contact to land on directly in
         * the expanded-width two-pane detail slot. Used by cross-section jumps
         * (e.g. tapping a group member in GroupsPane) that target Network instead
         * of falling back to the full-screen ContactDetail route — keeping the
         * jump inside the pane world. Defaults to the "-1 absent" sentinel, same
         * convention as Compose/TickleEdit's optional args.
         */
        const val ROUTE = "network?focusContactId={focusContactId}"

        fun createRouteFocusingContact(contactId: Long) = "network?focusContactId=$contactId"
    }
    object AddContact : Screen("add_contact")
    data class ContactDetail(val id: Long = 0L) : Screen("contact_detail/{contactId}") {
        companion object {
            const val ROUTE = "contact_detail/{contactId}"
            fun createRoute(id: Long) = "contact_detail/$id"
        }
    }
    object GroupList : Screen("groups")
    data class GroupDetail(val id: Long = 0L) : Screen("group_detail/{groupId}?openAdd={openAdd}") {
        companion object {
            const val ROUTE = "group_detail/{groupId}?openAdd={openAdd}"
            const val ARG_OPEN_ADD = "openAdd"
            fun createRoute(id: Long) = "group_detail/$id"

            /**
             * TIC-88 create-with-members: after a new group is created on the
             * Group list, land straight in its detail with the Add Members sheet
             * already open. `openAdd=true` drives that one-shot auto-open.
             */
            fun createRouteWithAddMembers(id: Long) = "group_detail/$id?openAdd=true"
        }
    }
    object Tickle : Screen("tickle")
    data class TickleEdit(val id: Long = -1L) : Screen("tickle_edit/{tickleId}?contactId={contactId}&groupId={groupId}") {
        companion object {
            const val ROUTE = "tickle_edit/{tickleId}?contactId={contactId}&groupId={groupId}"
            fun createRoute(id: Long = -1L) = "tickle_edit/$id"
            fun createRouteWithContact(contactId: Long) = "tickle_edit/-1?contactId=$contactId"

            /**
             * TIC-88 group tickle creation: opens a new tickle bound to a group
             * (contactId stays absent). The `groupId` query mirrors `contactId`;
             * both nav args default to the -1 "absent" sentinel, so a route that
             * carries only one still parses.
             */
            fun createRouteWithGroup(groupId: Long) = "tickle_edit/-1?groupId=$groupId"
        }
    }
    object Compose : Screen("compose") {
        const val ARG_CONTACT_ID = "contactId"
        const val ARG_REMINDER_ID = "reminderId"

        /**
         * In-app navigation route. Both query params are optional: `contactId`
         * pre-selects the recipient; `reminderId` (TIC-82) carries the due
         * tickle so the app can prompt "mark done?" on return from the SMS
         * handoff. Reaching Compose from the tab bar supplies neither.
         */
        const val ROUTE = "compose?contactId={contactId}&reminderId={reminderId}"

        fun createRoute(contactId: Long? = null, reminderId: Long? = null): String =
            "compose?contactId=${contactId ?: -1L}&reminderId=${reminderId ?: -1L}"

        /**
         * Deep-link target for tickle reminder notifications (TIC-35). Tapping a
         * reminder opens Compose pre-addressed to the contact. Both query params
         * are optional — a reminder with no contact opens plain Compose, and
         * `reminderId` (TIC-82) rides along so the mark-done prompt can fire.
         */
        const val DEEP_LINK_PATTERN = "ticklr://compose?contactId={contactId}&reminderId={reminderId}"
        fun deepLinkUri(contactId: Long?, reminderId: Long? = null): String {
            val base = "ticklr://compose?contactId=${contactId ?: -1L}"
            return if (reminderId != null) "$base&reminderId=$reminderId" else base
        }
    }
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
    object Import : Screen("import") {
        const val ARG_ORIGIN = "origin"

        /** First-run flow — no meaningful back stack to preserve on completion. */
        const val ORIGIN_ONBOARDING = "onboarding"

        /** Reached mid-session from Settings or the Network overflow menu — the
         * user's existing back stack must survive completion/skip. */
        const val ORIGIN_IN_APP = "inApp"

        /**
         * In-app navigation route. `origin` (TIC-94) tells NavGraph whether a
         * completed/skipped import should reset to a fresh tab (onboarding) or
         * simply return to whatever screen launched it (in-app). Defaults to
         * the non-destructive in-app behavior if ever omitted.
         */
        const val ROUTE = "import?origin={origin}"

        fun createRoute(origin: String = ORIGIN_IN_APP): String = "import?origin=$origin"
    }
    object TemplateList : Screen("template_list")
    data class TemplateEdit(val id: Long = -1L) : Screen("template_edit/{templateId}") {
        companion object {
            const val ROUTE = "template_edit/{templateId}"
            fun createRoute(id: Long = -1L) = "template_edit/$id"
        }
    }
}
