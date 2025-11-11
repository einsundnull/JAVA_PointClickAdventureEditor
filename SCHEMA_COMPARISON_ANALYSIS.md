# Schema Comparison Analysis
**Datum:** 2025-01-06
**Zweck:** Vergleich zwischen Schema-Definitionen und tatsächlicher Java-Implementierung

---

## 📋 ITEM FILE STRUCTURE

### ✅ Laut Schema (Schema File Structure Item Simple.txt):
```
#Name:
#ImagePath:
#Position: (x, y, z)
#Size: (width, height)
#IsInInventory:
#MouseHover:
#CustomClickArea: (points with x, y, z)
#MovingRange: (points with x, y, z)
#Path: (points with x, y, z)
#Actions:
```

### ⚠️ Tatsächliche Implementierung (ItemSaver.java):
```
#Name:                  ✓ Korrekt
#ImageFile:             ✗ NICHT im Schema (überflüssig)
#ImagePath:             ✓ Korrekt
#Position:              ⚠️ Speichert nur (x, y) - FEHLT z!
#Size:                  ✓ Korrekt
#Conditions:            ✗ NICHT im Schema! Sollte NICHT gespeichert werden!
#IsInInventory:         ✓ Korrekt
#MouseHover:            ✓ Korrekt
#ImageConditions:       ✗ NICHT im Schema (legacy?)
#ConditionalImages:     ✗ NICHT im Schema (neu?)
#ClickArea:             ✓ Korrekt
#Actions:               ✓ Korrekt
#CustomClickAreas:      ✓ Korrekt
#MovingRanges:          ✓ Korrekt
#Path:                  ✗ FEHLT KOMPLETT!
```

---

## 🔴 KRITISCHE PROBLEME

### Problem 1: Conditions werden in Item Files gespeichert
**Datei:** `ItemSaver.java` (Zeilen 42-52)
```java
// Conditions
writer.write("#Conditions:\n");
Map<String, Boolean> conditions = item.getConditions();
```
**Problem:** Laut Schema gehören Conditions NICHT in Item Files!
**Lösung:** Diese Sektion komplett entfernen

---

### Problem 2: Path wird NICHT gespeichert
**Datei:** `ItemSaver.java` (fehlt komplett)
**Problem:** Laut Schema sollte `#Path:` mit Points gespeichert werden
**Beweis:** Item.java hat bereits `private List<Path> paths;` (Zeile 32)
**Lösung:** Path-Sektion hinzufügen analog zu MovingRanges

---

### Problem 3: Z-Koordinate fehlt bei Position
**Datei:** `ItemSaver.java` (Zeilen 32-35)
```java
writer.write("#Position:\n");
writer.write("-x = " + item.getPosition().x + ";\n");
writer.write("-y = " + item.getPosition().y + ";\n\n");
// Z FEHLT!
```
**Problem:** Schema definiert Position mit (x, y, z)
**Lösung:** z-Koordinate hinzufügen

---

### Problem 4: Z-Koordinate fehlt bei CustomClickArea Points
**Datei:** `ItemSaver.java` (Zeile 245)
```java
writer.write("---point" + (i + 1) + ": x=" + p.x + ", y=" + p.y + ", z=0\n");
```
**Problem:** z wird hardcoded als 0 geschrieben statt aus Point zu lesen
**Lösung:** java.awt.Point auf Point3D erweitern oder z aus Item-Level lesen

---

### Problem 5: Path Editor fehlt im UI
**Datei:** `EditorMainSimple.java` (Zeile 2674-2677)
```java
private void editPathPoints() {
    // Items don't have separate Path objects, only MovingRanges
    log("Items don't have Path objects. Use MovingRange instead.");
}
```
**Problem:** Kommentar ist FALSCH! Items haben `List<Path> paths`
**Lösung:** Path Editor implementieren analog zu MovingRange

---

## 📊 NICHT MEHR BENÖTIGTE CODE-TEILE

