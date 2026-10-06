package com.jmnext.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 无头收集 baseline profile。
 *
 * 设计取舍：
 * - 不查找具体控件（目标应用启动后持续加载，uiautomator 拿不到 idle 状态），
 *   只做与内容无关的动作，覆盖"启动 + 列表加载 + 图片解码 + 滚动布局"这些热路径；
 * - 手势全部包在 try/catch 里：某些设备/系统版本会拒绝 instrumentation 注入输入事件，
 *   此时仍然保留"启动 + 等待"这段覆盖，而不是让整次收集失败；
 * - 只输出方法签名，profile 本身不含任何内容数据。
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        rule.collect(packageName = TARGET_PACKAGE) {
            // 无头唤醒：屏幕熄灭时输入注入会被系统拒绝
            runCatching { device.wakeUp() }
            pressHome()
            startActivityAndWait()
            // 首屏列表 + 图片解码
            Thread.sleep(8_000)
            // 手势：能注入就覆盖滚动，不能注入也不影响本次收集
            repeat(3) {
                runCatching {
                    val w = device.displayWidth
                    val h = device.displayHeight
                    device.swipe(w / 2, (h * 0.72f).toInt(), w / 2, (h * 0.30f).toInt(), 24)
                }
                Thread.sleep(2_500)
            }
            repeat(2) {
                runCatching {
                    val w = device.displayWidth
                    val h = device.displayHeight
                    device.swipe(w / 2, (h * 0.30f).toInt(), w / 2, (h * 0.72f).toInt(), 24)
                }
                Thread.sleep(1_500)
            }
            Thread.sleep(2_000)
        }
    }

    private companion object {
        const val TARGET_PACKAGE = "com.jmnext"
    }
}
