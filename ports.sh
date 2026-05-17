#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: $0 <port>"
    exit 1
fi

port=$1

pid=$(lsof -ti :"$port" 2>/dev/null)

if [ -z "$pid" ]; then
    echo "No process found on port $port"
    exit 1
fi

echo "$pid"