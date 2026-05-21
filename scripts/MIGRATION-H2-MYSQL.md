# Migration H2 → MySQL

## Configuration actuelle

- **Profil par défaut** : `dev` → MySQL `localhost:3306/doctoratdb`, utilisateur `root`, mot de passe vide.
- **Profil H2** (optionnel) : `--spring.profiles.active=h2` → fichier `./data/doctoratdb.mv.db`

## Cas 1 : ancienne base H2 en mémoire (`jdbc:h2:mem:...`)

Les données **ne sont pas persistées** après arrêt de l’application.  
Au premier démarrage MySQL :

1. Hibernate crée les tables (`ddl-auto=update`).
2. `DemoDataInitializer` recrée les comptes de démo (admin, directeur, candidat, etc.).

```bash
# Démarrer MySQL, puis l’application
mvn spring-boot:run
```

## Cas 2 : exporter une base H2 fichier vers MySQL

1. Démarrer une fois avec H2 fichier pour générer `./data/doctoratdb.mv.db` :
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=h2
   ```
2. Arrêter l’application (les données sont dans `./data/`).
3. Lancer la migration vers MySQL :
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev,migrate-h2
   ```

## Cas 3 : export manuel depuis la console H2

1. Profil `h2`, ouvrir http://localhost:8080/h2-console  
2. JDBC URL : `jdbc:h2:file:./data/doctoratdb`  
3. Exécuter : `SCRIPT TO './data/h2-export.sql';`  
4. Adapter le SQL pour MySQL (types, syntaxe) puis l’importer dans `doctoratdb`.

## Vérification MySQL

```sql
USE doctoratdb;
SHOW TABLES;
SELECT COUNT(*) FROM users;
```

Comptes démo attendus après seed :

| Rôle      | Email                 | Mot de passe   |
|-----------|------------------------|----------------|
| ADMIN     | admin@doctorat.ma      | admin123       |
| DIRECTEUR | directeur@doctorat.ma  | directeur123   |
| CANDIDAT  | candidat@doctorat.ma   | candidat123    |
