-- Since 1.160
ALTER TABLE repository ADD COLUMN repository_type varchar(10) DEFAULT 'proxy' NOT NULL;
ALTER TABLE repository ADD COLUMN policy_compliant_component_selection_enabled boolean DEFAULT false NOT NULL;
ALTER TABLE repository ADD COLUMN namespace_confusion_protection_enabled boolean DEFAULT false NOT NULL;
