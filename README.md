# Portlight

Catálogo comunitario de *ports* de juegos para Android, con descarga directa
desde las Releases de cada repositorio original. Kotlin + Jetpack Compose, UI
en inglés y español (sigue el idioma del sistema).

> A community catalog of Android game ports with direct downloads from each
> port's original repository releases. Kotlin + Jetpack Compose, English and
> Spanish UI (follows the system language).

## Qué es / What it is

- **Catálogo y descargador sin alojar APKs**: la app solo consulta un catálogo
  JSON y enlaza APKs publicados en las **Releases** de los repositorios
  originales de cada port. **No aloja, copia ni redistribuye binarios**; cada
  descarga sale del repositorio original y se verifica con su SHA-256 antes de
  instalar.
  > **A catalog and downloader that hosts no APKs**: the app reads a JSON
  > catalog and links APKs published in each port's original repository
  > **Releases**. It never hosts, copies or redistributes binaries; every
  > download comes from the original repository and is SHA-256-verified before
  > installation.
- **Copia propia obligatoria**: ninguna entrada contiene datos de juegos; cada
  port exige que la persona usuaria aporte su propia copia del juego original
  (`requires_user_game` y `no_game_assets` son siempre `true`).
  > **Your own copy required**: no entry ships game data; each port requires
  > the user to provide their own copy of the original game.
- **Catálogo**: [LUISVTP/portlight-catalog](https://github.com/LUISVTP/portlight-catalog),
  publicado en GitHub Pages y consumido en
  <https://luisvtp.github.io/portlight-catalog/catalog.json>.
  > **Catalog**: published at the link above.

## Contacto y retiradas / Contact and takedowns

Para solicitar la **retirada de una entrada** (o cualquier corrección del
catálogo), usa la sección **Issues** de
<https://github.com/LUISVTP/portlight-app/issues>. Respondemos en ambos
idiomas. El mismo canal aparece en la pantalla legal de la app.
> To request the **removal of a listing** (or any catalog correction), use the
> **Issues** section of <https://github.com/LUISVTP/portlight-app/issues>. We
> answer in both languages. The same channel appears in the app's legal
> screen.

## Compilar / Build

Requiere JDK 17 y Android SDK con `platforms;android-34` (Gradle wrapper
incluido) / Requires JDK 17 and the Android SDK with `platforms;android-34`
(Gradle wrapper included):

```bash
./gradlew :app:assembleDebug                           # APK de depuración / debug APK
./gradlew :lint-rules:test :app:testDebugUnitTest :app:lintDebug   # gate completo
```

La UI vive en `values/` (inglés) y `values-es/` (español); Lint y los tests de
paridad impiden texto hardcodeado. La firma de release se configura con
`keystore.properties` (no versionado): ver [docs/RELEASING.md](docs/RELEASING.md).
> The UI lives in `values/` (English) and `values-es/` (Spanish); Lint and the
> parity tests prevent hardcoded text. Release signing is configured via the
> unversioned `keystore.properties`: see [docs/RELEASING.md](docs/RELEASING.md).

## Repositorios / Repositories

- App: [LUISVTP/portlight-app](https://github.com/LUISVTP/portlight-app)
- Catálogo / Catalog: [LUISVTP/portlight-catalog](https://github.com/LUISVTP/portlight-catalog)

## Licencia / License

GPL-3.0: el texto oficial está en [LICENSE](LICENSE); procede de
<https://www.gnu.org/licenses/gpl-3.0.txt>.
> GPL-3.0: the official text is in [LICENSE](LICENSE).
