from pathlib import Path
import re
import json
import hashlib
import sqlite3
import tomllib
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/kotlin/com/ansa1r/projectadhd"
ANDROID = "{http://schemas.android.com/apk/res/android}"

def active_kotlin_files():
    return [
        path
        for variant in ("main", "test", "androidTest")
        for path in (ROOT / "app/src" / variant / "kotlin").rglob("*.kt")
    ]


def verify_resources():
    resources = {}
    for path in (ROOT / "app/src/main/res").rglob("*"):
        if not path.is_file():
            continue
        kind = path.parent.name.split("-")[0]
        if kind == "values":
            for element in ET.parse(path).getroot():
                resources.setdefault(element.tag, set()).add(element.get("name"))
        else:
            resources.setdefault(kind, set()).add(path.stem)
    xml_files = list((ROOT / "app/src/main/res").rglob("*.xml"))
    xml_files.append(ROOT / "app/src/main/AndroidManifest.xml")
    for path in xml_files:
        ET.parse(path)
        for kind, name in re.findall(r"@(\w+)/([\w.]+)", path.read_text()):
            assert name in resources.get(kind, set()), (path, kind, name)
    for path in active_kotlin_files():
        for kind, name in re.findall(r"\bR\.(\w+)\.(\w+)", path.read_text()):
            assert name in resources.get(kind, set()), (path, kind, name)
    print("PASS: XML and resource references")


def verify_structure():
    catalog = tomllib.loads((ROOT / "gradle/libs.versions.toml").read_text())
    for section in ("libraries", "plugins"):
        for item in catalog[section].values():
            ref = item.get("version", {}).get("ref") if isinstance(item.get("version"), dict) else None
            if ref:
                assert ref in catalog["versions"], ref
    for path in [ROOT / "build.gradle.kts", ROOT / "settings.gradle.kts", ROOT / "app/build.gradle.kts"]:
        for ref in re.findall(r"\blibs\.([a-zA-Z0-9.]+)", path.read_text()):
            section, key = ("plugins", ref[8:]) if ref.startswith("plugins.") else ("libraries", ref)
            assert key.replace(".", "-") in catalog[section], (path, ref)
    old_package = ".".join(("com", "example", "projectadhd"))
    old_name = "Project" + "AHDH"
    checked_files = active_kotlin_files() + list(ROOT.glob("*"))
    checked_files += list((ROOT / "docs").rglob("*.md"))
    checked_files += list((ROOT / "gradle").glob("*.toml"))
    checked_files += list((ROOT / "app/src/main/res").rglob("*.xml"))
    checked_files += [ROOT / "app/build.gradle.kts", ROOT / "app/src/main/AndroidManifest.xml"]
    for path in checked_files:
        if path.is_file() and (path.suffix in (".kt", ".kts", ".xml", ".md", ".toml") or path.name == "LICENSE"):
            assert old_package not in path.read_text() and old_name not in path.read_text(), path
    for path in active_kotlin_files():
        package = re.search(r"^package ([\w.]+)", path.read_text()).group(1)
        assert "/" + package.replace(".", "/") + "/" in path.as_posix(), path
    build = (ROOT / "app/build.gradle.kts").read_text()
    assert 'namespace = "com.ansa1r.projectadhd"' in build
    assert 'applicationId = "com.ansa1r.projectadhd"' in build
    for variant in ("main", "test", "androidTest"):
        assert 'kotlin.directories.add("src/' + variant + '/kotlin")' in build
    assert build.count("kotlin.directories.clear()") == 3
    assert build.count("java.directories.clear()") == 3
    with zipfile.ZipFile(ROOT / "gradle/wrapper/gradle-wrapper.jar") as jar:
        assert jar.testzip() is None
        assert "org/gradle/wrapper/GradleWrapperMain.class" in jar.namelist()
    assert "!gradle/wrapper/gradle-wrapper.jar" in (ROOT / ".gitignore").read_text()
    print("PASS: catalog aliases, package roots, old names, complete Gradle wrapper")


