import type { LocaleCatalog } from './en'

export const fr: LocaleCatalog = {
  app: {
    name: 'ValerisClient Launcher'
  },
  nav: {
    primary: 'Navigation principale',
    more: 'Plus',
    dashboard: 'Accueil',
    accounts: 'Comptes',
    profile: 'Profil',
    instances: 'Instances',
    skins: 'Skins',
    library: 'Bibliothèque',
    mods: 'Mods',
    resources: 'Packs de ressources',
    shaders: 'Shaders',
    store: 'Boutique',
    cosmetics: 'Cosmétiques',
    servers: 'Serveurs',
    friends: 'Amis',
    chat: 'Chat',
    news: 'Actualités',
    media: 'Médias',
    performance: 'Performances',
    downloads: 'Téléchargements',
    console: 'Console',
    settings: 'Paramètres'
  },
  boot: {
    core: 'Initialisation du cÅ“ur Valeris…',
    updates: 'Vérification des mises à jour…',
    minecraft: 'Chargement de Minecraft…',
    ready: 'Prêt.'
  },
  common: {
    play: 'Jouer',
    launching: 'Lancement…',
    join: 'Rejoindre',
    saved: 'Enregistré',
    guest: 'Invité',
    never: 'Jamais',
    free: 'Gratuit',
    prime: 'Valeris',
    primePlus: 'Valeris Plus',
    active: 'Actif',
    use: 'Utiliser',
    add: 'Ajouter',
    manage: 'Gérer',
    signIn: 'Se connecter',
    close: 'Fermer',
    checkNow: 'Vérifier',
    downloadUpdate: 'Télécharger la mise à jour',
    automatic: 'Automatique',
    updateNow: 'Mettre à jour'
  },
  newsTag: {
    update: 'Mise à jour',
    event: 'Événement',
    announcement: 'Annonce',
    launcher: 'Launcher'
  },
  dashboard: {
    signInTitle: 'Connectez-vous pour jouer',
    signInHint: 'Connectez Microsoft ou utilisez le mode hors ligne pour lancer ValerisClient.',
    addAccount: 'Ajouter un compte',
    welcomeBack: 'Bon retour',
    quickLaunch: 'Lancement rapide',
    profile: 'Profil',
    instance: 'Instance',
    primeAccount: 'Compte Valeris',
    lastSession: 'Dernière session',
    news: 'Actualités',
    moreNews: 'Toutes les actus',
    favoriteServers: 'Serveurs favoris',
    friendsActivity: 'Activité des amis',
    noFriendsOnline: 'Aucun ami en ligne',
    noFriendsOnlineHint: 'Ajoute des amis pour voir leur activité ici.',
    friendOnline: 'En ligne',
    friendAway: 'Absent',
    friendInGame: 'En jeu',
    noServers: 'Aucun serveur favori pour l’instant.',
    noServersHint: 'Ajoute un serveur pour le rejoindre depuis l’accueil.',
    addServer: 'Ajouter un serveur',
    playHub: {
      account: 'Compte',
      instance: 'Instance',
      server: 'Serveur',
      singleplayer: 'Solo / menu'
    },
    launchStatus: {
      preparing: 'Préparation…',
      download: 'Téléchargement…',
      mods: 'Installation des mods…',
      starting: 'Démarrage de Minecraft…',
      running: 'Jeu lancé',
      inGame: 'En jeu',
      error: 'Échec du lancement'
    },
    timePlayed: 'Temps de jeu',
    mods: 'Mods',
    ram: 'RAM',
    version: 'Version',
    whatsNew: {
      title: 'Nouveautés',
      themes: 'Trois thèmes synchronisés launcher et jeu (Crimson, Midnight, Aurora)',
      fps: 'Optimisation HUD — moins de perte FPS avec les modules désactivés',
      chat: 'Chat social en direct avec vrais pseudos',
      release: 'Voir la dernière release GitHub'
    }
  },
  settings: {
    title: 'Paramètres',
    subtitle: 'Enregistré localement — aucun compte ou cloud requis.',
    sections: {
      general: 'Général',
      appearance: 'Apparence',
      minecraft: 'Minecraft',
      performance: 'Performances',
      accounts: 'Comptes',
      privacy: 'Confidentialité',
      downloads: 'Téléchargements',
      updates: 'Mises à jour',
      advanced: 'Avancé'
    },
    language: {
      label: 'Langue',
      hint: "Langue d'affichage du launcher",
      en: 'English',
      fr: 'Français'
    },
    closeOnLaunch: {
      label: 'Réduire au lancement du jeu',
      hint: 'Le launcher reste ouvert — réduit seulement la fenêtre quand Minecraft démarre',
      toggle: 'Réduire au lancement'
    },
    checkUpdates: {
      label: 'Rechercher des mises à jour',
      hint: 'Comparer avec la dernière release GitHub'
    },
    autoUpdate: {
      label: 'Mise à jour automatique',
      hint: 'Vérifier GitHub au démarrage du launcher',
      toggle: 'Mise à jour auto'
    },
    theme: {
      label: 'Thème',
      hint: 'S’applique au launcher et se synchronise dans le profil du jeu',
      dark: 'Crimson',
      crimson: 'Crimson',
      midnight: 'Midnight',
      aurora: 'Aurora',
      obsidian: 'Obsidian',
      ember: 'Ember',
      elevated: 'Pro'
    },
    hardwareAccel: {
      label: 'Accélération matérielle',
      toggle: 'Accélération matérielle'
    },
    backgroundNebula: {
      label: 'Fond nébuleuse',
      hint: 'Fond animé débloqué dans la Boutique',
      toggle: 'Fond nébuleuse'
    },
    wallpaper: {
      label: 'Fond dâ€™Ã©cran',
      hint: 'Image optionnelle derrière le contenu',
      browse: 'Choisir une image',
      clear: 'Retirer'
    },
    accent: {
      label: 'Couleur d’accent',
      hint: 'Remplace l’accent du thème dans le launcher',
      reset: 'Réinitialiser'
    },
    uiSounds: {
      label: 'Sons d’interface',
      hint: 'Clics discrets sur Play et actions importantes',
      toggle: 'Sons d’interface'
    },
    restartRequired: 'Redémarrage requis pour l\'accélération matérielle.',
    restartNow: 'Redémarrer',
    javaPath: {
      label: 'Chemin Java par défaut',
      hint: 'JDK 21+ détecté automatiquement au lancement',
      addPath: 'Ajouter une path',
      browseFailed: 'Impossible d\'ajouter le chemin Java.'
    },
    defaultRam: {
      label: 'Allocation RAM par défaut'
    },
    gameResolution: {
      label: 'Résolution du jeu',
      hint: 'Taille de la fenêtre au lancement (largeur × hauteur en pixels)'
    },
    gameDisplayMode: {
      label: "Mode d'affichage",
      hint: 'Démarrage de Minecraft — le mode sans bordure utilise la résolution choisie en fenêtré',
      windowed: 'Fenêtré',
      borderless: 'Plein écran sans bordure',
      fullscreen: 'Plein écran'
    },
    performancePreset: {
      label: 'Profil de performance au lancement',
      low: 'PC faible',
      balanced: 'Équilibré',
      performance: 'Performance',
      ultra: 'Ultra'
    },
    activeAccount: {
      label: 'Compte actif',
      none: 'Aucun compte — ajoutez-en un pour jouer'
    },
    microsoftAccount: {
      label: 'Compte Microsoft'
    },
    analytics: {
      label: 'Analytiques',
      hint: 'Désactivé par défaut — rien n\'est envoyé sans serveur',
      toggle: 'Analytiques'
    },
    discordRpc: {
      label: 'Discord Rich Presence',
      hint: 'Statut ValerisClient Launcher sur Discord (le mod prend le relais en jeu)',
      toggle: 'Discord RPC'
    },
    concurrentDownloads: {
      label: 'Téléchargements simultanés'
    },
    jvmArgs: {
      label: 'Arguments JVM',
      hint: 'Un par ligne — aussi appliqués via les profils Performance'
    },
    developerMode: {
      label: 'Mode développeur',
      toggle: 'Mode développeur'
    },
    groqKey: {
      label: 'Clé Groq (override optionnel)',
      hint: 'Laisse vide — tout le monde utilise le proxy Valeris (clé serveur). Clé locale seulement pour self-host / hors ligne.',
      placeholder: 'gsk_…',
      save: 'Enregistrer',
      clear: 'Effacer',
      statusOk: 'Override local ({masked})',
      statusMissing: 'IA Valeris partagée (pas de clé perso)',
      statusProxy: 'IA Valeris partagée en ligne',
      statusDown: 'IA partagée hors ligne — clé locale ou réessaie plus tard'
    },
    updateNotes: 'v{current} → dernière v{latest} — {notes}'
  },
  ai: {
    title: 'Valeris Assistant',
    subtitle: 'Installer des mods & conseils pour ton instance',
    placeholder: 'Ex : Installe Sodium, Lithium et Iris…',
    send: 'Envoyer',
    thinking: 'Réflexion…',
    install: 'Installer',
    installing: 'Installation…',
    installed: 'Installé',
    installFailed: 'Échec',
    emptyTitle: 'Pose une question',
    emptyDesc:
      'Installe des mods, tips FPS, ou dépannage crash via latest.log & crash-reports.',
    needKey: 'IA partagée hors ligne. Réessaie plus tard, ou ajoute une clé locale (optionnel).',
    saveKey: 'Enregistrer',
    keyPlaceholder: 'gsk_… (optionnel)',
    instance: 'Instance : {name} ({version})',
    sourceModrinth: 'Modrinth',
    sourceCurseforge: 'CurseForge',
    clearChat: 'Effacer',
    errorGeneric: 'Une erreur est survenue',
    quickDiagnose: 'Analyser le crash',
    quickLatestLog: 'Lire latest.log',
    quickLauncherLog: 'Log launcher'
  },
  updates: {
    modal: {
      title: 'Mise à jour disponible',
      subtitle: 'Une nouvelle version de ValerisClient est prête. Installez-la sans quitter le launcher.'
    },
    launcher: {
      label: 'ValerisClient Launcher'
    },
    mod: {
      label: 'Mod ValerisClient'
    },
    versionLine: 'v{current} → v{latest}',
    installLauncher: 'Installer le launcher',
    installMod: 'Installer le mod',
    installing: 'Installation…',
    checking: 'Vérification…',
    later: 'Plus tard',
    upToDate: 'À jour',
    phase: {
      downloading: 'Téléchargement…',
      installing: 'Installation…',
      done: 'Terminé',
      error: 'Échec'
    },
    errors: {
      dev_mode: "La mise à jour intégrée ne fonctionne qu'avec l'application installée, pas en mode dev.",
      unsupported_platform: 'La mise à jour intégrée du launcher est disponible uniquement sur Windows.',
      no_update: 'Aucune mise à jour à installer.',
      game_running: 'Fermez Minecraft avant de mettre à jour le mod.',
      no_instance: 'Aucune instance — créez-en une d\'abord.',
      prime_mod_disabled: 'Le mod Valeris est désactivé sur l\'instance par défaut.',
      unknown: 'Échec de la mise à jour. Réessayez ou téléchargez depuis GitHub.'
    }
  },
  pages: {
    accounts: {
      title: 'Comptes',
      subtitle: 'Profil actif, comptes Microsoft et hors ligne.'
    },
    profile: {
      title: 'Profil',
      subtitle: 'Skin, grade, temps de jeu et amis.'
    },
    instances: {
      title: 'Instances',
      subtitle: 'Tes installs Minecraft — jouer, configurer, rester simple.'
    },
    mods: {
      title: 'Mods',
      subtitle: "Gérez les mods de l'instance active — fichiers locaux et Modrinth."
    },
    resources: {
      title: 'Packs de ressources',
      subtitle: "Importez ou installez des packs pour l'instance active."
    },
    shaders: {
      title: 'Shaders',
      subtitle: "Importez ou installez des shaders pour l'instance active."
    },
    store: {
      title: 'Boutique Valeris',
      subtitle: 'Débloquez cosmétiques et thèmes avec des Valeris Coins — sync cloud en ligne.'
    },
    skins: {
      title: 'Skins',
      subtitle: 'Aperçu du personnage, capes et ailes Valeris — ton look prêt à jouer.'
    },
    library: {
      title: 'Bibliothèque',
      subtitle: 'Mods, packs, shaders et outils pour l’instance active — sans surcharger la sidebar.'
    },
    cosmetics: {
      title: 'Cosmétiques',
      subtitle: 'Vraies capes et ailes — visibles pour toi et les autres joueurs ValerisClient.'
    },
    servers: {
      title: 'Hub Serveurs',
      subtitle: 'Bannières, joueurs en direct, ping et fiches serveur complètes.'
    },
    friends: {
      title: 'Amis',
      subtitle: 'Amis Valeris sync launcher ↔ jeu — demandes et présence en direct.'
    },
    chat: {
      title: 'Chat',
      subtitle: 'Messages privés avec tes amis — en direct, sync avec le jeu.'
    },
    news: {
      title: 'Actualités',
      subtitle: 'Annonces intégrées — pas de serveur de news distant.'
    },
    media: {
      title: 'Médias',
      subtitle: 'Captures et enregistrements du dossier instance.'
    },
    performance: {
      title: 'Performances',
      subtitle: 'Infos matériel et profils — RAM, distance de rendu, flags JVM.'
    },
    downloads: {
      title: 'Téléchargements',
      subtitle: 'Installations Modrinth et tâches du launcher.'
    },
    console: {
      title: 'Console',
      subtitle: 'Sortie de lancement, téléchargements, avertissements et erreurs en temps réel.'
    }
  },
  modals: {
    login: {
      title: 'Connexion à Valeris',
      subtitle: 'Utilisez votre compte Microsoft pour Minecraft officiel, ou jouez hors ligne.',
      microsoft: 'Se connecter avec Microsoft',
      offlineDivider: '— ou hors ligne —',
      offlinePlaceholder: 'Pseudo hors ligne',
      offlineContinue: 'Continuer hors ligne',
      cancel: 'Annuler',
      loginFailed: 'Échec de la connexion.',
      offlineFailed: 'Impossible de créer le compte hors ligne.'
    },
    instance: {
      createTitle: 'Nouvelle instance',
      editTitle: 'Configurer l\'instance',
      subtitle: 'Stockée localement dans AppData — choisissez un type et une version Minecraft. Valeris installe le bon jar automatiquement.',
      kind: 'Type d\'instance',
      kindPrimeHint: 'Fabric + mod ValerisClient',
      kindFabricHint: 'Loader Fabric uniquement',
      kindVanillaHint: 'Sans mods / loader',
      recommended: 'Recommandé',
      primeJarHint: 'Installe {prefix}-*.jar',
      javaHint: 'Java {major}+ recommandé',
      primeAutoNote: 'Valeris inclut toujours Fabric API et le mod ValerisClient correspondant à la version.',
      showAdvanced: 'Afficher les options avancées',
      hideAdvanced: 'Masquer les options avancées',
      name: 'Nom',
      minecraftVersion: 'Version Minecraft',
      loader: 'Loader',
      fabricLoader: 'Loader Fabric',
      includePrimeMod: 'Inclure le mod ValerisClient (+ Fabric API)',
      ram: 'RAM (Mo)',
      jvmArgs: 'Arguments JVM (un par ligne)',
      javaPath: 'Chemin Java (optionnel)',
      javaPathHint: 'Remplace le chemin global pour cette instance',
      createFailed: 'Impossible de créer l\'instance.',
      saveFailed: 'Impossible d\'enregistrer l\'instance.',
      saving: 'Enregistrement…',
      create: 'Créer'
    },
    browse: {
      modsTitle: 'Parcourir — Mods',
      resourcePacksTitle: 'Parcourir — Packs de ressources',
      shadersTitle: 'Parcourir — Shaders',
      subtitle: 'Recherchez sur Modrinth ou CurseForge — téléchargements vers l\'instance active.',
      searching: 'Recherche…',
      searchFailed: 'Échec de la recherche.',
      installFailed: 'Échec de l\'installation.',
      close: 'Fermer',
      chooseVersion: 'Choisir la version',
      chooseVersionHint: 'Sélectionnez la version à installer pour cette instance.',
      versionLabel: 'Version',
      gameVersions: 'Versions MC',
      loaders: 'Loaders',
      recommended: 'Recommandée',
      back: 'Retour',
      noVersions: 'Aucune version compatible trouvée.',
      versionsFailed: 'Impossible de charger les versions.'
    },
    modrinth: {
      modsTitle: 'Modrinth — Mods',
      resourcePacksTitle: 'Modrinth — Packs de ressources',
      shadersTitle: 'Modrinth — Shaders',
      subtitle: 'API Modrinth publique — téléchargements vers le dossier de l\'instance active.',
      searching: 'Recherche…',
      searchFailed: 'Échec de la recherche.',
      installFailed: 'Échec de l\'installation.',
      close: 'Fermer'
    }
  },
  profile: {
    memberSince: 'Membre depuis',
    playtime: 'Temps de jeu',
    playtimeValue: '{hours}h {minutes}m',
    accounts: 'Comptes',
    friends: 'Amis',
    recentFriends: 'Amis',
    noFriends: 'Aucun ami',
    noFriendsHint: 'Ajoute des amis depuis la page Amis.'
  },
  accounts: {
    addAccount: 'Ajouter un compte',
    activeProfile: 'Profil actif',
    editSkin: 'Modifier le skin',
    primeAccount: 'Compte Valeris',
    quickAdd: 'Ajout rapide',
    quickAddHint: 'Microsoft ouvre une fenêtre sécurisée (Xbox Live → Minecraft).',
    signInMicrosoft: 'Se connecter avec Microsoft',
    offlinePlaceholder: 'Pseudo hors ligne',
    addOffline: 'Ajouter hors ligne',
    minecraftAccounts: 'Comptes Minecraft',
    count: '{count} enregistré(s)',
    noAccounts: 'Aucun compte',
    emptyHint: 'Connecte-toi avec Microsoft ou ajoute un profil hors ligne pour jouer.',
    addOne: 'Ajoutez-en un',
    toPlay: 'pour jouer.',
    level: 'Niveau {level}',
    syncDescription: 'Configs, HUD, cosmétiques et stats restent sur ce PC — sync locale uniquement.',
    syncButton: 'Sync profil',
    refreshMicrosoft: 'Rafraîchir le token',
    refreshSuccess: 'Token Microsoft rafraîchi.',
    refreshFailed: 'Échec du rafraîchissement du token.',
    remove: 'Supprimer le compte',
    microsoft: 'Microsoft',
    offline: 'Hors ligne'
  },
  servers: {
    addServer: 'Ajouter un serveur',
    serverName: 'Nom du serveur',
    serverAddress: 'Adresse (host ou host:port)',
    refresh: 'Actualiser le statut',
    remove: 'Supprimer',
    empty: 'Aucun serveur favori.',
    joinNeedsAccount: 'Connectez-vous pour rejoindre un serveur.',
    added: 'Serveur ajouté.',
    addFailed: 'Impossible d\'ajouter le serveur.',
    removeFailed: 'Impossible de supprimer le serveur.',
    info: 'Informations',
    partner: 'Partenaire',
    online: 'En ligne',
    offlineCached: 'Cache',
    noDescription: 'Pas encore de description.',
    versionUnknown: 'Version inconnue',
    social: {
      discord: 'Discord',
      store: 'Boutique',
      website: 'Site web',
      vote: 'Vote',
      youtube: 'YouTube',
      tiktok: 'TikTok',
      instagram: 'Instagram',
      facebook: 'Facebook',
      x: 'X'
    }
  },
  friends: {
    addFriend: 'Ajouter un ami',
    usernamePrompt: 'Pseudo Minecraft',
    notePrompt: 'Note (optionnel)',
    notePlaceholder: 'Modifier la note…',
    saveNote: 'Enregistrer',
    remove: 'Supprimer',
    empty: 'Aucun ami. Les deux joueurs doivent ouvrir Valeris une fois.',
    offline: 'Hors ligne',
    addFailed: 'Impossible d\'ajouter l\'ami.',
    refreshStatus: 'Actualiser le statut',
    message: 'Message',
    inviteParty: 'Inviter party',
    accept: 'Accepter',
    partyServer: 'Serveur de party',
    joinPartyServer: 'Rejoindre le serveur',
    partyJoinPrompt: 'Ta party veut que tu rejoignes',
    dismiss: 'Ignorer',
    shareServer: 'Partager un serveur avec la party',
    shareServerPrompt: 'hôte:port ou adresse…',
    shareServerFailed: 'Impossible de partager (réservé au leader)',
    partyInvite: 'Invitation party',
    partyInviteUnknown: 'Quelqu’un t’a invité en party',
    acceptParty: 'Accepter',
    declineParty: 'Refuser',
    leaveParty: 'Quitter la party',
    createParty: 'Créer une party',
    partyMembers: 'Ta party',
    partyAlone: 'Toi seul'
  },
  chat: {
    refresh: 'Actualiser',
    conversations: 'Conversations',
    empty: 'Aucune conversation pour l’instant. Ouvre un DM depuis Amis.',
    selectConversation: 'Choisis une conversation',
    selectHint: 'Sélectionne quelqu’un à gauche pour ouvrir le fil privé.',
    messagePlaceholder: 'Écrire un message…',
    send: 'Envoyer',
    image: 'Image',
    imagePathPrompt: 'Chemin du fichier image',
    live: 'En direct',
    directMessage: 'Message privé',
    startTitle: 'Dis bonjour',
    startBody: 'Début de ta conversation avec {name}.',
    composerHint: 'Entrée pour envoyer · images supportées',
    today: 'Aujourd’hui',
    yesterday: 'Hier',
    typing: '{name} est en train dâ€™Ã©crire…'
  },
  actions: {
    import: 'Importer',
    browseModrinth: 'Parcourir Modrinth',
    browseContent: 'Parcourir en ligne',
    searchCurseForge: 'Rechercher sur CurseForge…',
    openFolder: 'Ouvrir le dossier',
    configure: 'Configurer',
    folder: 'Dossier',
    duplicate: 'Dupliquer',
    delete: 'Supprimer',
    equip: 'Équiper',
    unequip: 'Retirer',
    owned: 'Possédé',
    buy: 'Acheter',
    claim: 'Obtenir',
    free: 'Gratuit',
    applyPreset: 'Appliquer le profil',
    applying: 'Application…',
    clearCompleted: 'Effacer terminés',
    refresh: 'Actualiser',
    cancel: 'Annuler',
    save: 'Enregistrer',
    searchMods: 'Rechercher des mods…',
    searchModrinth: 'Rechercher sur Modrinth…',
    install: 'Installer',
    installing: 'Installation…',
    importJar: 'Importer .jar',
    close: 'Fermer',
    create: 'Créer'
  },
  mods: {
    all: 'Tout',
    enabled: 'Activés',
    disabled: 'Désactivés'
  },
  empty: {
    noMods: 'Aucun mod dans cette instance.',
    noResourcePacks: 'Aucun pack de ressources. Importez un .zip ou installez depuis Modrinth.',
    noShaders: 'Aucun shader. Installez Iris (mod) d\'abord, puis ajoutez des packs ici.',
    noDownloads: 'Aucun téléchargement.',
    noDownloadsHint: 'Aucun téléchargement récent. Lancez Minecraft pour voir la progression.',
    noScreenshots: 'Aucun média. F2 pour une capture, ou enregistrez un clip via ValerisClient (Créateur → Clip Recorder).',
    loadingInstance: 'Chargement de l\'instance…'
  },
  resources: {
    importZip: 'Importer .zip',
    active: 'Actif'
  },
  confirm: {
    deleteInstance: 'Supprimer « {name} » ? Les fichiers peuvent être conservés ou supprimés.',
    deleteFiles: 'Supprimer aussi saves et mods sur le disque ?',
    removeMod: 'Supprimer {name} ?',
    removePack: 'Supprimer {name} ?',
    removeShader: 'Supprimer {name} ?'
  },
  errors: {
    deleteInstance: 'Impossible de supprimer l\'instance.'
  },
  performance: {
    detectedHardware: 'Matériel détecté',
    cpu: 'CPU',
    gpu: 'GPU',
    ram: 'RAM',
    applySuccess: 'Profil appliqué à l\'instance active (RAM + options.txt).',
    applyFailed: 'Échec de l\'application du profil.',
    detecting: 'Détection du matériel…',
    systemRam: 'RAM système',
    chunks: '{count} chunks'
  },
  store: {
    coins: '{balance} Valeris Coins',
    unlocked: '{name} débloqué ! Équipez depuis Cosmétiques.',
    purchaseFailed: 'Achat échoué.',
    coinsPrice: '{price} coins',
    buyFor: 'Acheter · {price} coins',
    preview: 'Aperçu',
    emptyTitle: 'Rien ici',
    emptyDesc: 'Essayez une autre catégorie ou revenez plus tard.',
    historyEmptyTitle: 'Aucun achat',
    historyEmptyDesc: 'Les articles débloqués apparaîtront ici.',
    promoPlaceholder: 'Code promo',
    redeem: 'Utiliser',
    redeemed: 'Utilisé',
    promoOk: '+{coins} Valeris Coins ajoutés.',
    promoFailed: 'Impossible d\'utiliser le code.',
    syncSynced: 'Synchronisé',
    syncLocal: 'Local',
    tabs: {
      catalog: 'Catalogue',
      history: 'Historique',
      promos: 'Promos'
    },
    categories: {
      all: 'Tout',
      cosmetic: 'Cosmétiques',
      theme: 'Thèmes',
      background: 'Fonds',
      badge: 'Badges'
    }
  },
  cosmetics: {
    characterPreview: 'Aperçu personnage',
    previewHint: 'Le loadout équipé se sync dans le jeu. Capes et ailes visibles pour les peers Valeris.',
    peersNote: 'Visible pour les peers Valeris (LAN / intégré)',
    all: 'Tout',
    capes: 'Capes',
    wings: 'Ailes',
    badges: 'Badges',
    emptyOwned: 'Aucun cosmétique — visitez la Boutique.'
  },
  skins: {
    tabs: {
      skins: 'Skins',
      capes: 'Capes',
      wings: 'Ailes',
      badges: 'Badges'
    },
    pose: {
      idle: 'Immobile',
      walk: 'Marcher',
      run: 'Courir'
    },
    addSkin: 'Importer PNG',
    addSoon: 'Import de skins custom bientôt',
    applySkin: 'Appliquer ce skin',
    applyCosmetic: 'Appliquer le cosmétique',
    changeCape: 'Changer cape',
    openStore: 'Ouvrir la Boutique',
    remove: 'Supprimer',
    emptyCosmeticsHint: 'Débloque des cosmétiques dans la Boutique, puis équipe-les ici.'
  },
  onboarding: {
    eyebrow: 'Configuration initiale',
    subtitle: 'Quelques préférences, puis vous êtes prêt à jouer.',
    next: 'Continuer',
    back: 'Retour',
    finish: 'C’est parti',
    skip: 'Passer pour l’instant',
    browseFolder: 'Choisir un dossier',
    useDefaultFolder: 'Utiliser le défaut',
    folderHint: 'Les instances et fichiers de jeu seront stockés ici. Gardez le défaut sauf besoin d’un autre disque.',
    signedIn: 'Prêt à lancer',
    steps: {
      language: {
        short: 'Langue',
        title: 'Choisis ta langue',
        body: 'English ou Français — modifiable plus tard dans Réglages.'
      },
      theme: {
        short: 'Thème',
        title: 'Choisis un style',
        body: 'S’applique au launcher et se synchronise dans le profil Valeris en jeu.'
      },
      ram: {
        short: 'RAM',
        title: 'Mémoire par défaut',
        body: 'RAM allouée aux nouvelles instances. Modifiable par instance ensuite.'
      },
      folder: {
        short: 'Dossier',
        title: 'Dossier des instances',
        body: 'Emplacement disque où Valeris stocke les instances Minecraft.'
      },
      account: {
        short: 'Compte',
        title: 'Connexion',
        body: 'Microsoft est recommandé. Les profils offline marchent en local.'
      }
    }
  },
  import: {
    title: 'Importer des instances',
    subtitle: 'Détecte d’autres launchers et copie mods, packs, captures et options.',
    scanning: 'Scan des dossiers courants…',
    foundCount: '{count} trouvé(s)',
    notFound: 'Non détecté',
    selectInstances: 'Sélectionne les instances à importer',
    selectAll: 'Tout sélectionner',
    deselectAll: 'Tout désélectionner',
    loadingInstances: 'Chargement des instances…',
    emptySource: 'Aucune instance pour ce launcher.',
    importing: 'Import…',
    importSelected: 'Importer ({count})',
    success: '{count} instance(s) importée(s).',
    errorDetect: 'Impossible de scanner les autres launchers.',
    errorList: 'Impossible de lister les instances.',
    errorImport: 'Échec de l’import.',
    tagMods: 'mods',
    tagPacks: 'resource packs'
  },
  social: {
    title: 'Amis',
    onlineCount: '{count} en ligne',
    emptyTitle: 'Pas encore d’amis',
    emptyDesc: 'Ajoute des amis pour voir qui est en ligne et rejoindre leur serveur.',
    openFriends: 'Ouvrir Amis'
  },
  whatsNew: {
    eyebrow: 'Mise à jour',
    title: 'Nouveautés',
    gotIt: 'Compris',
    changelog: 'Changelog complet'
  },
  instances: {
    vanilla: 'Vanilla',
    fabric: 'Fabric',
    ValerisClient: 'ValerisClient',
    primeBadge: 'Valeris',
    newInstance: 'Nouvelle instance',
    import: 'Importer',
    createPrime26: 'Valeris 26.2',
    createPrime121: 'Valeris 1.21.11',
    play: 'Jouer',
    default: 'Par défaut',
    setDefault: 'Définir par défaut',
    loading: 'Chargement des instances…',
    signInToPlay: 'Se connecter',
    lastPlayed: 'Dernière session {date}',
    ramBadge: '{mb} Mo RAM',
    modsBadge: '{count} mods',
    emptyTitle: 'Aucune instance',
    emptyDesc: 'Crée une install ValerisClient pour commencer.'
  },
  media: {
    openFolder: 'Ouvrir le dossier clips',
    refresh: 'Actualiser',
    replaysNote: 'Replays et clips apparaissent ici via ValerisClient (config/valerisclient/clips).'
  },
  logs: {
    title: 'Logs de lancement',
    tab: 'Console',
    empty: 'Aucun log. Appuyez sur Jouer pour voir le téléchargement et le lancement ici.',
    openFolder: 'Ouvrir le dossier logs',
    clear: 'Effacer',
    copy: 'Tout copier',
    copied: 'Copié !',
    filterAll: 'Tous',
    filterInfo: 'Info',
    filterWarn: 'Warn',
    filterError: 'Erreurs',
    filterDebug: 'Debug'
  },
  crash: {
    title: 'Minecraft a crashé',
    sessionDuration: 'Session de {duration}',
    context: 'Contexte :',
    screen: 'Écran :',
    primeInvolved: 'ValerisClient impliqué',
    suggestion: 'Piste de correction',
    openReport: 'Ouvrir le crash report',
    openLogs: 'Ouvrir les logs',
    sendReport: 'Envoyer à Valeris',
    askAi: 'Demander à l’IA',
    dismiss: 'Fermer',
    fix: {
      blurOnce:
        'Conflit de rendu GUI (limite de blur). Mettez ValerisClient à jour — ce bug est corrigé dans les builds récents.',
      outOfMemory:
        'Java manque de mémoire. Augmentez la RAM dans les paramètres de l\'instance ou fermez d\'autres applications.',
      primeMod:
        'Le crash vient de ValerisClient ({location}). Mettez le mod à jour ou désactivez les modules récemment modifiés.',
      modConflict:
        'Conflit de mods probable. Désactivez les mods ajoutés récemment et retestez.',
      modConflictNamed:
        'Mods impliqués dans la stack trace : {mods}. Désactivez-les un par un.',
      loaderError:
        'Fabric ou un mod n\'a pas pu se charger. Vérifiez que les versions correspondent à Minecraft.',
      unknown:
        'Ouvrez le crash report pour les détails complets. Si le problème persiste, partagez-le au support.'
    }
  }
}
