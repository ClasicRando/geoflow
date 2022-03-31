DO $$DECLARE
    collectionPipeline text[] := ARRAY[
        'Build Pipeline Run',
        'Confirm Run Instance',
        'Download Missing Files',
        'Scan Source Folder',
        'Confirm Record Date',
        'Backup Files to Zip Folder',
        'Validate Source Tables'
    ];
    loadPipeline text[] := ARRAY[
        'Backup Old Tables',
        'Check If Data Is Old',
        'Analyze Files',
        'Check Table Stats',
        'Load Files',
        'Set Loading Logic',
        'Validate Loading Logic'
    ];
	r RECORD;
BEGIN
    INSERT INTO pipeline_tasks(pipeline_id,task_id,task_order)
    SELECT t1.pipeline_id, t2.task_id, i parent_task_order
    FROM   pipelines t1, tasks t2, (select name, row_number() over () i FROM unnest(collectionPipeline) as x(name)) t3
    WHERE  t1.name = 'Default Collection'
    AND    t2.name = t3.name;
    INSERT INTO pipeline_tasks(pipeline_id,task_id,task_order)
    SELECT t1.pipeline_id, t2.task_id, i parent_task_order
    FROM   pipelines t1, tasks t2, (select name, row_number() over () i FROM unnest(loadPipeline) as x(name)) t3
    WHERE  t1.name = 'Default Load'
    AND    t2.name = t3.name;
END$$;