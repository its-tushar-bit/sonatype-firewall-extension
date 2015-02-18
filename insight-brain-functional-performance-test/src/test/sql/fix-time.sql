-- java -cp h2-*.jar org.h2.tools.RunScript -url "jdbc:h2:file:ods;DATABASE_TO_UPPER=FALSE" -script fix-time.sql -showResults -time

SET SCHEMA insight_brain_ods;

SET @timeDelta = SELECT datediff('mi', max(time), now()) from policy_evaluation;

UPDATE policy_evaluation     SET time = dateadd('mi', @timeDelta, time);
UPDATE policy_violation      SET time = dateadd('mi', @timeDelta, time);
UPDATE application_component SET time = dateadd('mi', @timeDelta, time);

UPDATE hash_component_identifier SET create_time = dateadd('mi', @timeDelta, create_time);
UPDATE policy_waiver SET create_time = dateadd('mi', @timeDelta, create_time);
