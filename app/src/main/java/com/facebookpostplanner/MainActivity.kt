package com.facebookpostplanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.facebook.CallbackManager
import com.facebookpostplanner.ui.create.CreateScreen
import com.facebookpostplanner.ui.insights.InsightsScreen
import com.facebookpostplanner.ui.page.PageScreen
import com.facebookpostplanner.ui.publish.PublishScreen
import com.facebookpostplanner.ui.theme.PlannerTheme

/**
 * Bottom nav: Create / Publish / Insights only. No Community tab.
 * Publish is home. FAB goes to Create.
 *
 * The selected tab is read from the NavController's back stack, not from a
 * separate remembered variable — that is the single source of truth for
 * which screen is showing, so the highlighted tab can never disagree with
 * the content (e.g. Insights showing Create's screen).
 */
class MainActivity : ComponentActivity() {
    // Facebook Login hands its result back through this manager.
    private val fbCallbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlannerTheme { PlannerNav(fbCallbackManager) } }
    }

    @Deprecated("Used by the Facebook SDK login flow")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        fbCallbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}

private enum class Tab(val route: String, val label: String) {
    CREATE("create", "Create"),
    PUBLISH("publish", "Publish"),
    INSIGHTS("insights", "Insights")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerNav(fbCallbackManager: CallbackManager) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentTab = Tab.entries.firstOrNull { it.route == currentRoute }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Post Planner",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { nav.navigate("page") }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Connect Page")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                listOf(Tab.CREATE, Tab.PUBLISH, Tab.INSIGHTS).forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (tab) {
                                Tab.CREATE -> Icon(Icons.Default.Add, null)
                                Tab.PUBLISH -> Icon(Icons.Default.CalendarMonth, null)
                                Tab.INSIGHTS -> Icon(Icons.Default.BarChart, null)
                            }
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab != Tab.CREATE && currentRoute != "page") {
                FloatingActionButton(
                    onClick = { nav.navigate(Tab.CREATE.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation()
                ) {
                    Icon(Icons.Default.Add, "Create")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav, startDestination = Tab.PUBLISH.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.PUBLISH.route) {
                PublishScreen(onCreate = { nav.navigate(Tab.CREATE.route) })
            }
            composable(Tab.CREATE.route) {
                CreateScreen(
                    onDone = { nav.navigate(Tab.PUBLISH.route) },
                    onConnectPage = { nav.navigate("page") }
                )
            }
            composable(Tab.INSIGHTS.route) { InsightsScreen() }
            composable("page") { PageScreen(callbackManager = fbCallbackManager) }
        }
    }
}
