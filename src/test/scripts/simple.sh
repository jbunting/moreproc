#!/usr/bin/env bash

sleep 1

echo "Hello folks..."
echo "Arg $1"

echo "This is error text" 1>&2
exit 0
