# Schema Refactoring - Zusammenfassung der Änderungen
**Datum:** 2025-01-06
**Status:** Phase 2 Abgeschlossen ✅
**Letzte Aktualisierung:** 2025-11-06

---

## 🎯 ZIEL
Anpassung der Java-Implementierung an die definierten Schemas:
- `Schema File Structure Item Simple.txt`
- `Schema File Structure Scene Simple.txt`

---

## ✅ DURCHGEFÜHRTE ÄNDERUNGEN

### 1. **ItemSaver.java** - Bereinigt und Schema-konform gemacht

#### ❌ ENTFERNT (nicht im Schema):
- **Zeilen 24-26**: `#ImageFile` Sektion
  - War redundant zu ImagePath

- **Zeilen 42-52**: `#Conditions` Sektion (KRITISCH!)
  - Conditions gehören NICHT in Item Files!
  - Wurden fälschlicherweise gespeichert

- **Zeilen 69-90**: `#ImageConditions` Sektion
  - Nicht im Schema definiert
  - Legacy-Code

- **Zeilen 92-111**: `#ConditionalImages` Sektion
  - Nicht im Schema definiert
  - Wird durch andere Mechanismen ersetzt

#### ✅ HINZUGEFÜGT (fehlte):
- **Zeile 32**: z-Koordinate bei Position
  ```java
  writer.write("-z = 1;\n\n");
  ```
  - Position wird jetzt als (x, y, z) gespeichert

- **Zeilen 72-79**: #CustomClickArea Format angepasst
  ```java
  writer.write("#CustomClickArea:\n");
  writer.write("-point1: x=..., y=..., z=1\n");
  ```
  - Neues Format statt altes `--x=...` Format

- **Zeile 186**: z=1 für CustomClickAreas Points
  - Statt z=0 jetzt z=1

- **Zeilen 226-238**: #Path Sektion hinzugefügt
  ```java
  writer.write("\n#Path:\n");
  for (Path path : paths) {
      java.util.List<java.awt.Point> points = path.getPoints();
      for (int i = 0; i < points.size(); i++) {
          java.awt.Point p = points.get(i);
          writer.write("---point" + (i + 1) + ": x=" + p.x + ", y=" + p.y + ", z=1\n");
      }
  }
  ```
  - Path wird jetzt korrekt gespeichert

---

### 2. **ItemLoader.java** - Parsing für neues Format

#### ❌ ENTFERNT:
- **Zeilen 73-74**: `#Conditions` Section Handler
  - Wird nicht mehr geladen

- **Zeilen 78-80**: `#ImageConditions` Section Handler

- **Zeilen 82-83**: `#ConditionalImages` Section Handler

#### ✅ HINZUGEFÜGT:
- **Zeilen 78-86**: `#CustomClickArea` Section Handler
  - Erkennt neues Format

- **Zeilen 87-89**: `#MovingRange` Section Handler
  - Für einzelne MovingRange (nicht Plural)

- **Zeilen 90-92**: `#Path` Section Handler
  - Für einzelne Path (nicht Plural)

- **Zeilen 106-142**: Universal Point Parser
  ```java
  // Parse new point format: -point1: x=590, y=429, z=1
  else if ((currentSection.equals("CUSTOMCLICKAREA") ||
            currentSection.equals("MOVINGRANGE") ||
            currentSection.equals("PATH")) &&
           line.startsWith("-point")) {
      // Parse format...
  }
  ```
  - Parst das neue Format für CustomClickArea, MovingRange und Path

---

### 3. **Item.java** - Getter/Setter für Paths

#### ✅ HINZUGEFÜGT:
- **Zeilen 524-535**: Path Getter/Setter
  ```java
  public List<Path> getPaths() { return paths; }
  public void setPaths(List<Path> paths) { this.paths = paths; }
  public void addPath(Path path) { this.paths.add(path); }
  ```

---

## 📊 STATISTIK

| Datei | Zeilen Gelöscht | Zeilen Hinzugefügt | Geänderte Funktionen |
|-------|----------------|-------------------|---------------------|
| ItemSaver.java | ~70 | ~20 | saveItem() |
| ItemLoader.java | ~10 | ~40 | loadItem() |
| Item.java | 0 | 15 | getPaths(), setPaths(), addPath() |
| **GESAMT** | **~80** | **~75** | **3** |

---

## 🎯 FILE FORMAT ÄNDERUNGEN

### ALT → NEU:

#### Position:
```diff
  #Position:
  -x = 587;
  -y = 307;
+ -z = 1;
```

#### CustomClickArea:
```diff
- #ClickArea:
- ###
- --x = 590;
- --y = 429;

+ #CustomClickArea:
+ -point1: x=590, y=429, z=1
+ -point2: x=559, y=206, z=1
```

#### Path (NEU!):
```diff
+ #Path:
+ ---point1: x=1533, y=0, z=1
+ ---point2: x=502, y=268, z=1
+ ---point3: x=502, y=354, z=1
```

#### Entfernt:
```diff
- #ImageFile:
- -white_cup.png

- #Conditions:
- -lookAt = true;
- -isInInventory = false;

- #ImageConditions:
- --conditions:
- ---none

- #ConditionalImages:
- -Image:
- --Name: ...
```

---

## ✅ PHASE 2 ABGESCHLOSSEN (2025-11-06)

### MovingRange und Path Points Implementierung

