package com.jmnext.desktop

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 凭据存储改造（issue #11 节点迁移、#12 口令保护/销毁）的可回归测试。
 *
 * 为什么要有它：这些改动最初只有编译验证；`SelfCheck` 提供了可执行自检，
 * 但只有手动运行才会跑。放进测试源码集后，`gradle test` 与 CI 都会覆盖它们。
 */
class CredentialStoreTest {

    @Test
    fun credentialStoreSelfCheckPasses() {
        assertEquals("凭据存储自检应全部通过（失败项见标准输出）", 0, SelfCheck.run())
    }
}
