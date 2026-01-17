#!/bin/bash
# check-prerequisites.sh - Check if all prerequisites are installed for Gatling tests

echo "=========================================="
echo "Gatling Prerequisites Check"
echo "=========================================="
echo ""

# Check Java
echo "Checking Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo "✅ Java: $(java -version 2>&1 | head -1)"
    else
        echo "❌ Java: Version $JAVA_VERSION found, but Java 21+ required"
        echo "   Install: brew install openjdk@21"
    fi
else
    echo "❌ Java: Not found"
    echo "   Install: brew install openjdk@21"
fi
echo ""

# Check Maven
echo "Checking Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn --version | head -1 | awk '{print $3}')
    echo "✅ Maven: $MVN_VERSION"
    mvn --version | head -3
else
    echo "❌ Maven: Not found"
    echo "   Install: brew install maven"
    echo ""
    echo "   After installation, verify with: mvn --version"
fi
echo ""

# Check Scala (optional - will be installed by Maven)
echo "Checking Scala (optional)..."
if command -v scala &> /dev/null; then
    echo "✅ Scala: $(scala -version 2>&1)"
else
    echo "ℹ️  Scala: Not found (will be installed by Maven)"
fi
echo ""

# Summary
echo "=========================================="
if command -v java &> /dev/null && command -v mvn &> /dev/null; then
    echo "✅ All prerequisites met!"
    echo ""
    echo "Next steps:"
    echo "  1. cd load-tests/gatling"
    echo "  2. mvn clean compile"
    echo "  3. mvn gatling:test -DbaseUrl=http://localhost:8080 -DapiKey=test-api-key-local-dev"
else
    echo "❌ Missing prerequisites"
    echo ""
    echo "Install missing tools:"
    if ! command -v mvn &> /dev/null; then
        echo "  brew install maven"
    fi
    if ! command -v java &> /dev/null || [ "$JAVA_VERSION" -lt 21 ]; then
        echo "  brew install openjdk@21"
    fi
fi
echo "=========================================="

