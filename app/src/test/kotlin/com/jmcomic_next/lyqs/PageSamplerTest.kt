package com.jmcomic_next.lyqs

import com.jmcomic_next.lyqs.data.PageSampler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageSamplerTest {
    @Test
    fun `命中预取时延迟记 0，停留按屏幕级结算`() {
        PageSampler.clear()
        PageSampler.onPageEntered("p1", now = 1_000)
        PageSampler.onImageReady("p1", now = 1_050, hitCache = true, bytes = 0L)
        val s = PageSampler.settle("p1", now = 4_000)!!
        assertEquals(0L, s.latencyMs)
        assertTrue(s.hitCache)
        assertEquals(3_000L, s.dwellMs)
        assertFalse(s.failed)
        assertEquals(0L, s.bytes)
    }

    @Test
    fun `未命中时延迟按进页到图到位计算，字节未知保持 null`() {
        PageSampler.clear()
        PageSampler.onPageEntered("p2", now = 10_000)
        PageSampler.onImageReady("p2", now = 11_200, hitCache = false, bytes = null)
        val s = PageSampler.settle("p2", now = 12_000)!!
        assertEquals(1_200L, s.latencyMs)
        assertFalse(s.hitCache)
        assertNull("拿不到字节数就必须是 null，不能是 0", s.bytes)
        assertEquals(2_000L, s.dwellMs)
        assertFalse(s.failed)
    }

    @Test
    fun `图始终没到位记成失败样本`() {
        PageSampler.clear()
        PageSampler.onPageEntered("p3", now = 100)
        val s = PageSampler.settle("p3", now = 900)!!
        assertTrue(s.failed)
        assertEquals(800L, s.dwellMs)
    }

    @Test
    fun `没有记录的页结算返回 null（例如切章）`() {
        PageSampler.clear()
        assertNull(PageSampler.settle("不存在", now = 1_000))
    }

    @Test
    fun `结算后记录被移除，且清空会丢弃全部`() {
        PageSampler.clear()
        PageSampler.onPageEntered("p4", now = 1)
        PageSampler.onPageEntered("p5", now = 2)
        assertEquals(2, PageSampler.pendingCount())
        PageSampler.settle("p4", now = 10)
        assertEquals(1, PageSampler.pendingCount())
        PageSampler.clear()
        assertEquals(0, PageSampler.pendingCount())
    }
}
