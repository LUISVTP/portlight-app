# Portlight

Catálogo comunitario de *ports* de juegos para Android (Kotlin + Jetpack Compose), con UI en inglés y español.

- La app solo enlaza APKs publicados en las **Releases** de los repositorios originales de cada port: no aloja ni copia binarios.
- Cada entrada exige que la persona usuaria tenga su propia copia del juego.
- Repositorios: [LUISVTP/portlight-app](https://github.com/LUISVTP/portlight-app) (app) y [LUISVTP/portlight-catalog](https://github.com/LUISVTP/portlight-catalog) (catálogo, publicado en GitHub Pages).

## Licencia

GPL-3.0: el texto oficial está en [LICENSE](LICENSE), descargado de <https://www.gnu.org/licenses/gpl-3.0.txt>.

## Compilar y verificar

Requiere JDK 17 y Android SDK con `platforms;android-34` (Gradle wrapper incluido):

```bash
./gradlew :lint-rules:test :app:testDebugUnitTest :app:lintDebug    # Linux/macOS
gradlew.bat :lint-rules:test :app:testDebugUnitTest :app:lintDebug  # Windows
```

La UI vive en `values/` (inglés) y `values-es/` (español); Lint y los tests de paridad impiden texto hardcodeado en las fuentes.
