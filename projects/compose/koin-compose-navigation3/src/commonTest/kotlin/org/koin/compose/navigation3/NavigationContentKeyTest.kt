@file:OptIn(
    org.koin.core.annotation.KoinExperimentalAPI::class,
    org.koin.core.annotation.KoinInternalApi::class,
)

package org.koin.compose.navigation3

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import org.koin.core.scope.Scope
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NavigationContentKeyTest {
    @Test
    fun module_navigation_passes_each_route_to_the_key_factory_and_preserves_metadata() {
        val calls = mutableListOf<Route>()
        val metadata = mapOf<String, Any>("pane" to "detail")
        val application = koinApplication {
            modules(module {
                navigation<Route>(metadata = metadata, clazzContentKey = { route ->
                    calls += route
                    ContentKey(route.id)
                }) {}
            })
        }
        try {
            val provider = application.koin.scopeRegistry.rootScope.getEntryProvider<Any>()
            assertTrue(calls.isEmpty())
            val first = Route("first")
            val second = Route("second")
            val firstEntry = provider(first)
            val secondEntry = provider(second)

            assertEquals(ContentKey("first"), firstEntry.contentKey)
            assertEquals(ContentKey("second"), secondEntry.contentKey)
            assertNotEquals(firstEntry.contentKey, secondEntry.contentKey)
            assertEquals(listOf(first, second), calls)
            assertSame(first, calls[0])
            assertSame(second, calls[1])
            assertEquals(metadata, firstEntry.metadata)
            assertEquals(metadata, secondEntry.metadata)
        } finally {
            application.close()
        }
    }

    @Test
    fun scoped_navigation_passes_each_route_to_the_key_factory_and_preserves_metadata() {
        val calls = mutableListOf<Route>()
        val metadata = mapOf<String, Any>("pane" to "detail")
        val application = koinApplication {
            modules(module {
                scope<ScreenScope> {
                    navigation<Route>(metadata, clazzContentKey = { route ->
                        calls += route
                        ContentKey(route.id)
                    }, definition = {})
                }
            })
        }
        try {
            val scope = application.koin.createScope<ScreenScope>("screen")
            val provider = scope.getEntryProvider<Any>()
            assertTrue(calls.isEmpty())
            val first = Route("first")
            val second = Route("second")
            val firstEntry = provider(first)
            val secondEntry = provider(second)

            assertEquals(ContentKey("first"), firstEntry.contentKey)
            assertEquals(ContentKey("second"), secondEntry.contentKey)
            assertNotEquals(firstEntry.contentKey, secondEntry.contentKey)
            assertEquals(listOf(first, second), calls)
            assertSame(first, calls[0])
            assertSame(second, calls[1])
            assertEquals(metadata, firstEntry.metadata)
            assertEquals(metadata, secondEntry.metadata)
        } finally {
            application.close()
        }
    }

    @Test
    fun existing_module_call_forms_keep_nav3_default_keys() {
        val metadata = mapOf<String, Any>("pane" to "detail")
        val definition: @Composable Scope.(Route) -> Unit = {}
        val application = koinApplication {
            modules(module {
                navigation<Route>(metadata, definition)
                navigation<OtherRoute> {}
            })
        }
        try {
            val provider = application.koin.scopeRegistry.rootScope.getEntryProvider<Any>()
            val nav3Provider = entryProvider<Any> {
                entry<Route>(metadata = metadata) {}
                entry<OtherRoute> {}
            }
            val route = Route("default")
            assertEquals(nav3Provider(route).contentKey, provider(route).contentKey)
            assertEquals(metadata, provider(route).metadata)
            assertEquals(nav3Provider(OtherRoute).contentKey, provider(OtherRoute).contentKey)
        } finally {
            application.close()
        }
    }

    @Test
    fun existing_scoped_call_forms_keep_nav3_default_keys() {
        val metadata = mapOf<String, Any>("pane" to "detail")
        val definition: @Composable Scope.(Route) -> Unit = {}
        val application = koinApplication {
            modules(module {
                scope<ScreenScope> {
                    navigation<Route>(metadata = metadata, definition = definition)
                    navigation<OtherRoute> {}
                }
            })
        }
        try {
            val provider = application.koin.createScope<ScreenScope>("screen").getEntryProvider<Any>()
            val nav3Provider = entryProvider<Any> {
                entry<Route>(metadata = metadata) {}
                entry<OtherRoute> {}
            }
            val route = Route("default")
            assertEquals(nav3Provider(route).contentKey, provider(route).contentKey)
            assertEquals(metadata, provider(route).metadata)
            assertEquals(nav3Provider(OtherRoute).contentKey, provider(OtherRoute).contentKey)
        } finally {
            application.close()
        }
    }

    // The custom factory must distinguish routes even if their default text collides.
    private data class Route(val id: String) {
        override fun toString(): String = "Route"
    }

    private data class ContentKey(val id: String)
    private data object OtherRoute
    private class ScreenScope
}
