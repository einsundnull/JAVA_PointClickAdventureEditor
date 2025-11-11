# Konzept Schema - Point & Click
## Standardisierte Anleitungen für alle Kategorien

---

## 🎬 SCENES

**[…]** → Collapses ListView.

**[Load]** → Loads the default values of the selected Scene from the `/resources/scenes/<nameOfScene>.txt` file to the Editors and to the Main Window and opens the Scene for editing.

**[New]** → Creates a new Scene file in the `/resources/scenes/` directory and adds it to the ListView. (Prompt: {inputField <n>})

**[Edit]** → Opens Scene Editor.

**[Copy]** → Copies selected Scene file `/scenes/<nameOfScene_copy>.txt` and adds it to the ListView.

**[Delete]** → Deletes the selected Scene file `<nameOfScene>.txt` in the `/resources/scenes/` directory and removes it from the ListView. (Alert: Yes/No). Before deleting, a check of all `/resources/<category>/<n>.txt` files must be run for references and references must be deleted if found. If references are found, a warning with a ListView of all files that hold a reference will be shown: "Found multiple references to this entry! Do you want to delete it anyway? [Yes] [No]"

---

## 📦 ITEMS

**[…]** → Collapses ListView.

**[New]** → Creates a new Item file `/items/<nameOfItem>.txt` and adds it to the ListView. At the same time it writes a reference to the `scenes/<nameOfScene>.txt` file. (Prompt: {inputField <n>})

**[Edit]** → Opens Item Editor.

**[Actions]** → Opens Item Action Editor.

**[Copy]** → Copies selected Item file `/items/<nameOfItem_copy>.txt` and adds it to the ListView. At the same time it writes a `<nameOfItem>` reference to the `scenes/<nameOfScene>.txt` file under the #Items tag.

**[Delete]** → Deletes the selected Item file `<nameOfItem>.txt` in the `/resources/items/` directory and removes it from the ListView. (Alert: Yes/No). Before deleting, a check of all `/resources/<category>/<n>.txt` files must be run for references and references must be deleted if found. If references are found, a warning with a ListView of all categories that hold a reference will be shown: "Found multiple references to this entry! Do you want to delete it anyway? [Yes] [No]"

---

## 👤 CHARACTERS

**[…]** → Collapses ListView.

**[New]** → Creates a new Character file `/characters/<nameOfCharacter>.txt` and adds it to the ListView. At the same time it writes a reference to the `scenes/<nameOfScene>.txt` file. (Prompt: {inputField <n>})

**[Edit]** → Opens Character Editor.

**[Actions]** → Opens Character Action Editor.

**[Copy]** → Copies selected Character file `/characters/<nameOfCharacter_copy>.txt` and adds it to the ListView. At the same time it writes a reference to the `scenes/<nameOfScene>.txt` file.

**[Delete]** → Deletes the selected Character file `<nameOfCharacter>.txt` in the `/resources/characters/` directory and removes it from the ListView. (Alert: Yes/No). Before deleting, a check of all `/resources/<category>/<n>.txt` files must be run for references and references must be deleted if found. If references are found, a warning with a ListView of all files that hold a reference will be shown: "Found multiple references to this entry! Do you want to delete it anyway? [Yes] [No]"

---

## 🎯 KEYAREAS

**[…]** → Collapses ListView.

**[New]** → Creates a new KeyArea file `/keyareas/<nameOfKeyArea>.txt` and adds it to the ListView. At the same time it writes a reference to the `scenes/<nameOfScene>.txt` file. (Prompt: {inputField <n>})

**[Edit]** → Opens KeyArea Editor.

**[Actions]** → Opens KeyArea Action Editor.

**[Copy]** → Copies selected KeyArea file `/keyareas/<nameOfKeyArea_copy>.txt` and adds it to the ListView. At the same time it writes a reference to the `scenes/<nameOfScene>.txt` file.

**[Delete]** → Deletes the selected KeyArea file `<nameOfKeyArea>.txt` in the `/resources/keyareas/` directory and removes it from the ListView. (Alert: Yes/No). Before deleting, a check of all `/resources/<category>/<n>.txt` files must be run for references and references must be deleted if found. If references are found, a warning with a ListView of all files that hold a reference will be shown: "Found multiple references to this entry! Do you want to delete it anyway? [Yes] [No]"

---

## 💬 DIALOGS

**[…]** → Collapses ListView.