def verify_manifest():
    root = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
    permissions = {entry.get(ANDROID + "name") for entry in root.findall("uses-permission")}
    expected = {"PACKAGE_USAGE_STATS", "POST_NOTIFICATIONS", "FOREGROUND_SERVICE", "FOREGROUND_SERVICE_SPECIAL_USE", "SYSTEM_ALERT_WINDOW"}
    assert permissions == {"android.permission." + item for item in expected}, permissions
    application = root.find("application")
    assert application.get(ANDROID + "allowBackup") == "false"
    assert application.get(ANDROID + "name") == ".ProjectADHDApplication"
    service = application.find("service")
    assert service.get(ANDROID + "exported") == "false"
    assert service.get(ANDROID + "foregroundServiceType") == "specialUse"
    assert service.find("property").get(ANDROID + "name") == "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
    assert any(e.get(ANDROID + "name") == "android.intent.category.LAUNCHER" for e in root.findall("queries/intent/category"))
    assert len(application.findall("service")) == 1
    assert len(application.findall("activity")) == 1
    print("PASS: permissions, foreground service, package visibility and disabled backup")


def verify_sql():
    schema_root = ROOT / "app/schemas/com.ansa1r.projectadhd.data.local.AppDatabase"
    v1 = json.loads((schema_root / "1.json").read_text())["database"]
    v2 = json.loads((schema_root / "2.json").read_text())["database"]
    connection = sqlite3.connect(":memory:")
    connection.execute("PRAGMA foreign_keys=ON")
    def create(db, schema):
        for entity in schema["entities"]:
            db.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
            for index in entity.get("indices", []):
                db.execute(index["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
    create(connection, v1)
    connection.execute("INSERT INTO habits VALUES (10, 'Stage1 habit', 10, 1)")
    connection.execute("INSERT INTO habit_completions VALUES (10, '2026-09-30', 20)")
    connection.execute("INSERT INTO tracked_apps VALUES ('old.app', 'Old', 7, 1)")
    connection.execute("INSERT INTO intervention_events VALUES (10, 'old.app', 'Old', 60000, 60000, 30, 2)")
    migrations = (SOURCE / "data/local/Migrations.kt").read_text()
    statements = re.findall(r'db\.execSQL\((?:"""(.*?)"""|"([^"\n]*)")\)', migrations, re.S)
    assert len(statements) == 3
    for triple, single in statements:
        connection.execute(triple or single)
    assert connection.execute("SELECT title FROM habits WHERE id=10").fetchone() == ("Stage1 habit",)
    assert connection.execute("SELECT completedAt FROM habit_completions WHERE habitId=10").fetchone() == (20,)
    assert connection.execute("SELECT sessionLimitMinutes FROM tracked_apps WHERE packageName='old.app'").fetchone() == (7,)
    assert connection.execute("SELECT type, detail FROM intervention_events WHERE id=10").fetchone() == ("LEGACY_NOTIFICATION", "")
    expected = sqlite3.connect(":memory:")
    create(expected, v2)
    for entity in v2["entities"]:
        table = entity["tableName"]
        for pragma in ("table_info", "foreign_key_list", "index_list"):
            query = "PRAGMA " + pragma + "('" + table + "')"
            assert connection.execute(query).fetchall() == expected.execute(query).fetchall(), (table, pragma)
    expected.close()
    connection.execute("DELETE FROM habits")
    connection.execute("DELETE FROM tracked_apps")
    connection.execute("DELETE FROM intervention_events")
    print("PASS: actual MIGRATION_1_2 SQL preserves all four Stage 1 tables and matches schema 2 columns, defaults, keys and indices")
    queries = {}
    for path in (SOURCE / "data/local/dao").glob("*.kt"):
        for match in re.finditer(r'@Query\((?:"""(.*?)"""|"([^"]*)")\)\s*(?:suspend\s+)?fun\s+(\w+)', path.read_text(), re.S):
            triple, single, method = match.groups()
            sql = triple if triple is not None else single
            params = {key: 1 for key in re.findall(r":(\w+)", sql)}
            connection.execute("EXPLAIN " + sql, params).fetchall()
            queries[path.stem + "." + method] = sql
    habit_dao = (SOURCE / "data/local/dao/HabitDao.kt").read_text()
    assert "@Insert(onConflict = OnConflictStrategy.IGNORE)" in habit_dao
    connection.execute("INSERT INTO habits VALUES (1, 'Read', 100, 1)")
    connection.execute("INSERT OR IGNORE INTO habit_completions VALUES (1, '2026-09-29', 200)")
    connection.execute("INSERT OR IGNORE INTO habit_completions VALUES (1, '2026-09-29', 300)")
    assert connection.execute("SELECT COUNT(*) FROM habit_completions").fetchone()[0] == 1
    today = connection.execute(queries["HabitDao.observeDay"], {"date": "2026-09-29"}).fetchone()
    assert today[-1] == 1
    assert connection.execute(queries["HabitDao.incompleteCount"], {"date": "2026-09-29"}).fetchone()[0] == 0
    assert connection.execute(queries["HabitDao.incompleteCount"], {"date": "2026-09-30"}).fetchone()[0] == 1
    connection.execute(queries["HabitDao.setActive"], {"id": 1, "active": 0})
    assert connection.execute(queries["HabitDao.incompleteCount"], {"date": "2026-09-30"}).fetchone()[0] == 0
    connection.execute(queries["HabitDao.delete"], {"id": 1})
    assert connection.execute("SELECT COUNT(*) FROM habit_completions").fetchone()[0] == 0
    for identity, at in ((1, 500), (2, 700), (3, 700)):
        connection.execute("INSERT INTO intervention_events (id, packageName, appName, sessionDurationMillis, limitMillis, occurredAt, incompleteHabitCount) VALUES (?, 'app', 'App', 900000, 900000, ?, 2)", (identity, at))
    assert [row[0] for row in connection.execute(queries["InterventionDao.observeRecent"])] == [3, 2, 1]
    connection.execute(queries["InterventionDao.clear"])
    assert connection.execute("SELECT COUNT(*) FROM intervention_events").fetchone()[0] == 0
    connection.execute("INSERT INTO block_sessions VALUES ('app', 'App', 100, '2026-09-30', 60000, 60000, 1, '2,3', 1, NULL, NULL)")
    try:
        connection.execute("INSERT INTO block_sessions SELECT * FROM block_sessions")
        raise AssertionError("Duplicate package block accepted")
    except sqlite3.IntegrityError:
        pass
    assert connection.execute(queries["BlockSessionDao.release"], {"now": 200, "reason": "COMPLETION", "packageName": "app"}).rowcount == 1
    assert connection.execute(queries["BlockSessionDao.release"], {"now": 300, "reason": "COMPLETION", "packageName": "app"}).rowcount == 0
    assert connection.execute(queries["BlockSessionDao.active"]).fetchall() == []
    connection.close()
    print("PASS:", len(queries), "DAO queries prepared; uniqueness, local-day counts, disabled habits, cascade and event order checked on SQLite")


def verify_assets():
    hashes = {
        "background_main.png": "06dfd88e3fe62d2f72b01c461916487d048bf436694699f6274f450056dd3abf",
        "mascot_idle.png": "95818b3a0ee211091a3e8d7fbd7c9bb6cd14b4f43c3599be49a0baa9b92fe9f2",
        "mascot_blocking.png": "0b52cfcade607f503f0485ba75b75268ea9a5330dc526dae36a61cba66757892",
    }
    for name, digest in hashes.items():
        data = (ROOT / "app/src/main/res/drawable-nodpi" / name).read_bytes()
        assert hashlib.sha256(data).hexdigest() == digest, name
    print("PASS: three supplied assets preserved byte-for-byte")


if __name__ == "__main__":
    verify_resources()
    verify_structure()
    verify_manifest()
    verify_sql()
    verify_assets()
    print("Source checks passed. Kotlin compilation, JUnit, Android lint and device behavior are NOT checked by this script.")
