# PDF Pocket Lite

Application Android locale et légère de lecture, création et traitement de fichiers PDF. Le dépôt est conçu pour être téléversé depuis un téléphone puis compilé par GitHub Actions, sans Android Studio ni ordinateur local.

Le package provisoire est `com.pdfpocket.lite`. L’application ne reprend ni la marque, ni le logo, ni le code, ni l’interface d’Adobe.

## État réel de la version 1.0

### Fonctionnel dans le projet

- accueil Material 3, navigation téléphone, thèmes clair/sombre/système ;
- ouverture de PDF avec le Storage Access Framework et prise en charge des URI `content://` ;
- réception d’un PDF par `Ouvrir avec` et par partage Android ;
- lecteur basé sur `PdfRenderer`, chargement page par page, défilement continu ou page unique ;
- zoom par pincement, double toucher, page précédente/suivante ;
- recherche de texte avec PDFBox lorsque le PDF contient une couche texte ;
- PDF protégés avec demande du mot de passe ;
- historique Room, favoris et reprise à la dernière page ;
- partage du document ouvert ;
- images JPEG/PNG/WebP vers PDF, ordre modifiable, A4/Lettre/automatique, portrait/paysage et marges ;
- scanner multipage CameraX avec flash, reprise de pages, duplication, suppression et ordre ;
- fusion de plusieurs PDF ;
- extraction de pages et de plages ;
- rotation de pages ou de plages ;
- filigrane texte ;
- numéros de page ;
- protection et déverrouillage par mot de passe connu ;
- compression légère, équilibrée ou forte ;
- conversion des pages PDF en JPEG ;
- extraction directe d’images intégrées lorsque PDFBox peut les lire ;
- lecture et remplissage des formulaires AcroForm compatibles, avec aplatissement facultatif ;
- OCR local d’une image ou d’une page PDF avec ML Kit ;
- signatures dessinées, importées ou textuelles, stockées chiffrées avec Android Keystore ;
- placement d’une signature visuelle sur une page et export dans une nouvelle copie ;
- verrouillage biométrique ou PIN local ;
- blocage facultatif des captures d’écran ;
- nettoyage du cache avec WorkManager ;
- français et anglais ;
- compilation debug et release signée par GitHub Actions.

### Limites techniques déclarées

Le projet n’affiche pas de boutons prétendant faire ce que le moteur ne sait pas enregistrer correctement.

- La détection automatique des contours, la correction de perspective et le recadrage avancé du scanner ne sont pas inclus dans cette version.
- La compression équilibrée et forte rasterise les pages. Elle peut réduire la qualité et supprimer la sélection de texte.
- L’OCR retourne du texte modifiable et exportable. Il ne réinjecte pas encore une couche texte invisible dans un PDF consultable.
- Les signatures sont visuelles. Elles ne sont pas des signatures numériques certifiées avec certificat PKI.
- Les annotations vectorielles complètes, le surlignage lié aux coordonnées de texte et l’éditeur de miniatures par glisser-déposer ne sont pas encore exposés dans l’interface.
- Les formulaires XFA ne sont pas pris en charge. Les formulaires AcroForm standards dépendent des capacités de PDFBox Android.
- L’extraction d’images peut produire des doublons lorsqu’une même ressource est réutilisée sur plusieurs pages.
- Certains PDF chiffrés, endommagés ou utilisant des fonctions très récentes peuvent être refusés proprement.

Les fichiers originaux ne sont jamais écrasés automatiquement. Chaque transformation demande un nouveau fichier ou dossier de sortie.

## Architecture

Le projet utilise un module `app` et des packages séparés :

```text
com.pdfpocket.lite
├── core                 validateurs, plages, noms et erreurs
├── data/local           Room, entités et DAO
├── data/repository      dépôts de documents, paramètres et signatures
├── domain               interfaces métier
├── di                   modules Hilt
├── storage              SAF, URI, fichiers temporaires et partage
├── security             Android Keystore et PIN PBKDF2
├── pdf                  PdfRenderer, PDFBox et création de PDF
├── ocr                  ML Kit Text Recognition
├── workers              nettoyage WorkManager
├── navigation           Navigation Compose
├── ui                    thème et composants
└── features             accueil, fichiers, lecteur, outils, scanner,
                         OCR, signatures, paramètres, confidentialité
```

L’état des écrans repose sur `ViewModel`, `StateFlow` et les coroutines. Room ne stocke que les métadonnées et URI, jamais le contenu complet des PDF.

## Bibliothèques principales et licences

- AndroidX, Jetpack Compose, Material 3, Navigation, Room, CameraX, WorkManager, Biometric et DataStore : licences Apache 2.0.
- Hilt/Dagger : licence Apache 2.0.
- PDFBox Android `com.tom-roush:pdfbox-android:2.0.27.0` : licence Apache 2.0.
- ML Kit Text Recognition : bibliothèque Google distribuée selon ses conditions applicables, traitement local avec le modèle embarqué utilisé ici.
- Le code propre au dépôt est sous licence MIT, voir `LICENSE`.

`PdfRenderer` est utilisé pour afficher les pages sans charger le document complet en mémoire. PDFBox Android est utilisé pour les transformations que son port Android prend réellement en charge.

