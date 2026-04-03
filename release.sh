#!/bin/bash

# release.sh - Script to create a new release
# Usage: ./release.sh <version> [--dry-run]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 <version> [--dry-run]"
    echo ""
    echo "Examples:"
    echo "  $0 1.0.1              # Create release 1.0.1"
    echo "  $0 1.1.0 --dry-run    # Preview what would happen"
    echo ""
    echo "Version format: X.Y.Z (semantic versioning)"
    exit 1
}

# Check if version is provided
if [ $# -eq 0 ]; then
    show_usage
fi

VERSION=$1
DRY_RUN=false

if [ "$2" == "--dry-run" ]; then
    DRY_RUN=true
    print_warning "DRY RUN MODE - No changes will be made"
fi

# Validate version format
if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    print_error "Invalid version format. Use semantic versioning (X.Y.Z)"
    exit 1
fi

# Check if we're on main branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    print_error "Please switch to main branch before creating a release"
    exit 1
fi

# Check if working directory is clean
if [[ -n $(git status --porcelain) ]]; then
    print_error "Working directory is not clean. Please commit or stash changes"
    exit 1
fi

# Check if tag already exists
if git rev-parse "v$VERSION" >/dev/null 2>&1; then
    print_error "Tag v$VERSION already exists"
    exit 1
fi

print_status "Creating release v$VERSION"

# Update version in pom.xml
print_status "Updating version in pom.xml"
if [ "$DRY_RUN" == "false" ]; then
    mvn versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false
fi

# Test all variants
print_status "Testing all build variants..."

variants=(
    "basic:"
    "standard:-P standard-databases"
    "enterprise:-P standard-databases,enterprise-databases"
    "cloud-analytics:-P standard-databases,cloud-analytics"
    "all:-P standard-databases,enterprise-databases,cloud-analytics,big-data"
)

for variant in "${variants[@]}"; do
    name=$(echo $variant | cut -d: -f1)
    profiles=$(echo $variant | cut -d: -f2)

    print_status "Testing $name variant..."
    if [ "$DRY_RUN" == "false" ]; then
        mvn clean test $profiles
    fi
done

# Build all variants
print_status "Building all variants..."
mkdir -p target/releases

for variant in "${variants[@]}"; do
    name=$(echo $variant | cut -d: -f1)
    profiles=$(echo $variant | cut -d: -f2)

    print_status "Building $name variant..."
    if [ "$DRY_RUN" == "false" ]; then
        mvn clean package $profiles -DskipTests
        cp target/dbchat-$VERSION.jar target/releases/dbchat-$VERSION-$name.jar
    fi
done

# Show file sizes
if [ "$DRY_RUN" == "false" ]; then
    print_status "Build artifacts:"
    ls -lh target/releases/
fi

# Generate changelog
print_status "Generating changelog..."
LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

if [ "$DRY_RUN" == "false" ]; then
    cat > CHANGELOG-$VERSION.md << EOF
# Release v$VERSION

## Changes
EOF

    if [ -n "$LATEST_TAG" ]; then
        echo "Since $LATEST_TAG:" >> CHANGELOG-$VERSION.md
        git log --pretty=format:"- %s (%h)" $LATEST_TAG..HEAD >> CHANGELOG-$VERSION.md
    else
        echo "Initial release" >> CHANGELOG-$VERSION.md
        git log --pretty=format:"- %s (%h)" >> CHANGELOG-$VERSION.md
    fi

    cat >> CHANGELOG-$VERSION.md << EOF

## Available Variants

| Variant | Description | File Size |
|---------|-------------|-----------|
EOF

    for variant in "${variants[@]}"; do
        name=$(echo $variant | cut -d: -f1)
        case $name in
            "basic") desc="H2, SQLite, PostgreSQL" ;;
            "standard") desc="MySQL, MariaDB, ClickHouse" ;;
            "enterprise") desc="Oracle, SQL Server, IBM DB2" ;;
            "cloud-analytics") desc="Redshift, Snowflake, BigQuery" ;;
            "all") desc="All database drivers (400MB+)" ;;
        esac

        if [ -f "target/releases/dbchat-$VERSION-$name.jar" ]; then
            size=$(ls -lh target/releases/dbchat-$VERSION-$name.jar | awk '{print $5}')
            echo "| $name | $desc | $size |" >> CHANGELOG-$VERSION.md
        fi
    done

    cat >> CHANGELOG-$VERSION.md << EOF

## Installation

Download the appropriate variant for your needs:

\`\`\`bash
# Basic variant (smallest)
wget https://github.com/skanga/dbchat/releases/download/v$VERSION/dbchat-$VERSION-basic.jar

# Standard databases
wget https://github.com/skanga/dbchat/releases/download/v$VERSION/dbchat-$VERSION-standard.jar

# Enterprise databases
wget https://github.com/skanga/dbchat/releases/download/v$VERSION/dbchat-$VERSION-enterprise.jar

# Cloud analytics
wget https://github.com/skanga/dbchat/releases/download/v$VERSION/dbchat-$VERSION-cloud-analytics.jar

# All drivers (largest)
wget https://github.com/skanga/dbchat/releases/download/v$VERSION/dbchat-$VERSION-all.jar
\`\`\`

## Usage

\`\`\`bash
java -jar dbchat-$VERSION-<variant>.jar
\`\`\`
EOF

    print_status "Changelog created: CHANGELOG-$VERSION.md"
fi

# Commit and tag
if [ "$DRY_RUN" == "false" ]; then
    print_status "Committing version update..."
    git add pom.xml README.md INSTALL.md DIR.md src/main/java/com/skanga/mcp/config/CliUtils.java
    git commit -m "Release v$VERSION"

    print_status "Creating tag v$VERSION..."
    git tag -a "v$VERSION" -m "Release v$VERSION"

    print_status "Pushing to origin..."
    git push origin main
    git push origin "v$VERSION"

    print_status "Release v$VERSION created successfully!"
    print_status "GitHub Actions will automatically:"
    print_status "  - Build all variants"
    print_status "  - Create GitHub release"
    print_status "  - Upload artifacts"
    print_status "  - Publish to Maven Central (basic variant)"

    print_status "You can monitor the progress at:"
    print_status "  https://github.com/skanga/dbchat/actions"
else
    print_status "DRY RUN: Would create release v$VERSION with the following variants:"
    for variant in "${variants[@]}"; do
        name=$(echo $variant | cut -d: -f1)
        profiles=$(echo $variant | cut -d: -f2)
        echo "  - $name: mvn clean package $profiles"
    done
fi

print_status "Done!"
