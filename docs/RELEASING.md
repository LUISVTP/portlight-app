# Publicación de Portlight / Portlight release guide

Guía para compilar y firmar la APK de release. **Ningún paso de esta guía
sube claves al repositorio**; el keystore y `keystore.properties` están en
`.gitignore`.

This guide covers building and signing the release APK. **No step in this
guide ever commits keys to the repo**; the keystore and `keystore.properties`
are gitignored.

---

## (a) Generar el keystore fuera del repo / Generate the keystore outside the repo

Usa `keytool` (viene con el JDK 17) y guarda el fichero **fuera del árbol del
repositorio** (p. ej. `~/.android/keystores/` o un disco externo):

```bash
keytool -genkeypair -v \
  -keystore /ruta/fuera-del-repo/portlight-release.jks \
  -alias portlight \
  -keyalg RSA -keysize 4096 -validity 10000
```

Use `keytool` (bundled with JDK 17) and store the file **outside the repo
tree** (e.g. `~/.android/keystores/` or an external drive). You will be asked
for a store password and a key password; **do not type them anywhere in this
repository**.

`keytool` te pedirá contraseña del almacén y de la clave: **no las escribas en
ningún fichero de este repositorio** salvo `keystore.properties` (no
versionado) o variables de entorno.

## (b) Copia de seguridad en dos sitios / Back it up in two places

Copia el `.jks`/`.keystore` a **al menos dos ubicaciones distintas** (p. ej.
disco externo y gestor de contraseñas/cloud personal cifrado). Sin el keystore
no podrás actualizar la app en Google Play, así que verifica que las copias
abren (`keytool -list -keystore <copia>`).

Copy the keystore to **at least two separate locations** (e.g. an external
drive and your personal encrypted cloud/password manager). Without the
keystore you cannot update the app, so verify the copies open with
`keytool -list -keystore <copy>`.

## (c) Crear `keystore.properties` / Create `keystore.properties`

En la **raíz** del repo:

```bash
cp keystore.properties.example keystore.properties
```

Rellena `storeFile` (ruta absoluta o relativa a la raíz), `storePassword`,
`keyAlias` y `keyPassword`. Alternativa: variables de entorno
`PORTLIGHT_KEYSTORE_PATH`, `PORTLIGHT_KEYSTORE_PASSWORD`, `PORTLIGHT_KEY_ALIAS`
y `PORTLIGHT_KEY_PASSWORD`.

At the **repo root**, fill `storeFile` (absolute or repo-root-relative path),
`storePassword`, `keyAlias` and `keyPassword`. Alternatively set the
`PORTLIGHT_KEYSTORE_PATH`, `PORTLIGHT_KEYSTORE_PASSWORD`, `PORTLIGHT_KEY_ALIAS`
and `PORTLIGHT_KEY_PASSWORD` environment variables.

`keystore.properties` está en `.gitignore`. Antes de cada push, comprueba que
no se cuele:

```bash
git status --ignored=no
git ls-files | findstr keystore   # PowerShell; en bash: git ls-files | grep keystore
```

## (d) Compilar la release / Build the release

```powershell
gradlew.bat :app:assembleRelease --console=plain     # Windows
./gradlew :app:assembleRelease --console=plain       # Linux/macOS
```

Si falta la firma, **el build falla con un mensaje claro** (ES/EN) en lugar de
generar una APK rota. El resultado es
`app\build\outputs\apk\release\app-release.apk`.

If signing is missing the build **fails with a clear bilingual message**
instead of producing a broken APK. Output:
`app/build/outputs/apk/release/app-release.apk`.

**Minify/shrink:** `isMinifyEnabled` e `isShrinkResources` están activados en
`buildTypes.release`, con las reglas de `app/proguard-rules.pro`
(kotlinx.serialization + OkHttp). `assembleRelease` verifica que R8 compila y
enlaza; **antes de publicar, haz una prueba en dispositivo**: instala la APK,
arranca la app, carga el catálogo, abre un detalle, consulta la pantalla legal
y prueba un diálogo de descarga. (Medición de tamaño sin firmar:
`gradlew :app:assembleRelease -PportlightReleaseUnsigned`.)

**Minify/shrink** is enabled in `buildTypes.release` with the rules in
`app/proguard-rules.pro`. Before publishing, run a **device smoke test**:
install, open the catalog, open a port detail, check the legal screen and a
download dialog. (Unsigned size check: `gradlew :app:assembleRelease
-PportlightReleaseUnsigned`.)

## (e) Verificar la firma / Verify the signature

```powershell
apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk
```

Comprueba que el certificado (`DN`, `SHA-256`) es el tuyo y que la APK es
`Verified`.

Confirm the certificate (`DN`, `SHA-256`) is yours and the APK reports
`Verified`.

## (f) SHA-256 para las notas de la release / SHA-256 for the release notes

```powershell
# Windows
Get-FileHash -Algorithm SHA256 app\build\outputs\apk\release\app-release.apk
# Linux/macOS
sha256sum app/build/outputs/apk/release/app-release.apk
```

Pega el hash en las notas de la release para que la gente pueda verificar la
descarga. Publica la release desde la sección **Releases** de
[LUISVTP/portlight-app](https://github.com/LUISVTP/portlight-app); las
etiquetas y releases se crean a mano, nunca desde este pipeline.

Paste the hash into the release notes so users can verify the download.
Create the tag/release manually in the GitHub **Releases** section.

---

## Reglas / Rules

- **No** subir `keystore.properties`, `*.jks` ni `*.keystore` (hay tests de
  auditoría previa al push y `.gitignore` los cubre). — **Never** commit
  keystore material.
- **No** generar claves desde CI ni guardarlas en el repo. — Never generate or
  store keys in CI or the repo.
- El canal de retirada de entradas es la sección **Issues** de
  <https://github.com/LUISVTP/portlight-app/issues>. — Takedown channel: the
  repo **Issues** section.
