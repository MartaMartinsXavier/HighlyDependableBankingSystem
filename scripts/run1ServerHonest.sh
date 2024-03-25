#!/bin/bash


gnome-terminal --tab --title="install" --command="mvn clean install"

sleep 3
gnome-terminal --tab --title="Client 0" -- bash -c "cd Client/ ; mvn exec:java ; $SHELL"
gnome-terminal --tab --title="Client 1" -- bash -c "cd Client/ ; mvn exec:java -DmyNumber=1 ; $SHELL"
gnome-terminal --tab --title="Server" -- bash -c "cd Server/ ; mvn exec:java ; $SHELL"




