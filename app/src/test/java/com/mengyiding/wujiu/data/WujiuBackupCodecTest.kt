package com.mengyiding.wujiu.data

import com.mengyiding.wujiu.data.backup.BackupAssetRecord
import com.mengyiding.wujiu.data.backup.BackupCategoryRecord
import com.mengyiding.wujiu.data.backup.WujiuBackupCodec
import com.mengyiding.wujiu.data.backup.WujiuBackupData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WujiuBackupCodecTest {
    @Test
    fun `encode and decode round-trips backup payload accurately`() {
        val original = WujiuBackupData(
            version = 1,
            exportedAtMillis = 1_760_000_000_000L,
            categories = listOf(
                BackupCategoryRecord("手机", "phone", true),
                BackupCategoryRecord("乐器", "other", false),
            ),
            assets = listOf(
                BackupAssetRecord("MacBook Pro", "电脑", 1_499_900L, 20_000L, "工作主力", null),
                BackupAssetRecord("老镜头", "相机", 350_000L, 19_900L, "已出", 20_010L),
            ),
        )

        val decoded = WujiuBackupCodec.decode(WujiuBackupCodec.encode(original))
        assertEquals(original, decoded)
        assertNull(decoded.assets[0].inactiveDateEpochDay)
        assertEquals(20_010L, decoded.assets[1].inactiveDateEpochDay)
    }

    @Test
    fun `decode rejects malformed json, unsupported versions, and invalid records`() {
        assertThrows(IllegalArgumentException::class.java) {
            WujiuBackupCodec.decode("   ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WujiuBackupCodec.decode("{not-json")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WujiuBackupCodec.decode("""{"version":99,"categories":[],"assets":[]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WujiuBackupCodec.decode(
                """{"version":1,"categories":[],"assets":[{"name":"Bad","categoryName":"其他","priceCents":0,"purchaseDateEpochDay":20000}]}""",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            WujiuBackupCodec.decode(
                """{"version":1,"categories":[],"assets":[{"name":"BadDate","categoryName":"其他","priceCents":100,"purchaseDateEpochDay":20010,"inactiveDateEpochDay":20000}]}""",
            )
        }
    }
}
