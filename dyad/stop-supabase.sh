#!/bin/bash

PID=$(pgrep -f "supabase functions serve --no-verify-jwt")

if [ -z "$PID" ]; then
  echo "Supabase functions server is not running."
else
  echo "Stopping Supabase functions server (PID: $PID)..."
  kill $PID
  echo "Supabase functions server stopped."
fi

supabase stop