package com.xaymaca.sit

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the pure logic extracted from GroupDetailScreen and GroupListScreen:
 *  - Toast message formatting (group name ≤20 chars vs truncation fallback)
 *  - Group name validation (non-empty, max 30 chars)
 */
class GroupUiLogicTest {

    // Mirrors the inline logic in GroupDetailScreen's onAdd lambda
    private fun toastMessage(contactName: String, groupName: String): String {
        val display = if (groupName.length <= 20) groupName else "group"
        return "$contactName added to $display"
    }

    // Mirrors canSave in iOS GroupEditSheet / enabled condition in Android CreateGroupDialog
    private fun isGroupNameValid(name: String): Boolean =
        name.isNotBlank() && name.length <= 30

    // --- Toast message format ---

    @Test
    fun `toast uses group name when shorter than 20 chars`() {
        assertEquals("Alice Smith added to Hiking Crew", toastMessage("Alice Smith", "Hiking Crew"))
    }

    @Test
    fun `toast uses group name at exactly 20 chars`() {
        val name = "A".repeat(20)
        assertEquals("Alice Smith added to $name", toastMessage("Alice Smith", name))
    }

    @Test
    fun `toast falls back to generic label when group name is 21 chars`() {
        val name = "A".repeat(21)
        assertEquals("Alice Smith added to group", toastMessage("Alice Smith", name))
    }

    @Test
    fun `toast falls back to generic label for long group names`() {
        assertEquals(
            "Bob Jones added to group",
            toastMessage("Bob Jones", "Very Long Group Name That Overflows")
        )
    }

    @Test
    fun `toast includes full contact name`() {
        val msg = toastMessage("María García-López", "Family")
        assertTrue(msg.startsWith("María García-López"))
    }

    @Test
    fun `toast format is exactly firstName lastName added to groupName`() {
        assertEquals("Jane Doe added to Work Friends", toastMessage("Jane Doe", "Work Friends"))
    }

    // --- Group name validation ---

    @Test
    fun `group name valid when non-empty and under 30 chars`() {
        assertTrue(isGroupNameValid("Hiking Crew"))
    }

    @Test
    fun `group name valid at exactly 30 chars`() {
        assertTrue(isGroupNameValid("A".repeat(30)))
    }

    @Test
    fun `group name invalid when empty string`() {
        assertFalse(isGroupNameValid(""))
    }

    @Test
    fun `group name invalid when blank whitespace only`() {
        assertFalse(isGroupNameValid("   "))
    }

    @Test
    fun `group name invalid at 31 chars`() {
        assertFalse(isGroupNameValid("A".repeat(31)))
    }

    @Test
    fun `group name invalid well above 30 chars`() {
        assertFalse(isGroupNameValid("A".repeat(50)))
    }

    @Test
    fun `group name valid with single character`() {
        assertTrue(isGroupNameValid("X"))
    }
}
