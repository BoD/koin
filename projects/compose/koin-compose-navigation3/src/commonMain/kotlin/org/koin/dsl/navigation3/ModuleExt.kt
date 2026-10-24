/*
 * Copyright 2017-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.koin.dsl.navigation3

import androidx.compose.runtime.Composable
import org.koin.compose.navigation3.EntryProviderInstaller
import org.koin.compose.scope.UnboundKoinScope
import org.koin.core.annotation.KoinDelicateAPI
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.KoinDslMarker
import org.koin.core.module.Module
import org.koin.core.module._scopedInstanceFactory
import org.koin.core.module._singleInstanceFactory
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.ScopeDSL

/**
 * Declares a scoped navigation entry within a Koin scope DSL.
 *
 * This function registers a composable navigation destination that is scoped to a specific Koin scope,
 * allowing access to scoped dependencies within the composable. The route type [T] is used as both
 * the navigation destination identifier and a qualifier for the entry provider.
 *
 * The scope used to resolve this entry's installer is also provided to the composable hierarchy,
 * so default Compose injection uses the same scope as the [Scope] receiver.
 *
 * Example usage:
 * ```kotlin
 * activityScope {
 *     viewModel { MyViewModel() }
 *     navigation<MyRoute> { route ->
 *         MyScreen(viewModel = koinViewModel())
 *     }
 * }
 * ```
 *
 * @param T The type representing the navigation route/destination
 * @param metadata Optional metadata map to associate with the navigation entry (default is empty)
 * @param definition A composable function that receives the [Scope] and route instance [T] to render the destination
 * @return A [KoinDefinition] for the created [EntryProviderInstaller]
 *
 * @see Module.navigation for module-level navigation entries
 */
@KoinExperimentalAPI
@KoinDslMarker
@OptIn(KoinInternalApi::class, KoinDelicateAPI::class)
inline fun <reified T : Any> ScopeDSL.navigation(
    metadata: Map<String, Any> = emptyMap(),
    noinline definition: @Composable Scope.(T) -> Unit,
): KoinDefinition<EntryProviderInstaller> {
    val def = _scopedInstanceFactory<EntryProviderInstaller>(named<T>(), {
        val scope = this
        {
            entry<T>(
                content = { t -> UnboundKoinScope(scope) { definition(scope, t) } },
                metadata = metadata,
            )
        }
    }, scopeQualifier)
    module.indexPrimaryType(def)
    return KoinDefinition(module, def)
}

/**
 * Declares a scoped navigation entry with a custom Navigation 3 content-key factory.
 *
 * [clazzContentKey] receives the route instance and returns the entry's stable content identity.
 * The returned value is passed to Navigation 3 unchanged and must meet its state-saving
 * requirements. It does not select or create a Koin scope.
 *
 * The overload without [clazzContentKey] retains Navigation 3's own default key behaviour.
 * Both overloads provide the captured scope to destination content without owning its lifetime.
 *
 * @param metadata Metadata passed to the navigation entry
 * @param clazzContentKey Factory for the entry's content identity, as in Navigation 3's `entry`
 * @param definition Destination content receiving the captured scope and route instance
 */
@KoinExperimentalAPI
@KoinDslMarker
@OptIn(KoinInternalApi::class, KoinDelicateAPI::class)
inline fun <reified T : Any> ScopeDSL.navigation(
    metadata: Map<String, Any> = emptyMap(),
    noinline clazzContentKey: (T) -> Any,
    noinline definition: @Composable Scope.(T) -> Unit,
): KoinDefinition<EntryProviderInstaller> {
    val def = _scopedInstanceFactory<EntryProviderInstaller>(named<T>(), {
        val scope = this
        {
            entry<T>(
                clazzContentKey = clazzContentKey,
                content = { t -> UnboundKoinScope(scope) { definition(scope, t) } },
                metadata = metadata,
            )
        }
    }, scopeQualifier)
    module.indexPrimaryType(def)
    return KoinDefinition(module, def)
}

/**
 * Declares a singleton navigation entry within a Koin module.
 *
 * This function registers a composable navigation destination as a singleton in the Koin module,
 * allowing access to module-level dependencies within the composable. The route type [T] is used
 * as both the navigation destination identifier and a qualifier for the entry provider.
 *
 * The captured root scope is provided to the composable hierarchy, so default Compose injection
 * uses the same scope as the [Scope] receiver, even when the entry is displayed within another
 * Compose Koin scope.
 *
 * Example usage:
 * ```kotlin
 * module {
 *     viewModel { MyViewModel() }
 *     navigation<HomeRoute> { route ->
 *         HomeScreen(myViewModel = koinViewModel())
 *     }
 * }
 * ```
 *
 * @param T The type representing the navigation route/destination
 * @param metadata Optional metadata map to associate with the navigation entry (default is empty)
 * @param definition A composable function that receives the [Scope] and route instance [T] to render the destination
 * @return A [KoinDefinition] for the created [EntryProviderInstaller]
 *
 * @see ScopeDSL.navigation for scope-level navigation entries
 */
@KoinExperimentalAPI
@KoinDslMarker
@OptIn(KoinInternalApi::class, KoinDelicateAPI::class)
inline fun <reified T : Any> Module.navigation(
    metadata: Map<String, Any> = emptyMap(),
    noinline definition: @Composable Scope.(T) -> Unit,
): KoinDefinition<EntryProviderInstaller> {
    val def = _singleInstanceFactory<EntryProviderInstaller>(named<T>(), {
        val scope = this
        {
            entry<T>(
                content = { t -> UnboundKoinScope(scope) { definition(scope, t) } },
                metadata = metadata,
            )
        }
    })
    indexPrimaryType(def)
    return KoinDefinition(this, def)
}

/**
 * Declares a module-level navigation entry with a custom Navigation 3 content-key factory.
 *
 * [clazzContentKey] receives the route instance and returns the entry's stable content identity.
 * The returned value is passed to Navigation 3 unchanged and must meet its state-saving
 * requirements. It does not select or create a Koin scope.
 *
 * The overload without [clazzContentKey] retains Navigation 3's own default key behaviour.
 * Both overloads provide the captured scope to destination content without owning its lifetime.
 *
 * @param metadata Metadata passed to the navigation entry
 * @param clazzContentKey Factory for the entry's content identity, as in Navigation 3's `entry`
 * @param definition Destination content receiving the captured scope and route instance
 */
@KoinExperimentalAPI
@KoinDslMarker
@OptIn(KoinInternalApi::class, KoinDelicateAPI::class)
inline fun <reified T : Any> Module.navigation(
    metadata: Map<String, Any> = emptyMap(),
    noinline clazzContentKey: (T) -> Any,
    noinline definition: @Composable Scope.(T) -> Unit,
): KoinDefinition<EntryProviderInstaller> {
    val def = _singleInstanceFactory<EntryProviderInstaller>(named<T>(), {
        val scope = this
        {
            entry<T>(
                clazzContentKey = clazzContentKey,
                content = { t -> UnboundKoinScope(scope) { definition(scope, t) } },
                metadata = metadata,
            )
        }
    })
    indexPrimaryType(def)
    return KoinDefinition(this, def)
}
