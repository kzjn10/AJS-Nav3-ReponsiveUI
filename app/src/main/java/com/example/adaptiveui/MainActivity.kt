package com.example.adaptiveui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.NavDisplay
import com.example.adaptiveui.ui.theme.AdaptiveUiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdaptiveUiTheme {
                MainScreen()
            }
        }
    }
}

private data class HomeItem(
    val id: Int,
    val title: String,
    val subtitle: String,
)

private sealed interface TopLevelDestination {
    data object Home : TopLevelDestination
    data object Search : TopLevelDestination
    data object Settings : TopLevelDestination
}

private const val ROUTE_HOME = "home"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_HOME_DETAIL_PREFIX = "homeDetail:"
private const val ROUTE_SETTINGS_DETAIL_PREFIX = "settingsDetail:"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun MainScreen(modifier: Modifier = Modifier) {
    val activity = LocalContext.current.findActivity() ?: return
    val windowSizeClass = calculateWindowSizeClass(activity)
    val coroutineScope = rememberCoroutineScope()

    val homeItems = remember {
        List(30) { index ->
            val id = index + 1
            HomeItem(
                id = id,
                title = "Feed item #$id",
                subtitle = "A responsive card that works on phones and tablets",
            )
        }
    }

    var homeBadgeCount by rememberSaveable { mutableIntStateOf(3) }
    val tabs = remember {
        listOf(
            TopLevelDestination.Home,
            TopLevelDestination.Search,
            TopLevelDestination.Settings,
        )
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = tabs.getOrElse(selectedTabIndex) { TopLevelDestination.Home }

    val startRoute = when (selectedTab) {
        TopLevelDestination.Home -> ROUTE_HOME
        TopLevelDestination.Search -> ROUTE_SEARCH
        TopLevelDestination.Settings -> ROUTE_SETTINGS
    }

    val backStack = rememberSaveable(selectedTabIndex) { mutableStateListOf(startRoute) }

    val currentRoute = backStack.lastOrNull() ?: startRoute
    val showBottomBar = currentRoute == ROUTE_HOME ||
        currentRoute == ROUTE_SEARCH ||
        currentRoute == ROUTE_SETTINGS

    val homeListState = rememberLazyListState()
    val homeGridState = rememberLazyGridState()
    val searchListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()

    val entries = rememberDecoratedNavEntries(backStack) { key ->
        NavEntry(key) {
            when {
                key == ROUTE_HOME -> HomeScreen(
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    items = homeItems,
                    listState = homeListState,
                    gridState = homeGridState,
                    onOpenDetail = { item -> backStack.add("$ROUTE_HOME_DETAIL_PREFIX${item.id}") }
                )

                key.startsWith(ROUTE_HOME_DETAIL_PREFIX) -> {
                    val id = key.removePrefix(ROUTE_HOME_DETAIL_PREFIX).toIntOrNull() ?: -1
                    val item = homeItems.firstOrNull { it.id == id }
                    HomeDetailScreen(
                        windowWidthSizeClass = windowSizeClass.widthSizeClass,
                        item = item,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                key == ROUTE_SEARCH -> SearchScreen(
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    listState = searchListState,
                )

                key == ROUTE_SETTINGS -> SettingsScreen(
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    listState = settingsListState,
                    onOpenDetail = { item ->
                        backStack.add("$ROUTE_SETTINGS_DETAIL_PREFIX$item")
                    },
                )

                key.startsWith(ROUTE_SETTINGS_DETAIL_PREFIX) -> {
                    val item = key.removePrefix(ROUTE_SETTINGS_DETAIL_PREFIX)
                    SettingsDetailScreen(
                        windowWidthSizeClass = windowSizeClass.widthSizeClass,
                        title = item,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                else -> SimpleScreen(title = "Unknown").Content()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn() + expandVertically(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut() + shrinkVertically()
            ) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                if (selectedTab == tab) {
                                    when (tab) {
                                        TopLevelDestination.Home -> {
                                            coroutineScope.launch {
                                                if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                                                    homeGridState.animateScrollToItem(0)
                                                } else {
                                                    homeListState.animateScrollToItem(0)
                                                }
                                            }
                                        }

                                        TopLevelDestination.Search -> {
                                            coroutineScope.launch {
                                                searchListState.animateScrollToItem(0)
                                            }
                                        }

                                        TopLevelDestination.Settings -> {
                                            coroutineScope.launch {
                                                settingsListState.animateScrollToItem(0)
                                            }
                                        }
                                    }
                                } else {
                                    selectedTabIndex = tabs.indexOf(tab)
                                }
                            },
                            label = {
                                Text(
                                    text = when (tab) {
                                        TopLevelDestination.Home -> "Home"
                                        TopLevelDestination.Search -> "Search"
                                        TopLevelDestination.Settings -> "Settings"
                                    }
                                )
                            },
                            icon = {
                                when (tab) {
                                    TopLevelDestination.Home -> {
                                        BadgedBox(
                                            badge = {
                                                if (homeBadgeCount > 0) {
                                                    Badge(
                                                        containerColor = Color.Red,
                                                        contentColor = Color.White,
                                                    ) {
                                                        Text(text = homeBadgeCount.toString())
                                                    }
                                                }
                                            }
                                        ) {
                                            androidx.compose.material3.Icon(
                                                imageVector = Icons.Filled.Home,
                                                contentDescription = "Home"
                                            )
                                        }
                                    }

                                    TopLevelDestination.Search -> androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search"
                                    )

                                    TopLevelDestination.Settings -> androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Settings"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            entries = entries,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

private class SimpleScreen(private val title: String) {
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        Column(modifier = modifier.fillMaxSize()) {
            Text(text = title)
            Text(text = "This screen is driven by Navigation 3")
        }
    }
}

@Composable
private fun HomeScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    items: List<HomeItem>,
    listState: LazyListState,
    gridState: LazyGridState,
    onOpenDetail: (HomeItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Home")

        when (windowWidthSizeClass) {
            WindowWidthSizeClass.Compact, WindowWidthSizeClass.Medium -> {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDetail(item) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = item.title)
                                Text(text = item.subtitle)
                            }
                        }
                    }
                }
            }

            WindowWidthSizeClass.Expanded -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 240.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDetail(item) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = item.title)
                                Text(text = "Grid layout on large screens")
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDetail(item) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = item.title)
                                Text(text = "Fallback layout")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeDetailScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    item: HomeItem?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 24.dp
        WindowWidthSizeClass.Expanded -> 32.dp
        else -> 16.dp
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = item?.title ?: "Item not found")
                    Text(text = item?.subtitle ?: "")
                    Text(text = "This is a detail page. Use the back button to return.")
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val allItems = remember {
        List(80) { index -> "Result ${index + 1}" }
    }
    val results = remember(query, allItems) {
        val q = query.trim()
        if (q.isEmpty()) allItems else allItems.filter { it.contains(q, ignoreCase = true) }
    }

    val horizontalPadding = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 24.dp
        WindowWidthSizeClass.Expanded -> 32.dp
        else -> 16.dp
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Search")

        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(text = "Type to filter results") },
        )

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(results) { title ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = title)
                        Text(text = "Query: ${query.ifBlank { "(none)" }}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    listState: LazyListState,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = remember {
        listOf(
            "Notifications",
            "Theme",
            "Privacy",
            "About",
        )
    }
    var selected by rememberSaveable { mutableStateOf(settings.first()) }

    when (windowWidthSizeClass) {
        WindowWidthSizeClass.Expanded -> {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.weight(1f).fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(settings) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selected = item }
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = item)
                                    Text(text = if (selected == item) "Selected" else "Tap to view")
                                }
                            }
                        }
                    }
                }

                Card(modifier = Modifier.weight(2f).fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = selected)
                        Text(text = "Details pane shown side-by-side on large screens")
                    }
                }
            }
        }

        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Settings")
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(settings) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = item
                                    onOpenDetail(item)
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = item)
                                Text(text = if (selected == item) "Selected" else "Tap to view")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsDetailScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 24.dp
        WindowWidthSizeClass.Expanded -> 32.dp
        else -> 16.dp
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = title)
                    Text(text = "Details")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AdaptiveUiTheme {
        MainScreen()
    }
}