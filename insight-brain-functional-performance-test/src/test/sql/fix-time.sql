-- java -cp h2-*.jar org.h2.tools.RunScript -url "jdbc:h2:file:ods;DATABASE_TO_UPPER=FALSE" -script fix-time.sql -showResults -time

SET SCHEMA insight_brain_ods;

SET @timeDelta = SELECT datediff('ms', max(time), now()) from policy_evaluation;

UPDATE policy_evaluation     SET time = dateadd('ms', @timeDelta, time);
UPDATE policy_violation      SET time = dateadd('ms', @timeDelta, time);
UPDATE application_component SET time = dateadd('ms', @timeDelta, time);

UPDATE hash_gav      SET create_time = dateadd('ms', @timeDelta, create_time);
UPDATE policy_waiver SET create_time = dateadd('ms', @timeDelta, create_time);
