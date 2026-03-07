---
title: "Agent | Firecrawl"
source: "https://docs.firecrawl.dev/fr/features/agent"
author:
  - "[[Firecrawl Docs]]"
published:
created: 2025-12-20
description: "Collectez les données où qu'elles se trouvent sur le web. Décrivez ce que vous voulez, /agent s'occupe du reste."
tags:
  - "clippings"
---
Firecrawl `/agent` est une API révolutionnaire qui recherche, parcourt et collecte des données même depuis les sites web les plus complexes, trouvant des données dans des endroits difficiles d’accès et en découvrant partout sur internet. Elle accomplit en quelques minutes ce qui prendrait de nombreuses heures à un humain, et rend le scraping web traditionnel obsolète.**Décrivez simplement les données que vous souhaitez et `/agent` s’occupe du reste.**

**Version de recherche (Research Preview)**: Agent est en accès anticipé. Attendez-vous à quelques limitations. Il s’améliorera considérablement au fil du temps. [Donnez votre avis →](https://docs.firecrawl.dev/fr/features/)

Agent s’appuie sur tout ce qui fait la force de `/extract` et va encore plus loin:
- **Aucune URL requise**: Décrivez simplement ce dont vous avez besoin via le paramètre `prompt`. Les URL sont facultatives.
- **Recherche web approfondie**: Explore et navigue automatiquement en profondeur dans les sites pour trouver vos données
- **Fiable et précis**: Fonctionne avec un large éventail de requêtes et de cas d’usage
- **Plus rapide**: Traite plusieurs sources en parallèle pour des résultats plus rapides
- **Moins cher**: Agent est plus économique que `/extract` pour les cas d’usage complexes

## Utilisation de /agent

Le seul paramètre requis est `prompt`. Décrivez simplement les données que vous souhaitez extraire. Pour une sortie structurée, fournissez un schéma JSON. Les SDK prennent en charge Pydantic (Python) et Zod (Node) pour des définitions de schémas avec typage sûr:

### Réponse

JSON

## Fournir des URL (facultatif)

Vous pouvez éventuellement fournir des URL pour cibler l’agent sur des pages spécifiques:

## Statut et fin de la tâche

Les tâches d’agent s’exécutent de manière asynchrone. Lorsque vous soumettez une tâche, vous recevez un ID de tâche que vous pouvez utiliser pour consulter son statut:
- **Méthode par défaut**: `agent()` attend la fin de l’exécution et renvoie les résultats finaux
- **Démarrer puis interroger**: utilisez `start_agent` (Python) ou `startAgent` (Node) pour obtenir immédiatement un ID de tâche, puis interrogez avec `get_agent_status` / `getAgentStatus`

Les résultats de la tâche sont disponibles pendant 24 heures après la fin de l’exécution.

### États possibles

#### Exemple en attente

JSON

#### Exemple complété

JSON

## Paramètres

## Agent vs Extract: ce qui a été amélioré

## Exemples de cas d’utilisation

- **Recherche**: “Trouver les 5 principales startups d’IA et les montants de leurs financements”
- **Analyse concurrentielle**: “Comparer les offres tarifaires entre Slack et Microsoft Teams”
- **Collecte de données**: “Extraire les informations de contact depuis les sites web d’entreprises”
- **Synthèse de contenu**: “Résumer les derniers articles de blog sur le web scraping”

## Référence de l’API

Consultez la [Référence de l’API Agent](https://docs.firecrawl.dev/fr/api-reference/endpoint/agent) pour plus de détails.Vous avez des commentaires ou besoin d’aide? Envoyez un e-mail à [help@firecrawl.com](https://docs.firecrawl.dev/fr/features/).

## Tarification

Firecrawl Agent utilise une **facturation dynamique** qui s’adapte à la complexité de votre demande d’extraction de données. Vous payez en fonction du travail réellement effectué par Firecrawl Agent, ce qui garantit une tarification équitable, que vous extrayiez des données simples ou des informations structurées complexes provenant de plusieurs sources.

### Fonctionnement de la tarification de l’agent

La tarification de l’agent est **dynamique et basée sur les crédits** pendant la Research Preview:
- **Les extractions simples** (comme les informations de contact à partir d’une seule page) consomment généralement moins de crédits et coûtent moins cher
- **Les tâches de recherche complexes** (comme une analyse concurrentielle sur plusieurs domaines) consomment plus de crédits mais reflètent mieux l’effort total requis
- **Une transparence totale sur l’utilisation** vous montre exactement combien de crédits chaque requête a consommé
- **La conversion de crédits** convertit automatiquement l’utilisation de crédits par l’agent en crédits pour une facturation simplifiée

L’utilisation de crédits varie en fonction de la complexité de votre prompt, de la quantité de données traitées et de la structure du résultat demandé.

### Pour commencer

**Tous les utilisateurs** bénéficient de **5 exécutions gratuites par jour** pour explorer les fonctionnalités d’Agent sans frais.L’utilisation supplémentaire est facturée en fonction de la consommation de crédits et convertie en crédits.

### Gestion des coûts

Gardez le contrôle de vos dépenses liées à Agent:
- **Commencez avec des exécutions gratuites**: utilisez vos 5 requêtes gratuites quotidiennes pour comprendre la tarification
- **Définissez un paramètre `maxCredits`**: limitez vos dépenses en définissant un nombre maximal de crédits que vous êtes prêt à utiliser
- **Optimisez les prompts**: des prompts plus spécifiques consomment souvent moins de crédits
- **Surveillez votre utilisation**: suivez votre consommation via le tableau de bord
- **Définissez des attentes claires**: des recherches complexes couvrant plusieurs domaines utiliseront plus de crédits que de simples extractions sur une seule page
Essayez Agent dès maintenant sur [firecrawl.dev/app/agent](https://www.firecrawl.dev/app/agent) pour voir comment l’utilisation des crédits évolue selon vos cas d’usage spécifiques.

La tarification est susceptible d’évoluer à mesure que nous passons de la Research Preview à la disponibilité générale. Les utilisateurs actuels recevrront un préavis avant toute mise à jour des tarifs.[Guide de scraping avancé](https://docs.firecrawl.dev/fr/advanced-scraping-guide)

[

Précédent

](https://docs.firecrawl.dev/fr/advanced-scraping-guide)[

Scrape

Suivant

](https://docs.firecrawl.dev/fr/features/scrape)