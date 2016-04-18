e:
cd \apps\ABM

call mvn package -P aztec,2016 -DskipTests
call mvn package -P wildcat,2016 -DskipTests
call mvn package -P gaucho,2016 -DskipTests
call mvn package -P local,2016 -DskipTests