#### 1. MovingRange.java - Vollständig refaktoriert ✅
**Änderungen:**
- `private String keyAreaName;` → `private List<Point> points;`
- Neue Methoden: `getPoints()`, `setPoints()`, `addPoint()`, `removePoint()`
- Legacy-Methoden als `@Deprecated` markiert für Backward-Compatibility
- `copy()` Methode aktualisiert für Points

#### 2. ItemSaver.java - MovingRange Format aktualisiert ✅
**Neues Format:**
```java
writer.write("\n#MovingRange:\n");
for (int i = 0; i < points.size(); i++) {
    Point p = points.get(i);
    writer.write("---point" + (i + 1) + ": x=" + p.x + ", y=" + p.y + ", z=1\n");
}
```
- Schreibt jetzt Points statt KeyAreaName
- Format: `---point1: x=100, y=100, z=1` (3 Dashes!)

#### 3. ItemLoader.java - Finalisierungs-Logik hinzugefügt ✅
**Änderungen:**
- Finalisiert MovingRange/Path Objekte beim Erreichen neuer Sections (Zeilen 63-78)
- Finalisiert letzte Objekte am Ende der Datei (Zeilen 604-608)
- Universal Point Parser unterscheidet zwischen:
  - CustomClickArea: `-point` (1 Dash)
  - MovingRange/Path: `---point` (3 Dashes)
- Bedingungsparser excludiert `---point` Zeilen (Zeile 509)

#### 4. Tests ✅
**TestItemSaveLoad.java** erstellt und alle Tests bestanden:
- ✓ Item laden von cup.txt
- ✓ MovingRange mit 3 Points hinzufügen
- ✓ Path mit 4 Points hinzufügen
- ✓ Speichern zu Testdatei
- ✓ Laden von Testdatei
- ✓ Datenintegrität verifiziert (3 MovingRange Points, 4 Path Points)

**Ergebnis:** === ALL TESTS PASSED ✓ ===

---

## ⚠️ NOCH NICHT IMPLEMENTIERT

### 1. ~~MovingRange Points Support~~ ✅ ERLEDIGT (siehe Phase 2)

---

### 2. Path Editor UI
**Problem:** EditorMainSimple hat kein UI-Segment für Paths
**Fehlt:**
- Path Points ListView (wie CustomClickArea/MovingRange)
- [Add] [Edit] [Remove] Buttons für Paths
- Path Editor Dialog
- Path Visualization im Game Canvas

**Datei:** EditorMainSimple.java
**TODO:**
- Path Segment erstellen
- Path Editor implementieren
- editPathPoints() Methode korrigieren (aktuell: "Items don't have Path objects" ist FALSCH!)

---

### 3. CustomClickAreas (Plural) Parsing
**Problem:** `#CustomClickAreas:` (mit Hover-Areas) noch nicht vollständig getestet
**Status:** Parser existiert in ItemLoader.java, muss getestet werden

---

## 🧪 TESTS BENÖTIGT

### Manuelle Tests:
1. **Item speichern**: Neues Item erstellen → speichern → File prüfen
2. **Item laden**: Gespeichertes Item laden → Daten prüfen
3. **CustomClickArea**: Points speichern/laden
4. **Path**: Paths hinzufügen → speichern → laden
5. **Backwards Compatibility**: Alte Item-Files laden (sollten funktionieren)

### Unit Tests (TODO):
- ItemSaverTest.java - Test für korrektes Format
- ItemLoaderTest.java - Test für Parsing
- IntegrationTest - Speichern + Laden roundtrip

---

## 📝 BREAKING CHANGES

### ⚠️ Achtung: Inkompatibilität mit alten Files!

**Entfernte Sections:**
- Alte Item-Files mit `#ImageFile`, `#Conditions`, `#ImageConditions`, `#ConditionalImages` werden diese Sections nicht mehr speichern
- Beim Laden werden diese Sections ignoriert

**Format-Änderungen:**
- `#ClickArea` → `#CustomClickArea` mit neuem Format
- Alte Files mit `--x=...` Format werden noch unterstützt (backward compatible)
- Neue Files verwenden `-point1: x=..., y=..., z=1` Format

**Migration:**
- Alte Item-Files können geladen werden (backward compatible)
- Beim Speichern werden sie ins neue Format konvertiert
- KEIN automatischer Migrationsprozess nötig

---

## ✅ KOMPILIERUNG

```bash
javac -encoding UTF-8 -d target/classes -cp target/classes src/main/java/main/*.java
```
**Ergebnis:** ✅ Erfolgreich (0 Errors, 0 Warnings)

---

## 🚀 NÄCHSTE SCHRITTE

### ~~Phase 2: MovingRange Refactoring~~ ✅ ABGESCHLOSSEN
1. ✅ MovingRange auf Points umstellen
2. ✅ ItemSaver MovingRange Format anpassen
3. ✅ ItemLoader MovingRange Parsing implementieren
4. ✅ Path Parsing implementieren
5. ✅ Tests erstellt und bestanden

### Phase 3: Path UI Implementation (AKTUELL)
1. Path Editor Dialog erstellen
2. Path Segment in EditorMainSimple
3. Path Visualization im Game Canvas
4. Path Buttons [Add] [Edit] [Remove]

### Phase 4: Testing & Documentation
1. Manuelle Tests durchführen
2. Unit Tests schreiben
3. Migrations-Guide für alte Files
4. API-Dokumentation aktualisieren

---

**Ende der Zusammenfassung**
