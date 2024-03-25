# Highly Dependable Banking System

Design and implement a replicated system resilient to byzantine faults to ensure availability, protecting it from malicious clients and servers


In this project, to tolerate f server faults, including Byzantine or Crash-faults, I implemented a modified version of the (1, N) Byzantine Atomic Register on top of Point-To-Point Authenticated Perfect Links and using Double Echo Authenticated Broadcast.

By replicating the server N = 3f + 1, we ensure that even with f server faults we can still ensure the availability of the system


# Attack vectors and mitigations

### Data Loss Prevention
This projects ensures the persistence of data even in case of **crash-faults** by having the client and the server store their state in a persistent file. To protect against **corrupted** or **incomplete storage**, I create a temporary logger file and atomically rename it to replace the original logger.

### DOS attack
Server overloading attacks are mitigated by implementing **proof of work** on the client. A client will have to by generating a hash of its public key plus a counter, starting with a given number of 0-bits which involves heavy computation for the client that is easy to verify by the server,

### Man in the Middle attacks
These threats are avoided by adding a **digital signature** (prevents modifications) and **freshness tokens** including a **timestamp** (shortens the window of the attack) and a nonce (invalidates duplicated replies) to all messages exchanged. Both freshness tokens are signed. On top of that the RID (read counter) and WTS work as a **challenge response** (prevents attackers from reusing stored dropped/ignored messages)
thus providing **integrity** and **authentication**, allowing the detection of modifications made to the original message and discarding any unauthorised request/response.

### Message tampering
Every record contain a signed hash of its contents, making any tampering attempts detectable.

### Replay attacks
By adding a freshness token to the messages that traverse the network, it is possible to prevent the unauthorised replay of information.

### Secure key storage
To store private keys more securely and make them resistant to brute-force attacks, key derivation is used. The secret key is derived from a password and additional data (the salt).
# Assumptions
Previous public key distribution.
This project assumes no client-server collusion. For client-server collusion, the number of replicas would have to be increased to  4f + 1



# How to run
### Setup install 
chmod +x *.sh
./installGnome.sh


### Run scripts
to start 4 regular servers and 2 clients run:
./run4Servers.sh


# Manual Instructions
to install in root folder:
mvn clean install

### Generating keys example for 1f (4 servers)
cd Crypto
mvn exec:java -DnumberOfClients=2 -DnumberOfFaults=1

### Client options (run inside Client folder):
<mode> is "m" or "r" (r is default, r uses regular, m is byzantine simulation)
<myNumber> the number id of the client (incremental)
<numberOfByzantinesToTolerate> number of byzantine servers to tolerate from which we define total server count

mvn exec:java -Dmode=<mode> -DmyNumber=<myNumber> -Dbyzantine=<numberOfByzantinesToTolerate>
#### example 1 malicious 1 honest:
cd Client/ ; mvn exec:java -DmyNumber=0 -Dmode=m
cd Client/ ; mvn exec:java -DmyNumber=1


### Server options (run inside the Server folder):
<mode> is "m" or "r" (r is default, r uses regular, m is byzantine simulation)
<myNumber> the id (incremental from 0) a number that ids the server
<numberOfByzantinesToTolerate> number of byzantine servers to tolerate from which we define total server count

mvn exec:java -Dmode=<mode> -DmyNumber<myNumber> -Dbyzantine<numberOfByzantinesToTolerate>
#### example 4 honest servers
cd Server/ ; mvn exec:java -DmyNumber=0
cd Server/ ; mvn exec:java -DmyNumber=1
cd Server/ ; mvn exec:java -DmyNumber=2
cd Server/ ; mvn exec:java -DmyNumber=3


### Running Normal Experiment
launch either manually or with a script 2 clients and 4 servers 
when providing the path to public key simplify with "clientPublicKey" + the number identifying that client
#### on client0:
open
check
clientPublicKey0

#### on client1:
open
check
clientPublicKey1
clientPublicKey0

#### on client0:
send
clientPublicKey1
10

Read on screen the id of the transaction pending and copy
#### on client1:
check
clientPublicKey1
receive
<insert transactionID found in check>


### Running Normal Experiment
launch either manually or with a script 1 client regular 1 client malicious and 4 servers

#### on client0 (malicious):
replay
replay



#### on client1:
open
check

#### on client0(malicious):
evilBroadcast

#### on client0(malicious):
evilTransfer
clientPublicKey1
10

#### on client1:
check

verify no change from malicious transaction
