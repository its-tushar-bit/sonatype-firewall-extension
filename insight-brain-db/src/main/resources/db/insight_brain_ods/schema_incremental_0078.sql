-- Since 1.15.0
SET SCHEMA insight_brain_ods;

-- Add the new "CLM Administrator" role to all users/groups that have the "System Administrator" role
INSERT INTO membership_mapping (membership_mapping_id, context_id, role_id, member_name, member_type)
  SELECT REPLACE(RANDOM_UUID(),'-'), 'global', 'b9646757e98e486da7d730025f5245f8', member_name, member_type FROM membership_mapping WHERE role_id='1b92fae3e55a411793a091fb821c422d'
    AND member_name NOT IN (SELECT member_name FROM membership_mapping WHERE role_id='b9646757e98e486da7d730025f5245f8')

