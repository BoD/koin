@file:OptIn(
    KoinExperimentalAPI::class,
    KoinDelicateAPI::class,
    KoinInternalApi::class,
)

package org.koin.compose.navigation3

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.test.runTest
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject
import org.koin.compose.navigation3.annotation.NavigationEntry
import org.koin.compose.scope.UnboundKoinScope
import org.koin.core.annotation.KoinDelicateAPI
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.scope.Scope
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class NavigationEntryContractTest {
    private var renderedRoute: DetailRoute? = null
    private var renderedScope: Scope? = null
    private var renderedDependency: Dependency? = null

    @Test
    fun annotated_signature_and_typed_providers_work_with_explicit_equivalent_dsl() {
        runTest {
            val application = koinApplication {
                modules(module {
                    scope<DetailScope> {
                        scoped { Dependency() }
                        // Explicit equivalent registration. No annotation processing is involved.
                        navigation<DetailRoute>(
                            clazzContentKey = DetailContentKey::contentKey,
                            metadata = DetailMetadata.metadata,
                        ) { route -> DetailDestination(route) }
                    }
                })
            }
            val scope = application.koin.createScope<DetailScope>("detail")
            val route = DetailRoute("visit-1")
            val entry = scope.getEntryProvider<Any>()(route)
            val recomposer = Recomposer(coroutineContext)
            val composition = Composition(UnitApplier(), recomposer)
            try {
                assertEquals("detail:visit-1", entry.contentKey)
                assertEquals(mapOf("pane" to "detail"), entry.metadata)
                composition.setContent {
                    UnboundKoinScope(application.koin.scopeRegistry.rootScope) { entry.Content() }
                }
                assertSame(route, renderedRoute)
                assertSame(scope, renderedScope)
                assertSame(scope.get<Dependency>(), renderedDependency)
            } finally {
                composition.dispose()
                recomposer.cancel()
                application.close()
            }
        }
    }

    @NavigationEntry(
        contentKeyFactory = DetailContentKey::class,
        metadataProvider = DetailMetadata::class,
    )
    @org.koin.core.annotation.Scope(DetailScope::class)
    @Composable
    private fun Scope.DetailDestination(route: DetailRoute) {
        renderedRoute = route
        renderedScope = currentKoinScope()
        renderedDependency = koinInject()
    }

    private interface Route {
        val entryId: String
    }

    private data class DetailRoute(override val entryId: String) : Route
    private class DetailScope
    private class Dependency

    private object DetailContentKey : NavigationContentKeyFactory<Route> {
        override fun contentKey(route: Route): Any = "detail:${route.entryId}"
    }

    private object DetailMetadata : NavigationMetadataProvider {
        override val metadata: Map<String, Any> = mapOf("pane" to "detail")
    }

    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
