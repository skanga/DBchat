
set RELEASE_VER=4.1.0

:: 1. Update version in pom.xml
call mvn versions:set -DnewVersion=%RELEASE_VER%
call mvn versions:commit

:: 2. Build updates versioned docs and CliUtils via the Maven replacer plugin
call mvn clean package

:: 3. Commit and push to main
git add pom.xml DIR.md INSTALL.md README.md RELEASE.bat src/main/java/com/skanga/mcp/config/CliUtils.java
git commit -m "Release version %RELEASE_VER%"
git push origin main
