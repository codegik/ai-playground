#!/bin/bash

set -e

supabase start
supabase status
supabase functions serve --no-verify-jwt
