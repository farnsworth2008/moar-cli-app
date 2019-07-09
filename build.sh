set -e

# Build with gradle
BUILD_VERSION="`git describe --always --tags --long`"
./gradlew "-Dbuild_version=${BUILD_VERSION}" fatJar
mv moar-cli-app-main/build/libs/moar-cli-app-main.fat.jar cli/bin

# Build with npm
npm uninstall -g moar-cli
cd cli
echo "building moar-cli"
npm install -g
cd ..
