package com.vortix.app.data.battery

import android.annotation.SuppressLint
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
open class SystemDesignCapacityReader(
    private val capacityProvider: () -> Int?
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this({
        readFromPowerProfile(context)
    })

    /**
     * Cihazın fabrika tasarım kapasitesini (mAh) döner; okunamazsa null döner.
     */
    open fun readSystemDesignCapacityMah(): Int? = capacityProvider()

    companion object {
        @SuppressLint("PrivateApi")
        private fun readFromPowerProfile(context: Context): Int? = runCatching {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val instance = powerProfileClass.getConstructor(Context::class.java).newInstance(context)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = (method.invoke(instance) as? Double)?.roundToInt()
            capacity?.takeIf { it in 1500..12000 }
        }.getOrNull()
    }
}
