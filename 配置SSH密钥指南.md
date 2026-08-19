# SSH 密钥配置指南

## 为什么需要配置 SSH 密钥？

配置后可以：
- ✅ 无需每次输入密码
- ✅ AI 可以直接帮你执行远程命令
- ✅ 更安全（比密码更安全）
- ✅ 自动化脚本更方便

---

## 快速配置（5 分钟完成）

### 步骤 1：生成 SSH 密钥对（本地 Windows）

```powershell
# 检查是否已有密钥
Test-Path ~\.ssh\id_rsa.pub

# 如果没有，生成新密钥
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
# 按 Enter 使用默认路径
# 按 Enter 跳过密码（或设置密码）
# 再按 Enter 确认
```

### 步骤 2：复制公钥到服务器

```powershell
# 方法 1：使用 ssh-copy-id（如果有）
ssh-copy-id busizeshi@192.168.1.14

# 方法 2：手动复制
type ~\.ssh\id_rsa.pub | ssh busizeshi@192.168.1.14 "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

### 步骤 3：设置权限（在服务器上）

```bash
ssh busizeshi@192.168.1.14
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
exit
```

### 步骤 4：测试连接

```powershell
# 应该无需密码直接连接
ssh busizeshi@192.168.1.14 "echo '连接成功！'"
```

---

## 配置完成后的优势

```powershell
# 我就可以直接帮你执行命令了
ssh busizeshi@192.168.1.14 "docker ps -a"
ssh busizeshi@192.168.1.14 "docker stop \$(docker ps -q)"
ssh busizeshi@192.168.1.14 "sudo rm -rf /home/busizeshi/docker"
```

---

## 故障排除

### 问题 1：仍然要求输入密码

**解决方案**：
```bash
# 在服务器上检查权限
ls -la ~/.ssh/
# 应该显示：
# drwx------ 2 busizeshi busizeshi 4096 ... .
# -rw------- 1 busizeshi busizeshi  xxx ... authorized_keys

# 如果权限不对，修正：
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

### 问题 2：找不到 ssh-keygen 命令

**解决方案**：
```powershell
# Windows 10/11 应该自带 OpenSSH
# 如果没有，启用它：
Add-WindowsCapability -Online -Name OpenSSH.Client~~~~0.0.1.0
```

### 问题 3：公钥格式错误

**解决方案**：
```bash
# 在服务器上查看公钥格式
cat ~/.ssh/authorized_keys
# 应该是一行，格式：ssh-rsa AAAA... your_email@example.com

# 如果有多行或格式错误，重新复制
```

---

配置完成后告诉我，我就可以直接帮你清理服务器了！
