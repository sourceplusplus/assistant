SELECT
  app_uuid, artifact_qualified_name, create_date, last_updated, endpoint, subscribe_automatically,
  force_subscribe, module_name, component, endpoint_name, endpoint_ids, latest_failed_span
FROM source_artifact
WHERE 1=1
AND app_uuid = ?
AND latest_failed_span IS NOT NULL;