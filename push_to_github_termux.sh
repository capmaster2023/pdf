#!/data/data/com.termux/files/usr/bin/bash
set -u

fail() { printf '\nErreur: %s\n' "$1" >&2; exit 1; }
command -v git >/dev/null 2>&1 || fail "Git est absent. Installez-le avec: pkg update && pkg install git"
[ -f settings.gradle.kts ] && [ -f app/build.gradle.kts ] || fail "Lancez ce script depuis le dossier racine de PDF Pocket Lite."

read -r -p "Nom d’utilisateur GitHub: " GITHUB_USER
read -r -p "Nom du dépôt GitHub: " REPO_NAME
[ -n "$GITHUB_USER" ] && [ -n "$REPO_NAME" ] || fail "Le nom d’utilisateur et le dépôt sont obligatoires."

if [ ! -d .git ]; then git init || fail "Impossible d’initialiser Git."; fi
git branch -M main || fail "Impossible de configurer la branche main."

git add . || fail "Impossible d’ajouter les fichiers."
if git diff --cached --quiet; then
  echo "Aucun nouveau changement à valider."
else
  git commit -m "Initial PDF Pocket Lite project" || fail "Le commit a échoué. Configurez git config user.name et user.email."
fi

REMOTE="https://github.com/${GITHUB_USER}/${REPO_NAME}.git"
if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REMOTE" || fail "Impossible de mettre à jour origin."
else
  git remote add origin "$REMOTE" || fail "Impossible d’ajouter origin."
fi

echo "GitHub demandera une authentification. Utilisez un token personnel, GitHub CLI ou un gestionnaire d’identifiants. Ne mettez jamais le token dans ce script."
git push -u origin main || fail "Push refusé. Vérifiez que le dépôt existe, que le token a accès au dépôt et que l’URL est correcte."
echo "Projet envoyé. Ouvrez maintenant l’onglet Actions du dépôt."
