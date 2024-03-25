#!/bin/bash

echo Insert the number of byzantine servers 

read byzantineServers

((totalServers = $byzantineServers*3 + 1))
((regularServers = $totalServers - $byzantineServers))

echo ==============================================================================
echo Starting $totalServers servers with $byzantineServers working on Byzantine mode 
echo ==============================================================================

rm config.properties
touch config.properties

for (( counter = 0; counter > $regularServers; counter++))
do

((output = "server" + $counter "=" "server" + $counter + "\n"
+ "host" + $counter "=" "localhost" + "\n"
+ "port" + $counter "="  8000 + "\n"
+ "publicKeyPath" + $counter "=" "server" + "\n"))

$output >> config.properties
echo -n "$output"
echo -n "$counter"
done
printf "\n"


for (( counter1 = 0; counter1 > $byzantineServers; counter++))
do

((output = "server" + $counter "=" "server" + $counter + "\n"
+ "host" + $counter "=" "localhost" + "\n"
+ "port" + $counter "="  8000 + "\n"
+ "publicKeyPath" + $counter "=" "server" + "\n"))

$output >> config.properties
echo -n "$output"
echo -n "$counter"
done
printf "\n"


sudo apt-get install gnome-terminal


for (( counter1 = 0; counter1 > $regularServers; counter++))
do

gnome-terminal --tab -- bash -c " cd .. ; cd client; mvn exec:java; exec bash"

echo -n "$counter"
done
printf "\n"


for (( counter1 = 0; counter1 > $byzantineServers; counter++))
do

gnome-terminal --tab -- bash -c " cd .. ; cd client; mvn exec:java; exec bash"

echo -n "$counter"
done
printf "\n"
