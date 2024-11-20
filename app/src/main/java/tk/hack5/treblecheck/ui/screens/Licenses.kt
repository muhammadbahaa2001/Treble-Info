/*
 *     Treble Info
 *     Copyright (C) 2023 Hackintosh Five
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: GPL-3.0-or-later

package tk.hack5.treblecheck.ui.screens

import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.*
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.parcelize.Parcelize
import tk.hack5.treblecheck.*
import tk.hack5.treblecheck.R
import tk.hack5.treblecheck.ui.*

@Composable
fun Licenses(
    innerPadding: PaddingValues,
    scrollConnection: NestedScrollConnection,
    openLink: (String) -> Unit,
) {
    val libraries = remember { mutableStateOf<Libs?>(null) }

    val context = LocalContext.current
    if (!LocalInspectionMode.current) {
        LaunchedEffect(libraries) {
            libraries.value = Libs.Builder().withContext(context).build()
        }
    } else {
        libraries.value = Libs(emptyList(), emptySet())
    }

    val licenses = setOf(
        License(
            "GNU General Public License v3.0 or later",
            "https://spdx.org/licenses/GPL-3.0-or-later.html",
            null,
            "GPL-3.0-or-later",
            context.resources.openRawResource(R.raw.license).bufferedReader().readText(),
            "GPL-3.0-or-later-TrebleInfo"
        )
    )
    val thisLibrary = Library(
        "tk.hack5:treblecheck",
        BuildConfig.VERSION_NAME + '-' + BuildConfig.FLAVOR + '-' + BuildConfig.BUILD_TYPE,
        stringResource(R.string.title),
        stringResource(R.string.this_app),
        "https://hack5.dev/about/projects/TrebleInfo",
        listOf(Developer("hackintosh5", "https://hack5.dev/about")),
        null,
        Scm(
            "scm:git:https://hack5.dev/about/projects/TrebleInfo",
            "scm:git:ssh://git@gitlab.com/TrebleInfo/TrebleInfo.git",
            "https://gitlab.com/TrebleInfo/TrebleInfo.git"
        ),
        licenses,
        setOf(),
        null
    )

    val newLibraries = libraries.value?.let { Libs(listOf(thisLibrary) + it.libraries, licenses + it.licenses) }

    newLibraries?.let { Libraries(innerPadding, scrollConnection, it, openLink) }
}

@Parcelize
sealed class OpenItem<T> : Parcelable {
    abstract fun getItem(libraries: Libs): T

    @Parcelize
    data class OpenLibrary(val uniqueId: String) : OpenItem<Library>(), Parcelable {
        override fun getItem(libraries: Libs): Library {
            return libraries.libraries.first { it.uniqueId == uniqueId }
        }
    }

    @Parcelize
    data class OpenLicense(val libraryUniqueId: String, val hash: String) : OpenItem<Pair<Library, License>>() {
        override fun getItem(libraries: Libs): Pair<Library, License> {
            return libraries.libraries.first { it.uniqueId == libraryUniqueId } to libraries.licenses.first { it.hash == hash }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryDialog(library: Library, setOpenItem: (OpenItem<*>?) -> Unit, openLink: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = { setOpenItem(null) },
        confirmButton = {
            TextButton(onClick = { setOpenItem(null) }) {
                Text(stringResource(R.string.close_dialog))
            }
        },
        title = {
            Text(library.name)
        },
        text = {
            Column {
                library.description?.let { description ->
                    Text(description)
                }
                Spacer(Modifier.height(verticalSmallSpacer))
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(verticalSmallSpacer, Alignment.Top)) {
                    Text(
                        library.artifactId,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(verticalMediumSpacer - verticalSmallSpacer))
                    if (library.website != null || library.scm?.url != null) {
                        Text(stringResource(R.string.library_links), style = MaterialTheme.typography.titleMedium)
                        FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(horizontalSpacer, Alignment.Start)) {
                            if (!library.website.isNullOrEmpty()) {
                                library.website?.let { website ->
                                    OutlinedButton({
                                        openLink(website)
                                    }) {
                                        Text(stringResource(R.string.library_website))
                                    }
                                }
                            }
                            if (!library.scm?.url.isNullOrEmpty()) {
                                library.scm?.url?.let { url ->
                                    OutlinedButton({
                                        openLink(url)
                                    }) {
                                        Text(stringResource(R.string.library_source))
                                    }
                                }
                            }
                        }
                    }
                    val developers =
                        library.developers.filter { developer -> developer.name != null }
                    if (developers.isNotEmpty()) {
                        Text(stringResource(R.string.library_developers), style = MaterialTheme.typography.titleMedium)
                        FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(horizontalSpacer, Alignment.Start)) {
                            developers.forEachIndexed { i, developer ->
                                if (i != 0) {
                                    Spacer(Modifier.width(horizontalSpacer))
                                }
                                TextButton(
                                    {
                                        if (!developer.organisationUrl.isNullOrEmpty()) {
                                            developer.organisationUrl?.let { url ->
                                                openLink(url)
                                            }
                                        }
                                    }
                                ) {
                                    Text(developer.name!!)
                                }
                            }
                        }
                    }
                    library.organization?.let { organization ->
                        Text(stringResource(R.string.library_organization), style = MaterialTheme.typography.titleMedium)
                        TextButton({
                            if (!organization.url.isNullOrEmpty()) {
                                organization.url?.let { url ->
                                    openLink(url)
                                }
                            }
                        }) {
                            Text(organization.name)
                        }
                    }
                    if (library.funding.isNotEmpty()) {
                        Text(stringResource(R.string.library_funding), style = MaterialTheme.typography.titleMedium)
                        library.funding.forEach { funding ->
                            TextButton({
                                openLink(funding.url)
                            }) {
                                Text(funding.platform)
                            }
                        }
                    }
                    if (library.licenses.isNotEmpty()) {
                        Text(
                            stringResource(R.string.library_licenses),
                            style = MaterialTheme.typography.titleMedium
                        )
                        FlowRow(
                            verticalArrangement = Arrangement.Center,
                            horizontalArrangement = Arrangement.spacedBy(
                                horizontalSpacer,
                                Alignment.Start
                            )
                        ) {
                            library.licenses.forEach {
                                TextButton(
                                    onClick = {
                                        setOpenItem(OpenItem.OpenLicense(library.uniqueId, it.hash))
                                    }
                                ) {
                                    Text(it.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Libraries(innerPadding: PaddingValues, scrollConnection: NestedScrollConnection, libraries: Libs, openLink: (String) -> Unit) {
    var openItem by rememberSaveable { mutableStateOf<OpenItem<*>?>(null) }
    openItem?.let {
        when (it) {
            is OpenItem.OpenLibrary -> {
                val library = remember(it, libraries) { it.getItem(libraries) }

                LibraryDialog(library, { newItem -> openItem = newItem }, openLink)
            }
            is OpenItem.OpenLicense -> {
                val (library, license) = remember { it.getItem(libraries) }

                AlertDialog(
                    onDismissRequest = { openItem = null },
                    confirmButton = {
                        TextButton(onClick = { openItem = null }) {
                            Text(stringResource(R.string.close_dialog))
                        }
                    },
                    title = {
                        Text(license.name)
                    },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Text(library.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(verticalMediumSpacer))
                            license.licenseContent?.let { content ->
                                Text(content)
                            }
                        }
                    }
                )
            }
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .nestedScroll(scrollConnection)
            .consumeWindowInsets(innerPadding),
        contentPadding = innerPadding
    ) {
        items(libraries.libraries) { library ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { openItem = OpenItem.OpenLibrary(library.uniqueId) }
                    .padding(vertical = listVerticalPadding, horizontal = pageHorizontalPadding)
                    .safeDrawingPadding()
            ) {
                Row {
                    Text(
                        library.name,
                        Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge
                    )
                    library.artifactVersion?.let {
                        Text(
                            it,
                            Modifier
                                .padding(start = horizontalSpacer),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                (library.developers.firstOrNull()?.name ?: library.organization?.name)?.let {
                    Text(it)
                }
                FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(horizontalSpacer, Alignment.Start)) {
                    library.licenses.forEach {
                        TextButton(
                            onClick = { openItem = OpenItem.OpenLicense(library.uniqueId, it.hash) }
                        ) {
                            Text(it.name)
                        }
                    }
                }
            }
        }
    }
}