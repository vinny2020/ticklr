package com.xaymaca.sit

import com.xaymaca.sit.ui.nav.ImportExit
import com.xaymaca.sit.ui.nav.ImportOrigin
import com.xaymaca.sit.ui.nav.Screen
import com.xaymaca.sit.ui.nav.importExitFor
import com.xaymaca.sit.ui.nav.importOriginFor
import com.xaymaca.sit.ui.nav.startDestinationFor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ScreenRouteTest {

    @Test
    fun `createRouteWithContact produces tickleId of -1 with contactId query param`() {
        val route = Screen.TickleEdit.createRouteWithContact(contactId = 42L)
        assertEquals("tickle_edit/-1?contactId=42", route)
    }

    @Test
    fun `createRouteWithContact works for any contactId`() {
        assertEquals("tickle_edit/-1?contactId=1", Screen.TickleEdit.createRouteWithContact(contactId = 1L))
        assertEquals("tickle_edit/-1?contactId=999", Screen.TickleEdit.createRouteWithContact(contactId = 999L))
    }

    @Test
    fun `createRoute without contactId produces plain path`() {
        assertEquals("tickle_edit/7", Screen.TickleEdit.createRoute(id = 7L))
    }

    @Test
    fun `createRoute defaults tickleId to -1 for new tickle`() {
        assertEquals("tickle_edit/-1", Screen.TickleEdit.createRoute())
    }

    @Test
    fun `ROUTE template contains both tickleId and optional contactId placeholders`() {
        assertTrue(Screen.TickleEdit.ROUTE.contains("{tickleId}"))
        assertTrue(Screen.TickleEdit.ROUTE.contains("{contactId}"))
    }

    @Test
    fun `createRouteWithContact and createRoute produce distinct routes`() {
        val withContact = Screen.TickleEdit.createRouteWithContact(contactId = 5L)
        val withoutContact = Screen.TickleEdit.createRoute()
        assertNotEquals(withContact, withoutContact)
    }

    @Test
    fun `createRouteWithContact always uses -1 as tickle placeholder regardless of contactId`() {
        val route = Screen.TickleEdit.createRouteWithContact(contactId = 100L)
        assertTrue(route.startsWith("tickle_edit/-1"))
    }

    // --- Compose route + deep link (TIC-82) ---
    // The compose route carries an optional contactId (recipient pre-select) and
    // an optional reminderId (drives the mark-done prompt on return). The -1L
    // sentinel means "absent" and is what NavGraph filters out via takeIf.

    @Test
    fun `compose route template contains both contactId and reminderId placeholders`() {
        assertTrue(Screen.Compose.ROUTE.contains("{${Screen.Compose.ARG_CONTACT_ID}}"))
        assertTrue(Screen.Compose.ROUTE.contains("{${Screen.Compose.ARG_REMINDER_ID}}"))
    }

    @Test
    fun `compose createRoute embeds both contact and reminder ids`() {
        assertEquals(
            "compose?contactId=7&reminderId=42",
            Screen.Compose.createRoute(contactId = 7L, reminderId = 42L),
        )
    }

    @Test
    fun `compose createRoute falls back to -1 sentinels when ids are null`() {
        assertEquals(
            "compose?contactId=-1&reminderId=-1",
            Screen.Compose.createRoute(),
        )
    }

    @Test
    fun `compose createRoute keeps a contact but no reminder`() {
        assertEquals(
            "compose?contactId=5&reminderId=-1",
            Screen.Compose.createRoute(contactId = 5L),
        )
    }

    @Test
    fun `compose deep link uri includes the reminderId when present`() {
        assertEquals(
            "ticklr://compose?contactId=5&reminderId=42",
            Screen.Compose.deepLinkUri(contactId = 5L, reminderId = 42L),
        )
    }

    @Test
    fun `compose deep link uri omits reminderId when null but keeps contact sentinel`() {
        assertEquals(
            "ticklr://compose?contactId=-1",
            Screen.Compose.deepLinkUri(contactId = null, reminderId = null),
        )
    }

    @Test
    fun `compose deep link pattern matches the DEEP_LINK_PATTERN arg names`() {
        assertTrue(Screen.Compose.DEEP_LINK_PATTERN.contains("{${Screen.Compose.ARG_CONTACT_ID}}"))
        assertTrue(Screen.Compose.DEEP_LINK_PATTERN.contains("{${Screen.Compose.ARG_REMINDER_ID}}"))
    }

    // --- Start-destination selection (TIC-64) ---
    // The graph's start destination is chosen once from the persisted onboarding
    // flag. A completed onboarding must land on Tickle; an incomplete one on
    // Onboarding. Reading this reactively mid-session rebuilt the graph and popped
    // the back stack, dumping the "Add my first contact" user on the wrong screen.

    @Test
    fun `completed onboarding starts on the Tickle list`() {
        assertEquals(Screen.Tickle.route, startDestinationFor(onboardingComplete = true))
    }

    @Test
    fun `incomplete onboarding starts on the Onboarding screen`() {
        assertEquals(Screen.Onboarding.route, startDestinationFor(onboardingComplete = false))
    }

    @Test
    fun `start destination differs between onboarding states`() {
        assertNotEquals(
            startDestinationFor(onboardingComplete = true),
            startDestinationFor(onboardingComplete = false)
        )
    }

    // --- Import route + entry-context (TIC-94) ---
    // Import is reached from three places: onboarding (no back stack worth
    // keeping) and Settings/Network overflow (mid-session, real back stack
    // underneath). The `origin` nav arg tells NavGraph which behavior applies.

    @Test
    fun `import route template contains the origin placeholder`() {
        assertTrue(Screen.Import.ROUTE.contains("{${Screen.Import.ARG_ORIGIN}}"))
    }

    @Test
    fun `import createRoute embeds the onboarding origin`() {
        assertEquals(
            "import?origin=onboarding",
            Screen.Import.createRoute(Screen.Import.ORIGIN_ONBOARDING),
        )
    }

    @Test
    fun `import createRoute embeds the in-app origin`() {
        assertEquals(
            "import?origin=inApp",
            Screen.Import.createRoute(Screen.Import.ORIGIN_IN_APP),
        )
    }

    @Test
    fun `import createRoute defaults to in-app origin`() {
        assertEquals(Screen.Import.createRoute(Screen.Import.ORIGIN_IN_APP), Screen.Import.createRoute())
    }

    @Test
    fun `import origin parses the onboarding sentinel`() {
        assertEquals(ImportOrigin.Onboarding, importOriginFor(Screen.Import.ORIGIN_ONBOARDING))
    }

    @Test
    fun `import origin parses the in-app sentinel`() {
        assertEquals(ImportOrigin.InApp, importOriginFor(Screen.Import.ORIGIN_IN_APP))
    }

    @Test
    fun `import origin defaults to in-app for a null arg`() {
        // A missing origin arg must never fall back to onboarding's
        // stack-wiping behavior — in-app (popBackStack) is the safe default.
        assertEquals(ImportOrigin.InApp, importOriginFor(null))
    }

    @Test
    fun `import origin defaults to in-app for an unrecognized value`() {
        assertEquals(ImportOrigin.InApp, importOriginFor("bogus"))
    }

    @Test
    fun `onboarding origin resets to a fresh tab on completion`() {
        assertEquals(ImportExit.FreshStart, importExitFor(ImportOrigin.Onboarding))
    }

    @Test
    fun `in-app origin returns to the caller on completion`() {
        assertEquals(ImportExit.ReturnToCaller, importExitFor(ImportOrigin.InApp))
    }
}
