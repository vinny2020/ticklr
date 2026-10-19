package com.xaymaca.sit.ui.tutorials

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.xaymaca.sit.R
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.ui.theme.LocalIsAppDark
import com.xaymaca.sit.ui.theme.WarmHeadingFont
import com.xaymaca.sit.ui.theme.WarmTheme
import com.xaymaca.sit.ui.theme.Warmth
import kotlinx.coroutines.launch

/** All images were captured from the native Android app with bundled fake contacts.
 * Screenshots are display-only; a guide never seeds data or invokes an import. */
data class TutorialPage(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @DrawableRes val screenshot: Int,
    val focus: Float,
)

enum class TutorialTopic(@StringRes val title: Int, @StringRes val action: Int) {
    Phone(R.string.tutorial_phone_title, R.string.tutorial_phone_action),
    LinkedIn(R.string.tutorial_linkedin_title, R.string.tutorial_import_action),
    Tickle(R.string.tutorial_tickle_title, R.string.tutorial_tickle_action),
    Groups(R.string.tutorial_groups_title, R.string.tutorial_groups_action);

    val pages: List<TutorialPage> get() = when (this) {
        Phone -> listOf(
            TutorialPage(R.string.tutorial_phone_1_title, R.string.tutorial_phone_1_body, R.drawable.tutorial_android_import, -.65f),
            TutorialPage(R.string.tutorial_phone_2_title, R.string.tutorial_phone_2_body, R.drawable.tutorial_android_import, -.65f),
            TutorialPage(R.string.tutorial_phone_3_title, R.string.tutorial_phone_3_body, R.drawable.tutorial_android_network, -.65f),
        )
        LinkedIn -> listOf(
            TutorialPage(R.string.tutorial_linkedin_1_title, R.string.tutorial_linkedin_1_body, R.drawable.tutorial_android_import, -.65f),
            TutorialPage(R.string.tutorial_linkedin_2_title, R.string.tutorial_linkedin_2_body, R.drawable.tutorial_android_import, 0f),
            TutorialPage(R.string.tutorial_linkedin_3_title, R.string.tutorial_linkedin_3_body, R.drawable.tutorial_android_network, -.65f),
        )
        Tickle -> listOf(
            TutorialPage(R.string.tutorial_tickle_1_title, R.string.tutorial_tickle_1_body, R.drawable.tutorial_android_profile, 0f),
            TutorialPage(R.string.tutorial_tickle_2_title, R.string.tutorial_tickle_2_body, R.drawable.tutorial_android_birthday, 0f),
            TutorialPage(R.string.tutorial_tickle_3_title, R.string.tutorial_tickle_3_body, R.drawable.tutorial_android_tickle, .35f),
        )
        Groups -> listOf(
            TutorialPage(R.string.tutorial_groups_1_title, R.string.tutorial_groups_1_body, R.drawable.tutorial_android_groups, -.7f),
            TutorialPage(R.string.tutorial_groups_2_title, R.string.tutorial_groups_2_body, R.drawable.tutorial_android_membership, .65f),
            TutorialPage(R.string.tutorial_groups_3_title, R.string.tutorial_groups_3_body, R.drawable.tutorial_android_new_group, -.2f),
        )
    }
}

internal class TutorialHistory(context: Context) {
    private val preferences = context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)
    fun isDismissed(topic: TutorialTopic) = preferences.getBoolean(key(topic), false)
    fun dismiss(topic: TutorialTopic) { preferences.edit().putBoolean(key(topic), true).apply() }
    private fun key(topic: TutorialTopic) = "tutorial.dismissed.${topic.name}"
}

