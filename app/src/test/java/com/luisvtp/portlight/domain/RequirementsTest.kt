package com.luisvtp.portlight.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Advertencia de SDK (Fase 4): el mínimo incumplido, o nada. */
class RequirementsTest {

    @Test
    fun `dispositivo por debajo del minimo devuelve el minimo`() {
        assertEquals(30, unmetMinSdk(29, 30))
        assertEquals(24, unmetMinSdk(23, 24))
    }

    @Test
    fun `dispositivo en el minimo o por encima no avisa`() {
        assertNull(unmetMinSdk(30, 30))
        assertNull(unmetMinSdk(35, 30))
    }

    @Test
    fun `port sin minimo declarado nunca avisa`() {
        assertNull(unmetMinSdk(21, null))
        assertNull(unmetMinSdk(99, null))
    }
}
