package com.mengyiding.wujiu.data

import com.mengyiding.wujiu.data.local.defaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoriesTest {
    @Test
    fun `default categories are unique and complete`() {
        val names = defaultCategories.map { it.first }
        assertEquals(8, names.size)
        assertEquals(names.size, names.toSet().size)
        assertTrue("其他" in names)
    }
}
