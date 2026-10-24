package org.koin.compose.navigation3

import org.koin.compose.navigation3.annotation.NavigationEntry
import org.koin.core.annotation.KoinExperimentalAPI

/**
 * Content-key factory for [NavigationEntry].
 *
 * Return a stable identity meeting Navigation 3's state-saving requirements.
 * Omitting an annotation provider preserves Nav3's default.
 */
@KoinExperimentalAPI
public fun interface NavigationContentKeyFactory<in T : Any> {
    public fun contentKey(route: T): Any
}

/**
 * Entry metadata for [NavigationEntry].
 *
 * Use an object implementing this interface.
 */
@KoinExperimentalAPI
public interface NavigationMetadataProvider {
    public val metadata: Map<String, Any>
}
