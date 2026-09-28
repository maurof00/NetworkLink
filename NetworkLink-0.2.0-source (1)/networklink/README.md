# NetworkLink 0.2.0

NetworkLink è una base multi-modulo per reti Minecraft con nodi Spigot/Paper e proxy BungeeCord o Velocity.

## Collegamento node-to-node

Il collegamento non è limitato a proxy↔proxy. Ogni installazione NetworkLink è un **nodo** e può collegarsi a un altro nodo:

```text
Velocity / Bungee
       │
       ├── NetworkLink ──> Spigot/Paper LOBBY
       │
       └── NetworkLink ──> Velocity / Bungee
```

Il nodo Spigot/Paper ha una propria API/dashboard, quindi può essere controllato direttamente dal proxy oppure da un altro nodo NetworkLink.

## Funzioni presenti

- collegamento BungeeCord ↔ BungeeCord;
- collegamento Velocity ↔ Velocity;
- collegamento BungeeCord ↔ Velocity;
- collegamento proxy ↔ Spigot/Paper;
- collegamento Spigot/Paper ↔ proxy/altro nodo;
- configurazione dei link via `networklink.properties`;
- aggiunta/rimozione dei link via `/networklink`;
- aggiunta/rimozione dei link dalla dashboard web;
- dashboard web integrata, senza database esterno;
- stato online/offline, giocatori e backend;
- manutenzione locale e remota;
- blocco dei giocatori normali sui server in manutenzione;
- bypass manutenzione con permission configurabile;
- autenticazione dei nodi e dashboard tramite token HTTP.

## Manutenzione

La manutenzione appartiene al nodo/server che la espone. Per esempio, se il server Spigot `LOBBY` usa:

```properties
node.id=LOBBY
```

e il proxy ha:

```properties
links.LOBBY.url=http://10.0.0.20:8090
links.LOBBY.token=TOKEN-LOBBY
```

quando `LOBBY` viene messo in manutenzione, BungeeCord e Velocity leggono lo stato del nodo e bloccano la connessione dei giocatori normali verso `LOBBY`. La connessione viene consentita ai giocatori che possiedono:

```text
networklink.maintenance.bypass
```

Spigot/Paper applica inoltre il blocco al login diretto quando la manutenzione locale è attiva.

Configurazione:

```properties
maintenance.enabled=false
maintenance.reason=Server in manutenzione. Riprova piu tardi.
maintenance.bypass-permission=networklink.maintenance.bypass
```

Comandi disponibili su Bungee, Velocity e Spigot/Paper:

```text
/networklink maintenance on
/networklink maintenance off
/networklink maintenance on LOBBY Manutenzione programmata
/networklink maintenance off LOBBY
```

Senza `node-id` il comando modifica la manutenzione del nodo locale. Con `node-id` modifica un nodo remoto collegato.

## Dashboard

Esempi:

```text
http://IP_PROXY:8088/
http://IP_LOBBY:8090/
```

Dalla dashboard è possibile attivare/disattivare la manutenzione locale e quella dei nodi remoti collegati, oltre a visualizzare motivo e stato della manutenzione.

## API

```text
GET  /api/v1/status
GET  /api/v1/dashboard
GET  /api/v1/links
POST /api/v1/links
DELETE /api/v1/links?id=<id>
POST /api/v1/maintenance
POST /api/v1/remote-maintenance
```

Le API amministrative richiedono l'header:

```text
X-NetworkLink-Token: <token>
```

## Build

Il progetto usa Gradle e Java 25.

```bash
gradle buildAll
```

Output previsti:

```text
common/build/libs/NetworkLink-common-0.2.0.jar
bungee/build/libs/NetworkLink-bungee-0.2.0.jar
velocity/build/libs/NetworkLink-velocity-0.2.0.jar
spigot/build/libs/NetworkLink-spigot-0.2.0.jar
```

I tre adapter includono il modulo `common` nel proprio JAR.

## Installazione

### BungeeCord / Waterfall

Metti `NetworkLink-bungee-0.2.0.jar` in `plugins/` e avvia il proxy. La configurazione viene creata in `plugins/NetworkLink/networklink.properties`.

### Velocity

Metti `NetworkLink-velocity-0.2.0.jar` nella cartella `plugins/` e avvia il proxy. La configurazione viene creata nella cartella dati del plugin.

### Spigot / Paper

Metti `NetworkLink-spigot-0.2.0.jar` in `plugins/` del backend e avvialo una volta. La configurazione viene creata nella cartella `plugins/NetworkLink/`.

## Prossime funzioni

La parte cross-node è isolata nel modulo `common`, mentre Bungee, Velocity e Spigot/Paper hanno adapter separati. Questo permette di aggiungere in seguito trasferimento giocatori, messaggi/comandi cross-server, fallback, statistiche, heartbeat, gestione centralizzata e ruoli dashboard senza rifare il protocollo base.
