package com.luisvtp.portlight.ui

import com.luisvtp.portlight.data.Author
import com.luisvtp.portlight.data.LocalizedText
import com.luisvtp.portlight.data.Port
import com.luisvtp.portlight.data.Release
import com.luisvtp.portlight.data.ReleaseAsset

/**
 * Datos ficticios para previews y tests. No corresponden a ningún juego ni
 * repositorio real (mismos ejemplos que res/raw/catalog.json).
 */
object PreviewData {

    val ports: List<Port> = listOf(
        Port(
            id = "cubiq-quest-port",
            name = "Cubiq Quest Port (demo)",
            description = LocalizedText(
                es = "Port de demostración de un puzzle completamente ficticio. Requiere tu propia copia del juego original.",
                en = "Demonstration port of a fully fictional puzzle game. Requires your own copy of the original game.",
            ),
            author = Author(name = "Autor de ejemplo", contact = "https://github.com/example"),
            license = "GPL-3.0-only",
            repo = "example/cubiq-quest-port",
            assetPattern = "(?i)^.*-universal\\.apk$",
            sourceUrl = "https://github.com/example/cubiq-quest-port",
            screenshots = listOf("https://example.com/screenshots/cubiq-quest-1.png"),
            packageName = "com.example.cubiqquest",
            category = "puzzle",
            release = Release(
                tag = "v1.0.0",
                assets = listOf(
                    ReleaseAsset(
                        name = "cubiq-quest-v1.0.0-universal.apk",
                        size = 10_485_760,
                        url = "https://github.com/example/cubiq-quest-port/releases/download/v1.0.0/cubiq-quest-v1.0.0-universal.apk",
                        digest = "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                        abi = "universal",
                    ),
                ),
            ),
        ),
        Port(
            id = "neon-drift-port",
            name = "Neon Drift Port (demo)",
            description = LocalizedText(
                es = "Port de demostración de una carrera completamente ficticia. Requiere tu propia copia del juego original.",
            ),
            author = Author(name = "Autora de ejemplo"),
            license = "MIT",
            repo = "example/neon-drift-port",
            assetPattern = "(?i)^.*arm64-v8a\\.apk$",
            sourceUrl = "https://github.com/example/neon-drift-port",
            screenshots = listOf("https://example.com/screenshots/neon-drift-1.png"),
            packageName = "com.example.neondrift",
            category = "racing",
            release = Release(
                tag = "v1.2",
                assets = listOf(
                    ReleaseAsset(
                        name = "neon-drift-v1.2-arm64-v8a.apk",
                        size = 15_728_640,
                        url = "https://github.com/example/neon-drift-port/releases/download/v1.2/neon-drift-v1.2-arm64-v8a.apk",
                        digest = null,
                        abi = "arm64-v8a",
                    ),
                ),
            ),
        ),
        Port(
            id = "pixel-barge-port",
            name = "Pixel Barge Port (demo)",
            description = LocalizedText(
                es = "Port de demostración de una aventura completamente ficticia. Requiere tu propia copia del juego original. Sin capturas publicadas.",
                en = "Demonstration port of a fully fictional adventure. Requires your own copy of the original game. No published screenshots.",
            ),
            author = Author(name = "Autor de ejemplo", contact = "https://github.com/example"),
            license = "Apache-2.0",
            repo = "example/pixel-barge-port",
            assetPattern = "(?i)^.*-(arm64-v8a|universal)\\.apk$",
            sourceUrl = "https://github.com/example/pixel-barge-port",
            packageName = "com.example.pixelbarge",
            category = "adventure",
            release = Release(
                tag = "v2.0.0",
                assets = listOf(
                    ReleaseAsset(
                        name = "pixel-barge-v2.0.0-arm64-v8a.apk",
                        size = 12_582_912,
                        url = "https://github.com/example/pixel-barge-port/releases/download/v2.0.0/pixel-barge-v2.0.0-arm64-v8a.apk",
                        digest = "sha256:deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef",
                        abi = "arm64-v8a",
                    ),
                    ReleaseAsset(
                        name = "pixel-barge-v2.0.0-universal.apk",
                        size = 20_971_520,
                        url = "https://github.com/example/pixel-barge-port/releases/download/v2.0.0/pixel-barge-v2.0.0-universal.apk",
                        digest = "sha256:feedfacefeedfacefeedfacefeedfacefeedfacefeedfacefeedfacefeedface",
                        abi = "universal",
                    ),
                ),
            ),
        ),
    )
}
