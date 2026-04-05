# Feature: Prevent Duplicate Group Names

## What to Build

Validate group names for uniqueness at every creation and rename entry point. When a user
types a name that already exists (case-insensitive), the name field should show an error
state and the confirm button should be disabled. No silent overwriting, no DB-level crash —
just clear inline feedback.

---

## Root Cause

`ContactRepository.insertGroup()` and `updateGroup()` have no uniqueness constraint.
All group creation dialogs accept any non-blank name and write straight to the DB.
There is also no unique index on `ContactGroup.name` in the Room schema.

---

## Strategy

Add one helper to `GroupViewModel` that all dialogs can call. Pass a `isNameTaken` lambda
into each private dialog composable — keeps dialogs dumb, logic in the ViewModel.

For **rename** (edit), exclude the group's own current name so "Friends → Friends" is
still allowed.

---

## Change 1 — `GroupViewModel.kt`

Add this function after `createGroup()`:

```kotlin
/**
 * Returns true if any existing group has the same name (case-insensitive).
 * Pass [excludeId] when editing so the group's own current name is not flagged.
 */
fun isGroupNameTaken(name: String, excludeId: Long = -1L): Boolean {
    val trimmed = name.trim()
    return groups.value.any {
        it.name.equals(trimmed, ignoreCase = true) && it.id != excludeId
    }
}
```

---

## Change 2 — `GroupListScreen.kt` — `CreateGroupDialog`

`CreateGroupDialog` currently takes `onDismiss` and `onCreate` only. Add an `isNameTaken`
lambda param and wire up the error state.

**Signature change:**
```kotlin
// Before:
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
)

// After:
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
    isNameTaken: (String) -> Boolean
)
```

**Inside the composable**, derive a `isDuplicate` flag from the current name:

```kotlin
val isDuplicate = name.trim().isNotBlank() && isNameTaken(name)
```

**Update the name `OutlinedTextField`** to show the error:

```kotlin
OutlinedTextField(
    value = name,
    onValueChange = { if (it.length <= 30) name = it },
    label = { Text("Group Name") },
    singleLine = true,
    isError = isDuplicate,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    supportingText = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isDuplicate) {
                Text(
                    "Name already exists",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Text(
                text = "${name.length} / 30",
                style = MaterialTheme.typography.bodySmall,
                color = if (name.length >= 30) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
)
```

**Update the Create button** — disable when duplicate:

```kotlin
TextButton(
    onClick = { if (name.isNotBlank() && !isDuplicate) onCreate(name, emoji) },
    enabled = name.isNotBlank() && !isDuplicate
) {
    Text("Create", color = Cobalt)
}
```

**Update the call site** in `GroupListScreen` (where `showCreateDialog` is shown):

```kotlin
if (showCreateDialog) {
    CreateGroupDialog(
        onDismiss = { showCreateDialog = false },
        onCreate = { name, emoji ->
            viewModel.createGroup(name, emoji)
            showCreateDialog = false
        },
        isNameTaken = { viewModel.isGroupNameTaken(it) }
    )
}
```

---

## Change 3 — `GroupDetailScreen.kt` — `EditGroupDialog`

`EditGroupDialog` currently takes `group`, `onDismiss`, `onSave`. Add `isNameTaken`.

**Signature change:**
```kotlin
// Before:
private fun EditGroupDialog(
    group: ContactGroup,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit
)

// After:
private fun EditGroupDialog(
    group: ContactGroup,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit,
    isNameTaken: (String) -> Boolean
)
```

**Inside the composable**, derive `isDuplicate` — note we exclude the group's own name
so renaming to itself is allowed:

```kotlin
val isDuplicate = name.trim().isNotBlank() &&
    !name.trim().equals(group.name.trim(), ignoreCase = true) &&
    isNameTaken(name)
```

Apply identical `isError`, `supportingText`, and button-disabled logic as in Change 2.

**Update `canSave`:**
```kotlin
val canSave = name.trim().isNotEmpty() && name.length <= 30 && !isDuplicate
```

**Update the call site** in `GroupDetailScreen` (where `showEditDialog` is shown):

```kotlin
if (showEditDialog && currentGroup != null) {
    EditGroupDialog(
        group = currentGroup,
        onDismiss = { showEditDialog = false },
        onSave = { name, emoji ->
            viewModel.updateGroup(currentGroup.copy(name = name, emoji = emoji))
            showEditDialog = false
        },
        isNameTaken = { viewModel.isGroupNameTaken(it, excludeId = currentGroup.id) }
    )
}
```

---

## Change 4 — `ContactDetailScreen.kt` — inline "Create New Group" field

`ContactDetailScreen` has a `ModalBottomSheet` with an inline `OutlinedTextField` +
"Create" button for creating a new group directly from a contact. Find that inline
create-group section and apply the same pattern:

1. Derive `isDuplicateInline` from the inline text field's value:
   ```kotlin
   val isDuplicateInline = newGroupName.trim().isNotBlank() &&
       groupViewModel.isGroupNameTaken(newGroupName)  // or equivalent ViewModel call
   ```
2. Set `isError = isDuplicateInline` on that `OutlinedTextField`.
3. Add a `supportingText` showing "Name already exists" when `isDuplicateInline`.
4. Disable the inline "Create" button when `isDuplicateInline`.

> **Note:** `ContactDetailScreen` uses either `NetworkViewModel` or `GroupViewModel`
> for group operations — check which ViewModel is injected there and call
> `isGroupNameTaken()` on it, or add a matching helper if needed.

---

## Change 5 — `TickleEditScreen.kt` — new Create Group dialog

If the task in `tasks/feature-tickle-empty-groups-prompt.md` has already been applied,
the new `AlertDialog` for inline group creation in the Group-tab empty state also needs
the same dupe check:

1. Derive `isDuplicate` from `newGroupName` using `groupViewModel.isGroupNameTaken(newGroupName)`.
2. Set `isError = isDuplicate` on the `OutlinedTextField`.
3. Add `supportingText` showing "Name already exists" when true.
4. Disable the "Create" button when `isDuplicate`.

---

## Files to Modify
- `app/src/main/java/com/xaymaca/sit/ui/groups/GroupViewModel.kt` — add `isGroupNameTaken()`
- `app/src/main/java/com/xaymaca/sit/ui/groups/GroupListScreen.kt` — `CreateGroupDialog`
- `app/src/main/java/com/xaymaca/sit/ui/groups/GroupDetailScreen.kt` — `EditGroupDialog`
- `app/src/main/java/com/xaymaca/sit/ui/network/ContactDetailScreen.kt` — inline create field
- `app/src/main/java/com/xaymaca/sit/ui/tickle/TickleEditScreen.kt` — if empty-groups task applied

## Files to Leave Alone
- `ContactGroupDao.kt` — no DB unique constraint needed; client-side validation is sufficient
- `ContactRepository.kt` — no changes needed
- All other screens

---

## Verification
1. Groups list → FAB → type a name that already exists → "Name already exists" error appears,
   Create button disabled. Type a unique name → error clears, Create enabled. ✓
2. Group detail → Edit (pencil icon) → change name to an existing group's name →
   "Name already exists" error, Save disabled. Change back to original name → error clears. ✓
3. Contact detail → "Add to Group" → "Create New Group" → type duplicate name →
   inline Create button disabled, error shown. ✓
4. Comparison is case-insensitive: "friends" is flagged as duplicate of "Friends". ✓
5. Renaming a group to its own name (no change) is allowed — no false positive. ✓
