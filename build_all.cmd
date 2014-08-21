e:
cd \apps\ABM

call mvn package -P aztec_at,2010 -DskipTests
call mvn package -P aztec_at,2012 -DskipTests
call mvn package -P aztec_at,2020 -DskipTests
call mvn package -P aztec_at,2035 -DskipTests
call mvn package -P aztec_at,2050 -DskipTests

call mvn package -P charger_at,2010 -DskipTests
call mvn package -P charger_at,2012 -DskipTests
call mvn package -P charger_at,2020 -DskipTests
call mvn package -P charger_at,2035 -DskipTests
call mvn package -P charger_at,2050 -DskipTests

call mvn package -P mustang_at,2010 -DskipTests
call mvn package -P mustang_at,2012 -DskipTests
call mvn package -P mustang_at,2020 -DskipTests
call mvn package -P mustang_at,2035 -DskipTests
call mvn package -P mustang_at,2050 -DskipTests

call mvn package -P gaucho_at,2010 -DskipTests
call mvn package -P gaucho_at,2012 -DskipTests
call mvn package -P gaucho_at,2020 -DskipTests
call mvn package -P gaucho_at,2035 -DskipTests
call mvn package -P gaucho_at,2050 -DskipTests