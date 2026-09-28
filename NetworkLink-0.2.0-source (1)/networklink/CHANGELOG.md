# Changelog

## 0.2.0
- Added node maintenance mode with persistent config and reason.
- Added `networklink.maintenance.bypass` permission.
- BungeeCord/Velocity block connections to maintained linked nodes.
- Spigot/Paper blocks direct logins while local maintenance is enabled.
- Added dashboard controls for local and remote maintenance.


## 0.1.1 - 2026-09-28

- NetworkLink ora usa un modello **node-to-node**.
- Spigot/Paper è diventato un nodo completo, con dashboard/API propria.
- Un proxy Bungee/Velocity può collegarsi direttamente a un server Spigot/Paper che esegue NetworkLink.
- Un server Spigot/Paper può collegarsi a Bungee, Velocity o altri backend NetworkLink.
- Aggiunti `/networklink link`, `/networklink unlink`, `/networklink reload` e `/networklink dashboard` su Spigot/Paper.
- Aggiunta permission `networklink.admin` su Spigot/Paper.
- Aggiornati esempi di configurazione e documentazione.

## 0.1.0 - 2026-09-28

Prima base del progetto:

- modulo common con protocollo snapshot cross-proxy;
- adapter BungeeCord e Velocity;
- companion Spigot/Paper;
- dashboard web embedded;
- autenticazione tramite token;
- linking tramite config, comandi e dashboard;
- polling e ping dei backend;
- struttura predisposta per le prossime funzioni.
