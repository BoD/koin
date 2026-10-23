@file:OptIn(
    org.koin.core.annotation.KoinExperimentalAPI::class,
    org.koin.core.annotation.KoinDelicateAPI::class,
    org.koin.core.annotation.KoinInternalApi::class,
)

package org.koin.compose.navigation3

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.test.runTest
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject
import org.koin.compose.scope.UnboundKoinScope
import org.koin.core.scope.Scope
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import org.koin.dsl.onClose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NavigationScopeTest {
    @Test
    fun scoped_entry_provides_its_scope_to_nested_injection_and_restores_ambient_scope() {
        runTest {
            var receiverScope: Scope? = null
            var entryScope: Scope? = null
            var injected: Dependency? = null
            var surroundingScope: Scope? = null
            var surroundingDependency: Dependency? = null
            val application = koinApplication {
                modules(module {
                    scope<ScreenScope> {
                        scoped { Dependency() }
                        navigation<FirstRoute> {
                            receiverScope = this
                            entryScope = currentKoinScope()
                            NestedContent { injected = it }
                        }
                    }
                })
            }
            val destination = application.koin.createScope<ScreenScope>("destination")
            val ambient = application.koin.createScope<ScreenScope>("ambient")
            val entry = destination.getEntryProvider<Any>()(FirstRoute)
            val recomposer = Recomposer(coroutineContext)
            val composition = Composition(UnitApplier(), recomposer)
            try {
                composition.setContent {
                    UnboundKoinScope(ambient) {
                        entry.Content()
                        surroundingScope = currentKoinScope()
                        surroundingDependency = koinInject()
                    }
                }
                assertSame(destination, receiverScope)
                assertSame(destination, entryScope)
                assertSame(destination.get<Dependency>(), injected)
                assertSame(ambient, surroundingScope)
                assertSame(ambient.get<Dependency>(), surroundingDependency)
            } finally {
                composition.dispose()
                recomposer.cancel()
                application.close()
            }
        }
    }

    @Test
    fun module_entry_provides_its_root_scope_even_inside_another_scope() {
        runTest {
            var receiverScope: Scope? = null
            var entryScope: Scope? = null
            var injected: Dependency? = null
            val application = koinApplication {
                modules(module {
                    single { Dependency() }
                    scope<ScreenScope> { scoped { Dependency() } }
                    navigation<FirstRoute> {
                        receiverScope = this
                        entryScope = currentKoinScope()
                        NestedContent { injected = it }
                    }
                })
            }
            val root = application.koin.scopeRegistry.rootScope
            val ambient = application.koin.createScope<ScreenScope>("ambient")
            val entry = root.getEntryProvider<Any>()(FirstRoute)
            val recomposer = Recomposer(coroutineContext)
            val composition = Composition(UnitApplier(), recomposer)
            try {
                composition.setContent {
                    UnboundKoinScope(ambient) { entry.Content() }
                }
                assertSame(root, receiverScope)
                assertSame(root, entryScope)
                assertSame(root.get<Dependency>(), injected)
            } finally {
                composition.dispose()
                recomposer.cancel()
                application.close()
            }
        }
    }

    @Test
    fun disposing_entries_does_not_close_their_shared_external_scope() {
        runTest {
            val injected = mutableListOf<Dependency>()
            var closeCount = 0
            val application = koinApplication {
                modules(module {
                    scope<ScreenScope> {
                        scoped { Dependency() }.onClose { closeCount++ }
                        navigation<FirstRoute> { NestedContent { injected += it } }
                        navigation<SecondRoute> { NestedContent { injected += it } }
                    }
                })
            }
            val destination = application.koin.createScope<ScreenScope>("destination")
            val ambient = application.koin.createScope<ScreenScope>("ambient")
            val provider = destination.getEntryProvider<Any>()
            val recomposer = Recomposer(coroutineContext)
            val first = Composition(UnitApplier(), recomposer)
            val second = Composition(UnitApplier(), recomposer)
            try {
                first.setContent {
                    UnboundKoinScope(ambient) { provider(FirstRoute).Content() }
                }
                second.setContent {
                    UnboundKoinScope(ambient) { provider(SecondRoute).Content() }
                }
                assertEquals(2, injected.size)
                assertSame(destination.get<Dependency>(), injected[0])
                assertSame(injected[0], injected[1])

                first.dispose()
                second.dispose()
                assertFalse(destination.closed)
                assertEquals(0, closeCount)
                assertSame(injected[0], destination.get<Dependency>())

                destination.close()
                assertTrue(destination.closed)
                assertEquals(1, closeCount)
            } finally {
                first.dispose()
                second.dispose()
                recomposer.cancel()
                application.close()
            }
        }
    }

    @Composable
    private fun NestedContent(onInjected: (Dependency) -> Unit) {
        onInjected(koinInject())
    }

    private class ScreenScope
    private class Dependency
    private data object FirstRoute
    private data object SecondRoute

    // These tests exercise Compose context propagation without a platform UI tree.
    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
