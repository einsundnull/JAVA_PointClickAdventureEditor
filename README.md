# Point-and-Click Adventure Framework

Ein Java-Framework für Point-and-Click Adventure-Spiele mit flexiblen Bewegungsbereichen und z-Achsen-Skalierung.

## Überblick

Dieses Framework bietet eine vollständige Struktur für Point-and-Click Adventures mit:
- Szenenbasierter Architektur
- Flexiblen Bewegungsbereichen (geschlossene Polygone oder lineare Pfade)
- Z-Achsen-Skalierung für perspektivische Tiefe
- Einfaches Laden von Bewegungsbereichen aus Textdateien
- Interaktive Elemente mit Maus-Events

## Klassenstruktur

### Point
Repräsentiert einen Punkt im 2D-Raum mit z-Achsen-Skalierungsinformationen.
- `x, y`: Position als Prozentsatz der Bildschirmabmessungen (0.0 bis 1.0)
- `z1, z2`: Min/Max Sprite-Größen-Multiplikatoren an diesem Punkt

### MovingRange (abstrakt)
Basisklasse für Bewegungsbereiche.

#### MovingRange_0
Geschlossenes Polygon (`typeOfRange = 0`)
- Erster und letzter Punkt werden automatisch verbunden
- Elemente können sich überall innerhalb des Polygons bewegen
- Beispiel: Begehbarer Bereich in einem Raum

#### MovingRange_1
Linearer Pfad (`typeOfRange = 1`)
- Erster und letzter Punkt werden NICHT verbunden
- Elemente bewegen sich entlang des Pfades
- Beispiel: Patrouillenpfad für NPCs

### Element
Interaktive Elemente in einer Szene.

**Eigenschaften:**
- `imagePath`: Pfad zum Bild des Elements
- `exitPoint`: Zielszene bei Erreichen (für Übergänge)
- `typeOfRange`: 0 für MovingRange_0, 1 für MovingRange_1
- `movingRange`: Bewegungsbereich des Elements
- `isMouseTransparent`: Wenn true, keine Maus-Reaktionen
- `positionX, positionY`: Position als Prozentsatz (0.0 bis 1.0)
- `size`: Basisgröße des Elements (1.0 = 100%)

**Methoden:**
- `canMoveTo(x, y)`: Prüft, ob Bewegung zu Position möglich ist
- `moveTo(x, y)`: Bewegt Element zu Position (wenn erlaubt)
- `getEffectiveSize()`: Berechnet Größe basierend auf z-Achsen-Skalierung
- `isExitPoint()`: Prüft, ob Element ein Ausgangspunkt ist

### Scene (abstrakt)
Repräsentiert eine Szene im Adventure.

**Eigenschaften:**
- `backgroundImagePath`: Pfad zum Hintergrundbild
- `elements`: Liste der Elemente in der Szene
- `sceneName`: Name der Szene

**Überschreibbare Methoden:**
- `onEnter()`: Wird beim Betreten der Szene aufgerufen
- `onExit()`: Wird beim Verlassen der Szene aufgerufen
- `update(deltaTime)`: Wird jeden Frame aufgerufen
- `onElementClicked(element)`: Wird beim Klicken auf ein Element aufgerufen
- `onElementHovered(element)`: Wird beim Hovern über ein Element aufgerufen

### PointFileReader
Utility-Klasse zum Laden von Punkten aus .txt Dateien.

**Methoden:**
- `readPointsFromFile(filePath)`: Liest Punkte aus Datei
- `loadMovingRange_0(filePath)`: Erstellt MovingRange_0 aus Datei
- `loadMovingRange_1(filePath)`: Erstellt MovingRange_1 aus Datei

## Dateiformat für Punkte

Punkte werden in .txt Dateien im folgenden Format gespeichert:

```
# Kommentare beginnen mit # oder //

punkt: x=0.05; y=0.50; z1=1.1; z2=0.5;
punkt: x=0.15; y=0.50; z1=1.1; z2=0.5;
punkt: x=0.34; y=0.50; z1=1.1; z2=0.5;
```

- Alle Koordinaten sind als Prozentsätze (0.0 bis 1.0)
- `x`: Horizontale Position (0.0 = links, 1.0 = rechts)
- `y`: Vertikale Position (0.0 = oben, 1.0 = unten)
- `z1, z2`: Skalierungsfaktoren für Sprite-Größe an diesem Punkt

## Verwendungsbeispiel

### 1. Eigene Szene erstellen

```java
public class MeineZimmerSzene extends Scene {
    public MeineZimmerSzene() {
        super("MeinZimmer", "resources/backgrounds/zimmer.png");
        initializeScene();
    }

    private void initializeScene() {
        // Spielercharakter erstellen
        Element player = new Element("resources/sprites/player.png", 0.5, 0.5);
        player.setTypeOfRange(0);

        try {
            MovingRange_0 begehbarerBereich =
                PointFileReader.loadMovingRange_0("resources/ranges/zimmer_bereich.txt");
            player.setMovingRange(begehbarerBereich);
        } catch (IOException e) {
            e.printStackTrace();
        }

        addElement(player);

        // Tür als Ausgang erstellen
        Element tuer = new Element("resources/sprites/tuer.png", 0.9, 0.5);
        tuer.setExitPoint("Flur"); // Nächste Szene
        addElement(tuer);
    }

    @Override
    protected void onElementClicked(Element element) {
        if (element.isExitPoint()) {
            // Szenenübergang implementieren
            System.out.println("Gehe zu: " + element.getExitPoint());
        }
    }
}
```

### 2. Bewegungsbereich definieren

Erstelle eine Datei `resources/ranges/zimmer_bereich.txt`:

```
# Begehbarer Bereich im Zimmer (geschlossenes Polygon)
punkt: x=0.10; y=0.40; z1=0.8; z2=0.8;
punkt: x=0.90; y=0.40; z1=0.8; z2=0.8;
punkt: x=0.90; y=0.90; z1=1.5; z2=1.5;
punkt: x=0.10; y=0.90; z1=1.5; z2=1.5;
```

### 3. NPC mit Patrouillenpfad

```java
Element npc = new Element("resources/sprites/npc.png", 0.2, 0.5);
npc.setTypeOfRange(1); // Linearer Pfad

try {
    MovingRange_1 patrouillenPfad =
        PointFileReader.loadMovingRange_1("resources/ranges/npc_pfad.txt");
    npc.setMovingRange(patrouillenPfad);
} catch (IOException e) {
    e.printStackTrace();
}

addElement(npc);
```

## Beispieldateien

Das Projekt enthält drei Beispieldateien in `resources/ranges/`:

1. **walkable_area.txt** - Einfacher trapezförmiger Bereich (MovingRange_0)
2. **npc_path.txt** - Linearer Patrouillenpfad (MovingRange_1)
3. **complex_area.txt** - Komplexes unregelmäßiges Polygon (MovingRange_0)

## Koordinatensystem

- **X-Achse**: 0.0 (links) bis 1.0 (rechts)
- **Y-Achse**: 0.0 (oben) bis 1.0 (unten)
- **Z-Achse**: Skalierungsfaktoren (typisch 0.5 bis 2.0)
  - Kleinere Werte = kleinere Sprites (weiter weg)
  - Größere Werte = größere Sprites (näher)

## Erweiterungsmöglichkeiten

Das Framework kann erweitert werden durch:
- Animations-System für Elemente
- Inventar-System
- Dialog-System
- Sound-Integration
- Pathfinding-Algorithmen für Bewegung entlang des MovingRange
- Save/Load-Funktionalität
- Szenenübergangs-Manager

## Lizenz

Dieses Projekt ist Open Source und frei verwendbar.