## Compiler uniquement avec GitHub depuis un téléphone

1. Téléchargez puis extrayez `PDF-Pocket-Lite.zip` dans le stockage du téléphone.
2. Créez un dépôt GitHub vide, sans README automatique afin d’éviter un conflit inutile.
3. Envoyez tous les fichiers du dossier racine dans le dépôt. Le dossier `.github` doit être présent.
4. Ouvrez le dépôt dans le navigateur ou l’application GitHub.
5. Ouvrez l’onglet **Actions**.
6. Choisissez **Build debug APK**.
7. Appuyez sur **Run workflow**, sélectionnez `main`, puis lancez-le.
8. Ouvrez l’exécution terminée.
9. Dans **Artifacts**, téléchargez `pdf-pocket-lite-debug-apk`.
10. Extrayez le ZIP de l’artefact.
11. Installez `app-debug.apk`.
12. Android peut demander d’autoriser l’installation depuis le navigateur ou le gestionnaire de fichiers utilisé. Accordez cette autorisation uniquement à l’application choisie, puis désactivez-la après installation si souhaité.

Le fichier généré par Gradle se trouve à :

```text
app/build/outputs/apk/debug/app-debug.apk
```

Le workflow exécute :

```bash
chmod +x gradlew
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Envoyer le projet avec Termux

Dans Termux :

```bash
pkg update
pkg install git
termux-setup-storage
cd /sdcard/Download/PDF-Pocket-Lite
chmod +x push_to_github_termux.sh
./push_to_github_termux.sh
```

Le script demande le nom d’utilisateur et le dépôt. Il ne contient aucun mot de passe ni token. GitHub n’accepte pas le mot de passe du compte pour un `git push` HTTPS. Utilisez un token personnel limité au dépôt, GitHub CLI ou un gestionnaire d’identifiants officiel.

En cas de `Permission denied` sur un script placé dans `/sdcard`, exécutez-le explicitement avec Bash :

```bash
bash push_to_github_termux.sh
```

## Release signée avec GitHub Actions

Le workflow `.github/workflows/build-release.yml` s’exécute avec un tag tel que `v1.0.0` ou manuellement. Ajoutez dans **Settings > Secrets and variables > Actions** :

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Pour convertir un keystore en Base64 dans Termux :

```bash
base64 -w 0 mon-keystore.jks > keystore-base64.txt
```

Copiez le contenu du fichier texte dans `KEYSTORE_BASE64`. Ne téléversez jamais le keystore, le fichier Base64, un token ou un mot de passe dans le dépôt.

Le workflow debug reste indépendant des secrets.

## Modifier le nom, le package, l’icône ou les couleurs

- Nom visible : `app/src/main/res/values/strings.xml`, clé `app_name`.
- Package : `namespace` et `applicationId` dans `app/build.gradle.kts`, puis déplacement du dossier Kotlin et mise à jour des déclarations `package`.
- Icône : ressources `app/src/main/res/drawable/ic_launcher_*` et `mipmap-*`.
- Couleurs Compose : `ui/theme/Theme.kt`.
- Couleur du splash : `res/values/colors.xml`.
- Nouvelle langue : créez un dossier comme `values-es` et copiez-y `strings.xml` traduit.

## Permissions

- `CAMERA` : scanner les pages avec CameraX.
- `USE_BIOMETRIC` : verrouillage local facultatif.

L’application ne demande ni `INTERNET`, ni `READ_EXTERNAL_STORAGE`, ni `WRITE_EXTERNAL_STORAGE`, ni `MANAGE_EXTERNAL_STORAGE`. Les fichiers sont choisis par le Storage Access Framework.

## Confidentialité et sécurité

- aucun compte obligatoire ;
- aucun document téléversé automatiquement ;
- aucune clé API ;
- aucune publicité ;
- `android:allowBackup="false"` ;
- PIN dérivé avec PBKDF2 et sel aléatoire ;
- signatures chiffrées en AES-GCM avec une clé Android Keystore ;
- cache effaçable manuellement et nettoyé périodiquement ;
- captures d’écran bloquables avec `FLAG_SECURE` ;
- mot de passe d’un PDF utilisé uniquement pour l’opération demandée et non enregistré dans Room.

## Tests

Les tests unitaires couvrent notamment les noms de fichiers, plages de pages, mots de passe, ordre des pages, noms d’export et calcul de taille. Des tests instrumentés vérifient le démarrage, la navigation principale et la présence du bouton d’ouverture de PDF.

## Résolution de problèmes GitHub Actions

- `gradlew: Permission denied` : vérifiez que le workflow contient `chmod +x gradlew`.
- `gradle-wrapper.jar missing` : le fichier doit rester dans `gradle/wrapper/`.
- échec d’authentification Termux : le dépôt doit exister et le token doit avoir la permission d’écriture.
- artefact absent : ouvrez l’étape **Build debug APK** avant l’étape d’envoi de l’artefact.
- installation refusée : activez temporairement l’autorisation d’installer des applications inconnues pour l’application ayant ouvert l’APK.

## Commandes locales facultatives

Android Studio n’est pas requis. Sur un environnement possédant déjà le SDK Android, les commandes standards restent :

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Le dépôt ne dépend jamais d’un ordinateur local pour la compilation normale prévue.
