# 桌面端凭据存储：威胁模型与当前措施

对应 issue #12（登录令牌可被本机读取）。这份文档写清**防住了什么、没防住什么**，
避免给人"已经安全了"的错觉。

## 防护对象

| 攻击者 | 是否能防住 | 靠什么 |
| --- | --- | --- |
| 同机其它用户 | 能 | 密钥文件权限收紧（POSIX 600；Windows 移除宽泛主体的 NTFS ACL） |
| 拷走磁盘/备份后异地读取 | 能（口令开启时更强） | 同上；若开启口令保护，即使拿到文件也解不开 |
| 本机同用户的普通程序（PoC 那种：读文件 + 读注册表） | **口令开启时能防住；默认配置防不住** | 口令保护（PBKDF2 派生包裹密钥，攻击者没有口令就没有密钥材料） |
| 同用户下的恶意软件/注入进程 | **防不住** | 理论上无法在纯客户端解决：它能读内存、能调用同样的系统接口、能等用户输入口令 |
| 知道口令的人 | 防不住 | 口令即凭据 |

**结论**：同用户恶意软件这一档，客户端存储方式**无论怎么改都防不住**；
能改变的是"攻击者需要付出什么"：从"读一个文件"变成"注入进程或拿到口令"。

## 三层措施（都已实现）

1. **权限收紧**（`FileKeyProvider.hardenPermissions`）
   POSIX 走 `chmod 600`；Windows 上 `File.setReadable/setWritable` **只映射到 DOS 只读属性、
   不设置 NTFS ACL**，所以另用 `java.nio` 的 `AclFileAttributeView` 移除宽泛主体
   （Users / Authenticated Users / Everyone 等）。异常时只告警、不改动原 ACL。

2. **口令保护密钥**（`PassphraseKeyProvider`，默认关闭）
   `keys/<alias>.wrapped` = `salt(16) || iv(12) || AES-GCM 密文(32 字节密钥 + 16 字节 tag)`，
   包裹密钥由口令经 PBKDF2-HMAC-SHA256（12 万次迭代）派生。口令错误 -> **fail closed**（不落盘、不解密），
   不会退化成明文存储。
   开启方式（任一）：
   - 已存在 `keys/jm_session_v1.wrapped`；
   - 设置环境变量 `JMNEXT_KEY_PASSPHRASE_ENABLE=1`（首次启动会要求设置口令）；
   - 自动化/无界面场景用 `JMNEXT_KEY_PASSPHRASE=<口令>` 直接提供。

3. **登出即销毁密钥材料**（`SecretKeyProvider.forget` + `SecureStore.clear`）
   只清 prefs/注册表而留下密钥文件，等于"换了锁但钥匙还挂在门上"：之前加密过的内容仍可被解出。
   现在 `clear()` 会同时删除 `<alias>.key` / `<alias>.wrapped` 并清掉进程内缓存。

## 验证方法（可在真机自查）

```bat
:: 1. 密钥文件权限（加固后不应出现 BUILTIN\Users:(R) 之类）
icacls "%USERPROFILE%\.config\jmnext\keys\jm_session_v1.key"
:: 2. 开启口令保护后，PoC 应当失败（解不开）
set JMNEXT_KEY_PASSPHRASE_ENABLE=1
:: 3. 登出后确认密钥文件已消失
dir "%USERPROFILE%\.config\jmnext\keys"
```

## 仍未落实（需要产品/服务端配合）

- **不持久化 JWT**：只在内存里保留会话，重启要求重新登录；或改为短效 JWT + 刷新令牌轮换，
  并由服务端支持撤销（登出/改密后旧令牌立即失效）。这是**降低令牌本身价值**的做法，
  比继续加固本地存储更有效，但需要服务端配合。
- **新安装是否默认开启口令保护**：目前默认关闭（老用户行为不变），是否对没有旧密钥文件的新安装
  默认开启，属产品决定。

## 验证状态（如实记录）

- 桌面模块编译通过；两步加密/权限行为用同一个 JDK 复现并验证（错口令与文件改动均失败、
  包裹结果不含明文密钥、POSIX 权限收紧为 rw-------）。
- **未在 Windows 实测**：ACL 分支与注册表路径的实际表现（开发环境无 Windows）。
- **未验证**：Swing 口令框在真实桌面的交互、以及启动-输入口令-解密的端到端流程（开发环境无图形界面）。
- **未验证**：Android 侧未重新编译（本次改动是共享接口上带默认实现的 `forget`，按设计不影响 Android）。
