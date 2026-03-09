# Steps to Push This Project to GitHub

Follow these commands in **PowerShell** from the project root:

## 1) Open terminal in project root

Project path:

`C:\Users\akshi\OneDrive\Desktop\ResumeAnalyser`

---

## 2) Check Git is installed

```powershell
git --version
```

If it is not installed, install Git from: https://git-scm.com/download/win

---

## 3) Initialize Git (only if not already initialized)

```powershell
git init
```

---

## 4) Configure your Git identity (first time only)

```powershell
git config --global user.name "Your Name"
git config --global user.email "your-email@example.com"
```

Verify:

```powershell
git config --global --list
```

---

## 5) Create a `.gitignore` (recommended)

Use this if you do not already have one:

```gitignore
# Maven / Java
target/
*.class

# IntelliJ / VS Code
.idea/
*.iml
.vscode/

# Logs
*.log

# Python
ml-service/.venv/
ml-service/__pycache__/
*.pyc

# OS
.DS_Store
Thumbs.db
```

---

## 6) Stage and commit files

```powershell
git add .
git commit -m "Initial project commit"
```

---

## 7) Create an empty GitHub repository

On GitHub, create a new repository (do **not** add README/.gitignore/license there if this repo already has files).

Copy your repo URL, for example:

- `https://github.com/<username>/ResumeAnalyser.git`

---

## 8) Add remote origin

```powershell
git remote add origin https://github.com/<username>/ResumeAnalyser.git
```

If origin already exists:

```powershell
git remote set-url origin https://github.com/<username>/ResumeAnalyser.git
```

Check remotes:

```powershell
git remote -v
```

---

## 9) Push to GitHub

Use one of these (depending on branch name):

```powershell
git branch -M main
git push -u origin main
```

If your branch is `master` instead:

```powershell
git push -u origin master
```

---

## 10) For next pushes

```powershell
git add .
git commit -m "Your commit message"
git push
```

---

## Troubleshooting

### Authentication failed

- GitHub no longer accepts account password for Git over HTTPS.
- Use a **Personal Access Token (PAT)** when prompted for password.

### Remote already exists

```powershell
git remote remove origin
git remote add origin https://github.com/<username>/ResumeAnalyser.git
```

### Check current branch

```powershell
git branch
```
