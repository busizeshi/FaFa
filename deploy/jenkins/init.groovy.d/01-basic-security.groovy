/*
 * Jenkins 首次启动初始化（放在 init.groovy.d，容器启动时自动执行）
 *
 * 职责：
 *   1. 关闭安装向导（compose 已加 -Djenkins.install.runSetupWizard=false，此处兜底）；
 *   2. 创建管理员账号（账号密码由 compose 环境变量注入，默认 admin / FafaJenkins2026!）；
 *   3. 设置 Jenkins 根地址（Gitee Webhook / 远程触发需要正确的对外 URL）；
 *   4. 设置执行器数量（单机小型 CI，2 个足够）。
 *
 * 幂等：重复启动不会重复创建账号、不会覆盖已设置的管理员密码。
 */
import jenkins.model.Jenkins
import jenkins.install.InstallState
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy
import hudson.security.HudsonPrivateSecurityRealm
import jenkins.model.JenkinsLocationConfiguration

def instance = Jenkins.get()

def adminId = System.getenv('JENKINS_ADMIN_ID') ?: 'admin'
def adminPassword = System.getenv('JENKINS_ADMIN_PASSWORD') ?: 'FafaJenkins2026!'
def jenkinsUrl = System.getenv('JENKINS_URL') ?: 'http://192.168.1.254:9095/'

// ---- 1. 安全域：内置账号库 + 仅登录用户可操作 ----
def realm = instance.getSecurityRealm()
if (!(realm instanceof HudsonPrivateSecurityRealm)) {
    realm = new HudsonPrivateSecurityRealm(false)
    instance.setSecurityRealm(realm)
}
if (realm.getUser(adminId) == null) {
    realm.createAccount(adminId, adminPassword)
    println "[init] 已创建管理员账号: ${adminId}"
} else {
    println "[init] 管理员账号已存在，跳过创建: ${adminId}"
}

def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)

// ---- 2. 根地址（远程触发/邮件链接/Webhook 依赖）----
def location = JenkinsLocationConfiguration.get()
if (location.getUrl() != jenkinsUrl) {
    location.setUrl(jenkinsUrl)
    location.save()
    println "[init] Jenkins URL 已设置为: ${jenkinsUrl}"
}

// ---- 3. 执行器与安装状态 ----
instance.setNumExecutors(2)
if (!instance.getInstallState().isSetupComplete()) {
    instance.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)
    println '[init] 安装向导已标记完成'
}

instance.save()
println '[init] 初始化完成'
