set -e

# Build with gradle
BUILD_VERSION="`git describe --always --tags --long`"
./gradlew "-Dbuild_version=${BUILD_VERSION}" fatJar
mv moar-cli-app-main/build/libs/moar-cli-app-main.fat.jar cli/bin

# Build with npm
npm uninstall -g moar-cli
cd cli
echo "building moar-cli"
npm install 
npm run build
cp README.md package-lock.json package.json dist
cd dist
ln -s ../node_modules node_modules
ln -s ../bin bin 
npm install -g .
