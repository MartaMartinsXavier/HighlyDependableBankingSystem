#!/bin/bash



gnome-terminal --title="install" -- bash -c "mvn clean install ; $SHELL"

sleep 3
gnome-terminal --tab --title="Client 0" -- bash -c "cd Client/ ; mvn exec:java ; $SHELL"
gnome-terminal --tab --title="Client 1" -- bash -c "cd Client/ ; mvn exec:java -DmyNumber=1 ; $SHELL"
gnome-terminal --tab --title="Server 0" -- bash -c "cd Server/ ; mvn exec:java -Dmode=m; $SHELL"
gnome-terminal --tab --title="Server 1" -- bash -c "cd Server/ ; mvn exec:java -DmyNumber=1; $SHELL"
gnome-terminal --tab --title="Server 2" -- bash -c "cd Server/ ; mvn exec:java -DmyNumber=2; $SHELL"
gnome-terminal --tab --title="Server 3" -- bash -c "cd Server/ ; mvn exec:java -DmyNumber=3 ; $SHELL"




