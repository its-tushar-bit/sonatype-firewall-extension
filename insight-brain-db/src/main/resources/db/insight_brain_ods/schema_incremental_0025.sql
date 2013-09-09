SET SCHEMA insight_brain_ods;

UPDATE user SET password_hash='$shiro1$SHA-256$500000$MQE0sE4AN/+RmveFR2MruQ==$AnBUsybg4CT8HjK7zofGD9A+3xdDZTpUVDpp/K7wX9M=' WHERE user_id='ADMIN';
