package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.settings.VoiceSettingsScreen
import com.example.ui.settings.VoiceSettingsViewModel

object AppDestinations {
    const val CHAT = "chat"
    const val VOICE_SETTINGS = "voice_settings"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val voiceSettingsViewModel: VoiceSettingsViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = AppDestinations.CHAT,
        modifier = modifier
    ) {
        composable(AppDestinations.CHAT) {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = {
                    navController.navigate(AppDestinations.VOICE_SETTINGS)
                }
            )
        }

        composable(AppDestinations.VOICE_SETTINGS) {
            VoiceSettingsScreen(
                viewModel = voiceSettingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
