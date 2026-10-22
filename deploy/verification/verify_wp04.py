"""WP04-A 独立 MySQL 套件和真实 MinIO 验收；凭证只由环境传入。"""
import os
import secrets
import subprocess
import json
from pathlib import Path
import pymysql

ROOT = Path(__file__).resolve().parents[2]
DATABASE = "fafa_wp04_biz_suite"
USERNAME = "fafa_wp04_suite"

def main():
    password = secrets.token_urlsafe(32)
    host = os.environ["FAFA_VERIFY_DB_HOST"]
    with pymysql.connect(host=host,user="root",password=os.environ["FAFA_VERIFY_DB_ROOT_PASSWORD"],autocommit=True) as db:
        with db.cursor() as cursor:
            cursor.execute(f"CREATE DATABASE IF NOT EXISTS `{DATABASE}` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin")
            cursor.execute("CREATE USER IF NOT EXISTS %s@'%%' IDENTIFIED BY %s",(USERNAME,password))
            cursor.execute("ALTER USER %s@'%%' IDENTIFIED BY %s",(USERNAME,password))
            cursor.execute(f"GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX,REFERENCES ON `{DATABASE}`.* TO %s@'%%'",(USERNAME,))
    env = os.environ.copy()
    env.update(SPRING_DATASOURCE_URL=f"jdbc:mysql://{host}:3306/{DATABASE}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true",
               SPRING_DATASOURCE_USERNAME=USERNAME,SPRING_DATASOURCE_PASSWORD=password)
    output = ROOT/"fafa-java"/"target"/"wp04-verification"
    output.mkdir(parents=True,exist_ok=True)
    with (output/"mysql-minio.log").open("w",encoding="utf-8") as log:
        result = subprocess.run(["cmd","/c","mvnw.cmd","-q","-pl","fafa-biz","-am","test",
             "-Dtest=MediaHttpTest,MediaLiveTest,MinioStorageTest","-Dsurefire.failIfNoSpecifiedTests=false"],
             cwd=ROOT/"fafa-java",env=env,stdout=log,stderr=subprocess.STDOUT)
    (output/"result.json").write_text(json.dumps({"database":DATABASE,"exitCode":result.returncode,
        "minioEnabled":bool(env.get("FAFA_MEDIA_TEST_ENDPOINT")),"smtp":"未验证","gateway":"本脚本使用 MockMvc，不代表真实 Gateway 网络验收"},
        ensure_ascii=False,indent=2),encoding="utf-8")
    print("WP04 独立依赖验收退出码:",result.returncode)
    raise SystemExit(result.returncode)

if __name__ == "__main__":
    main()
