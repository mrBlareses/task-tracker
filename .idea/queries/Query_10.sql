select id, job_name, status, trigger_type, started_at, finished_at, error_message
from job_execution
order by started_at desc;
