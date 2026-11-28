#!/bin/sh
# Uso: ./cliente.sh [ip] [puerto]
java -cp dist/BatallaNaval.jar ar.edu.et32.batallanaval.cliente.MainCliente "$@"
