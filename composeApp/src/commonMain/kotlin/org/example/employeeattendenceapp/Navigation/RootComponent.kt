package org.example.employeeattendenceapp.Navigation

// shared/src/commonMain/kotlin/RootComponent.kt
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import org.example.employeeattendenceapp.Auth.isUserLoggedIn

class RootComponent(
    componentContext: ComponentContext,
    initialRole: String = "employee"
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()
    private var lastSelectedRole: String = initialRole

    val stack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = if (isUserLoggedIn()) Config.HomeWithFlag(false, initialRole) else Config.Dashboard,
        childFactory = ::createChild
    )

    private fun createChild(config: Config, context: ComponentContext): Child =
        when (config) {
            Config.Dashboard -> Child.Dashboard(
                DashboardComponent(
                    componentContext = context,
                    onNavigateToLogin = { role ->
                        lastSelectedRole = role
                        navigation.push(Config.Login(role))
                    }
                )
            )
            is Config.Login -> Child.Login(
                LoginComponent(
                    componentContext = context,
                    role = config.role,
                    onNavigateBack = { navigation.pop() },
                    onNavigateToSignup = { navigation.push(Config.Signup) },
                    onNavigateToHome = {
                        navigation.replaceAll(Config.HomeWithFlag(true, config.role))
                    }
                )
            )
            is Config.Signup -> Child.Signup(
                SignupComponent(
                    componentContext = context,
                    role = lastSelectedRole,
                    onNavigateBack = { navigation.pop() },
                    onNavigateToLogin = { navigation.pop() }
                )
            )
            is Config.HomeWithFlag -> Child.Home(config.justLoggedIn, config.role)
        }

    @Serializable
    sealed class Config {
        @Serializable
        object Dashboard : Config()
        @Serializable
        data class Login(val role: String) : Config()
        @Serializable
        object Signup : Config()
        @Serializable
        data class HomeWithFlag(val justLoggedIn: Boolean = false, val role: String = "employee") : Config()
    }

    sealed class Child {
        data class Dashboard(val component: DashboardComponent) : Child()
        data class Login(val component: LoginComponent) : Child()
        data class Signup(val component: SignupComponent) : Child()
        data class Home(val justLoggedIn: Boolean, val role: String) : Child()
    }
}