-- Since 1.22.0
-- System Administrator
UPDATE role SET
    description='Manages system configuration and users.'
  WHERE role_id='1b92fae3e55a411793a091fb821c422d';

-- Old CLM Administrator
UPDATE role SET
    name='Policy Administrator',
    name_lowercase_no_whitespace='policyadministrator'
  WHERE role_id='b9646757e98e486da7d730025f5245f8';
