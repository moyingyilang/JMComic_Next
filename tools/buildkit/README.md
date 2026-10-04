# buildkit（跨架构构建脚本集）

把"在 Termux + Linux 容器里构建 Android 与 Compose Desktop、并跨架构打包"这套流程参数化后的脚本集。
目标：换台机器、换个架构，改 `build.env` 就能跑，而不是重踩一遍坑。

## 用法

```bash
cp env.example build.env   # 按自己的环境改路径与 build-tools 版本
./10-enter.sh              # 进入构建环境 + 绑定挂载自检（挂载掉了是最常见的坑）
./20-build-android.sh      # 打 Android release 包（full/lite），带产物新鲜度自检
./30-verify-apk.sh         # 验收：包名、版本、签名
```

## 已验证 / 未验证

- **在本机实测通过**：`10-enter.sh`（容器内可见项目与 Android SDK）、`20`/`30` 的判据来源
  （`aapt2 dump badging` 的包名与版本、`apksigner verify`、时间戳自检）；
- **未验证**：在其他机器、其他发行版、proot 而非 chroot、x86_64 宿主机上的表现；
  以及 desktop 打包脚本（本仓库尚未收敛进来）。

## 许可

GPL-2.0-or-later（见 `LICENSE`）。脚本源自作者自己的项目，版权归作者，故可另行授权。
