import type { LocaleCatalog } from './en'

export const fr: LocaleCatalog = {
  app: {
    name: 'StellarClient Launcher'
  },
  nav: {
    primary: 'Navigation principale',
    more: 'Plus',
    dashboard: 'Accueil',
    accounts: 'Comptes',
    profile: 'Profil',
    instances: 'Instances',
    skins: 'Skins',
    library: 'BibliothÃ¨que',
    mods: 'Mods',
    resources: 'Packs de ressources',
    shaders: 'Shaders',
    store: 'Boutique',
    cosmetics: 'CosmÃ©tiques',
    servers: 'Serveurs',
    friends: 'Amis',
    chat: 'Chat',
    news: 'ActualitÃ©s',
    media: 'MÃ©dias',
    performance: 'Performances',
    downloads: 'TÃ©lÃ©chargements',
    console: 'Console',
    settings: 'ParamÃ¨tres'
  },
  boot: {
    core: 'Initialisation du cÅ“ur Primeâ€¦',
    updates: 'VÃ©rification des mises Ã  jourâ€¦',
    minecraft: 'Chargement de Minecraftâ€¦',
    ready: 'PrÃªt.'
  },
  common: {
    play: 'Jouer',
    launching: 'Lancementâ€¦',
    join: 'Rejoindre',
    saved: 'EnregistrÃ©',
    guest: 'InvitÃ©',
    never: 'Jamais',
    free: 'Gratuit',
    prime: 'Prime',
    primePlus: 'Prime Plus',
    active: 'Actif',
    use: 'Utiliser',
    add: 'Ajouter',
    manage: 'GÃ©rer',
    signIn: 'Se connecter',
    close: 'Fermer',
    checkNow: 'VÃ©rifier',
    downloadUpdate: 'TÃ©lÃ©charger la mise Ã  jour',
    automatic: 'Automatique',
    updateNow: 'Mettre Ã  jour'
  },
  newsTag: {
    update: 'Mise Ã  jour',
    event: 'Ã‰vÃ©nement',
    announcement: 'Annonce',
    launcher: 'Launcher'
  },
  dashboard: {
    signInTitle: 'Connectez-vous pour jouer',
    signInHint: 'Connectez Microsoft ou utilisez le mode hors ligne pour lancer StellarClient.',
    addAccount: 'Ajouter un compte',
    welcomeBack: 'Bon retour',
    quickLaunch: 'Lancement rapide',
    profile: 'Profil',
    instance: 'Instance',
    primeAccount: 'Compte Prime',
    lastSession: 'DerniÃ¨re session',
    news: 'ActualitÃ©s',
    moreNews: 'Toutes les actus',
    favoriteServers: 'Serveurs favoris',
    friendsActivity: 'ActivitÃ© des amis',
    noFriendsOnline: 'Aucun ami en ligne',
    noFriendsOnlineHint: 'Ajoute des amis pour voir leur activitÃ© ici.',
    friendOnline: 'En ligne',
    friendAway: 'Absent',
    friendInGame: 'En jeu',
    noServers: 'Aucun serveur favori pour lâ€™instant.',
    noServersHint: 'Ajoute un serveur pour le rejoindre depuis lâ€™accueil.',
    addServer: 'Ajouter un serveur',
    playHub: {
      account: 'Compte',
      instance: 'Instance',
      server: 'Serveur',
      singleplayer: 'Solo / menu'
    },
    launchStatus: {
      preparing: 'PrÃ©parationâ€¦',
      download: 'TÃ©lÃ©chargementâ€¦',
      mods: 'Installation des modsâ€¦',
      starting: 'DÃ©marrage de Minecraftâ€¦',
      running: 'Jeu lancÃ©',
      inGame: 'En jeu',
      error: 'Ã‰chec du lancement'
    },
    timePlayed: 'Temps de jeu',
    mods: 'Mods',
    ram: 'RAM',
    version: 'Version',
    whatsNew: {
      title: 'NouveautÃ©s',
      themes: 'Trois thÃ¨mes synchronisÃ©s launcher et jeu (Crimson, Midnight, Aurora)',
      fps: 'Optimisation HUD â€” moins de perte FPS avec les modules dÃ©sactivÃ©s',
      chat: 'Chat social en direct avec vrais pseudos',
      release: 'Voir la derniÃ¨re release GitHub'
    }
  },
  settings: {
    title: 'ParamÃ¨tres',
    subtitle: 'EnregistrÃ© localement â€” aucun compte ou cloud requis.',
    sections: {
      general: 'GÃ©nÃ©ral',
      appearance: 'Apparence',
      minecraft: 'Minecraft',
      performance: 'Performances',
      accounts: 'Comptes',
      privacy: 'ConfidentialitÃ©',
      downloads: 'TÃ©lÃ©chargements',
      updates: 'Mises Ã  jour',
      advanced: 'AvancÃ©'
    },
    language: {
      label: 'Langue',
      hint: "Langue d'affichage du launcher",
      en: 'English',
      fr: 'FranÃ§ais'
    },
    closeOnLaunch: {
      label: 'RÃ©duire au lancement du jeu',
      hint: 'Le launcher reste ouvert â€” rÃ©duit seulement la fenÃªtre quand Minecraft dÃ©marre',
      toggle: 'RÃ©duire au lancement'
    },
    checkUpdates: {
      label: 'Rechercher des mises Ã  jour',
      hint: 'Comparer avec la derniÃ¨re release GitHub'
    },
    autoUpdate: {
      label: 'Mise Ã  jour automatique',
      hint: 'VÃ©rifier GitHub au dÃ©marrage du launcher',
      toggle: 'Mise Ã  jour auto'
    },
    theme: {
      label: 'ThÃ¨me',
      hint: 'Sâ€™applique au launcher et se synchronise dans le profil du jeu',
      dark: 'Crimson',
      crimson: 'Crimson',
      midnight: 'Midnight',
      aurora: 'Aurora',
      obsidian: 'Obsidian',
      ember: 'Ember',
      elevated: 'Pro'
    },
    hardwareAccel: {
      label: 'AccÃ©lÃ©ration matÃ©rielle',
      toggle: 'AccÃ©lÃ©ration matÃ©rielle'
    },
    backgroundNebula: {
      label: 'Fond nÃ©buleuse',
      hint: 'Fond animÃ© dÃ©bloquÃ© dans la Boutique',
      toggle: 'Fond nÃ©buleuse'
    },
    wallpaper: {
      label: 'Fond dâ€™Ã©cran',
      hint: 'Image optionnelle derriÃ¨re le contenu',
      browse: 'Choisir une image',
      clear: 'Retirer'
    },
    accent: {
      label: 'Couleur dâ€™accent',
      hint: 'Remplace lâ€™accent du thÃ¨me dans le launcher',
      reset: 'RÃ©initialiser'
    },
    uiSounds: {
      label: 'Sons dâ€™interface',
      hint: 'Clics discrets sur Play et actions importantes',
      toggle: 'Sons dâ€™interface'
    },
    restartRequired: 'RedÃ©marrage requis pour l\'accÃ©lÃ©ration matÃ©rielle.',
    restartNow: 'RedÃ©marrer',
    javaPath: {
      label: 'Chemin Java par dÃ©faut',
      hint: 'JDK 21+ dÃ©tectÃ© automatiquement au lancement',
      addPath: 'Ajouter une path',
      browseFailed: 'Impossible d\'ajouter le chemin Java.'
    },
    defaultRam: {
      label: 'Allocation RAM par dÃ©faut'
    },
    gameResolution: {
      label: 'RÃ©solution du jeu',
      hint: 'Taille de la fenÃªtre au lancement (largeur Ã— hauteur en pixels)'
    },
    gameDisplayMode: {
      label: "Mode d'affichage",
      hint: 'DÃ©marrage de Minecraft â€” le mode sans bordure utilise la rÃ©solution choisie en fenÃªtrÃ©',
      windowed: 'FenÃªtrÃ©',
      borderless: 'Plein Ã©cran sans bordure',
      fullscreen: 'Plein Ã©cran'
    },
    performancePreset: {
      label: 'Profil de performance au lancement',
      low: 'PC faible',
      balanced: 'Ã‰quilibrÃ©',
      performance: 'Performance',
      ultra: 'Ultra'
    },
    activeAccount: {
      label: 'Compte actif',
      none: 'Aucun compte â€” ajoutez-en un pour jouer'
    },
    microsoftAccount: {
      label: 'Compte Microsoft'
    },
    analytics: {
      label: 'Analytiques',
      hint: 'DÃ©sactivÃ© par dÃ©faut â€” rien n\'est envoyÃ© sans serveur',
      toggle: 'Analytiques'
    },
    discordRpc: {
      label: 'Discord Rich Presence',
      hint: 'Statut StellarClient Launcher sur Discord (le mod prend le relais en jeu)',
      toggle: 'Discord RPC'
    },
    concurrentDownloads: {
      label: 'TÃ©lÃ©chargements simultanÃ©s'
    },
    jvmArgs: {
      label: 'Arguments JVM',
      hint: 'Un par ligne â€” aussi appliquÃ©s via les profils Performance'
    },
    developerMode: {
      label: 'Mode dÃ©veloppeur',
      toggle: 'Mode dÃ©veloppeur'
    },
    groqKey: {
      label: 'ClÃ© Groq (override optionnel)',
      hint: 'Laisse vide â€” tout le monde utilise le proxy Prime (clÃ© serveur). ClÃ© locale seulement pour self-host / hors ligne.',
      placeholder: 'gsk_â€¦',
      save: 'Enregistrer',
      clear: 'Effacer',
      statusOk: 'Override local ({masked})',
      statusMissing: 'IA Prime partagÃ©e (pas de clÃ© perso)',
      statusProxy: 'IA Prime partagÃ©e en ligne',
      statusDown: 'IA partagÃ©e hors ligne â€” clÃ© locale ou rÃ©essaie plus tard'
    },
    updateNotes: 'v{current} â†’ derniÃ¨re v{latest} â€” {notes}'
  },
  ai: {
    title: 'Prime Assistant',
    subtitle: 'Installer des mods & conseils pour ton instance',
    placeholder: 'Ex : Installe Sodium, Lithium et Irisâ€¦',
    send: 'Envoyer',
    thinking: 'RÃ©flexionâ€¦',
    install: 'Installer',
    installing: 'Installationâ€¦',
    installed: 'InstallÃ©',
    installFailed: 'Ã‰chec',
    emptyTitle: 'Pose une question',
    emptyDesc:
      'Installe des mods, tips FPS, ou dÃ©pannage crash via latest.log & crash-reports.',
    needKey: 'IA partagÃ©e hors ligne. RÃ©essaie plus tard, ou ajoute une clÃ© locale (optionnel).',
    saveKey: 'Enregistrer',
    keyPlaceholder: 'gsk_â€¦ (optionnel)',
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
      title: 'Mise Ã  jour disponible',
      subtitle: 'Une nouvelle version de StellarClient est prÃªte. Installez-la sans quitter le launcher.'
    },
    launcher: {
      label: 'StellarClient Launcher'
    },
    mod: {
      label: 'Mod StellarClient'
    },
    versionLine: 'v{current} â†’ v{latest}',
    installLauncher: 'Installer le launcher',
    installMod: 'Installer le mod',
    installing: 'Installationâ€¦',
    checking: 'VÃ©rificationâ€¦',
    later: 'Plus tard',
    upToDate: 'Ã€ jour',
    phase: {
      downloading: 'TÃ©lÃ©chargementâ€¦',
      installing: 'Installationâ€¦',
      done: 'TerminÃ©',
      error: 'Ã‰chec'
    },
    errors: {
      dev_mode: "La mise Ã  jour intÃ©grÃ©e ne fonctionne qu'avec l'application installÃ©e, pas en mode dev.",
      unsupported_platform: 'La mise Ã  jour intÃ©grÃ©e du launcher est disponible uniquement sur Windows.',
      no_update: 'Aucune mise Ã  jour Ã  installer.',
      game_running: 'Fermez Minecraft avant de mettre Ã  jour le mod.',
      no_instance: 'Aucune instance â€” crÃ©ez-en une d\'abord.',
      prime_mod_disabled: 'Le mod Prime est dÃ©sactivÃ© sur l\'instance par dÃ©faut.',
      unknown: 'Ã‰chec de la mise Ã  jour. RÃ©essayez ou tÃ©lÃ©chargez depuis GitHub.'
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
      subtitle: 'Tes installs Minecraft â€” jouer, configurer, rester simple.'
    },
    mods: {
      title: 'Mods',
      subtitle: "GÃ©rez les mods de l'instance active â€” fichiers locaux et Modrinth."
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
      title: 'Boutique Prime',
      subtitle: 'DÃ©bloquez cosmÃ©tiques et thÃ¨mes avec des Prime Coins â€” sync cloud en ligne.'
    },
    skins: {
      title: 'Skins',
      subtitle: 'AperÃ§u du personnage, capes et ailes Prime â€” ton look prÃªt Ã  jouer.'
    },
    library: {
      title: 'BibliothÃ¨que',
      subtitle: 'Mods, packs, shaders et outils pour lâ€™instance active â€” sans surcharger la sidebar.'
    },
    cosmetics: {
      title: 'CosmÃ©tiques',
      subtitle: 'Vraies capes et ailes â€” visibles pour toi et les autres joueurs StellarClient.'
    },
    servers: {
      title: 'Hub Serveurs',
      subtitle: 'BanniÃ¨res, joueurs en direct, ping et fiches serveur complÃ¨tes.'
    },
    friends: {
      title: 'Amis',
      subtitle: 'Amis Prime sync launcher â†” jeu â€” demandes et prÃ©sence en direct.'
    },
    chat: {
      title: 'Chat',
      subtitle: 'Messages privÃ©s avec tes amis â€” en direct, sync avec le jeu.'
    },
    news: {
      title: 'ActualitÃ©s',
      subtitle: 'Annonces intÃ©grÃ©es â€” pas de serveur de news distant.'
    },
    media: {
      title: 'MÃ©dias',
      subtitle: 'Captures et enregistrements du dossier instance.'
    },
    performance: {
      title: 'Performances',
      subtitle: 'Infos matÃ©riel et profils â€” RAM, distance de rendu, flags JVM.'
    },
    downloads: {
      title: 'TÃ©lÃ©chargements',
      subtitle: 'Installations Modrinth et tÃ¢ches du launcher.'
    },
    console: {
      title: 'Console',
      subtitle: 'Sortie de lancement, tÃ©lÃ©chargements, avertissements et erreurs en temps rÃ©el.'
    }
  },
  modals: {
    login: {
      title: 'Connexion Ã  Prime',
      subtitle: 'Utilisez votre compte Microsoft pour Minecraft officiel, ou jouez hors ligne.',
      microsoft: 'Se connecter avec Microsoft',
      offlineDivider: 'â€” ou hors ligne â€”',
      offlinePlaceholder: 'Pseudo hors ligne',
      offlineContinue: 'Continuer hors ligne',
      cancel: 'Annuler',
      loginFailed: 'Ã‰chec de la connexion.',
      offlineFailed: 'Impossible de crÃ©er le compte hors ligne.'
    },
    instance: {
      createTitle: 'Nouvelle instance',
      editTitle: 'Configurer l\'instance',
      subtitle: 'StockÃ©e localement dans AppData â€” choisissez un type et une version Minecraft. Prime installe le bon jar automatiquement.',
      kind: 'Type d\'instance',
      kindPrimeHint: 'Fabric + mod StellarClient',
      kindFabricHint: 'Loader Fabric uniquement',
      kindVanillaHint: 'Sans mods / loader',
      recommended: 'RecommandÃ©',
      primeJarHint: 'Installe {prefix}-*.jar',
      javaHint: 'Java {major}+ recommandÃ©',
      primeAutoNote: 'Prime inclut toujours Fabric API et le mod StellarClient correspondant Ã  la version.',
      showAdvanced: 'Afficher les options avancÃ©es',
      hideAdvanced: 'Masquer les options avancÃ©es',
      name: 'Nom',
      minecraftVersion: 'Version Minecraft',
      loader: 'Loader',
      fabricLoader: 'Loader Fabric',
      includePrimeMod: 'Inclure le mod StellarClient (+ Fabric API)',
      ram: 'RAM (Mo)',
      jvmArgs: 'Arguments JVM (un par ligne)',
      javaPath: 'Chemin Java (optionnel)',
      javaPathHint: 'Remplace le chemin global pour cette instance',
      createFailed: 'Impossible de crÃ©er l\'instance.',
      saveFailed: 'Impossible d\'enregistrer l\'instance.',
      saving: 'Enregistrementâ€¦',
      create: 'CrÃ©er'
    },
    browse: {
      modsTitle: 'Parcourir â€” Mods',
      resourcePacksTitle: 'Parcourir â€” Packs de ressources',
      shadersTitle: 'Parcourir â€” Shaders',
      subtitle: 'Recherchez sur Modrinth ou CurseForge â€” tÃ©lÃ©chargements vers l\'instance active.',
      searching: 'Rechercheâ€¦',
      searchFailed: 'Ã‰chec de la recherche.',
      installFailed: 'Ã‰chec de l\'installation.',
      close: 'Fermer',
      chooseVersion: 'Choisir la version',
      chooseVersionHint: 'SÃ©lectionnez la version Ã  installer pour cette instance.',
      versionLabel: 'Version',
      gameVersions: 'Versions MC',
      loaders: 'Loaders',
      recommended: 'RecommandÃ©e',
      back: 'Retour',
      noVersions: 'Aucune version compatible trouvÃ©e.',
      versionsFailed: 'Impossible de charger les versions.'
    },
    modrinth: {
      modsTitle: 'Modrinth â€” Mods',
      resourcePacksTitle: 'Modrinth â€” Packs de ressources',
      shadersTitle: 'Modrinth â€” Shaders',
      subtitle: 'API Modrinth publique â€” tÃ©lÃ©chargements vers le dossier de l\'instance active.',
      searching: 'Rechercheâ€¦',
      searchFailed: 'Ã‰chec de la recherche.',
      installFailed: 'Ã‰chec de l\'installation.',
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
    primeAccount: 'Compte Prime',
    quickAdd: 'Ajout rapide',
    quickAddHint: 'Microsoft ouvre une fenÃªtre sÃ©curisÃ©e (Xbox Live â†’ Minecraft).',
    signInMicrosoft: 'Se connecter avec Microsoft',
    offlinePlaceholder: 'Pseudo hors ligne',
    addOffline: 'Ajouter hors ligne',
    minecraftAccounts: 'Comptes Minecraft',
    count: '{count} enregistrÃ©(s)',
    noAccounts: 'Aucun compte',
    emptyHint: 'Connecte-toi avec Microsoft ou ajoute un profil hors ligne pour jouer.',
    addOne: 'Ajoutez-en un',
    toPlay: 'pour jouer.',
    level: 'Niveau {level}',
    syncDescription: 'Configs, HUD, cosmÃ©tiques et stats restent sur ce PC â€” sync locale uniquement.',
    syncButton: 'Sync profil',
    refreshMicrosoft: 'RafraÃ®chir le token',
    refreshSuccess: 'Token Microsoft rafraÃ®chi.',
    refreshFailed: 'Ã‰chec du rafraÃ®chissement du token.',
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
    added: 'Serveur ajoutÃ©.',
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
    notePlaceholder: 'Modifier la noteâ€¦',
    saveNote: 'Enregistrer',
    remove: 'Supprimer',
    empty: 'Aucun ami. Les deux joueurs doivent ouvrir Prime une fois.',
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
    shareServerPrompt: 'hÃ´te:port ou adresseâ€¦',
    shareServerFailed: 'Impossible de partager (rÃ©servÃ© au leader)',
    partyInvite: 'Invitation party',
    partyInviteUnknown: 'Quelquâ€™un tâ€™a invitÃ© en party',
    acceptParty: 'Accepter',
    declineParty: 'Refuser',
    leaveParty: 'Quitter la party',
    createParty: 'CrÃ©er une party',
    partyMembers: 'Ta party',
    partyAlone: 'Toi seul'
  },
  chat: {
    refresh: 'Actualiser',
    conversations: 'Conversations',
    empty: 'Aucune conversation pour lâ€™instant. Ouvre un DM depuis Amis.',
    selectConversation: 'Choisis une conversation',
    selectHint: 'SÃ©lectionne quelquâ€™un Ã  gauche pour ouvrir le fil privÃ©.',
    messagePlaceholder: 'Ã‰crire un messageâ€¦',
    send: 'Envoyer',
    image: 'Image',
    imagePathPrompt: 'Chemin du fichier image',
    live: 'En direct',
    directMessage: 'Message privÃ©',
    startTitle: 'Dis bonjour',
    startBody: 'DÃ©but de ta conversation avec {name}.',
    composerHint: 'EntrÃ©e pour envoyer Â· images supportÃ©es',
    today: 'Aujourdâ€™hui',
    yesterday: 'Hier',
    typing: '{name} est en train dâ€™Ã©crireâ€¦'
  },
  actions: {
    import: 'Importer',
    browseModrinth: 'Parcourir Modrinth',
    browseContent: 'Parcourir en ligne',
    searchCurseForge: 'Rechercher sur CurseForgeâ€¦',
    openFolder: 'Ouvrir le dossier',
    configure: 'Configurer',
    folder: 'Dossier',
    duplicate: 'Dupliquer',
    delete: 'Supprimer',
    equip: 'Ã‰quiper',
    unequip: 'Retirer',
    owned: 'PossÃ©dÃ©',
    buy: 'Acheter',
    claim: 'Obtenir',
    free: 'Gratuit',
    applyPreset: 'Appliquer le profil',
    applying: 'Applicationâ€¦',
    clearCompleted: 'Effacer terminÃ©s',
    refresh: 'Actualiser',
    cancel: 'Annuler',
    save: 'Enregistrer',
    searchMods: 'Rechercher des modsâ€¦',
    searchModrinth: 'Rechercher sur Modrinthâ€¦',
    install: 'Installer',
    installing: 'Installationâ€¦',
    importJar: 'Importer .jar',
    close: 'Fermer',
    create: 'CrÃ©er'
  },
  mods: {
    all: 'Tout',
    enabled: 'ActivÃ©s',
    disabled: 'DÃ©sactivÃ©s'
  },
  empty: {
    noMods: 'Aucun mod dans cette instance.',
    noResourcePacks: 'Aucun pack de ressources. Importez un .zip ou installez depuis Modrinth.',
    noShaders: 'Aucun shader. Installez Iris (mod) d\'abord, puis ajoutez des packs ici.',
    noDownloads: 'Aucun tÃ©lÃ©chargement.',
    noDownloadsHint: 'Aucun tÃ©lÃ©chargement rÃ©cent. Lancez Minecraft pour voir la progression.',
    noScreenshots: 'Aucun mÃ©dia. F2 pour une capture, ou enregistrez un clip via StellarClient (CrÃ©ateur â†’ Clip Recorder).',
    loadingInstance: 'Chargement de l\'instanceâ€¦'
  },
  resources: {
    importZip: 'Importer .zip',
    active: 'Actif'
  },
  confirm: {
    deleteInstance: 'Supprimer Â« {name} Â» ? Les fichiers peuvent Ãªtre conservÃ©s ou supprimÃ©s.',
    deleteFiles: 'Supprimer aussi saves et mods sur le disque ?',
    removeMod: 'Supprimer {name} ?',
    removePack: 'Supprimer {name} ?',
    removeShader: 'Supprimer {name} ?'
  },
  errors: {
    deleteInstance: 'Impossible de supprimer l\'instance.'
  },
  performance: {
    detectedHardware: 'MatÃ©riel dÃ©tectÃ©',
    cpu: 'CPU',
    gpu: 'GPU',
    ram: 'RAM',
    applySuccess: 'Profil appliquÃ© Ã  l\'instance active (RAM + options.txt).',
    applyFailed: 'Ã‰chec de l\'application du profil.',
    detecting: 'DÃ©tection du matÃ©rielâ€¦',
    systemRam: 'RAM systÃ¨me',
    chunks: '{count} chunks'
  },
  store: {
    coins: '{balance} Prime Coins',
    unlocked: '{name} dÃ©bloquÃ© ! Ã‰quipez depuis CosmÃ©tiques.',
    purchaseFailed: 'Achat Ã©chouÃ©.',
    coinsPrice: '{price} coins',
    buyFor: 'Acheter Â· {price} coins',
    preview: 'AperÃ§u',
    emptyTitle: 'Rien ici',
    emptyDesc: 'Essayez une autre catÃ©gorie ou revenez plus tard.',
    historyEmptyTitle: 'Aucun achat',
    historyEmptyDesc: 'Les articles dÃ©bloquÃ©s apparaÃ®tront ici.',
    promoPlaceholder: 'Code promo',
    redeem: 'Utiliser',
    redeemed: 'UtilisÃ©',
    promoOk: '+{coins} Prime Coins ajoutÃ©s.',
    promoFailed: 'Impossible d\'utiliser le code.',
    syncSynced: 'SynchronisÃ©',
    syncLocal: 'Local',
    tabs: {
      catalog: 'Catalogue',
      history: 'Historique',
      promos: 'Promos'
    },
    categories: {
      all: 'Tout',
      cosmetic: 'CosmÃ©tiques',
      theme: 'ThÃ¨mes',
      background: 'Fonds',
      badge: 'Badges'
    }
  },
  cosmetics: {
    characterPreview: 'AperÃ§u personnage',
    previewHint: 'Le loadout Ã©quipÃ© se sync dans le jeu. Capes et ailes visibles pour les peers Prime.',
    peersNote: 'Visible pour les peers Prime (LAN / intÃ©grÃ©)',
    all: 'Tout',
    capes: 'Capes',
    wings: 'Ailes',
    badges: 'Badges',
    emptyOwned: 'Aucun cosmÃ©tique â€” visitez la Boutique.'
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
    addSoon: 'Import de skins custom bientÃ´t',
    applySkin: 'Appliquer ce skin',
    applyCosmetic: 'Appliquer le cosmÃ©tique',
    changeCape: 'Changer cape',
    openStore: 'Ouvrir la Boutique',
    remove: 'Supprimer',
    emptyCosmeticsHint: 'DÃ©bloque des cosmÃ©tiques dans la Boutique, puis Ã©quipe-les ici.'
  },
  onboarding: {
    eyebrow: 'Configuration initiale',
    subtitle: 'Quelques prÃ©fÃ©rences, puis vous Ãªtes prÃªt Ã  jouer.',
    next: 'Continuer',
    back: 'Retour',
    finish: 'Câ€™est parti',
    skip: 'Passer pour lâ€™instant',
    browseFolder: 'Choisir un dossier',
    useDefaultFolder: 'Utiliser le dÃ©faut',
    folderHint: 'Les instances et fichiers de jeu seront stockÃ©s ici. Gardez le dÃ©faut sauf besoin dâ€™un autre disque.',
    signedIn: 'PrÃªt Ã  lancer',
    steps: {
      language: {
        short: 'Langue',
        title: 'Choisis ta langue',
        body: 'English ou FranÃ§ais â€” modifiable plus tard dans RÃ©glages.'
      },
      theme: {
        short: 'ThÃ¨me',
        title: 'Choisis un style',
        body: 'Sâ€™applique au launcher et se synchronise dans le profil Prime en jeu.'
      },
      ram: {
        short: 'RAM',
        title: 'MÃ©moire par dÃ©faut',
        body: 'RAM allouÃ©e aux nouvelles instances. Modifiable par instance ensuite.'
      },
      folder: {
        short: 'Dossier',
        title: 'Dossier des instances',
        body: 'Emplacement disque oÃ¹ Prime stocke les instances Minecraft.'
      },
      account: {
        short: 'Compte',
        title: 'Connexion',
        body: 'Microsoft est recommandÃ©. Les profils offline marchent en local.'
      }
    }
  },
  import: {
    title: 'Importer des instances',
    subtitle: 'DÃ©tecte dâ€™autres launchers et copie mods, packs, captures et options.',
    scanning: 'Scan des dossiers courantsâ€¦',
    foundCount: '{count} trouvÃ©(s)',
    notFound: 'Non dÃ©tectÃ©',
    selectInstances: 'SÃ©lectionne les instances Ã  importer',
    selectAll: 'Tout sÃ©lectionner',
    deselectAll: 'Tout dÃ©sÃ©lectionner',
    loadingInstances: 'Chargement des instancesâ€¦',
    emptySource: 'Aucune instance pour ce launcher.',
    importing: 'Importâ€¦',
    importSelected: 'Importer ({count})',
    success: '{count} instance(s) importÃ©e(s).',
    errorDetect: 'Impossible de scanner les autres launchers.',
    errorList: 'Impossible de lister les instances.',
    errorImport: 'Ã‰chec de lâ€™import.',
    tagMods: 'mods',
    tagPacks: 'resource packs'
  },
  social: {
    title: 'Amis',
    onlineCount: '{count} en ligne',
    emptyTitle: 'Pas encore dâ€™amis',
    emptyDesc: 'Ajoute des amis pour voir qui est en ligne et rejoindre leur serveur.',
    openFriends: 'Ouvrir Amis'
  },
  whatsNew: {
    eyebrow: 'Mise Ã  jour',
    title: 'NouveautÃ©s',
    gotIt: 'Compris',
    changelog: 'Changelog complet'
  },
  instances: {
    vanilla: 'Vanilla',
    fabric: 'Fabric',
    StellarClient: 'StellarClient',
    primeBadge: 'Prime',
    newInstance: 'Nouvelle instance',
    import: 'Importer',
    createPrime26: 'Prime 26.2',
    createPrime121: 'Prime 1.21.11',
    play: 'Jouer',
    default: 'Par dÃ©faut',
    setDefault: 'DÃ©finir par dÃ©faut',
    loading: 'Chargement des instancesâ€¦',
    signInToPlay: 'Se connecter',
    lastPlayed: 'DerniÃ¨re session {date}',
    ramBadge: '{mb} Mo RAM',
    modsBadge: '{count} mods',
    emptyTitle: 'Aucune instance',
    emptyDesc: 'CrÃ©e une install StellarClient pour commencer.'
  },
  media: {
    openFolder: 'Ouvrir le dossier clips',
    refresh: 'Actualiser',
    replaysNote: 'Replays et clips apparaissent ici via StellarClient (config/StellarClient/clips).'
  },
  logs: {
    title: 'Logs de lancement',
    tab: 'Console',
    empty: 'Aucun log. Appuyez sur Jouer pour voir le tÃ©lÃ©chargement et le lancement ici.',
    openFolder: 'Ouvrir le dossier logs',
    clear: 'Effacer',
    copy: 'Tout copier',
    copied: 'CopiÃ© !',
    filterAll: 'Tous',
    filterInfo: 'Info',
    filterWarn: 'Warn',
    filterError: 'Erreurs',
    filterDebug: 'Debug'
  },
  crash: {
    title: 'Minecraft a crashÃ©',
    sessionDuration: 'Session de {duration}',
    context: 'Contexte :',
    screen: 'Ã‰cran :',
    primeInvolved: 'StellarClient impliquÃ©',
    suggestion: 'Piste de correction',
    openReport: 'Ouvrir le crash report',
    openLogs: 'Ouvrir les logs',
    sendReport: 'Envoyer Ã  Prime',
    askAi: 'Demander Ã  lâ€™IA',
    dismiss: 'Fermer',
    fix: {
      blurOnce:
        'Conflit de rendu GUI (limite de blur). Mettez StellarClient Ã  jour â€” ce bug est corrigÃ© dans les builds rÃ©cents.',
      outOfMemory:
        'Java manque de mÃ©moire. Augmentez la RAM dans les paramÃ¨tres de l\'instance ou fermez d\'autres applications.',
      primeMod:
        'Le crash vient de StellarClient ({location}). Mettez le mod Ã  jour ou dÃ©sactivez les modules rÃ©cemment modifiÃ©s.',
      modConflict:
        'Conflit de mods probable. DÃ©sactivez les mods ajoutÃ©s rÃ©cemment et retestez.',
      modConflictNamed:
        'Mods impliquÃ©s dans la stack trace : {mods}. DÃ©sactivez-les un par un.',
      loaderError:
        'Fabric ou un mod n\'a pas pu se charger. VÃ©rifiez que les versions correspondent Ã  Minecraft.',
      unknown:
        'Ouvrez le crash report pour les dÃ©tails complets. Si le problÃ¨me persiste, partagez-le au support.'
    }
  }
}
