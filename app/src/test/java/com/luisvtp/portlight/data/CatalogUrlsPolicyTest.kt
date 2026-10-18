package com.luisvtp.portlight.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Política de red: HTTPS obligatorio y lista blanca de hosts. */
class CatalogUrlsPolicyTest {

    @Test
    fun `permite GitHub Pages sobre https`() {
        assertTrue(
            CatalogUrls.isAllowed("https://luisvtp.github.io/portlight-catalog/catalog.json"),
        )
    }

    @Test
    fun `permite raw githubusercontent sobre https`() {
        assertTrue(
            CatalogUrls.isAllowed(
                "https://raw.githubusercontent.com/LUISVTP/portlight-catalog/main/catalog.json",
            ),
        )
    }

    @Test
    fun `rechaza http sin cifrar`() {
        assertFalse(
            CatalogUrls.isAllowed("http://luisvtp.github.io/portlight-catalog/catalog.json"),
        )
    }

    @Test
    fun `rechaza hosts fuera de la lista blanca`() {
        assertFalse(CatalogUrls.isAllowed("https://evil.example.com/catalog.json"))
    }

    @Test
    fun `rechaza subdominios que imitan un host permitido`() {
        assertFalse(CatalogUrls.isAllowed("https://luisvtp.github.io.evil.com/catalog.json"))
    }

    @Test
    fun `rechaza urls malformadas o vacias`() {
        assertFalse(CatalogUrls.isAllowed("no es una url"))
        assertFalse(CatalogUrls.isAllowed(""))
    }
}