@Composable
fun TutorialInvitation(topic: TutorialTopic, onAction: (TutorialTopic) -> Unit) {
    val context = LocalContext.current
    val history = remember(context) { TutorialHistory(context) }
    var dismissed by remember(topic) { mutableStateOf(history.isDismissed(topic)) }
    var open by rememberSaveable(topic.name) { mutableStateOf(false) }
    if (!dismissed) {
        val palette = WarmTheme.palette()
        Surface(color = palette.cardBg, shape = RoundedCornerShape(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { open = true },
                    modifier = Modifier.weight(1f).testTag("tutorial.invitation.${topic.name}"),
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(topic.title), modifier = Modifier.fillMaxWidth())
                }
                IconButton(onClick = { history.dismiss(topic); dismissed = true }) {
                    Icon(Icons.Default.Close, stringResource(R.string.tutorial_dismiss_suggestion))
                }
            }
        }
    }
    if (open) {
        TutorialDialog(topic,
            onDismiss = { history.dismiss(topic); open = false; dismissed = true },
            onAction = { history.dismiss(topic); open = false; dismissed = true; onAction(topic) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialLibraryScreen(onBack: () -> Unit, onAction: (TutorialTopic) -> Unit) {
    val context = LocalContext.current
    val history = remember(context) { TutorialHistory(context) }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = TutorialTopic.entries.firstOrNull { it.name == selectedName }
    val palette = WarmTheme.palette()
    Scaffold(
        containerColor = palette.paper,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tutorial_library)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.paper),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(stringResource(R.string.tutorial_language), color = palette.ink2)
            Spacer(Modifier.height(16.dp))
            TutorialTopic.entries.forEach { topic ->
                ListItem(
                    headlineContent = { Text(stringResource(topic.title), color = palette.ink) },
                    leadingContent = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = palette.paper),
                    modifier = Modifier.clickable { selectedName = topic.name }
                        .testTag("tutorial.library.${topic.name}"),
                )
                HorizontalDivider(color = palette.cardBorder)
            }
        }
    }
    selected?.let { topic ->
        TutorialDialog(topic,
            onDismiss = { history.dismiss(topic); selectedName = null },
            onAction = { history.dismiss(topic); selectedName = null; onAction(topic) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TutorialDialog(topic: TutorialTopic, onDismiss: () -> Unit, onAction: () -> Unit) {
    val pages = remember(topic) { topic.pages }
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val palette = WarmTheme.palette()
    var fullScreenshot by rememberSaveable { mutableStateOf<Int?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        MatchDialogWindow()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = palette.paper,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(topic.title), style = MaterialTheme.typography.titleMedium) },
                    actions = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("tutorial.close")) {
                            Icon(Icons.Default.Close, stringResource(R.string.tutorial_close))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.paper),
                )
            },
            bottomBar = {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { scope.launch { pager.scrollToPage(pager.currentPage - 1) } },
                            enabled = pager.currentPage > 0,
                            modifier = Modifier.testTag("tutorial.back"),
                        ) { Text(stringResource(R.string.common_back)) }
                        Spacer(Modifier.weight(1f))
                        Text(stringResource(R.string.tutorial_progress, pager.currentPage + 1, pages.size),
                            color = palette.ink2, modifier = Modifier.testTag("tutorial.progress"))
                    }
                    Button(
                        onClick = {
                            if (pager.currentPage == pages.lastIndex) onAction()
                            else scope.launch { pager.scrollToPage(pager.currentPage + 1) }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tutorial.next"),
                    ) {
                        Text(stringResource(if (pager.currentPage == pages.lastIndex) topic.action else R.string.tutorial_next))
                    }
                }
            },
        ) { insets ->
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().padding(insets)) { index ->
                val page = pages[index]
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(stringResource(page.title), style = WarmHeadingFont.style(32.sp, Warmth.Subtle), color = palette.ink)
                    Text(stringResource(page.body), style = MaterialTheme.typography.bodyLarge, color = palette.ink2)
                    Surface(shape = RoundedCornerShape(20.dp), color = palette.cardBg) {
                        Image(
                            painter = painterResource(page.screenshot),
                            contentDescription = stringResource(R.string.tutorial_screenshot_description, stringResource(page.title)),
                            modifier = Modifier.fillMaxWidth().height(280.dp).clickable { fullScreenshot = page.screenshot },
                            contentScale = ContentScale.Crop,
                            alignment = BiasAlignment(0f, page.focus),
                        )
                    }
                    TextButton(onClick = { fullScreenshot = page.screenshot }, modifier = Modifier.testTag("tutorial.screenshot.$index")) {
                        Text(stringResource(R.string.tutorial_view_screenshot))
                    }
                    Text(stringResource(R.string.tutorial_sample), style = MaterialTheme.typography.labelSmall, color = palette.ink2)
                }
            }
        }
    }
    fullScreenshot?.let { image ->
        Dialog(onDismissRequest = { fullScreenshot = null }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            MatchDialogWindow()
        Surface(color = palette.paper, modifier = Modifier.fillMaxSize()) {
                Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { fullScreenshot = null }) {
                            Icon(Icons.Default.Close, stringResource(R.string.tutorial_close_screenshot))
                        }
                    }
                    Image(painterResource(image), contentDescription = stringResource(R.string.tutorial_full_screenshot),
                        modifier = Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Fit)
                }
            }
        }
    }
}

// Older Compose dialogs measure against screen dp on newer Android versions.
// Fit the window to the full display, then let Scaffold handle safe-area insets.
@Composable
private fun MatchDialogWindow() {
    val view = LocalView.current
    val dark = LocalIsAppDark.current
    val window = generateSequence(view) { it.parent as? View }
        .filterIsInstance<DialogWindowProvider>().firstOrNull()?.window
    SideEffect {
        window?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it.attributes = it.attributes.apply { setFitInsetsTypes(0) }
            }
            it.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            WindowCompat.getInsetsController(it, it.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
}
