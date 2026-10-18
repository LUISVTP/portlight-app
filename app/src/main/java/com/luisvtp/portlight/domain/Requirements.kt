package com.luisvtp.portlight.domain

/**
 * Compatibilidad del dispositivo con el mínimo que declara el port
 * (`min_sdk_port`, Fase 4). Devuelve el mínimo incumplido o null si el
 * dispositivo lo cumple (o si el port no declara mínimo).
 */
fun unmetMinSdk(sdkInt: Int, minSdkPort: Int?): Int? = minSdkPort?.takeIf { sdkInt < it }
