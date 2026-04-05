# Feature: Prompt to Create Group When None Exist in TickleEditScreen

## What to Build

When a user switches to the **Group tab** in `TickleEditScreen` and no groups exist yet,
instead of showing a blank list, show an empty state with a **"Create Group"** button.
Tapping it opens an inline `AlertDialog` to create a group without leaving the screen
(preserving all form state: contact, frequency, note, date).

---

## Why This Approach

`TickleEditScreen` already injects `GroupViewModel`, so `groups` and `createGroup()` are
both available with no new wiring. Navigating away would lose form state. An inline dialog
matches the existing pattern used in `ContactDetailScreen`'s "Add to Group" bottom sheet.

---

## Exact Change — `TickleEditScreen.kt`

### 1. Add dialog state at the top of the composable (alongside existing `var` declarations)

```kotlin
var showCreateGroupDialog by remember { mutableStateOf(false) }
var newGroupName by remember { mutableStateOf("") }
```

### 2. Replace the empty Group tab body

Find this block (currently inside the `else` branch of `if (selectedTab == 0)`, lines ~247–282):

```kotlin
} else {
    item {
        Text(
            "GROUP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (selectedGroup != null) {
        item {
            AssistChip(
                onClick = { selectedGroup = null },
                label = { Text("${selectedGroup!!.emoji} ${selectedGroup!!.name}") },
                trailingIcon = null
            )
        }
    } else {
        items(groups) { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedGroup = group }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.emoji,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(group.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
```

Replace with:

```kotlin
} else {
    item {
        Text(
            "GROUP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (selectedGroup != null) {
        item {
            AssistChip(
                onClick = { selectedGroup = null },
                label = { Text("${selectedGroup!!.emoji} ${selectedGroup!!.name}") },
                trailingIcon = null
            )
        }
    } else if (groups.isEmpty()) {
        // Empty state — no groups exist yet
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "No groups yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Groups let you tickle everyone on a team or in a circle at once.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = { showCreateGroupDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Cobalt),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create a Group")
                }
            }
        }
    } else {
        items(groups) { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedGroup = group }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.emoji,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(group.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
```

### 3. Add the Create Group dialog

Place this **outside** the `Box`/`Scaffold` (alongside the existing `TicklrToast`), at the
bottom of the composable before the closing `}`:

```kotlin
if (showCreateGroupDialog) {
    AlertDialog(
        onDismissRequest = {
            showCreateGroupDialog = false
            newGroupName = ""
        },
        title = { Text("New Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { if (it.length <= 30) newGroupName = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    supportingText = { Text("${newGroupName.length}/30") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = newGroupName.trim()
                    if (name.isNotBlank()) {
                        groupViewModel.createGroup(name, "👥")
                        showCreateGroupDialog = false
                        newGroupName = ""
                        // Switch to group tab so the new group appears immediately
                        selectedTab = 1
                    }
                },
                enabled = newGroupName.isNotBlank()
            ) {
                Text("Create", color = Cobalt)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                showCreateGroupDialog = false
                newGroupName = ""
            }) {
                Text("Cancel")
            }
        }
    )
}
```

---

## GroupViewModel note

`GroupViewModel.createGroup(name: String, emoji: String)` already exists — use `"👥"` as
the default emoji (same default used elsewhere in the app). No ViewModel changes needed.

---

## Files to Modify
- `app/src/main/java/com/xaymaca/sit/ui/tickle/TickleEditScreen.kt` only

## Files to Leave Alone
- `GroupViewModel.kt` — `createGroup()` already exists, no changes needed
- `NavGraph.kt` — no new routes or lambdas needed
- `Screen.kt` — no new routes needed

---

## Verification
1. Open app with no groups. Navigate to Tickle → New Tickle → tap "Group" tab.
   → Empty state appears: "No groups yet" message + "Create a Group" button. ✓
2. Tap "Create a Group" → dialog opens. Type a name (up to 30 chars) → "Create" enabled. ✓
3. Tap "Create" → dialog dismisses, new group appears in the list on the Group tab. ✓
4. Tap the new group → it becomes the `selectedGroup` chip. Save button enables. ✓
5. With groups present: Group tab shows the list as before, no empty state shown. ✓
6. Form state (contact on Contact tab, frequency, note) is preserved through dialog open/close. ✓
