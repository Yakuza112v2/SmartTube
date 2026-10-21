# SmartTube Personal auf GitHub

Dieses Setup baut SmartTube im eigenen GitHub-Repository und veröffentlicht
installierbare, signierte APKs unter **Releases**. Die Personal-Version speichert
Wiedergabestände in einer lokalen Datenbank je Profil, unabhängig vom begrenzten
Speichercache und von der YouTube-Synchronisierung.

Während der Wiedergabe wird alle 15 Sekunden und zusätzlich beim Pausieren,
Video-Wechsel und Beenden gespeichert. Ein plötzlicher Stromausfall kann daher
die letzten Sekunden verlieren. Ein bereits übersprungenes Video gilt anhand
seiner Abspielposition als angefangen, auch wenn bis dahin vorgespult wurde.
Ab 10 Sekunden Abspielposition oder 95 Prozent blendet die App Videos aus
Startseite, Abos und Empfehlungen aus. Bekannter YouTube-Fortschritt wird bei
fehlendem lokalen Eintrag ebenfalls berücksichtigt. Suche und Verlauf bleiben
zugänglich; Livestreams und Kapitel werden nicht ausgeblendet. Die Rückkehr zu
Startseite oder Abos aktualisiert die Liste, wenn sich der lokale Verlauf geändert hat.

Inkognito und deaktivierter Verlauf erzeugen keine neuen lokalen Wiedergabestände.
Ein lokal vorhandener Fortschritt hat beim Fortsetzen Vorrang vor YouTube-Angaben.
Die Übernahme von Fortschritt anderer Geräte ist damit keine vollständige Synchronisierung.

## Einmalige Einrichtung

1. Auf https://github.com/yuliskov/SmartTube/fork einen Fork erstellen.
2. Die vorbereiteten Änderungen in diesen Fork übernehmen.
3. Einen eigenen Signierschlüssel erzeugen und sicher aufbewahren. Derselbe
   Schlüssel muss für alle Updates verwendet werden. Beispiel mit JDK `keytool`:

   ```text
   keytool -genkeypair -keystore personal.p12 -storetype PKCS12 -alias personal -keyalg RSA -keysize 3072 -validity 10000
   ```

4. Unter **Settings → Secrets and variables → Actions** vier Repository-Secrets
   hinterlegen (entsprechen dem offiziellen SmartTube-Build-Setup):

   | Secret | Inhalt |
   | --- | --- |
   | `SIGNING_KEY` | Inhalt der Datei `personal.p12`, als Base64 codiert |
   | `KEY_STORE_PASSWORD` | Passwort des Keystores |
   | `ALIAS` | `personal` |
   | `KEY_PASSWORD` | Passwort des Schlüssels |

   Keystore und Passwörter niemals in den Quellcode oder einen Release hochladen.

5. Im Tab **Actions** die Workflows des Forks aktivieren und
   **SmartTube Personal APK → Run workflow** starten.
6. Nach erfolgreichem Build unter **Releases** die APK herunterladen.

## Download am Fernseher

Der feste Link für die Universal-APK lautet nach dem ersten erfolgreichen Release:

```text
https://github.com/DEIN-KONTO/SmartTube/releases/latest/download/SmartTube-Personal-universal.apk
```

Bei einem anderen Repository-Namen den Link entsprechend anpassen. Für einen
Download ohne GitHub-Anmeldung am Fernseher muss der Fork öffentlich sein.

Die App heißt **SmartTube Personal** und verwendet `org.smarttube.personal`.
Sie lässt sich neben der offiziellen App installieren und beginnt mit eigenen
App-Daten. Die automatischen Update-Quellen des Originalprojekts sind für diesen
Build leer; neue Personal-Versionen werden über die Releases dieses Forks installiert.

## Stand der Überprüfung

Der Quellstand ist SmartTube 32.68 mit den vom Projekt festgelegten Submodulen.
Ein erfolgreicher GitHub-Build und ein anschließender Test am Fernseher stehen
noch aus. Der Workflow führt zunächst vier Datenbanktests aus (Neustart, mehr
als 300 Videos, getrennte Profile und dauerhaftes Löschen). Er prüft vor der Veröffentlichung die APK-Signaturen und
legt SHA-256-Prüfsummen bei. Eine Signaturprüfung ersetzt keinen Funktionstest.
