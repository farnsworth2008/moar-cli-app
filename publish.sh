set -e

./build.sh
cd cli/dist
npm publish
