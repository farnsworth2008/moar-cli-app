set -e

# Build with gradle
BUILD_VERSION="`git describe --always --tags --long`"
./gradlew "-Dbuild_version=${BUILD_VERSION}" fatJar
mv moar-cli-j-main/build/libs/moar-cli-j-main.fat.jar cli/bin

# Build with npm
npm uninstall -g moar-cli
cd cli
echo "building moar-cli"
npm install babel-register babel-preset-env --save
npm run-script build
npm install -g
cd ..
