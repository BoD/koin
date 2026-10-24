package org.koin.compose.navigation3.annotation

import org.koin.compose.navigation3.NavigationContentKeyFactory
import org.koin.compose.navigation3.NavigationMetadataProvider
import org.koin.core.annotation.KoinExperimentalAPI
import kotlin.reflect.KClass

/**
 * Registers a Navigation 3 destination.
 *
 * The target must be a non-suspending `@Composable` function returning `Unit`, with exactly
 * one ordinary value parameter: its route.
 * An optional `Scope` **extension receiver** provides access to the registration scope.
 *
 * Combine with `@Scope` or a platform scope annotation to select the registration scope.
 * Without one, registration is at module level.
 *
 * Discovery follows Koin's `@Module` and `@ComponentScan` conventions.
 *
 * @param contentKeyFactory An object implementing [NavigationContentKeyFactory],
 * accepting this route type (or a supertype). Its `contentKey` method
 * supplies `clazzContentKey` to the DSL. [Unit] means omit the factory and retain Nav3's default.
 * @param metadataProvider An object implementing [NavigationMetadataProvider].
 * Its metadata map is passed to the DSL when registering the entry.
 * [Unit] means omit metadata and use the DSL's empty map default.
 *
 * The compiler plugin validates provider object shape and route compatibility. Providers are
 * referenced directly, without runtime reflection or construction through dependency injection.
 */
@Target(AnnotationTarget.FUNCTION)
@KoinExperimentalAPI
public annotation class NavigationEntry(
    val contentKeyFactory: KClass<*> = Unit::class,
    val metadataProvider: KClass<*> = Unit::class,
)