### 1. ImageFile Speicherung (überflüssig)
**Datei:** `ItemSaver.java` (Zeilen 24-26)
```java
writer.write("#ImageFile:\n");
writer.write("-" + item.getImageFileName() + "\n\n");
```
**Grund:** Nicht im Schema, redundant zu ImagePath
**Aktion:** Entfernen

---

### 2. Conditions Speicherung (falsch!)
**Datei:** `ItemSaver.java` (Zeilen 42-52)
```java
writer.write("#Conditions:\n");
Map<String, Boolean> conditions = item.getConditions();
// ... 10 Zeilen Code
```
**Grund:** Conditions gehören NICHT in Item Files!
**Aktion:** Komplett entfernen

---

### 3. ImageConditions Sektion (legacy?)
**Datei:** `ItemSaver.java` (Zeilen 85-106)
```java
writer.write("#ImageConditions:\n");
writer.write("--conditions:\n");
Map<String, String> imageConditions = item.getImageConditions();
// ... 20 Zeilen Code
```
**Grund:** Nicht im Schema definiert
**Aktion:** Prüfen ob noch verwendet → wenn nicht: Entfernen

---

### 4. ConditionalImages Sektion (nicht im Schema)
**Datei:** `ItemSaver.java` (Zeilen 108-127)
```java
java.util.List<ConditionalImage> conditionalImages = item.getConditionalImages();
if (conditionalImages != null && !conditionalImages.isEmpty()) {
    writer.write("#ConditionalImages:\n");
    // ... 15 Zeilen Code
}
```
**Grund:** Nicht im Schema definiert
**Aktion:** Prüfen ob noch verwendet → wenn nicht: Entfernen

---

## 🆕 FEHLENDE FUNKTIONALITÄT

### 1. Path Speicherung fehlt
**Wo:** `ItemSaver.java` (fehlt nach Zeile 283)
**Was fehlt:**
```java
// Paths - FEHLT KOMPLETT!
java.util.List<Path> paths = item.getPaths();
if (paths != null && !paths.isEmpty()) {
    writer.write("\n#Paths:\n");
    for (Path path : paths) {
        writer.write("-Path:\n");

        // Write points
        writer.write("--Points:\n");
        java.util.List<java.awt.Point> points = path.getPoints();
        for (int i = 0; i < points.size(); i++) {
            java.awt.Point p = points.get(i);
            writer.write("---point" + (i + 1) + ": x=" + p.x + ", y=" + p.y + ", z=1\n");
        }

        // Write conditions
        Map<String, Boolean> pathConditions = path.getConditions();
        if (pathConditions != null && !pathConditions.isEmpty()) {
            writer.write("--Conditions:\n");
            for (Map.Entry<String, Boolean> entry : pathConditions.entrySet()) {
                writer.write("---" + entry.getKey() + " = " + entry.getValue() + "\n");
            }
        }
    }
}
```

---

### 2. Path Laden fehlt
**Wo:** `ItemLoader.java` (Zeile 100-102)
**Problem:** Section header "#Paths:" wird erkannt, aber nicht verarbeitet!
```java
} else if (line.startsWith("#Paths:")) {
    currentSection = "PATHS";
    System.out.println("ItemLoader: Found #Paths: section");
}
// DANACH FEHLT DIE IMPLEMENTIERUNG!
```

---

### 3. Path Editor UI fehlt
**Wo:** `EditorMainSimple.java` (fehlt komplett)
**Was fehlt:**
- Path Points ListView Segment (analog zu CustomClickArea/MovingRange)
- [Add] [Edit] [Remove] Buttons für Paths
- Path Editor Dialog (analog zu PointEditorDialog)
- Path Visualization in Game Canvas

---

### 4. Z-Koordinate in Point3D
**Problem:** java.awt.Point hat nur x, y
**Lösung:** Entweder:
- Neue Klasse `Point3D` erstellen
- Oder z auf Item-Level speichern (z=1 für alle Points)
- Oder z in Custom-Wrapper-Klasse

