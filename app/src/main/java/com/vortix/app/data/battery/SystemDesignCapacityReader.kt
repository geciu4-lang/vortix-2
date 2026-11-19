package com.vortix.app.data.battery

import android.annotation.SuppressLint
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class SystemDesignCapacityReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Cihazın PowerProfile sistem sınıfından fabrika tasarım kapasitesini (mAh) okumayı dener.
     * Üretici erişimi kısıtladıysa null döner.
     */
    @SuppressLint("PrivateApi")
    fun readSystemDesignCapacityMah(): Int? = runCatching {
        val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
        val instance = powerProfileClass.getConstructor(Context::class.java).newInstance(context)
        val method = powerProfileClass.getMethod("getBatteryCapacity")
        val capacity = (method.invoke(instance) as? Double)?.roundToInt()
        capacity?.takeIf { it in 1500..12000 }
    }.getOrNull()
}
