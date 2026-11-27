package es.nuskysoftware.marketsales.utils

// Responsabilidad: Localiza la Activity asociada a un Context de Android.


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
