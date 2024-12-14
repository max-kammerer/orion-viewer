package universe.constellation.orion.viewer.test.device

import org.junit.Assert
import org.junit.Test
import universe.constellation.orion.viewer.device.M_1024_MB
import universe.constellation.orion.viewer.device.M_1536_MB
import universe.constellation.orion.viewer.device.M_2048_MB
import universe.constellation.orion.viewer.device.M_256_MB
import universe.constellation.orion.viewer.device.M_512_MB
import universe.constellation.orion.viewer.device.calcFZCacheSize

class FzCacheTest {

    @Test
    fun test() {
        Assert.assertEquals(48L shl 20, calcFZCacheSize(0L))

        Assert.assertEquals(48L shl 20, calcFZCacheSize(M_256_MB - 1))
        Assert.assertEquals(48L shl 20, calcFZCacheSize(M_256_MB))

        Assert.assertEquals(64L shl 20, calcFZCacheSize(M_256_MB + 1))
        Assert.assertEquals(64L shl 20, calcFZCacheSize(M_512_MB))

        Assert.assertEquals(96L shl 20, calcFZCacheSize(M_512_MB + 1))
        Assert.assertEquals(96L shl 20, calcFZCacheSize(M_1024_MB))

        Assert.assertEquals(128L shl 20, calcFZCacheSize(M_1024_MB + 1))
        Assert.assertEquals(128L shl 20, calcFZCacheSize(M_1536_MB))

        Assert.assertEquals(160L shl 20, calcFZCacheSize(M_1536_MB + 1))
        Assert.assertEquals(160L shl 20, calcFZCacheSize(M_2048_MB))
    }
}