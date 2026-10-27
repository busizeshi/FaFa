package com.jwd.fafa.feature.auth

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

/** M00 导航图：Navigation 3 单 Activity 承载 AUTH-01–05 与协议页。 */
@Composable
fun FaFaNavGraph(start: NavKey) {
    val backStack = rememberNavBackStack(start)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<WelcomeRoute> {
                WelcomeScreen(
                    onFinished = {
                        backStack.clear()
                        backStack.add(LoginRoute)
                    },
                )
            }
            entry<LegalRoute> { route ->
                LegalScreen(
                    documentType = route.documentType,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<LoginRoute> {
                LoginScreen(
                    onLoggedIn = {
                        backStack.clear()
                        backStack.add(OnboardingEntryRoute)
                    },
                    onOpenRegister = { backStack.add(RegisterRoute) },
                    onOpenReset = { backStack.add(ResetPasswordRoute()) },
                    onOpenDocument = { type -> backStack.add(LegalRoute(type)) },
                )
            }
            entry<RegisterRoute> {
                RegisterScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onChallengeCreated = { result ->
                        backStack.add(
                            VerifyRoute(
                                challengeId = result.challenge.challengeId,
                                maskedEmail = result.challenge.email,
                                expiresAtEpochMs = result.expiresAtEpochMs,
                            ),
                        )
                    },
                    onOpenDocument = { type -> backStack.add(LegalRoute(type)) },
                    onGoLogin = {
                        backStack.clear()
                        backStack.add(LoginRoute)
                    },
                )
            }
            entry<VerifyRoute> { route ->
                VerifyScreen(
                    route = route,
                    onBack = { backStack.removeLastOrNull() },
                    onVerified = {
                        backStack.clear()
                        backStack.add(OnboardingEntryRoute)
                    },
                )
            }
            entry<ResetPasswordRoute> { route ->
                ResetPasswordScreen(
                    prefillEmail = route.prefillEmail,
                    onDone = {
                        backStack.clear()
                        backStack.add(LoginRoute)
                    },
                    onExit = { backStack.removeLastOrNull() },
                )
            }
            entry<OnboardingEntryRoute> {
                OnboardingEntryScreen(
                    onExitToLogin = {
                        backStack.clear()
                        backStack.add(LoginRoute)
                    },
                )
            }
        },
    )
}
