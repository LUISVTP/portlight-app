package com.luisvtp.portlight.ui

/** Estado visible del refresco del catálogo en la lista. */
enum class CatalogRefreshState {
    /** Sin refresco en curso ni resultado que mostrar. */
    IDLE,

    /** Refresco en marcha. */
    UPDATING,

    /** 200: el catálogo se actualizó. */
    UPDATED,

    /** 304: el catálogo local ya estaba actualizado. */
    NOT_MODIFIED,

    /** Error, 404 o JSON inválido: se conserva el catálogo vigente. */
    FAILED,
}
