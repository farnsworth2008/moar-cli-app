set -e

./gradlew fatJar; 
echo 

java -Dmoar.ansi.enabled=true -jar ./moar-cli-j-main/build/libs/moar-cli-j-main.fat.jar "$@"
