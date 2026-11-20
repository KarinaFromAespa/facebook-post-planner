package com.facebookpostplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.facebookpostplanner.ui.create.CreateScreen
import com.facebookpostplanner.ui.insights.InsightsScreen
import com.facebookpostplanner.ui.publish.PublishScreen
import com.facebookpostplanner.ui.theme.PlannerTheme

/**
 * Bottom nav: Create / Publish / Insights only. No Community tab.
 * Publish is home. FAB goes to Create.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlannerTheme { PlannerNav() } }
    }
}

private enum class Tab(val route: String, val label: String) {
    CREATE("create", "Create"),
    PUBLISH("publish", "Publish"),
    INSIGHTS("insights", "Insights")
}

@Composable
private fun PlannerNav() {
    val nav = rememberNavController()
    var current by remember { mutableStateOf(Tab.PUBLISH) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Tab.CREATE, Tab.PUBLISH, Tab.INSIGHTS).forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab,
                        onClick = {
                            current = tab
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
            if (current != Tab.CREATE) {
                FloatingActionButton(onClick = {
                    current = Tab.CREATE
                    nav.navigate(Tab.CREATE.route)
                }) { Icon(Icons.Default.Add, "Create") }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav, startDestination = Tab.PUBLISH.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.PUBLISH.route) {
                PublishScreen(onCreate = {
                    current = Tab.CREATE
                    nav.navigate(Tab.CREATE.route)
                })
            }
            composable(Tab.CREATE.route) {
                CreateScreen(onDone = {
                    current = Tab.PUBLISH
                    nav.navigate(Tab.PUBLISH.route) {
                        popUpTo(Tab.PUBLISH.route) { inclusive = true }
                    }
                })
            }
            composable(Tab.INSIGHTS.route) { InsightsScreen() }
        }
    }
}
