from pathlib import Path
import re
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
    expected = {"PACKAGE_USAGE_STATS", "POST_NOTIFICATIONS", "FOREGROUND_SERVICE", "FOREGROUND_SERVICE_SPECIAL_USE"}
    assert permissions == {"android.permission." + item for item in expected}, permissions
    application = root.find("application")
    assert application.get(ANDROID + "allowBackup") == "false"
    assert application.get(ANDROID + "name") == ".ProjectADHDApplication"
    service = application.find("service")
    assert service.get(ANDROID + "exported") == "false"
    assert service.get(ANDROID + "foregroundServiceType") == "specialUse"
    assert service.find("property").get(ANDROID + "name") == "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
    assert root.find("queries/intent/category").get(ANDROID + "name") == "android.intent.category.LAUNCHER"
    print("PASS: permissions, foreground service, package visibility and disabled backup")


def verify_sql():
    entities = (SOURCE / "data/local/entity/Entities.kt").read_text()
    connection = sqlite3.connect(":memory:")
    connection.execute("PRAGMA foreign_keys=ON")
    mapping = {"String": "TEXT", "Long": "INTEGER", "Int": "INTEGER", "Boolean": "INTEGER"}
    for match in re.finditer(r"@Entity\b(.*?)data class (\w+)\((.*?)(?=\n@Entity|\Z)", entities, re.S):
        annotation, name, body = match.groups()
        table = re.search(r'tableName = "(\w+)"', annotation).group(1)
        fields = re.findall(r"val (\w+): (\w+)", body)
        columns = [column + " " + mapping[kind] + " NOT NULL" for column, kind in fields]
        primary = re.search(r"@PrimaryKey(?:\([^)]*\))?\s+val (\w+)", body)
        if primary:
            columns.append("PRIMARY KEY (" + primary.group(1) + ")")
        else:
            keys = re.search(r"primaryKeys = \[([^]]+)]", annotation).group(1)
            columns.append("PRIMARY KEY (" + keys.replace('"', "") + ")")
        if name == "HabitCompletionEntity":
            assert "onDelete = ForeignKey.CASCADE" in annotation
            columns.append("FOREIGN KEY (habitId) REFERENCES habits(id) ON DELETE CASCADE")
        connection.execute("CREATE TABLE " + table + " (" + ", ".join(columns) + ")")
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
        connection.execute("INSERT INTO intervention_events VALUES (?, 'app', 'App', 900000, 900000, ?, 2)", (identity, at))
    assert [row[0] for row in connection.execute(queries["InterventionDao.observeRecent"])] == [3, 2, 1]
    connection.execute(queries["InterventionDao.clear"])
    assert connection.execute("SELECT COUNT(*) FROM intervention_events").fetchone()[0] == 0
    connection.close()
    print("PASS:", len(queries), "DAO queries prepared; uniqueness, local-day counts, disabled habits, cascade and event order checked on SQLite")


if __name__ == "__main__":
    verify_resources()
    verify_structure()
    verify_manifest()
    verify_sql()
    print("Source checks passed. Kotlin compilation, JUnit, Android lint and device behavior are NOT checked by this script.")
