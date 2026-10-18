package com.luisvtp.portlight.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Cada salto de redirección se valida contra la lista blanca de hosts. */
class RedirectPolicyTest {

    private val policy = RedirectPolicy(
        allowedHosts = setOf("github.com", "objects.githubusercontent.com"),
    )

    @Test
    fun `acepta un host de la lista blanca sobre https`() {
        val next = policy.next(
            fromUrl = "https://github.com/owner/repo/releases/download/v1/a.apk",
            location = "https://objects.githubusercontent.com/hash/path",
            alreadyFollowed = 0,
        )
        assertEquals("https://objects.githubusercontent.com/hash/path", next)
    }

    @Test
    fun `rechaza un host fuera de la lista blanca`() {
        assertNull(
            policy.next(
                fromUrl = "https://github.com/o/r/releases/download/v1/a.apk",
                location = "https://evil.example.com/payload",
                alreadyFollowed = 0,
            ),
        )
    }

    @Test
    fun `rechaza esquemas no permitidos`() {
        assertNull(
            policy.next(
                fromUrl = "https://github.com/o/r/releases/download/v1/a.apk",
                location = "http://github.com/algo",
                alreadyFollowed = 0,
            ),
        )
    }

    @Test
    fun `rechaza Location ausente o vacia`() {
        assertNull(policy.next("https://github.com/o/r", null, 0))
        assertNull(policy.next("https://github.com/o/r", "  ", 0))
    }

    @Test
    fun `resuelve Location relativa contra la url actual`() {
        val next = policy.next(
            fromUrl = "https://github.com/o/r/releases/download/v1/a.apk",
            location = "/o/r/releases/download/v1/b.apk",
            alreadyFollowed = 0,
        )
        assertEquals("https://github.com/o/r/releases/download/v1/b.apk", next)
    }

    @Test
    fun `rechaza Location relativa que cambia de host`() {
        assertNull(
            policy.next(
                fromUrl = "https://github.com/o/r",
                location = "//evil.example.com/x",
                alreadyFollowed = 0,
            ),
        )
    }

    @Test
    fun `rechaza al superar el limite de saltos`() {
        assertNull(
            policy.next(
                fromUrl = "https://github.com/o/r",
                location = "https://objects.githubusercontent.com/x",
                alreadyFollowed = RedirectPolicy.DEFAULT_MAX_HOPS,
            ),
        )
    }

    @Test
    fun `rechaza Location malformada`() {
        assertNull(policy.next("https://github.com/o/r", "ht tp://malformed", 0))
    }

    @Test
    fun `los esquemas permitidos son configurables solo para pruebas locales`() {
        // En producción el default es HTTPS; aquí se inyecta http para el
        // servidor local de pruebas (nunca se contacta repositorios reales).
        val local = RedirectPolicy(
            allowedHosts = setOf("127.0.0.1"),
            allowedSchemes = setOf("http"),
        )
        assertEquals(
            "http://127.0.0.1:1234/destino",
            local.next("http://127.0.0.1:1234/origen", "/destino", 0),
        )
        // Pero la política por defecto sigue exigiendo HTTPS.
        assertNull(policy.next("http://github.com/o/r", "http://github.com/x", 0))
    }
}