---

## 📝 METHODEN DIE ÜBERARBEITET WERDEN MÜSSEN

### ItemSaver.java
| Zeile | Methode | Problem | Aktion |
|-------|---------|---------|--------|
| 14-287 | `saveItem()` | Speichert Conditions, fehlt Path, fehlt z | Refactoring |
| 24-26 | ImageFile schreiben | Nicht im Schema | Entfernen |
| 42-52 | Conditions schreiben | FALSCH! Nicht im Schema | Entfernen |
| 32-35 | Position schreiben | z fehlt | z hinzufügen |
| 85-106 | ImageConditions schreiben | Nicht im Schema | Prüfen/Entfernen |
| 108-127 | ConditionalImages schreiben | Nicht im Schema | Prüfen/Entfernen |
| nach 283 | **Path schreiben** | **FEHLT!** | **Hinzufügen** |

### ItemLoader.java
| Zeile | Methode | Problem | Aktion |
|-------|---------|---------|--------|
| 15-450 | `loadItem()` | Lädt Conditions (falsch), lädt kein Path | Refactoring |
| 72-73 | CONDITIONS Section | Sollte nicht geladen werden | Entfernen |
| 100-102 | PATHS Section Header | Erkannt aber nicht implementiert! | Implementieren |
| 68-70 | Position laden | z fehlt | z hinzufügen |

### EditorMainSimple.java
| Zeile | Methode | Problem | Aktion |
|-------|---------|---------|--------|
| 2674-2677 | `editPathPoints()` | "Items don't have Path objects" ist FALSCH! | Implementieren |
| fehlt | `createPathPointsSegment()` | UI Segment für Paths fehlt | Neu erstellen |
| fehlt | Path ListView | Analog zu CustomClickArea | Neu erstellen |

---

## 🎯 ZUSAMMENFASSUNG

### ❌ ZU ENTFERNEN:
1. **ItemSaver.java**: `#ImageFile` Sektion (Zeilen 24-26)
2. **ItemSaver.java**: `#Conditions` Sektion (Zeilen 42-52) - **KRITISCH!**
3. **ItemSaver.java**: `#ImageConditions` Sektion (Zeilen 85-106) - falls nicht mehr verwendet
4. **ItemSaver.java**: `#ConditionalImages` Sektion (Zeilen 108-127) - falls nicht mehr verwendet
5. **ItemLoader.java**: CONDITIONS Section Handler (Zeilen 72-73)

### ✅ HINZUZUFÜGEN:
1. **ItemSaver.java**: `#Path` Sektion speichern (nach Zeile 283)
2. **ItemSaver.java**: z-Koordinate bei Position (Zeile 35)
3. **ItemLoader.java**: Path Parsing implementieren (nach Zeile 102)
4. **ItemLoader.java**: z-Koordinate bei Position laden
5. **EditorMainSimple.java**: Path Points Segment im UI
6. **EditorMainSimple.java**: Path Editor Dialog
7. **EditorMainSimple.java**: Path Buttons [Add] [Edit] [Remove]
8. **AdventureGame.java**: Path Visualization/Rendering

### ⚠️ ZU PRÜFEN:
1. Wird `imageConditions` noch verwendet?
2. Wird `conditionalImages` noch verwendet?
3. Wie soll z-Koordinate gespeichert werden? (Point3D?)
4. Sollen alte Item Files migriert werden?

---

## 📌 NÄCHSTE SCHRITTE

1. ✅ Analyse abgeschlossen
2. ⏳ Bestätigung vom Benutzer einholen
3. ⏳ Überflüssige Code-Teile entfernen
4. ⏳ Fehlende Path-Funktionalität hinzufügen
5. ⏳ z-Koordinate Unterstützung implementieren
6. ⏳ Path Editor UI erstellen
7. ⏳ Alte Item Files migrieren (optional)
8. ⏳ Tests durchführen

---

**Ende der Analyse**
