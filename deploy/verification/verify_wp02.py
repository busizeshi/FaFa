"""在独立验收库验证 WP02/WP03；凭证只由环境和被 Git 忽略的本地状态提供。"""
from __future__ import annotations

import concurrent.futures
import hashlib
import hmac
import json
import logging
import os
from pathlib import Path
import secrets
import subprocess
import time
import socket
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any

import pymysql
import requests

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "fafa-java" / "target" / "wp02-verification"
DATABASES = {"biz": "fafa_wp02_biz_test", "admin": "fafa_wp02_admin_test"}
PORTS = {"biz": 18082, "admin": 18081, "gateway": 18080}
LOG = logging.getLogger(__name__)


class Verification:
    def __init__(self) -> None:
        OUTPUT.mkdir(parents=True, exist_ok=True)
        self.run_output = OUTPUT / "runs" / datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
        self.run_output.mkdir(parents=True, exist_ok=True)
        self.host = os.environ["FAFA_VERIFY_DB_HOST"]
        self.root_password = os.environ["FAFA_VERIFY_DB_ROOT_PASSWORD"]
        private_file = OUTPUT / "private.json"
        if private_file.exists():
            self.private = json.loads(private_file.read_text(encoding="utf-8"))
        else:
            self.private = {key: secrets.token_urlsafe(36) for key in ("biz", "admin", "rpc", "hmac", "password")}
            private_file.write_text(json.dumps(self.private), encoding="utf-8")
        self.environment = os.environ.copy()
        self.environment.update({
            "FAFA_INTERNAL_RPC_SECRET": self.private["rpc"],
            "FAFA_AUTH_HMAC_SECRET": self.private["hmac"],
            "FAFA_REDIS_HOST": os.environ["FAFA_VERIFY_REDIS_HOST"],
            "FAFA_REDIS_PASSWORD": os.environ["FAFA_VERIFY_REDIS_PASSWORD"],
            "FAFA_MAIL_FROM": "verification@fafa.invalid",
            "FAFA_SMTP_HOST": "127.0.0.1", "FAFA_SMTP_PORT": "1",
            "FAFA_INITIAL_STORAGE_BYTES": "0", "FAFA_TRUSTED_PROXY_ADDRESSES": "127.0.0.1",
            "FAFA_ADMIN_INIT_USERNAME": "verification-admin",
            "FAFA_ADMIN_INIT_PASSWORD": self.private["password"],
            "FAFA_ADMIN_URL": "http://127.0.0.1:18081", "FAFA_BIZ_URL": "http://127.0.0.1:18082",
            "FAFA_BIZ_RPC_URL": "tri://127.0.0.1:20882",
            "SPRING_PROFILES_ACTIVE": os.environ.get("FAFA_VERIFY_PROFILE", "dev"),
        })
        for kind, database in DATABASES.items():
            prefix = "FAFA_DB" if kind == "biz" else "FAFA_ADMIN_DB"
            self.environment.update({f"{prefix}_URL": self.jdbc_url(database),
                                     f"{prefix}_USERNAME": f"fafa_wp02_{kind}",
                                     f"{prefix}_PASSWORD": self.private[kind]})
        self.processes: list[subprocess.Popen[Any]] = []
        self.logs: list[Any] = []
        self.results: dict[str, Any] = {"databaseHost": self.host, "profile": self.environment["SPRING_PROFILES_ACTIVE"],
                                       "checks": [], "smtp": "未部署；注册使用显式 SENT 测试夹具"}

    def jdbc_url(self, database: str) -> str:
        return f"jdbc:mysql://{self.host}:3306/{database}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"

    def connection(self, kind: str) -> Any:
        return pymysql.connect(host=self.host, user=f"fafa_wp02_{kind}", password=self.private[kind],
                               database=DATABASES[kind], charset="utf8mb4", autocommit=True)

    def prepare_databases(self) -> None:
        with pymysql.connect(host=self.host, user="root", password=self.root_password, autocommit=True) as connection:
            with connection.cursor() as cursor:
                cursor.execute("SELECT VERSION()")
                self.results["mysqlVersion"] = cursor.fetchone()[0]
                for kind, database in DATABASES.items():
                    # 数据库和用户标识只来自本脚本固定的验收白名单。
                    cursor.execute(f"CREATE DATABASE IF NOT EXISTS `{database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin")
                    username = f"fafa_wp02_{kind}"
                    cursor.execute("CREATE USER IF NOT EXISTS %s@'%%' IDENTIFIED BY %s", (username, self.private[kind]))
                    cursor.execute(f"GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX,REFERENCES ON `{database}`.* TO %s@'%%'", (username,))
        self.record("独立 Biz/Admin 库与互不跨库授权已创建")

    def launch(self, module: str, init: bool = False) -> subprocess.Popen[Any]:
        environment = self.environment.copy()
        environment["SERVER_PORT"] = str(PORTS[module])
        environment["DUBBO_PORT"] = "20882" if module == "biz" else "-1"
        if init:
            environment["SPRING_PROFILES_ACTIVE"] = "dev,admin-init"
        jar = ROOT / "fafa-java" / f"fafa-{module}" / "target" / f"fafa-{module}-0.1.0-SNAPSHOT.jar"
        log = (self.run_output / f"{module}{'-init' if init else ''}.log").open("w", encoding="utf-8")
        self.logs.append(log)
        java = str(Path(os.environ["JAVA_HOME"]) / "bin" / ("java.exe" if os.name == "nt" else "java"))
        if not init:
            with socket.socket() as probe:
                if probe.connect_ex(("127.0.0.1", PORTS[module])) == 0:
                    raise RuntimeError(f"{module} 验收端口已被占用，请先停止旧验收进程")
        process = subprocess.Popen([java, "-jar", str(jar)], env=environment, cwd=ROOT,
                                   stdout=log, stderr=subprocess.STDOUT)
        self.processes.append(process)
        if init:
            if process.wait(timeout=90) != 0:
                raise RuntimeError("管理员初始化失败，见忽略目录中的 admin-init.log")
        else:
            self.wait_ready(process, module)
        return process

    def wait_ready(self, process: subprocess.Popen[Any], module: str) -> None:
        endpoint = "/actuator/health/readiness" if module != "admin" else "/actuator/health"
        for _ in range(90):
            if process.poll() is not None:
                raise RuntimeError(f"{module} 提前退出，请检查对应日志")
            try:
                if requests.get(f"http://127.0.0.1:{PORTS[module]}{endpoint}", timeout=2).status_code == 200:
                    self.record(f"{module} 已启动并完成迁移/探针验证")
                    return
            except requests.RequestException:
                pass
            time.sleep(1)
        raise RuntimeError(f"{module} 等待启动超时")

    def call(self, method: str, path: str, body: Any = None, token: str | None = None,
             key: str | None = None, expected: int = 200) -> Any:
        headers = {"X-Request-ID": "wp02-" + uuid.uuid4().hex}
        if token:
            headers["Authorization"] = "Bearer " + token
        if key:
            headers["Idempotency-Key"] = key
        response = requests.request(method, "http://127.0.0.1:18080" + path, json=body, headers=headers, timeout=15)
        if response.status_code != expected:
            raise AssertionError(f"{method} {path}: HTTP {response.status_code}, 预期 {expected}, code={response.json().get('code')}")
        if response.headers.get("X-Request-ID") != headers["X-Request-ID"]:
            raise AssertionError("请求关联标识未正确透传")
        return response.json().get("data")

    def register_fixture(self) -> tuple[str, str]:
        email = f"wp02-{uuid.uuid4().hex}@example.invalid"
        challenge_id = str(uuid.uuid4())
        code = "123456"
        digest = hmac.new(self.private["hmac"].encode(), f"{challenge_id}:{code}".encode(), hashlib.sha256).hexdigest()
        now = datetime.now(timezone.utc).replace(tzinfo=None)
        with self.connection("biz") as connection:
            with connection.cursor() as cursor:
                for kind in ("TERMS", "PRIVACY"):
                    cursor.execute("INSERT IGNORE INTO legal_document VALUES(%s,'verification','仅供验收','验收夹具，不是正式协议','fixture','PUBLISHED',%s)", (kind, now - timedelta(days=1)))
                cursor.execute("INSERT INTO auth_challenge(id,purpose,target_canonical,code_hmac,expires_at,attempts,max_attempts,send_state,version,created_at) "
                               "VALUES(%s,'REGISTER',%s,%s,%s,0,5,'SENT',1,%s)", (challenge_id, email, digest, now + timedelta(minutes=10), now))
        result = self.call("POST", "/api/v1/auth/registrations", {
            "email": email, "challengeId": challenge_id, "code": code, "password": self.private["password"],
            "confirmedPassword": self.private["password"], "legalVersions": {"TERMS": "verification", "PRIVACY": "verification"},
            "device": {"deviceId": "verification-device", "deviceName": "Android 验收"},
        }, expected=201)
        return result["user"]["id"], result["session"]["token"]

    def verify_http(self) -> None:
        user_id, token = self.register_fixture()
        _, stranger = self.register_fixture()
        key = "pet-" + uuid.uuid4().hex
        body = {"nickname": "雪球", "species": "CAT"}
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            futures = [executor.submit(self.call, "POST", "/api/v1/pets", body, token, key, 201) for _ in range(2)]
            pets = [future.result() for future in futures]
        assert pets[0]["id"] == pets[1]["id"]
        pet_id = pets[0]["id"]
        self.call("GET", f"/api/v1/pets/{pet_id}", token=stranger, expected=404)
        assert self.call("GET", "/api/v1/bootstrap", token=token)["currentPetId"] == pet_id
        self.record("真实 Gateway/MySQL/Redis：并发幂等建宠、bootstrap、跨用户拒绝")
        login = self.call("POST", "/api/admin/v1/auth/sessions", {"username": "verification-admin", "password": self.private["password"]})
        admin = login["token"]
        request = self.call("POST", "/api/v1/quota-grant-requests", {"bucket": "STORAGE_BYTES", "amount": "1000", "reason": "存储验收"}, token, "request-" + uuid.uuid4().hex, 201)
        decision = {"approved": True, "amount": "1000", "reason": "批准验收", "version": request["version"]}
        key = "approve-" + uuid.uuid4().hex
        path = f"/api/admin/v1/quota-grant-requests/{request['id']}:decide"
        self.call("POST", path, decision, admin, key)
        self.call("POST", path, decision, admin, key)
        assert self.call("GET", "/api/v1/usage", token=token)[0]["granted"] == "1000"
        assert self.call("GET", "/api/admin/v1/audit-logs", token=admin)["items"]
        self.record("真实 Admin 独立认证→签名 Dubbo→Biz：额度审批重试只到账一次、双方审计")
        with self.connection("admin") as connection:
            with connection.cursor() as cursor:
                try:
                    cursor.execute(f"SELECT COUNT(*) FROM `{DATABASES['biz']}`.user_account")
                except pymysql.err.OperationalError as error:
                    if error.args[0] != 1142:
                        raise
                else:
                    raise AssertionError("Admin 数据库账号不应能读取 Biz 表")
        self.call("GET", "/api/admin/v1/users", token=admin)
        breed_id = "VERIFY_" + uuid.uuid4().hex.upper()
        breed = {"id": breed_id, "species": "CAT", "name": breed_id, "status": "ACTIVE", "reason": "字典验收"}
        self.call("POST", "/api/admin/v1/pet-breeds", breed, admin, "breed-" + uuid.uuid4().hex, 201)
        self.call("GET", "/api/admin/v1/dictionaries/pet-breeds?species=CAT", token=admin)
        self.record("真实 RPC 字典与用户查询通过；Admin 数据库账号读取 Biz 表被 MySQL 拒绝")
        self.results["verificationUserId"] = user_id

    def record(self, check: str) -> None:
        self.results["checks"].append(check)
        LOG.info(check)

    def close(self) -> None:
        for process in reversed(self.processes):
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
        for log in self.logs:
            log.close()
        result = json.dumps(self.results, ensure_ascii=False, indent=2)
        (OUTPUT / "result.json").write_text(result, encoding="utf-8")
        (self.run_output / "result.json").write_text(result, encoding="utf-8")

    def run(self) -> None:
        try:
            self.prepare_databases()
            self.launch("biz")
            with self.connection("admin") as connection:
                with connection.cursor() as cursor:
                    cursor.execute("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=%s AND table_name='admin_account'", (DATABASES["admin"],))
                    initialized = cursor.fetchone()[0] > 0
                    if initialized:
                        cursor.execute("SELECT COUNT(*) FROM admin_account")
                        initialized = cursor.fetchone()[0] > 0
            if not initialized:
                self.launch("admin", init=True)
            self.launch("admin")
            self.launch("gateway")
            self.verify_http()
            self.results["passed"] = True
        finally:
            self.close()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
    Verification().run()
