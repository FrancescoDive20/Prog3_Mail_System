# Progetto di Laboratorio di Programmazione III: Sistema di Posta Elettronica

Questo repository contiene l'implementazione di un sistema distribuito Client-Server per la 
gestione della posta elettronica, sviluppato come prova d'esame per il corso di Programmazione III. 
L'infrastruttura rispetta i requisiti di scalabilità, esecuzione concorrente, mutua esclusione 
e tolleranza ai problemi.

## 🏛 Architettura e Scelte Ingegneristiche

Il sistema è strutturato come un progetto Maven multi-modulo che separa le logiche di dominio 
condivise (`protocolloCondiviso`), il layer di rete (`mailServer`) e l'interfaccia utente 
(`mailClient`).

### 1. Pattern MVC e GUI (JavaFX)

Sia l'applicativo Client che il Server sono sviluppati in JavaFX, aderendo strettamente al pattern 
architetturale **Model-View-Controller (MVC)**.

* **Disaccoppiamento strutturale:** Non sussiste alcuna comunicazione diretta tra il livello View 
(interfacce grafiche FXML) e il Model. Ogni interazione è mediata dal Controller.


* **Pattern Observer-Observable:** L'aggiornamento dell'interfaccia grafica avviene in modo 
interamente reattivo. Ottemperando al divieto di utilizzare le classi deprecate `java.util.Observer`
e `Observable`, il progetto sfrutta nativamente le `Properties` e le `ObservableList` fornite da 
JavaFX. Questo garantisce che la GUI mostri automaticamente la lista dei messaggi aggiornata senza 
refresh manuali.


* **Programmazione ad Eventi:** L'interazione dell'utente genera eventi asincroni catturati da 
appositi *event handlers* (Listener), separando il flusso di controllo dalla presentazione visiva.



### 2. Networking e Socket Stateless

La comunicazione distribuita avviene unicamente tramite la trasmissione di dati testuali 
(oggetti serializzati in JSON) su Java Socket.

* **Connessioni Non Permanenti:** Per soddisfare i requisiti di scalabilità, i socket vengono aperti
dal client solo nel momento in cui necessita di un'operazione, simulando un approccio *stateless* 
di tipo *request-response* (simile ad HTTP). Il canale viene chiuso immediatamente dopo la ricezione
della risposta.


* **Sincronizzazione Differenziale (Delta Sync):** L'architettura previene il trasferimento di intere
caselle di posta elettronica, limitando il consumo di banda. Il Client invia al Server l'ID 
dell'ultimo messaggio noto e il Server trasmette esclusivamente i messaggi non precedentemente 
distribuiti.



### 3. Concorrenza e Mutua Esclusione

* **Thread Pool:** Il Server accetta le connessioni in ingresso delegando l'elaborazione del singolo
client a un `ExecutorService` (Fixed Thread Pool). Questo approccio ottimizza le risorse di sistema
prevenendo l'overhead dovuto alla creazione continua di nuovi `Thread`.


* **Persistenza e Lock Striping:** Le caselle postali sono memorizzate fisicamente su file JSON, 
senza l'ausilio di database relazionali. Per garantire la mutua esclusione e prevenire *deadlock*
o corruzione dei dati durante invii paralleli, il `MailboxManager` utilizza un 
`ReentrantReadWriteLock` distinto per ogni utente. Ciò permette letture simultanee ma garantisce
accessi in scrittura rigidamente esclusivi.



### 4. Tolleranza ai Guasti e Resilienza

* Il sistema implementa una solida gestione delle eccezioni di I/O. In caso di spegnimento improvviso
del Server, il Client non termina l'esecuzione in modo anomalo (crash), soddisfacendo un requisito 
primario di progetto. L'applicativo notifica visivamente l'utente e utilizza un Thread Demone 
(Daemon Thread) in background (`ScheduledExecutorService`) per tentare un *polling* continuo, 
riconnettendosi automaticamente al ripristino del servizio.


* La verifica della ben-formatezza degli indirizzi (tramite Regex) viene eseguita in locale sul 
Client, delegando al Server la sola responsabilità di certificare la reale esistenza dell'account 
ed emettere messaggi di errore qualora il destinatario risulti inesistente.



---

## 🚀 Istruzioni di Compilazione ed Esecuzione

Assicurarsi che l'ambiente sia configurato con **Java JDK 17** (o superiore) e **Apache Maven**.

### 1. Compilazione del Progetto

Aprire il terminale nella root directory del progetto (dove risiede il `pom.xml` padre aggregatore)
ed eseguire il seguente comando per pulire, compilare l'infrastruttura e scaricare le dipendenze:

```bash
mvn clean install -DskipTests

```

### 2. Avvio del Mail Server

Per garantire la corretta instaurazione dei socket, avviare tassativamente il Mail Server **prima** dei Client.

* **Metodo 1: Tramite IDE (Consigliato)**
  Eseguire il metodo `main` all'interno della classe wrapper `it.unito.mailserver.ServerLauncher`. 
Questo bypasserà eventuali restrizioni dei moduli JavaFX, inizializzando il Thread Demone
  (Daemon Thread) in ascolto e aprendo la console di log grafica.
* **Metodo 2: Tramite Maven (Terminale)**
```bash
mvn javafx:run -pl mailServer

```



### 3. Avvio dei Mail Client (Multi-Istanza)

Per simulare un ambiente distribuito e superare i collaudi d'esame, è necessario lanciare 
**almeno 3 istanze simultanee** del Client.

* **Metodo 1: Tramite IDE (Consigliato per l'esame)**
  Eseguire la classe wrapper `it.unito.mailclient.ClientLauncher`.
> **Nota per IntelliJ IDEA:** Per avviare più finestre contemporaneamente, aprire la configurazione 
> di Run ("Edit Configurations" -> "Modify options") e spuntare la voce 
> **"Allow multiple instances"**. Cliccare poi il tasto *Run* tre volte consecutive.


* **Metodo 2: Tramite Maven (Terminale)**
  Aprire 3 terminali separati ed eseguire in ciascuno:
```bash
mvn javafx:run -pl mailClient

```



---

## 👥 Specificazioni

Come da specifiche, il sistema non prevede una procedura di registrazione di nuovi account dal 
Client, operando su un dominio di utenti preconfigurato sul Server. I file fisici `.json` di questi
utenti verranno creati e gestiti automaticamente dal `MailboxManager` nella directory 
`mailServer/server_storage/` al primo invio.

Per collaudare il sistema, accedere ai Client utilizzando i seguenti account pre-registrati:

1. `mario@unito.it`
2. `francesco@unito.it`
3. `giuseppe@unito.it`