**[New]** → Creates a new Dialog file `/dialogs/<nameOfDialog>.txt` and adds it to the ListView. At the same time it writes a reference to the `scenes/<nameOfScene>.txt` file. (Prompt: {inputField <n>})

**[Edit]** → Opens Dialog Editor.

**[Actions]** → Opens Dialog Action Editor.

**[Copy]** → Copies selected Dialog file `/dialogs/<nameOfDialog_copy>.txt` and adds it to the ListView. At the same time it writes a reference to the `scenes/<nameOfScene>.txt` file.

**[Delete]** → Deletes the selected Dialog file `<nameOfDialog>.txt` in the `/resources/dialogs/` directory and removes it from the ListView. (Alert: Yes/No). Before deleting, a check of all `/resources/<category>/<n>.txt` files must be run for references and references must be deleted if found. If references are found, a warning with a ListView of all files that hold a reference will be shown: "Found multiple references to this entry! Do you want to delete it anyway? [Yes] [No]"

---

## 📊 Vergleichstabelle

| Aktion     | Scenes | Items | Characters | KeyAreas | Dialogs |
|------------|--------|-------|------------|----------|---------|
| [...]      | ✓      | ✓     | ✓          | ✓        | ✓       |
| [Load]     | ✓      | —     | —          | —        | —       |
| [New]      | ✓      | ✓     | ✓          | ✓        | ✓       |
| [Edit]     | ✓      | ✓     | ✓          | ✓        | ✓       |
| [Actions]  | —      | ✓     | ✓          | ✓        | ✓       |
| [Copy]     | ✓      | ✓     | ✓          | ✓        | ✓       |
| [Delete]   | ✓      | ✓     | ✓          | ✓        | ✓       |

**Legende:** ✓ = Vorhanden | — = Nicht vorhanden

---

## ✅ Zusammenfassung der Standardisierung

### Angewendete Verbesserungen:

1. **Einheitliche Formulierung** - Alle Beschreibungen folgen der gleichen grammatikalischen Struktur
2. **Konsistente Pfadangaben** - Alle Verzeichnispfade sind vollständig und einheitlich formatiert
3. **Einheitliche Alert-Meldungen** - Alle Kategorien verwenden den gleichen Warntext bei Referenzen
4. **Copy-Funktion** - Klargestellt, dass immer Referenzen geschrieben werden
5. **Prompt-Konsistenz** - Alle [New]-Aktionen haben den gleichen Prompt-Hinweis
6. **Rechtschreibkorrekturen** - "Befor" → "Before", "Charakters" → "Characters"

### Hinzugefügte Elemente:

- **KeyAreas**: Vollständige [Actions]-Button-Beschreibung hinzugefügt
- **Scenes**: Spezifische Load-Funktion beibehalten (einzigartig für Scenes)
- **Alle Kategorien**: Einheitliche Delete-Warnung mit ListView-Anzeige

---

## 📋 Template-Struktur

Standard-Reihenfolge der Aktionen für alle Kategorien:

```
[…]       → Collapses ListView
[Load]    → Loads values (nur Scenes)
[New]     → Creates + writes reference + Prompt
[Edit]    → Opens Editor
[Actions] → Opens Action Editor (außer Scenes)
[Copy]    → Copies + writes reference
[Delete]  → Deletes + removes + Alert + Reference-Check + Warning
```

---

## 🔍 Wichtige Unterschiede

### Scenes (einzigartig):
- Hat einen **[Load]**-Button zum Laden von Scene-Daten
- Kein **[Actions]**-Button
- Copy-Funktion ohne zusätzliche Referenzschreibung

### Items, Characters, KeyAreas, Dialogs (gemeinsam):
- Alle haben einen **[Actions]**-Button
- Alle schreiben beim Erstellen eine Referenz zu `scenes/<nameOfScene>.txt`
- Alle haben identische Delete-Warnmeldungen mit Referenzprüfung

---

## 💡 Implementierungshinweise

Bei der Implementierung dieser Spezifikationen beachten:

1. **Referenzprüfung**: Vor jedem Delete muss ein vollständiger Scan aller `.txt`-Dateien erfolgen
2. **Konsistente Prompts**: Alle Eingabeaufforderungen sollten das gleiche Format verwenden
3. **Fehlerbehandlung**: Alert-Dialoge (Yes/No) sind obligatorisch bei Delete-Operationen
4. **Dateipfade**: Achten Sie auf konsistente Verwendung von `/resources/<category>/`
5. **ListView-Updates**: Nach Create, Copy und Delete muss die ListView aktualisiert werden
