DO $$DECLARE
    collectionPipeline text[] := ARRAY[
        'Build Pipeline Run',
        'Confirm Run Instance',
        'Scan Source Folder',
        'Confirm Record Date',
        'Backup Files to Zip Folder',
        'Validate Source Tables'
    ];
	r RECORD;
BEGIN
    INSERT INTO pipeline_tasks(pipeline_id,task_id,parent_task,parent_task_order)
    SELECT t1.pipeline_id, t2.task_id, 0 parent_task, i parent_task_order
    FROM   pipelines t1, tasks t2, (select name, row_number() over () i FROM unnest(collectionPipeline) as x(name)) t3
    WHERE  t1.name = 'Default Collection'
    AND    t2.name = t3.name;
END$$;