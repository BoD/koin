package org.koin.compose.navigation3

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.compose.currentKoinScope
import org.koin.compose.scope.UnboundKoinScope
import org.koin.core.Koin
import org.koin.core.annotation.KoinDelicateAPI
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeID

/**
 * Creates a Koin scope owned by the current Nav Entry's ViewModel store.
 *
 * The nearest [LocalViewModelStoreOwner] determines the lifetime.
 *
 * The scope and [source] are created once per [scopeID] in that store. Leaving composition
 * does not close the scope, and recomposition or Activity recreation reuses it. The scope is
 * closed when leaving the navigation entry.
 */
@KoinExperimentalAPI
@Composable
fun rememberKoinNavEntryScope(
    scopeID: ScopeID,
    scopeQualifier: Qualifier,
    koin: Koin = currentKoinScope().getKoin(),
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "KoinNavEntryScope requires a ViewModelStoreOwner."
    },
    source: () -> Any? = { null },
): Scope {
    val holder = viewModel<NavEntryScopeHolder>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = "org.koin.compose.navigation3.scope:$scopeID",
    ) {
        NavEntryScopeHolder(koin.createScope(scopeID, scopeQualifier, source()))
    }
    return holder.scope
}

/** Type-qualified version of [rememberKoinNavEntryScope]. */
@KoinExperimentalAPI
@Composable
inline fun <reified T : Any> rememberKoinNavEntryScope(
    scopeID: ScopeID,
    koin: Koin = currentKoinScope().getKoin(),
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "KoinNavEntryScope requires a ViewModelStoreOwner."
    },
    noinline source: () -> Any? = { null },
): Scope = rememberKoinNavEntryScope(scopeID, named<T>(), koin, viewModelStoreOwner, source)

/**
 * Provides a Nav Entry owned scope to [content] and its nested Compose injection calls.
 */
@OptIn(KoinDelicateAPI::class)
@KoinExperimentalAPI
@Composable
fun KoinNavEntryScope(
    scopeID: ScopeID,
    scopeQualifier: Qualifier,
    koin: Koin = currentKoinScope().getKoin(),
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "KoinNavEntryScope requires a ViewModelStoreOwner."
    },
    source: () -> Any? = { null },
    content: @Composable Scope.() -> Unit,
) {
    val scope = rememberKoinNavEntryScope(scopeID, scopeQualifier, koin, viewModelStoreOwner, source)
    UnboundKoinScope(scope) { content(scope) }
}

/**
 * Provides a Nav Entry owned scope to [content] and its nested Compose injection calls.
 */
@KoinExperimentalAPI
@Composable
inline fun <reified T : Any> KoinNavEntryScope(
    scopeID: ScopeID,
    koin: Koin = currentKoinScope().getKoin(),
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "KoinNavEntryScope requires a ViewModelStoreOwner."
    },
    noinline source: () -> Any? = { null },
    noinline content: @Composable Scope.() -> Unit,
) {
    KoinNavEntryScope(scopeID, named<T>(), koin, viewModelStoreOwner, source, content)
}

private class NavEntryScopeHolder(val scope: Scope) : ViewModel() {
    override fun onCleared() {
        if (!scope.closed) scope.close()
    }
}
