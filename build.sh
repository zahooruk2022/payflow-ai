#!/usr/bin/env bash
# PayFlow AI — CF build script
set -euo pipefail

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║  PayFlow AI — Cloud Foundry Build        ║"
echo "╚══════════════════════════════════════════╝"
echo ""

echo "▶ [1/2] Building React frontend..."
cd frontend
npm ci
npm run build
cd ..
echo "  ✓ Frontend built ($(find backend/src/main/resources/static -type f | wc -l | tr -d ' ') files)"

echo ""
echo "▶ [2/2] Building Spring Boot jar..."
cd backend
mvn clean package -DskipTests -q
cd ..

JAR="backend/target/payflow-ai.jar"
SIZE=$(du -sh "$JAR" | cut -f1)
echo "  ✓ Built: $JAR ($SIZE)"
echo ""
echo "╔══════════════════════════════════════════╗"
echo "║  Build complete — ready to deploy        ║"
echo "╚══════════════════════════════════════════╝"
echo ""
echo "  Next steps:"
echo "    cf push"
echo ""
