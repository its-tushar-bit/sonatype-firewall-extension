-- Since 1.91
ALTER TABLE repository_component
  ADD COLUMN analyzer_features_json varchar(1000);
