@file:OptIn(
    org.koin.core.annotation.KoinExperimentalAPI::class,
    org.koin.core.annotation.KoinDelicateAPI::class,
    org.koin.core.annotation.KoinInternalApi::class,
)

package org.koin.compose.navigation3

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlinx.coroutines.test.runTest
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject
import org.koin.compose.scope.UnboundKoinScope
import org.koin.core.error.ScopeAlreadyCreatedException
import org.koin.core.scope.Scope
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.onClose
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KoinNavEntryScopeTest {
    @Test
    fun scope_and_source_survive_recomposition_and_composition_recreation_until_store_clears() {
        runTest {
            var closeCount = 0
            var sourceCount = 0
            val application = koinApplication {
                modules(module {
                    scope<ScreenScope> { scoped { Dependency() }.onClose { closeCount++ } }
                })
            }
            val root = application.koin.scopeRegistry.rootScope
            val owner = TestOwner()
            val first = TestComposition(coroutineContext)
            val recreated = TestComposition(coroutineContext)
            var observedScope: Scope? = null
            var observedSource: Source? = null
            var observedDependency: Dependency? = null
            val content: @Composable () -> Unit = {
                KoinNavEntryScope<ScreenScope>("entry", source = { sourceCount++; Source() }) {
                    assertSame(this, currentKoinScope())
                    observedScope = this
                    observedSource = getSource()
                    observedDependency = koinInject()
                }
            }
            try {
                first.render(owner, root, content)
                val scope = observedScope!!
                val source = observedSource!!
                val dependency = observedDependency!!
                source.page = 1

                first.render(owner, root, content)
                assertSame(scope, observedScope)
                assertEquals(1, sourceCount)

                first.close()
                assertFalse(scope.closed)
                assertEquals(0, closeCount)

                recreated.render(owner, root, content)
                assertSame(scope, observedScope)
                assertSame(source, observedSource)
                assertEquals(1, observedSource!!.page)
                assertSame(dependency, observedDependency)
                assertEquals(1, sourceCount)

                owner.viewModelStore.clear()
                assertTrue(scope.closed)
                assertNull(application.koin.getScopeOrNull("entry"))
                assertEquals(1, closeCount)
                owner.viewModelStore.clear()
                assertEquals(1, closeCount)
            } finally {
                first.close()
                recreated.close()
                owner.viewModelStore.clear()
                application.close()
            }
        }
    }

    @Test
    fun independent_entry_stores_own_independent_scopes() {
        runTest {
            val application = koinApplication {
                modules(module { scope<ScreenScope> { scoped { Dependency() } } })
            }
            val root = application.koin.scopeRegistry.rootScope
            val firstOwner = TestOwner()
            val secondOwner = TestOwner()
            val first = TestComposition(coroutineContext)
            val second = TestComposition(coroutineContext)
            var firstScope: Scope? = null
            var secondScope: Scope? = null
            try {
                first.render(firstOwner, root) { firstScope = rememberKoinNavEntryScope<ScreenScope>("first") }
                second.render(secondOwner, root) { secondScope = rememberKoinNavEntryScope<ScreenScope>("second") }
                assertNotSame(firstScope!!.get<Dependency>(), secondScope!!.get<Dependency>())

                firstOwner.viewModelStore.clear()
                assertTrue(firstScope!!.closed)
                assertFalse(secondScope!!.closed)
            } finally {
                first.close()
                second.close()
                firstOwner.viewModelStore.clear()
                secondOwner.viewModelStore.clear()
                application.close()
            }
        }
    }

    @Test
    fun existing_external_scope_is_not_borrowed_or_closed() {
        runTest {
            val application = koinApplication()
            val root = application.koin.scopeRegistry.rootScope
            val external = application.koin.createScope<ScreenScope>("existing")
            val owner = TestOwner()
            val composition = TestComposition(coroutineContext)
            try {
                assertFailsWith<ScopeAlreadyCreatedException> {
                    composition.render(owner, root) { rememberKoinNavEntryScope<ScreenScope>("existing") }
                }
                owner.viewModelStore.clear()
                composition.close()
                assertFalse(external.closed)
                assertSame(external, application.koin.getScope("existing"))
            } finally {
                composition.close()
                owner.viewModelStore.clear()
                application.close()
            }
        }
    }

    @Test
    fun retained_scope_cannot_change_qualifier() {
        runTest {
            val application = koinApplication()
            val root = application.koin.scopeRegistry.rootScope
            val owner = TestOwner()
            val composition = TestComposition(coroutineContext)
            try {
                composition.render(owner, root) { rememberKoinNavEntryScope<ScreenScope>("entry") }
                val error = assertFailsWith<IllegalStateException> {
                    composition.render(owner, root) { rememberKoinNavEntryScope<OtherScope>("entry") }
                }
                assertTrue(error.message!!.contains("qualifier cannot change"))
                assertFalse(application.koin.getScope("entry").closed)
            } finally {
                composition.close()
                owner.viewModelStore.clear()
                application.close()
            }
        }
    }

    @Test
    fun retained_scope_cannot_change_koin_container() {
        runTest {
            val firstApp = koinApplication()
            val secondApp = koinApplication()
            val owner = TestOwner()
            val composition = TestComposition(coroutineContext)
            try {
                composition.render(owner, firstApp.koin.scopeRegistry.rootScope) {
                    rememberKoinNavEntryScope<ScreenScope>("entry")
                }
                val error = assertFailsWith<IllegalStateException> {
                    composition.render(owner, secondApp.koin.scopeRegistry.rootScope) {
                        rememberKoinNavEntryScope<ScreenScope>("entry")
                    }
                }
                assertTrue(error.message!!.contains("container cannot change"))
                assertNull(secondApp.koin.getScopeOrNull("entry"))
            } finally {
                composition.close()
                owner.viewModelStore.clear()
                firstApp.close()
                secondApp.close()
            }
        }
    }

    @Test
    fun missing_viewmodel_owner_fails_before_creating_a_scope() {
        runTest {
            val application = koinApplication()
            val composition = TestComposition(coroutineContext)
            try {
                val error = assertFailsWith<IllegalStateException> {
                    composition.render(null, application.koin.scopeRegistry.rootScope) {
                        rememberKoinNavEntryScope<ScreenScope>("entry")
                    }
                }
                assertTrue(error.message!!.contains("rememberViewModelStoreNavEntryDecorator"))
                assertNull(application.koin.getScopeOrNull("entry"))
            } finally {
                composition.close()
                application.close()
            }
        }
    }

    private class ScreenScope
    private class OtherScope
    private class Dependency
    private class Source(var page: Int = 0)

    private class TestOwner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    private class TestComposition(context: CoroutineContext) {
        private val recomposer = Recomposer(context)
        private val composition = Composition(UnitApplier(), recomposer)

        fun render(owner: ViewModelStoreOwner?, scope: Scope, content: @Composable () -> Unit) {
            composition.setContent {
                UnboundKoinScope(scope) {
                    if (owner == null) {
                        content()
                    } else {
                        CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
                    }
                }
            }
        }

        fun close() {
            composition.dispose()
            recomposer.cancel()
        }
    }

    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
