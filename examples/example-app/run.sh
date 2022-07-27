#!/bin/bash
sbt "runMain org.virtuslab.example.App compute 25251" &
sbt "runMain org.virtuslab.example.App compute 25252" &
sbt "runMain org.virtuslab.example.App compute  0" &
sbt "runMain org.virtuslab.example.App client 0"
