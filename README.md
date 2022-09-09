# SECProj

# Setup install 
chmod +x *.sh
./installGnome.sh


# Run scripts
to start 4 regular servers and 2 clients run:
./run4Servers.sh


# Manual Instructions
to install in root folder:
mvn clean install

## Generating keys example for 1f (4 servers)
cd Crypto
mvn exec:java -DnumberOfClients=2 -DnumberOfFaults=1

## Client options (run inside Client folder):
<mode> is "m" or "r" (r is default, r uses regular, m is byzantine simulation)
<myNumber> the number id of the client (incremental)
<numberOfByzantinesToTolerate> number of byzantine servers to tolerate from which we define total server count

mvn exec:java -Dmode=<mode> -DmyNumber=<myNumber> -Dbyzantine=<numberOfByzantinesToTolerate>
### example 1 malicious 1 honest:
cd Client/ ; mvn exec:java -DmyNumber=0 -Dmode=m
cd Client/ ; mvn exec:java -DmyNumber=1


## Server options (run inside the Server folder):
<mode> is "m" or "r" (r is default, r uses regular, m is byzantine simulation)
<myNumber> the id (incremental from 0) a number that ids the server
<numberOfByzantinesToTolerate> number of byzantine servers to tolerate from which we define total server count

mvn exec:java -Dmode=<mode> -DmyNumber<myNumber> -Dbyzantine<numberOfByzantinesToTolerate>
### example 4 honest servers
cd Server/ ; mvn exec:java -DmyNumber=0
cd Server/ ; mvn exec:java -DmyNumber=1
cd Server/ ; mvn exec:java -DmyNumber=2
cd Server/ ; mvn exec:java -DmyNumber=3


# Running Normal Experiment
launch either manually or with a script 2 clients and 4 servers 
when providing the path to public key simplify with "clientPublicKey" + the number identifying that client
### on client0:
open
check
clientPublicKey0

### on client1:
open
check
clientPublicKey1
clientPublicKey0

### on client0:
send
clientPublicKey1
10

Read on screen the id of the transaction pending and copy
### on client1:
check
clientPublicKey1
receive
<insert transactionID found in check>


# Running Normal Experiment
launch either manually or with a script 1 client regular 1 client malicious and 4 servers

### on client0 (malicious):
replay
replay



### on client1:
open
check

### on client0(malicious):
evilBroadcast

### on client0(malicious):
evilTransfer
clientPublicKey1
10

### on client1:
check

verify no change from malicious transaction
