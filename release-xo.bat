set VERSION=%1
set BRANCH="xomad-%VERSION%"

mvn versions:set -DgenerateBackupPoms=false "-DnewVersion=%VERSION%" && ^
mvn -pl "querydsl-ext-api,querydsl-ext-impl" -am clean install -DskipTests
